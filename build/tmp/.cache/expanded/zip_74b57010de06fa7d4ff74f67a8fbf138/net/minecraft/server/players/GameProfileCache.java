package net.minecraft.server.players;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringUtil;
import org.slf4j.Logger;

public class GameProfileCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int GAMEPROFILES_MRU_LIMIT = 1000;
    private static final int GAMEPROFILES_EXPIRATION_MONTHS = 1;
    private static boolean usesAuthentication;
    private final Map<String, GameProfileCache.GameProfileInfo> profilesByName = Maps.newConcurrentMap();
    private final Map<UUID, GameProfileCache.GameProfileInfo> profilesByUUID = Maps.newConcurrentMap();
    private final Map<String, CompletableFuture<Optional<GameProfile>>> requests = Maps.newConcurrentMap();
    private final GameProfileRepository profileRepository;
    private final Gson gson = new GsonBuilder().create();
    private final File file;
    private final AtomicLong operationCount = new AtomicLong();
    @Nullable
    private Executor executor;

    public GameProfileCache(GameProfileRepository p_10974_, File p_10975_) {
        this.profileRepository = p_10974_;
        this.file = p_10975_;
        Lists.reverse(this.load()).forEach(this::safeAdd);
    }

    private void safeAdd(GameProfileCache.GameProfileInfo p_10980_) {
        GameProfile gameprofile = p_10980_.getProfile();
        p_10980_.setLastAccess(this.getNextOperation());
        this.profilesByName.put(gameprofile.getName().toLowerCase(Locale.ROOT), p_10980_);
        this.profilesByUUID.put(gameprofile.getId(), p_10980_);
    }

    private static Optional<GameProfile> lookupGameProfile(GameProfileRepository p_10994_, String p_10995_) {
        if (!StringUtil.isValidPlayerName(p_10995_)) {
            return createUnknownProfile(p_10995_);
        } else {
            Optional<GameProfile> optional = p_10994_.findProfileByName(p_10995_);
            return optional.isEmpty() ? createUnknownProfile(p_10995_) : optional;
        }
    }

    private static Optional<GameProfile> createUnknownProfile(String p_311687_) {
        return usesAuthentication() ? Optional.empty() : Optional.of(UUIDUtil.createOfflineProfile(p_311687_));
    }

    public static void setUsesAuthentication(boolean p_11005_) {
        usesAuthentication = p_11005_;
    }

    private static boolean usesAuthentication() {
        return usesAuthentication;
    }

    public void add(GameProfile p_10992_) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(2, 1);
        Date date = calendar.getTime();
        GameProfileCache.GameProfileInfo gameprofilecache$gameprofileinfo = new GameProfileCache.GameProfileInfo(p_10992_, date);
        this.safeAdd(gameprofilecache$gameprofileinfo);
        this.save();
    }

    private long getNextOperation() {
        return this.operationCount.incrementAndGet();
    }

    public Optional<GameProfile> get(String p_10997_) {
        String s = p_10997_.toLowerCase(Locale.ROOT);
        GameProfileCache.GameProfileInfo gameprofilecache$gameprofileinfo = this.profilesByName.get(s);
        boolean flag = false;
        if (gameprofilecache$gameprofileinfo != null && new Date().getTime() >= gameprofilecache$gameprofileinfo.expirationDate.getTime()) {
            this.profilesByUUID.remove(gameprofilecache$gameprofileinfo.getProfile().getId());
            this.profilesByName.remove(gameprofilecache$gameprofileinfo.getProfile().getName().toLowerCase(Locale.ROOT));
            flag = true;
            gameprofilecache$gameprofileinfo = null;
        }

        Optional<GameProfile> optional;
        if (gameprofilecache$gameprofileinfo != null) {
            gameprofilecache$gameprofileinfo.setLastAccess(this.getNextOperation());
            optional = Optional.of(gameprofilecache$gameprofileinfo.getProfile());
        } else {
            optional = lookupGameProfile(this.profileRepository, s);
            if (optional.isPresent()) {
                this.add(optional.get());
                flag = false;
            }
        }

        if (flag) {
            this.save();
        }

        return optional;
    }

    public CompletableFuture<Optional<GameProfile>> getAsync(String p_143968_) {
        if (this.executor == null) {
            throw new IllegalStateException("No executor");
        } else {
            CompletableFuture<Optional<GameProfile>> completablefuture = this.requests.get(p_143968_);
            if (completablefuture != null) {
                return completablefuture;
            } else {
                CompletableFuture<Optional<GameProfile>> completablefuture1 = CompletableFuture.<Optional<GameProfile>>supplyAsync(
                        () -> this.get(p_143968_), Util.backgroundExecutor().forName("getProfile")
                    )
                    .whenCompleteAsync((p_143965_, p_143966_) -> this.requests.remove(p_143968_), this.executor);
                this.requests.put(p_143968_, completablefuture1);
                return completablefuture1;
            }
        }
    }

    public Optional<GameProfile> get(UUID p_11003_) {
        GameProfileCache.GameProfileInfo gameprofilecache$gameprofileinfo = this.profilesByUUID.get(p_11003_);
        if (gameprofilecache$gameprofileinfo == null) {
            return Optional.empty();
        } else {
            gameprofilecache$gameprofileinfo.setLastAccess(this.getNextOperation());
            return Optional.of(gameprofilecache$gameprofileinfo.getProfile());
        }
    }

    public void setExecutor(Executor p_143975_) {
        this.executor = p_143975_;
    }

    public void clearExecutor() {
        this.executor = null;
    }

    private static DateFormat createDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
    }

    public List<GameProfileCache.GameProfileInfo> load() {
        List<GameProfileCache.GameProfileInfo> list = Lists.newArrayList();

        try {
            Object object;
            try (Reader reader = Files.newReader(this.file, StandardCharsets.UTF_8)) {
                JsonArray jsonarray = this.gson.fromJson(reader, JsonArray.class);
                if (jsonarray != null) {
                    DateFormat dateformat = createDateFormat();
                    jsonarray.forEach(p_143973_ -> readGameProfile(p_143973_, dateformat).ifPresent(list::add));
                    return list;
                }

                object = list;
            }

            return (List<GameProfileCache.GameProfileInfo>)object;
        } catch (FileNotFoundException filenotfoundexception) {
        } catch (JsonParseException | IOException ioexception) {
            LOGGER.warn("Failed to load profile cache {}", this.file, ioexception);
        }

        return list;
    }

    public void save() {
        JsonArray jsonarray = new JsonArray();
        DateFormat dateformat = createDateFormat();
        this.getTopMRUProfiles(1000).forEach(p_143962_ -> jsonarray.add(writeGameProfile(p_143962_, dateformat)));
        String s = this.gson.toJson((JsonElement)jsonarray);

        try (Writer writer = Files.newWriter(this.file, StandardCharsets.UTF_8)) {
            writer.write(s);
        } catch (IOException ioexception) {
        }
    }

    private Stream<GameProfileCache.GameProfileInfo> getTopMRUProfiles(int p_10978_) {
        return ImmutableList.copyOf(this.profilesByUUID.values())
            .stream()
            .sorted(Comparator.comparing(GameProfileCache.GameProfileInfo::getLastAccess).reversed())
            .limit(p_10978_);
    }

    private static JsonElement writeGameProfile(GameProfileCache.GameProfileInfo p_10982_, DateFormat p_10983_) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("name", p_10982_.getProfile().getName());
        jsonobject.addProperty("uuid", p_10982_.getProfile().getId().toString());
        jsonobject.addProperty("expiresOn", p_10983_.format(p_10982_.getExpirationDate()));
        return jsonobject;
    }

    private static Optional<GameProfileCache.GameProfileInfo> readGameProfile(JsonElement p_10989_, DateFormat p_10990_) {
        if (p_10989_.isJsonObject()) {
            JsonObject jsonobject = p_10989_.getAsJsonObject();
            JsonElement jsonelement = jsonobject.get("name");
            JsonElement jsonelement1 = jsonobject.get("uuid");
            JsonElement jsonelement2 = jsonobject.get("expiresOn");
            if (jsonelement != null && jsonelement1 != null) {
                String s = jsonelement1.getAsString();
                String s1 = jsonelement.getAsString();
                Date date = null;
                if (jsonelement2 != null) {
                    try {
                        date = p_10990_.parse(jsonelement2.getAsString());
                    } catch (ParseException parseexception) {
                    }
                }

                if (s1 != null && s != null && date != null) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(s);
                    } catch (Throwable throwable) {
                        return Optional.empty();
                    }

                    return Optional.of(new GameProfileCache.GameProfileInfo(new GameProfile(uuid, s1), date));
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    static class GameProfileInfo {
        private final GameProfile profile;
        final Date expirationDate;
        private volatile long lastAccess;

        GameProfileInfo(GameProfile p_11022_, Date p_11023_) {
            this.profile = p_11022_;
            this.expirationDate = p_11023_;
        }

        public GameProfile getProfile() {
            return this.profile;
        }

        public Date getExpirationDate() {
            return this.expirationDate;
        }

        public void setLastAccess(long p_11030_) {
            this.lastAccess = p_11030_;
        }

        public long getLastAccess() {
            return this.lastAccess;
        }
    }
}