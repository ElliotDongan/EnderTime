/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.level;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.fml.LogicalSide;

/**
 * This event is fired whenever a chunk has a watch-related action.
 * <p>
 * The {@linkplain #getPlayer() player}'s level may not be the same as the {@linkplain #getLevel() level of the chunk}
 * when the player is teleporting to another dimension.
 * <p>
 * This event is not {@linkplain Cancelable cancellable} and does not {@linkplain HasResult have a result}.
 * <p>
 * This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus}
 * only on the {@linkplain LogicalSide#SERVER logical server}.
 **/
public sealed class ChunkWatchEvent extends MutableEvent implements InheritableEvent {
    public static final EventBus<ChunkWatchEvent> BUS = EventBus.create(ChunkWatchEvent.class);

    private final ServerLevel level;
    private final ServerPlayer player;
    private final ChunkPos pos;

    public ChunkWatchEvent(ServerPlayer player, ChunkPos pos, ServerLevel level) {
        this.player = player;
        this.pos = pos;
        this.level = level;
    }

    /**
     * {@return the server player involved with the watch action}
     */
    public ServerPlayer getPlayer() {
        return this.player;
    }

    /**
     * {@return the chunk position this watch event is affecting}
     */
    public ChunkPos getPos() {
        return this.pos;
    }

    /**
     * {@return the server level containing the chunk}
     */
    public ServerLevel getLevel() {
        return this.level;
    }

    /**
     * This event is fired when chunk data is sent to the {@link ServerPlayer} (see {@link net.minecraft.server.network.PlayerChunkSender}).
     * <p>
     * This event may be used to send additional chunk-related data to the client.
     * <p>
     * This event is not {@linkplain Cancelable cancellable} and does not {@linkplain HasResult have a result}.
     * <p>
     * This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus}
     * only on the {@linkplain LogicalSide#SERVER logical server}.
     **/
    public static final class Watch extends ChunkWatchEvent {
        public static final EventBus<Watch> BUS = EventBus.create(Watch.class);

        private final LevelChunk chunk;

        public Watch(ServerPlayer player, LevelChunk chunk, ServerLevel level) {
            super(player, chunk.getPos(), level);
            this.chunk = chunk;
        }

        public LevelChunk getChunk() {
            return this.chunk;
        }
    }

    /**
     * This event is fired when server sends "forget chunk" packet to the {@link ServerPlayer}.
     * <p>
     * This event is not {@linkplain Cancelable cancellable} and does not {@linkplain HasResult have a result}.
     * <p>
     * This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus}
     * only on the {@linkplain LogicalSide#SERVER logical server}.
     **/
    public static final class UnWatch extends ChunkWatchEvent {
        public static final EventBus<UnWatch> BUS = EventBus.create(UnWatch.class);

        public UnWatch(ServerPlayer player, ChunkPos pos, ServerLevel level) {
            super(player, pos, level);
        }
    }
}
