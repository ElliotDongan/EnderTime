package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SpectralArrow extends AbstractArrow {
    private static final int DEFAULT_DURATION = 200;
    private int duration = 200;

    public SpectralArrow(EntityType<? extends SpectralArrow> p_37411_, Level p_37412_) {
        super(p_37411_, p_37412_);
    }

    public SpectralArrow(Level p_37414_, LivingEntity p_311904_, ItemStack p_309799_, @Nullable ItemStack p_342763_) {
        super(EntityType.SPECTRAL_ARROW, p_311904_, p_37414_, p_309799_, p_342763_);
    }

    public SpectralArrow(Level p_37419_, double p_309976_, double p_310894_, double p_309922_, ItemStack p_311719_, @Nullable ItemStack p_343959_) {
        super(EntityType.SPECTRAL_ARROW, p_309976_, p_310894_, p_309922_, p_37419_, p_311719_, p_343959_);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.isInGround()) {
            this.level().addParticle(ParticleTypes.INSTANT_EFFECT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity p_37422_) {
        super.doPostHurtEffects(p_37422_);
        MobEffectInstance mobeffectinstance = new MobEffectInstance(MobEffects.GLOWING, this.duration, 0);
        p_37422_.addEffect(mobeffectinstance, this.getEffectSource());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_409154_) {
        super.readAdditionalSaveData(p_409154_);
        this.duration = p_409154_.getIntOr("Duration", 200);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_407438_) {
        super.addAdditionalSaveData(p_407438_);
        p_407438_.putInt("Duration", this.duration);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.SPECTRAL_ARROW);
    }
}