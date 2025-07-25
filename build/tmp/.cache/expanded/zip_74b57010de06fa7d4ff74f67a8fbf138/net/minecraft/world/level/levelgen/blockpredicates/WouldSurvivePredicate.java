package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class WouldSurvivePredicate implements BlockPredicate {
    public static final MapCodec<WouldSurvivePredicate> CODEC = RecordCodecBuilder.mapCodec(
        p_190577_ -> p_190577_.group(
                Vec3i.offsetCodec(16).optionalFieldOf("offset", Vec3i.ZERO).forGetter(p_190581_ -> p_190581_.offset),
                BlockState.CODEC.fieldOf("state").forGetter(p_190579_ -> p_190579_.state)
            )
            .apply(p_190577_, WouldSurvivePredicate::new)
    );
    private final Vec3i offset;
    private final BlockState state;

    protected WouldSurvivePredicate(Vec3i p_190570_, BlockState p_190571_) {
        this.offset = p_190570_;
        this.state = p_190571_;
    }

    public boolean test(WorldGenLevel p_190574_, BlockPos p_190575_) {
        return this.state.canSurvive(p_190574_, p_190575_.offset(this.offset));
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.WOULD_SURVIVE;
    }
}