/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.living;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

/**
 * The ShieldBlockEvent is fired when an entity successfully blocks with a shield.<br>
 * Cancelling this event will have the same impact as if the shield was not eligible to block.<br>
 * The damage blocked cannot be set lower than zero or greater than the original value.<br>
 * Note: The shield item stack "should" be available from {@link LivingEntity#getUseItem()}
 * at least for players.
 */
public final class ShieldBlockEvent extends LivingEvent implements Cancellable {
    public static final CancellableEventBus<ShieldBlockEvent> BUS = CancellableEventBus.create(ShieldBlockEvent.class);

    private final DamageSource source;
    private final float originalBlocked;
    private float dmgBlocked;
    private boolean shieldTakesDamage = true;
    private final ItemStack blockedWith;

    public ShieldBlockEvent(LivingEntity blocker, DamageSource source, float blocked, ItemStack blockedWith) {
        super(blocker);
        this.source = source;
        this.originalBlocked = blocked;
        this.dmgBlocked = blocked;
        this.blockedWith = blockedWith;
    }

    /** @return The damage source. */
    public DamageSource getDamageSource() {
        return this.source;
    }

    /** @return The original amount of damage blocked, which is the same as the original incoming damage value. */
    public float getOriginalBlockedDamage() {
        return this.originalBlocked;
    }

    /** @return The current amount of damage blocked, as a result of this event. */
    public float getBlockedDamage() {
        return this.dmgBlocked;
    }

    /** @return If the shield item will take durability damage or not. */
    public boolean shieldTakesDamage() {
        return this.shieldTakesDamage;
    }

    /**
     * Sets how much damage is blocked by this action.
     * <p>Note that initially the blocked amount is the entire attack.</p>
     */
    public void setBlockedDamage(float blocked) {
        this.dmgBlocked = Mth.clamp(blocked, 0, this.originalBlocked);
    }

    /** Sets if the shield will take durability damage or not. */
    public void setShieldTakesDamage(boolean damage) {
        this.shieldTakesDamage = damage;
    }

    /** @return The item that was used to block damage. */
    public ItemStack getBlockedWith() {
        return this.blockedWith;
    }
}
