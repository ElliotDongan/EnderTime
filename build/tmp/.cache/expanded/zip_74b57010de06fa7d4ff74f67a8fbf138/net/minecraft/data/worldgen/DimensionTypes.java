package net.minecraft.data.worldgen;

import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

public class DimensionTypes {
    public static void bootstrap(BootstrapContext<DimensionType> p_333835_) {
        p_333835_.register(
            BuiltinDimensionTypes.OVERWORLD,
            new DimensionType(
                OptionalLong.empty(),
                true,
                false,
                false,
                true,
                1.0,
                true,
                false,
                -64,
                384,
                384,
                BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                0.0F,
                Optional.of(192),
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
            )
        );
        p_333835_.register(
            BuiltinDimensionTypes.NETHER,
            new DimensionType(
                OptionalLong.of(18000L),
                false,
                true,
                true,
                false,
                8.0,
                false,
                true,
                0,
                256,
                128,
                BlockTags.INFINIBURN_NETHER,
                BuiltinDimensionTypes.NETHER_EFFECTS,
                0.1F,
                Optional.empty(),
                new DimensionType.MonsterSettings(true, false, ConstantInt.of(7), 15)
            )
        );
        p_333835_.register(
            BuiltinDimensionTypes.END,
            new DimensionType(
                OptionalLong.of(6000L),
                false,
                false,
                false,
                false,
                1.0,
                false,
                false,
                0,
                256,
                256,
                BlockTags.INFINIBURN_END,
                BuiltinDimensionTypes.END_EFFECTS,
                0.0F,
                Optional.empty(),
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
            )
        );
        p_333835_.register(
            BuiltinDimensionTypes.OVERWORLD_CAVES,
            new DimensionType(
                OptionalLong.empty(),
                true,
                true,
                false,
                true,
                1.0,
                true,
                false,
                -64,
                384,
                384,
                BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                0.0F,
                Optional.of(192),
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
            )
        );
    }
}