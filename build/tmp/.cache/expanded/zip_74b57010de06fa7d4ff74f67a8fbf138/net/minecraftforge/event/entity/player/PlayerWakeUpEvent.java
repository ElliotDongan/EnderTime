/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.bus.EventBus;

/**
 * This event is fired when the player is waking up.<br/>
 * This is merely for purposes of listening for this to happen.<br/>
 * There is nothing that can be manipulated with this event.
 */
public final class PlayerWakeUpEvent extends PlayerEvent {
    public static final EventBus<PlayerWakeUpEvent> BUS = EventBus.create(PlayerWakeUpEvent.class);

    private final boolean wakeImmediately;
    private final boolean updateLevel;

    public PlayerWakeUpEvent(Player player, boolean wakeImmediately, boolean updateLevel) {
        super(player);
        this.wakeImmediately = wakeImmediately;
        this.updateLevel = updateLevel;
    }

    /**
     * Used for the 'wake up animation'.
     * This is false if the player is considered 'sleepy' and the overlay should slowly fade away.
     */
    public boolean wakeImmediately() { return wakeImmediately; }

    /**
     * Indicates if the server should be notified of sleeping changes.
     * This will only be false if the server is considered 'up to date' already, because, for example, it initiated the call.
     */
    public boolean updateLevel() { return updateLevel; }
}
