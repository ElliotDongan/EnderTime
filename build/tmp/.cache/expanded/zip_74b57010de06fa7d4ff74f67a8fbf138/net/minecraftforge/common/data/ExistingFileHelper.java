/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.IndexedAssetSource;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources.PathResourcesSupplier;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Enables data providers to check if other data files currently exist. The
 * instance provided in the {@link GatherDataEvent} utilizes the standard
 * resources (via {@link VanillaPackResources}), forge's resources, as well as any
 * extra resource packs passed in via the {@code --existing} argument,
 * or mod resources via the {@code --existing-mod} argument.
 */
public class ExistingFileHelper {

    public interface IResourceType {

        PackType getPackType();

        String getSuffix();

        String getPrefix();
    }

    public static class ResourceType implements IResourceType {

        final PackType packType;
        final String suffix, prefix;
        public ResourceType(PackType type, String suffix, String prefix) {
            this.packType = type;
            this.suffix = suffix;
            this.prefix = prefix;
        }

        @Override
        public PackType getPackType() { return packType; }

        @Override
        public String getSuffix() { return suffix; }

        @Override
        public String getPrefix() { return prefix; }
    }

    private final MultiPackResourceManager clientResources, serverData;
    private final boolean enable;
    private final Multimap<PackType, ResourceLocation> generated = HashMultimap.create();

    /**
     * Create a new helper. This should probably <em>NOT</em> be used by mods, as
     * the instance provided by forge is designed to be a central instance that
     * tracks existence of generated data.
     * <p>
     * Only create a new helper if you intentionally want to ignore the existence of
     * other generated files.
     * @param existingPacks a collection of paths to existing packs
     * @param existingMods a set of mod IDs for existing mods
     * @param enable {@code true} if validation is enabled
     * @param assetIndex the identifier for the asset index, generally Minecraft's current major version
     * @param assetsDir the directory in which to find vanilla assets and indexes
     */
    public ExistingFileHelper(Collection<Path> existingPacks, final Set<String> existingMods, boolean enable, @Nullable final String assetIndex, @Nullable final File assetsDir) {
        List<PackResources> candidateClientResources = new ArrayList<>();
        List<PackResources> candidateServerResources = new ArrayList<>();

        if (assetIndex != null && assetsDir != null && assetsDir.exists())
        {
            candidateClientResources.add(ClientPackSource.createVanillaPackSource(IndexedAssetSource.createIndexFs(assetsDir.toPath(), assetIndex)));
        }
        candidateServerResources.add(ServerPacksSource.createVanillaPackSource());

        var symlinks = new ArrayList<ForbiddenSymlinkInfo>();
        var folder = new FolderRepositorySource.FolderPackDetector(new DirectoryValidator(p  -> true));

        for (Path existing : existingPacks) {
            try {
                var info = new PackLocationInfo(existing.getFileName().toString(), Component.literal("data_gen"), PackSource.DEFAULT, Optional.empty());
                var supplier = folder.detectPackResources(existing, symlinks, false);
                PackResources pack = supplier.openPrimary(info);
                candidateClientResources.add(pack);
                candidateServerResources.add(pack);
            }catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (String existingMod : existingMods) {
            IModFileInfo modFileInfo = ModList.get().getModFileById(existingMod);
            if (modFileInfo != null) {
                var root = modFileInfo.getFile().findResource("/");
                var supplier = new PathResourcesSupplier(root);
                var info = new PackLocationInfo("mod:" + existingMod, Component.literal("data_gen:" + existingMod), PackSource.DEFAULT, Optional.empty());
                var pack = supplier.openPrimary(info);
                candidateClientResources.add(pack);
                candidateServerResources.add(pack);
            }
        }

        this.clientResources = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, candidateClientResources);
        this.serverData = new MultiPackResourceManager(PackType.SERVER_DATA, candidateServerResources);

        this.enable = enable;
    }

    private ResourceManager getManager(PackType packType) {
        return packType == PackType.CLIENT_RESOURCES ? clientResources : serverData;
    }

    private ResourceLocation getLocation(ResourceLocation base, String suffix, String prefix) {
        return base.withPath(p -> prefix + '/' + p + suffix);
    }

    /**
     * Check if a given resource exists in the known resource packs.
     *
     * @param loc      the complete location of the resource, e.g.
     *                 {@code "minecraft:textures/block/stone.png"}
     * @param packType the type of resources to check
     * @return {@code true} if the resource exists in any pack, {@code false}
     *         otherwise
     */
    public boolean exists(ResourceLocation loc, PackType packType) {
        return generated.get(packType).contains(loc) || getManager(packType).getResource(loc).isPresent();
    }

