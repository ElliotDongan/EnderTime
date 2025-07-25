/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterNamedRenderTypesEvent;
import net.minecraftforge.fml.ModLoader;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for named {@link RenderType render types}.
 * <p>
 * Provides a lookup.
 */
public final class NamedRenderTypeManager {
    private static ImmutableMap<ResourceLocation, RenderTypeGroup> RENDER_TYPES;

    /**
     * Finds the {@link RenderTypeGroup} for a given name, or the {@link RenderTypeGroup#EMPTY empty group} if not found.
     */
    public static RenderTypeGroup get(ResourceLocation name) {
        return RENDER_TYPES.getOrDefault(name, RenderTypeGroup.EMPTY);
    }

    @ApiStatus.Internal
    public static void init() {
        var renderTypes = new HashMap<ResourceLocation, RenderTypeGroup>();
        preRegisterVanillaRenderTypes(renderTypes);
        var event = new RegisterNamedRenderTypesEvent(renderTypes);
        ModLoader.get().postEventWrapContainerInModOrder(event);
        RENDER_TYPES = ImmutableMap.copyOf(renderTypes);
    }

    /**
     * Pre-registers vanilla render types.
     */
    private static void preRegisterVanillaRenderTypes(Map<ResourceLocation, RenderTypeGroup> blockRenderTypes) {
        blockRenderTypes.put(rl("solid"), new RenderTypeGroup(ChunkSectionLayer.SOLID, ForgeRenderTypes.ITEM_LAYERED_SOLID.get()));
        blockRenderTypes.put(rl("cutout"), new RenderTypeGroup(ChunkSectionLayer.CUTOUT, ForgeRenderTypes.ITEM_LAYERED_CUTOUT.get()));
        // Generally entity/item rendering shouldn't use mipmaps, so cutout_mipped has them off by default. To enforce them, use cutout_mipped_all.
        blockRenderTypes.put(rl("cutout_mipped"), new RenderTypeGroup(ChunkSectionLayer.CUTOUT_MIPPED, ForgeRenderTypes.ITEM_LAYERED_CUTOUT.get()));
        blockRenderTypes.put(rl("cutout_mipped_all"), new RenderTypeGroup(ChunkSectionLayer.CUTOUT_MIPPED, ForgeRenderTypes.ITEM_LAYERED_CUTOUT_MIPPED.get()));
        blockRenderTypes.put(rl("translucent_moving_block"), new RenderTypeGroup(ChunkSectionLayer.TRANSLUCENT, ForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get()));
        blockRenderTypes.put(rl("tripwire"), new RenderTypeGroup(ChunkSectionLayer.TRIPWIRE, ForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get()));
    }

    private static ResourceLocation rl(String paht) {
        return ResourceLocation.withDefaultNamespace(paht);
    }

    private NamedRenderTypeManager() { }
}
