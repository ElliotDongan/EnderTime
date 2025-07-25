package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EvokerFangs extends Entity implements TraceableEntity {
    public static final int ATTACK_DURATION = 20;
    public static final int LIFE_OFFSET = 2;
    public static final int ATTACK_TRIGGER_TICKS = 14;
    private static final int DEFAULT_WARMUP_DELAY = 0;
    private int warmupDelayTicks = 0;
    private boolean sentSpikeEvent;
    private int lifeTicks = 22;
    private boolean clientSideAttackStarted;
    @Nullable
    private EntityReference<LivingEntity> owner;

    public EvokerFangs(EntityType<? extends EvokerFangs> p_36923_, Level p_36924_) {
        super(p_36923_, p_36924_);
    }

    public EvokerFangs(Level p_36926_, double p_36927_, double p_36928_, double p_36929_, float p_36930_, int p_36931_, LivingEntity p_36932_) {
        this(EntityType.EVOKER_FANGS, p_36926_);
        this.warmupDelayTicks = p_36931_;
        this.setOwner(p_36932_);
        this.setYRot(p_36930_ * (180.0F / (float)Math.PI));
        this.setPos(p_36927_, p_36928_, p_36929_);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_335129_) {
    }

    public void setOwner(@Nullable LivingEntity p_36939_) {
        this.owner = p_36939_ != null ? new EntityReference<>(p_36939_) : null;
    }

    @Nullable
    public LivingEntity getOwner() {
        return EntityReference.get(this.owner, this.level(), LivingEntity.class);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_408053_) {
        this.warmupDelayTicks = p_408053_.getIntOr("Warmup", 0);
        this.owner = EntityReference.read(p_408053_, "Owner");
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_409274_) {
        p_409274_.putInt("Warmup", this.warmupDelayTicks);
        EntityReference.store(this.owner, p_409274_, "Owner");
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.clientSideAttackStarted) {
                this.lifeTicks--;
                if (this.lifeTicks == 14) {
                    for (int i = 0; i < 12; i++) {
                        double d0 = this.getX() + (this.random.nextDouble() * 2.0 - 1.0) * this.getBbWidth() * 0.5;
                        double d1 = this.getY() + 0.05 + this.random.nextDouble();
                        double d2 = this.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * this.getBbWidth() * 0.5;
                        double d3 = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                        double d4 = 0.3 + this.random.nextDouble() * 0.3;
                        double d5 = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                        this.level().addParticle(ParticleTypes.CRIT, d0, d1 + 1.0, d2, d3, d4, d5);
                    }
                }
            }
        } else if (--this.warmupDelayTicks < 0) {
            if (this.warmupDelayTicks == -8) {
                for (LivingEntity livingentity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2, 0.0, 0.2))) {
                    this.dealDamageTo(livingentity);
                }
            }

            if (!this.sentSpikeEvent) {
                this.level().broadcastEntityEvent(this, (byte)4);
                this.sentSpikeEvent = true;
            }

            if (--this.lifeTicks < 0) {
                this.discard();
            }
        }
    }

    private void dealDamageTo(LivingEntity p_36945_) {
        LivingEntity livingentity = this.getOwner();
        if (p_36945_.isAlive() && !p_36945_.isInvulnerable() && p_36945_ != livingentity) {
            if (livingentity == null) {
                p_36945_.hurt(this.damageSources().magic(), 6.0F);
            } else {
                if (livingentity.isAlliedTo(p_36945_)) {
                    return;
                }

                DamageSource damagesource = this.damageSources().indirectMagic(this, livingentity);
                if (this.level() instanceof ServerLevel serverlevel && p_36945_.hurtServer(serverlevel, damagesource, 6.0F)) {
                    EnchantmentHelper.doPostAttackEffects(serverlevel, p_36945_, damagesource);
                }
            }
        }
    }

    @Override
    public void handleEntityEvent(byte p_36935_) {
        super.handleEntityEvent(p_36935_);
        if (p_36935_ == 4) {
            this.clientSideAttackStarted = true;
            if (!this.isSilent()) {
                this.level()
                    .playLocalSound(
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.EVOKER_FANGS_ATTACK,
                        this.getSoundSource(),
                        1.0F,
                        this.random.nextFloat() * 0.2F + 0.85F,
                        false
                    );
            }
        }
    }

    public float getAnimationProgress(float p_36937_) {
        if (!this.clientSideAttackStarted) {
            return 0.0F;
        } else {
            int i = this.lifeTicks - 2;
            return i <= 0 ? 1.0F : 1.0F - (i - p_36937_) / 20.0F;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel p_362713_, DamageSource p_362680_, float p_369558_) {
        return false;
    }
}