package net.minecraft.world.entity.animal.frog;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

public class TadpoleAi {
    private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.0F;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING_IN_WATER = 0.5F;
    private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 1.25F;

    protected static Brain<?> makeBrain(Brain<Tadpole> p_218742_) {
        initCoreActivity(p_218742_);
        initIdleActivity(p_218742_);
        p_218742_.setCoreActivities(ImmutableSet.of(Activity.CORE));
        p_218742_.setDefaultActivity(Activity.IDLE);
        p_218742_.useDefaultActivity();
        return p_218742_;
    }

    private static void initCoreActivity(Brain<Tadpole> p_218746_) {
        p_218746_.addActivity(
            Activity.CORE,
            0,
            ImmutableList.of(
                new AnimalPanic<>(2.0F), new LookAtTargetSink(45, 90), new MoveToTargetSink(), new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)
            )
        );
    }

    private static void initIdleActivity(Brain<Tadpole> p_218748_) {
        p_218748_.addActivity(
            Activity.IDLE,
            ImmutableList.of(
                Pair.of(0, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6.0F, UniformInt.of(30, 60))),
                Pair.of(1, new FollowTemptation(p_218740_ -> 1.25F)),
                Pair.of(
                    2,
                    new GateBehavior<>(
                        ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                        ImmutableSet.of(),
                        GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.TRY_ALL,
                        ImmutableList.of(
                            Pair.of(RandomStroll.swim(0.5F), 2),
                            Pair.of(SetWalkTargetFromLookTarget.create(0.5F, 3), 3),
                            Pair.of(BehaviorBuilder.triggerIf(Entity::isInWater), 5)
                        )
                    )
                )
            )
        );
    }

    public static void updateActivity(Tadpole p_218744_) {
        p_218744_.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
    }
}