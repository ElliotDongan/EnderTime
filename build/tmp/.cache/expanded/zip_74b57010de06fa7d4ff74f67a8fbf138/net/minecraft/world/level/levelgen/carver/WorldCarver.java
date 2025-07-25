package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.mutable.MutableBoolean;

public abstract class WorldCarver<C extends CarverConfiguration> {
    public static final WorldCarver<CaveCarverConfiguration> CAVE = register("cave", new CaveWorldCarver(CaveCarverConfiguration.CODEC));
    public static final WorldCarver<CaveCarverConfiguration> NETHER_CAVE = register("nether_cave", new NetherWorldCarver(CaveCarverConfiguration.CODEC));
    public static final WorldCarver<CanyonCarverConfiguration> CANYON = register("canyon", new CanyonWorldCarver(CanyonCarverConfiguration.CODEC));
    protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
    protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
    protected static final FluidState WATER = Fluids.WATER.defaultFluidState();
    protected static final FluidState LAVA = Fluids.LAVA.defaultFluidState();
    protected Set<Fluid> liquids = ImmutableSet.of(Fluids.WATER);
    private final MapCodec<ConfiguredWorldCarver<C>> configuredCodec;

    private static <C extends CarverConfiguration, F extends WorldCarver<C>> F register(String p_65066_, F p_65067_) {
        return Registry.register(BuiltInRegistries.CARVER, p_65066_, p_65067_);
    }

    public WorldCarver(Codec<C> p_159366_) {
        this.configuredCodec = p_159366_.fieldOf("config").xmap(this::configured, ConfiguredWorldCarver::config);
    }

    public ConfiguredWorldCarver<C> configured(C p_65064_) {
        return new ConfiguredWorldCarver<>(this, p_65064_);
    }

    public MapCodec<ConfiguredWorldCarver<C>> configuredCodec() {
        return this.configuredCodec;
    }

    public int getRange() {
        return 4;
    }

    protected boolean carveEllipsoid(
        CarvingContext p_190754_,
        C p_190755_,
        ChunkAccess p_190756_,
        Function<BlockPos, Holder<Biome>> p_190757_,
        Aquifer p_190758_,
        double p_190759_,
        double p_190760_,
        double p_190761_,
        double p_190762_,
        double p_190763_,
        CarvingMask p_190764_,
        WorldCarver.CarveSkipChecker p_190765_
    ) {
        ChunkPos chunkpos = p_190756_.getPos();
        double d0 = chunkpos.getMiddleBlockX();
        double d1 = chunkpos.getMiddleBlockZ();
        double d2 = 16.0 + p_190762_ * 2.0;
        if (!(Math.abs(p_190759_ - d0) > d2) && !(Math.abs(p_190761_ - d1) > d2)) {
            int i = chunkpos.getMinBlockX();
            int j = chunkpos.getMinBlockZ();
            int k = Math.max(Mth.floor(p_190759_ - p_190762_) - i - 1, 0);
            int l = Math.min(Mth.floor(p_190759_ + p_190762_) - i, 15);
            int i1 = Math.max(Mth.floor(p_190760_ - p_190763_) - 1, p_190754_.getMinGenY() + 1);
            int j1 = p_190756_.isUpgrading() ? 0 : 7;
            int k1 = Math.min(Mth.floor(p_190760_ + p_190763_) + 1, p_190754_.getMinGenY() + p_190754_.getGenDepth() - 1 - j1);
            int l1 = Math.max(Mth.floor(p_190761_ - p_190762_) - j - 1, 0);
            int i2 = Math.min(Mth.floor(p_190761_ + p_190762_) - j, 15);
            boolean flag = false;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();

            for (int j2 = k; j2 <= l; j2++) {
                int k2 = chunkpos.getBlockX(j2);
                double d3 = (k2 + 0.5 - p_190759_) / p_190762_;

                for (int l2 = l1; l2 <= i2; l2++) {
                    int i3 = chunkpos.getBlockZ(l2);
                    double d4 = (i3 + 0.5 - p_190761_) / p_190762_;
                    if (!(d3 * d3 + d4 * d4 >= 1.0)) {
                        MutableBoolean mutableboolean = new MutableBoolean(false);

                        for (int j3 = k1; j3 > i1; j3--) {
                            double d5 = (j3 - 0.5 - p_190760_) / p_190763_;
                            if (!p_190765_.shouldSkip(p_190754_, d3, d5, d4, j3) && (!p_190764_.get(j2, j3, l2) || isDebugEnabled(p_190755_))) {
                                p_190764_.set(j2, j3, l2);
                                blockpos$mutableblockpos.set(k2, j3, i3);
                                flag |= this.carveBlock(
                                    p_190754_,
                                    p_190755_,
                                    p_190756_,
                                    p_190757_,
                                    p_190764_,
                                    blockpos$mutableblockpos,
                                    blockpos$mutableblockpos1,
                                    p_190758_,
                                    mutableboolean
                                );
                            }
                        }
                    }
                }
            }

            return flag;
        } else {
            return false;
        }
    }

