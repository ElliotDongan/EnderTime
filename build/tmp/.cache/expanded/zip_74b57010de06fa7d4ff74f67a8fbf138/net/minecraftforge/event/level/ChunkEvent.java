/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.level;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.bus.EventBus;
import org.jetbrains.annotations.ApiStatus;

/**
 * ChunkEvent is fired when an event involving a chunk occurs.<br>
 * If a method utilizes this {@link Event} as its parameter, the method will
 * receive every child event of this class.<br>
 * <br>
 * {@link #chunk} contains the Chunk this event is affecting.<br>
 * <br>
 * All children of this event are fired on the {@link MinecraftForge#EVENT_BUS}.<br>
 **/
public class ChunkEvent extends LevelEvent {
    public static final EventBus<ChunkEvent> BUS = EventBus.create(ChunkEvent.class);

    private final ChunkAccess chunk;

    public ChunkEvent(ChunkAccess chunk) {
        super(chunk.getWorldForge());
        this.chunk = chunk;
    }

    public ChunkEvent(ChunkAccess chunk, LevelAccessor level) {
        super(level);
        this.chunk = chunk;
    }

    public ChunkAccess getChunk() {
        return chunk;
    }

    /**
     * ChunkEvent.Load is fired when vanilla Minecraft attempts to load a Chunk into the level.<br>
     * This event is fired during chunk loading in <br>
     *
     * Chunk.onChunkLoad(). <br>
     * <strong>Note:</strong> This event may be called before the underlying {@link LevelChunk} is promoted to {@link ChunkStatus#FULL}. You will cause chunk loading deadlocks if you don't delay your level interactions.<br>
     * <br>
     * This event is not {@link Cancelable}.<br>
     * <br>
     * This event does not have a result. {@link HasResult} <br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     **/
    public static final class Load extends ChunkEvent {
        public static final EventBus<ChunkEvent.Load> BUS = EventBus.create(ChunkEvent.Load.class);

        private final boolean newChunk;

        @ApiStatus.Internal
        public Load(ChunkAccess chunk, boolean newChunk) {
            super(chunk);
            this.newChunk = newChunk;
        }

        /**
         * Check whether the Chunk is newly generated, and being loaded for the first time.
         *
         * <p>Will only ever return {@code true} on the {@linkplain net.minecraftforge.fml.LogicalSide#SERVER logical server}.</p>
         *
         * @return whether the Chunk is newly generated
         */
        public boolean isNewChunk() {
            return newChunk;
        }
    }

    /**
     * ChunkEvent.Unload is fired when vanilla Minecraft attempts to unload a Chunk from the level.<br>
     * This event is fired during chunk unloading in <br>
     * Chunk.onChunkUnload(). <br>
     * <br>
     * This event is not {@link Cancelable}.<br>
     * <br>
     * This event does not have a result. {@link HasResult} <br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     **/
    public static final class Unload extends ChunkEvent {
        public static final EventBus<ChunkEvent.Unload> BUS = EventBus.create(ChunkEvent.Unload.class);

        public Unload(ChunkAccess chunk) {
            super(chunk);
        }
    }

    /**
     * ChunkEvent.LightingCalculated is fired when MinecraftForge flags that lighting is correct in a chunk.<br>
     * This event is fired during light propagation in ThreadedLevelLightEngine.CompletableFuture(), specifically upon setting
     * the ChunkAccess isLightCorrect to true.<br>
     * <br>
     * The game test for this event is lighting_event_test in net.minecraftforge.debug.chunk<br>
     * <br>
     * This event is not {@link Cancelable}.<br>
     * <br>
     * This event does not have a result. {@link HasResult} <br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     **/
    public static final class LightingCalculated extends ChunkEvent {
        public static final EventBus<ChunkEvent.LightingCalculated> BUS = EventBus.create(ChunkEvent.LightingCalculated.class);

        public LightingCalculated(ChunkAccess chunk) {
            super(chunk);
        }
    }
}
