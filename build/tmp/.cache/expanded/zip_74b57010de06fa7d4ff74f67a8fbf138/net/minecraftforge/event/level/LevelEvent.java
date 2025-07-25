/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * This event is fired whenever an event involving a {@link LevelAccessor} occurs.
 * <p>
 * All children of this event are fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus}.
 */
public class LevelEvent extends MutableEvent implements InheritableEvent {
    public static final EventBus<LevelEvent> BUS = EventBus.create(LevelEvent.class);

    private final LevelAccessor level;

    public LevelEvent(LevelAccessor level) {
        this.level = level;
    }

    /**
     * {@return the level this event is affecting}
     */
    public LevelAccessor getLevel() {
        return level;
    }

    /**
     * This event is fired whenever a level loads.
     * This event is fired whenever a level loads in ClientLevel's constructor and
     * {@literal MinecraftServer#createLevels(ChunkProgressListener)}.
     * <p>
     * This event is not {@linkplain Cancelable cancellable} and does not {@linkplain HasResult have a result}.
     * <p>
     * This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus}
     * on both logical sides.
     **/
    public static final class Load extends LevelEvent {
        public static final EventBus<Load> BUS = EventBus.create(Load.class);

        public Load(LevelAccessor level) { super(level); }
    }

    /**
     * This event is fired whenever a level unloads.
     * This event is fired whenever a level unloads in
     * {@link Minecraft#setLevel(ClientLevel, ReceivingLevelScreen.Reason)},
     * {@link MinecraftServer#stopServer()},
     * {@link Minecraft#clearClientLevel(Screen)}.
     * <p>
     * This event is not {@linkplain Cancelable cancellable} and does not {@linkplain HasResult have a result}.
     * <p>
     * This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus}
     * on both logical sides.
     **/
    public static final class Unload extends LevelEvent {
        public static final EventBus<Unload> BUS = EventBus.create(Unload.class);

        public Unload(LevelAccessor level) { super(level); }
    }

    /**
     * This event fires whenever a level is saved.
     * This event is fired when a level is saved in
     * {@link ServerLevel#save(ProgressListener, boolean, boolean)}.
     * <p>
     * This event is not {@linkplain Cancelable cancellable} and does not {@linkplain HasResult have a result}.
     * <p>
     * This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus}
     * only on the {@linkplain LogicalSide#SERVER logical server}.
     **/
    public static final class Save extends LevelEvent {
        public static final EventBus<Save> BUS = EventBus.create(Save.class);

        public Save(LevelAccessor level) { super(level); }
    }

    /**
     * This event fires whenever a {@link ServerLevel} is initialized for the first time
     * and a spawn position needs to be chosen.
     * <p>
     * This event is {@linkplain Cancelable cancellable} and does not {@linkplain HasResult have a result}.
     * If the event is canceled, the vanilla logic to choose a spawn position will be skipped.
     * <p>
     * This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus}
     * only on the {@linkplain LogicalSide#SERVER logical server}.
     *
     * @see ServerLevelData#isInitialized()
     */
    public static final class CreateSpawnPosition extends LevelEvent implements Cancellable {
        public static final CancellableEventBus<CreateSpawnPosition> BUS = CancellableEventBus.create(CreateSpawnPosition.class);

        private final ServerLevelData settings;

        public CreateSpawnPosition(LevelAccessor level, ServerLevelData settings) {
            super(level);
            this.settings = settings;
        }

        public ServerLevelData getSettings() {
            return settings;
        }
    }

    /**
     * Fired when building a list of all possible entities that can spawn at the specified location.
     *
     * <p>If an entry is added to the list, it needs to be a globally unique instance.</p>
     *
     * The event is called in {@link net.minecraft.world.level.NaturalSpawner#mobsAt(ServerLevel, StructureManager,
     * ChunkGenerator, MobCategory, BlockPos, Holder)}.</p>
     * 
     * <p>This event is {@linkplain Cancelable cancellable}, and does not {@linkplain HasResult have a result}.
     * Canceling the event will result in an empty list, meaning no entity will be spawned.</p>
     */
    public static final class PotentialSpawns extends LevelEvent implements Cancellable {
        public static final CancellableEventBus<PotentialSpawns> BUS = CancellableEventBus.create(PotentialSpawns.class);

        private final MobCategory mobcategory;
        private final BlockPos pos;
        private final List<Weighted<MobSpawnSettings.SpawnerData>> list;
        private final @UnmodifiableView List<Weighted<MobSpawnSettings.SpawnerData>> view;

        public PotentialSpawns(LevelAccessor level, MobCategory category, BlockPos pos, WeightedList<MobSpawnSettings.SpawnerData> oldList) {
            super(level);
            this.pos = pos;
            this.mobcategory = category;
            if (!oldList.isEmpty())
                this.list = new ArrayList<>(oldList.unwrap());
            else
                this.list = new ArrayList<>();

            this.view = Collections.unmodifiableList(list);
        }

        /** @return the category of the mobs in the spawn list. */
        public MobCategory getMobCategory() {
            return this.mobcategory;
        }

        /** @return the block position where the chosen mob will be spawned. */
        public BlockPos getPos() {
            return this.pos;
        }

        /** @return the list of mobs that can potentially be spawned. */
        public @UnmodifiableView List<Weighted<MobSpawnSettings.SpawnerData>> getSpawnerDataList() {
            return this.view;
        }

        /**
         * Appends a SpawnerData entry to the spawn list.
         *
         * @param data   SpawnerData entry to be appended to the spawn list.
         * @param weight The weight for the data entry to have in the list.
         */
        public void addSpawnerData(MobSpawnSettings.SpawnerData data, int weight) {
            this.list.add(new Weighted<>(data, weight));
        }

        /**
         * Removes all entries of the given SpawnerData from the spawn list.
         *
         * @param data SpawnerData entry to be removed from the spawn list.
         * @return {@code true} if the spawn list contained the specified element.
         */
        public boolean removeSpawnerData(MobSpawnSettings.SpawnerData data) {
            return this.list.removeIf(weighted -> weighted.value().equals(data));
        }
    }
}
