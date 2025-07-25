/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.event;

import com.google.common.base.Strings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired when the client is about to send a chat message to the server.
 *
 * <p>This event is {@linkplain Cancelable cancellable}, and does not {@linkplain HasResult have a result}.
 * If the event is cancelled, the chat message will not be sent to the server.</p>
 *
 * <p>This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus},
 * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
 **/
public final class ClientChatEvent extends MutableEvent implements Cancellable {
    public static final CancellableEventBus<ClientChatEvent> BUS = CancellableEventBus.create(ClientChatEvent.class);

    private String message;
    private final String originalMessage;

    @ApiStatus.Internal
    public ClientChatEvent(String message) {
        this.setMessage(message);
        this.originalMessage = Strings.nullToEmpty(message);
        this.message = this.originalMessage;
    }

    /**
     * {@return the message that will be sent to the server, if the event is not cancelled. This can be changed by mods}
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Sets the new message to be sent to the server, if the event is not cancelled.
     *
     * @param message the new message to be sent
     */
    public void setMessage(String message) {
        this.message = Strings.nullToEmpty(message);
    }

    /**
     * {@return the original message that was to be sent to the server. This cannot be changed by mods}
     */
    public String getOriginalMessage() {
        return this.originalMessage;
    }
}
