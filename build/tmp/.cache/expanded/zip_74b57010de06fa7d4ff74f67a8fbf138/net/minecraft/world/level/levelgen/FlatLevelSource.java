package net.minecraft.world.level.levelgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class FlatLevelSource extends ChunkGenerator {
    public static final MapCodec<FlatLevelSource> CODEC = RecordCodecBuilder.mapCodec(
        p_255577_ -> p_255577_.group(FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(FlatLevelSource::settings))
            .apply(p_255577_, p_255577_.stable(FlatLevelSource::new))
    );
    private final FlatLevelGeneratorSettings settings;

    public FlatLevelSource(FlatLevelGeneratorSettings p_256337_) {
        super(new FixedBiomeSource(p_256337_.getBiome()), Util.memoize(p_256337_::adjustGenerationSettings));
        this.settings = p_256337_;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> p_256602_, RandomState p_255830_, long p_256355_) {
        Stream<Holder<StructureSet>> stream = this.settings
            .structureOverrides()
            .map(HolderSet::stream)
            .orElseGet(() -> p_256602_.listElements().map(p_255579_ -> (Holder<StructureSet>)p_255579_));
        return ChunkGeneratorStructureState.createForFlat(p_255830_, p_256355_, this.biomeSource, stream);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public FlatLevelGeneratorSettings settings() {
        return this.settings;
    }

    @Override
    public void buildSurface(WorldGenRegion p_224174_, StructureManager p_224175_, RandomState p_224176_, ChunkAccess p_224177_) {
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor p_158279_) {
        return p_158279_.getMinY() + Math.min(p_158279_.getHeight(), this.settings.getLayers().size());
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender p_224184_, RandomState p_224185_, StructureManager p_224186_, ChunkAccess p_224187_) {
        List<BlockState> list = this.settings.getLayers();
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        Heightmap heightmap = p_224187_.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmap1 = p_224187_.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        for (int i = 0; i < Math.min(p_224187_.getHeight(), list.size()); i++) {
            BlockState blockstate = list.get(i);
            if (blockstate != null) {
                int j = p_224187_.getMinY() + i;

                for (int k = 0; k < 16; k++) {
                    for (int l = 0; l < 16; l++) {
                        p_224187_.setBlockState(blockpos$mutableblockpos.set(k, j, l), blockstate);
                        heightmap.update(k, j, l, blockstate);
                        heightmap1.update(k, j, l, blockstate);
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(p_224187_);
    }

    @Override
    public int getBaseHeight(int p_224160_, int p_224161_, Heightmap.Types p_224162_, LevelHeightAccessor p_224163_, RandomState p_224164_) {
        List<BlockState> list = this.settings.getLayers();

        for (int i = Math.min(list.size() - 1, p_224163_.getMaxY()); i >= 0; i--) {
            BlockState blockstate = list.get(i);
            if (blockstate != null && p_224162_.isOpaque().test(blockstate)) {
                return p_224163_.getMinY() + i + 1;
            }
        }

        return p_224163_.getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int p_224155_, int p_224156_, LevelHeightAccessor p_224157_, RandomState p_224158_) {
        return new NoiseColumn(
            p_224157_.getMinY(),
            this.settings
                .getLayers()
                .stream()
                .limit(p_224157_.getHeight())
                .map(p_204549_ -> p_204549_ == null ? Blocks.AIR.defaultBlockState() : p_204549_)
                .toArray(BlockState[]::new)
        );
    }

    @Override
    public void addDebugScreenInfo(List<String> p_224179_, RandomState p_224180_, BlockPos p_224181_) {
    }

    @Override
    public void applyCarvers(
        WorldGenRegion p_224166_, long p_224167_, RandomState p_224168_, BiomeManager p_224169_, StructureManager p_224170_, ChunkAccess p_224171_
    ) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion p_188545_) {
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }
}