package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HuskRenderer extends ZombieRenderer {
    private static final ResourceLocation HUSK_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/zombie/husk.png");

    public HuskRenderer(EntityRendererProvider.Context p_174180_) {
        super(
            p_174180_, ModelLayers.HUSK, ModelLayers.HUSK_BABY, ModelLayers.HUSK_INNER_ARMOR, ModelLayers.HUSK_OUTER_ARMOR, ModelLayers.HUSK_BABY_INNER_ARMOR, ModelLayers.HUSK_BABY_OUTER_ARMOR
        );
    }

    @Override
    public ResourceLocation getTextureLocation(ZombieRenderState p_366257_) {
        return HUSK_LOCATION;
    }
}