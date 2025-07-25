package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class AssignProfessionFromJobSite {
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(
            p_258312_ -> p_258312_.group(p_258312_.present(MemoryModuleType.POTENTIAL_JOB_SITE), p_258312_.registered(MemoryModuleType.JOB_SITE))
                .apply(
                    p_258312_,
                    (p_258304_, p_258305_) -> (p_258309_, p_258310_, p_258311_) -> {
                        GlobalPos globalpos = p_258312_.get(p_258304_);
                        if (!globalpos.pos().closerToCenterThan(p_258310_.position(), 2.0) && !p_258310_.assignProfessionWhenSpawned()) {
                            return false;
                        } else {
                            p_258304_.erase();
                            p_258305_.set(globalpos);
                            p_258309_.broadcastEntityEvent(p_258310_, (byte)14);
                            if (!p_258310_.getVillagerData().profession().is(VillagerProfession.NONE)) {
                                return true;
                            } else {
                                MinecraftServer minecraftserver = p_258309_.getServer();
                                Optional.ofNullable(minecraftserver.getLevel(globalpos.dimension()))
                                    .flatMap(p_22467_ -> p_22467_.getPoiManager().getType(globalpos.pos()))
                                    .flatMap(
                                        p_390545_ -> BuiltInRegistries.VILLAGER_PROFESSION
                                            .listElements()
                                            .filter(p_390547_ -> p_390547_.value().heldJobSite().test((Holder<PoiType>)p_390545_))
                                            .findFirst()
                                    )
                                    .ifPresent(p_390550_ -> {
                                        p_258310_.setVillagerData(p_258310_.getVillagerData().withProfession(p_390550_));
                                        p_258310_.refreshBrain(p_258309_);
                                    });
                                return true;
                            }
                        }
                    }
                )
        );
    }
}