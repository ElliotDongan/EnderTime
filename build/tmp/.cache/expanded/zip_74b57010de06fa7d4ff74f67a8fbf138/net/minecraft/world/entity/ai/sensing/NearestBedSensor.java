package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.pathfinder.Path;

public class NearestBedSensor extends Sensor<Mob> {
    private static final int CACHE_TIMEOUT = 40;
    private static final int BATCH_SIZE = 5;
    private static final int RATE = 20;
    private final Long2LongMap batchCache = new Long2LongOpenHashMap();
    private int triedCount;
    private long lastUpdate;

    public NearestBedSensor() {
        super(20);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_BED);
    }

    protected void doTick(ServerLevel p_26685_, Mob p_26686_) {
        if (p_26686_.isBaby()) {
            this.triedCount = 0;
            this.lastUpdate = p_26685_.getGameTime() + p_26685_.getRandom().nextInt(20);
            PoiManager poimanager = p_26685_.getPoiManager();
            Predicate<BlockPos> predicate = p_26688_ -> {
                long i = p_26688_.asLong();
                if (this.batchCache.containsKey(i)) {
                    return false;
                } else if (++this.triedCount >= 5) {
                    return false;
                } else {
                    this.batchCache.put(i, this.lastUpdate + 40L);
                    return true;
                }
            };
            Set<Pair<Holder<PoiType>, BlockPos>> set = poimanager.findAllWithType(
                    p_217819_ -> p_217819_.is(PoiTypes.HOME), predicate, p_26686_.blockPosition(), 48, PoiManager.Occupancy.ANY
                )
                .collect(Collectors.toSet());
            Path path = AcquirePoi.findPathToPois(p_26686_, set);
            if (path != null && path.canReach()) {
                BlockPos blockpos = path.getTarget();
                Optional<Holder<PoiType>> optional = poimanager.getType(blockpos);
                if (optional.isPresent()) {
                    p_26686_.getBrain().setMemory(MemoryModuleType.NEAREST_BED, blockpos);
                }
            } else if (this.triedCount < 5) {
                this.batchCache.long2LongEntrySet().removeIf(p_217821_ -> p_217821_.getLongValue() < this.lastUpdate);
            }
        }
    }
}