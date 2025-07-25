/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.server;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.bus.EventBus;

/**
 * Called after {@link ServerStoppingEvent} when the server has completely shut down.
 * Called immediately before shutting down, on the dedicated server, and before returning
 * to the main menu on the client.
 *
 * @author cpw
 */
public final class ServerStoppedEvent extends ServerLifecycleEvent {
    public static final EventBus<ServerStoppedEvent> BUS = EventBus.create(ServerStoppedEvent.class);

    public ServerStoppedEvent(MinecraftServer server) {
        super(server);
    }
}
