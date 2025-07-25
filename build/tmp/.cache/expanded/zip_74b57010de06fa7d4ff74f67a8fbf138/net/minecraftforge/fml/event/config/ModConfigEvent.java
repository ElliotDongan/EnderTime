/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.event.config;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.IModBusEvent;

public sealed class ModConfigEvent implements IModBusEvent, IConfigEvent {
    private final ModConfig config;

    ModConfigEvent(final ModConfig config) {
        this.config = config;
    }

    @Override
    public ModConfig getConfig() {
        return config;
    }

    /**
     * Fired during mod and server loading, depending on {@link ModConfig.Type} of config file.
     * Any Config objects associated with this will be valid and can be queried directly.
     */
    public static final class Loading extends ModConfigEvent {
        public static EventBus<Loading> getBus(BusGroup modBusGroup) {
            return IModBusEvent.getBus(modBusGroup, Loading.class);
        }

        public Loading(final ModConfig config) {
            super(config);
        }
    }

    /**
     * Fired when the configuration is changed. This can be caused by a change to the config
     * from a UI or from editing the file itself. IMPORTANT: this can fire at any time
     * and may not even be on the server or client threads. Ensure you properly synchronize
     * any resultant changes.
     */
    public static final class Reloading extends ModConfigEvent {
        public static EventBus<Reloading> getBus(BusGroup modBusGroup) {
            return IModBusEvent.getBus(modBusGroup, Reloading.class);
        }

        public Reloading(final ModConfig config) {
            super(config);
        }
    }

    /**
     * Fired when a config is unloaded. This only happens when the server closes, which is
     * probably only really relevant on the client, to reset internal mod state when the
     * server goes away, though it will fire on the dedicated server as well.
     * The config file will be saved after this event has fired.
     */
    public static final class Unloading extends ModConfigEvent {
        public static EventBus<Unloading> getBus(BusGroup modBusGroup) {
            return IModBusEvent.getBus(modBusGroup, Unloading.class);
        }

        public Unloading(final ModConfig config) {
            super(config);
        }
    }
}
