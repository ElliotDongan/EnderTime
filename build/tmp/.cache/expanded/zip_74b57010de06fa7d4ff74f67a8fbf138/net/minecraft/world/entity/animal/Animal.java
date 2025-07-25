package net.minecraft.world.entity.animal;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class Animal extends AgeableMob {
    protected static final int PARENT_AGE_AFTER_BREEDING = 6000;
    private static final int DEFAULT_IN_LOVE_TIME = 0;
    private int inLove = 0;
    @Nullable
    private EntityReference<ServerPlayer> loveCause;

    protected Animal(EntityType<? extends Animal> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    public static AttributeSupplier.Builder createAnimalAttributes() {
        return Mob.createMobAttributes().add(Attributes.TEMPT_RANGE, 10.0);
    }

    @Override
    protected void customServerAiStep(ServerLevel p_366177_) {
        if (this.getAge() != 0) {
            this.inLove = 0;
        }

        super.customServerAiStep(p_366177_);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getAge() != 0) {
            this.inLove = 0;
        }

        if (this.inLove > 0) {
            this.inLove--;
            if (this.inLove % 10 == 0) {
                double d0 = this.random.nextGaussian() * 0.02;
                double d1 = this.random.nextGaussian() * 0.02;
                double d2 = this.random.nextGaussian() * 0.02;
                this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), d0, d1, d2);
            }
        }
    }

    @Override
    protected void actuallyHurt(ServerLevel p_364204_, DamageSource p_328294_, float p_327706_) {
        this.resetLove();
        super.actuallyHurt(p_364204_, p_328294_, p_327706_);
    }

    @Override
    public float getWalkTargetValue(BlockPos p_27573_, LevelReader p_27574_) {
        return p_27574_.getBlockState(p_27573_.below()).is(Blocks.GRASS_BLOCK) ? 10.0F : p_27574_.getPathfindingCostFromLightLevels(p_27573_);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_405980_) {
        super.addAdditionalSaveData(p_405980_);
        p_405980_.putInt("InLove", this.inLove);
        EntityReference.store(this.loveCause, p_405980_, "LoveCause");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_406409_) {
        super.readAdditionalSaveData(p_406409_);
        this.inLove = p_406409_.getIntOr("InLove", 0);
        this.loveCause = EntityReference.read(p_406409_, "LoveCause");
    }

    public static boolean checkAnimalSpawnRules(
        EntityType<? extends Animal> p_218105_, LevelAccessor p_218106_, EntitySpawnReason p_367954_, BlockPos p_218108_, RandomSource p_218109_
    ) {
        boolean flag = EntitySpawnReason.ignoresLightRequirements(p_367954_) || isBrightEnoughToSpawn(p_218106_, p_218108_);
        return p_218106_.getBlockState(p_218108_.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && flag;
    }

    protected static boolean isBrightEnoughToSpawn(BlockAndTintGetter p_186210_, BlockPos p_186211_) {
        return p_186210_.getRawBrightness(p_186211_, 0) > 8;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    public boolean removeWhenFarAway(double p_27598_) {
        return false;
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel p_364547_) {
        return 1 + this.random.nextInt(3);
    }

    public abstract boolean isFood(ItemStack p_27600_);

    @Override
    public InteractionResult mobInteract(Player p_27584_, InteractionHand p_27585_) {
        ItemStack itemstack = p_27584_.getItemInHand(p_27585_);
        if (this.isFood(itemstack)) {
            int i = this.getAge();
            if (p_27584_ instanceof ServerPlayer serverplayer && i == 0 && this.canFallInLove()) {
                this.usePlayerItem(p_27584_, p_27585_, itemstack);
                this.setInLove(serverplayer);
                this.playEatingSound();
                return InteractionResult.SUCCESS_SERVER;
            }

            if (this.isBaby()) {
                this.usePlayerItem(p_27584_, p_27585_, itemstack);
                this.ageUp(getSpeedUpSecondsWhenFeeding(-i), true);
                this.playEatingSound();
                return InteractionResult.SUCCESS;
            }

            if (this.level().isClientSide) {
                return InteractionResult.CONSUME;
            }
        }

        return super.mobInteract(p_27584_, p_27585_);
    }

    protected void playEatingSound() {
    }

    protected void usePlayerItem(Player p_148715_, InteractionHand p_148716_, ItemStack p_148717_) {
        int i = p_148717_.getCount();
        UseRemainder useremainder = p_148717_.get(DataComponents.USE_REMAINDER);
        p_148717_.consume(1, p_148715_);
        if (useremainder != null) {
            ItemStack itemstack = useremainder.convertIntoRemainder(p_148717_, i, p_148715_.hasInfiniteMaterials(), p_148715_::handleExtraItemsCreatedOnUse);
            p_148715_.setItemInHand(p_148716_, itemstack);
        }
    }

    public boolean canFallInLove() {
        return this.inLove <= 0;
    }

    public void setInLove(@Nullable Player p_27596_) {
        this.inLove = 600;
        if (p_27596_ instanceof ServerPlayer serverplayer) {
            this.loveCause = new EntityReference<>(serverplayer);
        }

        this.level().broadcastEntityEvent(this, (byte)18);
    }

    public void setInLoveTime(int p_27602_) {
        this.inLove = p_27602_;
    }

    public int getInLoveTime() {
        return this.inLove;
    }

    @Nullable
    public ServerPlayer getLoveCause() {
        return EntityReference.get(this.loveCause, uuid -> (ServerPlayer)this.level().getPlayerByUUID(uuid), ServerPlayer.class);
    }

    public boolean isInLove() {
        return this.inLove > 0;
    }

    public void resetLove() {
        this.inLove = 0;
    }

    public boolean canMate(Animal p_27569_) {
        if (p_27569_ == this) {
            return false;
        } else {
            return p_27569_.getClass() != this.getClass() ? false : this.isInLove() && p_27569_.isInLove();
        }
    }

    public void spawnChildFromBreeding(ServerLevel p_27564_, Animal p_27565_) {
        AgeableMob ageablemob = this.getBreedOffspring(p_27564_, p_27565_);
        final var event = new net.minecraftforge.event.entity.living.BabyEntitySpawnEvent(this, p_27565_, ageablemob);
        final boolean cancelled = net.minecraftforge.event.entity.living.BabyEntitySpawnEvent.BUS.post(event);
        ageablemob = event.getChild();
        if (cancelled) {
            //Reset the "inLove" state for the animals
            this.setAge(6000);
            p_27565_.setAge(6000);
            this.resetLove();
            p_27565_.resetLove();
            return;
        }
        if (ageablemob != null) {
            ageablemob.setBaby(true);
            ageablemob.snapTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            this.finalizeSpawnChildFromBreeding(p_27564_, p_27565_, ageablemob);
            p_27564_.addFreshEntityWithPassengers(ageablemob);
        }
    }

    public void finalizeSpawnChildFromBreeding(ServerLevel p_277963_, Animal p_277357_, @Nullable AgeableMob p_277516_) {
        Optional.ofNullable(this.getLoveCause()).or(() -> Optional.ofNullable(p_277357_.getLoveCause())).ifPresent(p_277486_ -> {
            p_277486_.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(p_277486_, this, p_277357_, p_277516_);
        });
        this.setAge(6000);
        p_277357_.setAge(6000);
        this.resetLove();
        p_277357_.resetLove();
        p_277963_.broadcastEntityEvent(this, (byte)18);
        if (p_277963_.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            p_277963_.addFreshEntity(new ExperienceOrb(p_277963_, this.getX(), this.getY(), this.getZ(), this.getRandom().nextInt(7) + 1));
        }
    }

    @Override
    public void handleEntityEvent(byte p_27562_) {
        if (p_27562_ == 18) {
            for (int i = 0; i < 7; i++) {
                double d0 = this.random.nextGaussian() * 0.02;
                double d1 = this.random.nextGaussian() * 0.02;
                double d2 = this.random.nextGaussian() * 0.02;
                this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), d0, d1, d2);
            }
        } else {
            super.handleEntityEvent(p_27562_);
        }
    }
}
