package net.minecraft.data.worldgen.features;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FallenTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.ThreeLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.AcaciaFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BushFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.DarkOakFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.MegaJungleFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.MegaPineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.RandomSpreadFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.rootplacers.AboveRootPlacement;
import net.minecraft.world.level.levelgen.feature.rootplacers.MangroveRootPlacement;
import net.minecraft.world.level.levelgen.feature.rootplacers.MangroveRootPlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.AlterGroundDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.AttachedToLeavesDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.AttachedToLogsDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.CocoaDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.CreakingHeartDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.LeaveVineDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.PaleMossDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.PlaceOnGroundDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TrunkVineDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.BendingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.CherryTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.DarkOakTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.MegaJungleTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.UpwardsBranchingTrunkPlacer;

public class TreeFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> CRIMSON_FUNGUS = FeatureUtils.createKey("crimson_fungus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CRIMSON_FUNGUS_PLANTED = FeatureUtils.createKey("crimson_fungus_planted");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WARPED_FUNGUS = FeatureUtils.createKey("warped_fungus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WARPED_FUNGUS_PLANTED = FeatureUtils.createKey("warped_fungus_planted");
    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_BROWN_MUSHROOM = FeatureUtils.createKey("huge_brown_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_RED_MUSHROOM = FeatureUtils.createKey("huge_red_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK = FeatureUtils.createKey("oak");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DARK_OAK = FeatureUtils.createKey("dark_oak");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_OAK = FeatureUtils.createKey("pale_oak");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_OAK_BONEMEAL = FeatureUtils.createKey("pale_oak_bonemeal");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_OAK_CREAKING = FeatureUtils.createKey("pale_oak_creaking");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIRCH = FeatureUtils.createKey("birch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ACACIA = FeatureUtils.createKey("acacia");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPRUCE = FeatureUtils.createKey("spruce");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PINE = FeatureUtils.createKey("pine");
    public static final ResourceKey<ConfiguredFeature<?, ?>> JUNGLE_TREE = FeatureUtils.createKey("jungle_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_OAK = FeatureUtils.createKey("fancy_oak");
    public static final ResourceKey<ConfiguredFeature<?, ?>> JUNGLE_TREE_NO_VINE = FeatureUtils.createKey("jungle_tree_no_vine");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MEGA_JUNGLE_TREE = FeatureUtils.createKey("mega_jungle_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MEGA_SPRUCE = FeatureUtils.createKey("mega_spruce");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MEGA_PINE = FeatureUtils.createKey("mega_pine");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SUPER_BIRCH_BEES_0002 = FeatureUtils.createKey("super_birch_bees_0002");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SUPER_BIRCH_BEES = FeatureUtils.createKey("super_birch_bees");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SWAMP_OAK = FeatureUtils.createKey("swamp_oak");
    public static final ResourceKey<ConfiguredFeature<?, ?>> JUNGLE_BUSH = FeatureUtils.createKey("jungle_bush");
    public static final ResourceKey<ConfiguredFeature<?, ?>> AZALEA_TREE = FeatureUtils.createKey("azalea_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MANGROVE = FeatureUtils.createKey("mangrove");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TALL_MANGROVE = FeatureUtils.createKey("tall_mangrove");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CHERRY = FeatureUtils.createKey("cherry");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK_BEES_0002_LEAF_LITTER = FeatureUtils.createKey("oak_bees_0002_leaf_litter");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK_BEES_002 = FeatureUtils.createKey("oak_bees_002");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK_BEES_005 = FeatureUtils.createKey("oak_bees_005");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIRCH_BEES_0002 = FeatureUtils.createKey("birch_bees_0002");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIRCH_BEES_0002_LEAF_LITTER = FeatureUtils.createKey("birch_bees_0002_leaf_litter");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIRCH_BEES_002 = FeatureUtils.createKey("birch_bees_002");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIRCH_BEES_005 = FeatureUtils.createKey("birch_bees_005");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_OAK_BEES_0002_LEAF_LITTER = FeatureUtils.createKey("fancy_oak_bees_0002_leaf_litter");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_OAK_BEES_002 = FeatureUtils.createKey("fancy_oak_bees_002");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_OAK_BEES_005 = FeatureUtils.createKey("fancy_oak_bees_005");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_OAK_BEES = FeatureUtils.createKey("fancy_oak_bees");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CHERRY_BEES_005 = FeatureUtils.createKey("cherry_bees_005");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK_LEAF_LITTER = FeatureUtils.createKey("oak_leaf_litter");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DARK_OAK_LEAF_LITTER = FeatureUtils.createKey("dark_oak_leaf_litter");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIRCH_LEAF_LITTER = FeatureUtils.createKey("birch_leaf_litter");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_OAK_LEAF_LITTER = FeatureUtils.createKey("fancy_oak_leaf_litter");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_OAK_TREE = FeatureUtils.createKey("fallen_oak_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_JUNGLE_TREE = FeatureUtils.createKey("fallen_jungle_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_SPRUCE_TREE = FeatureUtils.createKey("fallen_spruce_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_BIRCH_TREE = FeatureUtils.createKey("fallen_birch_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_SUPER_BIRCH_TREE = FeatureUtils.createKey("fallen_super_birch_tree");

    private static TreeConfiguration.TreeConfigurationBuilder createStraightBlobTree(
        Block p_195147_, Block p_195148_, int p_195149_, int p_195150_, int p_195151_, int p_195152_
    ) {
        return new TreeConfiguration.TreeConfigurationBuilder(
            BlockStateProvider.simple(p_195147_),
            new StraightTrunkPlacer(p_195149_, p_195150_, p_195151_),
            BlockStateProvider.simple(p_195148_),
            new BlobFoliagePlacer(ConstantInt.of(p_195152_), ConstantInt.of(0), 3),
            new TwoLayersFeatureSize(1, 0, 1)
        );
    }

    private static TreeConfiguration.TreeConfigurationBuilder createOak() {
        return createStraightBlobTree(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4, 2, 0, 2).ignoreVines();
    }

    private static TreeConfiguration.TreeConfigurationBuilder createDarkOak() {
        return new TreeConfiguration.TreeConfigurationBuilder(
            BlockStateProvider.simple(Blocks.DARK_OAK_LOG),
            new DarkOakTrunkPlacer(6, 2, 1),
            BlockStateProvider.simple(Blocks.DARK_OAK_LEAVES),
            new DarkOakFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0)),
            new ThreeLayersFeatureSize(1, 1, 0, 1, 2, OptionalInt.empty())
        );
    }

    private static FallenTreeConfiguration.FallenTreeConfigurationBuilder createFallenOak() {
        return createFallenTrees(Blocks.OAK_LOG, 4, 7).stumpDecorators(ImmutableList.of(TrunkVineDecorator.INSTANCE));
    }

    private static FallenTreeConfiguration.FallenTreeConfigurationBuilder createFallenBirch(int p_394750_) {
        return createFallenTrees(Blocks.BIRCH_LOG, 5, p_394750_);
    }

    private static FallenTreeConfiguration.FallenTreeConfigurationBuilder createFallenJungle() {
        return createFallenTrees(Blocks.JUNGLE_LOG, 4, 11).stumpDecorators(ImmutableList.of(TrunkVineDecorator.INSTANCE));
    }

    private static FallenTreeConfiguration.FallenTreeConfigurationBuilder createFallenSpruce() {
        return createFallenTrees(Blocks.SPRUCE_LOG, 6, 10);
    }

    private static FallenTreeConfiguration.FallenTreeConfigurationBuilder createFallenTrees(Block p_394430_, int p_393752_, int p_391459_) {
        return new FallenTreeConfiguration.FallenTreeConfigurationBuilder(BlockStateProvider.simple(p_394430_), UniformInt.of(p_393752_, p_391459_))
            .logDecorators(
                ImmutableList.of(
                    new AttachedToLogsDecorator(
                        0.1F,
                        new WeightedStateProvider(
                            WeightedList.<BlockState>builder().add(Blocks.RED_MUSHROOM.defaultBlockState(), 2).add(Blocks.BROWN_MUSHROOM.defaultBlockState(), 1)
                        ),
                        List.of(Direction.UP)
                    )
                )
            );
    }

    private static TreeConfiguration.TreeConfigurationBuilder createBirch() {
        return createStraightBlobTree(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5, 2, 0, 2).ignoreVines();
    }

    private static TreeConfiguration.TreeConfigurationBuilder createSuperBirch() {
        return createStraightBlobTree(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5, 2, 6, 2).ignoreVines();
    }

    private static TreeConfiguration.TreeConfigurationBuilder createJungleTree() {
        return createStraightBlobTree(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, 4, 8, 0, 2);
    }

    private static TreeConfiguration.TreeConfigurationBuilder createFancyOak() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.OAK_LOG),
                new FancyTrunkPlacer(3, 11, 0),
                BlockStateProvider.simple(Blocks.OAK_LEAVES),
                new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(4), 4),
                new TwoLayersFeatureSize(0, 0, 0, OptionalInt.of(4))
            )
            .ignoreVines();
    }

    private static TreeConfiguration.TreeConfigurationBuilder cherry() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.CHERRY_LOG),
                new CherryTrunkPlacer(
                    7,
                    1,
                    0,
                    new WeightedListInt(
                        WeightedList.<IntProvider>builder()
                            .add(ConstantInt.of(1), 1)
                            .add(ConstantInt.of(2), 1)
                            .add(ConstantInt.of(3), 1)
                            .build()
                    ),
                    UniformInt.of(2, 4),
                    UniformInt.of(-4, -3),
                    UniformInt.of(-1, 0)
                ),
                BlockStateProvider.simple(Blocks.CHERRY_LEAVES),
                new CherryFoliagePlacer(ConstantInt.of(4), ConstantInt.of(0), ConstantInt.of(5), 0.25F, 0.5F, 0.16666667F, 0.33333334F),
                new TwoLayersFeatureSize(1, 0, 2)
            )
            .ignoreVines();
    }

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> p_334110_) {
        HolderGetter<Block> holdergetter = p_334110_.lookup(Registries.BLOCK);
        BlockPredicate blockpredicate = BlockPredicate.matchesBlocks(
            Blocks.OAK_SAPLING,
            Blocks.SPRUCE_SAPLING,
            Blocks.BIRCH_SAPLING,
            Blocks.JUNGLE_SAPLING,
            Blocks.ACACIA_SAPLING,
            Blocks.CHERRY_SAPLING,
            Blocks.DARK_OAK_SAPLING,
            Blocks.PALE_OAK_SAPLING,
            Blocks.MANGROVE_PROPAGULE,
            Blocks.DANDELION,
            Blocks.TORCHFLOWER,
            Blocks.POPPY,
            Blocks.BLUE_ORCHID,
            Blocks.ALLIUM,
            Blocks.AZURE_BLUET,
            Blocks.RED_TULIP,
            Blocks.ORANGE_TULIP,
            Blocks.WHITE_TULIP,
            Blocks.PINK_TULIP,
            Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER,
            Blocks.WITHER_ROSE,
            Blocks.LILY_OF_THE_VALLEY,
            Blocks.BROWN_MUSHROOM,
            Blocks.RED_MUSHROOM,
            Blocks.WHEAT,
            Blocks.SUGAR_CANE,
            Blocks.ATTACHED_PUMPKIN_STEM,
            Blocks.ATTACHED_MELON_STEM,
            Blocks.PUMPKIN_STEM,
            Blocks.MELON_STEM,
            Blocks.LILY_PAD,
            Blocks.NETHER_WART,
            Blocks.COCOA,
            Blocks.CARROTS,
            Blocks.POTATOES,
            Blocks.CHORUS_PLANT,
            Blocks.CHORUS_FLOWER,
            Blocks.TORCHFLOWER_CROP,
            Blocks.PITCHER_CROP,
            Blocks.BEETROOTS,
            Blocks.SWEET_BERRY_BUSH,
            Blocks.WARPED_FUNGUS,
            Blocks.CRIMSON_FUNGUS,
            Blocks.WEEPING_VINES,
            Blocks.WEEPING_VINES_PLANT,
            Blocks.TWISTING_VINES,
            Blocks.TWISTING_VINES_PLANT,
            Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT,
            Blocks.SPORE_BLOSSOM,
            Blocks.AZALEA,
            Blocks.FLOWERING_AZALEA,
            Blocks.MOSS_CARPET,
            Blocks.PINK_PETALS,
            Blocks.WILDFLOWERS,
            Blocks.BIG_DRIPLEAF,
            Blocks.BIG_DRIPLEAF_STEM,
            Blocks.SMALL_DRIPLEAF
        );
        FeatureUtils.register(
            p_334110_,
            CRIMSON_FUNGUS,
            Feature.HUGE_FUNGUS,
            new HugeFungusConfiguration(
                Blocks.CRIMSON_NYLIUM.defaultBlockState(), Blocks.CRIMSON_STEM.defaultBlockState(), Blocks.NETHER_WART_BLOCK.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState(), blockpredicate, false
            )
        );
        FeatureUtils.register(
            p_334110_,
            CRIMSON_FUNGUS_PLANTED,
            Feature.HUGE_FUNGUS,
            new HugeFungusConfiguration(
                Blocks.CRIMSON_NYLIUM.defaultBlockState(), Blocks.CRIMSON_STEM.defaultBlockState(), Blocks.NETHER_WART_BLOCK.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState(), blockpredicate, true
            )
        );
        FeatureUtils.register(
            p_334110_,
            WARPED_FUNGUS,
            Feature.HUGE_FUNGUS,
            new HugeFungusConfiguration(
                Blocks.WARPED_NYLIUM.defaultBlockState(), Blocks.WARPED_STEM.defaultBlockState(), Blocks.WARPED_WART_BLOCK.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState(), blockpredicate, false
            )
        );
        FeatureUtils.register(
            p_334110_,
            WARPED_FUNGUS_PLANTED,
            Feature.HUGE_FUNGUS,
            new HugeFungusConfiguration(
                Blocks.WARPED_NYLIUM.defaultBlockState(), Blocks.WARPED_STEM.defaultBlockState(), Blocks.WARPED_WART_BLOCK.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState(), blockpredicate, true
            )
        );
        FeatureUtils.register(
            p_334110_,
            HUGE_BROWN_MUSHROOM,
            Feature.HUGE_BROWN_MUSHROOM,
            new HugeMushroomFeatureConfiguration(
                BlockStateProvider.simple(Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState().setValue(HugeMushroomBlock.UP, true).setValue(HugeMushroomBlock.DOWN, false)),
                BlockStateProvider.simple(Blocks.MUSHROOM_STEM.defaultBlockState().setValue(HugeMushroomBlock.UP, false).setValue(HugeMushroomBlock.DOWN, false)),
                3
            )
        );
        FeatureUtils.register(
            p_334110_,
            HUGE_RED_MUSHROOM,
            Feature.HUGE_RED_MUSHROOM,
            new HugeMushroomFeatureConfiguration(
                BlockStateProvider.simple(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState().setValue(HugeMushroomBlock.DOWN, false)),
                BlockStateProvider.simple(Blocks.MUSHROOM_STEM.defaultBlockState().setValue(HugeMushroomBlock.UP, false).setValue(HugeMushroomBlock.DOWN, false)),
                2
            )
        );
        BeehiveDecorator beehivedecorator = new BeehiveDecorator(0.002F);
        BeehiveDecorator beehivedecorator1 = new BeehiveDecorator(0.01F);
        BeehiveDecorator beehivedecorator2 = new BeehiveDecorator(0.02F);
        BeehiveDecorator beehivedecorator3 = new BeehiveDecorator(0.05F);
        BeehiveDecorator beehivedecorator4 = new BeehiveDecorator(1.0F);
        PlaceOnGroundDecorator placeongrounddecorator = new PlaceOnGroundDecorator(96, 4, 2, new WeightedStateProvider(VegetationFeatures.leafLitterPatchBuilder(1, 3)));
        PlaceOnGroundDecorator placeongrounddecorator1 = new PlaceOnGroundDecorator(150, 2, 2, new WeightedStateProvider(VegetationFeatures.leafLitterPatchBuilder(1, 4)));
        FeatureUtils.register(p_334110_, OAK, Feature.TREE, createOak().build());
        FeatureUtils.register(p_334110_, DARK_OAK, Feature.TREE, createDarkOak().ignoreVines().build());
        FeatureUtils.register(
            p_334110_,
            PALE_OAK,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.PALE_OAK_LOG),
                    new DarkOakTrunkPlacer(6, 2, 1),
                    BlockStateProvider.simple(Blocks.PALE_OAK_LEAVES),
                    new DarkOakFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0)),
                    new ThreeLayersFeatureSize(1, 1, 0, 1, 2, OptionalInt.empty())
                )
                .decorators(ImmutableList.of(new PaleMossDecorator(0.15F, 0.4F, 0.8F)))
                .ignoreVines()
                .build()
        );
        FeatureUtils.register(
            p_334110_,
            PALE_OAK_BONEMEAL,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.PALE_OAK_LOG),
                    new DarkOakTrunkPlacer(6, 2, 1),
                    BlockStateProvider.simple(Blocks.PALE_OAK_LEAVES),
                    new DarkOakFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0)),
                    new ThreeLayersFeatureSize(1, 1, 0, 1, 2, OptionalInt.empty())
                )
                .ignoreVines()
                .build()
        );
        FeatureUtils.register(
            p_334110_,
            PALE_OAK_CREAKING,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.PALE_OAK_LOG),
                    new DarkOakTrunkPlacer(6, 2, 1),
                    BlockStateProvider.simple(Blocks.PALE_OAK_LEAVES),
                    new DarkOakFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0)),
                    new ThreeLayersFeatureSize(1, 1, 0, 1, 2, OptionalInt.empty())
                )
                .decorators(ImmutableList.of(new PaleMossDecorator(0.15F, 0.4F, 0.8F), new CreakingHeartDecorator(1.0F)))
                .ignoreVines()
                .build()
        );
        FeatureUtils.register(p_334110_, BIRCH, Feature.TREE, createBirch().build());
        FeatureUtils.register(
            p_334110_,
            ACACIA,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.ACACIA_LOG),
                    new ForkingTrunkPlacer(5, 2, 2),
                    BlockStateProvider.simple(Blocks.ACACIA_LEAVES),
                    new AcaciaFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0)),
                    new TwoLayersFeatureSize(1, 0, 2)
                )
                .ignoreVines()
                .build()
        );
        FeatureUtils.register(p_334110_, CHERRY, Feature.TREE, cherry().build());
        FeatureUtils.register(p_334110_, CHERRY_BEES_005, Feature.TREE, cherry().decorators(List.of(beehivedecorator3)).build());
        FeatureUtils.register(
            p_334110_,
            SPRUCE,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.SPRUCE_LOG),
                    new StraightTrunkPlacer(5, 2, 1),
                    BlockStateProvider.simple(Blocks.SPRUCE_LEAVES),
                    new SpruceFoliagePlacer(UniformInt.of(2, 3), UniformInt.of(0, 2), UniformInt.of(1, 2)),
                    new TwoLayersFeatureSize(2, 0, 2)
                )
                .ignoreVines()
                .build()
        );
        FeatureUtils.register(
            p_334110_,
            PINE,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.SPRUCE_LOG),
                    new StraightTrunkPlacer(6, 4, 0),
                    BlockStateProvider.simple(Blocks.SPRUCE_LEAVES),
                    new PineFoliagePlacer(ConstantInt.of(1), ConstantInt.of(1), UniformInt.of(3, 4)),
                    new TwoLayersFeatureSize(2, 0, 2)
                )
                .ignoreVines()
                .build()
        );
        FeatureUtils.register(
            p_334110_,
            JUNGLE_TREE,
            Feature.TREE,
            createJungleTree().decorators(ImmutableList.of(new CocoaDecorator(0.2F), TrunkVineDecorator.INSTANCE, new LeaveVineDecorator(0.25F))).ignoreVines().build()
        );
        FeatureUtils.register(p_334110_, FANCY_OAK, Feature.TREE, createFancyOak().build());
        FeatureUtils.register(p_334110_, JUNGLE_TREE_NO_VINE, Feature.TREE, createJungleTree().ignoreVines().build());
        FeatureUtils.register(
            p_334110_,
            MEGA_JUNGLE_TREE,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.JUNGLE_LOG),
                    new MegaJungleTrunkPlacer(10, 2, 19),
                    BlockStateProvider.simple(Blocks.JUNGLE_LEAVES),
                    new MegaJungleFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 2),
                    new TwoLayersFeatureSize(1, 1, 2)
                )
                .decorators(ImmutableList.of(TrunkVineDecorator.INSTANCE, new LeaveVineDecorator(0.25F)))
                .build()
        );
        FeatureUtils.register(
            p_334110_,
            MEGA_SPRUCE,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.SPRUCE_LOG),
                    new GiantTrunkPlacer(13, 2, 14),
                    BlockStateProvider.simple(Blocks.SPRUCE_LEAVES),
                    new MegaPineFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0), UniformInt.of(13, 17)),
                    new TwoLayersFeatureSize(1, 1, 2)
                )
                .decorators(ImmutableList.of(new AlterGroundDecorator(BlockStateProvider.simple(Blocks.PODZOL))))
                .build()
        );
        FeatureUtils.register(
            p_334110_,
            MEGA_PINE,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.SPRUCE_LOG),
                    new GiantTrunkPlacer(13, 2, 14),
                    BlockStateProvider.simple(Blocks.SPRUCE_LEAVES),
                    new MegaPineFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0), UniformInt.of(3, 7)),
                    new TwoLayersFeatureSize(1, 1, 2)
                )
                .decorators(ImmutableList.of(new AlterGroundDecorator(BlockStateProvider.simple(Blocks.PODZOL))))
                .build()
        );
        FeatureUtils.register(p_334110_, SUPER_BIRCH_BEES_0002, Feature.TREE, createSuperBirch().decorators(ImmutableList.of(beehivedecorator)).build());
        FeatureUtils.register(p_334110_, SUPER_BIRCH_BEES, Feature.TREE, createSuperBirch().decorators(ImmutableList.of(beehivedecorator4)).build());
        FeatureUtils.register(
            p_334110_,
            SWAMP_OAK,
            Feature.TREE,
            createStraightBlobTree(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 5, 3, 0, 3).decorators(ImmutableList.of(new LeaveVineDecorator(0.25F))).build()
        );
        FeatureUtils.register(
            p_334110_,
            JUNGLE_BUSH,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.JUNGLE_LOG),
                    new StraightTrunkPlacer(1, 0, 0),
                    BlockStateProvider.simple(Blocks.OAK_LEAVES),
                    new BushFoliagePlacer(ConstantInt.of(2), ConstantInt.of(1), 2),
                    new TwoLayersFeatureSize(0, 0, 0)
                )
                .build()
        );
        FeatureUtils.register(
            p_334110_,
            AZALEA_TREE,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.OAK_LOG),
                    new BendingTrunkPlacer(4, 2, 0, 3, UniformInt.of(1, 2)),
                    new WeightedStateProvider(
                        WeightedList.<BlockState>builder().add(Blocks.AZALEA_LEAVES.defaultBlockState(), 3).add(Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState(), 1)
                    ),
                    new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), ConstantInt.of(2), 50),
                    new TwoLayersFeatureSize(1, 0, 1)
                )
                .dirt(BlockStateProvider.simple(Blocks.ROOTED_DIRT))
                .forceDirt()
                .build()
        );
        FeatureUtils.register(
            p_334110_,
            MANGROVE,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.MANGROVE_LOG),
                    new UpwardsBranchingTrunkPlacer(
                        2, 1, 4, UniformInt.of(1, 4), 0.5F, UniformInt.of(0, 1), holdergetter.getOrThrow(BlockTags.MANGROVE_LOGS_CAN_GROW_THROUGH)
                    ),
                    BlockStateProvider.simple(Blocks.MANGROVE_LEAVES),
                    new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), ConstantInt.of(2), 70),
                    Optional.of(
                        new MangroveRootPlacer(
                            UniformInt.of(1, 3),
                            BlockStateProvider.simple(Blocks.MANGROVE_ROOTS),
                            Optional.of(new AboveRootPlacement(BlockStateProvider.simple(Blocks.MOSS_CARPET), 0.5F)),
                            new MangroveRootPlacement(
                                holdergetter.getOrThrow(BlockTags.MANGROVE_ROOTS_CAN_GROW_THROUGH),
                                HolderSet.direct(Block::builtInRegistryHolder, Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS),
                                BlockStateProvider.simple(Blocks.MUDDY_MANGROVE_ROOTS),
                                8,
                                15,
                                0.2F
                            )
                        )
                    ),
                    new TwoLayersFeatureSize(2, 0, 2)
                )
                .decorators(
                    List.of(
                        new LeaveVineDecorator(0.125F),
                        new AttachedToLeavesDecorator(
                            0.14F,
                            1,
                            0,
                            new RandomizedIntStateProvider(
                                BlockStateProvider.simple(Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(MangrovePropaguleBlock.HANGING, true)),
                                MangrovePropaguleBlock.AGE,
                                UniformInt.of(0, 4)
                            ),
                            2,
                            List.of(Direction.DOWN)
                        ),
                        beehivedecorator1
                    )
                )
                .ignoreVines()
                .build()
        );
        FeatureUtils.register(
            p_334110_,
            TALL_MANGROVE,
            Feature.TREE,
            new TreeConfiguration.TreeConfigurationBuilder(
                    BlockStateProvider.simple(Blocks.MANGROVE_LOG),
                    new UpwardsBranchingTrunkPlacer(
                        4, 1, 9, UniformInt.of(1, 6), 0.5F, UniformInt.of(0, 1), holdergetter.getOrThrow(BlockTags.MANGROVE_LOGS_CAN_GROW_THROUGH)
                    ),
                    BlockStateProvider.simple(Blocks.MANGROVE_LEAVES),
                    new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), ConstantInt.of(2), 70),
                    Optional.of(
                        new MangroveRootPlacer(
                            UniformInt.of(3, 7),
                            BlockStateProvider.simple(Blocks.MANGROVE_ROOTS),
                            Optional.of(new AboveRootPlacement(BlockStateProvider.simple(Blocks.MOSS_CARPET), 0.5F)),
                            new MangroveRootPlacement(
                                holdergetter.getOrThrow(BlockTags.MANGROVE_ROOTS_CAN_GROW_THROUGH),
                                HolderSet.direct(Block::builtInRegistryHolder, Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS),
                                BlockStateProvider.simple(Blocks.MUDDY_MANGROVE_ROOTS),
                                8,
                                15,
                                0.2F
                            )
                        )
                    ),
                    new TwoLayersFeatureSize(3, 0, 2)
                )
                .decorators(
                    List.of(
                        new LeaveVineDecorator(0.125F),
                        new AttachedToLeavesDecorator(
                            0.14F,
                            1,
                            0,
                            new RandomizedIntStateProvider(
                                BlockStateProvider.simple(Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(MangrovePropaguleBlock.HANGING, true)),
                                MangrovePropaguleBlock.AGE,
                                UniformInt.of(0, 4)
                            ),
                            2,
                            List.of(Direction.DOWN)
                        ),
                        beehivedecorator1
                    )
                )
                .ignoreVines()
                .build()
        );
        FeatureUtils.register(
            p_334110_, OAK_BEES_0002_LEAF_LITTER, Feature.TREE, createOak().decorators(List.of(beehivedecorator, placeongrounddecorator, placeongrounddecorator1)).build()
        );
        FeatureUtils.register(p_334110_, OAK_BEES_002, Feature.TREE, createOak().decorators(List.of(beehivedecorator2)).build());
        FeatureUtils.register(p_334110_, OAK_BEES_005, Feature.TREE, createOak().decorators(List.of(beehivedecorator3)).build());
        FeatureUtils.register(p_334110_, BIRCH_BEES_0002, Feature.TREE, createBirch().decorators(List.of(beehivedecorator)).build());
        FeatureUtils.register(
            p_334110_, BIRCH_BEES_0002_LEAF_LITTER, Feature.TREE, createBirch().decorators(List.of(beehivedecorator, placeongrounddecorator, placeongrounddecorator1)).build()
        );
        FeatureUtils.register(p_334110_, BIRCH_BEES_002, Feature.TREE, createBirch().decorators(List.of(beehivedecorator2)).build());
        FeatureUtils.register(p_334110_, BIRCH_BEES_005, Feature.TREE, createBirch().decorators(List.of(beehivedecorator3)).build());
        FeatureUtils.register(
            p_334110_, FANCY_OAK_BEES_0002_LEAF_LITTER, Feature.TREE, createFancyOak().decorators(List.of(beehivedecorator, placeongrounddecorator, placeongrounddecorator1)).build()
        );
        FeatureUtils.register(p_334110_, FANCY_OAK_BEES_002, Feature.TREE, createFancyOak().decorators(List.of(beehivedecorator2)).build());
        FeatureUtils.register(p_334110_, FANCY_OAK_BEES_005, Feature.TREE, createFancyOak().decorators(List.of(beehivedecorator3)).build());
        FeatureUtils.register(p_334110_, FANCY_OAK_BEES, Feature.TREE, createFancyOak().decorators(List.of(beehivedecorator4)).build());
        FeatureUtils.register(
            p_334110_, OAK_LEAF_LITTER, Feature.TREE, createOak().decorators(ImmutableList.of(placeongrounddecorator, placeongrounddecorator1)).build()
        );
        FeatureUtils.register(
            p_334110_,
            DARK_OAK_LEAF_LITTER,
            Feature.TREE,
            createDarkOak().ignoreVines().decorators(ImmutableList.of(placeongrounddecorator, placeongrounddecorator1)).build()
        );
        FeatureUtils.register(
            p_334110_, BIRCH_LEAF_LITTER, Feature.TREE, createBirch().decorators(ImmutableList.of(placeongrounddecorator, placeongrounddecorator1)).build()
        );
        FeatureUtils.register(
            p_334110_, FANCY_OAK_LEAF_LITTER, Feature.TREE, createFancyOak().decorators(List.of(placeongrounddecorator, placeongrounddecorator1)).build()
        );
        FeatureUtils.register(p_334110_, FALLEN_OAK_TREE, Feature.FALLEN_TREE, createFallenOak().build());
        FeatureUtils.register(p_334110_, FALLEN_BIRCH_TREE, Feature.FALLEN_TREE, createFallenBirch(8).build());
        FeatureUtils.register(p_334110_, FALLEN_SUPER_BIRCH_TREE, Feature.FALLEN_TREE, createFallenBirch(15).build());
        FeatureUtils.register(p_334110_, FALLEN_JUNGLE_TREE, Feature.FALLEN_TREE, createFallenJungle().build());
        FeatureUtils.register(p_334110_, FALLEN_SPRUCE_TREE, Feature.FALLEN_TREE, createFallenSpruce().build());
    }
}