package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class DefendVillageTargetGoal extends TargetGoal {
    private final IronGolem golem;
    @Nullable
    private LivingEntity potentialTarget;
    private final TargetingConditions attackTargeting = TargetingConditions.forCombat().range(64.0);

    public DefendVillageTargetGoal(IronGolem p_26029_) {
        super(p_26029_, false, true);
        this.golem = p_26029_;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        AABB aabb = this.golem.getBoundingBox().inflate(10.0, 8.0, 10.0);
        ServerLevel serverlevel = getServerLevel(this.golem);
        List<? extends LivingEntity> list = serverlevel.getNearbyEntities(Villager.class, this.attackTargeting, this.golem, aabb);
        List<Player> list1 = serverlevel.getNearbyPlayers(this.attackTargeting, this.golem, aabb);

        for (LivingEntity livingentity : list) {
            Villager villager = (Villager)livingentity;

            for (Player player : list1) {
                int i = villager.getPlayerReputation(player);
                if (i <= -100) {
                    this.potentialTarget = player;
                }
            }
        }

        return this.potentialTarget == null ? false : !(this.potentialTarget instanceof Player player1 && (player1.isSpectator() || player1.isCreative()));
    }

    @Override
    public void start() {
        this.golem.setTarget(this.potentialTarget);
        super.start();
    }
}