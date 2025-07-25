/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.server;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.bus.EventBus;

/**
 * Called when the server begins an orderly shutdown, before {@link ServerStoppedEvent}.
 *
 * @author cpw
 */
public final class ServerStoppingEvent extends ServerLifecycleEvent {
    public static final EventBus<ServerStoppingEvent> BUS = EventBus.create(ServerStoppingEvent.class);

    public ServerStoppingEvent(MinecraftServer server) {
        super(server);
    }
}
