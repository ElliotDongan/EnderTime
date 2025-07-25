/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.event;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;

/**
 * <p>Fired after the player's movement inputs are updated.</p>
 *
 * <p>This event is not {@linkplain Cancelable cancellable}, and does not {@linkplain HasResult have a result}.</p>
 *
 * <p>This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus},
 * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
 */
public final class MovementInputUpdateEvent extends PlayerEvent {
    public static final EventBus<MovementInputUpdateEvent> BUS = EventBus.create(MovementInputUpdateEvent.class);

    private final ClientInput input;

    @ApiStatus.Internal
    public MovementInputUpdateEvent(Player player, ClientInput input) {
        super(player);
        this.input = input;
    }

    /**
     * {@return the player's movement inputs}
     */
    public ClientInput getInput() {
        return input;
    }
}
