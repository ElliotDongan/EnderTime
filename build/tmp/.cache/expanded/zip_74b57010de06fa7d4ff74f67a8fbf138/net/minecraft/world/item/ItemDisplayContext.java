package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum ItemDisplayContext implements StringRepresentable, net.minecraftforge.common.IExtensibleEnum {
    NONE(0, "none"),
    THIRD_PERSON_LEFT_HAND(1, "thirdperson_lefthand"),
    THIRD_PERSON_RIGHT_HAND(2, "thirdperson_righthand"),
    FIRST_PERSON_LEFT_HAND(3, "firstperson_lefthand"),
    FIRST_PERSON_RIGHT_HAND(4, "firstperson_righthand"),
    HEAD(5, "head"),
    GUI(6, "gui"),
    GROUND(7, "ground"),
    FIXED(8, "fixed");

    public static final Codec<ItemDisplayContext> CODEC =  com.mojang.serialization.Codec.lazyInitialized(() -> StringRepresentable.fromEnum(ItemDisplayContext::values));
    public static final IntFunction<ItemDisplayContext> BY_ID = id -> java.util.Objects.requireNonNullElse(((net.minecraftforge.registries.IForgeRegistryInternal<ItemDisplayContext>) net.minecraftforge.registries.ForgeRegistries.DISPLAY_CONTEXTS.get()).getValue(id < 0 ? Byte.MAX_VALUE + -id : id), NONE);
    private byte id;
    private final String name;

    private ItemDisplayContext(final int p_270624_, final String p_270851_) {
        this.name = p_270851_;
        this.id = (byte)p_270624_;
        this.isModded = false;
        this.fallback = null;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public byte getId() {
        return this.id;
    }

    public boolean firstPerson() {
        return this == FIRST_PERSON_LEFT_HAND || this == FIRST_PERSON_RIGHT_HAND;
    }

    public boolean leftHand() {
        return this == FIRST_PERSON_LEFT_HAND || this == THIRD_PERSON_LEFT_HAND;
    }

    private final boolean isModded;
    @javax.annotation.Nullable
    private final ItemDisplayContext fallback;

    private ItemDisplayContext(net.minecraft.resources.ResourceLocation serializeName, ItemDisplayContext fallback) {
        this.id = 0; // If the context is never registered then it should be treated as the NONE one
        this.name = java.util.Objects.requireNonNull(serializeName, "Modded ItemDisplayContexts must have a non-null serializeName").toString();
        this.isModded = true;
        this.fallback = fallback;
    }

    public boolean isModded() {
        return isModded;
    }

    public @javax.annotation.Nullable ItemDisplayContext fallback() {
        return fallback;
    }

    public static ItemDisplayContext create(String keyName, net.minecraft.resources.ResourceLocation serializedName, @javax.annotation.Nullable ItemDisplayContext fallback) {
        throw new IllegalStateException("Enum not extended!");
    }

    public static final net.minecraftforge.registries.IForgeRegistry.AddCallback<ItemDisplayContext> ADD_CALLBACK = (owner, stage, id, key, obj, oldObj) -> obj.id = id > Byte.MAX_VALUE ? (byte) -(id - Byte.MAX_VALUE) : (byte) id; // if the ID is > 127 start doing negative IDs
}
