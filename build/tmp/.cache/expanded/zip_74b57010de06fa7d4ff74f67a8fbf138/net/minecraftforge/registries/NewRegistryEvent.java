/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.fml.event.IModBusEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Register new registries when you receive this event through {@link RegistryBuilder} and {@link #create(RegistryBuilder)}.
 */
public final class NewRegistryEvent implements IModBusEvent {
    public static EventBus<NewRegistryEvent> getBus(BusGroup modBusGroup) {
        return IModBusEvent.getBus(modBusGroup, NewRegistryEvent.class);
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<RegistryData<?>> registries = new ArrayList<>();

    public NewRegistryEvent() {}

    /**
     * Adds a registry builder to be created.
     *
     * @param builder The builder to turn into a {@link IForgeRegistry}
     * @return A supplier of the {@link IForgeRegistry} created by the builder. Resolving too early will return null.
     */
    public <V> Supplier<IForgeRegistry<V>> create(RegistryBuilder<V> builder) {
        return create(builder, null);
    }

    /**
     * Adds a registry builder to be created.
     *
     * @param builder The builder to turn into a {@link IForgeRegistry}
     * @param onFill  Called when the returned supplier is filled with the registry
     * @return a supplier of the {@link IForgeRegistry} created by the builder. Resolving too early will return null.
     */
    public <V> Supplier<IForgeRegistry<V>> create(RegistryBuilder<V> builder, @Nullable Consumer<IForgeRegistry<V>> onFill) {
        RegistryHolder<V> registryHolder = new RegistryHolder<>();

        registries.add(new RegistryData<>(builder, registryHolder, onFill));

        return registryHolder;
    }

    @SuppressWarnings("deprecation")
    void fill() {
        RuntimeException aggregate = new RuntimeException();
        Map<RegistryBuilder<?>, IForgeRegistry<?>> builtRegistries = new IdentityHashMap<>();

        if (BuiltInRegistries.REGISTRY instanceof MappedRegistry<?> rootRegistry)
            rootRegistry.unfreeze();

        for (RegistryData<?> data : this.registries) {
            try {
                buildRegistry(builtRegistries, data);
            } catch (Throwable t) {
                aggregate.addSuppressed(t);
                return;
            }
        }

        if (BuiltInRegistries.REGISTRY instanceof MappedRegistry<?> rootRegistry)
            rootRegistry.freeze();

        if (aggregate.getSuppressed().length > 0)
            LOGGER.error(LogUtils.FATAL_MARKER, "Failed to create some forge registries, see suppressed exceptions for details", aggregate);
    }

    private <T> void buildRegistry(Map<RegistryBuilder<?>, IForgeRegistry<?>> builtRegistries, RegistryData<T> data) {
        RegistryBuilder<T> builder = data.builder;
        IForgeRegistry<T> registry = builder.create();

        builtRegistries.put(builder, registry);

        if (builder.getHasWrapper() && !BuiltInRegistries.REGISTRY.containsKey(registry.getRegistryName()))
            RegistryManager.registerToRootRegistry((ForgeRegistry<?>) registry);

        data.registryHolder.registry = registry;
        if (data.onFill != null)
            data.onFill.accept(registry);
    }

    private record RegistryData<V>(
            RegistryBuilder<V> builder,
            RegistryHolder<V> registryHolder,
            Consumer<IForgeRegistry<V>> onFill
    ) {}

    private static final class RegistryHolder<V> implements Supplier<IForgeRegistry<V>> {
        IForgeRegistry<V> registry = null;

        @Override
        public IForgeRegistry<V> get() {
            return this.registry;
        }
    }

    @Override
    public String toString() {
        return "RegistryEvent.NewRegistry";
    }
}
