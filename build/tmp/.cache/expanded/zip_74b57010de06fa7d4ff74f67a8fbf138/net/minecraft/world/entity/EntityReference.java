package net.minecraft.world.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.UUIDLookup;
import net.minecraft.world.level.entity.UniquelyIdentifyable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class EntityReference<StoredEntityType extends UniquelyIdentifyable> {
    private static final Codec<? extends EntityReference<?>> CODEC = UUIDUtil.CODEC.xmap(EntityReference::new, EntityReference::getUUID);
    private static final StreamCodec<ByteBuf, ? extends EntityReference<?>> STREAM_CODEC = UUIDUtil.STREAM_CODEC
        .map(EntityReference::new, EntityReference::getUUID);
    private Either<UUID, StoredEntityType> entity;

    public static <Type extends UniquelyIdentifyable> Codec<EntityReference<Type>> codec() {
        return (Codec<EntityReference<Type>>)CODEC;
    }

    public static <Type extends UniquelyIdentifyable> StreamCodec<ByteBuf, EntityReference<Type>> streamCodec() {
        return (StreamCodec<ByteBuf, EntityReference<Type>>)STREAM_CODEC;
    }

    public EntityReference(StoredEntityType p_394409_) {
        this.entity = Either.right(p_394409_);
    }

    public EntityReference(UUID p_396798_) {
        this.entity = Either.left(p_396798_);
    }

    public UUID getUUID() {
        return this.entity.map(p_392547_ -> (UUID)p_392547_, UniquelyIdentifyable::getUUID);
    }

    @Nullable
    public StoredEntityType getEntity(UUIDLookup<? super StoredEntityType> p_395137_, Class<StoredEntityType> p_396644_) {
        Optional<StoredEntityType> optional = this.entity.right();
        if (optional.isPresent()) {
            StoredEntityType storedentitytype = optional.get();
            if (!storedentitytype.isRemoved()) {
                return storedentitytype;
            }

            this.entity = Either.left(storedentitytype.getUUID());
        }

        Optional<UUID> optional1 = this.entity.left();
        if (optional1.isPresent()) {
            StoredEntityType storedentitytype1 = this.resolve(p_395137_.getEntity(optional1.get()), p_396644_);
            if (storedentitytype1 != null && !storedentitytype1.isRemoved()) {
                this.entity = Either.right(storedentitytype1);
                return storedentitytype1;
            }
        }

        return null;
    }

    @Nullable
    private StoredEntityType resolve(@Nullable UniquelyIdentifyable p_396749_, Class<StoredEntityType> p_391245_) {
        return p_396749_ != null && p_391245_.isAssignableFrom(p_396749_.getClass()) ? p_391245_.cast(p_396749_) : null;
    }

    public boolean matches(StoredEntityType p_396754_) {
        return this.getUUID().equals(p_396754_.getUUID());
    }

    public void store(ValueOutput p_410717_, String p_396728_) {
        p_410717_.store(p_396728_, UUIDUtil.CODEC, this.getUUID());
    }

    public static void store(@Nullable EntityReference<?> p_409297_, ValueOutput p_406877_, String p_406736_) {
        if (p_409297_ != null) {
            p_409297_.store(p_406877_, p_406736_);
        }
    }

    @Nullable
    public static <StoredEntityType extends UniquelyIdentifyable> StoredEntityType get(
        @Nullable EntityReference<StoredEntityType> p_392004_, UUIDLookup<? super StoredEntityType> p_397681_, Class<StoredEntityType> p_397380_
    ) {
        return p_392004_ != null ? p_392004_.getEntity(p_397681_, p_397380_) : null;
    }

    @Nullable
    public static <StoredEntityType extends UniquelyIdentifyable> EntityReference<StoredEntityType> read(ValueInput p_407277_, String p_393635_) {
        return p_407277_.read(p_393635_, EntityReference.<StoredEntityType>codec()).orElse(null);
    }

    @Nullable
    public static <StoredEntityType extends UniquelyIdentifyable> EntityReference<StoredEntityType> readWithOldOwnerConversion(
        ValueInput p_407222_, String p_395953_, Level p_395721_
    ) {
        Optional<UUID> optional = p_407222_.read(p_395953_, UUIDUtil.CODEC);
        return optional.isPresent()
            ? new EntityReference<>(optional.get())
            : p_407222_.getString(p_395953_)
                .map(p_395444_ -> OldUsersConverter.convertMobOwnerIfNecessary(p_395721_.getServer(), p_395444_))
                .map(EntityReference<StoredEntityType>::new)
                .orElse(null);
    }

    @Override
    public boolean equals(Object p_407192_) {
        return p_407192_ == this ? true : p_407192_ instanceof EntityReference<?> entityreference && this.getUUID().equals(entityreference.getUUID());
    }

    @Override
    public int hashCode() {
        return this.getUUID().hashCode();
    }
}