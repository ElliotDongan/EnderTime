package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PistonHeadRenderer implements BlockEntityRenderer<PistonMovingBlockEntity> {
    private BlockRenderDispatcher blockRenderer;

    public PistonHeadRenderer(BlockEntityRendererProvider.Context p_173623_) {
        this.blockRenderer = p_173623_.getBlockRenderDispatcher();
    }

    public void render(
        PistonMovingBlockEntity p_112452_, float p_112453_, PoseStack p_112454_, MultiBufferSource p_112455_, int p_112456_, int p_112457_, Vec3 p_395483_
    ) {
        Level level = p_112452_.getLevel();
        if (level != null) {
            BlockPos blockpos = p_112452_.getBlockPos().relative(p_112452_.getMovementDirection().getOpposite());
            BlockState blockstate = p_112452_.getMovedState();
            if (!blockstate.isAir()) {
                ModelBlockRenderer.enableCaching();
                p_112454_.pushPose();
                p_112454_.translate(p_112452_.getXOff(p_112453_), p_112452_.getYOff(p_112453_), p_112452_.getZOff(p_112453_));
                if (blockstate.is(Blocks.PISTON_HEAD) && p_112452_.getProgress(p_112453_) <= 4.0F) {
                    blockstate = blockstate.setValue(PistonHeadBlock.SHORT, p_112452_.getProgress(p_112453_) <= 0.5F);
                    this.renderBlock(blockpos, blockstate, p_112454_, p_112455_, level, false, p_112457_);
                } else if (p_112452_.isSourcePiston() && !p_112452_.isExtending()) {
                    PistonType pistontype = blockstate.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
                    BlockState blockstate1 = Blocks.PISTON_HEAD
                        .defaultBlockState()
                        .setValue(PistonHeadBlock.TYPE, pistontype)
                        .setValue(PistonHeadBlock.FACING, blockstate.getValue(PistonBaseBlock.FACING));
                    blockstate1 = blockstate1.setValue(PistonHeadBlock.SHORT, p_112452_.getProgress(p_112453_) >= 0.5F);
                    this.renderBlock(blockpos, blockstate1, p_112454_, p_112455_, level, false, p_112457_);
                    BlockPos blockpos1 = blockpos.relative(p_112452_.getMovementDirection());
                    p_112454_.popPose();
                    p_112454_.pushPose();
                    blockstate = blockstate.setValue(PistonBaseBlock.EXTENDED, true);
                    this.renderBlock(blockpos1, blockstate, p_112454_, p_112455_, level, true, p_112457_);
                } else {
                    this.renderBlock(blockpos, blockstate, p_112454_, p_112455_, level, false, p_112457_);
                }

                p_112454_.popPose();
                ModelBlockRenderer.clearCache();
            }
        }
    }

    private void renderBlock(
        BlockPos p_112459_, BlockState p_112460_, PoseStack p_112461_, MultiBufferSource p_112462_, Level p_112463_, boolean p_112464_, int p_112465_
    ) {
        var model = this.blockRenderer.getBlockModel(p_112460_);
        var rand = RandomSource.create(p_112460_.getSeed(p_112459_));
        var data = net.minecraftforge.client.model.data.ModelData.EMPTY;
        for (var rendertype : model.getRenderTypes(p_112460_, rand, data)) {
           rand = RandomSource.create(p_112460_.getSeed(p_112459_)); // Reset rand to keep
           VertexConsumer vertexconsumer = p_112462_.getBuffer(net.minecraftforge.client.RenderTypeHelper.getMovingBlockRenderType(rendertype));
           List<BlockModelPart> list = model.collectParts(rand, data, rendertype);
        this.blockRenderer.getModelRenderer().tesselateBlock(p_112463_, list, p_112460_, p_112459_, p_112461_, vertexconsumer, p_112464_, p_112465_);
        }
    }

    @Override
    public int getViewDistance() {
        return 68;
    }
}
