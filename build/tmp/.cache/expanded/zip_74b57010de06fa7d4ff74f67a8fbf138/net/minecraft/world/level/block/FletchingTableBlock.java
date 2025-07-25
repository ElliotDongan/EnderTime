package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class FletchingTableBlock extends CraftingTableBlock {
    public static final MapCodec<FletchingTableBlock> CODEC = simpleCodec(FletchingTableBlock::new);

    @Override
    public MapCodec<FletchingTableBlock> codec() {
        return CODEC;
    }

    public FletchingTableBlock(BlockBehaviour.Properties p_53499_) {
        super(p_53499_);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_53501_, Level p_53502_, BlockPos p_53503_, Player p_53504_, BlockHitResult p_53506_) {
        return InteractionResult.PASS;
    }
}