/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.bus.EventBus;

/**
 * This event is fired when all players are asleep and the time should be set to day.<br>
 *
 * setWakeUpTime(wakeUpTime) sets a new time that will be added to the dayTime.<br>
 */
public final class SleepFinishedTimeEvent extends LevelEvent {
    public static final EventBus<SleepFinishedTimeEvent> BUS = EventBus.create(SleepFinishedTimeEvent.class);

    private long newTime;
    private final long minTime;

    public SleepFinishedTimeEvent(ServerLevel level, long newTime, long minTime) {
        super(level);
        this.newTime = newTime;
        this.minTime = minTime;
    }

    /**
     * @return the new time
     */
    public long getNewTime() {
        return newTime;
    }

    /**
     * Sets the new time which should be set when all players wake up
     * @param newTimeIn The new time at wakeup
     * @return {@code false} if newTimeIn was lower than current time
     */
    public boolean setTimeAddition(long newTimeIn) {
        if (minTime > newTimeIn)
            return false;

        this.newTime = newTimeIn;
        return true;
    }
}
