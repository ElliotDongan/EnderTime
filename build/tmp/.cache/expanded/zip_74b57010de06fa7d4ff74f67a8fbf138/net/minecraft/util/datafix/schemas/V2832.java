package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2832 extends NamespacedSchema {
    public V2832(int p_185217_, Schema p_185218_) {
        super(p_185217_, p_185218_);
    }

    @Override
    public void registerTypes(Schema p_185234_, Map<String, Supplier<TypeTemplate>> p_185235_, Map<String, Supplier<TypeTemplate>> p_185236_) {
        super.registerTypes(p_185234_, p_185235_, p_185236_);
        p_185234_.registerType(
            false,
            References.CHUNK,
            () -> DSL.fields(
                "Level",
                DSL.optionalFields(
                    "Entities",
                    DSL.list(References.ENTITY_TREE.in(p_185234_)),
                    "TileEntities",
                    DSL.list(DSL.or(References.BLOCK_ENTITY.in(p_185234_), DSL.remainder())),
                    "TileTicks",
                    DSL.list(DSL.fields("i", References.BLOCK_NAME.in(p_185234_))),
                    "Sections",
                    DSL.list(
                        DSL.optionalFields(
                            "biomes",
                            DSL.optionalFields("palette", DSL.list(References.BIOME.in(p_185234_))),
                            "block_states",
                            DSL.optionalFields("palette", DSL.list(References.BLOCK_STATE.in(p_185234_)))
                        )
                    ),
                    "Structures",
                    DSL.optionalFields("Starts", DSL.compoundList(References.STRUCTURE_FEATURE.in(p_185234_)))
                )
            )
        );
        p_185234_.registerType(false, References.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, () -> DSL.constType(namespacedString()));
        p_185234_.registerType(
            false,
            References.WORLD_GEN_SETTINGS,
            () -> DSL.fields(
                "dimensions",
                DSL.compoundList(
                    DSL.constType(namespacedString()),
                    DSL.fields(
                        "generator",
                        DSL.or(DSL.taggedChoiceLazy(
                            "type",
                            DSL.string(),
                            ImmutableMap.of(
                                "minecraft:debug",
                                DSL::remainder,
                                "minecraft:flat",
                                () -> DSL.optionalFields(
                                    "settings",
                                    DSL.optionalFields(
                                        "biome",
                                        References.BIOME.in(p_185234_),
                                        "layers",
                                        DSL.list(DSL.optionalFields("block", References.BLOCK_NAME.in(p_185234_)))
                                    )
                                ),
                                "minecraft:noise",
                                () -> DSL.optionalFields(
                                    "biome_source",
                                    DSL.taggedChoiceLazy(
                                        "type",
                                        DSL.string(),
                                        ImmutableMap.of(
                                            "minecraft:fixed",
                                            () -> DSL.fields("biome", References.BIOME.in(p_185234_)),
                                            "minecraft:multi_noise",
                                            () -> DSL.or(
                                                DSL.fields("preset", References.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST.in(p_185234_)),
                                                DSL.list(DSL.fields("biome", References.BIOME.in(p_185234_)))
                                            ),
                                            "minecraft:checkerboard",
                                            () -> DSL.fields("biomes", DSL.list(References.BIOME.in(p_185234_))),
                                            "minecraft:the_end",
                                            DSL::remainder
                                        )
                                    ),
                                    "settings",
                                    DSL.or(
                                        DSL.constType(DSL.string()),
                                        DSL.optionalFields(
                                            "default_block", References.BLOCK_NAME.in(p_185234_), "default_fluid", References.BLOCK_NAME.in(p_185234_)
                                        )
                                    )
                                )
                            )
                        ), DSL.remainder()) // Keep data if the generator type can't be found
                    )
                )
            )
        );
    }
}
