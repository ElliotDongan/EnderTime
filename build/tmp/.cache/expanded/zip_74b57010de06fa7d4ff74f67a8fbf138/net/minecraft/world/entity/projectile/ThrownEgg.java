package net.minecraft.world.entity.projectile;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownEgg extends ThrowableItemProjectile {
    private static final EntityDimensions ZERO_SIZED_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);

    public ThrownEgg(EntityType<? extends ThrownEgg> p_37473_, Level p_37474_) {
        super(p_37473_, p_37474_);
    }

    public ThrownEgg(Level p_37476_, LivingEntity p_368678_, ItemStack p_365953_) {
        super(EntityType.EGG, p_368678_, p_37476_, p_365953_);
    }

    public ThrownEgg(Level p_37481_, double p_366951_, double p_367553_, double p_369744_, ItemStack p_362601_) {
        super(EntityType.EGG, p_366951_, p_367553_, p_369744_, p_37481_, p_362601_);
    }

    @Override
    public void handleEntityEvent(byte p_37484_) {
        if (p_37484_ == 3) {
            double d0 = 0.08;

            for (int i = 0; i < 8; i++) {
                this.level()
                    .addParticle(
                        new ItemParticleOption(ParticleTypes.ITEM, this.getItem()),
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        (this.random.nextFloat() - 0.5) * 0.08,
                        (this.random.nextFloat() - 0.5) * 0.08,
                        (this.random.nextFloat() - 0.5) * 0.08
                    );
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult p_37486_) {
        super.onHitEntity(p_37486_);
        p_37486_.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
    }

    @Override
    protected void onHit(HitResult p_37488_) {
        super.onHit(p_37488_);
        if (!this.level().isClientSide) {
            if (this.random.nextInt(8) == 0) {
                int i = 1;
                if (this.random.nextInt(32) == 0) {
                    i = 4;
                }

                for (int j = 0; j < i; j++) {
                    Chicken chicken = EntityType.CHICKEN.create(this.level(), EntitySpawnReason.TRIGGERED);
                    if (chicken != null) {
                        chicken.setAge(-24000);
                        chicken.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                        Optional.ofNullable(this.getItem().get(DataComponents.CHICKEN_VARIANT))
                            .flatMap(p_405565_ -> p_405565_.unwrap(this.registryAccess()))
                            .ifPresent(chicken::setVariant);
                        if (!chicken.fudgePositionAfterSizeChange(ZERO_SIZED_DIMENSIONS)) {
                            break;
                        }

                        this.level().addFreshEntity(chicken);
                    }
                }
            }

            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }

    @Override
    protected Item getDefaultItem() {
        return Items.EGG;
    }
}