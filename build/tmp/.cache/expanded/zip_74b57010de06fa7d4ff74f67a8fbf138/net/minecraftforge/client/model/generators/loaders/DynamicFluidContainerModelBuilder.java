/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model.generators.loaders;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.CustomLoaderBuilder;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * In 1.21.4 Mojang exposed their data generators for their models. So it should be feasible to just use theirs.
 * If you find something lacking feel free to open a PR so that we can extend it.
 * @deprecated Use Vanilla's providers {@link net.minecraft.client.data.models.ModelProvider}
 */
public class DynamicFluidContainerModelBuilder<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T>
{
    private static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath("forge", "fluid_container");
    public static <T extends ModelBuilder<T>> DynamicFluidContainerModelBuilder<T> begin(T parent, ExistingFileHelper existingFileHelper)
    {
        return new DynamicFluidContainerModelBuilder<>(parent, existingFileHelper);
    }

    private ResourceLocation fluid;
    private Boolean flipGas;
    private Boolean applyTint;
    private Boolean coverIsMask;
    private Boolean applyFluidLuminosity;

    protected DynamicFluidContainerModelBuilder(T parent, ExistingFileHelper existingFileHelper)
    {
        super(NAME, parent, existingFileHelper);
    }

    public DynamicFluidContainerModelBuilder<T> fluid(Fluid fluid)
    {
        Preconditions.checkNotNull(fluid, "fluid must not be null");
        this.fluid = ForgeRegistries.FLUIDS.getKey(fluid);
        return this;
    }

    public DynamicFluidContainerModelBuilder<T> flipGas(boolean flip)
    {
        this.flipGas = flip;
        return this;
    }

    public DynamicFluidContainerModelBuilder<T> applyTint(boolean tint)
    {
        this.applyTint = tint;
        return this;
    }

    public DynamicFluidContainerModelBuilder<T> coverIsMask(boolean coverIsMask)
    {
        this.coverIsMask = coverIsMask;
        return this;
    }

    public DynamicFluidContainerModelBuilder<T> applyFluidLuminosity(boolean applyFluidLuminosity)
    {
        this.applyFluidLuminosity = applyFluidLuminosity;
        return this;
    }

    @Override
    public JsonObject toJson(JsonObject json)
    {
        json = super.toJson(json);

        Preconditions.checkNotNull(fluid, "fluid must not be null");

        json.addProperty("fluid", fluid.toString());

        if (flipGas != null)
            json.addProperty("flip_gas", flipGas);

        if (applyTint != null)
            json.addProperty("apply_tint", applyTint);

        if (coverIsMask != null)
            json.addProperty("cover_is_mask", coverIsMask);

        if (applyFluidLuminosity != null)
            json.addProperty("apply_fluid_luminosity", applyFluidLuminosity);

        return json;
    }
}
