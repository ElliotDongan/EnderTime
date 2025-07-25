package net.minecraft.world.entity.animal;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FollowFlockLeaderGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public abstract class AbstractSchoolingFish extends AbstractFish {
    @Nullable
    private AbstractSchoolingFish leader;
    private int schoolSize = 1;

    public AbstractSchoolingFish(EntityType<? extends AbstractSchoolingFish> p_27523_, Level p_27524_) {
        super(p_27523_, p_27524_);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(5, new FollowFlockLeaderGoal(this));
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return this.getMaxSchoolSize();
    }

    public int getMaxSchoolSize() {
        return super.getMaxSpawnClusterSize();
    }

    @Override
    protected boolean canRandomSwim() {
        return !this.isFollower();
    }

    public boolean isFollower() {
        return this.leader != null && this.leader.isAlive();
    }

    public AbstractSchoolingFish startFollowing(AbstractSchoolingFish p_27526_) {
        this.leader = p_27526_;
        p_27526_.addFollower();
        return p_27526_;
    }

    public void stopFollowing() {
        this.leader.removeFollower();
        this.leader = null;
    }

    private void addFollower() {
        this.schoolSize++;
    }

    private void removeFollower() {
        this.schoolSize--;
    }

    public boolean canBeFollowed() {
        return this.hasFollowers() && this.schoolSize < this.getMaxSchoolSize();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.hasFollowers() && this.level().random.nextInt(200) == 1) {
            List<? extends AbstractFish> list = this.level()
                .getEntitiesOfClass((Class<? extends AbstractFish>)this.getClass(), this.getBoundingBox().inflate(8.0, 8.0, 8.0));
            if (list.size() <= 1) {
                this.schoolSize = 1;
            }
        }
    }

    public boolean hasFollowers() {
        return this.schoolSize > 1;
    }

    public boolean inRangeOfLeader() {
        return this.distanceToSqr(this.leader) <= 121.0;
    }

    public void pathToLeader() {
        if (this.isFollower()) {
            this.getNavigation().moveTo(this.leader, 1.0);
        }
    }

    public void addFollowers(Stream<? extends AbstractSchoolingFish> p_27534_) {
        p_27534_.limit(this.getMaxSchoolSize() - this.schoolSize).filter(p_27538_ -> p_27538_ != this).forEach(p_27536_ -> p_27536_.startFollowing(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_27528_, DifficultyInstance p_27529_, EntitySpawnReason p_367765_, @Nullable SpawnGroupData p_27531_) {
        super.finalizeSpawn(p_27528_, p_27529_, p_367765_, p_27531_);
        if (p_27531_ == null) {
            p_27531_ = new AbstractSchoolingFish.SchoolSpawnGroupData(this);
        } else {
            this.startFollowing(((AbstractSchoolingFish.SchoolSpawnGroupData)p_27531_).leader);
        }

        return p_27531_;
    }

    public static class SchoolSpawnGroupData implements SpawnGroupData {
        public final AbstractSchoolingFish leader;

        public SchoolSpawnGroupData(AbstractSchoolingFish p_27553_) {
            this.leader = p_27553_;
        }
    }
}