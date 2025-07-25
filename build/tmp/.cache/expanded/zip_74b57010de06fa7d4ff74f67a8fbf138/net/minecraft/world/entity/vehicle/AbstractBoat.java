package net.minecraft.world.entity.vehicle;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractBoat extends VehicleEntity implements Leashable, net.minecraftforge.common.extensions.IForgeAbstractBoat {
    private static final EntityDataAccessor<Boolean> DATA_ID_PADDLE_LEFT = SynchedEntityData.defineId(AbstractBoat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ID_PADDLE_RIGHT = SynchedEntityData.defineId(AbstractBoat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ID_BUBBLE_TIME = SynchedEntityData.defineId(AbstractBoat.class, EntityDataSerializers.INT);
    public static final int PADDLE_LEFT = 0;
    public static final int PADDLE_RIGHT = 1;
    private static final int TIME_TO_EJECT = 60;
    private static final float PADDLE_SPEED = (float) (Math.PI / 8);
    public static final double PADDLE_SOUND_TIME = (float) (Math.PI / 4);
    public static final int BUBBLE_TIME = 60;
    private final float[] paddlePositions = new float[2];
    private float outOfControlTicks;
    private float deltaRotation;
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputUp;
    private boolean inputDown;
    private double waterLevel;
    private float landFriction;
    private AbstractBoat.Status status;
    private AbstractBoat.Status oldStatus;
    private double lastYd;
    private boolean isAboveBubbleColumn;
    private boolean bubbleColumnDirectionIsDown;
    private float bubbleMultiplier;
    private float bubbleAngle;
    private float bubbleAngleO;
    @Nullable
    private Leashable.LeashData leashData;
    private final Supplier<Item> dropItem;

    public AbstractBoat(EntityType<? extends AbstractBoat> p_361501_, Level p_362983_, Supplier<Item> p_365566_) {
        super(p_361501_, p_362983_);
        this.dropItem = p_365566_;
        this.blocksBuilding = true;
    }

    public void setInitialPos(double p_364862_, double p_363329_, double p_361885_) {
        this.setPos(p_364862_, p_363329_, p_361885_);
        this.xo = p_364862_;
        this.yo = p_363329_;
        this.zo = p_361885_;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_362019_) {
        super.defineSynchedData(p_362019_);
        p_362019_.define(DATA_ID_PADDLE_LEFT, false);
        p_362019_.define(DATA_ID_PADDLE_RIGHT, false);
        p_362019_.define(DATA_ID_BUBBLE_TIME, 0);
    }

    @Override
    public boolean canCollideWith(Entity p_364219_) {
        return canVehicleCollide(this, p_364219_);
    }

    public static boolean canVehicleCollide(Entity p_362540_, Entity p_368220_) {
        return (p_368220_.canBeCollidedWith(p_362540_) || p_368220_.isPushable()) && !p_362540_.isPassengerOfSameVehicle(p_368220_);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity p_408776_) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis p_368283_, BlockUtil.FoundRectangle p_365178_) {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(p_368283_, p_365178_));
    }

    protected abstract double rideHeight(EntityDimensions p_363309_);

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity p_369514_, EntityDimensions p_366303_, float p_367794_) {
        float f = this.getSinglePassengerXOffset();
        if (this.getPassengers().size() > 1) {
            int i = this.getPassengers().indexOf(p_369514_);
            if (i == 0) {
                f = 0.2F;
            } else {
                f = -0.6F;
            }

            if (p_369514_ instanceof Animal) {
                f += 0.2F;
            }
        }

        return new Vec3(0.0, this.rideHeight(p_366303_), f).yRot(-this.getYRot() * (float) (Math.PI / 180.0));
    }

    @Override
    public void onAboveBubbleColumn(boolean p_392747_, BlockPos p_392716_) {
        if (this.level() instanceof ServerLevel) {
            this.isAboveBubbleColumn = true;
            this.bubbleColumnDirectionIsDown = p_392747_;
            if (this.getBubbleTime() == 0) {
                this.setBubbleTime(60);
            }
        }

        if (!this.isUnderWater() && this.random.nextInt(100) == 0) {
            this.level()
                .playLocalSound(
                    this.getX(), this.getY(), this.getZ(), this.getSwimSplashSound(), this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat(), false
                );
            this.level()
                .addParticle(
                    ParticleTypes.SPLASH,
                    this.getX() + this.random.nextFloat(),
                    this.getY() + 0.7,
                    this.getZ() + this.random.nextFloat(),
                    0.0,
                    0.0,
                    0.0
                );
            this.gameEvent(GameEvent.SPLASH, this.getControllingPassenger());
        }
    }

    @Override
    public void push(Entity p_362452_) {
        if (p_362452_ instanceof AbstractBoat) {
            if (p_362452_.getBoundingBox().minY < this.getBoundingBox().maxY) {
                super.push(p_362452_);
            }
        } else if (p_362452_.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.push(p_362452_);
        }
    }

    @Override
    public void animateHurt(float p_365475_) {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() * 11.0F);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public Direction getMotionDirection() {
        return this.getDirection().getClockWise();
    }

    @Override
    public void tick() {
        this.oldStatus = this.status;
        this.status = this.getStatus();
        if (this.status != AbstractBoat.Status.UNDER_WATER && this.status != AbstractBoat.Status.UNDER_FLOWING_WATER) {
            this.outOfControlTicks = 0.0F;
        } else {
            this.outOfControlTicks++;
        }

        if (!this.level().isClientSide && this.outOfControlTicks >= 60.0F) {
            this.ejectPassengers();
        }

        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        super.tick();
        this.interpolation.interpolate();
        if (this.isLocalInstanceAuthoritative()) {
            if (!(this.getFirstPassenger() instanceof Player)) {
                this.setPaddleState(false, false);
            }

            this.floatBoat();
            if (this.level().isClientSide) {
                this.controlBoat();
                this.level().sendPacketToServer(new ServerboundPaddleBoatPacket(this.getPaddleState(0), this.getPaddleState(1)));
            }

            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        this.applyEffectsFromBlocks();
        this.applyEffectsFromBlocks();
        this.tickBubbleColumn();

        for (int i = 0; i <= 1; i++) {
            if (this.getPaddleState(i)) {
                if (!this.isSilent()
                    && this.paddlePositions[i] % (float) (Math.PI * 2) <= (float) (Math.PI / 4)
                    && (this.paddlePositions[i] + (float) (Math.PI / 8)) % (float) (Math.PI * 2) >= (float) (Math.PI / 4)) {
                    SoundEvent soundevent = this.getPaddleSound();
                    if (soundevent != null) {
                        Vec3 vec3 = this.getViewVector(1.0F);
                        double d0 = i == 1 ? -vec3.z : vec3.z;
                        double d1 = i == 1 ? vec3.x : -vec3.x;
                        this.level()
                            .playSound(
                                null,
                                this.getX() + d0,
                                this.getY(),
                                this.getZ() + d1,
                                soundevent,
                                this.getSoundSource(),
                                1.0F,
                                0.8F + 0.4F * this.random.nextFloat()
                            );
                    }
                }

                this.paddlePositions[i] = this.paddlePositions[i] + (float) (Math.PI / 8);
            } else {
                this.paddlePositions[i] = 0.0F;
            }
        }

        List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.2F, -0.01F, 0.2F), EntitySelector.pushableBy(this));
        if (!list.isEmpty()) {
            boolean flag = !this.level().isClientSide && !(this.getControllingPassenger() instanceof Player);

            for (Entity entity : list) {
                if (!entity.hasPassenger(this)) {
                    if (flag
                        && this.getPassengers().size() < this.getMaxPassengers()
                        && !entity.isPassenger()
                        && this.hasEnoughSpaceFor(entity)
                        && entity instanceof LivingEntity
                        && !(entity instanceof WaterAnimal)
                        && !(entity instanceof Player)
                        && !(entity instanceof Creaking)) {
                        entity.startRiding(this);
                    } else {
                        this.push(entity);
                    }
                }
            }
        }
    }

    private void tickBubbleColumn() {
        if (this.level().isClientSide) {
            int i = this.getBubbleTime();
            if (i > 0) {
                this.bubbleMultiplier += 0.05F;
            } else {
                this.bubbleMultiplier -= 0.1F;
            }

            this.bubbleMultiplier = Mth.clamp(this.bubbleMultiplier, 0.0F, 1.0F);
            this.bubbleAngleO = this.bubbleAngle;
            this.bubbleAngle = 10.0F * (float)Math.sin(0.5 * this.tickCount) * this.bubbleMultiplier;
        } else {
            if (!this.isAboveBubbleColumn) {
                this.setBubbleTime(0);
            }

            int k = this.getBubbleTime();
            if (k > 0) {
                this.setBubbleTime(--k);
                int j = 60 - k - 1;
                if (j > 0 && k == 0) {
                    this.setBubbleTime(0);
                    Vec3 vec3 = this.getDeltaMovement();
                    if (this.bubbleColumnDirectionIsDown) {
                        this.setDeltaMovement(vec3.add(0.0, -0.7, 0.0));
                        this.ejectPassengers();
                    } else {
                        this.setDeltaMovement(vec3.x, this.hasPassenger(p_367092_ -> p_367092_ instanceof Player) ? 2.7 : 0.6, vec3.z);
                    }
                }

                this.isAboveBubbleColumn = false;
            }
        }
    }

    @Nullable
    protected SoundEvent getPaddleSound() {
        return switch (this.getStatus()) {
            case IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER -> SoundEvents.BOAT_PADDLE_WATER;
            case ON_LAND -> SoundEvents.BOAT_PADDLE_LAND;
            default -> null;
        };
    }

    public void setPaddleState(boolean p_364965_, boolean p_365347_) {
        this.entityData.set(DATA_ID_PADDLE_LEFT, p_364965_);
        this.entityData.set(DATA_ID_PADDLE_RIGHT, p_365347_);
    }

    public float getRowingTime(int p_364511_, float p_368779_) {
        return this.getPaddleState(p_364511_) ? Mth.clampedLerp(this.paddlePositions[p_364511_] - (float) (Math.PI / 8), this.paddlePositions[p_364511_], p_368779_) : 0.0F;
    }

    @Nullable
    @Override
    public Leashable.LeashData getLeashData() {
        return this.leashData;
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData p_361544_) {
        this.leashData = p_361544_;
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.88F * this.getBbHeight(), 0.64F * this.getBbWidth());
    }

    @Override
    public boolean supportQuadLeash() {
        return true;
    }

    @Override
    public Vec3[] getQuadLeashOffsets() {
        return Leashable.createQuadLeashOffsets(this, 0.0, 0.64, 0.382, 0.88);
    }

    private AbstractBoat.Status getStatus() {
        AbstractBoat.Status abstractboat$status = this.isUnderwater();
        if (abstractboat$status != null) {
            this.waterLevel = this.getBoundingBox().maxY;
            return abstractboat$status;
        } else if (this.checkInWater()) {
            return AbstractBoat.Status.IN_WATER;
        } else {
            float f = this.getGroundFriction();
            if (f > 0.0F) {
                this.landFriction = f;
                return AbstractBoat.Status.ON_LAND;
            } else {
                return AbstractBoat.Status.IN_AIR;
            }
        }
    }

    public float getWaterLevelAbove() {
        AABB aabb = this.getBoundingBox();
        int i = Mth.floor(aabb.minX);
        int j = Mth.ceil(aabb.maxX);
        int k = Mth.floor(aabb.maxY);
        int l = Mth.ceil(aabb.maxY - this.lastYd);
        int i1 = Mth.floor(aabb.minZ);
        int j1 = Mth.ceil(aabb.maxZ);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        label39:
        for (int k1 = k; k1 < l; k1++) {
            float f = 0.0F;

            for (int l1 = i; l1 < j; l1++) {
                for (int i2 = i1; i2 < j1; i2++) {
                    blockpos$mutableblockpos.set(l1, k1, i2);
                    FluidState fluidstate = this.level().getFluidState(blockpos$mutableblockpos);
                    if (this.canBoatInFluid(fluidstate)) {
                        f = Math.max(f, fluidstate.getHeight(this.level(), blockpos$mutableblockpos));
                    }

                    if (f >= 1.0F) {
                        continue label39;
                    }
                }
            }

            if (f < 1.0F) {
                return blockpos$mutableblockpos.getY() + f;
            }
        }

        return l + 1;
    }

    public float getGroundFriction() {
        AABB aabb = this.getBoundingBox();
        AABB aabb1 = new AABB(aabb.minX, aabb.minY - 0.001, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ);
        int i = Mth.floor(aabb1.minX) - 1;
        int j = Mth.ceil(aabb1.maxX) + 1;
        int k = Mth.floor(aabb1.minY) - 1;
        int l = Mth.ceil(aabb1.maxY) + 1;
        int i1 = Mth.floor(aabb1.minZ) - 1;
        int j1 = Mth.ceil(aabb1.maxZ) + 1;
        VoxelShape voxelshape = Shapes.create(aabb1);
        float f = 0.0F;
        int k1 = 0;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int l1 = i; l1 < j; l1++) {
            for (int i2 = i1; i2 < j1; i2++) {
                int j2 = (l1 != i && l1 != j - 1 ? 0 : 1) + (i2 != i1 && i2 != j1 - 1 ? 0 : 1);
                if (j2 != 2) {
                    for (int k2 = k; k2 < l; k2++) {
                        if (j2 <= 0 || k2 != k && k2 != l - 1) {
                            blockpos$mutableblockpos.set(l1, k2, i2);
                            BlockState blockstate = this.level().getBlockState(blockpos$mutableblockpos);
                            if (!(blockstate.getBlock() instanceof WaterlilyBlock)
                                && Shapes.joinIsNotEmpty(
                                    blockstate.getCollisionShape(this.level(), blockpos$mutableblockpos).move(blockpos$mutableblockpos),
                                    voxelshape,
                                    BooleanOp.AND
                                )) {
                                f += blockstate.getFriction(this.level(), blockpos$mutableblockpos, this);
                                k1++;
                            }
                        }
                    }
                }
            }
        }

        return f / k1;
    }

    private boolean checkInWater() {
        AABB aabb = this.getBoundingBox();
        int i = Mth.floor(aabb.minX);
        int j = Mth.ceil(aabb.maxX);
        int k = Mth.floor(aabb.minY);
        int l = Mth.ceil(aabb.minY + 0.001);
        int i1 = Mth.floor(aabb.minZ);
        int j1 = Mth.ceil(aabb.maxZ);
        boolean flag = false;
        this.waterLevel = -Double.MAX_VALUE;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int k1 = i; k1 < j; k1++) {
            for (int l1 = k; l1 < l; l1++) {
                for (int i2 = i1; i2 < j1; i2++) {
                    blockpos$mutableblockpos.set(k1, l1, i2);
                    FluidState fluidstate = this.level().getFluidState(blockpos$mutableblockpos);
                    if (this.canBoatInFluid(fluidstate)) {
                        float f = l1 + fluidstate.getHeight(this.level(), blockpos$mutableblockpos);
                        this.waterLevel = Math.max((double)f, this.waterLevel);
                        flag |= aabb.minY < f;
                    }
                }
            }
        }

        return flag;
    }

    @Nullable
    private AbstractBoat.Status isUnderwater() {
        AABB aabb = this.getBoundingBox();
        double d0 = aabb.maxY + 0.001;
        int i = Mth.floor(aabb.minX);
        int j = Mth.ceil(aabb.maxX);
        int k = Mth.floor(aabb.maxY);
        int l = Mth.ceil(d0);
        int i1 = Mth.floor(aabb.minZ);
        int j1 = Mth.ceil(aabb.maxZ);
        boolean flag = false;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int k1 = i; k1 < j; k1++) {
            for (int l1 = k; l1 < l; l1++) {
                for (int i2 = i1; i2 < j1; i2++) {
                    blockpos$mutableblockpos.set(k1, l1, i2);
                    FluidState fluidstate = this.level().getFluidState(blockpos$mutableblockpos);
                    if (this.canBoatInFluid(fluidstate)
                        && d0 < blockpos$mutableblockpos.getY() + fluidstate.getHeight(this.level(), blockpos$mutableblockpos)) {
                        if (!fluidstate.isSource()) {
                            return AbstractBoat.Status.UNDER_FLOWING_WATER;
                        }

                        flag = true;
                    }
                }
            }
        }

        return flag ? AbstractBoat.Status.UNDER_WATER : null;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    private void floatBoat() {
        double d0 = -this.getGravity();
        double d1 = 0.0;
        float f = 0.05F;
        if (this.oldStatus == AbstractBoat.Status.IN_AIR && this.status != AbstractBoat.Status.IN_AIR && this.status != AbstractBoat.Status.ON_LAND) {
            this.waterLevel = this.getY(1.0);
            double d2 = this.getWaterLevelAbove() - this.getBbHeight() + 0.101;
            if (this.level().noCollision(this, this.getBoundingBox().move(0.0, d2 - this.getY(), 0.0))) {
                this.setPos(this.getX(), d2, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                this.lastYd = 0.0;
            }

            this.status = AbstractBoat.Status.IN_WATER;
        } else {
            if (this.status == AbstractBoat.Status.IN_WATER) {
                d1 = (this.waterLevel - this.getY()) / this.getBbHeight();
                f = 0.9F;
            } else if (this.status == AbstractBoat.Status.UNDER_FLOWING_WATER) {
                d0 = -7.0E-4;
                f = 0.9F;
            } else if (this.status == AbstractBoat.Status.UNDER_WATER) {
                d1 = 0.01F;
                f = 0.45F;
            } else if (this.status == AbstractBoat.Status.IN_AIR) {
                f = 0.9F;
            } else if (this.status == AbstractBoat.Status.ON_LAND) {
                f = this.landFriction;
                if (this.getControllingPassenger() instanceof Player) {
                    this.landFriction /= 2.0F;
                }
            }

            Vec3 vec3 = this.getDeltaMovement();
            this.setDeltaMovement(vec3.x * f, vec3.y + d0, vec3.z * f);
            this.deltaRotation *= f;
            if (d1 > 0.0) {
                Vec3 vec31 = this.getDeltaMovement();
                this.setDeltaMovement(vec31.x, (vec31.y + d1 * (this.getDefaultGravity() / 0.65)) * 0.75, vec31.z);
            }
        }
    }

    private void controlBoat() {
        if (this.isVehicle()) {
            float f = 0.0F;
            if (this.inputLeft) {
                this.deltaRotation--;
            }

            if (this.inputRight) {
                this.deltaRotation++;
            }

            if (this.inputRight != this.inputLeft && !this.inputUp && !this.inputDown) {
                f += 0.005F;
            }

            this.setYRot(this.getYRot() + this.deltaRotation);
            if (this.inputUp) {
                f += 0.04F;
            }

            if (this.inputDown) {
                f -= 0.005F;
            }

            this.setDeltaMovement(
                this.getDeltaMovement()
                    .add(
                        Mth.sin(-this.getYRot() * (float) (Math.PI / 180.0)) * f, 0.0, Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * f
                    )
            );
            this.setPaddleState(this.inputRight && !this.inputLeft || this.inputUp, this.inputLeft && !this.inputRight || this.inputUp);
        }
    }

    protected float getSinglePassengerXOffset() {
        return 0.0F;
    }

    public boolean hasEnoughSpaceFor(Entity p_363801_) {
        return p_363801_.getBbWidth() < this.getBbWidth();
    }

    @Override
    protected void positionRider(Entity p_362738_, Entity.MoveFunction p_365234_) {
        super.positionRider(p_362738_, p_365234_);
        if (!p_362738_.getType().is(EntityTypeTags.CAN_TURN_IN_BOATS)) {
            p_362738_.setYRot(p_362738_.getYRot() + this.deltaRotation);
            p_362738_.setYHeadRot(p_362738_.getYHeadRot() + this.deltaRotation);
            this.clampRotation(p_362738_);
            if (p_362738_ instanceof Animal && this.getPassengers().size() == this.getMaxPassengers()) {
                int i = p_362738_.getId() % 2 == 0 ? 90 : 270;
                p_362738_.setYBodyRot(((Animal)p_362738_).yBodyRot + i);
                p_362738_.setYHeadRot(p_362738_.getYHeadRot() + i);
            }
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity p_367609_) {
        Vec3 vec3 = getCollisionHorizontalEscapeVector(this.getBbWidth() * Mth.SQRT_OF_TWO, p_367609_.getBbWidth(), p_367609_.getYRot());
        double d0 = this.getX() + vec3.x;
        double d1 = this.getZ() + vec3.z;
        BlockPos blockpos = BlockPos.containing(d0, this.getBoundingBox().maxY, d1);
        BlockPos blockpos1 = blockpos.below();
        if (!this.level().isWaterAt(blockpos1)) {
            List<Vec3> list = Lists.newArrayList();
            double d2 = this.level().getBlockFloorHeight(blockpos);
            if (DismountHelper.isBlockFloorValid(d2)) {
                list.add(new Vec3(d0, blockpos.getY() + d2, d1));
            }

            double d3 = this.level().getBlockFloorHeight(blockpos1);
            if (DismountHelper.isBlockFloorValid(d3)) {
                list.add(new Vec3(d0, blockpos1.getY() + d3, d1));
            }

            for (Pose pose : p_367609_.getDismountPoses()) {
                for (Vec3 vec31 : list) {
                    if (DismountHelper.canDismountTo(this.level(), vec31, p_367609_, pose)) {
                        p_367609_.setPose(pose);
                        return vec31;
                    }
                }
            }
        }

        return super.getDismountLocationForPassenger(p_367609_);
    }

    protected void clampRotation(Entity p_365128_) {
        p_365128_.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(p_365128_.getYRot() - this.getYRot());
        float f1 = Mth.clamp(f, -105.0F, 105.0F);
        p_365128_.yRotO += f1 - f;
        p_365128_.setYRot(p_365128_.getYRot() + f1 - f);
        p_365128_.setYHeadRot(p_365128_.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity p_363923_) {
        this.clampRotation(p_363923_);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_408030_) {
        this.writeLeashData(p_408030_, this.leashData);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_406425_) {
        this.readLeashData(p_406425_);
    }

    @Override
    public InteractionResult interact(Player p_367363_, InteractionHand p_362250_) {
        InteractionResult interactionresult = super.interact(p_367363_, p_362250_);
        if (interactionresult != InteractionResult.PASS) {
            return interactionresult;
        } else {
            return (InteractionResult)(p_367363_.isSecondaryUseActive() || !(this.outOfControlTicks < 60.0F) || !this.level().isClientSide && !p_367363_.startRiding(this)
                ? InteractionResult.PASS
                : InteractionResult.SUCCESS);
        }
    }

    @Override
    public void remove(Entity.RemovalReason p_365927_) {
        if (!this.level().isClientSide && p_365927_.shouldDestroy() && this.isLeashed()) {
            this.dropLeash();
        }

        super.remove(p_365927_);
    }

    @Override
    protected void checkFallDamage(double p_361830_, boolean p_361999_, BlockState p_365352_, BlockPos p_367645_) {
        this.lastYd = this.getDeltaMovement().y;
        if (!this.isPassenger()) {
            if (p_361999_) {
                this.resetFallDistance();
            } else if (!this.canBoatInFluid(this.level().getFluidState(this.blockPosition().below())) && p_361830_ < 0.0) {
                this.fallDistance -= (float)p_361830_;
            }
        }
    }

    public boolean getPaddleState(int p_363453_) {
        return this.entityData.get(p_363453_ == 0 ? DATA_ID_PADDLE_LEFT : DATA_ID_PADDLE_RIGHT) && this.getControllingPassenger() != null;
    }

    private void setBubbleTime(int p_362638_) {
        this.entityData.set(DATA_ID_BUBBLE_TIME, p_362638_);
    }

    private int getBubbleTime() {
        return this.entityData.get(DATA_ID_BUBBLE_TIME);
    }

    public float getBubbleAngle(float p_361198_) {
        return Mth.lerp(p_361198_, this.bubbleAngleO, this.bubbleAngle);
    }

    @Override
    protected boolean canAddPassenger(Entity p_366021_) {
        return this.getPassengers().size() < this.getMaxPassengers() && !this.canBoatInFluid(this.getEyeInFluidType());
    }

    protected int getMaxPassengers() {
        return 2;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity livingentity ? livingentity : super.getControllingPassenger();
    }

    public void setInput(boolean p_370030_, boolean p_363750_, boolean p_364020_, boolean p_369506_) {
        this.inputLeft = p_370030_;
        this.inputRight = p_363750_;
        this.inputUp = p_364020_;
        this.inputDown = p_369506_;
    }

    @Override
    public boolean isUnderWater() {
        return this.status == AbstractBoat.Status.UNDER_WATER || this.status == AbstractBoat.Status.UNDER_FLOWING_WATER;
    }

    @Override
    protected final Item getDropItem() {
        return this.dropItem.get();
    }

    @Override
    public final ItemStack getPickResult() {
        return new ItemStack(this.dropItem.get());
    }

    public static enum Status {
        IN_WATER,
        UNDER_WATER,
        UNDER_FLOWING_WATER,
        ON_LAND,
        IN_AIR;
    }
}
