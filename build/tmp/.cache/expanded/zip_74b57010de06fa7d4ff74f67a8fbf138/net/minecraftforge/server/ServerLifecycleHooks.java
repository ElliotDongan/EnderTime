/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.world.StructureModifier;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.ConnectionType;
import net.minecraftforge.network.NetworkContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.registries.ForgeRegistries.Keys;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.GameData;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ServerLifecycleHooks {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker SERVERHOOKS = MarkerManager.getMarker("SERVERHOOKS");
    private static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");
    private static final AtomicBoolean allowLogins = new AtomicBoolean(false);
    private static volatile CountDownLatch exitLatch = null;
    private static MinecraftServer currentServer;

    private static Path getServerConfigPath(final MinecraftServer server) {
        final Path serverConfig = server.getWorldPath(SERVERCONFIG);
        if (!Files.isDirectory(serverConfig)) {
            try {
                Files.createDirectories(serverConfig);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return serverConfig;
    }

    public static boolean handleServerAboutToStart(final MinecraftServer server) {
        currentServer = server;
        // on the dedi server we need to force the stuff to setup properly
        LogicalSidedProvider.setServer(()->server);
        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.SERVER, getServerConfigPath(server));
        runModifiers(server);
        return !ServerAboutToStartEvent.BUS.post(new ServerAboutToStartEvent(server));
    }

    public static boolean handleServerStarting(final MinecraftServer server) {
        if (FMLEnvironment.dist.isDedicatedServer())
            LanguageHook.loadLanguagesOnServer(server);
        PermissionAPI.initializePermissionAPI();
        return !ServerStartingEvent.BUS.post(new ServerStartingEvent(server));
    }

    public static void expectServerStopped() {
        exitLatch = new CountDownLatch(1);
    }

    public static void handleServerStopped(final MinecraftServer server) {
        if (!server.isDedicatedServer()) GameData.revertToFrozen();
        ServerStoppedEvent.BUS.post(new ServerStoppedEvent(server));
        currentServer = null;
        LogicalSidedProvider.setServer(null);
        CountDownLatch latch = exitLatch;

        if (latch != null) {
            latch.countDown();
            exitLatch = null;
        }
        ConfigTracker.INSTANCE.unloadConfigs(ModConfig.Type.SERVER, getServerConfigPath(server));
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }

    private static void runModifiers(final MinecraftServer server) {
        final RegistryAccess registries = server.registryAccess();

        // The order of holders() is the order modifiers were loaded in.
        final List<BiomeModifier> biomeModifiers = registries.lookupOrThrow(ForgeRegistries.Keys.BIOME_MODIFIERS)
            .listElements()
            .map(Holder::value)
            .toList();
        final List<StructureModifier> structureModifiers = registries.lookupOrThrow(Keys.STRUCTURE_MODIFIERS)
              .listElements()
              .map(Holder::value)
              .toList();

        // Apply sorted biome modifiers to each biome.
        registries.lookupOrThrow(Registries.BIOME).listElements().forEach(biomeHolder ->
            biomeHolder.value().modifiableBiomeInfo().applyBiomeModifiers(biomeHolder, biomeModifiers)
        );
        // Rebuild the indexed feature list
        registries.lookupOrThrow(Registries.LEVEL_STEM).forEach(levelStem -> {
            levelStem.generator().refreshFeaturesPerStep();
        });
        // Apply sorted structure modifiers to each structure.
        registries.lookupOrThrow(Registries.STRUCTURE).listElements().forEach(structureHolder ->
            structureHolder.value().modifiableStructureInfo().applyStructureModifiers(structureHolder, structureModifiers)
        );
    }

    //==================================================================================================================================================================================
    //==================================================================================================================================================================================
    //==================================================================================================================================================================================
    //==================================================================================================================================================================================
    //==================================================================================================================================================================================
    //==================================================================================================================================================================================

    public static void handleServerStarted(final MinecraftServer server) {
        ServerStartedEvent.BUS.post(new ServerStartedEvent(server));
        allowLogins.set(true);
    }

    public static void handleServerStopping(final MinecraftServer server) {
        allowLogins.set(false);
        ServerStoppingEvent.BUS.post(new ServerStoppingEvent(server));
    }

    public static boolean handleServerLogin(final ClientIntentionPacket packet, final Connection connection) {
        var ctx = NetworkContext.get(connection);
        ctx.processIntention(packet.hostName());

        if (!allowLogins.get())
            return rejectConnection(connection, ctx.getType(), "Server is still starting! Please wait before reconnecting.");

        if (packet.intention() == ClientIntent.STATUS)
            return true;

        if (ctx.getType() == ConnectionType.MODDED && ctx.getNetVersion() != NetworkContext.NET_VERSION)
            return rejectConnection(connection, ctx.getType(), "This modded server is not impl compatible with your modded client. Please verify your Forge version closely matches the server. Got net version " + ctx.getNetVersion() + " this server is net version " + NetworkContext.NET_VERSION);

        if (ctx.getType() == ConnectionType.VANILLA && !NetworkRegistry.acceptsVanillaClientConnections())
            return rejectConnection(connection, ctx.getType(), "This server has mods that require Forge to be installed on the client. Contact your server admin for more details.");

        NetworkRegistry.onConnectionStart(connection);
        return true;
    }

    private static boolean rejectConnection(final Connection connection, ConnectionType type, String message) {
        //connection.setClientboundProtocolAfterHandshake(ClientIntent.LOGIN);
        LOGGER.info(SERVERHOOKS, "[{}] Disconnecting {} connection attempt: {}", connection.getLoggableAddress(true), type, message); // TODO: Respect logIP setting
        var text = Component.literal(message);
        connection.send(new ClientboundLoginDisconnectPacket(text));
        connection.disconnect(text);
        return false;
    }
}
