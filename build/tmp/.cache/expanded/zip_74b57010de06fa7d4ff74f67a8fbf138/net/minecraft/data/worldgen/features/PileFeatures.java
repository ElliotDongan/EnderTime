package net.minecraft.data.worldgen.features;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RotatedBlockProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;

public class PileFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> PILE_HAY = FeatureUtils.createKey("pile_hay");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PILE_MELON = FeatureUtils.createKey("pile_melon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PILE_SNOW = FeatureUtils.createKey("pile_snow");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PILE_ICE = FeatureUtils.createKey("pile_ice");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PILE_PUMPKIN = FeatureUtils.createKey("pile_pumpkin");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> p_329897_) {
        FeatureUtils.register(p_329897_, PILE_HAY, Feature.BLOCK_PILE, new BlockPileConfiguration(new RotatedBlockProvider(Blocks.HAY_BLOCK)));
        FeatureUtils.register(p_329897_, PILE_MELON, Feature.BLOCK_PILE, new BlockPileConfiguration(BlockStateProvider.simple(Blocks.MELON)));
        FeatureUtils.register(p_329897_, PILE_SNOW, Feature.BLOCK_PILE, new BlockPileConfiguration(BlockStateProvider.simple(Blocks.SNOW)));
        FeatureUtils.register(
            p_329897_,
            PILE_ICE,
            Feature.BLOCK_PILE,
            new BlockPileConfiguration(
                new WeightedStateProvider(
                    WeightedList.<BlockState>builder().add(Blocks.BLUE_ICE.defaultBlockState(), 1).add(Blocks.PACKED_ICE.defaultBlockState(), 5)
                )
            )
        );
        FeatureUtils.register(
            p_329897_,
            PILE_PUMPKIN,
            Feature.BLOCK_PILE,
            new BlockPileConfiguration(
                new WeightedStateProvider(
                    WeightedList.<BlockState>builder().add(Blocks.PUMPKIN.defaultBlockState(), 19).add(Blocks.JACK_O_LANTERN.defaultBlockState(), 1)
                )
            )
        );
    }
}