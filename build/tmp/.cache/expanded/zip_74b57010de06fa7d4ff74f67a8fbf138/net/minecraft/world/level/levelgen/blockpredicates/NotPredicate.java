package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

class NotPredicate implements BlockPredicate {
    public static final MapCodec<NotPredicate> CODEC = RecordCodecBuilder.mapCodec(
        p_190515_ -> p_190515_.group(BlockPredicate.CODEC.fieldOf("predicate").forGetter(p_190517_ -> p_190517_.predicate))
            .apply(p_190515_, NotPredicate::new)
    );
    private final BlockPredicate predicate;

    public NotPredicate(BlockPredicate p_190509_) {
        this.predicate = p_190509_;
    }

    public boolean test(WorldGenLevel p_190512_, BlockPos p_190513_) {
        return !this.predicate.test(p_190512_, p_190513_);
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.NOT;
    }
}