    /**
     * Check if a given resource exists in the known resource packs. This is a
     * convenience method to avoid repeating type/prefix/suffix and instead use the
     * common definitions in {@link ResourceType}, or a custom {@link IResourceType}
     * definition.
     *
     * @param loc  the base location of the resource, e.g.
     *             {@code "minecraft:block/stone"}
     * @param type a {@link IResourceType} describing how to form the path to the
     *             resource
     * @return {@code true} if the resource exists in any pack, {@code false}
     *         otherwise
     */
    public boolean exists(ResourceLocation loc, IResourceType type) {
        return exists(getLocation(loc, type.getSuffix(), type.getPrefix()), type.getPackType());
    }

    /**
     * Check if a given resource exists in the known resource packs.
     *
     * @param loc        the base location of the resource, e.g.
     *                   {@code "minecraft:block/stone"}
     * @param packType   the type of resources to check
     * @param pathSuffix a string to append after the path, e.g. {@code ".json"}
     * @param pathPrefix a string to append before the path, before a slash, e.g.
     *                   {@code "models"}
     * @return {@code true} if the resource exists in any pack, {@code false}
     *         otherwise
     */
    public boolean exists(ResourceLocation loc, PackType packType, String pathSuffix, String pathPrefix) {
        return exists(getLocation(loc, pathSuffix, pathPrefix), packType);
    }

    /**
     * Track the existence of a generated file. This is a convenience method to
     * avoid repeating type/prefix/suffix and instead use the common definitions in
     * {@link ResourceType}, or a custom {@link IResourceType} definition.
     * <p>
     * This should be called by data providers immediately when a new data object is
     * created, i.e. not during
     * {@link DataProvider#run(net.minecraft.data.CachedOutput) run} but instead
     * when the "builder" (or whatever intermediate object) is created, such as a
     * {@link ModelBuilder}.
     * <p>
     * This represents a <em>promise</em> to generate the file later, since other
     * datagen may rely on this file existing.
     *
     * @param loc  the base location of the resource, e.g.
     *             {@code "minecraft:block/stone"}
     * @param type a {@link IResourceType} describing how to form the path to the
     *             resource
     */
    public void trackGenerated(ResourceLocation loc, IResourceType type) {
        this.generated.put(type.getPackType(), getLocation(loc, type.getSuffix(), type.getPrefix()));
    }

    /**
     * Track the existence of a generated file.
     * <p>
     * This should be called by data providers immediately when a new data object is
     * created, i.e. not during
     * {@link DataProvider#run(net.minecraft.data.CachedOutput) run} but instead
     * when the "builder" (or whatever intermediate object) is created, such as a
     * {@link ModelBuilder}.
     * <p>
     * This represents a <em>promise</em> to generate the file later, since other
     * datagen may rely on this file existing.
     *
     * @param loc        the base location of the resource, e.g.
     *                   {@code "minecraft:block/stone"}
     * @param packType   the type of resources to check
     * @param pathSuffix a string to append after the path, e.g. {@code ".json"}
     * @param pathPrefix a string to append before the path, before a slash, e.g.
     *                   {@code "models"}
     */
    public void trackGenerated(ResourceLocation loc, PackType packType, String pathSuffix, String pathPrefix) {
        this.generated.put(packType, getLocation(loc, pathSuffix, pathPrefix));
    }

    @VisibleForTesting
    public Resource getResource(ResourceLocation loc, PackType packType, String pathSuffix, String pathPrefix) throws FileNotFoundException {
        return getResource(getLocation(loc, pathSuffix, pathPrefix), packType);
    }

    @VisibleForTesting
    public Resource getResource(ResourceLocation loc, PackType packType) throws FileNotFoundException {
        return getManager(packType).getResourceOrThrow(loc);
    }

    @VisibleForTesting
    public List<Resource> getResourceStack(ResourceLocation loc, PackType packType) {
        return getManager(packType).getResourceStack(loc);
    }

    /**
     * @return {@code true} if validation is enabled, {@code false} otherwise
     * @deprecated  Validation is always enabled, check things as you deem fit.
     */
    @Deprecated(forRemoval = true, since = "1.21.5")
    public boolean isEnabled() {
        return enable;
    }
}
