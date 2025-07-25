package net.minecraft.world.level.block.entity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Services;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SkullBlockEntity extends BlockEntity {
    private static final String TAG_PROFILE = "profile";
    private static final String TAG_NOTE_BLOCK_SOUND = "note_block_sound";
    private static final String TAG_CUSTOM_NAME = "custom_name";
    @Nullable
    private static Executor mainThreadExecutor;
    @Nullable
    private static LoadingCache<String, CompletableFuture<Optional<GameProfile>>> profileCacheByName;
    @Nullable
    private static LoadingCache<UUID, CompletableFuture<Optional<GameProfile>>> profileCacheById;
    public static final Executor CHECKED_MAIN_THREAD_EXECUTOR = p_296964_ -> {
        Executor executor = mainThreadExecutor;
        if (executor != null) {
            executor.execute(p_296964_);
        }
    };
    @Nullable
    private ResolvableProfile owner;
    @Nullable
    private ResourceLocation noteBlockSound;
    private int animationTickCount;
    private boolean isAnimating;
    @Nullable
    private Component customName;

    public SkullBlockEntity(BlockPos p_155731_, BlockState p_155732_) {
        super(BlockEntityType.SKULL, p_155731_, p_155732_);
    }

    public static void setup(final Services p_222886_, Executor p_222887_) {
        mainThreadExecutor = p_222887_;
        final BooleanSupplier booleansupplier = () -> profileCacheById == null;
        profileCacheByName = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10L))
            .maximumSize(256L)
            .build(new CacheLoader<String, CompletableFuture<Optional<GameProfile>>>() {
                public CompletableFuture<Optional<GameProfile>> load(String p_312380_) {
                    return SkullBlockEntity.fetchProfileByName(p_312380_, p_222886_);
                }
            });
        profileCacheById = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10L))
            .maximumSize(256L)
            .build(new CacheLoader<UUID, CompletableFuture<Optional<GameProfile>>>() {
                public CompletableFuture<Optional<GameProfile>> load(UUID p_330530_) {
                    return SkullBlockEntity.fetchProfileById(p_330530_, p_222886_, booleansupplier);
                }
            });
    }

    static CompletableFuture<Optional<GameProfile>> fetchProfileByName(String p_333451_, Services p_332839_) {
        return p_332839_.profileCache()
            .getAsync(p_333451_)
            .thenCompose(
                p_327322_ -> {
                    LoadingCache<UUID, CompletableFuture<Optional<GameProfile>>> loadingcache = profileCacheById;
                    return loadingcache != null && !p_327322_.isEmpty()
                        ? loadingcache.getUnchecked(p_327322_.get().getId()).thenApply(p_327317_ -> p_327317_.or(() -> p_327322_))
                        : CompletableFuture.completedFuture(Optional.empty());
                }
            );
    }

    static CompletableFuture<Optional<GameProfile>> fetchProfileById(UUID p_332548_, Services p_336268_, BooleanSupplier p_335205_) {
        return CompletableFuture.supplyAsync(() -> {
            if (p_335205_.getAsBoolean()) {
                return Optional.empty();
            } else {
                ProfileResult profileresult = p_336268_.sessionService().fetchProfile(p_332548_, true);
                return Optional.ofNullable(profileresult).map(ProfileResult::profile);
            }
        }, Util.backgroundExecutor().forName("fetchProfile"));
    }

    public static void clear() {
        mainThreadExecutor = null;
        profileCacheByName = null;
        profileCacheById = null;
    }

    @Override
    protected void saveAdditional(ValueOutput p_409149_) {
        super.saveAdditional(p_409149_);
        p_409149_.storeNullable("profile", ResolvableProfile.CODEC, this.owner);
        p_409149_.storeNullable("note_block_sound", ResourceLocation.CODEC, this.noteBlockSound);
        p_409149_.storeNullable("custom_name", ComponentSerialization.CODEC, this.customName);
    }

    @Override
    protected void loadAdditional(ValueInput p_409251_) {
        super.loadAdditional(p_409251_);
        this.setOwner(p_409251_.read("profile", ResolvableProfile.CODEC).orElse(null));
        this.noteBlockSound = p_409251_.read("note_block_sound", ResourceLocation.CODEC).orElse(null);
        this.customName = parseCustomNameSafe(p_409251_, "custom_name");
    }

    public static void animation(Level p_261710_, BlockPos p_262153_, BlockState p_262021_, SkullBlockEntity p_261594_) {
        if (p_262021_.hasProperty(SkullBlock.POWERED) && p_262021_.getValue(SkullBlock.POWERED)) {
            p_261594_.isAnimating = true;
            p_261594_.animationTickCount++;
        } else {
            p_261594_.isAnimating = false;
        }
    }

    public float getAnimation(float p_262053_) {
        return this.isAnimating ? this.animationTickCount + p_262053_ : this.animationTickCount;
    }

    @Nullable
    public ResolvableProfile getOwnerProfile() {
        return this.owner;
    }

    @Nullable
    public ResourceLocation getNoteBlockSound() {
        return this.noteBlockSound;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_335540_) {
        return this.saveCustomOnly(p_335540_);
    }

    public void setOwner(@Nullable ResolvableProfile p_328553_) {
        synchronized (this) {
            this.owner = p_328553_;
        }

        this.updateOwnerProfile();
    }

    private void updateOwnerProfile() {
        if (this.owner != null && !this.owner.isResolved()) {
            this.owner.resolve().thenAcceptAsync(p_327314_ -> {
                this.owner = p_327314_;
                this.setChanged();
            }, CHECKED_MAIN_THREAD_EXECUTOR);
        } else {
            this.setChanged();
        }
    }

    public static CompletableFuture<Optional<GameProfile>> fetchGameProfile(String p_298654_) {
        LoadingCache<String, CompletableFuture<Optional<GameProfile>>> loadingcache = profileCacheByName;
        return loadingcache != null && StringUtil.isValidPlayerName(p_298654_)
            ? loadingcache.getUnchecked(p_298654_)
            : CompletableFuture.completedFuture(Optional.empty());
    }

    public static CompletableFuture<Optional<GameProfile>> fetchGameProfile(UUID p_331248_) {
        LoadingCache<UUID, CompletableFuture<Optional<GameProfile>>> loadingcache = profileCacheById;
        return loadingcache != null ? loadingcache.getUnchecked(p_331248_) : CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_393162_) {
        super.applyImplicitComponents(p_393162_);
        this.setOwner(p_393162_.get(DataComponents.PROFILE));
        this.noteBlockSound = p_393162_.get(DataComponents.NOTE_BLOCK_SOUND);
        this.customName = p_393162_.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_335245_) {
        super.collectImplicitComponents(p_335245_);
        p_335245_.set(DataComponents.PROFILE, this.owner);
        p_335245_.set(DataComponents.NOTE_BLOCK_SOUND, this.noteBlockSound);
        p_335245_.set(DataComponents.CUSTOM_NAME, this.customName);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput p_410487_) {
        super.removeComponentsFromTag(p_410487_);
        p_410487_.discard("profile");
        p_410487_.discard("note_block_sound");
        p_410487_.discard("custom_name");
    }
}