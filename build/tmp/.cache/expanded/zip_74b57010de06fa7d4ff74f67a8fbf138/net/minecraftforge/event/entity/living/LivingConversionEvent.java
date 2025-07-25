/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

import java.util.function.Consumer;

/**
 * Currently known conversions:
 *   Pig -> Zombie Piglin when struck by lightning
 *   Villager -> Zombie Villager when killed by a zombie
 *   Zombie -> Drowned when under water
 *   Husk -> Zombie when under water
 *   Zombie Villager -> Villager
 *   Hoglin -> Zogline when in overworld
 *   Piglin/Piglin Brute -> Zombie Pigman when in overworld
 *   Villager -> Witch when struck by lightning
 *   Skeleton -> Stray when sitting in snow
 *   Tadpole -> Frog when it grows up
 *   Mushroom Cow -> Cow when sheared
 */
public sealed class LivingConversionEvent extends LivingEvent implements InheritableEvent {
    public static final EventBus<LivingConversionEvent> BUS = EventBus.create(LivingConversionEvent.class);

    public LivingConversionEvent(LivingEntity entity) {
        super(entity);
    }

    /**
     * LivingConversionEvent.Pre is triggered when an entity is trying
     * to replace itself with another entity
     *
     * This event may trigger every tick even if it was cancelled last tick
     * for entities like Zombies and Hoglins. To prevent it, the conversion
     * timer needs to be changed or reset
     *
     * This event is {@link Cancelable}
     * If cancelled, the replacement will not occur
     */
    public static final class Pre extends LivingConversionEvent implements Cancellable {
        public static final CancellableEventBus<Pre> BUS = CancellableEventBus.create(Pre.class);

        private final EntityType<? extends LivingEntity> outcome;
        private final Consumer<Integer> timer;

        public Pre(LivingEntity entity, EntityType<? extends LivingEntity> outcome, Consumer<Integer> timer) {
            super(entity);
            this.outcome = outcome;
            this.timer = timer;
        }

        /**
         * Gets the entity type of the new entity this living entity is
         * converting to
         * @return the entity type of the new entity
         */
        public EntityType<? extends LivingEntity> getOutcome() {
            return outcome;
        }

        /**
         * Sets the conversion timer, by changing this it prevents the
         * event being triggered every tick
         * Do note the timer of some of the entities are increments, but
         * some of them are decrements
         * Not every conversion is applicable for this
         * @param ticks timer ticks
         */
        public void setConversionTimer(int ticks) {
            timer.accept(ticks);
        }
    }

    /**
     * LivingConversionEvent.Post is triggered when an entity is replacing
     * itself with another entity.
     * The old living entity is likely to be removed right after this event.
     */
    public static final class Post extends LivingConversionEvent {
        public static final EventBus<Post> BUS = EventBus.create(Post.class);

        private final LivingEntity outcome;

        public Post(LivingEntity entity, LivingEntity outcome) {
            super(entity);
            this.outcome = outcome;
        }

        /**
         * Gets the finalized new entity (with all data like potion
         * effect and equipments set)
         * @return the finalized new entity
         */
        public LivingEntity getOutcome() {
            return outcome;
        }
    }
}
