package net.minecraft.world.entity.player;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public abstract class Player extends LivingEntity implements net.minecraftforge.common.extensions.IForgePlayer {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final HumanoidArm DEFAULT_MAIN_HAND = HumanoidArm.RIGHT;
    public static final int DEFAULT_MODEL_CUSTOMIZATION = 0;
    public static final int MAX_HEALTH = 20;
    public static final int SLEEP_DURATION = 100;
    public static final int WAKE_UP_DURATION = 10;
    public static final int ENDER_SLOT_OFFSET = 200;
    public static final int HELD_ITEM_SLOT = 499;
    public static final int CRAFTING_SLOT_OFFSET = 500;
    public static final float DEFAULT_BLOCK_INTERACTION_RANGE = 4.5F;
    public static final float DEFAULT_ENTITY_INTERACTION_RANGE = 3.0F;
    public static final float CROUCH_BB_HEIGHT = 1.5F;
    public static final float SWIMMING_BB_WIDTH = 0.6F;
    public static final float SWIMMING_BB_HEIGHT = 0.6F;
    public static final float DEFAULT_EYE_HEIGHT = 1.62F;
    private static final int CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME_TICKS = 40;
    public static final Vec3 DEFAULT_VEHICLE_ATTACHMENT = new Vec3(0.0, 0.6, 0.0);
    public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F)
        .withEyeHeight(1.62F)
        .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, DEFAULT_VEHICLE_ATTACHMENT));
    private static final Map<Pose, EntityDimensions> POSES = ImmutableMap.<Pose, EntityDimensions>builder()
        .put(Pose.STANDING, STANDING_DIMENSIONS)
        .put(Pose.SLEEPING, SLEEPING_DIMENSIONS)
        .put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
        .put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
        .put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
        .put(
            Pose.CROUCHING,
            EntityDimensions.scalable(0.6F, 1.5F).withEyeHeight(1.27F).withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, DEFAULT_VEHICLE_ATTACHMENT))
        )
        .put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(1.62F))
        .build();
    private static final EntityDataAccessor<Float> DATA_PLAYER_ABSORPTION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SCORE_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> DATA_PLAYER_MAIN_HAND = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_LEFT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
    protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_RIGHT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
    public static final int CLIENT_LOADED_TIMEOUT_TIME = 60;
    private static final short DEFAULT_SLEEP_TIMER = 0;
    private static final float DEFAULT_EXPERIENCE_PROGRESS = 0.0F;
    private static final int DEFAULT_EXPERIENCE_LEVEL = 0;
    private static final int DEFAULT_TOTAL_EXPERIENCE = 0;
    private static final int NO_ENCHANTMENT_SEED = 0;
    private static final int DEFAULT_SELECTED_SLOT = 0;
    private static final int DEFAULT_SCORE = 0;
    private static final boolean DEFAULT_IGNORE_FALL_DAMAGE_FROM_CURRENT_IMPULSE = false;
    private static final int DEFAULT_CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME = 0;
    private long timeEntitySatOnShoulder;
    final Inventory inventory;
    protected PlayerEnderChestContainer enderChestInventory = new PlayerEnderChestContainer();
    public final InventoryMenu inventoryMenu;
    public AbstractContainerMenu containerMenu;
    protected FoodData foodData = new FoodData();
    protected int jumpTriggerTime;
    private boolean clientLoaded = false;
    protected int clientLoadedTimeoutTimer = 60;
    public float oBob;
    public float bob;
    public int takeXpDelay;
    public double xCloakO;
    public double yCloakO;
    public double zCloakO;
    public double xCloak;
    public double yCloak;
    public double zCloak;
    private int sleepCounter = 0;
    protected boolean wasUnderwater;
    private final Abilities abilities = new Abilities();
    public int experienceLevel = 0;
    public int totalExperience = 0;
    public float experienceProgress = 0.0F;
    protected int enchantmentSeed = 0;
    protected final float defaultFlySpeed = 0.02F;
    private int lastLevelUpTime;
    private final GameProfile gameProfile;
    private boolean reducedDebugInfo;
    private ItemStack lastItemInMainHand = ItemStack.EMPTY;
    private final ItemCooldowns cooldowns = this.createItemCooldowns();
    private Optional<GlobalPos> lastDeathLocation = Optional.empty();
    @Nullable
    public FishingHook fishing;
    protected float hurtDir;
    @Nullable
    public Vec3 currentImpulseImpactPos;
    @Nullable
    public Entity currentExplosionCause;
    private boolean ignoreFallDamageFromCurrentImpulse = false;
    private int currentImpulseContextResetGraceTime = 0;
    private final java.util.Collection<MutableComponent> prefixes = new java.util.LinkedList<>();
    private final java.util.Collection<MutableComponent> suffixes = new java.util.LinkedList<>();
    @Nullable private Pose forcedPose;

    public Player(Level p_250508_, GameProfile p_252153_) {
        super(EntityType.PLAYER, p_250508_);
        this.setUUID(p_252153_.getId());
        this.gameProfile = p_252153_;
        this.inventory = new Inventory(this, this.equipment);
        this.inventoryMenu = new InventoryMenu(this.inventory, !p_250508_.isClientSide, this);
        this.containerMenu = this.inventoryMenu;

        // Forge: wrapped inventories moved to constructor as the inventory init was moved in here too
        this.playerMainHandler = net.minecraftforge.common.util.LazyOptional.of(
            () -> new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(inventory));
        this.playerEquipmentHandler = net.minecraftforge.common.util.LazyOptional.of(
            () -> new net.minecraftforge.items.wrapper.PlayerEquipmentInvWrapper(inventory));
        this.playerJoinedHandler = net.minecraftforge.common.util.LazyOptional.of(
            () -> new net.minecraftforge.items.wrapper.CombinedInvWrapper(
                new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(inventory),
                new net.minecraftforge.items.wrapper.PlayerEquipmentInvWrapper(inventory)
            ));
    }

    @Override
    protected EntityEquipment createEquipment() {
        return new PlayerEquipment(this);
    }

    public boolean blockActionRestricted(Level p_36188_, BlockPos p_36189_, GameType p_36190_) {
        if (!p_36190_.isBlockPlacingRestricted()) {
            return false;
        } else if (p_36190_ == GameType.SPECTATOR) {
            return true;
        } else if (this.mayBuild()) {
            return false;
        } else {
            ItemStack itemstack = this.getMainHandItem();
            return itemstack.isEmpty() || !itemstack.canBreakBlockInAdventureMode(new BlockInWorld(p_36188_, p_36189_, false));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.1F)
            .add(Attributes.ATTACK_SPEED)
            .add(Attributes.LUCK)
            .add(Attributes.BLOCK_INTERACTION_RANGE, 4.5)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 3.0)
            .add(Attributes.BLOCK_BREAK_SPEED)
            .add(Attributes.SUBMERGED_MINING_SPEED)
            .add(Attributes.SNEAKING_SPEED)
            .add(Attributes.MINING_EFFICIENCY)
            .add(Attributes.SWEEPING_DAMAGE_RATIO)
            .add(Attributes.WAYPOINT_TRANSMIT_RANGE, 6.0E7)
            .add(Attributes.WAYPOINT_RECEIVE_RANGE, 6.0E7)
            .add(Attributes.ATTACK_KNOCKBACK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_335298_) {
        super.defineSynchedData(p_335298_);
        p_335298_.define(DATA_PLAYER_ABSORPTION_ID, 0.0F);
        p_335298_.define(DATA_SCORE_ID, 0);
        p_335298_.define(DATA_PLAYER_MODE_CUSTOMISATION, (byte)0);
        p_335298_.define(DATA_PLAYER_MAIN_HAND, (byte)DEFAULT_MAIN_HAND.getId());
        p_335298_.define(DATA_SHOULDER_LEFT, new CompoundTag());
        p_335298_.define(DATA_SHOULDER_RIGHT, new CompoundTag());
    }

    @Override
    public void tick() {
        net.minecraftforge.event.ForgeEventFactory.onPlayerPreTick(this);
        this.noPhysics = this.isSpectator();
        if (this.isSpectator() || this.isPassenger()) {
            this.setOnGround(false);
        }

        if (this.takeXpDelay > 0) {
            this.takeXpDelay--;
        }

        if (this.isSleeping()) {
            this.sleepCounter++;
            if (this.sleepCounter > 100) {
                this.sleepCounter = 100;
            }

            if (!this.level().isClientSide && !net.minecraftforge.event.ForgeEventFactory.onSleepingTimeCheck(this, getSleepingPos())) {
                this.stopSleepInBed(false, true);
            }
        } else if (this.sleepCounter > 0) {
            this.sleepCounter++;
            if (this.sleepCounter >= 110) {
                this.sleepCounter = 0;
            }
        }

        this.updateIsUnderwater();
        super.tick();
        if (!this.level().isClientSide && this.containerMenu != null && !this.containerMenu.stillValid(this)) {
            this.closeContainer();
            this.containerMenu = this.inventoryMenu;
        }

        this.moveCloak();
        if (this instanceof ServerPlayer serverplayer) {
            this.foodData.tick(serverplayer);
            this.awardStat(Stats.PLAY_TIME);
            this.awardStat(Stats.TOTAL_WORLD_TIME);
            if (this.isAlive()) {
                this.awardStat(Stats.TIME_SINCE_DEATH);
            }

            if (this.isDiscrete()) {
                this.awardStat(Stats.CROUCH_TIME);
            }

            if (!this.isSleeping()) {
                this.awardStat(Stats.TIME_SINCE_REST);
            }
        }

        int i = 29999999;
        double d0 = Mth.clamp(this.getX(), -2.9999999E7, 2.9999999E7);
        double d1 = Mth.clamp(this.getZ(), -2.9999999E7, 2.9999999E7);
        if (d0 != this.getX() || d1 != this.getZ()) {
            this.setPos(d0, this.getY(), d1);
        }

        this.attackStrengthTicker++;
        ItemStack itemstack = this.getMainHandItem();
        if (!ItemStack.matches(this.lastItemInMainHand, itemstack)) {
            if (!ItemStack.isSameItem(this.lastItemInMainHand, itemstack)) {
                this.resetAttackStrengthTicker();
            }

            this.lastItemInMainHand = itemstack.copy();
        }

        if (!this.isEyeInFluid(FluidTags.WATER) && this.isEquipped(Items.TURTLE_HELMET)) {
            this.turtleHelmetTick();
        }

        this.cooldowns.tick();
        this.updatePlayerPose();
        if (this.currentImpulseContextResetGraceTime > 0) {
            this.currentImpulseContextResetGraceTime--;
        }
        net.minecraftforge.event.ForgeEventFactory.onPlayerPostTick(this);
    }

    @Override
    protected float getMaxHeadRotationRelativeToBody() {
        return this.isBlocking() ? 15.0F : super.getMaxHeadRotationRelativeToBody();
    }

    public boolean isSecondaryUseActive() {
        return this.isShiftKeyDown();
    }

    protected boolean wantsToStopRiding() {
        return this.isShiftKeyDown();
    }

    protected boolean isStayingOnGroundSurface() {
        return this.isShiftKeyDown();
    }

    protected boolean updateIsUnderwater() {
        this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
        return this.wasUnderwater;
    }

    @Override
    public void onAboveBubbleColumn(boolean p_397973_, BlockPos p_391484_) {
        if (!this.getAbilities().flying) {
            super.onAboveBubbleColumn(p_397973_, p_391484_);
        }
    }

    @Override
    public void onInsideBubbleColumn(boolean p_369072_) {
        if (!this.getAbilities().flying) {
            super.onInsideBubbleColumn(p_369072_);
        }
    }

    private void turtleHelmetTick() {
        this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
    }

    private boolean isEquipped(Item p_365145_) {
        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
            if (itemstack.is(p_365145_) && equippable != null && equippable.slot() == equipmentslot) {
                return true;
            }
        }

        return false;
    }

    protected ItemCooldowns createItemCooldowns() {
        return new ItemCooldowns();
    }

    private void moveCloak() {
        this.xCloakO = this.xCloak;
        this.yCloakO = this.yCloak;
        this.zCloakO = this.zCloak;
        double d0 = this.getX() - this.xCloak;
        double d1 = this.getY() - this.yCloak;
        double d2 = this.getZ() - this.zCloak;
        double d3 = 10.0;
        if (d0 > 10.0) {
            this.xCloak = this.getX();
            this.xCloakO = this.xCloak;
        }

        if (d2 > 10.0) {
            this.zCloak = this.getZ();
            this.zCloakO = this.zCloak;
        }

        if (d1 > 10.0) {
            this.yCloak = this.getY();
            this.yCloakO = this.yCloak;
        }

        if (d0 < -10.0) {
            this.xCloak = this.getX();
            this.xCloakO = this.xCloak;
        }

        if (d2 < -10.0) {
            this.zCloak = this.getZ();
            this.zCloakO = this.zCloak;
        }

        if (d1 < -10.0) {
            this.yCloak = this.getY();
            this.yCloakO = this.yCloak;
        }

        this.xCloak += d0 * 0.25;
        this.zCloak += d2 * 0.25;
        this.yCloak += d1 * 0.25;
    }

    protected void updatePlayerPose() {
        if (forcedPose != null) {
            this.setPose(forcedPose);
            return;
        }
        if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
            Pose pose = this.getDesiredPose();
            Pose pose1;
            if (this.isSpectator() || this.isPassenger() || this.canPlayerFitWithinBlocksAndEntitiesWhen(pose)) {
                pose1 = pose;
            } else if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)) {
                pose1 = Pose.CROUCHING;
            } else {
                pose1 = Pose.SWIMMING;
            }

            this.setPose(pose1);
        }
    }

    private Pose getDesiredPose() {
        if (this.isSleeping()) {
            return Pose.SLEEPING;
        } else if (this.isSwimming()) {
            return Pose.SWIMMING;
        } else if (this.isFallFlying()) {
            return Pose.FALL_FLYING;
        } else if (this.isAutoSpinAttack()) {
            return Pose.SPIN_ATTACK;
        } else {
            return this.isShiftKeyDown() && !this.abilities.flying ? Pose.CROUCHING : Pose.STANDING;
        }
    }

    protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose p_297636_) {
        return this.level().noCollision(this, this.getDimensions(p_297636_).makeBoundingBox(this.position()).deflate(1.0E-7));
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.PLAYER_SWIM;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.PLAYER_SPLASH;
    }

    @Override
    protected SoundEvent getSwimHighSpeedSplashSound() {
        return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
    }

    @Override
    public int getDimensionChangingDelay() {
        return 10;
    }

    @Override
    public void playSound(SoundEvent p_36137_, float p_36138_, float p_36139_) {
        this.level().playSound(this, this.getX(), this.getY(), this.getZ(), p_36137_, this.getSoundSource(), p_36138_, p_36139_);
    }

    public void playNotifySound(SoundEvent p_36140_, SoundSource p_36141_, float p_36142_, float p_36143_) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.PLAYERS;
    }

    @Override
    protected int getFireImmuneTicks() {
        return 20;
    }

    @Override
    public void handleEntityEvent(byte p_36120_) {
        if (p_36120_ == 9) {
            this.completeUsingItem();
        } else if (p_36120_ == 23) {
            this.reducedDebugInfo = false;
        } else if (p_36120_ == 22) {
            this.reducedDebugInfo = true;
        } else {
            super.handleEntityEvent(p_36120_);
        }
    }

    public void closeContainer() {
        this.containerMenu = this.inventoryMenu;
    }

    protected void doCloseContainer() {
    }

    @Override
    public void rideTick() {
        if (!this.level().isClientSide && this.wantsToStopRiding() && this.isPassenger()) {
            this.stopRiding();
            this.setShiftKeyDown(false);
        } else {
            super.rideTick();
            this.oBob = this.bob;
            this.bob = 0.0F;
        }
    }

    @Override
    public void aiStep() {
        if (this.jumpTriggerTime > 0) {
            this.jumpTriggerTime--;
        }

        this.tickRegeneration();
        this.inventory.tick();
        this.oBob = this.bob;
        if (this.abilities.flying && !this.isPassenger()) {
            this.resetFallDistance();
        }

        super.aiStep();
        this.updateSwingTime();
        this.yHeadRot = this.getYRot();
        this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
        float f;
        if (this.onGround() && !this.isDeadOrDying() && !this.isSwimming()) {
            f = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
        } else {
            f = 0.0F;
        }

        this.bob = this.bob + (f - this.bob) * 0.4F;
        if (this.getHealth() > 0.0F && !this.isSpectator()) {
            AABB aabb;
            if (this.isPassenger() && !this.getVehicle().isRemoved()) {
                aabb = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).inflate(1.0, 0.0, 1.0);
            } else {
                aabb = this.getBoundingBox().inflate(1.0, 0.5, 1.0);
            }

            List<Entity> list = this.level().getEntities(this, aabb);
            List<Entity> list1 = Lists.newArrayList();

            for (Entity entity : list) {
                if (entity.getType() == EntityType.EXPERIENCE_ORB) {
                    list1.add(entity);
                } else if (!entity.isRemoved()) {
                    this.touch(entity);
                }
            }

            if (!list1.isEmpty()) {
                this.touch(Util.getRandom(list1, this.random));
            }
        }

        this.playShoulderEntityAmbientSound(this.getShoulderEntityLeft());
        this.playShoulderEntityAmbientSound(this.getShoulderEntityRight());
        if (!this.level().isClientSide && (this.fallDistance > 0.5 || this.isInWater()) || this.abilities.flying || this.isSleeping() || this.isInPowderSnow) {
            this.removeEntitiesOnShoulder();
        }
    }

    protected void tickRegeneration() {
    }

    private void playShoulderEntityAmbientSound(CompoundTag p_36368_) {
        if (!p_36368_.isEmpty() && !p_36368_.getBooleanOr("Silent", false)) {
            if (this.level().random.nextInt(200) == 0) {
                EntityType<?> entitytype = p_36368_.read("id", EntityType.CODEC).orElse(null);
                if (entitytype == EntityType.PARROT && !Parrot.imitateNearbyMobs(this.level(), this)) {
                    this.level()
                        .playSound(
                            null,
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            Parrot.getAmbient(this.level(), this.level().random),
                            this.getSoundSource(),
                            1.0F,
                            Parrot.getPitch(this.level().random)
                        );
                }
            }
        }
    }

    private void touch(Entity p_36278_) {
        p_36278_.playerTouch(this);
    }

    public int getScore() {
        return this.entityData.get(DATA_SCORE_ID);
    }

    public void setScore(int p_36398_) {
        this.entityData.set(DATA_SCORE_ID, p_36398_);
    }

    public void increaseScore(int p_36402_) {
        int i = this.getScore();
        this.entityData.set(DATA_SCORE_ID, i + p_36402_);
    }

    public void startAutoSpinAttack(int p_204080_, float p_344736_, ItemStack p_343326_) {
        this.autoSpinAttackTicks = p_204080_;
        this.autoSpinAttackDmg = p_344736_;
        this.autoSpinAttackItemStack = p_343326_;
        if (!this.level().isClientSide) {
            this.removeEntitiesOnShoulder();
            this.setLivingEntityFlag(4, true);
        }
    }

    @Nonnull
    @Override
    public ItemStack getWeaponItem() {
        return this.isAutoSpinAttack() && this.autoSpinAttackItemStack != null ? this.autoSpinAttackItemStack : super.getWeaponItem();
    }

    @Override
    public void die(DamageSource p_36152_) {
        if (net.minecraftforge.event.ForgeEventFactory.onLivingDeath(this, p_36152_)) return;
        super.die(p_36152_);
        this.reapplyPosition();
        if (!this.isSpectator() && this.level() instanceof ServerLevel serverlevel) {
            this.dropAllDeathLoot(serverlevel, p_36152_);
        }

        if (p_36152_ != null) {
            this.setDeltaMovement(
                -Mth.cos((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F,
                0.1F,
                -Mth.sin((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F
            );
        } else {
            this.setDeltaMovement(0.0, 0.1, 0.0);
        }

        this.awardStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.clearFire();
        this.setSharedFlagOnFire(false);
        this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level().dimension(), this.blockPosition())));
    }

    @Override
    protected void dropEquipment(ServerLevel p_369623_) {
        super.dropEquipment(p_369623_);
        if (!p_369623_.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            this.destroyVanishingCursedItems();
            this.inventory.dropAll();
        }
    }

    protected void destroyVanishingCursedItems() {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty() && EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                this.inventory.removeItemNoUpdate(i);
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource p_36310_) {
        return p_36310_.type().effects().sound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    public void handleCreativeModeItemDrop(ItemStack p_369068_) {
    }

    @Nullable
    public ItemEntity drop(ItemStack p_36177_, boolean p_36178_) {
        return net.minecraftforge.common.ForgeHooks.onPlayerTossEvent(this, p_36177_, p_36178_);
    }

    /** @deprecated Use {@link #getDestroySpeed(BlockState,BlockPos) */
    public float getDestroySpeed(BlockState p_36282_) {
        return getDestroySpeed(p_36282_, null);
    }

    public float getDestroySpeed(BlockState p_36282_, @Nullable BlockPos pos) {
        float f = this.inventory.getSelectedItem().getDestroySpeed(p_36282_);
        if (f > 1.0F) {
            f += (float)this.getAttributeValue(Attributes.MINING_EFFICIENCY);
        }

        if (MobEffectUtil.hasDigSpeed(this)) {
            f *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
        }

        if (this.hasEffect(MobEffects.MINING_FATIGUE)) {
            float f1 = switch (this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
            f *= f1;
        }

        f *= (float)this.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        if (this.isEyeInFluid(FluidTags.WATER)) {
            f *= (float)this.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
        }

        if (!this.onGround()) {
            f /= 5.0F;
        }

        f = net.minecraftforge.event.ForgeEventFactory.getBreakSpeed(this, p_36282_, f, pos);

        return f;
    }

    public boolean hasCorrectToolForDrops(BlockState p_36299_) {
        var vanilla = !p_36299_.requiresCorrectToolForDrops() || this.inventory.getSelectedItem().isCorrectToolForDrops(p_36299_);
        return net.minecraftforge.event.ForgeEventFactory.doPlayerHarvestCheck(this, p_36299_, vanilla);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_410352_) {
        super.readAdditionalSaveData(p_410352_);
        this.setUUID(this.gameProfile.getId());
        this.inventory.load(p_410352_.listOrEmpty("Inventory", ItemStackWithSlot.CODEC));
        this.inventory.setSelectedSlot(p_410352_.getIntOr("SelectedItemSlot", 0));
        this.sleepCounter = p_410352_.getShortOr("SleepTimer", (short)0);
        this.experienceProgress = p_410352_.getFloatOr("XpP", 0.0F);
        this.experienceLevel = p_410352_.getIntOr("XpLevel", 0);
        this.totalExperience = p_410352_.getIntOr("XpTotal", 0);
        this.enchantmentSeed = p_410352_.getIntOr("XpSeed", 0);
        if (this.enchantmentSeed == 0) {
            this.enchantmentSeed = this.random.nextInt();
        }

        this.setScore(p_410352_.getIntOr("Score", 0));
        this.foodData.readAdditionalSaveData(p_410352_);
        p_410352_.read("abilities", Abilities.Packed.CODEC).ifPresent(this.abilities::apply);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(this.abilities.getWalkingSpeed());
        this.enderChestInventory.fromSlots(p_410352_.listOrEmpty("EnderItems", ItemStackWithSlot.CODEC));
        this.setShoulderEntityLeft(p_410352_.read("ShoulderEntityLeft", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        this.setShoulderEntityRight(p_410352_.read("ShoulderEntityRight", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        this.setLastDeathLocation(p_410352_.read("LastDeathLocation", GlobalPos.CODEC));
        this.currentImpulseImpactPos = p_410352_.read("current_explosion_impact_pos", Vec3.CODEC).orElse(null);
        this.ignoreFallDamageFromCurrentImpulse = p_410352_.getBooleanOr("ignore_fall_damage_from_current_explosion", false);
        this.currentImpulseContextResetGraceTime = p_410352_.getIntOr("current_impulse_context_reset_grace_time", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_406026_) {
        super.addAdditionalSaveData(p_406026_);
        NbtUtils.addCurrentDataVersion(p_406026_);
        this.inventory.save(p_406026_.list("Inventory", ItemStackWithSlot.CODEC));
        p_406026_.putInt("SelectedItemSlot", this.inventory.getSelectedSlot());
        p_406026_.putShort("SleepTimer", (short)this.sleepCounter);
        p_406026_.putFloat("XpP", this.experienceProgress);
        p_406026_.putInt("XpLevel", this.experienceLevel);
        p_406026_.putInt("XpTotal", this.totalExperience);
        p_406026_.putInt("XpSeed", this.enchantmentSeed);
        p_406026_.putInt("Score", this.getScore());
        this.foodData.addAdditionalSaveData(p_406026_);
        p_406026_.store("abilities", Abilities.Packed.CODEC, this.abilities.pack());
        this.enderChestInventory.storeAsSlots(p_406026_.list("EnderItems", ItemStackWithSlot.CODEC));
        if (!this.getShoulderEntityLeft().isEmpty()) {
            p_406026_.store("ShoulderEntityLeft", CompoundTag.CODEC, this.getShoulderEntityLeft());
        }

        if (!this.getShoulderEntityRight().isEmpty()) {
            p_406026_.store("ShoulderEntityRight", CompoundTag.CODEC, this.getShoulderEntityRight());
        }

        this.lastDeathLocation.ifPresent(p_405554_ -> p_406026_.store("LastDeathLocation", GlobalPos.CODEC, p_405554_));
        p_406026_.storeNullable("current_explosion_impact_pos", Vec3.CODEC, this.currentImpulseImpactPos);
        p_406026_.putBoolean("ignore_fall_damage_from_current_explosion", this.ignoreFallDamageFromCurrentImpulse);
        p_406026_.putInt("current_impulse_context_reset_grace_time", this.currentImpulseContextResetGraceTime);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel p_360775_, DamageSource p_36249_) {
        if (super.isInvulnerableTo(p_360775_, p_36249_)) {
            return true;
        } else if (p_36249_.is(DamageTypeTags.IS_DROWNING)) {
            return !p_360775_.getGameRules().getBoolean(GameRules.RULE_DROWNING_DAMAGE);
        } else if (p_36249_.is(DamageTypeTags.IS_FALL)) {
            return !p_360775_.getGameRules().getBoolean(GameRules.RULE_FALL_DAMAGE);
        } else if (p_36249_.is(DamageTypeTags.IS_FIRE)) {
            return !p_360775_.getGameRules().getBoolean(GameRules.RULE_FIRE_DAMAGE);
        } else {
            return p_36249_.is(DamageTypeTags.IS_FREEZING) ? !p_360775_.getGameRules().getBoolean(GameRules.RULE_FREEZE_DAMAGE) : false;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel p_369360_, DamageSource p_364544_, float p_368576_) {
        if (!net.minecraftforge.common.ForgeHooks.onPlayerAttack(this, p_364544_, p_368576_)) return false;
        if (this.isInvulnerableTo(p_369360_, p_364544_)) {
            return false;
        } else if (this.abilities.invulnerable && !p_364544_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            this.noActionTime = 0;
            if (this.isDeadOrDying()) {
                return false;
            } else {
                this.removeEntitiesOnShoulder();
                if (p_364544_.scalesWithDifficulty()) {
                    if (p_369360_.getDifficulty() == Difficulty.PEACEFUL) {
                        p_368576_ = 0.0F;
                    }

                    if (p_369360_.getDifficulty() == Difficulty.EASY) {
                        p_368576_ = Math.min(p_368576_ / 2.0F + 1.0F, p_368576_);
                    }

                    if (p_369360_.getDifficulty() == Difficulty.HARD) {
                        p_368576_ = p_368576_ * 3.0F / 2.0F;
                    }
                }

                return p_368576_ == 0.0F ? false : super.hurtServer(p_369360_, p_364544_, p_368576_);
            }
        }
    }

    @Override
    protected void blockUsingItem(ServerLevel p_395321_, LivingEntity p_395720_) {
        super.blockUsingItem(p_395321_, p_395720_);
        ItemStack itemstack = this.getItemBlockingWith();
        BlocksAttacks blocksattacks = itemstack != null ? itemstack.get(DataComponents.BLOCKS_ATTACKS) : null;
        float f = p_395720_.getSecondsToDisableBlocking();
        if (f > 0.0F && blocksattacks != null) {
            blocksattacks.disable(p_395321_, this, f, itemstack);
        }
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
    }

    public boolean canHarmPlayer(Player p_36169_) {
        Team team = this.getTeam();
        Team team1 = p_36169_.getTeam();
        if (team == null) {
            return true;
        } else {
            return !team.isAlliedTo(team1) ? true : team.isAllowFriendlyFire();
        }
    }

    @Override
    protected void hurtArmor(DamageSource p_36251_, float p_36252_) {
        this.doHurtEquipment(p_36251_, p_36252_, new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD});
    }

    @Override
    protected void hurtHelmet(DamageSource p_150103_, float p_150104_) {
        this.doHurtEquipment(p_150103_, p_150104_, new EquipmentSlot[]{EquipmentSlot.HEAD});
    }

    @Override
    protected void actuallyHurt(ServerLevel p_365751_, DamageSource p_36312_, float p_36313_) {
        if (!this.isInvulnerableTo(p_365751_, p_36312_)) {
            p_36313_ = net.minecraftforge.common.ForgeHooks.onLivingHurt(this, p_36312_, p_36313_);
            if (p_36313_ <= 0) return;
            p_36313_ = this.getDamageAfterArmorAbsorb(p_36312_, p_36313_);
            p_36313_ = this.getDamageAfterMagicAbsorb(p_36312_, p_36313_);
            float f1 = Math.max(p_36313_ - this.getAbsorptionAmount(), 0.0F);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - (p_36313_ - f1));
            f1 = net.minecraftforge.common.ForgeHooks.onLivingDamage(this, p_36312_, f1);
            float f = p_36313_ - f1;
            if (f > 0.0F && f < 3.4028235E37F) {
                this.awardStat(Stats.DAMAGE_ABSORBED, Math.round(f * 10.0F));
            }

            if (f1 != 0.0F) {
                this.causeFoodExhaustion(p_36312_.getFoodExhaustion());
                this.getCombatTracker().recordDamage(p_36312_, f1);
                this.setHealth(this.getHealth() - f1);
                if (f1 < 3.4028235E37F) {
                    this.awardStat(Stats.DAMAGE_TAKEN, Math.round(f1 * 10.0F));
                }

                this.gameEvent(GameEvent.ENTITY_DAMAGE);
            }
        }
    }

    public boolean isTextFilteringEnabled() {
        return false;
    }

    public void openTextEdit(SignBlockEntity p_36193_, boolean p_277837_) {
    }

    public void openMinecartCommandBlock(BaseCommandBlock p_36182_) {
    }

    public void openCommandBlock(CommandBlockEntity p_36191_) {
    }

    public void openStructureBlock(StructureBlockEntity p_36194_) {
    }

    public void openTestBlock(TestBlockEntity p_396402_) {
    }

    public void openTestInstanceBlock(TestInstanceBlockEntity p_391756_) {
    }

    public void openJigsawBlock(JigsawBlockEntity p_36192_) {
    }

    public void openHorseInventory(AbstractHorse p_36167_, Container p_36168_) {
    }

    public OptionalInt openMenu(@Nullable MenuProvider p_36150_) {
        return OptionalInt.empty();
    }

    public void openDialog(Holder<Dialog> p_410470_) {
    }

    public void sendMerchantOffers(int p_36121_, MerchantOffers p_36122_, int p_36123_, int p_36124_, boolean p_36125_, boolean p_36126_) {
    }

    public void openItemGui(ItemStack p_36174_, InteractionHand p_36175_) {
    }

    public InteractionResult interactOn(Entity p_36158_, InteractionHand p_36159_) {
        if (this.isSpectator()) {
            if (p_36158_ instanceof MenuProvider) {
                this.openMenu((MenuProvider)p_36158_);
            }

            return InteractionResult.PASS;
        } else {
            var event = new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(this, p_36159_, p_36158_);
            if (net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract.BUS.post(event)) {
                return event.getCancellationResult();
            }
            ItemStack itemstack = this.getItemInHand(p_36159_);
            ItemStack itemstack1 = itemstack.copy();
            InteractionResult interactionresult = p_36158_.interact(this, p_36159_);
            if (interactionresult.consumesAction()) {
                if (this.hasInfiniteMaterials() && itemstack == this.getItemInHand(p_36159_) && itemstack.getCount() < itemstack1.getCount()) {
                    itemstack.setCount(itemstack1.getCount());
                }

                if (!this.abilities.instabuild && itemstack.isEmpty()) {
                    net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(this, itemstack1, getSlotForHand(p_36159_));
                }

                return interactionresult;
            } else {
                if (!itemstack.isEmpty() && p_36158_ instanceof LivingEntity) {
                    if (this.hasInfiniteMaterials()) {
                        itemstack = itemstack1;
                    }

                    InteractionResult interactionresult1 = itemstack.interactLivingEntity(this, (LivingEntity)p_36158_, p_36159_);
                    if (interactionresult1.consumesAction()) {
                        this.level().gameEvent(GameEvent.ENTITY_INTERACT, p_36158_.position(), GameEvent.Context.of(this));
                        if (itemstack.isEmpty() && !this.hasInfiniteMaterials()) {
                            net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(this, itemstack1, getSlotForHand(p_36159_));
                            this.setItemInHand(p_36159_, ItemStack.EMPTY);
                        }

                        return interactionresult1;
                    }
                }

                return InteractionResult.PASS;
            }
        }
    }

    @Override
    public void removeVehicle() {
        super.removeVehicle();
        this.boardingCooldown = 0;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isSleeping();
    }

    @Override
    public boolean isAffectedByFluids() {
        return !this.abilities.flying;
    }

    @Override
    protected Vec3 maybeBackOffFromEdge(Vec3 p_36201_, MoverType p_36202_) {
        float f = this.maxUpStep();
        if (!this.abilities.flying
            && !(p_36201_.y > 0.0)
            && (p_36202_ == MoverType.SELF || p_36202_ == MoverType.PLAYER)
            && this.isStayingOnGroundSurface()
            && this.isAboveGround(f)) {
            double d0 = p_36201_.x;
            double d1 = p_36201_.z;
            double d2 = 0.05;
            double d3 = Math.signum(d0) * 0.05;

            double d4;
            for (d4 = Math.signum(d1) * 0.05; d0 != 0.0 && this.canFallAtLeast(d0, 0.0, f); d0 -= d3) {
                if (Math.abs(d0) <= 0.05) {
                    d0 = 0.0;
                    break;
                }
            }

            while (d1 != 0.0 && this.canFallAtLeast(0.0, d1, f)) {
                if (Math.abs(d1) <= 0.05) {
                    d1 = 0.0;
                    break;
                }

                d1 -= d4;
            }

            while (d0 != 0.0 && d1 != 0.0 && this.canFallAtLeast(d0, d1, f)) {
                if (Math.abs(d0) <= 0.05) {
                    d0 = 0.0;
                } else {
                    d0 -= d3;
                }

                if (Math.abs(d1) <= 0.05) {
                    d1 = 0.0;
                } else {
                    d1 -= d4;
                }
            }

            return new Vec3(d0, p_36201_.y, d1);
        } else {
            return p_36201_;
        }
    }

    private boolean isAboveGround(float p_328745_) {
        return this.onGround() || this.fallDistance < p_328745_ && !this.canFallAtLeast(0.0, 0.0, p_328745_ - this.fallDistance);
    }

    private boolean canFallAtLeast(double p_333341_, double p_331138_, double p_396282_) {
        AABB aabb = this.getBoundingBox();
        return this.level()
            .noCollision(
                this,
                new AABB(
                    aabb.minX + 1.0E-7 + p_333341_,
                    aabb.minY - p_396282_ - 1.0E-7,
                    aabb.minZ + 1.0E-7 + p_331138_,
                    aabb.maxX - 1.0E-7 + p_333341_,
                    aabb.minY,
                    aabb.maxZ - 1.0E-7 + p_331138_
                )
            );
    }

    public void attack(Entity p_36347_) {
        if (!net.minecraftforge.common.ForgeHooks.onPlayerAttackTarget(this, p_36347_)) return;
        if (p_36347_.isAttackable()) {
            if (!p_36347_.skipAttackInteraction(this)) {
                float f = this.isAutoSpinAttack() ? this.autoSpinAttackDmg : (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                ItemStack itemstack = this.getWeaponItem();
                DamageSource damagesource = Optional.ofNullable(itemstack.getItem().getDamageSource(this)).orElse(this.damageSources().playerAttack(this));
                float f1 = this.getEnchantedDamage(p_36347_, f, damagesource) - f;
                float f2 = this.getAttackStrengthScale(0.5F);
                f *= 0.2F + f2 * f2 * 0.8F;
                f1 *= f2;
                this.resetAttackStrengthTicker();
                if (p_36347_.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE)
                    && p_36347_ instanceof Projectile projectile
                    && projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this, this, true)) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource());
                } else {
                    if (f > 0.0F || f1 > 0.0F) {
                        boolean flag3 = f2 > 0.9F;
                        boolean flag;
                        if (this.isSprinting() && flag3) {
                            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F);
                            flag = true;
                        } else {
                            flag = false;
                        }

                        f += itemstack.getItem().getAttackDamageBonus(p_36347_, f, damagesource);
                        boolean flag1 = flag3
                            && this.fallDistance > 0.0
                            && !this.onGround()
                            && !this.onClimbable()
                            && !this.isInWater()
                            && !this.hasEffect(MobEffects.BLINDNESS)
                            && !this.isPassenger()
                            && p_36347_ instanceof LivingEntity
                            && !this.isSprinting();
                        var hitResult = net.minecraftforge.common.ForgeHooks.getCriticalHit(this, p_36347_, flag1, flag1 ? 1.5F : 1.0F);
                        flag1 = hitResult != null;
                        if (flag1) {
                            f *= hitResult.getDamageModifier();
                        }

                        float f3 = f + f1;
                        boolean flag2 = false;
                        if (flag3 && !flag1 && !flag && this.onGround()) {
                            double d0 = this.getKnownMovement().horizontalDistanceSqr();
                            double d1 = this.getSpeed() * 2.5;
                            if (d0 < Mth.square(d1) && this.getItemInHand(InteractionHand.MAIN_HAND).canPerformAction(net.minecraftforge.common.ToolActions.SWORD_SWEEP)) {
                                flag2 = true;
                            }
                        }

                        float f5 = 0.0F;
                        if (p_36347_ instanceof LivingEntity livingentity) {
                            f5 = livingentity.getHealth();
                        }

                        Vec3 vec3 = p_36347_.getDeltaMovement();
                        boolean flag4 = p_36347_.hurtOrSimulate(damagesource, f3);
                        if (flag4) {
                            float f4 = this.getKnockback(p_36347_, damagesource) + (flag ? 1.0F : 0.0F);
                            if (f4 > 0.0F) {
                                if (p_36347_ instanceof LivingEntity livingentity1) {
                                    livingentity1.knockback(
                                        f4 * 0.5F,
                                        Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)),
                                        -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0))
                                    );
                                } else {
                                    p_36347_.push(
                                        -Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)) * f4 * 0.5F,
                                        0.1,
                                        Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * f4 * 0.5F
                                    );
                                }

                                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                                this.setSprinting(false);
                            }

                            if (flag2) {
                                float f6 = 1.0F + (float)this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * f;

                                for (LivingEntity livingentity2 : this.level().getEntitiesOfClass(LivingEntity.class, this.getItemInHand(InteractionHand.MAIN_HAND).getSweepHitBox(this, p_36347_))) {
                                    if (livingentity2 != this
                                        && livingentity2 != p_36347_
                                        && !this.isAlliedTo(livingentity2)
                                        && !(livingentity2 instanceof ArmorStand armorstand && armorstand.isMarker())
                                        && this.distanceToSqr(livingentity2) < 9.0) {
                                        float f8 = this.getEnchantedDamage(livingentity2, f6, damagesource) * f2;
                                        if (this.level() instanceof ServerLevel serverlevel && livingentity2.hurtServer(serverlevel, damagesource, f8)) {
                                            livingentity2.knockback(
                                                0.4F,
                                                Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)),
                                                -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0))
                                            );
                                            EnchantmentHelper.doPostAttackEffects(serverlevel, livingentity2, damagesource);
                                        }
                                    }
                                }

                                this.level()
                                    .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F);
                                this.sweepAttack();
                            }

                            if (p_36347_ instanceof ServerPlayer && p_36347_.hurtMarked) {
                                ((ServerPlayer)p_36347_).connection.send(new ClientboundSetEntityMotionPacket(p_36347_));
                                p_36347_.hurtMarked = false;
                                p_36347_.setDeltaMovement(vec3);
                            }

                            if (flag1) {
                                this.level()
                                    .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F);
                                this.crit(p_36347_);
                            }

                            if (!flag1 && !flag2) {
                                if (flag3) {
                                    this.level()
                                        .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F);
                                } else {
                                    this.level()
                                        .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F);
                                }
                            }

                            if (f1 > 0.0F) {
                                this.magicCrit(p_36347_);
                            }

                            this.setLastHurtMob(p_36347_);
                            Entity entity = p_36347_;
                            if (p_36347_ instanceof net.minecraftforge.entity.PartEntity<?> pe) {
                                entity = pe.getParent();
                            }

                            boolean flag5 = false;
                            if (this.level() instanceof ServerLevel serverlevel1) {
                                if (entity instanceof LivingEntity livingentity3) {
                                    flag5 = itemstack.hurtEnemy(livingentity3, this);
                                }

                                EnchantmentHelper.doPostAttackEffects(serverlevel1, p_36347_, damagesource);
                            }

                            if (!this.level().isClientSide && !itemstack.isEmpty() && entity instanceof LivingEntity) {
                                if (flag5) {
                                    itemstack.postHurtEnemy((LivingEntity)entity, this);
                                }

                                if (itemstack.isEmpty()) {
                                    if (itemstack == this.getMainHandItem()) {
                                        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                                    } else {
                                        this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                                    }
                                }
                            }

                            if (p_36347_ instanceof LivingEntity) {
                                float f7 = f5 - ((LivingEntity)p_36347_).getHealth();
                                this.awardStat(Stats.DAMAGE_DEALT, Math.round(f7 * 10.0F));
                                if (this.level() instanceof ServerLevel && f7 > 2.0F) {
                                    int i = (int)(f7 * 0.5);
                                    ((ServerLevel)this.level())
                                        .sendParticles(
                                            ParticleTypes.DAMAGE_INDICATOR, p_36347_.getX(), p_36347_.getY(0.5), p_36347_.getZ(), i, 0.1, 0.0, 0.1, 0.2
                                        );
                                }
                            }

                            this.causeFoodExhaustion(0.1F);
                        } else {
                            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F);
                        }
                    }
                }
                this.resetAttackStrengthTicker(); // FORGE: Moved from beginning of attack() so that getAttackStrengthScale() returns an accurate value during all attack events
            }
        }
    }

    protected float getEnchantedDamage(Entity p_344881_, float p_345044_, DamageSource p_343261_) {
        return p_345044_;
    }

    @Override
    protected void doAutoAttackOnTouch(LivingEntity p_36355_) {
        this.attack(p_36355_);
    }

    public void crit(Entity p_36156_) {
    }

    public void magicCrit(Entity p_36253_) {
    }

    public void sweepAttack() {
        double d0 = -Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
        double d1 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
        if (this.level() instanceof ServerLevel) {
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + d0, this.getY(0.5), this.getZ() + d1, 0, d0, 0.0, d1, 0.0);
        }
    }

    public void respawn() {
    }

    @Override
    public void remove(Entity.RemovalReason p_150097_) {
        super.remove(p_150097_);
        this.inventoryMenu.removed(this);
        if (this.containerMenu != null && this.hasContainerOpen()) {
            this.doCloseContainer();
        }
    }

    @Override
    public boolean isClientAuthoritative() {
        return true;
    }

    @Override
    protected boolean isLocalClientAuthoritative() {
        return this.isLocalPlayer();
    }

    public boolean isLocalPlayer() {
        return false;
    }

    @Override
    public boolean canSimulateMovement() {
        return !this.level().isClientSide || this.isLocalPlayer();
    }

    @Override
    public boolean isEffectiveAi() {
        return !this.level().isClientSide || this.isLocalPlayer();
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public Abilities getAbilities() {
        return this.abilities;
    }

    @Override
    public boolean hasInfiniteMaterials() {
        return this.abilities.instabuild;
    }

    public boolean preventsBlockDrops() {
        return this.abilities.instabuild;
    }

    public void updateTutorialInventoryAction(ItemStack p_150098_, ItemStack p_150099_, ClickAction p_150100_) {
    }

    public boolean hasContainerOpen() {
        return this.containerMenu != this.inventoryMenu;
    }

    public boolean canDropItems() {
        return true;
    }

    public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos p_36203_) {
        this.startSleeping(p_36203_);
        this.sleepCounter = 0;
        return Either.right(Unit.INSTANCE);
    }

    public void stopSleepInBed(boolean p_36226_, boolean p_36227_) {
        net.minecraftforge.event.ForgeEventFactory.onPlayerWakeup(this, p_36226_, p_36227_);
        super.stopSleeping();
        if (this.level() instanceof ServerLevel && p_36227_) {
            ((ServerLevel)this.level()).updateSleepingPlayerList();
        }

        this.sleepCounter = p_36226_ ? 0 : 100;
    }

    @Override
    public void stopSleeping() {
        this.stopSleepInBed(true, true);
    }

    public boolean isSleepingLongEnough() {
        return this.isSleeping() && this.sleepCounter >= 100;
    }

    public int getSleepTimer() {
        return this.sleepCounter;
    }

    public void displayClientMessage(Component p_36216_, boolean p_36217_) {
    }

    public void awardStat(ResourceLocation p_36221_) {
        this.awardStat(Stats.CUSTOM.get(p_36221_));
    }

    public void awardStat(ResourceLocation p_36223_, int p_36224_) {
        this.awardStat(Stats.CUSTOM.get(p_36223_), p_36224_);
    }

    public void awardStat(Stat<?> p_36247_) {
        this.awardStat(p_36247_, 1);
    }

    public void awardStat(Stat<?> p_36145_, int p_36146_) {
    }

    public void resetStat(Stat<?> p_36144_) {
    }

    public int awardRecipes(Collection<RecipeHolder<?>> p_36213_) {
        return 0;
    }

    public void triggerRecipeCrafted(RecipeHolder<?> p_298309_, List<ItemStack> p_283609_) {
    }

    public void awardRecipesByKey(List<ResourceKey<Recipe<?>>> p_312830_) {
    }

    public int resetRecipes(Collection<RecipeHolder<?>> p_36263_) {
        return 0;
    }

    @Override
    public void travel(Vec3 p_36359_) {
        if (this.isPassenger()) {
            super.travel(p_36359_);
        } else {
            if (this.isSwimming()) {
                double d0 = this.getLookAngle().y;
                double d1 = d0 < -0.2 ? 0.085 : 0.06;
                if (d0 <= 0.0
                    || this.jumping
                    || !this.level().getFluidState(BlockPos.containing(this.getX(), this.getY() + 1.0 - 0.1, this.getZ())).isEmpty()) {
                    Vec3 vec3 = this.getDeltaMovement();
                    this.setDeltaMovement(vec3.add(0.0, (d0 - vec3.y) * d1, 0.0));
                }
            }

            if (this.getAbilities().flying) {
                double d2 = this.getDeltaMovement().y;
                super.travel(p_36359_);
                this.setDeltaMovement(this.getDeltaMovement().with(Direction.Axis.Y, d2 * 0.6));
            } else {
                super.travel(p_36359_);
            }
        }
    }

    @Override
    protected boolean canGlide() {
        return !this.abilities.flying && super.canGlide();
    }

    @Override
    public void updateSwimming() {
        if (this.abilities.flying) {
            this.setSwimming(false);
        } else {
            super.updateSwimming();
        }
    }

    protected boolean freeAt(BlockPos p_36351_) {
        return !this.level().getBlockState(p_36351_).isSuffocating(this.level(), p_36351_);
    }

    @Override
    public float getSpeed() {
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public boolean causeFallDamage(double p_391627_, float p_150093_, DamageSource p_150095_) {
        if (this.abilities.mayfly) {
            net.minecraftforge.event.ForgeEventFactory.onPlayerFall(this, p_150093_, p_150093_);
            return false;
        } else {
            if (p_391627_ >= 2.0) {
                this.awardStat(Stats.FALL_ONE_CM, (int)Math.round(p_391627_ * 100.0));
            }

            boolean flag = this.currentImpulseImpactPos != null && this.ignoreFallDamageFromCurrentImpulse;
            double d0;
            if (flag) {
                d0 = Math.min(p_391627_, this.currentImpulseImpactPos.y - this.getY());
                boolean flag1 = d0 <= 0.0;
                if (flag1) {
                    this.resetCurrentImpulseContext();
                } else {
                    this.tryResetCurrentImpulseContext();
                }
            } else {
                d0 = p_391627_;
            }

            if (d0 > 0.0 && super.causeFallDamage(d0, p_150093_, p_150095_)) {
                this.resetCurrentImpulseContext();
                return true;
            } else {
                this.propagateFallToPassengers(p_391627_, p_150093_, p_150095_);
                return false;
            }
        }
    }

    public boolean tryToStartFallFlying() {
        if (!this.isFallFlying() && this.canGlide() && !this.isInWater()) {
            this.startFallFlying();
            return true;
        } else {
            return false;
        }
    }

    public void startFallFlying() {
        this.setSharedFlag(7, true);
    }

    @Override
    protected void doWaterSplashEffect() {
        if (!this.isSpectator()) {
            super.doWaterSplashEffect();
        }
    }

    @Override
    protected void playStepSound(BlockPos p_282121_, BlockState p_282194_) {
        if (this.isInWater()) {
            this.waterSwimSound();
            this.playMuffledStepSound(p_282194_, p_282121_);
        } else {
            BlockPos blockpos = this.getPrimaryStepSoundBlockPos(p_282121_);
            if (!p_282121_.equals(blockpos)) {
                BlockState blockstate = this.level().getBlockState(blockpos);
                if (blockstate.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                    this.playCombinationStepSounds(blockstate, p_282194_, blockpos, p_282121_);
                } else {
                    super.playStepSound(blockpos, blockstate);
                }
            } else {
                super.playStepSound(p_282121_, p_282194_);
            }
        }
    }

    @Override
    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.PLAYER_SMALL_FALL, SoundEvents.PLAYER_BIG_FALL);
    }

    @Override
    public boolean killedEntity(ServerLevel p_219735_, LivingEntity p_219736_) {
        this.awardStat(Stats.ENTITY_KILLED.get(p_219736_.getType()));
        return true;
    }

    @Override
    public void makeStuckInBlock(BlockState p_36196_, Vec3 p_36197_) {
        if (!this.abilities.flying) {
            super.makeStuckInBlock(p_36196_, p_36197_);
        }

        this.tryResetCurrentImpulseContext();
    }

    public void giveExperiencePoints(int p_36291_) {
        var event = net.minecraftforge.event.ForgeEventFactory.onPlayerXpChange(this, p_36291_);
        if (event == null) {
            return;
        }
        p_36291_ = event.getAmount();

        this.increaseScore(p_36291_);
        this.experienceProgress = this.experienceProgress + (float)p_36291_ / this.getXpNeededForNextLevel();
        this.totalExperience = Mth.clamp(this.totalExperience + p_36291_, 0, Integer.MAX_VALUE);

        while (this.experienceProgress < 0.0F) {
            float f = this.experienceProgress * this.getXpNeededForNextLevel();
            if (this.experienceLevel > 0) {
                this.giveExperienceLevels(-1);
                this.experienceProgress = 1.0F + f / this.getXpNeededForNextLevel();
            } else {
                this.giveExperienceLevels(-1);
                this.experienceProgress = 0.0F;
            }
        }

        while (this.experienceProgress >= 1.0F) {
            this.experienceProgress = (this.experienceProgress - 1.0F) * this.getXpNeededForNextLevel();
            this.giveExperienceLevels(1);
            this.experienceProgress = this.experienceProgress / this.getXpNeededForNextLevel();
        }
    }

    public int getEnchantmentSeed() {
        return this.enchantmentSeed;
    }

    public void onEnchantmentPerformed(ItemStack p_36172_, int p_36173_) {
        giveExperienceLevels(-p_36173_);
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }

        this.enchantmentSeed = this.random.nextInt();
    }

    public void giveExperienceLevels(int p_36276_) {
        var event = net.minecraftforge.event.ForgeEventFactory.onPlayerLevelChange(this, p_36276_);
        if (event == null) {
            return;
        }
        p_36276_ = event.getLevels();

        this.experienceLevel = IntMath.saturatedAdd(this.experienceLevel, p_36276_);
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }

        if (p_36276_ > 0 && this.experienceLevel % 5 == 0 && this.lastLevelUpTime < this.tickCount - 100.0F) {
            float f = this.experienceLevel > 30 ? 1.0F : this.experienceLevel / 30.0F;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), f * 0.75F, 1.0F);
            this.lastLevelUpTime = this.tickCount;
        }
    }

    public int getXpNeededForNextLevel() {
        if (this.experienceLevel >= 30) {
            return 112 + (this.experienceLevel - 30) * 9;
        } else {
            return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
        }
    }

    public void causeFoodExhaustion(float p_36400_) {
        if (!this.abilities.invulnerable) {
            if (!this.level().isClientSide) {
                this.foodData.addExhaustion(p_36400_);
            }
        }
    }

    public Optional<WardenSpawnTracker> getWardenSpawnTracker() {
        return Optional.empty();
    }

    public FoodData getFoodData() {
        return this.foodData;
    }

    public boolean canEat(boolean p_36392_) {
        return this.abilities.invulnerable || p_36392_ || this.foodData.needsFood();
    }

    public boolean isHurt() {
        return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
    }

    public boolean mayBuild() {
        return this.abilities.mayBuild;
    }

    public boolean mayUseItemAt(BlockPos p_36205_, Direction p_36206_, ItemStack p_36207_) {
        if (this.abilities.mayBuild) {
            return true;
        } else {
            BlockPos blockpos = p_36205_.relative(p_36206_.getOpposite());
            BlockInWorld blockinworld = new BlockInWorld(this.level(), blockpos, false);
            return p_36207_.canPlaceOnBlockInAdventureMode(blockinworld);
        }
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel p_361105_) {
        return !p_361105_.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !this.isSpectator() ? Math.min(this.experienceLevel * 7, 100) : 0;
    }

    @Override
    protected boolean isAlwaysExperienceDropper() {
        return true;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return this.abilities.flying || this.onGround() && this.isDiscrete() ? Entity.MovementEmission.NONE : Entity.MovementEmission.ALL;
    }

    public void onUpdateAbilities() {
    }

    @Override
    public Component getName() {
        return Component.literal(this.gameProfile.getName());
    }

    public PlayerEnderChestContainer getEnderChestInventory() {
        return this.enderChestInventory;
    }

    @Override
    protected boolean doesEmitEquipEvent(EquipmentSlot p_219741_) {
        return p_219741_.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
    }

    public boolean addItem(ItemStack p_36357_) {
        return this.inventory.add(p_36357_);
    }

    public boolean setEntityOnShoulder(CompoundTag p_36361_) {
        if (this.isPassenger() || !this.onGround() || this.isInWater() || this.isInPowderSnow) {
            return false;
        } else if (this.getShoulderEntityLeft().isEmpty()) {
            this.setShoulderEntityLeft(p_36361_);
            this.timeEntitySatOnShoulder = this.level().getGameTime();
            return true;
        } else if (this.getShoulderEntityRight().isEmpty()) {
            this.setShoulderEntityRight(p_36361_);
            this.timeEntitySatOnShoulder = this.level().getGameTime();
            return true;
        } else {
            return false;
        }
    }

    protected void removeEntitiesOnShoulder() {
        if (this.timeEntitySatOnShoulder + 20L < this.level().getGameTime()) {
            this.respawnEntityOnShoulder(this.getShoulderEntityLeft());
            this.setShoulderEntityLeft(new CompoundTag());
            this.respawnEntityOnShoulder(this.getShoulderEntityRight());
            this.setShoulderEntityRight(new CompoundTag());
        }
    }

    private void respawnEntityOnShoulder(CompoundTag p_36371_) {
        if (this.level() instanceof ServerLevel serverlevel && !p_36371_.isEmpty()) {
            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
                EntityType.create(
                        TagValueInput.create(problemreporter$scopedcollector.forChild(() -> ".shoulder"), serverlevel.registryAccess(), p_36371_),
                        serverlevel,
                        EntitySpawnReason.LOAD
                    )
                    .ifPresent(p_405550_ -> {
                        if (p_405550_ instanceof TamableAnimal tamableanimal) {
                            tamableanimal.setOwner(this);
                        }

                        p_405550_.setPos(this.getX(), this.getY() + 0.7F, this.getZ());
                        serverlevel.addWithUUID(p_405550_);
                    });
            }
        }
    }

    @Nullable
    public abstract GameType gameMode();

    @Override
    public boolean isSpectator() {
        return this.gameMode() == GameType.SPECTATOR;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return !this.isSpectator() && super.canBeHitByProjectile();
    }

    @Override
    public boolean isSwimming() {
        return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
    }

    public boolean isCreative() {
        return this.gameMode() == GameType.CREATIVE;
    }

    @Override
    public boolean isPushedByFluid() {
        return !this.abilities.flying;
    }

    public Scoreboard getScoreboard() {
        return this.level().getScoreboard();
    }

    @Override
    public Component getDisplayName() {
        if (this.displayname == null) {
            this.displayname = net.minecraftforge.event.ForgeEventFactory.getPlayerDisplayName(this, this.getName());
        }
        MutableComponent mutablecomponent = Component.literal("");
        mutablecomponent = prefixes.stream().reduce(mutablecomponent, MutableComponent::append);
        mutablecomponent = mutablecomponent.append(PlayerTeam.formatNameForTeam(this.getTeam(), this.displayname));
        mutablecomponent = suffixes.stream().reduce(mutablecomponent, MutableComponent::append);
        return this.decorateDisplayNameComponent(mutablecomponent);
    }

    private MutableComponent decorateDisplayNameComponent(MutableComponent p_36219_) {
        String s = this.getGameProfile().getName();
        return p_36219_.withStyle(p_405552_ -> p_405552_.withClickEvent(new ClickEvent.SuggestCommand("/tell " + s + " ")).withHoverEvent(this.createHoverEvent()).withInsertion(s));
    }

    @Override
    public String getScoreboardName() {
        return this.getGameProfile().getName();
    }

    @Override
    protected void internalSetAbsorptionAmount(float p_301235_) {
        this.getEntityData().set(DATA_PLAYER_ABSORPTION_ID, p_301235_);
    }

    @Override
    public float getAbsorptionAmount() {
        return this.getEntityData().get(DATA_PLAYER_ABSORPTION_ID);
    }

    public boolean isModelPartShown(PlayerModelPart p_36171_) {
        return (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & p_36171_.getMask()) == p_36171_.getMask();
    }

    @Override
    public SlotAccess getSlot(int p_150112_) {
        if (p_150112_ == 499) {
            return new SlotAccess() {
                @Override
                public ItemStack get() {
                    return Player.this.containerMenu.getCarried();
                }

                @Override
                public boolean set(ItemStack p_333834_) {
                    Player.this.containerMenu.setCarried(p_333834_);
                    return true;
                }
            };
        } else {
            final int i = p_150112_ - 500;
            if (i >= 0 && i < 4) {
                return new SlotAccess() {
                    @Override
                    public ItemStack get() {
                        return Player.this.inventoryMenu.getCraftSlots().getItem(i);
                    }

                    @Override
                    public boolean set(ItemStack p_333999_) {
                        Player.this.inventoryMenu.getCraftSlots().setItem(i, p_333999_);
                        Player.this.inventoryMenu.slotsChanged(Player.this.inventory);
                        return true;
                    }
                };
            } else if (p_150112_ >= 0 && p_150112_ < this.inventory.getNonEquipmentItems().size()) {
                return SlotAccess.forContainer(this.inventory, p_150112_);
            } else {
                int j = p_150112_ - 200;
                return j >= 0 && j < this.enderChestInventory.getContainerSize() ? SlotAccess.forContainer(this.enderChestInventory, j) : super.getSlot(p_150112_);
            }
        }
    }

    public boolean isReducedDebugInfo() {
        return this.reducedDebugInfo;
    }

    public void setReducedDebugInfo(boolean p_36394_) {
        this.reducedDebugInfo = p_36394_;
    }

    @Override
    public void setRemainingFireTicks(int p_36353_) {
        super.setRemainingFireTicks(this.abilities.invulnerable ? Math.min(p_36353_, 1) : p_36353_);
    }

    @Override
    public HumanoidArm getMainArm() {
        return this.entityData.get(DATA_PLAYER_MAIN_HAND) == 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    public void setMainArm(HumanoidArm p_36164_) {
        this.entityData.set(DATA_PLAYER_MAIN_HAND, (byte)(p_36164_ == HumanoidArm.LEFT ? 0 : 1));
    }

    public CompoundTag getShoulderEntityLeft() {
        return this.entityData.get(DATA_SHOULDER_LEFT);
    }

    protected void setShoulderEntityLeft(CompoundTag p_36363_) {
        this.entityData.set(DATA_SHOULDER_LEFT, p_36363_);
    }

    public CompoundTag getShoulderEntityRight() {
        return this.entityData.get(DATA_SHOULDER_RIGHT);
    }

    protected void setShoulderEntityRight(CompoundTag p_36365_) {
        this.entityData.set(DATA_SHOULDER_RIGHT, p_36365_);
    }

    public float getCurrentItemAttackStrengthDelay() {
        return (float)(1.0 / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0);
    }

    public float getAttackStrengthScale(float p_36404_) {
        return Mth.clamp((this.attackStrengthTicker + p_36404_) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
    }

    public void resetAttackStrengthTicker() {
        this.attackStrengthTicker = 0;
    }

    public ItemCooldowns getCooldowns() {
        return this.cooldowns;
    }

    @Override
    protected float getBlockSpeedFactor() {
        return !this.abilities.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
    }

    @Override
    public float getLuck() {
        return (float)this.getAttributeValue(Attributes.LUCK);
    }

    public boolean canUseGameMasterBlocks() {
        return this.abilities.instabuild && this.getPermissionLevel() >= 2;
    }

    public int getPermissionLevel() {
        return 0;
    }

    public boolean hasPermissions(int p_370047_) {
        return this.getPermissionLevel() >= p_370047_;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose p_36166_) {
        return POSES.getOrDefault(p_36166_, STANDING_DIMENSIONS);
    }

    @Override
    public ImmutableList<Pose> getDismountPoses() {
        return ImmutableList.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);
    }

    @Override
    public ItemStack getProjectile(ItemStack p_36349_) {
        if (!(p_36349_.getItem() instanceof ProjectileWeaponItem)) {
            return ItemStack.EMPTY;
        } else {
            Predicate<ItemStack> predicate = ((ProjectileWeaponItem)p_36349_.getItem()).getSupportedHeldProjectiles();
            ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
            if (!itemstack.isEmpty()) {
                return net.minecraftforge.common.ForgeHooks.getProjectile(this, p_36349_, itemstack);
            } else {
                predicate = ((ProjectileWeaponItem)p_36349_.getItem()).getAllSupportedProjectiles();

                for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                    ItemStack itemstack1 = this.inventory.getItem(i);
                    if (predicate.test(itemstack1)) {
                        return net.minecraftforge.common.ForgeHooks.getProjectile(this, p_36349_, itemstack1);
                    }
                }

                var vanilla = this.abilities.instabuild ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
                return net.minecraftforge.common.ForgeHooks.getProjectile(this, p_36349_, vanilla);
            }
        }
    }

    @Override
    public Vec3 getRopeHoldPosition(float p_36374_) {
        double d0 = 0.22 * (this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0);
        float f = Mth.lerp(p_36374_ * 0.5F, this.getXRot(), this.xRotO) * (float) (Math.PI / 180.0);
        float f1 = Mth.lerp(p_36374_, this.yBodyRotO, this.yBodyRot) * (float) (Math.PI / 180.0);
        if (this.isFallFlying() || this.isAutoSpinAttack()) {
            Vec3 vec31 = this.getViewVector(p_36374_);
            Vec3 vec3 = this.getDeltaMovement();
            double d6 = vec3.horizontalDistanceSqr();
            double d3 = vec31.horizontalDistanceSqr();
            float f2;
            if (d6 > 0.0 && d3 > 0.0) {
                double d4 = (vec3.x * vec31.x + vec3.z * vec31.z) / Math.sqrt(d6 * d3);
                double d5 = vec3.x * vec31.z - vec3.z * vec31.x;
                f2 = (float)(Math.signum(d5) * Math.acos(d4));
            } else {
                f2 = 0.0F;
            }

            return this.getPosition(p_36374_).add(new Vec3(d0, -0.11, 0.85).zRot(-f2).xRot(-f).yRot(-f1));
        } else if (this.isVisuallySwimming()) {
            return this.getPosition(p_36374_).add(new Vec3(d0, 0.2, -0.15).xRot(-f).yRot(-f1));
        } else {
            double d1 = this.getBoundingBox().getYsize() - 1.0;
            double d2 = this.isCrouching() ? -0.2 : 0.07;
            return this.getPosition(p_36374_).add(new Vec3(d0, d1, d2).yRot(-f1));
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    public boolean isScoping() {
        return this.isUsingItem() && this.getUseItem().is(Items.SPYGLASS);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public Optional<GlobalPos> getLastDeathLocation() {
        return this.lastDeathLocation;
    }

    public void setLastDeathLocation(Optional<GlobalPos> p_219750_) {
        this.lastDeathLocation = p_219750_;
    }

    @Override
    public float getHurtDir() {
        return this.hurtDir;
    }

    @Override
    public void animateHurt(float p_265280_) {
        super.animateHurt(p_265280_);
        this.hurtDir = p_265280_;
    }

    @Override
    public boolean canSprint() {
        return true;
    }

    @Override
    protected float getFlyingSpeed() {
        if (this.abilities.flying && !this.isPassenger()) {
            return this.isSprinting() ? this.abilities.getFlyingSpeed() * 2.0F : this.abilities.getFlyingSpeed();
        } else {
            return this.isSprinting() ? 0.025999999F : 0.02F;
        }
    }

    public boolean hasClientLoaded() {
        return this.clientLoaded || this.clientLoadedTimeoutTimer <= 0;
    }

    public void tickClientLoadTimeout() {
        if (!this.clientLoaded) {
            this.clientLoadedTimeoutTimer--;
        }
    }

    public void setClientLoaded(boolean p_377570_) {
        this.clientLoaded = p_377570_;
        if (!this.clientLoaded) {
            this.clientLoadedTimeoutTimer = 60;
        }
    }

    public double blockInteractionRange() {
        return this.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
    }

    public double entityInteractionRange() {
        return this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
    }

    public boolean canInteractWithEntity(Entity p_333619_, double p_330803_) {
        return p_333619_.isRemoved() ? false : this.canInteractWithEntity(p_333619_.getBoundingBox(), p_330803_);
    }

    public boolean canInteractWithEntity(AABB p_329456_, double p_332906_) {
        double d0 = this.entityInteractionRange() + p_332906_;
        return p_329456_.distanceToSqr(this.getEyePosition()) < d0 * d0;
    }

    public boolean canInteractWithBlock(BlockPos p_331132_, double p_328439_) {
        double d0 = this.blockInteractionRange() + p_328439_;
        return new AABB(p_331132_).distanceToSqr(this.getEyePosition()) < d0 * d0;
    }

    public void setIgnoreFallDamageFromCurrentImpulse(boolean p_344459_) {
        this.ignoreFallDamageFromCurrentImpulse = p_344459_;
        if (p_344459_) {
            this.currentImpulseContextResetGraceTime = 40;
        } else {
            this.currentImpulseContextResetGraceTime = 0;
        }
    }

    public boolean isIgnoringFallDamageFromCurrentImpulse() {
        return this.ignoreFallDamageFromCurrentImpulse;
    }

    public void tryResetCurrentImpulseContext() {
        if (this.currentImpulseContextResetGraceTime == 0) {
            this.resetCurrentImpulseContext();
        }
    }

    public void resetCurrentImpulseContext() {
        this.currentImpulseContextResetGraceTime = 0;
        this.currentExplosionCause = null;
        this.currentImpulseImpactPos = null;
        this.ignoreFallDamageFromCurrentImpulse = false;
    }

    public Collection<MutableComponent> getPrefixes() {
        return this.prefixes;
    }

    public Collection<MutableComponent> getSuffixes() {
        return this.suffixes;
    }

    private Component displayname = null;

    /**
     * Force the displayed name to refresh, by firing {@link net.minecraftforge.event.entity.player.PlayerEvent.NameFormat}, using the real player name as event parameter.
     */
    public void refreshDisplayName() {
        this.displayname = net.minecraftforge.event.ForgeEventFactory.getPlayerDisplayName(this, this.getName());
    }

    private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>
        playerMainHandler, playerEquipmentHandler, playerJoinedHandler;

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && this.isAlive()) {
            if (facing == null) return playerJoinedHandler.cast();
            else if (facing.getAxis().isVertical()) return playerMainHandler.cast();
            else if (facing.getAxis().isHorizontal()) return playerEquipmentHandler.cast();
        }
        return super.getCapability(capability, facing);
    }

    /**
     * Force a pose for the player. If set, the vanilla pose determination and clearance check is skipped. Make sure the pose is clear yourself (e.g. in PlayerTick).
     * This has to be set just once, do not set it every tick.
     * Make sure to clear (null) the pose if not required anymore and only use if necessary.
     */
    public void setForcedPose(@Nullable Pose pose) {
        this.forcedPose = pose;
    }

    /**
     * @return The forced pose if set, null otherwise
     */
    @Nullable
    public Pose getForcedPose() {
        return this.forcedPose;
    }


    public boolean shouldRotateWithMinecart() {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return this.abilities.flying ? false : super.onClimbable();
    }

    public String debugInfo() {
        return MoreObjects.toStringHelper(this)
            .add("name", this.getName().getString())
            .add("id", this.getId())
            .add("pos", this.position())
            .add("mode", this.gameMode())
            .add("permission", this.getPermissionLevel())
            .toString();
    }

    public static enum BedSleepingProblem {
        NOT_POSSIBLE_HERE,
        NOT_POSSIBLE_NOW(Component.translatable("block.minecraft.bed.no_sleep")),
        TOO_FAR_AWAY(Component.translatable("block.minecraft.bed.too_far_away")),
        OBSTRUCTED(Component.translatable("block.minecraft.bed.obstructed")),
        OTHER_PROBLEM,
        NOT_SAFE(Component.translatable("block.minecraft.bed.not_safe"));

        @Nullable
        private final Component message;

        private BedSleepingProblem() {
            this.message = null;
        }

        private BedSleepingProblem(final Component p_36422_) {
            this.message = p_36422_;
        }

        @Nullable
        public Component getMessage() {
            return this.message;
        }
    }
}
