/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.brewing;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.bus.EventBus;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player picks up a potion from a brewing stand.
 */
public final class PlayerBrewedPotionEvent extends PlayerEvent {
    public static final EventBus<PlayerBrewedPotionEvent> BUS = EventBus.create(PlayerBrewedPotionEvent.class);

    private final ItemStack stack;

    public PlayerBrewedPotionEvent(Player player, @NotNull ItemStack stack) {
        super(player);
        this.stack = stack;
    }

    /**
     * The ItemStack of the potion.
     */
    @NotNull
    public ItemStack getStack() {
        return stack;
    }
}
