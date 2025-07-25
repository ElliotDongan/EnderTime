/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.living;

import java.util.Collection;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

/**
 * LivingDropsEvent is fired when an Entity's death causes dropped items to appear.<br>
 * This event is fired whenever an Entity dies and drops items in
 * {@link LivingEntity#die(DamageSource)}.<br>
 * <br>
 * This event is fired via the {@link ForgeEventFactory#onLivingDrops(LivingEntity, DamageSource, Collection, int, boolean)} .<br>
 * <br>
 * {@link #source} contains the DamageSource that caused the drop to occur.<br>
 * {@link #drops} contains the ArrayList of EntityItems that will be dropped.<br>
 * {@link #lootingLevel} contains the amount of loot that will be dropped.<br>
 * {@link #recentlyHit} determines whether the Entity doing the drop has recently been damaged.<br>
 * <br>
 * This event is {@link Cancelable}.<br>
 * If this event is canceled, the Entity does not drop anything.<br>
 * <br>
 * This event does not have a result. {@link HasResult}<br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 **/
public final class LivingDropsEvent extends LivingEvent implements Cancellable {
    public static final CancellableEventBus<LivingDropsEvent> BUS = CancellableEventBus.create(LivingDropsEvent.class);

    private final DamageSource source;
    private final Collection<ItemEntity> drops;
    private final boolean recentlyHit;

    public LivingDropsEvent(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, boolean recentlyHit) {
        super(entity);
        this.source = source;
        this.drops = drops;
        this.recentlyHit = recentlyHit;
    }

    public DamageSource getSource() {
        return source;
    }

    public Collection<ItemEntity> getDrops() {
        return drops;
    }

    public boolean isRecentlyHit() {
        return recentlyHit;
    }
}
