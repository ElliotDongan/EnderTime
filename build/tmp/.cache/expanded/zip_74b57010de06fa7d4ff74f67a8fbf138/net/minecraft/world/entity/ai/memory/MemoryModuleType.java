package net.minecraft.world.entity.ai.memory;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class MemoryModuleType<U> {
    public static final MemoryModuleType<Void> DUMMY = register("dummy");
    public static final MemoryModuleType<GlobalPos> HOME = register("home", GlobalPos.CODEC);
    public static final MemoryModuleType<GlobalPos> JOB_SITE = register("job_site", GlobalPos.CODEC);
    public static final MemoryModuleType<GlobalPos> POTENTIAL_JOB_SITE = register("potential_job_site", GlobalPos.CODEC);
    public static final MemoryModuleType<GlobalPos> MEETING_POINT = register("meeting_point", GlobalPos.CODEC);
    public static final MemoryModuleType<List<GlobalPos>> SECONDARY_JOB_SITE = register("secondary_job_site");
    public static final MemoryModuleType<List<LivingEntity>> NEAREST_LIVING_ENTITIES = register("mobs");
    public static final MemoryModuleType<NearestVisibleLivingEntities> NEAREST_VISIBLE_LIVING_ENTITIES = register("visible_mobs");
    public static final MemoryModuleType<List<LivingEntity>> VISIBLE_VILLAGER_BABIES = register("visible_villager_babies");
    public static final MemoryModuleType<List<Player>> NEAREST_PLAYERS = register("nearest_players");
    public static final MemoryModuleType<Player> NEAREST_VISIBLE_PLAYER = register("nearest_visible_player");
    public static final MemoryModuleType<Player> NEAREST_VISIBLE_ATTACKABLE_PLAYER = register("nearest_visible_targetable_player");
    public static final MemoryModuleType<List<Player>> NEAREST_VISIBLE_ATTACKABLE_PLAYERS = register("nearest_visible_targetable_players");
    public static final MemoryModuleType<WalkTarget> WALK_TARGET = register("walk_target");
    public static final MemoryModuleType<PositionTracker> LOOK_TARGET = register("look_target");
    public static final MemoryModuleType<LivingEntity> ATTACK_TARGET = register("attack_target");
    public static final MemoryModuleType<Boolean> ATTACK_COOLING_DOWN = register("attack_cooling_down");
    public static final MemoryModuleType<LivingEntity> INTERACTION_TARGET = register("interaction_target");
    public static final MemoryModuleType<AgeableMob> BREED_TARGET = register("breed_target");
    public static final MemoryModuleType<Entity> RIDE_TARGET = register("ride_target");
    public static final MemoryModuleType<Path> PATH = register("path");
    public static final MemoryModuleType<List<GlobalPos>> INTERACTABLE_DOORS = register("interactable_doors");
    public static final MemoryModuleType<Set<GlobalPos>> DOORS_TO_CLOSE = register("doors_to_close");
    public static final MemoryModuleType<BlockPos> NEAREST_BED = register("nearest_bed");
    public static final MemoryModuleType<DamageSource> HURT_BY = register("hurt_by");
    public static final MemoryModuleType<LivingEntity> HURT_BY_ENTITY = register("hurt_by_entity");
    public static final MemoryModuleType<LivingEntity> AVOID_TARGET = register("avoid_target");
    public static final MemoryModuleType<LivingEntity> NEAREST_HOSTILE = register("nearest_hostile");
    public static final MemoryModuleType<LivingEntity> NEAREST_ATTACKABLE = register("nearest_attackable");
    public static final MemoryModuleType<GlobalPos> HIDING_PLACE = register("hiding_place");
    public static final MemoryModuleType<Long> HEARD_BELL_TIME = register("heard_bell_time");
    public static final MemoryModuleType<Long> CANT_REACH_WALK_TARGET_SINCE = register("cant_reach_walk_target_since");
    public static final MemoryModuleType<Boolean> GOLEM_DETECTED_RECENTLY = register("golem_detected_recently", Codec.BOOL);
    public static final MemoryModuleType<Boolean> DANGER_DETECTED_RECENTLY = register("danger_detected_recently", Codec.BOOL);
    public static final MemoryModuleType<Long> LAST_SLEPT = register("last_slept", Codec.LONG);
    public static final MemoryModuleType<Long> LAST_WOKEN = register("last_woken", Codec.LONG);
    public static final MemoryModuleType<Long> LAST_WORKED_AT_POI = register("last_worked_at_poi", Codec.LONG);
    public static final MemoryModuleType<LivingEntity> NEAREST_VISIBLE_ADULT = register("nearest_visible_adult");
    public static final MemoryModuleType<ItemEntity> NEAREST_VISIBLE_WANTED_ITEM = register("nearest_visible_wanted_item");
    public static final MemoryModuleType<Mob> NEAREST_VISIBLE_NEMESIS = register("nearest_visible_nemesis");
    public static final MemoryModuleType<Integer> PLAY_DEAD_TICKS = register("play_dead_ticks", Codec.INT);
    public static final MemoryModuleType<Player> TEMPTING_PLAYER = register("tempting_player");
    public static final MemoryModuleType<Integer> TEMPTATION_COOLDOWN_TICKS = register("temptation_cooldown_ticks", Codec.INT);
    public static final MemoryModuleType<Integer> GAZE_COOLDOWN_TICKS = register("gaze_cooldown_ticks", Codec.INT);
    public static final MemoryModuleType<Boolean> IS_TEMPTED = register("is_tempted", Codec.BOOL);
    public static final MemoryModuleType<Integer> LONG_JUMP_COOLDOWN_TICKS = register("long_jump_cooling_down", Codec.INT);
    public static final MemoryModuleType<Boolean> LONG_JUMP_MID_JUMP = register("long_jump_mid_jump");
    public static final MemoryModuleType<Boolean> HAS_HUNTING_COOLDOWN = register("has_hunting_cooldown", Codec.BOOL);
    public static final MemoryModuleType<Integer> RAM_COOLDOWN_TICKS = register("ram_cooldown_ticks", Codec.INT);
    public static final MemoryModuleType<Vec3> RAM_TARGET = register("ram_target");
    public static final MemoryModuleType<Unit> IS_IN_WATER = register("is_in_water", Unit.CODEC);
    public static final MemoryModuleType<Unit> IS_PREGNANT = register("is_pregnant", Unit.CODEC);
    public static final MemoryModuleType<Boolean> IS_PANICKING = register("is_panicking", Codec.BOOL);
    public static final MemoryModuleType<List<UUID>> UNREACHABLE_TONGUE_TARGETS = register("unreachable_tongue_targets");
    public static final MemoryModuleType<UUID> ANGRY_AT = register("angry_at", UUIDUtil.CODEC);
    public static final MemoryModuleType<Boolean> UNIVERSAL_ANGER = register("universal_anger", Codec.BOOL);
    public static final MemoryModuleType<Boolean> ADMIRING_ITEM = register("admiring_item", Codec.BOOL);
    public static final MemoryModuleType<Integer> TIME_TRYING_TO_REACH_ADMIRE_ITEM = register("time_trying_to_reach_admire_item");
    public static final MemoryModuleType<Boolean> DISABLE_WALK_TO_ADMIRE_ITEM = register("disable_walk_to_admire_item");
    public static final MemoryModuleType<Boolean> ADMIRING_DISABLED = register("admiring_disabled", Codec.BOOL);
    public static final MemoryModuleType<Boolean> HUNTED_RECENTLY = register("hunted_recently", Codec.BOOL);
    public static final MemoryModuleType<BlockPos> CELEBRATE_LOCATION = register("celebrate_location");
    public static final MemoryModuleType<Boolean> DANCING = register("dancing");
    public static final MemoryModuleType<Hoglin> NEAREST_VISIBLE_HUNTABLE_HOGLIN = register("nearest_visible_huntable_hoglin");
    public static final MemoryModuleType<Hoglin> NEAREST_VISIBLE_BABY_HOGLIN = register("nearest_visible_baby_hoglin");
    public static final MemoryModuleType<Player> NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD = register("nearest_targetable_player_not_wearing_gold");
    public static final MemoryModuleType<List<AbstractPiglin>> NEARBY_ADULT_PIGLINS = register("nearby_adult_piglins");
    public static final MemoryModuleType<List<AbstractPiglin>> NEAREST_VISIBLE_ADULT_PIGLINS = register("nearest_visible_adult_piglins");
    public static final MemoryModuleType<List<Hoglin>> NEAREST_VISIBLE_ADULT_HOGLINS = register("nearest_visible_adult_hoglins");
    public static final MemoryModuleType<AbstractPiglin> NEAREST_VISIBLE_ADULT_PIGLIN = register("nearest_visible_adult_piglin");
    public static final MemoryModuleType<LivingEntity> NEAREST_VISIBLE_ZOMBIFIED = register("nearest_visible_zombified");
    public static final MemoryModuleType<Integer> VISIBLE_ADULT_PIGLIN_COUNT = register("visible_adult_piglin_count");
    public static final MemoryModuleType<Integer> VISIBLE_ADULT_HOGLIN_COUNT = register("visible_adult_hoglin_count");
    public static final MemoryModuleType<Player> NEAREST_PLAYER_HOLDING_WANTED_ITEM = register("nearest_player_holding_wanted_item");
    public static final MemoryModuleType<Boolean> ATE_RECENTLY = register("ate_recently");
    public static final MemoryModuleType<BlockPos> NEAREST_REPELLENT = register("nearest_repellent");
    public static final MemoryModuleType<Boolean> PACIFIED = register("pacified");
    public static final MemoryModuleType<LivingEntity> ROAR_TARGET = register("roar_target");
    public static final MemoryModuleType<BlockPos> DISTURBANCE_LOCATION = register("disturbance_location");
    public static final MemoryModuleType<Unit> RECENT_PROJECTILE = register("recent_projectile", Unit.CODEC);
    public static final MemoryModuleType<Unit> IS_SNIFFING = register("is_sniffing", Unit.CODEC);
    public static final MemoryModuleType<Unit> IS_EMERGING = register("is_emerging", Unit.CODEC);
    public static final MemoryModuleType<Unit> ROAR_SOUND_DELAY = register("roar_sound_delay", Unit.CODEC);
    public static final MemoryModuleType<Unit> DIG_COOLDOWN = register("dig_cooldown", Unit.CODEC);
    public static final MemoryModuleType<Unit> ROAR_SOUND_COOLDOWN = register("roar_sound_cooldown", Unit.CODEC);
    public static final MemoryModuleType<Unit> SNIFF_COOLDOWN = register("sniff_cooldown", Unit.CODEC);
    public static final MemoryModuleType<Unit> TOUCH_COOLDOWN = register("touch_cooldown", Unit.CODEC);
    public static final MemoryModuleType<Unit> VIBRATION_COOLDOWN = register("vibration_cooldown", Unit.CODEC);
    public static final MemoryModuleType<Unit> SONIC_BOOM_COOLDOWN = register("sonic_boom_cooldown", Unit.CODEC);
    public static final MemoryModuleType<Unit> SONIC_BOOM_SOUND_COOLDOWN = register("sonic_boom_sound_cooldown", Unit.CODEC);
    public static final MemoryModuleType<Unit> SONIC_BOOM_SOUND_DELAY = register("sonic_boom_sound_delay", Unit.CODEC);
    public static final MemoryModuleType<UUID> LIKED_PLAYER = register("liked_player", UUIDUtil.CODEC);
    public static final MemoryModuleType<GlobalPos> LIKED_NOTEBLOCK_POSITION = register("liked_noteblock", GlobalPos.CODEC);
    public static final MemoryModuleType<Integer> LIKED_NOTEBLOCK_COOLDOWN_TICKS = register("liked_noteblock_cooldown_ticks", Codec.INT);
    public static final MemoryModuleType<Integer> ITEM_PICKUP_COOLDOWN_TICKS = register("item_pickup_cooldown_ticks", Codec.INT);
    public static final MemoryModuleType<List<GlobalPos>> SNIFFER_EXPLORED_POSITIONS = register("sniffer_explored_positions", Codec.list(GlobalPos.CODEC));
    public static final MemoryModuleType<BlockPos> SNIFFER_SNIFFING_TARGET = register("sniffer_sniffing_target");
    public static final MemoryModuleType<Boolean> SNIFFER_DIGGING = register("sniffer_digging");
    public static final MemoryModuleType<Boolean> SNIFFER_HAPPY = register("sniffer_happy");
    public static final MemoryModuleType<Unit> BREEZE_JUMP_COOLDOWN = register("breeze_jump_cooldown", Unit.CODEC);
    public static final MemoryModuleType<Unit> BREEZE_SHOOT = register("breeze_shoot", Unit.CODEC);
    public static final MemoryModuleType<Unit> BREEZE_SHOOT_CHARGING = register("breeze_shoot_charging", Unit.CODEC);
    public static final MemoryModuleType<Unit> BREEZE_SHOOT_RECOVERING = register("breeze_shoot_recover", Unit.CODEC);
    public static final MemoryModuleType<Unit> BREEZE_SHOOT_COOLDOWN = register("breeze_shoot_cooldown", Unit.CODEC);
    public static final MemoryModuleType<Unit> BREEZE_JUMP_INHALING = register("breeze_jump_inhaling", Unit.CODEC);
    public static final MemoryModuleType<BlockPos> BREEZE_JUMP_TARGET = register("breeze_jump_target", BlockPos.CODEC);
    public static final MemoryModuleType<Unit> BREEZE_LEAVING_WATER = register("breeze_leaving_water", Unit.CODEC);
    private final Optional<Codec<ExpirableValue<U>>> codec;

    @VisibleForTesting
    public MemoryModuleType(Optional<Codec<U>> p_26386_) {
        this.codec = p_26386_.map(ExpirableValue::codec);
    }

    @Override
    public String toString() {
        return BuiltInRegistries.MEMORY_MODULE_TYPE.getKey(this).toString();
    }

    public Optional<Codec<ExpirableValue<U>>> getCodec() {
        return this.codec;
    }

    private static <U> MemoryModuleType<U> register(String p_26391_, Codec<U> p_26392_) {
        return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, ResourceLocation.withDefaultNamespace(p_26391_), new MemoryModuleType<>(Optional.of(p_26392_)));
    }

    private static <U> MemoryModuleType<U> register(String p_26389_) {
        return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, ResourceLocation.withDefaultNamespace(p_26389_), new MemoryModuleType<>(Optional.empty()));
    }
}