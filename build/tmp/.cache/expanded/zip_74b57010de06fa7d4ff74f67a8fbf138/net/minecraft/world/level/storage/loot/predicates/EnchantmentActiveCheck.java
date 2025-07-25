package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record EnchantmentActiveCheck(boolean active) implements LootItemCondition {
    public static final MapCodec<EnchantmentActiveCheck> CODEC = RecordCodecBuilder.mapCodec(
        p_342477_ -> p_342477_.group(Codec.BOOL.fieldOf("active").forGetter(EnchantmentActiveCheck::active)).apply(p_342477_, EnchantmentActiveCheck::new)
    );

    public boolean test(LootContext p_344469_) {
        return p_344469_.getParameter(LootContextParams.ENCHANTMENT_ACTIVE) == this.active;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ENCHANTMENT_ACTIVE_CHECK;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.ENCHANTMENT_ACTIVE);
    }

    public static LootItemCondition.Builder enchantmentActiveCheck() {
        return () -> new EnchantmentActiveCheck(true);
    }

    public static LootItemCondition.Builder enchantmentInactiveCheck() {
        return () -> new EnchantmentActiveCheck(false);
    }
}