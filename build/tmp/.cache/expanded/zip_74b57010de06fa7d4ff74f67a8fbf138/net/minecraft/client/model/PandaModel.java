package net.minecraft.client.model;

import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.PandaRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PandaModel extends QuadrupedModel<PandaRenderState> {
    public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(true, 23.0F, 4.8F, 2.7F, 3.0F, 49.0F, Set.of("head"));

    public PandaModel(ModelPart p_170771_) {
        super(p_170771_);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 6)
                .addBox(-6.5F, -5.0F, -4.0F, 13.0F, 10.0F, 9.0F)
                .texOffs(45, 16)
                .addBox("nose", -3.5F, 0.0F, -6.0F, 7.0F, 5.0F, 2.0F)
                .texOffs(52, 25)
                .addBox("left_ear", 3.5F, -8.0F, -1.0F, 5.0F, 4.0F, 1.0F)
                .texOffs(52, 25)
                .addBox("right_ear", -8.5F, -8.0F, -1.0F, 5.0F, 4.0F, 1.0F),
            PartPose.offset(0.0F, 11.5F, -17.0F)
        );
        partdefinition.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 25).addBox(-9.5F, -13.0F, -6.5F, 19.0F, 26.0F, 13.0F),
            PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
        );
        int i = 9;
        int j = 6;
        CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(40, 0).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F);
        partdefinition.addOrReplaceChild("right_hind_leg", cubelistbuilder, PartPose.offset(-5.5F, 15.0F, 9.0F));
        partdefinition.addOrReplaceChild("left_hind_leg", cubelistbuilder, PartPose.offset(5.5F, 15.0F, 9.0F));
        partdefinition.addOrReplaceChild("right_front_leg", cubelistbuilder, PartPose.offset(-5.5F, 15.0F, -9.0F));
        partdefinition.addOrReplaceChild("left_front_leg", cubelistbuilder, PartPose.offset(5.5F, 15.0F, -9.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void setupAnim(PandaRenderState p_368382_) {
        super.setupAnim(p_368382_);
        if (p_368382_.isUnhappy) {
            this.head.yRot = 0.35F * Mth.sin(0.6F * p_368382_.ageInTicks);
            this.head.zRot = 0.35F * Mth.sin(0.6F * p_368382_.ageInTicks);
            this.rightFrontLeg.xRot = -0.75F * Mth.sin(0.3F * p_368382_.ageInTicks);
            this.leftFrontLeg.xRot = 0.75F * Mth.sin(0.3F * p_368382_.ageInTicks);
        } else {
            this.head.zRot = 0.0F;
        }

        if (p_368382_.isSneezing) {
            if (p_368382_.sneezeTime < 15) {
                this.head.xRot = (float) (-Math.PI / 4) * p_368382_.sneezeTime / 14.0F;
            } else if (p_368382_.sneezeTime < 20) {
                float f = (p_368382_.sneezeTime - 15) / 5;
                this.head.xRot = (float) (-Math.PI / 4) + (float) (Math.PI / 4) * f;
            }
        }

        if (p_368382_.sitAmount > 0.0F) {
            this.body.xRot = Mth.rotLerpRad(p_368382_.sitAmount, this.body.xRot, 1.7407963F);
            this.head.xRot = Mth.rotLerpRad(p_368382_.sitAmount, this.head.xRot, (float) (Math.PI / 2));
            this.rightFrontLeg.zRot = -0.27079642F;
            this.leftFrontLeg.zRot = 0.27079642F;
            this.rightHindLeg.zRot = 0.5707964F;
            this.leftHindLeg.zRot = -0.5707964F;
            if (p_368382_.isEating) {
                this.head.xRot = (float) (Math.PI / 2) + 0.2F * Mth.sin(p_368382_.ageInTicks * 0.6F);
                this.rightFrontLeg.xRot = -0.4F - 0.2F * Mth.sin(p_368382_.ageInTicks * 0.6F);
                this.leftFrontLeg.xRot = -0.4F - 0.2F * Mth.sin(p_368382_.ageInTicks * 0.6F);
            }

            if (p_368382_.isScared) {
                this.head.xRot = 2.1707964F;
                this.rightFrontLeg.xRot = -0.9F;
                this.leftFrontLeg.xRot = -0.9F;
            }
        } else {
            this.rightHindLeg.zRot = 0.0F;
            this.leftHindLeg.zRot = 0.0F;
            this.rightFrontLeg.zRot = 0.0F;
            this.leftFrontLeg.zRot = 0.0F;
        }

        if (p_368382_.lieOnBackAmount > 0.0F) {
            this.rightHindLeg.xRot = -0.6F * Mth.sin(p_368382_.ageInTicks * 0.15F);
            this.leftHindLeg.xRot = 0.6F * Mth.sin(p_368382_.ageInTicks * 0.15F);
            this.rightFrontLeg.xRot = 0.3F * Mth.sin(p_368382_.ageInTicks * 0.25F);
            this.leftFrontLeg.xRot = -0.3F * Mth.sin(p_368382_.ageInTicks * 0.25F);
            this.head.xRot = Mth.rotLerpRad(p_368382_.lieOnBackAmount, this.head.xRot, (float) (Math.PI / 2));
        }

        if (p_368382_.rollAmount > 0.0F) {
            this.head.xRot = Mth.rotLerpRad(p_368382_.rollAmount, this.head.xRot, 2.0561945F);
            this.rightHindLeg.xRot = -0.5F * Mth.sin(p_368382_.ageInTicks * 0.5F);
            this.leftHindLeg.xRot = 0.5F * Mth.sin(p_368382_.ageInTicks * 0.5F);
            this.rightFrontLeg.xRot = 0.5F * Mth.sin(p_368382_.ageInTicks * 0.5F);
            this.leftFrontLeg.xRot = -0.5F * Mth.sin(p_368382_.ageInTicks * 0.5F);
        }
    }
}