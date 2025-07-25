package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class HalfTransparentBlock extends Block {
    public static final MapCodec<HalfTransparentBlock> CODEC = simpleCodec(HalfTransparentBlock::new);

    @Override
    protected MapCodec<? extends HalfTransparentBlock> codec() {
        return CODEC;
    }

    public HalfTransparentBlock(BlockBehaviour.Properties p_53970_) {
        super(p_53970_);
    }

    @Override
    protected boolean skipRendering(BlockState p_53972_, BlockState p_53973_, Direction p_53974_) {
        return p_53973_.is(this) ? true : super.skipRendering(p_53972_, p_53973_, p_53974_);
    }
}