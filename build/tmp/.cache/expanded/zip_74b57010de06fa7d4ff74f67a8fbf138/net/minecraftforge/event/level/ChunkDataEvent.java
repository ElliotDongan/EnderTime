/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.bus.EventBus;

/**
 * ChunkDataEvent is fired when an event involving chunk data occurs.<br>
 * If a method utilizes this {@link Event} as its parameter, the method will
 * receive every child event of this class.<br>
 * <br>
 * {@link #data} contains the NBTTagCompound containing the chunk data for this event.<br>
 * <br>
 * All children of this event are fired on the {@link MinecraftForge#EVENT_BUS}.<br>
 **/
public sealed class ChunkDataEvent extends ChunkEvent {
    public static final EventBus<ChunkDataEvent> BUS = EventBus.create(ChunkDataEvent.class);

    private final SerializableChunkData data;

    public ChunkDataEvent(ChunkAccess chunk, SerializableChunkData data) {
        super(chunk);
        this.data = data;
    }

    public ChunkDataEvent(ChunkAccess chunk, LevelAccessor world, SerializableChunkData data) {
        super(chunk, world);
        this.data = data;
    }

    public SerializableChunkData getData() {
        return data;
    }

    /**
     * ChunkDataEvent.Load is fired when vanilla Minecraft attempts to load Chunk data.<br>
     * This event is fired during chunk loading in
     * {@link ChunkSerializer#read(ServerLevel, PoiManager, ChunkPos, SerializableChunkData)} which means it is async, so be careful.<br>
     **/
    public static final class Load extends ChunkDataEvent {
        public static final EventBus<ChunkDataEvent.Load> BUS = EventBus.create(ChunkDataEvent.Load.class);

        private final ChunkType status;

        public Load(ChunkAccess chunk, SerializableChunkData data, ChunkType status) {
            super(chunk, data);
            this.status = status;
        }

        public ChunkType getStatus() {
            return this.status;
        }
    }

    /**
     * ChunkDataEvent.Save is fired when vanilla Minecraft attempts to save Chunk data.<br>
     * This event is fired during chunk saving in
     * {@code ChunkMap#save(ChunkAccess)}. <br>
     **/
    public static final class Save extends ChunkDataEvent {
        public static final EventBus<ChunkDataEvent.Save> BUS = EventBus.create(ChunkDataEvent.Save.class);

        public Save(ChunkAccess chunk, LevelAccessor world, SerializableChunkData data) {
            super(chunk, world, data);
        }
    }
}
