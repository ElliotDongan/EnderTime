/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/** Where all the events for Gathering Components will exist */
public abstract sealed class GatherComponentsEvent extends MutableEvent implements InheritableEvent {
    public static final EventBus<GatherComponentsEvent> BUS = EventBus.create(GatherComponentsEvent.class);

    private final DataComponentMap.Builder components = DataComponentMap.builder();
    private final DataComponentMap originalComponents;
    private final Object owner;

    protected GatherComponentsEvent(Object owner, DataComponentMap originalComponents) {
        this.originalComponents = originalComponents;
        this.owner = owner;
    }

    public <T> void register(DataComponentType<T> componentType, @Nullable T value) {
        components.set(componentType, value);
    }

    @ApiStatus.Internal
    public DataComponentMap getDataComponentMap() {
        return components.build();
    }

    public DataComponentMap getOriginalComponentMap() {
        return originalComponents;
    }

    public Object getOwner() {
        return owner;
    }

    /**
     * Used to get additional Components for any {@link net.minecraft.world.item.Item}
     *
     * Fired once for every {@link net.minecraft.world.item.Item} instance, only once, Lazily.
     *
     * Recursion is not supported. Cant call {@link net.minecraft.world.item.Item#components()} as that would cause a loop.
     *
     * References in {@link net.minecraft.world.item.Items} may not be valid at the current time.
     */
    public static final class Item extends GatherComponentsEvent {
        public static final EventBus<Item> BUS = EventBus.create(Item.class);

        public Item(net.minecraft.world.item.Item item, DataComponentMap dataComponents) {
            super(item, dataComponents);
        }

        @Override
        public net.minecraft.world.item.Item getOwner() {
            return (net.minecraft.world.item.Item) super.getOwner();
        }
    }
}
