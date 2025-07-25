package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.commons.lang3.mutable.MutableInt;

public class FossilFeature extends Feature<FossilFeatureConfiguration> {
    public FossilFeature(Codec<FossilFeatureConfiguration> p_65851_) {
        super(p_65851_);
    }

    @Override
    public boolean place(FeaturePlaceContext<FossilFeatureConfiguration> p_159789_) {
        RandomSource randomsource = p_159789_.random();
        WorldGenLevel worldgenlevel = p_159789_.level();
        BlockPos blockpos = p_159789_.origin();
        Rotation rotation = Rotation.getRandom(randomsource);
        FossilFeatureConfiguration fossilfeatureconfiguration = p_159789_.config();
        int i = randomsource.nextInt(fossilfeatureconfiguration.fossilStructures.size());
        StructureTemplateManager structuretemplatemanager = worldgenlevel.getLevel().getServer().getStructureManager();
        StructureTemplate structuretemplate = structuretemplatemanager.getOrCreate(fossilfeatureconfiguration.fossilStructures.get(i));
        StructureTemplate structuretemplate1 = structuretemplatemanager.getOrCreate(fossilfeatureconfiguration.overlayStructures.get(i));
        ChunkPos chunkpos = new ChunkPos(blockpos);
        BoundingBox boundingbox = new BoundingBox(
            chunkpos.getMinBlockX() - 16,
            worldgenlevel.getMinY(),
            chunkpos.getMinBlockZ() - 16,
            chunkpos.getMaxBlockX() + 16,
            worldgenlevel.getMaxY(),
            chunkpos.getMaxBlockZ() + 16
        );
        StructurePlaceSettings structureplacesettings = new StructurePlaceSettings().setRotation(rotation).setBoundingBox(boundingbox).setRandom(randomsource);
        Vec3i vec3i = structuretemplate.getSize(rotation);
        BlockPos blockpos1 = blockpos.offset(-vec3i.getX() / 2, 0, -vec3i.getZ() / 2);
        int j = blockpos.getY();

        for (int k = 0; k < vec3i.getX(); k++) {
            for (int l = 0; l < vec3i.getZ(); l++) {
                j = Math.min(j, worldgenlevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, blockpos1.getX() + k, blockpos1.getZ() + l));
            }
        }

        int i1 = Math.max(j - 15 - randomsource.nextInt(10), worldgenlevel.getMinY() + 10);
        BlockPos blockpos2 = structuretemplate.getZeroPositionWithTransform(blockpos1.atY(i1), Mirror.NONE, rotation);
        if (countEmptyCorners(worldgenlevel, structuretemplate.getBoundingBox(structureplacesettings, blockpos2)) > fossilfeatureconfiguration.maxEmptyCornersAllowed) {
            return false;
        } else {
            structureplacesettings.clearProcessors();
            fossilfeatureconfiguration.fossilProcessors.value().list().forEach(structureplacesettings::addProcessor);
            structuretemplate.placeInWorld(worldgenlevel, blockpos2, blockpos2, structureplacesettings, randomsource, 260);
            structureplacesettings.clearProcessors();
            fossilfeatureconfiguration.overlayProcessors.value().list().forEach(structureplacesettings::addProcessor);
            structuretemplate1.placeInWorld(worldgenlevel, blockpos2, blockpos2, structureplacesettings, randomsource, 260);
            return true;
        }
    }

    private static int countEmptyCorners(WorldGenLevel p_159782_, BoundingBox p_159783_) {
        MutableInt mutableint = new MutableInt(0);
        p_159783_.forAllCorners(p_360607_ -> {
            BlockState blockstate = p_159782_.getBlockState(p_360607_);
            if (blockstate.isAir() || blockstate.is(Blocks.LAVA) || blockstate.is(Blocks.WATER)) {
                mutableint.add(1);
            }
        });
        return mutableint.getValue();
    }
}