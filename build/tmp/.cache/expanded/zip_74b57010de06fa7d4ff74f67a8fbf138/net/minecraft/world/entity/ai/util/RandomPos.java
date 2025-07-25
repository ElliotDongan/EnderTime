package net.minecraft.world.entity.ai.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class RandomPos {
    private static final int RANDOM_POS_ATTEMPTS = 10;

    public static BlockPos generateRandomDirection(RandomSource p_217852_, int p_217853_, int p_217854_) {
        int i = p_217852_.nextInt(2 * p_217853_ + 1) - p_217853_;
        int j = p_217852_.nextInt(2 * p_217854_ + 1) - p_217854_;
        int k = p_217852_.nextInt(2 * p_217853_ + 1) - p_217853_;
        return new BlockPos(i, j, k);
    }

    @Nullable
    public static BlockPos generateRandomDirectionWithinRadians(RandomSource p_217856_, int p_217857_, int p_217858_, int p_217859_, double p_217860_, double p_217861_, double p_217862_) {
        double d0 = Mth.atan2(p_217861_, p_217860_) - (float) (Math.PI / 2);
        double d1 = d0 + (2.0F * p_217856_.nextFloat() - 1.0F) * p_217862_;
        double d2 = Math.sqrt(p_217856_.nextDouble()) * Mth.SQRT_OF_TWO * p_217857_;
        double d3 = -d2 * Math.sin(d1);
        double d4 = d2 * Math.cos(d1);
        if (!(Math.abs(d3) > p_217857_) && !(Math.abs(d4) > p_217857_)) {
            int i = p_217856_.nextInt(2 * p_217858_ + 1) - p_217858_ + p_217859_;
            return BlockPos.containing(d3, i, d4);
        } else {
            return null;
        }
    }

    @VisibleForTesting
    public static BlockPos moveUpOutOfSolid(BlockPos p_148546_, int p_148547_, Predicate<BlockPos> p_148548_) {
        if (!p_148548_.test(p_148546_)) {
            return p_148546_;
        } else {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = p_148546_.mutable().move(Direction.UP);

            while (blockpos$mutableblockpos.getY() <= p_148547_ && p_148548_.test(blockpos$mutableblockpos)) {
                blockpos$mutableblockpos.move(Direction.UP);
            }

            return blockpos$mutableblockpos.immutable();
        }
    }

    @VisibleForTesting
    public static BlockPos moveUpToAboveSolid(BlockPos p_26948_, int p_26949_, int p_26950_, Predicate<BlockPos> p_26951_) {
        if (p_26949_ < 0) {
            throw new IllegalArgumentException("aboveSolidAmount was " + p_26949_ + ", expected >= 0");
        } else if (!p_26951_.test(p_26948_)) {
            return p_26948_;
        } else {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = p_26948_.mutable().move(Direction.UP);

            while (blockpos$mutableblockpos.getY() <= p_26950_ && p_26951_.test(blockpos$mutableblockpos)) {
                blockpos$mutableblockpos.move(Direction.UP);
            }

            int i = blockpos$mutableblockpos.getY();

            while (blockpos$mutableblockpos.getY() <= p_26950_ && blockpos$mutableblockpos.getY() - i < p_26949_) {
                blockpos$mutableblockpos.move(Direction.UP);
                if (p_26951_.test(blockpos$mutableblockpos)) {
                    blockpos$mutableblockpos.move(Direction.DOWN);
                    break;
                }
            }

            return blockpos$mutableblockpos.immutable();
        }
    }

    @Nullable
    public static Vec3 generateRandomPos(PathfinderMob p_148543_, Supplier<BlockPos> p_148544_) {
        return generateRandomPos(p_148544_, p_148543_::getWalkTargetValue);
    }

    @Nullable
    public static Vec3 generateRandomPos(Supplier<BlockPos> p_148562_, ToDoubleFunction<BlockPos> p_148563_) {
        double d0 = Double.NEGATIVE_INFINITY;
        BlockPos blockpos = null;

        for (int i = 0; i < 10; i++) {
            BlockPos blockpos1 = p_148562_.get();
            if (blockpos1 != null) {
                double d1 = p_148563_.applyAsDouble(blockpos1);
                if (d1 > d0) {
                    d0 = d1;
                    blockpos = blockpos1;
                }
            }
        }

        return blockpos != null ? Vec3.atBottomCenterOf(blockpos) : null;
    }

    public static BlockPos generateRandomPosTowardDirection(PathfinderMob p_217864_, int p_217865_, RandomSource p_217866_, BlockPos p_217867_) {
        int i = p_217867_.getX();
        int j = p_217867_.getZ();
        if (p_217864_.hasHome() && p_217865_ > 1) {
            BlockPos blockpos = p_217864_.getHomePosition();
            if (p_217864_.getX() > blockpos.getX()) {
                i -= p_217866_.nextInt(p_217865_ / 2);
            } else {
                i += p_217866_.nextInt(p_217865_ / 2);
            }

            if (p_217864_.getZ() > blockpos.getZ()) {
                j -= p_217866_.nextInt(p_217865_ / 2);
            } else {
                j += p_217866_.nextInt(p_217865_ / 2);
            }
        }

        return BlockPos.containing(i + p_217864_.getX(), p_217867_.getY() + p_217864_.getY(), j + p_217864_.getZ());
    }
}