/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.HasResult;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.eventbus.api.bus.EventBus;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fired whenever a sapling, fungus, mushroom or azalea grows into a tree.
 * <p>
 * This event is not {@linkplain Cancelable cancellable} but does {@linkplain HasResult have a result}.
 * {@linkplain Result#ALLOW ALLOW} and {@linkplain Result#DEFAULT DEFAULT} will allow the sapling to grow
 * using the features set on the event.
 * {@linkplain Result#DENY DENY} will prevent the sapling from growing.
 * <p>
 * This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus}
 * only on the {@linkplain net.minecraftforge.fml.LogicalSide#SERVER logical server}.
 */
// TODO: Rename to BlockFeatureGrowEvent in 1.20
@Deprecated(forRemoval = true, since = "1.21.1") // Dont remove, rename
public final class SaplingGrowTreeEvent extends LevelEvent implements HasResult {
    public static final EventBus<SaplingGrowTreeEvent> BUS = EventBus.create(SaplingGrowTreeEvent.class);

    private final RandomSource randomSource;
    private final BlockPos pos;
    @Nullable
    private Holder<ConfiguredFeature<?, ?>> feature;
    private Result result = Result.DEFAULT;

    public SaplingGrowTreeEvent(LevelAccessor level, RandomSource randomSource, BlockPos pos, @Nullable Holder<ConfiguredFeature<?, ?>> feature) {
        super(level);
        this.randomSource = randomSource;
        this.pos = pos;
        this.feature = feature;
    }

    /**
     * {@return the random source which initiated the sapling growth}
     */
    public RandomSource getRandomSource() {
        return this.randomSource;
    }

    /**
     * {@return the coordinates of the sapling attempting to grow}
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * {@return the holder of the feature which will be placed, possibly null}
     */
    @Nullable
    public Holder<ConfiguredFeature<?, ?>> getFeature() {
        return feature;
    }

    /**
     * @param feature a {@linkplain Holder} referencing a tree feature to be placed instead of the current feature.
     */
    public void setFeature(@Nullable Holder<ConfiguredFeature<?, ?>> feature) {
        this.feature = feature;
    }

    /**
     * @param featureKey a {@linkplain ResourceKey} referencing a tree feature to be placed instead of the current feature.
     */
    public void setFeature(ResourceKey<ConfiguredFeature<?, ?>> featureKey) {
        this.feature = this.getLevel().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(featureKey).orElse(null);
    }

    @Override
    public Result getResult() {
        return result;
    }

    @Override
    public void setResult(Result result) {
        this.result = result;
    }
}
