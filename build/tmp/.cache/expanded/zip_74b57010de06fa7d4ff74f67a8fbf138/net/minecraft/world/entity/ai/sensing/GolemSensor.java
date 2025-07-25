package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class GolemSensor extends Sensor<LivingEntity> {
    private static final int GOLEM_SCAN_RATE = 200;
    private static final int MEMORY_TIME_TO_LIVE = 599;

    public GolemSensor() {
        this(200);
    }

    public GolemSensor(int p_26642_) {
        super(p_26642_);
    }

    @Override
    protected void doTick(ServerLevel p_26645_, LivingEntity p_26646_) {
        checkForNearbyGolem(p_26646_);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES);
    }

    public static void checkForNearbyGolem(LivingEntity p_26648_) {
        Optional<List<LivingEntity>> optional = p_26648_.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
        if (!optional.isEmpty()) {
            boolean flag = optional.get().stream().anyMatch(p_405420_ -> p_405420_.getType().equals(EntityType.IRON_GOLEM));
            if (flag) {
                golemDetected(p_26648_);
            }
        }
    }

    public static void golemDetected(LivingEntity p_26650_) {
        p_26650_.getBrain().setMemoryWithExpiry(MemoryModuleType.GOLEM_DETECTED_RECENTLY, true, 599L);
    }
}