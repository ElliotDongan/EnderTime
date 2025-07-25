package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1929 extends NamespacedSchema {
    public V1929(int p_17811_, Schema p_17812_) {
        super(p_17811_, p_17812_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17820_) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_17820_);
        p_17820_.register(
            map,
            "minecraft:wandering_trader",
            p_390384_ -> DSL.optionalFields(
                "Inventory", DSL.list(References.ITEM_STACK.in(p_17820_)), "Offers", DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(p_17820_)))
            )
        );
        p_17820_.register(
            map,
            "minecraft:trader_llama",
            p_390386_ -> DSL.optionalFields(
                "Items",
                DSL.list(References.ITEM_STACK.in(p_17820_)),
                "SaddleItem",
                References.ITEM_STACK.in(p_17820_),
                "DecorItem",
                References.ITEM_STACK.in(p_17820_)
            )
        );
        return map;
    }
}