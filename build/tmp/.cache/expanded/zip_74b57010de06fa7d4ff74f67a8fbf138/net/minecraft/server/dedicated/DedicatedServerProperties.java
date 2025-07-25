package net.minecraft.server.dedicated;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;

public class DedicatedServerProperties extends Settings<DedicatedServerProperties> {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();
    public final boolean onlineMode = this.get("online-mode", true);
    public final boolean preventProxyConnections = this.get("prevent-proxy-connections", false);
    public final String serverIp = this.get("server-ip", "");
    public final boolean pvp = this.get("pvp", true);
    public final boolean allowFlight = this.get("allow-flight", false);
    public final String motd = this.get("motd", "A Minecraft Server");
    public final String bugReportLink = this.get("bug-report-link", "");
    public final boolean forceGameMode = this.get("force-gamemode", false);
    public final boolean enforceWhitelist = this.get("enforce-whitelist", false);
    public final Difficulty difficulty = this.get(
        "difficulty", dispatchNumberOrString(Difficulty::byId, Difficulty::byName), Difficulty::getKey, Difficulty.EASY
    );
    public final GameType gamemode = this.get("gamemode", dispatchNumberOrString(GameType::byId, GameType::byName), GameType::getName, GameType.SURVIVAL);
    public final String levelName = this.get("level-name", "world");
    public final int serverPort = this.get("server-port", 25565);
    @Nullable
    public final Boolean announcePlayerAchievements = this.getLegacyBoolean("announce-player-achievements");
    public final boolean enableQuery = this.get("enable-query", false);
    public final int queryPort = this.get("query.port", 25565);
    public final boolean enableRcon = this.get("enable-rcon", false);
    public final int rconPort = this.get("rcon.port", 25575);
    public final String rconPassword = this.get("rcon.password", "");
    public final boolean hardcore = this.get("hardcore", false);
    public final boolean allowNether = this.get("allow-nether", true);
    public final boolean spawnMonsters = this.get("spawn-monsters", true);
    public final boolean useNativeTransport = this.get("use-native-transport", true);
    public final boolean enableCommandBlock = this.get("enable-command-block", false);
    public final int spawnProtection = this.get("spawn-protection", 16);
    public final int opPermissionLevel = this.get("op-permission-level", 4);
    public final int functionPermissionLevel = this.get("function-permission-level", 2);
    public final long maxTickTime = this.get("max-tick-time", TimeUnit.MINUTES.toMillis(1L));
    public final int maxChainedNeighborUpdates = this.get("max-chained-neighbor-updates", 1000000);
    public final int rateLimitPacketsPerSecond = this.get("rate-limit", 0);
    public final int viewDistance = this.get("view-distance", 10);
    public final int simulationDistance = this.get("simulation-distance", 10);
    public final int maxPlayers = this.get("max-players", 20);
    public final int networkCompressionThreshold = this.get("network-compression-threshold", 256);
    public final boolean broadcastRconToOps = this.get("broadcast-rcon-to-ops", true);
    public final boolean broadcastConsoleToOps = this.get("broadcast-console-to-ops", true);
    public final int maxWorldSize = this.get("max-world-size", p_139771_ -> Mth.clamp(p_139771_, 1, 29999984), 29999984);
    public final boolean syncChunkWrites = this.get("sync-chunk-writes", true);
    public final String regionFileComression = this.get("region-file-compression", "deflate");
    public final boolean enableJmxMonitoring = this.get("enable-jmx-monitoring", false);
    public final boolean enableStatus = this.get("enable-status", true);
    public final boolean hideOnlinePlayers = this.get("hide-online-players", false);
    public final int entityBroadcastRangePercentage = this.get("entity-broadcast-range-percentage", p_139769_ -> Mth.clamp(p_139769_, 10, 1000), 100);
    public final String textFilteringConfig = this.get("text-filtering-config", "");
    public final int textFilteringVersion = this.get("text-filtering-version", 0);
    public final Optional<MinecraftServer.ServerResourcePackInfo> serverResourcePackInfo;
    public final DataPackConfig initialDataPackConfiguration;
    public final Settings<DedicatedServerProperties>.MutableValue<Integer> playerIdleTimeout = this.getMutable("player-idle-timeout", 0);
    public final Settings<DedicatedServerProperties>.MutableValue<Boolean> whiteList = this.getMutable("white-list", false);
    public final boolean enforceSecureProfile = this.get("enforce-secure-profile", true);
    public final boolean logIPs = this.get("log-ips", true);
    public final int pauseWhenEmptySeconds = this.get("pause-when-empty-seconds", 60);
    private final DedicatedServerProperties.WorldDimensionData worldDimensionData;
    public final WorldOptions worldOptions;
    public boolean acceptsTransfers = this.get("accepts-transfers", false);

    public DedicatedServerProperties(Properties p_180926_) {
        super(p_180926_);
        String s = this.get("level-seed", "");
        boolean flag = this.get("generate-structures", true);
        long i = WorldOptions.parseSeed(s).orElse(WorldOptions.randomSeed());
        this.worldOptions = new WorldOptions(i, flag, false);
        this.worldDimensionData = new DedicatedServerProperties.WorldDimensionData(
            this.get("generator-settings", p_211543_ -> GsonHelper.parse(!p_211543_.isEmpty() ? p_211543_ : "{}"), new JsonObject()),
            this.get("level-type", p_211541_ -> p_211541_.toLowerCase(Locale.ROOT), WorldPresets.NORMAL.location().toString())
        );
        this.serverResourcePackInfo = getServerPackInfo(
            this.get("resource-pack-id", ""),
            this.get("resource-pack", ""),
            this.get("resource-pack-sha1", ""),
            this.getLegacyString("resource-pack-hash"),
            this.get("require-resource-pack", false),
            this.get("resource-pack-prompt", "")
        );
        this.initialDataPackConfiguration = getDatapackConfig(
            this.get("initial-enabled-packs", String.join(",", WorldDataConfiguration.DEFAULT.dataPacks().getEnabled())),
            this.get("initial-disabled-packs", String.join(",", WorldDataConfiguration.DEFAULT.dataPacks().getDisabled()))
        );
    }

