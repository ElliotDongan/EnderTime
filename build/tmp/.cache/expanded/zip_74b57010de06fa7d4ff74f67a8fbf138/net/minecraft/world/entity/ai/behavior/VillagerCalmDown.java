package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class VillagerCalmDown {
    private static final int SAFE_DISTANCE_FROM_DANGER = 36;

    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(
            p_258884_ -> p_258884_.group(
                    p_258884_.registered(MemoryModuleType.HURT_BY),
                    p_258884_.registered(MemoryModuleType.HURT_BY_ENTITY),
                    p_258884_.registered(MemoryModuleType.NEAREST_HOSTILE)
                )
                .apply(
                    p_258884_,
                    (p_258886_, p_258887_, p_258888_) -> (p_390614_, p_390615_, p_390616_) -> {
                        boolean flag = p_258884_.tryGet(p_258886_).isPresent()
                            || p_258884_.tryGet(p_258888_).isPresent()
                            || p_258884_.<LivingEntity>tryGet(p_258887_).filter(p_326904_ -> p_326904_.distanceToSqr(p_390615_) <= 36.0).isPresent();
                        if (!flag) {
                            p_258886_.erase();
                            p_258887_.erase();
                            p_390615_.getBrain().updateActivityFromSchedule(p_390614_.getDayTime(), p_390614_.getGameTime());
                        }

                        return true;
                    }
                )
        );
    }
}