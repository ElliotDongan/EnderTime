/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

/**
 * PlayerXpEvent is fired whenever an event involving player experience occurs. <br>
 * If a method utilizes this {@link net.minecraftforge.eventbus.api.Event} as its parameter, the method will
 * receive every child event of this class.<br>
 * <br>
 * All children of this event are fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public sealed class PlayerXpEvent extends PlayerEvent {
    public static final EventBus<PlayerXpEvent> BUS = EventBus.create(PlayerXpEvent.class);

    public PlayerXpEvent(Player player) {
        super(player);
    }

    /**
     * This event is fired after the player collides with an experience orb, but before the player has been given the experience.
     * It can be cancelled, and no further processing will be done.
     */
    public static final class PickupXp extends PlayerXpEvent implements Cancellable {
        public static final CancellableEventBus<PickupXp> BUS = CancellableEventBus.create(PickupXp.class);

        private final ExperienceOrb orb;

        public PickupXp(Player player, ExperienceOrb orb) {
            super(player);
            this.orb = orb;
        }

        public ExperienceOrb getOrb() {
            return orb;
        }
    }

    /**
     * This event is fired when the player's experience changes through the {@link Player#giveExperiencePoints(int)} method.
     * It can be cancelled, and no further processing will be done.
     */
    public static final class XpChange extends PlayerXpEvent implements Cancellable {
        public static final CancellableEventBus<XpChange> BUS = CancellableEventBus.create(XpChange.class);

        private int amount;

        public XpChange(Player player, int amount) {
            super(player);
            this.amount = amount;
        }

        public int getAmount() {
            return this.amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }

    /**
     * This event is fired when the player's experience level changes through the {@link Player#giveExperienceLevels(int)} method.
     * It can be cancelled, and no further processing will be done.
     */
    public static final class LevelChange extends PlayerXpEvent implements Cancellable {
        public static final CancellableEventBus<LevelChange> BUS = CancellableEventBus.create(LevelChange.class);

        private int levels;

        public LevelChange(Player player, int levels) {
            super(player);
            this.levels = levels;
        }

        public int getLevels() {
            return this.levels;
        }

        public void setLevels(int levels) {
            this.levels = levels;
        }
    }
}