    public static DedicatedServerProperties fromFile(Path p_180930_) {
        return new DedicatedServerProperties(loadFromFile(p_180930_));
    }

    protected DedicatedServerProperties reload(RegistryAccess p_139761_, Properties p_139762_) {
        return new DedicatedServerProperties(p_139762_);
    }

    @Nullable
    private static Component parseResourcePackPrompt(String p_214815_) {
        if (!Strings.isNullOrEmpty(p_214815_)) {
            try {
                JsonElement jsonelement = StrictJsonParser.parse(p_214815_);
                return ComponentSerialization.CODEC
                    .parse(RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE), jsonelement)
                    .resultOrPartial(p_405212_ -> LOGGER.warn("Failed to parse resource pack prompt '{}': {}", p_214815_, p_405212_))
                    .orElse(null);
            } catch (Exception exception) {
                LOGGER.warn("Failed to parse resource pack prompt '{}'", p_214815_, exception);
            }
        }

        return null;
    }

    private static Optional<MinecraftServer.ServerResourcePackInfo> getServerPackInfo(
        String p_214809_, String p_214810_, String p_214811_, @Nullable String p_214813_, boolean p_214812_, String p_312092_
    ) {
        if (p_214810_.isEmpty()) {
            return Optional.empty();
        } else {
            String s;
            if (!p_214811_.isEmpty()) {
                s = p_214811_;
                if (!Strings.isNullOrEmpty(p_214813_)) {
                    LOGGER.warn("resource-pack-hash is deprecated and found along side resource-pack-sha1. resource-pack-hash will be ignored.");
                }
            } else if (!Strings.isNullOrEmpty(p_214813_)) {
                LOGGER.warn("resource-pack-hash is deprecated. Please use resource-pack-sha1 instead.");
                s = p_214813_;
            } else {
                s = "";
            }

            if (s.isEmpty()) {
                LOGGER.warn(
                    "You specified a resource pack without providing a sha1 hash. Pack will be updated on the client only if you change the name of the pack."
                );
            } else if (!SHA1.matcher(s).matches()) {
                LOGGER.warn("Invalid sha1 for resource-pack-sha1");
            }

            Component component = parseResourcePackPrompt(p_312092_);
            UUID uuid;
            if (p_214809_.isEmpty()) {
                uuid = UUID.nameUUIDFromBytes(p_214810_.getBytes(StandardCharsets.UTF_8));
                LOGGER.warn("resource-pack-id missing, using default of {}", uuid);
            } else {
                try {
                    uuid = UUID.fromString(p_214809_);
                } catch (IllegalArgumentException illegalargumentexception) {
                    LOGGER.warn("Failed to parse '{}' into UUID", p_214809_);
                    return Optional.empty();
                }
            }

            return Optional.of(new MinecraftServer.ServerResourcePackInfo(uuid, p_214810_, s, p_214812_, component));
        }
    }

    private static DataPackConfig getDatapackConfig(String p_251757_, String p_249979_) {
        List<String> list = COMMA_SPLITTER.splitToList(p_251757_);
        List<String> list1 = COMMA_SPLITTER.splitToList(p_249979_);
        return new DataPackConfig(list, list1);
    }

    public WorldDimensions createDimensions(HolderLookup.Provider p_364968_) {
        return this.worldDimensionData.create(p_364968_);
    }

    record WorldDimensionData(JsonObject generatorSettings, String levelType) {
        private static final Map<String, ResourceKey<WorldPreset>> LEGACY_PRESET_NAMES = Map.of("default", WorldPresets.NORMAL, "largebiomes", WorldPresets.LARGE_BIOMES);

        public WorldDimensions create(HolderLookup.Provider p_363968_) {
            HolderLookup<WorldPreset> holderlookup = p_363968_.lookupOrThrow(Registries.WORLD_PRESET);
            Holder.Reference<WorldPreset> reference = holderlookup.get(WorldPresets.NORMAL)
                .or(() -> holderlookup.listElements().findAny())
                .orElseThrow(() -> new IllegalStateException("Invalid datapack contents: can't find default preset"));
            Holder<WorldPreset> holder = Optional.ofNullable(ResourceLocation.tryParse(this.levelType))
                .map(p_258240_ -> ResourceKey.create(Registries.WORLD_PRESET, p_258240_))
                .or(() -> Optional.ofNullable(LEGACY_PRESET_NAMES.get(this.levelType)))
                .flatMap(holderlookup::get)
                .orElseGet(() -> {
                    DedicatedServerProperties.LOGGER
                        .warn("Failed to parse level-type {}, defaulting to {}", this.levelType, reference.key().location());
                    return reference;
                });
            WorldDimensions worlddimensions = holder.value().createWorldDimensions();
            if (holder.is(WorldPresets.FLAT)) {
                RegistryOps<JsonElement> registryops = p_363968_.createSerializationContext(JsonOps.INSTANCE);
                Optional<FlatLevelGeneratorSettings> optional = FlatLevelGeneratorSettings.CODEC
                    .parse(new Dynamic<>(registryops, this.generatorSettings()))
                    .resultOrPartial(DedicatedServerProperties.LOGGER::error);
                if (optional.isPresent()) {
                    return worlddimensions.replaceOverworldGenerator(p_363968_, new FlatLevelSource(optional.get()));
                }
            }

            return worlddimensions;
        }
    }
}