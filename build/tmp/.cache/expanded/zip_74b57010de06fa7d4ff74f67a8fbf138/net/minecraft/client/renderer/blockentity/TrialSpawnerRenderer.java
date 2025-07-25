package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrialSpawnerRenderer implements BlockEntityRenderer<TrialSpawnerBlockEntity> {
    private final EntityRenderDispatcher entityRenderer;

    public TrialSpawnerRenderer(BlockEntityRendererProvider.Context p_311333_) {
        this.entityRenderer = p_311333_.getEntityRenderer();
    }

    public void render(
        TrialSpawnerBlockEntity p_311991_, float p_312826_, PoseStack p_310994_, MultiBufferSource p_310042_, int p_311268_, int p_312508_, Vec3 p_395752_
    ) {
        Level level = p_311991_.getLevel();
        if (level != null) {
            TrialSpawner trialspawner = p_311991_.getTrialSpawner();
            TrialSpawnerStateData trialspawnerstatedata = trialspawner.getStateData();
            Entity entity = trialspawnerstatedata.getOrCreateDisplayEntity(trialspawner, level, trialspawner.getState());
            if (entity != null) {
                SpawnerRenderer.renderEntityInSpawner(
                    p_312826_, p_310994_, p_310042_, p_311268_, entity, this.entityRenderer, trialspawnerstatedata.getOSpin(), trialspawnerstatedata.getSpin()
                );
            }
        }
    }
}