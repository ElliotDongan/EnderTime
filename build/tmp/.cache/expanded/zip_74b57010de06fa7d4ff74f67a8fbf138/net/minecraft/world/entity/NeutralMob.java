package net.minecraft.world.entity;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface NeutralMob {
    String TAG_ANGER_TIME = "AngerTime";
    String TAG_ANGRY_AT = "AngryAt";

    int getRemainingPersistentAngerTime();

    void setRemainingPersistentAngerTime(int p_21673_);

    @Nullable
    UUID getPersistentAngerTarget();

    void setPersistentAngerTarget(@Nullable UUID p_21672_);

    void startPersistentAngerTimer();

    default void addPersistentAngerSaveData(ValueOutput p_407892_) {
        p_407892_.putInt("AngerTime", this.getRemainingPersistentAngerTime());
        p_407892_.storeNullable("AngryAt", UUIDUtil.CODEC, this.getPersistentAngerTarget());
    }

    default void readPersistentAngerSaveData(Level p_147286_, ValueInput p_410075_) {
        this.setRemainingPersistentAngerTime(p_410075_.getIntOr("AngerTime", 0));
        if (p_147286_ instanceof ServerLevel serverlevel) {
            UUID $$4 = p_410075_.read("AngryAt", UUIDUtil.CODEC).orElse(null);
            this.setPersistentAngerTarget($$4);
            if (($$4 != null ? serverlevel.getEntity($$4) : null) instanceof LivingEntity livingentity) {
                this.setTarget(livingentity);
            }
        }
    }

    default void updatePersistentAnger(ServerLevel p_21667_, boolean p_21668_) {
        LivingEntity livingentity = this.getTarget();
        UUID uuid = this.getPersistentAngerTarget();
        if ((livingentity == null || livingentity.isDeadOrDying()) && uuid != null && p_21667_.getEntity(uuid) instanceof Mob) {
            this.stopBeingAngry();
        } else {
            if (livingentity != null && !Objects.equals(uuid, livingentity.getUUID())) {
                this.setPersistentAngerTarget(livingentity.getUUID());
                this.startPersistentAngerTimer();
            }

            if (this.getRemainingPersistentAngerTime() > 0 && (livingentity == null || livingentity.getType() != EntityType.PLAYER || !p_21668_)) {
                this.setRemainingPersistentAngerTime(this.getRemainingPersistentAngerTime() - 1);
                if (this.getRemainingPersistentAngerTime() == 0) {
                    this.stopBeingAngry();
                }
            }
        }
    }

    default boolean isAngryAt(LivingEntity p_21675_, ServerLevel p_366229_) {
        if (!this.canAttack(p_21675_)) {
            return false;
        } else {
            return p_21675_.getType() == EntityType.PLAYER && this.isAngryAtAllPlayers(p_366229_) ? true : p_21675_.getUUID().equals(this.getPersistentAngerTarget());
        }
    }

    default boolean isAngryAtAllPlayers(ServerLevel p_362225_) {
        return p_362225_.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER) && this.isAngry() && this.getPersistentAngerTarget() == null;
    }

    default boolean isAngry() {
        return this.getRemainingPersistentAngerTime() > 0;
    }

    default void playerDied(ServerLevel p_360871_, Player p_21677_) {
        if (p_360871_.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            if (p_21677_.getUUID().equals(this.getPersistentAngerTarget())) {
                this.stopBeingAngry();
            }
        }
    }

    default void forgetCurrentTargetAndRefreshUniversalAnger() {
        this.stopBeingAngry();
        this.startPersistentAngerTimer();
    }

    default void stopBeingAngry() {
        this.setLastHurtByMob(null);
        this.setPersistentAngerTarget(null);
        this.setTarget(null);
        this.setRemainingPersistentAngerTime(0);
    }

    @Nullable
    LivingEntity getLastHurtByMob();

    void setLastHurtByMob(@Nullable LivingEntity p_21669_);

    void setTarget(@Nullable LivingEntity p_21681_);

    boolean canAttack(LivingEntity p_181126_);

    @Nullable
    LivingEntity getTarget();
}