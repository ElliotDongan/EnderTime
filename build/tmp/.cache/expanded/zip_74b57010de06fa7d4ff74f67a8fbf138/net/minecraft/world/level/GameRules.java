package net.minecraft.world.level;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicLike;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.slf4j.Logger;

public class GameRules {
    public static final int DEFAULT_RANDOM_TICK_SPEED = 3;
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<GameRules.Key<?>, GameRules.Type<?>> GAME_RULE_TYPES = Maps.newTreeMap(Comparator.comparing(p_46218_ -> p_46218_.id));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOFIRETICK = register(
        "doFireTick", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_ALLOWFIRETICKAWAYFROMPLAYERS = register(
        "allowFireTicksAwayFromPlayer", GameRules.Category.UPDATES, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_MOBGRIEFING = register("mobGriefing", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_KEEPINVENTORY = register(
        "keepInventory", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOMOBSPAWNING = register(
        "doMobSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOMOBLOOT = register("doMobLoot", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_PROJECTILESCANBREAKBLOCKS = register(
        "projectilesCanBreakBlocks", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOBLOCKDROPS = register(
        "doTileDrops", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOENTITYDROPS = register(
        "doEntityDrops", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_COMMANDBLOCKOUTPUT = register(
        "commandBlockOutput", GameRules.Category.CHAT, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_NATURAL_REGENERATION = register(
        "naturalRegeneration", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DAYLIGHT = register(
        "doDaylightCycle", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_LOGADMINCOMMANDS = register(
        "logAdminCommands", GameRules.Category.CHAT, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SHOWDEATHMESSAGES = register(
        "showDeathMessages", GameRules.Category.CHAT, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_RANDOMTICKING = register(
        "randomTickSpeed", GameRules.Category.UPDATES, GameRules.IntegerValue.create(3)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SENDCOMMANDFEEDBACK = register(
        "sendCommandFeedback", GameRules.Category.CHAT, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_REDUCEDDEBUGINFO = register(
        "reducedDebugInfo", GameRules.Category.MISC, GameRules.BooleanValue.create(false, (p_296932_, p_296933_) -> {
            byte b0 = (byte)(p_296933_.get() ? 22 : 23);

            for (ServerPlayer serverplayer : p_296932_.getPlayerList().getPlayers()) {
                serverplayer.connection.send(new ClientboundEntityEventPacket(serverplayer, b0));
            }
        })
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SPECTATORSGENERATECHUNKS = register(
        "spectatorsGenerateChunks", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_SPAWN_RADIUS = register("spawnRadius", GameRules.Category.PLAYER, GameRules.IntegerValue.create(10));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_PLAYER_MOVEMENT_CHECK = register(
        "disablePlayerMovementCheck", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_ELYTRA_MOVEMENT_CHECK = register(
        "disableElytraMovementCheck", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_ENTITY_CRAMMING = register(
        "maxEntityCramming", GameRules.Category.MOBS, GameRules.IntegerValue.create(24)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_WEATHER_CYCLE = register(
        "doWeatherCycle", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_LIMITED_CRAFTING = register(
        "doLimitedCrafting", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false, (p_296930_, p_296931_) -> {
            for (ServerPlayer serverplayer : p_296930_.getPlayerList().getPlayers()) {
                serverplayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LIMITED_CRAFTING, p_296931_.get() ? 1.0F : 0.0F));
            }
        })
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_COMMAND_CHAIN_LENGTH = register(
        "maxCommandChainLength", GameRules.Category.MISC, GameRules.IntegerValue.create(65536)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_COMMAND_FORK_COUNT = register(
        "maxCommandForkCount", GameRules.Category.MISC, GameRules.IntegerValue.create(65536)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_COMMAND_MODIFICATION_BLOCK_LIMIT = register(
        "commandModificationBlockLimit", GameRules.Category.MISC, GameRules.IntegerValue.create(32768)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_ANNOUNCE_ADVANCEMENTS = register(
        "announceAdvancements", GameRules.Category.CHAT, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_RAIDS = register(
        "disableRaids", GameRules.Category.MOBS, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOINSOMNIA = register(
        "doInsomnia", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_IMMEDIATE_RESPAWN = register(
        "doImmediateRespawn", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false, (p_296928_, p_296929_) -> {
            for (ServerPlayer serverplayer : p_296928_.getPlayerList().getPlayers()) {
                serverplayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, p_296929_.get() ? 1.0F : 0.0F));
            }
        })
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY = register(
        "playersNetherPortalDefaultDelay", GameRules.Category.PLAYER, GameRules.IntegerValue.create(80)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY = register(
        "playersNetherPortalCreativeDelay", GameRules.Category.PLAYER, GameRules.IntegerValue.create(0)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DROWNING_DAMAGE = register(
        "drowningDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FALL_DAMAGE = register(
        "fallDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FIRE_DAMAGE = register(
        "fireDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FREEZE_DAMAGE = register(
        "freezeDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_PATROL_SPAWNING = register(
        "doPatrolSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_TRADER_SPAWNING = register(
        "doTraderSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_WARDEN_SPAWNING = register(
        "doWardenSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FORGIVE_DEAD_PLAYERS = register(
        "forgiveDeadPlayers", GameRules.Category.MOBS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_UNIVERSAL_ANGER = register(
        "universalAnger", GameRules.Category.MOBS, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_PLAYERS_SLEEPING_PERCENTAGE = register(
        "playersSleepingPercentage", GameRules.Category.PLAYER, GameRules.IntegerValue.create(100)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_BLOCK_EXPLOSION_DROP_DECAY = register(
        "blockExplosionDropDecay", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_MOB_EXPLOSION_DROP_DECAY = register(
        "mobExplosionDropDecay", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_TNT_EXPLOSION_DROP_DECAY = register(
        "tntExplosionDropDecay", GameRules.Category.DROPS, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_SNOW_ACCUMULATION_HEIGHT = register(
        "snowAccumulationHeight", GameRules.Category.UPDATES, GameRules.IntegerValue.create(1)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_WATER_SOURCE_CONVERSION = register(
        "waterSourceConversion", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_LAVA_SOURCE_CONVERSION = register(
        "lavaSourceConversion", GameRules.Category.UPDATES, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_GLOBAL_SOUND_EVENTS = register(
        "globalSoundEvents", GameRules.Category.MISC, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_VINES_SPREAD = register(
        "doVinesSpread", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_ENDER_PEARLS_VANISH_ON_DEATH = register(
        "enderPearlsVanishOnDeath", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_MINECART_MAX_SPEED = register(
        "minecartMaxSpeed",
        GameRules.Category.MISC,
        GameRules.IntegerValue.create(8, 1, 1000, FeatureFlagSet.of(FeatureFlags.MINECART_IMPROVEMENTS), (p_359952_, p_359953_) -> {})
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_SPAWN_CHUNK_RADIUS = register(
        "spawnChunkRadius", GameRules.Category.MISC, GameRules.IntegerValue.create(2, 0, 32, FeatureFlagSet.of(), (p_405670_, p_405671_) -> {
            ServerLevel serverlevel = p_405670_.overworld();
            serverlevel.setDefaultSpawnPos(serverlevel.getSharedSpawnPos(), serverlevel.getSharedSpawnAngle());
        })
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_TNT_EXPLODES = register(
        "tntExplodes", GameRules.Category.MISC, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_LOCATOR_BAR = register(
        "locatorBar", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true, (p_405668_, p_405669_) -> p_405668_.getAllLevels().forEach(p_405667_ -> {
            ServerWaypointManager serverwaypointmanager = p_405667_.getWaypointManager();
            if (p_405669_.get()) {
                p_405667_.players().forEach(serverwaypointmanager::updatePlayer);
            } else {
                serverwaypointmanager.breakAllConnections();
            }
        }))
    );
    private final Map<GameRules.Key<?>, GameRules.Value<?>> rules;
    private final FeatureFlagSet enabledFeatures;

    public static <T extends GameRules.Value<T>> GameRules.Type<T> getType(GameRules.Key<T> p_395151_) {
        return (GameRules.Type<T>)GAME_RULE_TYPES.get(p_395151_);
    }

    public static <T extends GameRules.Value<T>> Codec<GameRules.Key<T>> keyCodec(Class<T> p_393360_) {
        return Codec.STRING
            .comapFlatMap(
                p_390884_ -> GAME_RULE_TYPES.entrySet()
                    .stream()
                    .filter(p_390879_ -> p_390879_.getValue().valueClass == p_393360_)
                    .map(Entry::getKey)
                    .filter(p_390882_ -> p_390882_.getId().equals(p_390884_))
                    .map(p_390877_ -> (GameRules.Key<T>)p_390877_)
                    .findFirst()
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Invalid game rule ID for type: " + p_390884_)),
                GameRules.Key::getId
            );
    }

    public static <T extends GameRules.Value<T>> GameRules.Key<T> register(String p_46190_, GameRules.Category p_46191_, GameRules.Type<T> p_46192_) {
        GameRules.Key<T> key = new GameRules.Key<>(p_46190_, p_46191_);
        GameRules.Type<?> type = GAME_RULE_TYPES.put(key, p_46192_);
        if (type != null) {
            throw new IllegalStateException("Duplicate game rule registration for " + p_46190_);
        } else {
            return key;
        }
    }

    public GameRules(FeatureFlagSet p_363788_, DynamicLike<?> p_369354_) {
        this(p_363788_);
        this.loadFromTag(p_369354_);
    }

    public GameRules(FeatureFlagSet p_370215_) {
        this(availableRules(p_370215_).collect(ImmutableMap.toImmutableMap(Entry::getKey, p_46210_ -> p_46210_.getValue().createRule())), p_370215_);
    }

    private static Stream<Entry<GameRules.Key<?>, GameRules.Type<?>>> availableRules(FeatureFlagSet p_367533_) {
        return GAME_RULE_TYPES.entrySet().stream().filter(p_359947_ -> p_359947_.getValue().requiredFeatures.isSubsetOf(p_367533_));
    }

    private GameRules(Map<GameRules.Key<?>, GameRules.Value<?>> p_369817_, FeatureFlagSet p_361937_) {
        this.rules = p_369817_;
        this.enabledFeatures = p_361937_;
    }

    public <T extends GameRules.Value<T>> T getRule(GameRules.Key<T> p_46171_) {
        T t = (T)this.rules.get(p_46171_);
        if (t == null) {
            throw new IllegalArgumentException("Tried to access invalid game rule");
        } else {
            return t;
        }
    }

    public CompoundTag createTag() {
        CompoundTag compoundtag = new CompoundTag();
        this.rules.forEach((p_46197_, p_46198_) -> compoundtag.putString(p_46197_.id, p_46198_.serialize()));
        return compoundtag;
    }

    private void loadFromTag(DynamicLike<?> p_46184_) {
        this.rules.forEach((p_327232_, p_327233_) -> p_46184_.get(p_327232_.id).asString().ifSuccess(p_327233_::deserialize));
    }

    public GameRules copy(FeatureFlagSet p_366179_) {
        return new GameRules(
            availableRules(p_366179_)
                .collect(
                    ImmutableMap.toImmutableMap(
                        Entry::getKey,
                        p_405665_ -> this.rules.containsKey(p_405665_.getKey())
                            ? this.rules.get(p_405665_.getKey()).copy()
                            : p_405665_.getValue().createRule()
                    )
                ),
            p_366179_
        );
    }

    public void visitGameRuleTypes(GameRules.GameRuleTypeVisitor p_46165_) {
        GAME_RULE_TYPES.forEach((p_359949_, p_359950_) -> this.callVisitorCap(p_46165_, (GameRules.Key<?>)p_359949_, (GameRules.Type<?>)p_359950_));
    }

    private <T extends GameRules.Value<T>> void callVisitorCap(GameRules.GameRuleTypeVisitor p_46167_, GameRules.Key<?> p_46168_, GameRules.Type<?> p_46169_) {
        if (p_46169_.requiredFeatures.isSubsetOf(this.enabledFeatures)) {
            p_46167_.visit((Key)p_46168_, p_46169_);
            p_46169_.callVisitor(p_46167_, (Key)p_46168_);
        }
    }

    public void assignFrom(GameRules p_46177_, @Nullable MinecraftServer p_46178_) {
        p_46177_.rules.keySet().forEach(p_46182_ -> this.assignCap((GameRules.Key<?>)p_46182_, p_46177_, p_46178_));
    }

    private <T extends GameRules.Value<T>> void assignCap(GameRules.Key<T> p_46173_, GameRules p_46174_, @Nullable MinecraftServer p_46175_) {
        T t = p_46174_.getRule(p_46173_);
        this.<T>getRule(p_46173_).setFrom(t, p_46175_);
    }

    public boolean getBoolean(GameRules.Key<GameRules.BooleanValue> p_46208_) {
        return this.getRule(p_46208_).get();
    }

    public int getInt(GameRules.Key<GameRules.IntegerValue> p_46216_) {
        return this.getRule(p_46216_).get();
    }

    public static class BooleanValue extends GameRules.Value<GameRules.BooleanValue> {
        private boolean value;

        private static GameRules.Type<GameRules.BooleanValue> create(
            boolean p_408062_, BiConsumer<MinecraftServer, GameRules.BooleanValue> p_409988_, FeatureFlagSet p_406933_
        ) {
            return new GameRules.Type<>(
                BoolArgumentType::bool,
                p_46242_ -> new GameRules.BooleanValue(p_46242_, p_408062_),
                p_409988_,
                GameRules.GameRuleTypeVisitor::visitBoolean,
                GameRules.BooleanValue.class,
                p_406933_
            );
        }

        public static GameRules.Type<GameRules.BooleanValue> create(boolean p_46253_, BiConsumer<MinecraftServer, GameRules.BooleanValue> p_46254_) {
            return new GameRules.Type<>(
                BoolArgumentType::bool,
                p_405673_ -> new GameRules.BooleanValue(p_405673_, p_46253_),
                p_46254_,
                GameRules.GameRuleTypeVisitor::visitBoolean,
                GameRules.BooleanValue.class,
                FeatureFlagSet.of()
            );
        }

        public static GameRules.Type<GameRules.BooleanValue> create(boolean p_46251_) {
            return create(p_46251_, (p_46236_, p_46237_) -> {});
        }

        public BooleanValue(GameRules.Type<GameRules.BooleanValue> p_46221_, boolean p_46222_) {
            super(p_46221_);
            this.value = p_46222_;
        }

        @Override
        protected void updateFromArgument(CommandContext<CommandSourceStack> p_46231_, String p_46232_) {
            this.value = BoolArgumentType.getBool(p_46231_, p_46232_);
        }

        public boolean get() {
            return this.value;
        }

        public void set(boolean p_46247_, @Nullable MinecraftServer p_46248_) {
            this.value = p_46247_;
            this.onChanged(p_46248_);
        }

        @Override
        public String serialize() {
            return Boolean.toString(this.value);
        }

        @Override
        protected void deserialize(String p_46234_) {
            this.value = Boolean.parseBoolean(p_46234_);
        }

        @Override
        public int getCommandResult() {
            return this.value ? 1 : 0;
        }

        protected GameRules.BooleanValue getSelf() {
            return this;
        }

        protected GameRules.BooleanValue copy() {
            return new GameRules.BooleanValue(this.type, this.value);
        }

        public void setFrom(GameRules.BooleanValue p_46225_, @Nullable MinecraftServer p_46226_) {
            this.value = p_46225_.value;
            this.onChanged(p_46226_);
        }
    }

    public static enum Category {
        PLAYER("gamerule.category.player"),
        MOBS("gamerule.category.mobs"),
        SPAWNING("gamerule.category.spawning"),
        DROPS("gamerule.category.drops"),
        UPDATES("gamerule.category.updates"),
        CHAT("gamerule.category.chat"),
        MISC("gamerule.category.misc");

        private final String descriptionId;

        private Category(final String p_46273_) {
            this.descriptionId = p_46273_;
        }

        public String getDescriptionId() {
            return this.descriptionId;
        }
    }

    public interface GameRuleTypeVisitor {
        default <T extends GameRules.Value<T>> void visit(GameRules.Key<T> p_46278_, GameRules.Type<T> p_46279_) {
        }

        default void visitBoolean(GameRules.Key<GameRules.BooleanValue> p_46280_, GameRules.Type<GameRules.BooleanValue> p_46281_) {
        }

        default void visitInteger(GameRules.Key<GameRules.IntegerValue> p_46282_, GameRules.Type<GameRules.IntegerValue> p_46283_) {
        }
    }

    public static class IntegerValue extends GameRules.Value<GameRules.IntegerValue> {
        private int value;

        public static GameRules.Type<GameRules.IntegerValue> create(int p_46295_, BiConsumer<MinecraftServer, GameRules.IntegerValue> p_46296_) {
            return new GameRules.Type<>(
                IntegerArgumentType::integer,
                p_46293_ -> new GameRules.IntegerValue(p_46293_, p_46295_),
                p_46296_,
                GameRules.GameRuleTypeVisitor::visitInteger,
                GameRules.IntegerValue.class,
                FeatureFlagSet.of()
            );
        }

        static GameRules.Type<GameRules.IntegerValue> create(
            int p_332409_, int p_333284_, int p_329881_, FeatureFlagSet p_364744_, BiConsumer<MinecraftServer, GameRules.IntegerValue> p_334400_
        ) {
            return new GameRules.Type<>(
                () -> IntegerArgumentType.integer(p_333284_, p_329881_),
                p_327235_ -> new GameRules.IntegerValue(p_327235_, p_332409_),
                p_334400_,
                GameRules.GameRuleTypeVisitor::visitInteger,
                GameRules.IntegerValue.class,
                p_364744_
            );
        }

        public static GameRules.Type<GameRules.IntegerValue> create(int p_46313_) {
            return create(p_46313_, (p_46309_, p_46310_) -> {});
        }

        public IntegerValue(GameRules.Type<GameRules.IntegerValue> p_46286_, int p_46287_) {
            super(p_46286_);
            this.value = p_46287_;
        }

        @Override
        protected void updateFromArgument(CommandContext<CommandSourceStack> p_46304_, String p_46305_) {
            this.value = IntegerArgumentType.getInteger(p_46304_, p_46305_);
        }

        public int get() {
            return this.value;
        }

        public void set(int p_151490_, @Nullable MinecraftServer p_151491_) {
            this.value = p_151490_;
            this.onChanged(p_151491_);
        }

        @Override
        public String serialize() {
            return Integer.toString(this.value);
        }

        @Override
        protected void deserialize(String p_46307_) {
            this.value = safeParse(p_46307_);
        }

        public boolean tryDeserialize(String p_46315_) {
            try {
                StringReader stringreader = new StringReader(p_46315_);
                this.value = (Integer)this.type.argument.get().parse(stringreader);
                return !stringreader.canRead();
            } catch (CommandSyntaxException commandsyntaxexception) {
                return false;
            }
        }

        private static int safeParse(String p_46318_) {
            if (!p_46318_.isEmpty()) {
                try {
                    return Integer.parseInt(p_46318_);
                } catch (NumberFormatException numberformatexception) {
                    GameRules.LOGGER.warn("Failed to parse integer {}", p_46318_);
                }
            }

            return 0;
        }

        @Override
        public int getCommandResult() {
            return this.value;
        }

        protected GameRules.IntegerValue getSelf() {
            return this;
        }

        protected GameRules.IntegerValue copy() {
            return new GameRules.IntegerValue(this.type, this.value);
        }

        public void setFrom(GameRules.IntegerValue p_46298_, @Nullable MinecraftServer p_46299_) {
            this.value = p_46298_.value;
            this.onChanged(p_46299_);
        }
    }

    public static final class Key<T extends GameRules.Value<T>> {
        final String id;
        private final GameRules.Category category;

        public Key(String p_46326_, GameRules.Category p_46327_) {
            this.id = p_46326_;
            this.category = p_46327_;
        }

        @Override
        public String toString() {
            return this.id;
        }

        @Override
        public boolean equals(Object p_46334_) {
            return this == p_46334_ ? true : p_46334_ instanceof GameRules.Key && ((GameRules.Key)p_46334_).id.equals(this.id);
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        public String getId() {
            return this.id;
        }

        public String getDescriptionId() {
            return "gamerule." + this.id;
        }

        public GameRules.Category getCategory() {
            return this.category;
        }
    }

    public static class Type<T extends GameRules.Value<T>> {
        final Supplier<ArgumentType<?>> argument;
        private final Function<GameRules.Type<T>, T> constructor;
        final BiConsumer<MinecraftServer, T> callback;
        private final GameRules.VisitorCaller<T> visitorCaller;
        final Class<T> valueClass;
        final FeatureFlagSet requiredFeatures;

        Type(
            Supplier<ArgumentType<?>> p_46342_,
            Function<GameRules.Type<T>, T> p_46343_,
            BiConsumer<MinecraftServer, T> p_46344_,
            GameRules.VisitorCaller<T> p_46345_,
            Class<T> p_395574_,
            FeatureFlagSet p_363269_
        ) {
            this.argument = p_46342_;
            this.constructor = p_46343_;
            this.callback = p_46344_;
            this.visitorCaller = p_46345_;
            this.valueClass = p_395574_;
            this.requiredFeatures = p_363269_;
        }

        public RequiredArgumentBuilder<CommandSourceStack, ?> createArgument(String p_46359_) {
            return Commands.argument(p_46359_, this.argument.get());
        }

        public T createRule() {
            return this.constructor.apply(this);
        }

        public void callVisitor(GameRules.GameRuleTypeVisitor p_46354_, GameRules.Key<T> p_46355_) {
            this.visitorCaller.call(p_46354_, p_46355_, this);
        }

        public FeatureFlagSet requiredFeatures() {
            return this.requiredFeatures;
        }
    }

    public abstract static class Value<T extends GameRules.Value<T>> {
        protected final GameRules.Type<T> type;

        public Value(GameRules.Type<T> p_46362_) {
            this.type = p_46362_;
        }

        protected abstract void updateFromArgument(CommandContext<CommandSourceStack> p_46365_, String p_46366_);

        public void setFromArgument(CommandContext<CommandSourceStack> p_46371_, String p_46372_) {
            this.updateFromArgument(p_46371_, p_46372_);
            this.onChanged(p_46371_.getSource().getServer());
        }

        protected void onChanged(@Nullable MinecraftServer p_46369_) {
            if (p_46369_ != null) {
                this.type.callback.accept(p_46369_, this.getSelf());
            }
        }

        protected abstract void deserialize(String p_46367_);

        public abstract String serialize();

        @Override
        public String toString() {
            return this.serialize();
        }

        public abstract int getCommandResult();

        protected abstract T getSelf();

        protected abstract T copy();

        public abstract void setFrom(T p_46363_, @Nullable MinecraftServer p_46364_);
    }

    interface VisitorCaller<T extends GameRules.Value<T>> {
        void call(GameRules.GameRuleTypeVisitor p_46375_, GameRules.Key<T> p_46376_, GameRules.Type<T> p_46377_);
    }
}