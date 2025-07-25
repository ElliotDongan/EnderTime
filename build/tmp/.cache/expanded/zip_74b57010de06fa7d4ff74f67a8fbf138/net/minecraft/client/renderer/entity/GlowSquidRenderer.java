package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.SquidModel;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.GlowSquid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlowSquidRenderer extends SquidRenderer<GlowSquid> {
    private static final ResourceLocation GLOW_SQUID_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/squid/glow_squid.png");

    public GlowSquidRenderer(EntityRendererProvider.Context p_174136_, SquidModel p_174137_, SquidModel p_367166_) {
        super(p_174136_, p_174137_, p_367166_);
    }

    @Override
    public ResourceLocation getTextureLocation(SquidRenderState p_362361_) {
        return GLOW_SQUID_LOCATION;
    }

    protected int getBlockLightLevel(GlowSquid p_174146_, BlockPos p_174147_) {
        int i = (int)Mth.clampedLerp(0.0F, 15.0F, 1.0F - p_174146_.getDarkTicksRemaining() / 10.0F);
        return i == 15 ? 15 : Math.max(i, super.getBlockLightLevel(p_174146_, p_174147_));
    }
}