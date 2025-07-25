package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.GhastRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GhastModel extends EntityModel<GhastRenderState> {
    private final ModelPart[] tentacles = new ModelPart[9];

    public GhastModel(ModelPart p_170570_) {
        super(p_170570_);

        for (int i = 0; i < this.tentacles.length; i++) {
            this.tentacles[i] = p_170570_.getChild(PartNames.tentacle(i));
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.offset(0.0F, 17.6F, 0.0F)
        );
        RandomSource randomsource = RandomSource.create(1660L);

        for (int i = 0; i < 9; i++) {
            float f = ((i % 3 - i / 3 % 2 * 0.5F + 0.25F) / 2.0F * 2.0F - 1.0F) * 5.0F;
            float f1 = (i / 3 / 2.0F * 2.0F - 1.0F) * 5.0F;
            int j = randomsource.nextInt(7) + 8;
            partdefinition.addOrReplaceChild(
                PartNames.tentacle(i),
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, j, 2.0F),
                PartPose.offset(f, 24.6F, f1)
            );
        }

        return LayerDefinition.create(meshdefinition, 64, 32).apply(MeshTransformer.scaling(4.5F));
    }

    public void setupAnim(GhastRenderState p_365609_) {
        super.setupAnim(p_365609_);
        animateTentacles(p_365609_, this.tentacles);
    }

    public static void animateTentacles(EntityRenderState p_406987_, ModelPart[] p_409671_) {
        for (int i = 0; i < p_409671_.length; i++) {
            p_409671_[i].xRot = 0.2F * Mth.sin(p_406987_.ageInTicks * 0.3F + i) + 0.4F;
        }
    }
}