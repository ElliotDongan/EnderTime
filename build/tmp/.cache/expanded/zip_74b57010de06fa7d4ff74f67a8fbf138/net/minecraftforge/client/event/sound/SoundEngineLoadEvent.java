/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.event.sound;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraftforge.common.util.HasResult;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired when the {@link SoundEngine} is constructed or (re)loaded, such as during game initialization or when the sound
 * output device is changed.
 *
 * <p>This event is not {@linkplain Cancellable cancellable}, and does not {@linkplain HasResult have a result}.</p>
 *
 * <p>This event is fired on the {@linkplain FMLJavaModLoadingContext#getModEventBus() mod-specific event bus},
 * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
 */
public final class SoundEngineLoadEvent extends SoundEvent implements IModBusEvent {
    public static EventBus<SoundEngineLoadEvent> getBus(BusGroup modBusGroup) {
        return IModBusEvent.getBus(modBusGroup, SoundEngineLoadEvent.class);
    }

    @ApiStatus.Internal
    public SoundEngineLoadEvent(SoundEngine manager) {
        super(manager);
    }
}
