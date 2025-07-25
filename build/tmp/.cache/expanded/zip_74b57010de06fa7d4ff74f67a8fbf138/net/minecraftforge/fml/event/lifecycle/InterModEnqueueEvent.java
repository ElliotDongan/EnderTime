/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.event.lifecycle;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.event.IModBusEvent;

/**
 * This is the third of four commonly called events during mod core startup.
 *
 * Called before {@link InterModProcessEvent}
 * Called after {@link FMLClientSetupEvent} or {@link FMLDedicatedServerSetupEvent}
 *
 *
 * Enqueue {@link net.minecraftforge.fml.InterModComms} messages to other mods with this event.
 *
 * This is a parallel dispatch event.
 */
public final class InterModEnqueueEvent extends ParallelDispatchEvent {
    public static EventBus<InterModEnqueueEvent> getBus(BusGroup modBusGroup) {
        return IModBusEvent.getBus(modBusGroup, InterModEnqueueEvent.class);
    }

    public InterModEnqueueEvent(final ModContainer container, final ModLoadingStage stage) {
        super(container, stage);
    }
}
