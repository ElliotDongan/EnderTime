/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.registries.tags.ITagManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Utility class to help with managing registry entries.
 * Maintains a list of all suppliers for entries and registers them during the proper Register event.
 * Suppliers should return NEW instances every time.
 *
 *Example Usage:
 *<pre>{@code
 *   private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
 *   private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
 *
 *   public static final RegistryObject<Block> ROCK_BLOCK = BLOCKS.register("rock", () -> new Block(Block.Properties.create(Material.ROCK)));
 *   public static final RegistryObject<Item> ROCK_ITEM = ITEMS.register("rock", () -> new BlockItem(ROCK_BLOCK.get(), new Item.Properties().group(ItemGroup.MISC)));
 *
 *   public ExampleMod() {
 *       ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
 *       BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
 *   }
 *}</pre>
 *
 * @param <T> The base registry type
 */
public class DeferredRegister<T> {
    /**
     * DeferredRegister factory for forge registries that exist <i>before</i> this DeferredRegister is created.
     * <p>
     * If you have a supplier, <u>do not use this method.</u>
     * Instead, use one of the other factories that takes in a registry key or registry name.
     *
     * @param reg the forge registry to wrap
     * @param modid the namespace for all objects registered to this DeferredRegister
     * @see #create(ResourceKey, String)
     * @see #create(ResourceLocation, String)
     */
    public static <B> DeferredRegister<B> create(IForgeRegistry<B> reg, String modid) {
        return new DeferredRegister<>(reg, modid);
    }

    /**
     * DeferredRegister factory for forge registries that exist <i>after</i> this DeferredRegister is created.
     *
     * @param reg the forge registry to wrap
     * @param modid the namespace for all objects registered to this DeferredRegister
     * @see #create(ResourceKey, String)
     * @see #create(ResourceLocation, String)
     */
    public static <B> DeferredRegister<B> create(RegistryHolder<B> reg, String modid) {
        if (reg instanceof RegistryHolder<B> holder)
            return create(holder.registryKey, modid);
        throw new IllegalArgumentException("Registry argument was not made by DefferredRegister.makeRegistry. Use another create method. This method will be changed to use the hard type in1 1.20.3, but I don't wanna break everything in 1.20.2 so we have this error");
    }

    /**
     * DeferredRegister factory for custom forge registries or {@link BuiltInRegistries vanilla registries}
     * to lookup based on the provided registry key. Supports both registries that already exist or do not exist yet.
     * <p>
     * If the registry is never created, any {@link RegistryObject}s made from this DeferredRegister will throw an exception.
     * To allow the optional existence of a registry without error, use {@link #createOptional(ResourceKey, String)}.
     *
     * @param key the key of the registry to reference. May come from another DeferredRegister through {@link #getRegistryKey()}.
     * @param modid the namespace for all objects registered to this DeferredRegister
     * @see #createOptional(ResourceKey, String)
     * @see #create(IForgeRegistry, String)
     * @see #create(ResourceLocation, String)
     */
    public static <B> DeferredRegister<B> create(ResourceKey<? extends Registry<B>> key, String modid) {
        return new DeferredRegister<>(key, modid, false);
    }

    /**
     * DeferredRegister factory for the optional existence of custom forge registries
     * or {@link BuiltInRegistries vanilla registries} to lookup based on the provided registry key.
     * Supports both registries that already exist or do not exist yet.
     * <p>
     * If the registry is never created, any {@link RegistryObject}s made from this DeferredRegister will never be filled but will not throw an exception.
     *
     * @param key the key of the registry to reference
     * @param modid the namespace for all objects registered to this DeferredRegister
     * @see #create(ResourceKey, String)
     * @see #create(IForgeRegistry, String)
     * @see #create(ResourceLocation, String)
     */
    public static <B> DeferredRegister<B> createOptional(ResourceKey<? extends Registry<B>> key, String modid) {
        return new DeferredRegister<>(key, modid, true);
    }

