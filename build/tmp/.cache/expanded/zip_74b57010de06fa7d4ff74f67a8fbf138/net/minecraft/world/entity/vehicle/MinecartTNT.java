package net.minecraft.world.entity.vehicle;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MinecartTNT extends AbstractMinecart {
    private static final byte EVENT_PRIME = 10;
    private static final String TAG_EXPLOSION_POWER = "explosion_power";
    private static final String TAG_EXPLOSION_SPEED_FACTOR = "explosion_speed_factor";
    private static final String TAG_FUSE = "fuse";
    private static final float DEFAULT_EXPLOSION_POWER_BASE = 4.0F;
    private static final float DEFAULT_EXPLOSION_SPEED_FACTOR = 1.0F;
    private static final int NO_FUSE = -1;
    @Nullable
    private DamageSource ignitionSource;
    private int fuse = -1;
    private float explosionPowerBase = 4.0F;
    private float explosionSpeedFactor = 1.0F;

    public MinecartTNT(EntityType<? extends MinecartTNT> p_38649_, Level p_38650_) {
        super(p_38649_, p_38650_);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.TNT.defaultBlockState();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.fuse > 0) {
            this.fuse--;
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
        } else if (this.fuse == 0) {
            this.explode(this.ignitionSource, this.getDeltaMovement().horizontalDistanceSqr());
        }

        if (this.horizontalCollision) {
            double d0 = this.getDeltaMovement().horizontalDistanceSqr();
            if (d0 >= 0.01F) {
                this.explode(d0);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel p_369878_, DamageSource p_366706_, float p_368719_) {
        if (p_366706_.getDirectEntity() instanceof AbstractArrow abstractarrow && abstractarrow.isOnFire()) {
            DamageSource damagesource = this.damageSources().explosion(this, p_366706_.getEntity());
            this.explode(damagesource, abstractarrow.getDeltaMovement().lengthSqr());
        }

        return super.hurtServer(p_369878_, p_366706_, p_368719_);
    }

    @Override
    public void destroy(ServerLevel p_366240_, DamageSource p_38664_) {
        double d0 = this.getDeltaMovement().horizontalDistanceSqr();
        if (!damageSourceIgnitesTnt(p_38664_) && !(d0 >= 0.01F)) {
            this.destroy(p_366240_, this.getDropItem());
        } else {
            if (this.fuse < 0) {
                this.primeFuse(p_38664_);
                this.fuse = this.random.nextInt(20) + this.random.nextInt(20);
            }
        }
    }

    @Override
    protected Item getDropItem() {
        return Items.TNT_MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.TNT_MINECART);
    }

    protected void explode(double p_38689_) {
        this.explode(null, p_38689_);
    }

    protected void explode(@Nullable DamageSource p_259539_, double p_260287_) {
        if (this.level() instanceof ServerLevel serverlevel) {
            if (serverlevel.getGameRules().getBoolean(GameRules.RULE_TNT_EXPLODES)) {
                double d0 = Math.min(Math.sqrt(p_260287_), 5.0);
                serverlevel.explode(
                    this,
                    p_259539_,
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    (float)(this.explosionPowerBase + this.explosionSpeedFactor * this.random.nextDouble() * 1.5 * d0),
                    false,
                    Level.ExplosionInteraction.TNT
                );
                this.discard();
            } else if (this.isPrimed()) {
                this.discard();
            }
        }
    }

    @Override
    public boolean causeFallDamage(double p_392041_, float p_150347_, DamageSource p_150349_) {
        if (p_392041_ >= 3.0) {
            double d0 = p_392041_ / 10.0;
            this.explode(d0 * d0);
        }

        return super.causeFallDamage(p_392041_, p_150347_, p_150349_);
    }

    @Override
    public void activateMinecart(int p_38659_, int p_38660_, int p_38661_, boolean p_38662_) {
        if (p_38662_ && this.fuse < 0) {
            this.primeFuse(null);
        }
    }

    @Override
    public void handleEntityEvent(byte p_38657_) {
        if (p_38657_ == 10) {
            this.primeFuse(null);
        } else {
            super.handleEntityEvent(p_38657_);
        }
    }

    public void primeFuse(@Nullable DamageSource p_393504_) {
        if (!(this.level() instanceof ServerLevel serverlevel && !serverlevel.getGameRules().getBoolean(GameRules.RULE_TNT_EXPLODES))) {
            this.fuse = 80;
            if (!this.level().isClientSide) {
                if (p_393504_ != null && this.ignitionSource == null) {
                    this.ignitionSource = this.damageSources().explosion(this, p_393504_.getEntity());
                }

                this.level().broadcastEntityEvent(this, (byte)10);
                if (!this.isSilent()) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }

    public int getFuse() {
        return this.fuse;
    }

    public boolean isPrimed() {
        return this.fuse > -1;
    }

    @Override
    public float getBlockExplosionResistance(Explosion p_38675_, BlockGetter p_38676_, BlockPos p_38677_, BlockState p_38678_, FluidState p_38679_, float p_38680_) {
        return !this.isPrimed() || !p_38678_.is(BlockTags.RAILS) && !p_38676_.getBlockState(p_38677_.above()).is(BlockTags.RAILS)
            ? super.getBlockExplosionResistance(p_38675_, p_38676_, p_38677_, p_38678_, p_38679_, p_38680_)
            : 0.0F;
    }

    @Override
    public boolean shouldBlockExplode(Explosion p_38669_, BlockGetter p_38670_, BlockPos p_38671_, BlockState p_38672_, float p_38673_) {
        return !this.isPrimed() || !p_38672_.is(BlockTags.RAILS) && !p_38670_.getBlockState(p_38671_.above()).is(BlockTags.RAILS)
            ? super.shouldBlockExplode(p_38669_, p_38670_, p_38671_, p_38672_, p_38673_)
            : false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_408127_) {
        super.readAdditionalSaveData(p_408127_);
        this.fuse = p_408127_.getIntOr("fuse", -1);
        this.explosionPowerBase = Mth.clamp(p_408127_.getFloatOr("explosion_power", 4.0F), 0.0F, 128.0F);
        this.explosionSpeedFactor = Mth.clamp(p_408127_.getFloatOr("explosion_speed_factor", 1.0F), 0.0F, 128.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_407527_) {
        super.addAdditionalSaveData(p_407527_);
        p_407527_.putInt("fuse", this.fuse);
        if (this.explosionPowerBase != 4.0F) {
            p_407527_.putFloat("explosion_power", this.explosionPowerBase);
        }

        if (this.explosionSpeedFactor != 1.0F) {
            p_407527_.putFloat("explosion_speed_factor", this.explosionSpeedFactor);
        }
    }

    @Override
    boolean shouldSourceDestroy(DamageSource p_310072_) {
        return damageSourceIgnitesTnt(p_310072_);
    }

    private static boolean damageSourceIgnitesTnt(DamageSource p_311405_) {
        return p_311405_.getDirectEntity() instanceof Projectile projectile
            ? projectile.isOnFire()
            : p_311405_.is(DamageTypeTags.IS_FIRE) || p_311405_.is(DamageTypeTags.IS_EXPLOSION);
    }
}