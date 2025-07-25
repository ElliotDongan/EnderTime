package net.minecraft.world.entity.animal;

import com.mojang.serialization.Codec;
import java.util.EnumSet;
import java.util.List;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;

public class Panda extends Animal {
    private static final EntityDataAccessor<Integer> UNHAPPY_COUNTER = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SNEEZE_COUNTER = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EAT_COUNTER = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> MAIN_GENE_ID = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> HIDDEN_GENE_ID = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(Panda.class, EntityDataSerializers.BYTE);
    static final TargetingConditions BREED_TARGETING = TargetingConditions.forNonCombat().range(8.0);
    private static final EntityDimensions BABY_DIMENSIONS = EntityType.PANDA
        .getDimensions()
        .scale(0.5F)
        .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.40625F, 0.0F));
    private static final int FLAG_SNEEZE = 2;
    private static final int FLAG_ROLL = 4;
    private static final int FLAG_SIT = 8;
    private static final int FLAG_ON_BACK = 16;
    private static final int EAT_TICK_INTERVAL = 5;
    public static final int TOTAL_ROLL_STEPS = 32;
    private static final int TOTAL_UNHAPPY_TIME = 32;
    boolean gotBamboo;
    boolean didBite;
    public int rollCounter;
    private Vec3 rollDelta;
    private float sitAmount;
    private float sitAmountO;
    private float onBackAmount;
    private float onBackAmountO;
    private float rollAmount;
    private float rollAmountO;
    Panda.PandaLookAtPlayerGoal lookAtPlayerGoal;

    public Panda(EntityType<? extends Panda> p_29086_, Level p_29087_) {
        super(p_29086_, p_29087_);
        this.moveControl = new Panda.PandaMoveControl(this);
        if (!this.isBaby()) {
            this.setCanPickUpLoot(true);
        }
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_369921_) {
        return p_369921_ == EquipmentSlot.MAINHAND && this.canPickUpLoot();
    }

    public int getUnhappyCounter() {
        return this.entityData.get(UNHAPPY_COUNTER);
    }

    public void setUnhappyCounter(int p_29207_) {
        this.entityData.set(UNHAPPY_COUNTER, p_29207_);
    }

    public boolean isSneezing() {
        return this.getFlag(2);
    }

    public boolean isSitting() {
        return this.getFlag(8);
    }

    public void sit(boolean p_29209_) {
        this.setFlag(8, p_29209_);
    }

    public boolean isOnBack() {
        return this.getFlag(16);
    }

    public void setOnBack(boolean p_29213_) {
        this.setFlag(16, p_29213_);
    }

    public boolean isEating() {
        return this.entityData.get(EAT_COUNTER) > 0;
    }

    public void eat(boolean p_29217_) {
        this.entityData.set(EAT_COUNTER, p_29217_ ? 1 : 0);
    }

    private int getEatCounter() {
        return this.entityData.get(EAT_COUNTER);
    }

    private void setEatCounter(int p_29215_) {
        this.entityData.set(EAT_COUNTER, p_29215_);
    }

    public void sneeze(boolean p_29221_) {
        this.setFlag(2, p_29221_);
        if (!p_29221_) {
            this.setSneezeCounter(0);
        }
    }

    public int getSneezeCounter() {
        return this.entityData.get(SNEEZE_COUNTER);
    }

    public void setSneezeCounter(int p_29211_) {
        this.entityData.set(SNEEZE_COUNTER, p_29211_);
    }

    public Panda.Gene getMainGene() {
        return Panda.Gene.byId(this.entityData.get(MAIN_GENE_ID));
    }

    public void setMainGene(Panda.Gene p_29100_) {
        if (p_29100_.getId() > 6) {
            p_29100_ = Panda.Gene.getRandom(this.random);
        }

        this.entityData.set(MAIN_GENE_ID, (byte)p_29100_.getId());
    }

    public Panda.Gene getHiddenGene() {
        return Panda.Gene.byId(this.entityData.get(HIDDEN_GENE_ID));
    }

    public void setHiddenGene(Panda.Gene p_29117_) {
        if (p_29117_.getId() > 6) {
            p_29117_ = Panda.Gene.getRandom(this.random);
        }

        this.entityData.set(HIDDEN_GENE_ID, (byte)p_29117_.getId());
    }

    public boolean isRolling() {
        return this.getFlag(4);
    }

    public void roll(boolean p_29223_) {
        this.setFlag(4, p_29223_);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_336304_) {
        super.defineSynchedData(p_336304_);
        p_336304_.define(UNHAPPY_COUNTER, 0);
        p_336304_.define(SNEEZE_COUNTER, 0);
        p_336304_.define(MAIN_GENE_ID, (byte)0);
        p_336304_.define(HIDDEN_GENE_ID, (byte)0);
        p_336304_.define(DATA_ID_FLAGS, (byte)0);
        p_336304_.define(EAT_COUNTER, 0);
    }

    private boolean getFlag(int p_29219_) {
        return (this.entityData.get(DATA_ID_FLAGS) & p_29219_) != 0;
    }

    private void setFlag(int p_29135_, boolean p_29136_) {
        byte b0 = this.entityData.get(DATA_ID_FLAGS);
        if (p_29136_) {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b0 | p_29135_));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b0 & ~p_29135_));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_406238_) {
        super.addAdditionalSaveData(p_406238_);
        p_406238_.store("MainGene", Panda.Gene.CODEC, this.getMainGene());
        p_406238_.store("HiddenGene", Panda.Gene.CODEC, this.getHiddenGene());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_409734_) {
        super.readAdditionalSaveData(p_409734_);
        this.setMainGene(p_409734_.read("MainGene", Panda.Gene.CODEC).orElse(Panda.Gene.NORMAL));
        this.setHiddenGene(p_409734_.read("HiddenGene", Panda.Gene.CODEC).orElse(Panda.Gene.NORMAL));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_148967_, AgeableMob p_148968_) {
        Panda panda = EntityType.PANDA.create(p_148967_, EntitySpawnReason.BREEDING);
        if (panda != null) {
            if (p_148968_ instanceof Panda panda1) {
                panda.setGeneFromParents(this, panda1);
            }

            panda.setAttributes();
        }

        return panda;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new Panda.PandaPanicGoal(this, 2.0));
        this.goalSelector.addGoal(2, new Panda.PandaBreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new Panda.PandaAttackGoal(this, 1.2F, true));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, p_326978_ -> p_326978_.is(ItemTags.PANDA_FOOD), false));
        this.goalSelector.addGoal(6, new Panda.PandaAvoidGoal<>(this, Player.class, 8.0F, 2.0, 2.0));
        this.goalSelector.addGoal(6, new Panda.PandaAvoidGoal<>(this, Monster.class, 4.0F, 2.0, 2.0));
        this.goalSelector.addGoal(7, new Panda.PandaSitGoal());
        this.goalSelector.addGoal(8, new Panda.PandaLieOnBackGoal(this));
        this.goalSelector.addGoal(8, new Panda.PandaSneezeGoal(this));
        this.lookAtPlayerGoal = new Panda.PandaLookAtPlayerGoal(this, Player.class, 6.0F);
        this.goalSelector.addGoal(9, this.lookAtPlayerGoal);
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(12, new Panda.PandaRollGoal(this));
        this.goalSelector.addGoal(13, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(14, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new Panda.PandaHurtByTargetGoal(this).setAlertOthers(new Class[0]));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.15F).add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    public Panda.Gene getVariant() {
        return Panda.Gene.getVariantFromGenes(this.getMainGene(), this.getHiddenGene());
    }

    public boolean isLazy() {
        return this.getVariant() == Panda.Gene.LAZY;
    }

    public boolean isWorried() {
        return this.getVariant() == Panda.Gene.WORRIED;
    }

    public boolean isPlayful() {
        return this.getVariant() == Panda.Gene.PLAYFUL;
    }

    public boolean isBrown() {
        return this.getVariant() == Panda.Gene.BROWN;
    }

    public boolean isWeak() {
        return this.getVariant() == Panda.Gene.WEAK;
    }

    @Override
    public boolean isAggressive() {
        return this.getVariant() == Panda.Gene.AGGRESSIVE;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean doHurtTarget(ServerLevel p_361261_, Entity p_29091_) {
        if (!this.isAggressive()) {
            this.didBite = true;
        }

        return super.doHurtTarget(p_361261_, p_29091_);
    }

    @Override
    public void playAttackSound() {
        this.playSound(SoundEvents.PANDA_BITE, 1.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isWorried()) {
            if (this.level().isThundering() && !this.isInWater()) {
                this.sit(true);
                this.eat(false);
            } else if (!this.isEating()) {
                this.sit(false);
            }
        }

        LivingEntity livingentity = this.getTarget();
        if (livingentity == null) {
            this.gotBamboo = false;
            this.didBite = false;
        }

        if (this.getUnhappyCounter() > 0) {
            if (livingentity != null) {
                this.lookAt(livingentity, 90.0F, 90.0F);
            }

            if (this.getUnhappyCounter() == 29 || this.getUnhappyCounter() == 14) {
                this.playSound(SoundEvents.PANDA_CANT_BREED, 1.0F, 1.0F);
            }

            this.setUnhappyCounter(this.getUnhappyCounter() - 1);
        }

        if (this.isSneezing()) {
            this.setSneezeCounter(this.getSneezeCounter() + 1);
            if (this.getSneezeCounter() > 20) {
                this.sneeze(false);
                this.afterSneeze();
            } else if (this.getSneezeCounter() == 1) {
                this.playSound(SoundEvents.PANDA_PRE_SNEEZE, 1.0F, 1.0F);
            }
        }

        if (this.isRolling()) {
            this.handleRoll();
        } else {
            this.rollCounter = 0;
        }

        if (this.isSitting()) {
            this.setXRot(0.0F);
        }

        this.updateSitAmount();
        this.handleEating();
        this.updateOnBackAnimation();
        this.updateRollAmount();
    }

    public boolean isScared() {
        return this.isWorried() && this.level().isThundering();
    }

    private void handleEating() {
        if (!this.isEating() && this.isSitting() && !this.isScared() && !this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && this.random.nextInt(80) == 1) {
            this.eat(true);
        } else if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() || !this.isSitting()) {
            this.eat(false);
        }

        if (this.isEating()) {
            this.addEatingParticles();
            if (!this.level().isClientSide && this.getEatCounter() > 80 && this.random.nextInt(20) == 1) {
                if (this.getEatCounter() > 100 && this.getItemBySlot(EquipmentSlot.MAINHAND).is(ItemTags.PANDA_EATS_FROM_GROUND)) {
                    if (!this.level().isClientSide) {
                        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                        this.gameEvent(GameEvent.EAT);
                    }

                    this.sit(false);
                }

                this.eat(false);
                return;
            }

            this.setEatCounter(this.getEatCounter() + 1);
        }
    }

    private void addEatingParticles() {
        if (this.getEatCounter() % 5 == 0) {
            this.playSound(SoundEvents.PANDA_EAT, 0.5F + 0.5F * this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);

            for (int i = 0; i < 6; i++) {
                Vec3 vec3 = new Vec3((this.random.nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, (this.random.nextFloat() - 0.5) * 0.1);
                vec3 = vec3.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
                vec3 = vec3.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
                double d0 = -this.random.nextFloat() * 0.6 - 0.3;
                Vec3 vec31 = new Vec3((this.random.nextFloat() - 0.5) * 0.8, d0, 1.0 + (this.random.nextFloat() - 0.5) * 0.4);
                vec31 = vec31.yRot(-this.yBodyRot * (float) (Math.PI / 180.0));
                vec31 = vec31.add(this.getX(), this.getEyeY() + 1.0, this.getZ());
                this.level()
                    .addParticle(
                        new ItemParticleOption(ParticleTypes.ITEM, this.getItemBySlot(EquipmentSlot.MAINHAND)),
                        vec31.x,
                        vec31.y,
                        vec31.z,
                        vec3.x,
                        vec3.y + 0.05,
                        vec3.z
                    );
            }
        }
    }

    private void updateSitAmount() {
        this.sitAmountO = this.sitAmount;
        if (this.isSitting()) {
            this.sitAmount = Math.min(1.0F, this.sitAmount + 0.15F);
        } else {
            this.sitAmount = Math.max(0.0F, this.sitAmount - 0.19F);
        }
    }

    private void updateOnBackAnimation() {
        this.onBackAmountO = this.onBackAmount;
        if (this.isOnBack()) {
            this.onBackAmount = Math.min(1.0F, this.onBackAmount + 0.15F);
        } else {
            this.onBackAmount = Math.max(0.0F, this.onBackAmount - 0.19F);
        }
    }

    private void updateRollAmount() {
        this.rollAmountO = this.rollAmount;
        if (this.isRolling()) {
            this.rollAmount = Math.min(1.0F, this.rollAmount + 0.15F);
        } else {
            this.rollAmount = Math.max(0.0F, this.rollAmount - 0.19F);
        }
    }

    public float getSitAmount(float p_29225_) {
        return Mth.lerp(p_29225_, this.sitAmountO, this.sitAmount);
    }

    public float getLieOnBackAmount(float p_29227_) {
        return Mth.lerp(p_29227_, this.onBackAmountO, this.onBackAmount);
    }

    public float getRollAmount(float p_29089_) {
        return Mth.lerp(p_29089_, this.rollAmountO, this.rollAmount);
    }

    private void handleRoll() {
        this.rollCounter++;
        if (this.rollCounter > 32) {
            this.roll(false);
        } else {
            if (!this.level().isClientSide) {
                Vec3 vec3 = this.getDeltaMovement();
                if (this.rollCounter == 1) {
                    float f = this.getYRot() * (float) (Math.PI / 180.0);
                    float f1 = this.isBaby() ? 0.1F : 0.2F;
                    this.rollDelta = new Vec3(vec3.x + -Mth.sin(f) * f1, 0.0, vec3.z + Mth.cos(f) * f1);
                    this.setDeltaMovement(this.rollDelta.add(0.0, 0.27, 0.0));
                } else if (this.rollCounter != 7.0F && this.rollCounter != 15.0F && this.rollCounter != 23.0F) {
                    this.setDeltaMovement(this.rollDelta.x, vec3.y, this.rollDelta.z);
                } else {
                    this.setDeltaMovement(0.0, this.onGround() ? 0.27 : vec3.y, 0.0);
                }
            }
        }
    }

    private void afterSneeze() {
        Vec3 vec3 = this.getDeltaMovement();
        Level level = this.level();
        level.addParticle(
            ParticleTypes.SNEEZE,
            this.getX() - (this.getBbWidth() + 1.0F) * 0.5 * Mth.sin(this.yBodyRot * (float) (Math.PI / 180.0)),
            this.getEyeY() - 0.1F,
            this.getZ() + (this.getBbWidth() + 1.0F) * 0.5 * Mth.cos(this.yBodyRot * (float) (Math.PI / 180.0)),
            vec3.x,
            0.0,
            vec3.z
        );
        this.playSound(SoundEvents.PANDA_SNEEZE, 1.0F, 1.0F);

        for (Panda panda : level.getEntitiesOfClass(Panda.class, this.getBoundingBox().inflate(10.0))) {
            if (!panda.isBaby() && panda.onGround() && !panda.isInWater() && panda.canPerformAction()) {
                panda.jumpFromGround();
            }
        }

        if (this.level() instanceof ServerLevel serverlevel && serverlevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            this.dropFromGiftLootTable(serverlevel, BuiltInLootTables.PANDA_SNEEZE, this::spawnAtLocation);
        }
    }

    @Override
    protected void pickUpItem(ServerLevel p_362086_, ItemEntity p_29121_) {
        if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && canPickUpAndEat(p_29121_)) {
            this.onItemPickup(p_29121_);
            ItemStack itemstack = p_29121_.getItem();
            this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
            this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            this.take(p_29121_, itemstack.getCount());
            p_29121_.discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel p_368541_, DamageSource p_366264_, float p_361030_) {
        this.sit(false);
        return super.hurtServer(p_368541_, p_366264_, p_361030_);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_29109_, DifficultyInstance p_29110_, EntitySpawnReason p_363460_, @Nullable SpawnGroupData p_29112_) {
        RandomSource randomsource = p_29109_.getRandom();
        this.setMainGene(Panda.Gene.getRandom(randomsource));
        this.setHiddenGene(Panda.Gene.getRandom(randomsource));
        this.setAttributes();
        if (p_29112_ == null) {
            p_29112_ = new AgeableMob.AgeableMobGroupData(0.2F);
        }

        return super.finalizeSpawn(p_29109_, p_29110_, p_363460_, p_29112_);
    }

    public void setGeneFromParents(Panda p_29104_, @Nullable Panda p_29105_) {
        if (p_29105_ == null) {
            if (this.random.nextBoolean()) {
                this.setMainGene(p_29104_.getOneOfGenesRandomly());
                this.setHiddenGene(Panda.Gene.getRandom(this.random));
            } else {
                this.setMainGene(Panda.Gene.getRandom(this.random));
                this.setHiddenGene(p_29104_.getOneOfGenesRandomly());
            }
        } else if (this.random.nextBoolean()) {
            this.setMainGene(p_29104_.getOneOfGenesRandomly());
            this.setHiddenGene(p_29105_.getOneOfGenesRandomly());
        } else {
            this.setMainGene(p_29105_.getOneOfGenesRandomly());
            this.setHiddenGene(p_29104_.getOneOfGenesRandomly());
        }

        if (this.random.nextInt(32) == 0) {
            this.setMainGene(Panda.Gene.getRandom(this.random));
        }

        if (this.random.nextInt(32) == 0) {
            this.setHiddenGene(Panda.Gene.getRandom(this.random));
        }
    }

    private Panda.Gene getOneOfGenesRandomly() {
        return this.random.nextBoolean() ? this.getMainGene() : this.getHiddenGene();
    }

    public void setAttributes() {
        if (this.isWeak()) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0);
        }

        if (this.isLazy()) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.07F);
        }
    }

    void tryToSit() {
        if (!this.isInWater()) {
            this.setZza(0.0F);
            this.getNavigation().stop();
            this.sit(true);
        }
    }

    @Override
    public InteractionResult mobInteract(Player p_29123_, InteractionHand p_29124_) {
        ItemStack itemstack = p_29123_.getItemInHand(p_29124_);
        if (this.isScared()) {
            return InteractionResult.PASS;
        } else if (this.isOnBack()) {
            this.setOnBack(false);
            return InteractionResult.SUCCESS;
        } else if (this.isFood(itemstack)) {
            if (this.getTarget() != null) {
                this.gotBamboo = true;
            }

            if (this.isBaby()) {
                this.usePlayerItem(p_29123_, p_29124_, itemstack);
                this.ageUp((int)(-this.getAge() / 20 * 0.1F), true);
            } else if (!this.level().isClientSide && this.getAge() == 0 && this.canFallInLove()) {
                this.usePlayerItem(p_29123_, p_29124_, itemstack);
                this.setInLove(p_29123_);
            } else {
                if (!(this.level() instanceof ServerLevel serverlevel) || this.isSitting() || this.isInWater()) {
                    return InteractionResult.PASS;
                }

                this.tryToSit();
                this.eat(true);
                ItemStack itemstack1 = this.getItemBySlot(EquipmentSlot.MAINHAND);
                if (!itemstack1.isEmpty() && !p_29123_.hasInfiniteMaterials()) {
                    this.spawnAtLocation(serverlevel, itemstack1);
                }

                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(itemstack.getItem(), 1));
                this.usePlayerItem(p_29123_, p_29124_, itemstack);
            }

            return InteractionResult.SUCCESS_SERVER;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isAggressive()) {
            return SoundEvents.PANDA_AGGRESSIVE_AMBIENT;
        } else {
            return this.isWorried() ? SoundEvents.PANDA_WORRIED_AMBIENT : SoundEvents.PANDA_AMBIENT;
        }
    }

    @Override
    protected void playStepSound(BlockPos p_29126_, BlockState p_29127_) {
        this.playSound(SoundEvents.PANDA_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean isFood(ItemStack p_29192_) {
        return p_29192_.is(ItemTags.PANDA_FOOD);
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PANDA_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource p_29142_) {
        return SoundEvents.PANDA_HURT;
    }

    public boolean canPerformAction() {
        return !this.isOnBack() && !this.isScared() && !this.isEating() && !this.isRolling() && !this.isSitting();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose p_330547_) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_330547_);
    }

    private static boolean canPickUpAndEat(ItemEntity p_363198_) {
        return p_363198_.getItem().is(ItemTags.PANDA_EATS_FROM_GROUND) && p_363198_.isAlive() && !p_363198_.hasPickUpDelay();
    }

    public static enum Gene implements StringRepresentable {
        NORMAL(0, "normal", false),
        LAZY(1, "lazy", false),
        WORRIED(2, "worried", false),
        PLAYFUL(3, "playful", false),
        BROWN(4, "brown", true),
        WEAK(5, "weak", true),
        AGGRESSIVE(6, "aggressive", false);

        public static final Codec<Panda.Gene> CODEC = StringRepresentable.fromEnum(Panda.Gene::values);
        private static final IntFunction<Panda.Gene> BY_ID = ByIdMap.continuous(Panda.Gene::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        private static final int MAX_GENE = 6;
        private final int id;
        private final String name;
        private final boolean isRecessive;

        private Gene(final int p_29244_, final String p_29245_, final boolean p_29246_) {
            this.id = p_29244_;
            this.name = p_29245_;
            this.isRecessive = p_29246_;
        }

        public int getId() {
            return this.id;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public boolean isRecessive() {
            return this.isRecessive;
        }

        static Panda.Gene getVariantFromGenes(Panda.Gene p_29261_, Panda.Gene p_29262_) {
            if (p_29261_.isRecessive()) {
                return p_29261_ == p_29262_ ? p_29261_ : NORMAL;
            } else {
                return p_29261_;
            }
        }

        public static Panda.Gene byId(int p_29249_) {
            return BY_ID.apply(p_29249_);
        }

        public static Panda.Gene getRandom(RandomSource p_218235_) {
            int i = p_218235_.nextInt(16);
            if (i == 0) {
                return LAZY;
            } else if (i == 1) {
                return WORRIED;
            } else if (i == 2) {
                return PLAYFUL;
            } else if (i == 4) {
                return AGGRESSIVE;
            } else if (i < 9) {
                return WEAK;
            } else {
                return i < 11 ? BROWN : NORMAL;
            }
        }
    }

    static class PandaAttackGoal extends MeleeAttackGoal {
        private final Panda panda;

        public PandaAttackGoal(Panda p_29269_, double p_29270_, boolean p_29271_) {
            super(p_29269_, p_29270_, p_29271_);
            this.panda = p_29269_;
        }

        @Override
        public boolean canUse() {
            return this.panda.canPerformAction() && super.canUse();
        }
    }

    static class PandaAvoidGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
        private final Panda panda;

        public PandaAvoidGoal(Panda p_29275_, Class<T> p_29276_, float p_29277_, double p_29278_, double p_29279_) {
            super(p_29275_, p_29276_, p_29277_, p_29278_, p_29279_, EntitySelector.NO_SPECTATORS::test);
            this.panda = p_29275_;
        }

        @Override
        public boolean canUse() {
            return this.panda.isWorried() && this.panda.canPerformAction() && super.canUse();
        }
    }

    static class PandaBreedGoal extends BreedGoal {
        private final Panda panda;
        private int unhappyCooldown;

        public PandaBreedGoal(Panda p_186221_, double p_186222_) {
            super(p_186221_, p_186222_);
            this.panda = p_186221_;
        }

        @Override
        public boolean canUse() {
            if (!super.canUse() || this.panda.getUnhappyCounter() != 0) {
                return false;
            } else if (!this.canFindBamboo()) {
                if (this.unhappyCooldown <= this.panda.tickCount) {
                    this.panda.setUnhappyCounter(32);
                    this.unhappyCooldown = this.panda.tickCount + 600;
                    if (this.panda.isEffectiveAi()) {
                        Player player = this.level.getNearestPlayer(Panda.BREED_TARGETING, this.panda);
                        this.panda.lookAtPlayerGoal.setTarget(player);
                    }
                }

                return false;
            } else {
                return true;
            }
        }

        private boolean canFindBamboo() {
            BlockPos blockpos = this.panda.blockPosition();
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 8; j++) {
                    for (int k = 0; k <= j; k = k > 0 ? -k : 1 - k) {
                        for (int l = k < j && k > -j ? j : 0; l <= j; l = l > 0 ? -l : 1 - l) {
                            blockpos$mutableblockpos.setWithOffset(blockpos, k, i, l);
                            if (this.level.getBlockState(blockpos$mutableblockpos).is(Blocks.BAMBOO)) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    static class PandaHurtByTargetGoal extends HurtByTargetGoal {
        private final Panda panda;

        public PandaHurtByTargetGoal(Panda p_29292_, Class<?>... p_29293_) {
            super(p_29292_, p_29293_);
            this.panda = p_29292_;
        }

        @Override
        public boolean canContinueToUse() {
            if (!this.panda.gotBamboo && !this.panda.didBite) {
                return super.canContinueToUse();
            } else {
                this.panda.setTarget(null);
                return false;
            }
        }

        @Override
        protected void alertOther(Mob p_29295_, LivingEntity p_29296_) {
            if (p_29295_ instanceof Panda && p_29295_.isAggressive()) {
                p_29295_.setTarget(p_29296_);
            }
        }
    }

    static class PandaLieOnBackGoal extends Goal {
        private final Panda panda;
        private int cooldown;

        public PandaLieOnBackGoal(Panda p_29301_) {
            this.panda = p_29301_;
        }

        @Override
        public boolean canUse() {
            return this.cooldown < this.panda.tickCount
                && this.panda.isLazy()
                && this.panda.canPerformAction()
                && this.panda.random.nextInt(reducedTickDelay(400)) == 1;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.panda.isInWater() && (this.panda.isLazy() || this.panda.random.nextInt(reducedTickDelay(600)) != 1)
                ? this.panda.random.nextInt(reducedTickDelay(2000)) != 1
                : false;
        }

        @Override
        public void start() {
            this.panda.setOnBack(true);
            this.cooldown = 0;
        }

        @Override
        public void stop() {
            this.panda.setOnBack(false);
            this.cooldown = this.panda.tickCount + 200;
        }
    }

    static class PandaLookAtPlayerGoal extends LookAtPlayerGoal {
        private final Panda panda;

        public PandaLookAtPlayerGoal(Panda p_29308_, Class<? extends LivingEntity> p_29309_, float p_29310_) {
            super(p_29308_, p_29309_, p_29310_);
            this.panda = p_29308_;
        }

        public void setTarget(LivingEntity p_29313_) {
            this.lookAt = p_29313_;
        }

        @Override
        public boolean canContinueToUse() {
            return this.lookAt != null && super.canContinueToUse();
        }

        @Override
        public boolean canUse() {
            if (this.mob.getRandom().nextFloat() >= this.probability) {
                return false;
            } else {
                if (this.lookAt == null) {
                    ServerLevel serverlevel = getServerLevel(this.mob);
                    if (this.lookAtType == Player.class) {
                        this.lookAt = serverlevel.getNearestPlayer(
                            this.lookAtContext, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ()
                        );
                    } else {
                        this.lookAt = serverlevel.getNearestEntity(
                            this.mob
                                .level()
                                .getEntitiesOfClass(this.lookAtType, this.mob.getBoundingBox().inflate(this.lookDistance, 3.0, this.lookDistance), p_148985_ -> true),
                            this.lookAtContext,
                            this.mob,
                            this.mob.getX(),
                            this.mob.getEyeY(),
                            this.mob.getZ()
                        );
                    }
                }

                return this.panda.canPerformAction() && this.lookAt != null;
            }
        }

        @Override
        public void tick() {
            if (this.lookAt != null) {
                super.tick();
            }
        }
    }

    static class PandaMoveControl extends MoveControl {
        private final Panda panda;

        public PandaMoveControl(Panda p_29318_) {
            super(p_29318_);
            this.panda = p_29318_;
        }

        @Override
        public void tick() {
            if (this.panda.canPerformAction()) {
                super.tick();
            }
        }
    }

    static class PandaPanicGoal extends PanicGoal {
        private final Panda panda;

        public PandaPanicGoal(Panda p_29322_, double p_29323_) {
            super(p_29322_, p_29323_, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES);
            this.panda = p_29322_;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.panda.isSitting()) {
                this.panda.getNavigation().stop();
                return false;
            } else {
                return super.canContinueToUse();
            }
        }
    }

    static class PandaRollGoal extends Goal {
        private final Panda panda;

        public PandaRollGoal(Panda p_29328_) {
            this.panda = p_29328_;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if ((this.panda.isBaby() || this.panda.isPlayful()) && this.panda.onGround()) {
                if (!this.panda.canPerformAction()) {
                    return false;
                } else {
                    float f = this.panda.getYRot() * (float) (Math.PI / 180.0);
                    float f1 = -Mth.sin(f);
                    float f2 = Mth.cos(f);
                    int i = Math.abs(f1) > 0.5 ? Mth.sign(f1) : 0;
                    int j = Math.abs(f2) > 0.5 ? Mth.sign(f2) : 0;
                    if (this.panda.level().getBlockState(this.panda.blockPosition().offset(i, -1, j)).isAir()) {
                        return true;
                    } else {
                        return this.panda.isPlayful() && this.panda.random.nextInt(reducedTickDelay(60)) == 1
                            ? true
                            : this.panda.random.nextInt(reducedTickDelay(500)) == 1;
                    }
                }
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.panda.roll(true);
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }
    }

    class PandaSitGoal extends Goal {
        private int cooldown;

        public PandaSitGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > Panda.this.tickCount || Panda.this.isBaby() || Panda.this.isInWater() || !Panda.this.canPerformAction() || Panda.this.getUnhappyCounter() > 0) {
                return false;
            } else {
                return !Panda.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
                    ? true
                    : !Panda.this.level().getEntitiesOfClass(ItemEntity.class, Panda.this.getBoundingBox().inflate(6.0, 6.0, 6.0), Panda::canPickUpAndEat).isEmpty();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !Panda.this.isInWater() && (Panda.this.isLazy() || Panda.this.random.nextInt(reducedTickDelay(600)) != 1)
                ? Panda.this.random.nextInt(reducedTickDelay(2000)) != 1
                : false;
        }

        @Override
        public void tick() {
            if (!Panda.this.isSitting() && !Panda.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                Panda.this.tryToSit();
            }
        }

        @Override
        public void start() {
            if (Panda.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                List<ItemEntity> list = Panda.this.level().getEntitiesOfClass(ItemEntity.class, Panda.this.getBoundingBox().inflate(8.0, 8.0, 8.0), Panda::canPickUpAndEat);
                if (!list.isEmpty()) {
                    Panda.this.getNavigation().moveTo(list.getFirst(), 1.2F);
                }
            } else {
                Panda.this.tryToSit();
            }

            this.cooldown = 0;
        }

        @Override
        public void stop() {
            ItemStack itemstack = Panda.this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!itemstack.isEmpty()) {
                Panda.this.spawnAtLocation(getServerLevel(Panda.this.level()), itemstack);
                Panda.this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                int i = Panda.this.isLazy() ? Panda.this.random.nextInt(50) + 10 : Panda.this.random.nextInt(150) + 10;
                this.cooldown = Panda.this.tickCount + i * 20;
            }

            Panda.this.sit(false);
        }
    }

    static class PandaSneezeGoal extends Goal {
        private final Panda panda;

        public PandaSneezeGoal(Panda p_29344_) {
            this.panda = p_29344_;
        }

        @Override
        public boolean canUse() {
            if (this.panda.isBaby() && this.panda.canPerformAction()) {
                return this.panda.isWeak() && this.panda.random.nextInt(reducedTickDelay(500)) == 1
                    ? true
                    : this.panda.random.nextInt(reducedTickDelay(6000)) == 1;
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.panda.sneeze(true);
        }
    }
}