    /**
     * DeferredRegister factory for custom forge registries or {@link BuiltInRegistries vanilla registries}
     * to lookup based on the provided registry name. Supports both registries that already exist or do not exist yet.
     * <p>
     * If the registry is never created, any {@link RegistryObject}s made from this DeferredRegister will throw an exception.
     * To allow the optional existence of a registry without error, use {@link #createOptional(ResourceLocation, String)}.
     *
     * @param registryName The name of the registry, should include namespace. May come from another DeferredRegister through {@link #getRegistryName()}.
     * @param modid The namespace for all objects registered to this DeferredRegister
     * @see #createOptional(ResourceLocation, String)
     * @see #create(IForgeRegistry, String)
     * @see #create(ResourceKey, String)
     */
    public static <B> DeferredRegister<B> create(ResourceLocation registryName, String modid) {
        return new DeferredRegister<>(ResourceKey.createRegistryKey(registryName), modid, false);
    }

    /**
     * DeferredRegister factory for the optional existence of custom forge registries
     * or {@link BuiltInRegistries vanilla registries} to lookup based on the provided registry name.
     * Supports both registries that already exist or do not exist yet.
     * <p>
     * If the registry is never created, any {@link RegistryObject}s made from this DeferredRegister will never be filled but will not throw an exception.
     *
     * @param registryName The name of the registry, should include namespace. May come from another DeferredRegister through {@link #getRegistryName()}.
     * @param modid The namespace for all objects registered to this DeferredRegister
     * @see #create(ResourceLocation, String)
     * @see #create(IForgeRegistry, String)
     * @see #create(ResourceKey, String)
     */
    public static <B> DeferredRegister<B> createOptional(ResourceLocation registryName, String modid) {
        return new DeferredRegister<>(ResourceKey.createRegistryKey(registryName), modid, true);
    }

    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String modid;
    private final boolean optionalRegistry;
    private final Map<RegistryObject<T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private final Set<RegistryObject<T>> entriesView = Collections.unmodifiableSet(entries.keySet());

    @Nullable
    private Supplier<RegistryBuilder<?>> registryFactory;
    @Nullable
    private SetMultimap<TagKey<T>, Supplier<T>> optionalTags;
    private boolean seenRegisterEvent = false;

