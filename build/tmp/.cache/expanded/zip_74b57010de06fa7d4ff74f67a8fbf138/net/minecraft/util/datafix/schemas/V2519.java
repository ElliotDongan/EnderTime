package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V2519 extends NamespacedSchema {
    public V2519(int p_17892_, Schema p_17893_) {
        super(p_17892_, p_17893_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17901_) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_17901_);
        p_17901_.registerSimple(map, "minecraft:strider");
        return map;
    }
}