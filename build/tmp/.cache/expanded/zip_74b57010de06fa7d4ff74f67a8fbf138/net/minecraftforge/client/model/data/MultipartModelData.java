/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model.data;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public class MultipartModelData
{
    public static final ModelProperty<MultipartModelData> PROPERTY = new ModelProperty<>();

    private final Map<BlockModelPart, ModelData> partData;

    private MultipartModelData(Map<BlockModelPart, ModelData> partData)
    {
        this.partData = partData;
    }

    @Nullable
    public ModelData get(BlockModelPart model)
    {
        return partData.get(model);
    }

    /**
     * Helper to get the data from a {@link ModelData} instance.
     *
     * @param modelData The object to get data from
     * @param model     The model to get data for
     * @return The data for the part, or the one passed in if not found
     */
    public static ModelData resolve(ModelData modelData, BlockModelPart model)
    {
        var multipartData = modelData.get(PROPERTY);
        if (multipartData == null)
            return modelData;
        var partData = multipartData.get(model);
        return partData != null ? partData : modelData;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private final Map<BlockModelPart, ModelData> partData = new IdentityHashMap<>();

        public Builder with(BlockModelPart model, ModelData data)
        {
            partData.put(model, data);
            return this;
        }

        public MultipartModelData build()
        {
            return new MultipartModelData(partData);
        }
    }
}
