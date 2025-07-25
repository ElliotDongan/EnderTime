/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model.generators.loaders;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.CustomLoaderBuilder;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.*;

/**
 * In 1.21.4 Mojang exposed their data generators for their models. So it should be feasible to just use theirs.
 * If you find something lacking feel free to open a PR so that we can extend it.
 * @deprecated Use Vanilla's providers {@link net.minecraft.client.data.models.ModelProvider}
 */
public class CompositeModelBuilder<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T>
{
    private static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath("forge", "composite");

    public static <T extends ModelBuilder<T>> CompositeModelBuilder<T> begin(T parent, ExistingFileHelper existingFileHelper)
    {
        return new CompositeModelBuilder<>(parent, existingFileHelper);
    }

    private final Map<String, T> childModels = new LinkedHashMap<>();
    private final List<String> itemRenderOrder = new ArrayList<>();

    protected CompositeModelBuilder(T parent, ExistingFileHelper existingFileHelper)
    {
        super(NAME, parent, existingFileHelper);
    }

    public CompositeModelBuilder<T> child(String name, T modelBuilder)
    {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(modelBuilder, "modelBuilder must not be null");
        childModels.put(name, modelBuilder);
        itemRenderOrder.add(name);
        return this;
    }

    public CompositeModelBuilder<T> itemRenderOrder(String... names)
    {
        Preconditions.checkNotNull(names, "names must not be null");
        Preconditions.checkArgument(names.length > 0, "names must contain at least one element");
        for (String name : names)
            if (!childModels.containsKey(name))
                throw new IllegalArgumentException("names contains \"" + name + "\", which is not a child of this model");
        itemRenderOrder.clear();
        itemRenderOrder.addAll(Arrays.asList(names));
        return this;
    }

    @Override
    public JsonObject toJson(JsonObject json)
    {
        json = super.toJson(json);

        JsonObject children = new JsonObject();
        for(Map.Entry<String, T> entry : childModels.entrySet())
        {
            children.add(entry.getKey(), entry.getValue().toJson());
        }
        json.add("children", children);

        JsonArray itemRenderOrder = new JsonArray();
        for (String name : this.itemRenderOrder) {
            itemRenderOrder.add(name);
        }
        json.add("item_render_order", itemRenderOrder);

        return json;
    }
}
