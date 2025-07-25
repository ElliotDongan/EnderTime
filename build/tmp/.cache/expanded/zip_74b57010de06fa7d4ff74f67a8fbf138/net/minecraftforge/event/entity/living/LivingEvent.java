/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import org.jetbrains.annotations.Nullable;

/**
 * LivingEvent is fired whenever an event involving a {@link LivingEntity} occurs.<br>
 * If a method utilizes this {@link Event} as its parameter, the method will
 * receive every child event of this class.<br>
 * <br>
 * All children of this event are fired on the {@link MinecraftForge#EVENT_BUS}.<br>
 **/
public class LivingEvent extends EntityEvent {
    public static final EventBus<LivingEvent> BUS = EventBus.create(LivingEvent.class);

    private final LivingEntity livingEntity;

    public LivingEvent(LivingEntity entity) {
        super(entity);
        livingEntity = entity;
    }

    @Override
    public LivingEntity getEntity() {
        return livingEntity;
    }

    /**
     * LivingUpdateEvent is fired when a LivingEntity is ticked in {@link LivingEntity#tick()}. <br>
     * <br>
     * This event is fired via the {@link ForgeEventFactory#onLivingTick(LivingEntity)}.<br>
     * <br>
     * This event is {@link Cancelable}.<br>
     * If this event is canceled, the Entity does not update.<br>
     * <br>
     * This event does not have a result. {@link HasResult}<br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
     **/
    public static final class LivingTickEvent extends LivingEvent implements Cancellable {
        public static final CancellableEventBus<LivingTickEvent> BUS = CancellableEventBus.create(LivingTickEvent.class);

        public LivingTickEvent(LivingEntity e) { super(e); }
    }

    /**
     * LivingJumpEvent is fired when an Entity jumps.<br>
     * This event is fired whenever an Entity jumps in
     * {@code LivingEntity#jumpFromGround()}, {@code MagmaCube#jumpFromGround()},
     * and {@code Horse#jumpFromGround()}.<br>
     * <br>
     * This event is fired via the {@link ForgeHooks#onLivingJump(LivingEntity)}.<br>
     * <br>
     * This event is not {@link Cancelable}.<br>
     * <br>
     * This event does not have a result. {@link HasResult}<br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
     **/
    public static final class LivingJumpEvent extends LivingEvent {
        public static final EventBus<LivingJumpEvent> BUS = EventBus.create(LivingJumpEvent.class);

        public LivingJumpEvent(LivingEntity e) { super(e); }
    }

    public static final class LivingVisibilityEvent extends LivingEvent {
        public static final EventBus<LivingVisibilityEvent> BUS = EventBus.create(LivingVisibilityEvent.class);

        private double visibilityModifier;
        @Nullable
        private final Entity lookingEntity;

        public LivingVisibilityEvent(LivingEntity livingEntity, @Nullable Entity lookingEntity, double originalMultiplier) {
            super(livingEntity);
            this.visibilityModifier = originalMultiplier;
            this.lookingEntity = lookingEntity;
        }

        /**
         * @param mod Is multiplied with the current modifier
         */
        public void modifyVisibility(double mod) {
            visibilityModifier *= mod;
        }

        /**
         * @return The current modifier
         */
        public double getVisibilityModifier() {
            return visibilityModifier;
        }

        /**
         * @return The entity trying to see this LivingEntity, if available
         */
        @Nullable
        public Entity getLookingEntity() {
            return lookingEntity;
        }
    }
}