    private DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String modid, boolean optionalRegistry) {
        requireNonNull("registryKey", registryKey);
        this.registryKey = registryKey;
        this.modid = modid;
        this.optionalRegistry = optionalRegistry;
    }

    private DeferredRegister(IForgeRegistry<T> reg, String modid) {
        this(reg.getRegistryKey(), modid, false);
    }

    /**
     * Adds a new supplier to the list of entries to be registered, and returns a RegistryObject that will be populated with the created entry automatically.
     *
     * @param name The new entry's name, it will automatically have the modid prefixed.
     * @param factory A factory for the new entry, it should return a new instance every time it is called.
     * @return A RegistryObject that will be updated with when the entries in the registry change.
     */
    @SuppressWarnings("unchecked")
    public <I extends T> RegistryObject<I> register(final String name, final Supplier<? extends I> factory) {
        if (seenRegisterEvent)
            throw new IllegalStateException("Cannot register new entries to DeferredRegister after RegisterEvent has been fired.");

        requireNonNull("name", name);
        requireNonNull("factory", factory);
        var key = ResourceLocation.fromNamespaceAndPath(modid, name);

        RegistryObject<I> ret;
        if (this.optionalRegistry)
            ret = RegistryObject.createOptional(key, this.registryKey, this.modid);
        else
            ret = RegistryObject.create(key, this.registryKey, this.modid);

        if (entries.putIfAbsent((RegistryObject<T>) ret, factory) != null)
            throw new IllegalArgumentException("Duplicate registration " + name);

        return ret;
    }

    /**
     * Only used for custom registries to fill the forge registry held in this DeferredRegister.
     *
     * Calls {@link RegistryBuilder#setName} automatically.
     *
     * @param sup Supplier of a RegistryBuilder that initializes a {@link IForgeRegistry} during the {@link NewRegistryEvent} event
     * @return A supplier of the {@link IForgeRegistry} created by the builder.
     * Will always return null until after the {@link NewRegistryEvent} event fires.
     */
    public RegistryHolder<T> makeRegistry(final Supplier<RegistryBuilder<T>> sup) {
        return makeRegistry(this.registryKey.location(), sup);
    }

    /**
     * Creates a ResourceKey based on the current modid and provided path as the location and the registry name linked to this DeferredRegister.
     * To control the namespace, use {@link #key(ResourceLocation)}.
     *
     * @see #key(ResourceLocation)
     */
    @NotNull
    public ResourceKey<T> key(@NotNull String path) {
        requireNonNull("path", path);
        return key(ResourceLocation.fromNamespaceAndPath(this.modid, path));
    }

    /**
     * Creates a tag key based on the provided resource location and the registry name linked to this DeferredRegister.
     * To use the current modid as the namespace, use {@link #createTagKey(String)}.
     *
     * @see #key(String)
     */
    @NotNull
    public ResourceKey<T> key(@NotNull ResourceLocation location) {
        requireNonNull("location", location);
        return ResourceKey.create(getRegistryKey(), location);
    }

    /**
     * Creates a tag key based on the current modid and provided path as the location and the registry name linked to this DeferredRegister.
     * To control the namespace, use {@link #createTagKey(ResourceLocation)}.
     *
     * @see #createTagKey(ResourceLocation)
     * @see #createOptionalTagKey(String, Set)
     */
    @NotNull
    public TagKey<T> createTagKey(@NotNull String path) {
        requireNonNull("path", path);
        return createTagKey(ResourceLocation.fromNamespaceAndPath(this.modid, path));
    }

    /**
     * Creates a tag key based on the provided resource location and the registry name linked to this DeferredRegister.
     * To use the current modid as the namespace, use {@link #createTagKey(String)}.
     *
     * @see #createTagKey(String)
     * @see #createOptionalTagKey(ResourceLocation, Set)
     */
    @NotNull
    public TagKey<T> createTagKey(@NotNull ResourceLocation location) {
        requireNonNull("location", location);
        return TagKey.create(this.registryKey, location);
    }

    /**
     * Creates a tag key with the current modid and provided path that will use the set of defaults if the tag is not loaded from any datapacks.
     * Useful on the client side when a server may not provide a specific tag.
     * To control the namespace, use {@link #createOptionalTagKey(ResourceLocation, Set)}.
     *
     * @see #createTagKey(String)
     * @see #createTagKey(ResourceLocation)
     * @see #createOptionalTagKey(ResourceLocation, Set)
     * @see #addOptionalTagDefaults(TagKey, Set)
     */
    @NotNull
    public TagKey<T> createOptionalTagKey(@NotNull String path, @NotNull Set<? extends Supplier<T>> defaults) {
        requireNonNull("path", path);
        return createOptionalTagKey(ResourceLocation.fromNamespaceAndPath(this.modid, path), defaults);
    }

    /**
     * Creates a tag key with the provided location that will use the set of defaults if the tag is not loaded from any datapacks.
     * Useful on the client side when a server may not provide a specific tag.
     * To use the current modid as the namespace, use {@link #createOptionalTagKey(String, Set)}.
     *
     * @see #createTagKey(String)
     * @see #createTagKey(ResourceLocation)
     * @see #createOptionalTagKey(String, Set)
     * @see #addOptionalTagDefaults(TagKey, Set)
     */
    @NotNull
    public TagKey<T> createOptionalTagKey(@NotNull ResourceLocation location, @NotNull Set<? extends Supplier<T>> defaults) {
        TagKey<T> tagKey = createTagKey(location);

        addOptionalTagDefaults(tagKey, defaults);

        return tagKey;
    }

    /**
     * Adds defaults to an existing tag key.
     * The set of defaults will be bound to the tag if the tag is not loaded from any datapacks.
     * Useful on the client side when a server may not provide a specific tag.
     *
     * @see #createOptionalTagKey(String, Set)
     * @see #createOptionalTagKey(ResourceLocation, Set)
     */
    public void addOptionalTagDefaults(@NotNull TagKey<T> name, @NotNull Set<? extends Supplier<T>> defaults) {
        requireNonNull("defaults", defaults);
        if (optionalTags == null)
            optionalTags = Multimaps.newSetMultimap(new IdentityHashMap<>(), HashSet::new);

        optionalTags.putAll(name, defaults);
    }

    /**
     * Adds our event handler to the specified event bus, this MUST be called in order for this class to function.
     * See {@link DeferredRegister the example usage}.
     *
     * @param modBusGroup The mod-specific event bus group, obtained from {@link FMLModContainer#getModBusGroup()} or
     *                    your language provider's equivalent.
     */
    public void register(BusGroup modBusGroup) {
        modBusGroup.register(EventDispatcher.LOOKUP, new EventDispatcher());
    }

    /**
     * @return The unmodifiable view of registered entries. Useful for bulk operations on all values.
     */
    public Collection<RegistryObject<T>> getEntries() {
        return entriesView;
    }

    /**
     * @return The registry key stored in this deferred register. Useful for creating new deferred registers based on an existing one.
     */
    public ResourceKey<? extends Registry<T>> getRegistryKey() {
        return this.registryKey;
    }

    /**
     * @return The registry name stored in this deferred register. Useful for creating new deferred registers based on an existing one.
     */
    @NotNull
    public ResourceLocation getRegistryName() {
        return this.registryKey.location();
    }

    private RegistryHolder<T> makeRegistry(final ResourceLocation registryName, final Supplier<RegistryBuilder<T>> sup) {
        if (registryName == null)
            throw new IllegalStateException("Cannot create a registry without specifying a registry name");
        if (RegistryManager.ACTIVE.getRegistry(registryName) != null || this.registryFactory != null)
            throw new IllegalStateException("Cannot create a registry for a type that already exists");

        this.registryFactory = () -> sup.get().setName(registryName);
        return new RegistryHolder<>(this.registryKey);
    }

    @SuppressWarnings("unchecked")
    private void onFill(IForgeRegistry<?> registry) {
        if (this.optionalTags == null)
            return;

        ITagManager<T> tagManager = (ITagManager<T>) registry.tags();
        if (tagManager == null)
            throw new IllegalStateException("The forge registry " + registry.getRegistryName() + " does not support tags, but optional tags were registered!");

        Multimaps.asMap(this.optionalTags).forEach(tagManager::addOptionalTagDefaults);
    }

    private static <T> T requireNonNull(String name, T value) {
        if (value == null)
            throw new IllegalArgumentException(name + " can not be null");
        return value;
    }

    private final class EventDispatcher {
        private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

        @SubscribeEvent
        public void handleEvent(RegisterEvent event) {
            if (event.getRegistryKey().equals(registryKey)) {
                seenRegisterEvent = true;
                for (var e : entries.entrySet()) {
                    event.register(registryKey, e.getKey().getId(), () -> e.getValue().get());
                    e.getKey().updateReference(event);
                }

                if (registryFactory == null && event.getForgeRegistry() != null)
                    onFill(event.getForgeRegistry());
            }
        }

        @SubscribeEvent
        public void createRegistry(NewRegistryEvent event) {
            if (registryFactory != null)
                event.create(registryFactory.get(), DeferredRegister.this::onFill);
        }
    }

    public static final class RegistryHolder<V> implements Supplier<IForgeRegistry<V>> {
        private final ResourceKey<? extends Registry<V>> registryKey;
        private IForgeRegistry<V> registry = null;

        private RegistryHolder(ResourceKey<? extends Registry<V>> registryKey) {
            this.registryKey = registryKey;
        }

        public ResourceKey<? extends Registry<V>> getKey() {
            return this.registryKey;
        }

        @Override
        public IForgeRegistry<V> get() {
            // Keep looking up the registry until it's not null
            if (this.registry == null)
                this.registry = RegistryManager.ACTIVE.getRegistry(this.registryKey);

            return this.registry;
        }
    }
}