    protected boolean carveBlock(
        CarvingContext p_190744_,
        C p_190745_,
        ChunkAccess p_190746_,
        Function<BlockPos, Holder<Biome>> p_190747_,
        CarvingMask p_190748_,
        BlockPos.MutableBlockPos p_190749_,
        BlockPos.MutableBlockPos p_190750_,
        Aquifer p_190751_,
        MutableBoolean p_190752_
    ) {
        BlockState blockstate = p_190746_.getBlockState(p_190749_);
        if (blockstate.is(Blocks.GRASS_BLOCK) || blockstate.is(Blocks.MYCELIUM)) {
            p_190752_.setTrue();
        }

        if (!this.canReplaceBlock(p_190745_, blockstate) && !isDebugEnabled(p_190745_)) {
            return false;
        } else {
            BlockState blockstate1 = this.getCarveState(p_190744_, p_190745_, p_190749_, p_190751_);
            if (blockstate1 == null) {
                return false;
            } else {
                p_190746_.setBlockState(p_190749_, blockstate1);
                if (p_190751_.shouldScheduleFluidUpdate() && !blockstate1.getFluidState().isEmpty()) {
                    p_190746_.markPosForPostprocessing(p_190749_);
                }

                if (p_190752_.isTrue()) {
                    p_190750_.setWithOffset(p_190749_, Direction.DOWN);
                    if (p_190746_.getBlockState(p_190750_).is(Blocks.DIRT)) {
                        p_190744_.topMaterial(p_190747_, p_190746_, p_190750_, !blockstate1.getFluidState().isEmpty()).ifPresent(p_391040_ -> {
                            p_190746_.setBlockState(p_190750_, p_391040_);
                            if (!p_391040_.getFluidState().isEmpty()) {
                                p_190746_.markPosForPostprocessing(p_190750_);
                            }
                        });
                    }
                }

                return true;
            }
        }
    }

    @Nullable
    private BlockState getCarveState(CarvingContext p_159419_, C p_159420_, BlockPos p_159421_, Aquifer p_159422_) {
        if (p_159421_.getY() <= p_159420_.lavaLevel.resolveY(p_159419_)) {
            return LAVA.createLegacyBlock();
        } else {
            BlockState blockstate = p_159422_.computeSubstance(
                new DensityFunction.SinglePointContext(p_159421_.getX(), p_159421_.getY(), p_159421_.getZ()), 0.0
            );
            if (blockstate == null) {
                return isDebugEnabled(p_159420_) ? p_159420_.debugSettings.getBarrierState() : null;
            } else {
                return isDebugEnabled(p_159420_) ? getDebugState(p_159420_, blockstate) : blockstate;
            }
        }
    }

    private static BlockState getDebugState(CarverConfiguration p_159382_, BlockState p_159383_) {
        if (p_159383_.is(Blocks.AIR)) {
            return p_159382_.debugSettings.getAirState();
        } else if (p_159383_.is(Blocks.WATER)) {
            BlockState blockstate = p_159382_.debugSettings.getWaterState();
            return blockstate.hasProperty(BlockStateProperties.WATERLOGGED) ? blockstate.setValue(BlockStateProperties.WATERLOGGED, true) : blockstate;
        } else {
            return p_159383_.is(Blocks.LAVA) ? p_159382_.debugSettings.getLavaState() : p_159383_;
        }
    }

    public abstract boolean carve(
        CarvingContext p_224913_,
        C p_224914_,
        ChunkAccess p_224915_,
        Function<BlockPos, Holder<Biome>> p_224916_,
        RandomSource p_224917_,
        Aquifer p_224918_,
        ChunkPos p_224919_,
        CarvingMask p_224920_
    );

    public abstract boolean isStartChunk(C p_224908_, RandomSource p_224909_);

    protected boolean canReplaceBlock(C p_224911_, BlockState p_224912_) {
        return p_224912_.is(p_224911_.replaceable);
    }

    protected static boolean canReach(ChunkPos p_159368_, double p_159369_, double p_159370_, int p_159371_, int p_159372_, float p_159373_) {
        double d0 = p_159368_.getMiddleBlockX();
        double d1 = p_159368_.getMiddleBlockZ();
        double d2 = p_159369_ - d0;
        double d3 = p_159370_ - d1;
        double d4 = p_159372_ - p_159371_;
        double d5 = p_159373_ + 2.0F + 16.0F;
        return d2 * d2 + d3 * d3 - d4 * d4 <= d5 * d5;
    }

    private static boolean isDebugEnabled(CarverConfiguration p_159424_) {
        return p_159424_.debugSettings.isDebugMode();
    }

    public interface CarveSkipChecker {
        boolean shouldSkip(CarvingContext p_159426_, double p_159427_, double p_159428_, double p_159429_, int p_159430_);
    }
}