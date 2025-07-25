/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.RenderTypeGroup;

import java.util.List;

/**
 * Base interface for any object that collects culled and unculled faces and bakes them into a model.
 * <p>
 * Provides a generic base implementation via {@link #of(boolean, boolean, boolean, ItemTransforms, TextureAtlasSprite, RenderTypeGroup, RenderTypeGroup)}
 * and a quad-collecting alternative via {@link #collecting(List)}.
 *
 * @deprecated Use {@link net.minecraft.client.resources.model.QuadCollection} instead.
 */
@Deprecated(forRemoval = true, since = "1.21.5")
public interface IModelBuilder<T extends IModelBuilder<T>> {
    /**
     * Creates a new model builder that uses the provided attributes in the final baked model.
     */
    static IModelBuilder<?> of(
        boolean hasAmbientOcclusion,
        boolean usesBlockLight,
        boolean isGui3d,
        ItemTransforms transforms,
        TextureAtlasSprite particle
    ){
        return of(hasAmbientOcclusion, usesBlockLight, isGui3d, transforms, particle, RenderTypeGroup.EMPTY);
    }

    static IModelBuilder<?> of(
        boolean hasAmbientOcclusion,
        boolean usesBlockLight,
        boolean isGui3d,
        ItemTransforms transforms,
        TextureAtlasSprite particle,
        RenderTypeGroup renderTypes
    ){
        return new Simple(hasAmbientOcclusion, usesBlockLight, isGui3d, transforms, particle, renderTypes);
    }

    static IModelBuilder<?> of(
        boolean hasAmbientOcclusion,
        boolean usesBlockLight,
        boolean isGui3d,
        ItemTransforms transforms,
        TextureAtlasSprite particle,
        RenderTypeGroup renderTypes,
        RenderTypeGroup renderTypesFast
    ){
        return new Simple(hasAmbientOcclusion, usesBlockLight, isGui3d, transforms, particle, renderTypes, renderTypesFast);
    }

    /**
     * Creates a new model builder that collects quads to the provided list, returning
     * {@linkplain EmptyModel#BAKED an empty model} if you call {@link #build()}.
     */
    static IModelBuilder<?> collecting(List<BakedQuad> quads) {
        return new Collecting(quads);
    }

    T addCulledFace(Direction facing, BakedQuad quad);

    T addUnculledFace(BakedQuad quad);

    BlockModelPart build();

    class Simple implements IModelBuilder<Simple> {
        private final QuadCollection.Builder builder;

        private final boolean hasAmbientOcclusion;

        // TODO: [VEN] These are UNUSED, and this is GOING TO BREAK.
        private final boolean usesBlockLight;
        private final boolean isGui3d;
        private final ItemTransforms transforms;
        private final TextureAtlasSprite particle;
        private final RenderTypeGroup renderTypes;
        private final RenderTypeGroup renderTypesFast;

        private Simple(
            boolean hasAmbientOcclusion, boolean usesBlockLight, boolean isGui3d,
            ItemTransforms transforms, TextureAtlasSprite particle, RenderTypeGroup renderTypes
        ) {
            this(hasAmbientOcclusion, usesBlockLight, isGui3d, transforms, particle, renderTypes, RenderTypeGroup.EMPTY);
        }

        private Simple(
            boolean hasAmbientOcclusion, boolean usesBlockLight, boolean isGui3d,
            ItemTransforms transforms, TextureAtlasSprite particle, RenderTypeGroup renderTypes, RenderTypeGroup renderTypesFast
        ) {
            this.builder = new QuadCollection.Builder();
            this.hasAmbientOcclusion = hasAmbientOcclusion;
            this.usesBlockLight = usesBlockLight;
            this.isGui3d = isGui3d;
            this.transforms = transforms;
            this.particle = particle;
            this.renderTypes = renderTypes;
            this.renderTypesFast = renderTypesFast;
        }

        @Override
        public Simple addCulledFace(Direction facing, BakedQuad quad) {
            builder.addCulledFace(facing, quad);
            return this;
        }

        @Override
        public Simple addUnculledFace(BakedQuad quad) {
            builder.addUnculledFace(quad);
            return this;
        }

        @Override
        public BlockModelPart build() {
            var quads = builder.build();
            // TODO: [VEN] Models will NOT be complete here!!!!
            return new SimpleModelWrapper(quads, hasAmbientOcclusion, particle);
        }
    }

    class Collecting implements IModelBuilder<Collecting> {
        private static final BlockModelPart EMPTY = new SimpleModelWrapper(QuadCollection.EMPTY, false, new TextureAtlasSprite(ResourceLocation.fromNamespaceAndPath("forge", "empty"), null, 0, 0, 0, 0) {});
        private final List<BakedQuad> quads;

        private Collecting(List<BakedQuad> quads) {
            this.quads = quads;
        }

        @Override
        public Collecting addCulledFace(Direction facing, BakedQuad quad) {
            quads.add(quad);
            return this;
        }

        @Override
        public Collecting addUnculledFace(BakedQuad quad) {
            quads.add(quad);
            return this;
        }

        @Override
        public BlockModelPart build() {
            return EMPTY;
        }
    }
}
