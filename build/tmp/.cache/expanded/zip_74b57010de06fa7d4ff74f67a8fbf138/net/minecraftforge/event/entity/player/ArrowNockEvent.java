/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.bus.EventBus;
import org.jetbrains.annotations.NotNull;

/**
 * ArrowNockEvent is fired when a player begins using a bow.<br>
 * This event is fired whenever a player begins using a bow in
 * {@link BowItem#use(Level, Player, InteractionHand)}.<br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 **/
public final class ArrowNockEvent extends PlayerEvent {
    public static final EventBus<ArrowNockEvent> BUS = EventBus.create(ArrowNockEvent.class);

    private final ItemStack bow;
    private final InteractionHand hand;
    private final Level level;
    private final boolean hasAmmo;
    private InteractionResult action;

    public ArrowNockEvent(Player player, @NotNull ItemStack item, InteractionHand hand, Level level, boolean hasAmmo) {
        super(player);
        this.bow = item;
        this.hand = hand;
        this.level = level;
        this.hasAmmo = hasAmmo;
    }

    @NotNull
    public ItemStack getBow() { return this.bow; }
    public Level getLevel() { return this.level; }
    public InteractionHand getHand() { return this.hand; }
    public boolean hasAmmo() { return this.hasAmmo; }
    public InteractionResult getAction() {
        return this.action;
    }

    public void setAction(InteractionResult action) {
        this.action = action;
    }
}
