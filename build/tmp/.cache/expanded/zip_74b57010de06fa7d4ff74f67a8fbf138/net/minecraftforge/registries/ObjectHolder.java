/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ObjectHolder can be used to automatically populate public static final fields with entries
 * from the registry. These values can then be referred within mod code directly.
 * @deprecated Use {@link DeferredRegister} or {@link RegistryObject} instead. Refer to the MDK for more detailed examples.
 * <br>
 * Example usage of alternatives:
 * <pre>{@code
 * // To register something
 * public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
 * public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties()));
 *
 * // To get a RegistryObject that you didn't register yourself:
 * public static final RegistryObject<Block> DIRT = RegistryObject.create(ResourceLocation.withDefaultNamespace("dirt"), ForgeRegistries.BLOCKS);
 * }</pre>
 */
@Deprecated(since = "1.21.4", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ObjectHolder
{
    /**
     * The name of the registry to load registry entries from.
     * This string is parsed as a {@link ResourceLocation} and can contain a namespace.
     *
     * @return the registry name
     */
    String registryName();

    /**
     * Represents a name in the form of a {@link ResourceLocation} which points to a registry object from the registry given by {@link #registryName()}.
     * Must specify the modid if not inside a class annotated with {@link Mod}.
     *
     * @return a name in the form of a {@link ResourceLocation}
     */
    String value();
}
