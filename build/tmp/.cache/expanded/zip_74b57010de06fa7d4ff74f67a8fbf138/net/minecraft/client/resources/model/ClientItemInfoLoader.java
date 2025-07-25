package net.minecraft.client.resources.model;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.PlaceholderLookupProvider;
import net.minecraft.util.StrictJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientItemInfoLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = FileToIdConverter.json("items");

    public static CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> scheduleLoad(ResourceManager p_377664_, Executor p_378750_) {
        RegistryAccess.Frozen registryaccess$frozen = ClientRegistryLayer.createRegistryAccess().compositeAccess();
        return CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(() -> LISTER.listMatchingResources(p_377664_), p_378750_)
            .thenCompose(
                p_389580_ -> {
                    List<CompletableFuture<ClientItemInfoLoader.PendingLoad>> list = new ArrayList<>(p_389580_.size());
                    p_389580_.forEach(
                        (p_389584_, p_389585_) -> list.add(
                            CompletableFuture.supplyAsync(
                                () -> {
                                    ResourceLocation resourcelocation = LISTER.fileToId(p_389584_);

                                    try {
                                        ClientItemInfoLoader.PendingLoad clientiteminfoloader$pendingload;
                                        try (Reader reader = p_389585_.openAsReader()) {
                                            PlaceholderLookupProvider placeholderlookupprovider = new PlaceholderLookupProvider(registryaccess$frozen);
                                            DynamicOps<JsonElement> dynamicops = placeholderlookupprovider.createSerializationContext(JsonOps.INSTANCE);
                                            ClientItem clientitem = ClientItem.CODEC
                                                .parse(dynamicops, StrictJsonParser.parse(reader))
                                                .ifError(
                                                    p_376861_ -> LOGGER.error(
                                                        "Couldn't parse item model '{}' from pack '{}': {}",
                                                        resourcelocation,
                                                        p_389585_.sourcePackId(),
                                                        p_376861_.message()
                                                    )
                                                )
                                                .result()
                                                .map(
                                                    p_389587_ -> placeholderlookupprovider.hasRegisteredPlaceholders()
                                                        ? p_389587_.withRegistrySwapper(placeholderlookupprovider.createSwapper())
                                                        : p_389587_
                                                )
                                                .orElse(null);
                                            clientiteminfoloader$pendingload = new ClientItemInfoLoader.PendingLoad(resourcelocation, clientitem);
                                        }

                                        return clientiteminfoloader$pendingload;
                                    } catch (Exception exception) {
                                        LOGGER.error("Failed to open item model {} from pack '{}'", p_389584_, p_389585_.sourcePackId(), exception);
                                        return new ClientItemInfoLoader.PendingLoad(resourcelocation, null);
                                    }
                                },
                                p_378750_
                            )
                        )
                    );
                    return Util.sequence(list).thenApply(p_376092_ -> {
                        Map<ResourceLocation, ClientItem> map = new HashMap<>();

                        for (ClientItemInfoLoader.PendingLoad clientiteminfoloader$pendingload : p_376092_) {
                            if (clientiteminfoloader$pendingload.clientItemInfo != null) {
                                map.put(clientiteminfoloader$pendingload.id, clientiteminfoloader$pendingload.clientItemInfo);
                            }
                        }

                        return new ClientItemInfoLoader.LoadedClientInfos(map);
                    });
                }
            );
    }

    @OnlyIn(Dist.CLIENT)
    public record LoadedClientInfos(Map<ResourceLocation, ClientItem> contents) {
    }

    @OnlyIn(Dist.CLIENT)
    record PendingLoad(ResourceLocation id, @Nullable ClientItem clientItemInfo) {
    }
}