package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.schedule.Activity;

public class PiglinBruteAi {
    private static final int ANGER_DURATION = 600;
    private static final int MELEE_ATTACK_COOLDOWN = 20;
    private static final double ACTIVITY_SOUND_LIKELIHOOD_PER_TICK = 0.0125;
    private static final int MAX_LOOK_DIST = 8;
    private static final int INTERACTION_RANGE = 8;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;
    private static final int HOME_CLOSE_ENOUGH_DISTANCE = 2;
    private static final int HOME_TOO_FAR_DISTANCE = 100;
    private static final int HOME_STROLL_AROUND_DISTANCE = 5;

    protected static Brain<?> makeBrain(PiglinBrute p_35100_, Brain<PiglinBrute> p_35101_) {
        initCoreActivity(p_35100_, p_35101_);
        initIdleActivity(p_35100_, p_35101_);
        initFightActivity(p_35100_, p_35101_);
        p_35101_.setCoreActivities(ImmutableSet.of(Activity.CORE));
        p_35101_.setDefaultActivity(Activity.IDLE);
        p_35101_.useDefaultActivity();
        return p_35101_;
    }

    protected static void initMemories(PiglinBrute p_35095_) {
        GlobalPos globalpos = GlobalPos.of(p_35095_.level().dimension(), p_35095_.blockPosition());
        p_35095_.getBrain().setMemory(MemoryModuleType.HOME, globalpos);
    }

    private static void initCoreActivity(PiglinBrute p_35112_, Brain<PiglinBrute> p_35113_) {
        p_35113_.addActivity(
            Activity.CORE,
            0,
            ImmutableList.of(new LookAtTargetSink(45, 90), new MoveToTargetSink(), InteractWithDoor.create(), StopBeingAngryIfTargetDead.create())
        );
    }

    private static void initIdleActivity(PiglinBrute p_35120_, Brain<PiglinBrute> p_35121_) {
        p_35121_.addActivity(
            Activity.IDLE,
            10,
            ImmutableList.of(
                StartAttacking.<PiglinBrute>create(PiglinBruteAi::findNearestValidAttackTarget), createIdleLookBehaviors(), createIdleMovementBehaviors(), SetLookAndInteract.create(EntityType.PLAYER, 4)
            )
        );
    }

    private static void initFightActivity(PiglinBrute p_35125_, Brain<PiglinBrute> p_35126_) {
        p_35126_.addActivityAndRemoveMemoryWhenStopped(
            Activity.FIGHT,
            10,
            ImmutableList.of(
                StopAttackingIfTargetInvalid.create((p_359295_, p_359296_) -> !isNearestValidAttackTarget(p_359295_, p_35125_, p_359296_)),
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
                MeleeAttack.create(20)
            ),
            MemoryModuleType.ATTACK_TARGET
        );
    }

    private static RunOne<PiglinBrute> createIdleLookBehaviors() {
        return new RunOne<>(
            ImmutableList.of(
                Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(EntityType.PIGLIN, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(EntityType.PIGLIN_BRUTE, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(8.0F), 1),
                Pair.of(new DoNothing(30, 60), 1)
            )
        );
    }

    private static RunOne<PiglinBrute> createIdleMovementBehaviors() {
        return new RunOne<>(
            ImmutableList.of(
                Pair.of(RandomStroll.stroll(0.6F), 2),
                Pair.of(InteractWith.of(EntityType.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
                Pair.of(InteractWith.of(EntityType.PIGLIN_BRUTE, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
                Pair.of(StrollToPoi.create(MemoryModuleType.HOME, 0.6F, 2, 100), 2),
                Pair.of(StrollAroundPoi.create(MemoryModuleType.HOME, 0.6F, 5), 2),
                Pair.of(new DoNothing(30, 60), 1)
            )
        );
    }

    protected static void updateActivity(PiglinBrute p_35110_) {
        Brain<PiglinBrute> brain = p_35110_.getBrain();
        Activity activity = brain.getActiveNonCoreActivity().orElse(null);
        brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
        Activity activity1 = brain.getActiveNonCoreActivity().orElse(null);
        if (activity != activity1) {
            playActivitySound(p_35110_);
        }

        p_35110_.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
    }

    private static boolean isNearestValidAttackTarget(ServerLevel p_363954_, AbstractPiglin p_35089_, LivingEntity p_35090_) {
        return findNearestValidAttackTarget(p_363954_, p_35089_).filter(p_35085_ -> p_35085_ == p_35090_).isPresent();
    }

    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(ServerLevel p_364523_, AbstractPiglin p_35087_) {
        Optional<LivingEntity> optional = BehaviorUtils.getLivingEntityFromUUIDMemory(p_35087_, MemoryModuleType.ANGRY_AT);
        if (optional.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(p_364523_, p_35087_, optional.get())) {
            return optional;
        } else {
            Optional<? extends LivingEntity> optional1 = p_35087_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
            return optional1.isPresent() ? optional1 : p_35087_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
        }
    }

    protected static void wasHurtBy(ServerLevel p_367846_, PiglinBrute p_35097_, LivingEntity p_35098_) {
        if (!(p_35098_ instanceof AbstractPiglin)) {
            PiglinAi.maybeRetaliate(p_367846_, p_35097_, p_35098_);
        }
    }

    protected static void setAngerTarget(PiglinBrute p_149989_, LivingEntity p_149990_) {
        p_149989_.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        p_149989_.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, p_149990_.getUUID(), 600L);
    }

    protected static void maybePlayActivitySound(PiglinBrute p_35115_) {
        if (p_35115_.level().random.nextFloat() < 0.0125) {
            playActivitySound(p_35115_);
        }
    }

    private static void playActivitySound(PiglinBrute p_35123_) {
        p_35123_.getBrain().getActiveNonCoreActivity().ifPresent(p_35104_ -> {
            if (p_35104_ == Activity.FIGHT) {
                p_35123_.playAngrySound();
            }
        });
    }
}