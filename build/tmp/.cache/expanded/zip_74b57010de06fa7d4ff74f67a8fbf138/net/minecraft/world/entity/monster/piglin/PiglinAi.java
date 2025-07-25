package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BackUpIfTooClose;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CopyMemoryWithExpiry;
import net.minecraft.world.entity.ai.behavior.CrossbowAttack;
import net.minecraft.world.entity.ai.behavior.DismountOrSkipMounting;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.GoToTargetLocation;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.Mount;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StartCelebratingIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.behavior.TriggerGate;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class PiglinAi {
    public static final int REPELLENT_DETECTION_RANGE_HORIZONTAL = 8;
    public static final int REPELLENT_DETECTION_RANGE_VERTICAL = 4;
    public static final Item BARTERING_ITEM = Items.GOLD_INGOT;
    private static final int PLAYER_ANGER_RANGE = 16;
    private static final int ANGER_DURATION = 600;
    private static final int ADMIRE_DURATION = 119;
    private static final int MAX_DISTANCE_TO_WALK_TO_ITEM = 9;
    private static final int MAX_TIME_TO_WALK_TO_ITEM = 200;
    private static final int HOW_LONG_TIME_TO_DISABLE_ADMIRE_WALKING_IF_CANT_REACH_ITEM = 200;
    private static final int CELEBRATION_TIME = 300;
    protected static final UniformInt TIME_BETWEEN_HUNTS = TimeUtil.rangeOfSeconds(30, 120);
    private static final int BABY_FLEE_DURATION_AFTER_GETTING_HIT = 100;
    private static final int HIT_BY_PLAYER_MEMORY_TIMEOUT = 400;
    private static final int MAX_WALK_DISTANCE_TO_START_RIDING = 8;
    private static final UniformInt RIDE_START_INTERVAL = TimeUtil.rangeOfSeconds(10, 40);
    private static final UniformInt RIDE_DURATION = TimeUtil.rangeOfSeconds(10, 30);
    private static final UniformInt RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
    private static final int MELEE_ATTACK_COOLDOWN = 20;
    private static final int EAT_COOLDOWN = 200;
    private static final int DESIRED_DISTANCE_FROM_ENTITY_WHEN_AVOIDING = 12;
    private static final int MAX_LOOK_DIST = 8;
    private static final int MAX_LOOK_DIST_FOR_PLAYER_HOLDING_LOVED_ITEM = 14;
    private static final int INTERACTION_RANGE = 8;
    private static final int MIN_DESIRED_DIST_FROM_TARGET_WHEN_HOLDING_CROSSBOW = 5;
    private static final float SPEED_WHEN_STRAFING_BACK_FROM_TARGET = 0.75F;
    private static final int DESIRED_DISTANCE_FROM_ZOMBIFIED = 6;
    private static final UniformInt AVOID_ZOMBIFIED_DURATION = TimeUtil.rangeOfSeconds(5, 7);
    private static final UniformInt BABY_AVOID_NEMESIS_DURATION = TimeUtil.rangeOfSeconds(5, 7);
    private static final float PROBABILITY_OF_CELEBRATION_DANCE = 0.1F;
    private static final float SPEED_MULTIPLIER_WHEN_AVOIDING = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_RETREATING = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_MOUNTING = 0.8F;
    private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_WANTED_ITEM = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_CELEBRATE_LOCATION = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_DANCING = 0.6F;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;

    protected static Brain<?> makeBrain(Piglin p_34841_, Brain<Piglin> p_34842_) {
        initCoreActivity(p_34842_);
        initIdleActivity(p_34842_);
        initAdmireItemActivity(p_34842_);
        initFightActivity(p_34841_, p_34842_);
        initCelebrateActivity(p_34842_);
        initRetreatActivity(p_34842_);
        initRideHoglinActivity(p_34842_);
        p_34842_.setCoreActivities(ImmutableSet.of(Activity.CORE));
        p_34842_.setDefaultActivity(Activity.IDLE);
        p_34842_.useDefaultActivity();
        return p_34842_;
    }

    protected static void initMemories(Piglin p_219206_, RandomSource p_219207_) {
        int i = TIME_BETWEEN_HUNTS.sample(p_219207_);
        p_219206_.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, i);
    }

    private static void initCoreActivity(Brain<Piglin> p_34821_) {
        p_34821_.addActivity(
            Activity.CORE,
            0,
            ImmutableList.of(
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink(),
                InteractWithDoor.create(),
                babyAvoidNemesis(),
                avoidZombified(),
                StopHoldingItemIfNoLongerAdmiring.create(),
                StartAdmiringItemIfSeen.create(119),
                StartCelebratingIfTargetDead.create(300, PiglinAi::wantsToDance),
                StopBeingAngryIfTargetDead.create()
            )
        );
    }

    private static void initIdleActivity(Brain<Piglin> p_34892_) {
        p_34892_.addActivity(
            Activity.IDLE,
            10,
            ImmutableList.of(
                SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0F),
                StartAttacking.<Piglin>create((p_405531_, p_405532_) -> p_405532_.isAdult(), PiglinAi::findNearestValidAttackTarget),
                BehaviorBuilder.triggerIf(Piglin::canHunt, StartHuntingHoglin.create()),
                avoidRepellent(),
                babySometimesRideBabyHoglin(),
                createIdleLookBehaviors(),
                createIdleMovementBehaviors(),
                SetLookAndInteract.create(EntityType.PLAYER, 4)
            )
        );
    }

    private static void initFightActivity(Piglin p_34904_, Brain<Piglin> p_34905_) {
        p_34905_.addActivityAndRemoveMemoryWhenStopped(
            Activity.FIGHT,
            10,
            ImmutableList.of(
                StopAttackingIfTargetInvalid.create((p_359284_, p_359285_) -> !isNearestValidAttackTarget(p_359284_, p_34904_, p_359285_)),
                BehaviorBuilder.triggerIf(PiglinAi::hasCrossbow, BackUpIfTooClose.create(5, 0.75F)),
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
                MeleeAttack.create(20),
                new CrossbowAttack<>(),
                RememberIfHoglinWasKilled.create(),
                EraseMemoryIf.create(PiglinAi::isNearZombified, MemoryModuleType.ATTACK_TARGET)
            ),
            MemoryModuleType.ATTACK_TARGET
        );
    }

    private static void initCelebrateActivity(Brain<Piglin> p_34921_) {
        p_34921_.addActivityAndRemoveMemoryWhenStopped(
            Activity.CELEBRATE,
            10,
            ImmutableList.of(
                avoidRepellent(),
                SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0F),
                StartAttacking.<Piglin>create((p_405529_, p_405530_) -> p_405530_.isAdult(), PiglinAi::findNearestValidAttackTarget),
                BehaviorBuilder.triggerIf(p_34804_ -> !p_34804_.isDancing(), GoToTargetLocation.create(MemoryModuleType.CELEBRATE_LOCATION, 2, 1.0F)),
                BehaviorBuilder.triggerIf(Piglin::isDancing, GoToTargetLocation.create(MemoryModuleType.CELEBRATE_LOCATION, 4, 0.6F)),
                new RunOne<LivingEntity>(
                    ImmutableList.of(
                        Pair.of(SetEntityLookTarget.create(EntityType.PIGLIN, 8.0F), 1),
                        Pair.of((BehaviorControl<LivingEntity>)(BehaviorControl)RandomStroll.stroll(0.6F, 2, 1), 1),
                        Pair.of(new DoNothing(10, 20), 1)
                    )
                )
            ),
            MemoryModuleType.CELEBRATE_LOCATION
        );
    }

    private static void initAdmireItemActivity(Brain<Piglin> p_34941_) {
        p_34941_.addActivityAndRemoveMemoryWhenStopped(
            Activity.ADMIRE_ITEM,
            10,
            ImmutableList.of(
                GoToWantedItem.create(PiglinAi::isNotHoldingLovedItemInOffHand, 1.0F, true, 9),
                StopAdmiringIfItemTooFarAway.create(9),
                StopAdmiringIfTiredOfTryingToReachItem.create(200, 200)
            ),
            MemoryModuleType.ADMIRING_ITEM
        );
    }

    private static void initRetreatActivity(Brain<Piglin> p_34959_) {
        p_34959_.addActivityAndRemoveMemoryWhenStopped(
            Activity.AVOID,
            10,
            ImmutableList.of(
                SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.0F, 12, true),
                createIdleLookBehaviors(),
                createIdleMovementBehaviors(),
                EraseMemoryIf.create(PiglinAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)
            ),
            MemoryModuleType.AVOID_TARGET
        );
    }

    private static void initRideHoglinActivity(Brain<Piglin> p_34974_) {
        p_34974_.addActivityAndRemoveMemoryWhenStopped(
            Activity.RIDE,
            10,
            ImmutableList.of(
                Mount.create(0.8F),
                SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 8.0F),
                BehaviorBuilder.sequence(
                    BehaviorBuilder.triggerIf(Entity::isPassenger),
                    TriggerGate.triggerOneShuffled(
                        ImmutableList.<Pair<? extends Trigger<? super LivingEntity>, Integer>>builder()
                            .addAll(createLookBehaviors())
                            .add(Pair.of(BehaviorBuilder.triggerIf(p_258950_ -> true), 1))
                            .build()
                    )
                ),
                DismountOrSkipMounting.create(8, PiglinAi::wantsToStopRiding)
            ),
            MemoryModuleType.RIDE_TARGET
        );
    }

    private static ImmutableList<Pair<OneShot<LivingEntity>, Integer>> createLookBehaviors() {
        return ImmutableList.of(
            Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 1),
            Pair.of(SetEntityLookTarget.create(EntityType.PIGLIN, 8.0F), 1),
            Pair.of(SetEntityLookTarget.create(8.0F), 1)
        );
    }

    private static RunOne<LivingEntity> createIdleLookBehaviors() {
        return new RunOne<>(
            ImmutableList.<Pair<? extends BehaviorControl<? super LivingEntity>, Integer>>builder()
                .addAll(createLookBehaviors())
                .add(Pair.of(new DoNothing(30, 60), 1))
                .build()
        );
    }

    private static RunOne<Piglin> createIdleMovementBehaviors() {
        return new RunOne<>(
            ImmutableList.of(
                Pair.of(RandomStroll.stroll(0.6F), 2),
                Pair.of(InteractWith.of(EntityType.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
                Pair.of(BehaviorBuilder.triggerIf(PiglinAi::doesntSeeAnyPlayerHoldingLovedItem, SetWalkTargetFromLookTarget.create(0.6F, 3)), 2),
                Pair.of(new DoNothing(30, 60), 1)
            )
        );
    }

    private static BehaviorControl<PathfinderMob> avoidRepellent() {
        return SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, false);
    }

    private static BehaviorControl<Piglin> babyAvoidNemesis() {
        return CopyMemoryWithExpiry.create(Piglin::isBaby, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.AVOID_TARGET, BABY_AVOID_NEMESIS_DURATION);
    }

    private static BehaviorControl<Piglin> avoidZombified() {
        return CopyMemoryWithExpiry.create(PiglinAi::isNearZombified, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, MemoryModuleType.AVOID_TARGET, AVOID_ZOMBIFIED_DURATION);
    }

    protected static void updateActivity(Piglin p_34899_) {
        Brain<Piglin> brain = p_34899_.getBrain();
        Activity activity = brain.getActiveNonCoreActivity().orElse(null);
        brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.ADMIRE_ITEM, Activity.FIGHT, Activity.AVOID, Activity.CELEBRATE, Activity.RIDE, Activity.IDLE));
        Activity activity1 = brain.getActiveNonCoreActivity().orElse(null);
        if (activity != activity1) {
            getSoundForCurrentActivity(p_34899_).ifPresent(p_34899_::makeSound);
        }

        p_34899_.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
        if (!brain.hasMemoryValue(MemoryModuleType.RIDE_TARGET) && isBabyRidingBaby(p_34899_)) {
            p_34899_.stopRiding();
        }

        if (!brain.hasMemoryValue(MemoryModuleType.CELEBRATE_LOCATION)) {
            brain.eraseMemory(MemoryModuleType.DANCING);
        }

        p_34899_.setDancing(brain.hasMemoryValue(MemoryModuleType.DANCING));
    }

    private static boolean isBabyRidingBaby(Piglin p_34993_) {
        if (!p_34993_.isBaby()) {
            return false;
        } else {
            Entity entity = p_34993_.getVehicle();
            return entity instanceof Piglin && ((Piglin)entity).isBaby() || entity instanceof Hoglin && ((Hoglin)entity).isBaby();
        }
    }

    protected static void pickUpItem(ServerLevel p_363619_, Piglin p_34844_, ItemEntity p_34845_) {
        stopWalking(p_34844_);
        ItemStack itemstack;
        if (p_34845_.getItem().is(Items.GOLD_NUGGET)) {
            p_34844_.take(p_34845_, p_34845_.getItem().getCount());
            itemstack = p_34845_.getItem();
            p_34845_.discard();
        } else {
            p_34844_.take(p_34845_, 1);
            itemstack = removeOneItemFromItemEntity(p_34845_);
        }

        if (isLovedItem(itemstack)) {
            p_34844_.getBrain().eraseMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            holdInOffhand(p_363619_, p_34844_, itemstack);
            admireGoldItem(p_34844_);
        } else if (isFood(itemstack) && !hasEatenRecently(p_34844_)) {
            eat(p_34844_);
        } else {
            boolean flag = !p_34844_.equipItemIfPossible(p_363619_, itemstack).equals(ItemStack.EMPTY);
            if (!flag) {
                putInInventory(p_34844_, itemstack);
            }
        }
    }

    private static void holdInOffhand(ServerLevel p_363193_, Piglin p_34933_, ItemStack p_34934_) {
        if (isHoldingItemInOffHand(p_34933_)) {
            p_34933_.spawnAtLocation(p_363193_, p_34933_.getItemInHand(InteractionHand.OFF_HAND));
        }

        p_34933_.holdInOffHand(p_34934_);
    }

    private static ItemStack removeOneItemFromItemEntity(ItemEntity p_34823_) {
        ItemStack itemstack = p_34823_.getItem();
        ItemStack itemstack1 = itemstack.split(1);
        if (itemstack.isEmpty()) {
            p_34823_.discard();
        } else {
            p_34823_.setItem(itemstack);
        }

        return itemstack1;
    }

    protected static void stopHoldingOffHandItem(ServerLevel p_367801_, Piglin p_34868_, boolean p_34869_) {
        ItemStack itemstack = p_34868_.getItemInHand(InteractionHand.OFF_HAND);
        p_34868_.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        if (p_34868_.isAdult()) {
            boolean flag = isBarterCurrency(itemstack);
            if (p_34869_ && flag) {
                throwItems(p_34868_, getBarterResponseItems(p_34868_));
            } else if (!flag) {
                boolean flag1 = !p_34868_.equipItemIfPossible(p_367801_, itemstack).isEmpty();
                if (!flag1) {
                    putInInventory(p_34868_, itemstack);
                }
            }
        } else {
            boolean flag2 = !p_34868_.equipItemIfPossible(p_367801_, itemstack).isEmpty();
            if (!flag2) {
                ItemStack itemstack1 = p_34868_.getMainHandItem();
                if (isLovedItem(itemstack1)) {
                    putInInventory(p_34868_, itemstack1);
                } else {
                    throwItems(p_34868_, Collections.singletonList(itemstack1));
                }

                p_34868_.holdInMainHand(itemstack);
            }
        }
    }

    protected static void cancelAdmiring(ServerLevel p_368810_, Piglin p_34928_) {
        if (isAdmiringItem(p_34928_) && !p_34928_.getOffhandItem().isEmpty()) {
            p_34928_.spawnAtLocation(p_368810_, p_34928_.getOffhandItem());
            p_34928_.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    private static void putInInventory(Piglin p_34953_, ItemStack p_34954_) {
        ItemStack itemstack = p_34953_.addToInventory(p_34954_);
        throwItemsTowardRandomPos(p_34953_, Collections.singletonList(itemstack));
    }

    private static void throwItems(Piglin p_34861_, List<ItemStack> p_34862_) {
        Optional<Player> optional = p_34861_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        if (optional.isPresent()) {
            throwItemsTowardPlayer(p_34861_, optional.get(), p_34862_);
        } else {
            throwItemsTowardRandomPos(p_34861_, p_34862_);
        }
    }

    private static void throwItemsTowardRandomPos(Piglin p_34913_, List<ItemStack> p_34914_) {
        throwItemsTowardPos(p_34913_, p_34914_, getRandomNearbyPos(p_34913_));
    }

    private static void throwItemsTowardPlayer(Piglin p_34851_, Player p_34852_, List<ItemStack> p_34853_) {
        throwItemsTowardPos(p_34851_, p_34853_, p_34852_.position());
    }

    private static void throwItemsTowardPos(Piglin p_34864_, List<ItemStack> p_34865_, Vec3 p_34866_) {
        if (!p_34865_.isEmpty()) {
            p_34864_.swing(InteractionHand.OFF_HAND);

            for (ItemStack itemstack : p_34865_) {
                BehaviorUtils.throwItem(p_34864_, itemstack, p_34866_.add(0.0, 1.0, 0.0));
            }
        }
    }

    private static List<ItemStack> getBarterResponseItems(Piglin p_34997_) {
        LootTable loottable = p_34997_.level().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.PIGLIN_BARTERING);
        List<ItemStack> list = loottable.getRandomItems(
            new LootParams.Builder((ServerLevel)p_34997_.level()).withParameter(LootContextParams.THIS_ENTITY, p_34997_).create(LootContextParamSets.PIGLIN_BARTER)
        );
        return list;
    }

    private static boolean wantsToDance(LivingEntity p_34811_, LivingEntity p_34812_) {
        return p_34812_.getType() != EntityType.HOGLIN ? false : RandomSource.create(p_34811_.level().getGameTime()).nextFloat() < 0.1F;
    }

    protected static boolean wantsToPickup(Piglin p_34858_, ItemStack p_34859_) {
        if (p_34858_.isBaby() && p_34859_.is(ItemTags.IGNORED_BY_PIGLIN_BABIES)) {
            return false;
        } else if (p_34859_.is(ItemTags.PIGLIN_REPELLENTS)) {
            return false;
        } else if (isAdmiringDisabled(p_34858_) && p_34858_.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            return false;
        } else if (isBarterCurrency(p_34859_)) {
            return isNotHoldingLovedItemInOffHand(p_34858_);
        } else {
            boolean flag = p_34858_.canAddToInventory(p_34859_);
            if (p_34859_.is(Items.GOLD_NUGGET)) {
                return flag;
            } else if (isFood(p_34859_)) {
                return !hasEatenRecently(p_34858_) && flag;
            } else {
                return !isLovedItem(p_34859_) ? p_34858_.canReplaceCurrentItem(p_34859_) : isNotHoldingLovedItemInOffHand(p_34858_) && flag;
            }
        }
    }

    protected static boolean isLovedItem(ItemStack p_149966_) {
        return p_149966_.is(ItemTags.PIGLIN_LOVED);
    }

    private static boolean wantsToStopRiding(Piglin p_34835_, Entity p_34836_) {
        return !(p_34836_ instanceof Mob mob)
            ? false
            : !mob.isBaby() || !mob.isAlive() || wasHurtRecently(p_34835_) || wasHurtRecently(mob) || mob instanceof Piglin && mob.getVehicle() == null;
    }

    private static boolean isNearestValidAttackTarget(ServerLevel p_362158_, Piglin p_34901_, LivingEntity p_34902_) {
        return findNearestValidAttackTarget(p_362158_, p_34901_).filter(p_34887_ -> p_34887_ == p_34902_).isPresent();
    }

    private static boolean isNearZombified(Piglin p_34999_) {
        Brain<Piglin> brain = p_34999_.getBrain();
        if (brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED)) {
            LivingEntity livingentity = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED).get();
            return p_34999_.closerThan(livingentity, 6.0);
        } else {
            return false;
        }
    }

    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(ServerLevel p_368686_, Piglin p_35001_) {
        Brain<Piglin> brain = p_35001_.getBrain();
        if (isNearZombified(p_35001_)) {
            return Optional.empty();
        } else {
            Optional<LivingEntity> optional = BehaviorUtils.getLivingEntityFromUUIDMemory(p_35001_, MemoryModuleType.ANGRY_AT);
            if (optional.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(p_368686_, p_35001_, optional.get())) {
                return optional;
            } else {
                if (brain.hasMemoryValue(MemoryModuleType.UNIVERSAL_ANGER)) {
                    Optional<Player> optional1 = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
                    if (optional1.isPresent()) {
                        return optional1;
                    }
                }

                Optional<Mob> optional3 = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
                if (optional3.isPresent()) {
                    return optional3;
                } else {
                    Optional<Player> optional2 = brain.getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
                    return optional2.isPresent() && Sensor.isEntityAttackable(p_368686_, p_35001_, optional2.get()) ? optional2 : Optional.empty();
                }
            }
        }
    }

    public static void angerNearbyPiglins(ServerLevel p_362859_, Player p_34874_, boolean p_34875_) {
        List<Piglin> list = p_34874_.level().getEntitiesOfClass(Piglin.class, p_34874_.getBoundingBox().inflate(16.0));
        list.stream().filter(PiglinAi::isIdle).filter(p_34881_ -> !p_34875_ || BehaviorUtils.canSee(p_34881_, p_34874_)).forEach(p_359282_ -> {
            if (p_362859_.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                setAngerTargetToNearestTargetablePlayerIfFound(p_362859_, p_359282_, p_34874_);
            } else {
                setAngerTarget(p_362859_, p_359282_, p_34874_);
            }
        });
    }

    public static InteractionResult mobInteract(ServerLevel p_362693_, Piglin p_34847_, Player p_34848_, InteractionHand p_34849_) {
        ItemStack itemstack = p_34848_.getItemInHand(p_34849_);
        if (canAdmire(p_34847_, itemstack)) {
            ItemStack itemstack1 = itemstack.consumeAndReturn(1, p_34848_);
            holdInOffhand(p_362693_, p_34847_, itemstack1);
            admireGoldItem(p_34847_);
            stopWalking(p_34847_);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    protected static boolean canAdmire(Piglin p_34910_, ItemStack p_34911_) {
        return !isAdmiringDisabled(p_34910_) && !isAdmiringItem(p_34910_) && p_34910_.isAdult() && isBarterCurrency(p_34911_);
    }

    protected static void wasHurtBy(ServerLevel p_361749_, Piglin p_34838_, LivingEntity p_34839_) {
        if (!(p_34839_ instanceof Piglin)) {
            if (isHoldingItemInOffHand(p_34838_)) {
                stopHoldingOffHandItem(p_361749_, p_34838_, false);
            }

            Brain<Piglin> brain = p_34838_.getBrain();
            brain.eraseMemory(MemoryModuleType.CELEBRATE_LOCATION);
            brain.eraseMemory(MemoryModuleType.DANCING);
            brain.eraseMemory(MemoryModuleType.ADMIRING_ITEM);
            if (p_34839_ instanceof Player) {
                brain.setMemoryWithExpiry(MemoryModuleType.ADMIRING_DISABLED, true, 400L);
            }

            getAvoidTarget(p_34838_).ifPresent(p_405523_ -> {
                if (p_405523_.getType() != p_34839_.getType()) {
                    brain.eraseMemory(MemoryModuleType.AVOID_TARGET);
                }
            });
            if (p_34838_.isBaby()) {
                brain.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, p_34839_, 100L);
                if (Sensor.isEntityAttackableIgnoringLineOfSight(p_361749_, p_34838_, p_34839_)) {
                    broadcastAngerTarget(p_361749_, p_34838_, p_34839_);
                }
            } else if (p_34839_.getType() == EntityType.HOGLIN && hoglinsOutnumberPiglins(p_34838_)) {
                setAvoidTargetAndDontHuntForAWhile(p_34838_, p_34839_);
                broadcastRetreat(p_34838_, p_34839_);
            } else {
                maybeRetaliate(p_361749_, p_34838_, p_34839_);
            }
        }
    }

    protected static void maybeRetaliate(ServerLevel p_362623_, AbstractPiglin p_34827_, LivingEntity p_34828_) {
        if (!p_34827_.getBrain().isActive(Activity.AVOID)) {
            if (Sensor.isEntityAttackableIgnoringLineOfSight(p_362623_, p_34827_, p_34828_)) {
                if (!BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(p_34827_, p_34828_, 4.0)) {
                    if (p_34828_.getType() == EntityType.PLAYER && p_362623_.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                        setAngerTargetToNearestTargetablePlayerIfFound(p_362623_, p_34827_, p_34828_);
                        broadcastUniversalAnger(p_362623_, p_34827_);
                    } else {
                        setAngerTarget(p_362623_, p_34827_, p_34828_);
                        broadcastAngerTarget(p_362623_, p_34827_, p_34828_);
                    }
                }
            }
        }
    }

    public static Optional<SoundEvent> getSoundForCurrentActivity(Piglin p_34948_) {
        return p_34948_.getBrain().getActiveNonCoreActivity().map(p_34908_ -> getSoundForActivity(p_34948_, p_34908_));
    }

    private static SoundEvent getSoundForActivity(Piglin p_34855_, Activity p_34856_) {
        if (p_34856_ == Activity.FIGHT) {
            return SoundEvents.PIGLIN_ANGRY;
        } else if (p_34855_.isConverting()) {
            return SoundEvents.PIGLIN_RETREAT;
        } else if (p_34856_ == Activity.AVOID && isNearAvoidTarget(p_34855_)) {
            return SoundEvents.PIGLIN_RETREAT;
        } else if (p_34856_ == Activity.ADMIRE_ITEM) {
            return SoundEvents.PIGLIN_ADMIRING_ITEM;
        } else if (p_34856_ == Activity.CELEBRATE) {
            return SoundEvents.PIGLIN_CELEBRATE;
        } else if (seesPlayerHoldingLovedItem(p_34855_)) {
            return SoundEvents.PIGLIN_JEALOUS;
        } else {
            return isNearRepellent(p_34855_) ? SoundEvents.PIGLIN_RETREAT : SoundEvents.PIGLIN_AMBIENT;
        }
    }

    private static boolean isNearAvoidTarget(Piglin p_35003_) {
        Brain<Piglin> brain = p_35003_.getBrain();
        return !brain.hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? false : brain.getMemory(MemoryModuleType.AVOID_TARGET).get().closerThan(p_35003_, 12.0);
    }

    protected static List<AbstractPiglin> getVisibleAdultPiglins(Piglin p_35005_) {
        return p_35005_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).orElse(ImmutableList.of());
    }

    private static List<AbstractPiglin> getAdultPiglins(AbstractPiglin p_34961_) {
        return p_34961_.getBrain().getMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS).orElse(ImmutableList.of());
    }

    public static boolean isWearingSafeArmor(LivingEntity p_365495_) {
        for (EquipmentSlot equipmentslot : EquipmentSlotGroup.ARMOR) {
            if (p_365495_.getItemBySlot(equipmentslot).makesPiglinsNeutral(p_365495_)) {
                return true;
            }
        }

        return false;
    }

    private static void stopWalking(Piglin p_35007_) {
        p_35007_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        p_35007_.getNavigation().stop();
    }

    private static BehaviorControl<LivingEntity> babySometimesRideBabyHoglin() {
        SetEntityLookTargetSometimes.Ticker setentitylooktargetsometimes$ticker = new SetEntityLookTargetSometimes.Ticker(RIDE_START_INTERVAL);
        return CopyMemoryWithExpiry.create(
            p_405525_ -> p_405525_.isBaby() && setentitylooktargetsometimes$ticker.tickDownAndCheck(p_405525_.level().random),
            MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
            MemoryModuleType.RIDE_TARGET,
            RIDE_DURATION
        );
    }

    protected static void broadcastAngerTarget(ServerLevel p_368320_, AbstractPiglin p_366359_, LivingEntity p_364430_) {
        getAdultPiglins(p_366359_).forEach(p_405528_ -> {
            if (p_364430_.getType() != EntityType.HOGLIN || p_405528_.canHunt() && ((Hoglin)p_364430_).canBeHunted()) {
                setAngerTargetIfCloserThanCurrent(p_368320_, p_405528_, p_364430_);
            }
        });
    }

    protected static void broadcastUniversalAnger(ServerLevel p_369015_, AbstractPiglin p_34896_) {
        getAdultPiglins(p_34896_).forEach(p_359293_ -> getNearestVisibleTargetablePlayer(p_359293_).ifPresent(p_359276_ -> setAngerTarget(p_369015_, p_359293_, p_359276_)));
    }

    protected static void setAngerTarget(ServerLevel p_364284_, AbstractPiglin p_34925_, LivingEntity p_34926_) {
        if (Sensor.isEntityAttackableIgnoringLineOfSight(p_364284_, p_34925_, p_34926_)) {
            p_34925_.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            p_34925_.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, p_34926_.getUUID(), 600L);
            if (p_34926_.getType() == EntityType.HOGLIN && p_34925_.canHunt()) {
                dontKillAnyMoreHoglinsForAWhile(p_34925_);
            }

            if (p_34926_.getType() == EntityType.PLAYER && p_364284_.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                p_34925_.getBrain().setMemoryWithExpiry(MemoryModuleType.UNIVERSAL_ANGER, true, 600L);
            }
        }
    }

    private static void setAngerTargetToNearestTargetablePlayerIfFound(ServerLevel p_368737_, AbstractPiglin p_34945_, LivingEntity p_34946_) {
        Optional<Player> optional = getNearestVisibleTargetablePlayer(p_34945_);
        if (optional.isPresent()) {
            setAngerTarget(p_368737_, p_34945_, optional.get());
        } else {
            setAngerTarget(p_368737_, p_34945_, p_34946_);
        }
    }

    private static void setAngerTargetIfCloserThanCurrent(ServerLevel p_365346_, AbstractPiglin p_34963_, LivingEntity p_34964_) {
        Optional<LivingEntity> optional = getAngerTarget(p_34963_);
        LivingEntity livingentity = BehaviorUtils.getNearestTarget(p_34963_, optional, p_34964_);
        if (!optional.isPresent() || optional.get() != livingentity) {
            setAngerTarget(p_365346_, p_34963_, livingentity);
        }
    }

    private static Optional<LivingEntity> getAngerTarget(AbstractPiglin p_34976_) {
        return BehaviorUtils.getLivingEntityFromUUIDMemory(p_34976_, MemoryModuleType.ANGRY_AT);
    }

    public static Optional<LivingEntity> getAvoidTarget(Piglin p_34987_) {
        return p_34987_.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? p_34987_.getBrain().getMemory(MemoryModuleType.AVOID_TARGET) : Optional.empty();
    }

    public static Optional<Player> getNearestVisibleTargetablePlayer(AbstractPiglin p_34894_) {
        return p_34894_.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER) ? p_34894_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER) : Optional.empty();
    }

    private static void broadcastRetreat(Piglin p_34930_, LivingEntity p_34931_) {
        getVisibleAdultPiglins(p_34930_).stream().filter(p_34985_ -> p_34985_ instanceof Piglin).forEach(p_34819_ -> retreatFromNearestTarget((Piglin)p_34819_, p_34931_));
    }

    private static void retreatFromNearestTarget(Piglin p_34950_, LivingEntity p_34951_) {
        Brain<Piglin> brain = p_34950_.getBrain();
        LivingEntity $$3 = BehaviorUtils.getNearestTarget(p_34950_, brain.getMemory(MemoryModuleType.AVOID_TARGET), p_34951_);
        $$3 = BehaviorUtils.getNearestTarget(p_34950_, brain.getMemory(MemoryModuleType.ATTACK_TARGET), $$3);
        setAvoidTargetAndDontHuntForAWhile(p_34950_, $$3);
    }

    private static boolean wantsToStopFleeing(Piglin p_35009_) {
        Brain<Piglin> brain = p_35009_.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
            return true;
        } else {
            LivingEntity livingentity = brain.getMemory(MemoryModuleType.AVOID_TARGET).get();
            EntityType<?> entitytype = livingentity.getType();
            if (entitytype == EntityType.HOGLIN) {
                return piglinsEqualOrOutnumberHoglins(p_35009_);
            } else {
                return isZombified(entitytype) ? !brain.isMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, livingentity) : false;
            }
        }
    }

    private static boolean piglinsEqualOrOutnumberHoglins(Piglin p_35011_) {
        return !hoglinsOutnumberPiglins(p_35011_);
    }

    private static boolean hoglinsOutnumberPiglins(Piglin p_35013_) {
        int i = p_35013_.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0) + 1;
        int j = p_35013_.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0);
        return j > i;
    }

    private static void setAvoidTargetAndDontHuntForAWhile(Piglin p_34968_, LivingEntity p_34969_) {
        p_34968_.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        p_34968_.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        p_34968_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        p_34968_.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, p_34969_, RETREAT_DURATION.sample(p_34968_.level().random));
        dontKillAnyMoreHoglinsForAWhile(p_34968_);
    }

    protected static void dontKillAnyMoreHoglinsForAWhile(AbstractPiglin p_34923_) {
        p_34923_.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, TIME_BETWEEN_HUNTS.sample(p_34923_.level().random));
    }

    private static void eat(Piglin p_35015_) {
        p_35015_.getBrain().setMemoryWithExpiry(MemoryModuleType.ATE_RECENTLY, true, 200L);
    }

    private static Vec3 getRandomNearbyPos(Piglin p_35017_) {
        Vec3 vec3 = LandRandomPos.getPos(p_35017_, 4, 2);
        return vec3 == null ? p_35017_.position() : vec3;
    }

    private static boolean hasEatenRecently(Piglin p_35019_) {
        return p_35019_.getBrain().hasMemoryValue(MemoryModuleType.ATE_RECENTLY);
    }

    protected static boolean isIdle(AbstractPiglin p_34943_) {
        return p_34943_.getBrain().isActive(Activity.IDLE);
    }

    private static boolean hasCrossbow(LivingEntity p_34919_) {
        return p_34919_.isHolding(is -> is.getItem() instanceof net.minecraft.world.item.CrossbowItem);
    }

    private static void admireGoldItem(LivingEntity p_34939_) {
        p_34939_.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, 119L);
    }

    private static boolean isAdmiringItem(Piglin p_35021_) {
        return p_35021_.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM);
    }

    private static boolean isBarterCurrency(ItemStack p_149968_) {
        return p_149968_.is(BARTERING_ITEM) || p_149968_.isPiglinCurrency();
    }

    private static boolean isFood(ItemStack p_149970_) {
        return p_149970_.is(ItemTags.PIGLIN_FOOD);
    }

    private static boolean isNearRepellent(Piglin p_35023_) {
        return p_35023_.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_REPELLENT);
    }

    private static boolean seesPlayerHoldingLovedItem(LivingEntity p_34972_) {
        return p_34972_.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
    }

    private static boolean doesntSeeAnyPlayerHoldingLovedItem(LivingEntity p_34983_) {
        return !seesPlayerHoldingLovedItem(p_34983_);
    }

    public static boolean isPlayerHoldingLovedItem(LivingEntity p_34884_) {
        return p_34884_.getType() == EntityType.PLAYER && p_34884_.isHolding(PiglinAi::isLovedItem);
    }

    private static boolean isAdmiringDisabled(Piglin p_35025_) {
        return p_35025_.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_DISABLED);
    }

    private static boolean wasHurtRecently(LivingEntity p_34989_) {
        return p_34989_.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
    }

    private static boolean isHoldingItemInOffHand(Piglin p_35027_) {
        return !p_35027_.getOffhandItem().isEmpty();
    }

    private static boolean isNotHoldingLovedItemInOffHand(Piglin p_35029_) {
        return p_35029_.getOffhandItem().isEmpty() || !isLovedItem(p_35029_.getOffhandItem());
    }

    public static boolean isZombified(EntityType<?> p_34807_) {
        return p_34807_ == EntityType.ZOMBIFIED_PIGLIN || p_34807_ == EntityType.ZOGLIN;
    }
}
