package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class LegacyStructureDataHandler {
    private static final Map<String, String> CURRENT_TO_LEGACY_MAP = Util.make(Maps.newHashMap(), p_71337_ -> {
        p_71337_.put("Village", "Village");
        p_71337_.put("Mineshaft", "Mineshaft");
        p_71337_.put("Mansion", "Mansion");
        p_71337_.put("Igloo", "Temple");
        p_71337_.put("Desert_Pyramid", "Temple");
        p_71337_.put("Jungle_Pyramid", "Temple");
        p_71337_.put("Swamp_Hut", "Temple");
        p_71337_.put("Stronghold", "Stronghold");
        p_71337_.put("Monument", "Monument");
        p_71337_.put("Fortress", "Fortress");
        p_71337_.put("EndCity", "EndCity");
    });
    private static final Map<String, String> LEGACY_TO_CURRENT_MAP = Util.make(Maps.newHashMap(), p_71325_ -> {
        p_71325_.put("Iglu", "Igloo");
        p_71325_.put("TeDP", "Desert_Pyramid");
        p_71325_.put("TeJP", "Jungle_Pyramid");
        p_71325_.put("TeSH", "Swamp_Hut");
    });
    private static final Set<String> OLD_STRUCTURE_REGISTRY_KEYS = Set.of(
        "pillager_outpost",
        "mineshaft",
        "mansion",
        "jungle_pyramid",
        "desert_pyramid",
        "igloo",
        "ruined_portal",
        "shipwreck",
        "swamp_hut",
        "stronghold",
        "monument",
        "ocean_ruin",
        "fortress",
        "endcity",
        "buried_treasure",
        "village",
        "nether_fossil",
        "bastion_remnant"
    );
    private final boolean hasLegacyData;
    private final Map<String, Long2ObjectMap<CompoundTag>> dataMap = Maps.newHashMap();
    private final Map<String, StructureFeatureIndexSavedData> indexMap = Maps.newHashMap();
    private final List<String> legacyKeys;
    private final List<String> currentKeys;

    public LegacyStructureDataHandler(@Nullable DimensionDataStorage p_71308_, List<String> p_71309_, List<String> p_71310_) {
        this.legacyKeys = p_71309_;
        this.currentKeys = p_71310_;
        this.populateCaches(p_71308_);
        boolean flag = false;

        for (String s : this.currentKeys) {
            flag |= this.dataMap.get(s) != null;
        }

        this.hasLegacyData = flag;
    }

    public void removeIndex(long p_71319_) {
        for (String s : this.legacyKeys) {
            StructureFeatureIndexSavedData structurefeatureindexsaveddata = this.indexMap.get(s);
            if (structurefeatureindexsaveddata != null && structurefeatureindexsaveddata.hasUnhandledIndex(p_71319_)) {
                structurefeatureindexsaveddata.removeIndex(p_71319_);
            }
        }
    }

    public CompoundTag updateFromLegacy(CompoundTag p_71327_) {
        CompoundTag compoundtag = p_71327_.getCompoundOrEmpty("Level");
        ChunkPos chunkpos = new ChunkPos(compoundtag.getIntOr("xPos", 0), compoundtag.getIntOr("zPos", 0));
        if (this.isUnhandledStructureStart(chunkpos.x, chunkpos.z)) {
            p_71327_ = this.updateStructureStart(p_71327_, chunkpos);
        }

        CompoundTag compoundtag1 = compoundtag.getCompoundOrEmpty("Structures");
        CompoundTag compoundtag2 = compoundtag1.getCompoundOrEmpty("References");

        for (String s : this.currentKeys) {
            boolean flag = OLD_STRUCTURE_REGISTRY_KEYS.contains(s.toLowerCase(Locale.ROOT));
            if (!compoundtag2.getLongArray(s).isPresent() && flag) {
                int i = 8;
                LongList longlist = new LongArrayList();

                for (int j = chunkpos.x - 8; j <= chunkpos.x + 8; j++) {
                    for (int k = chunkpos.z - 8; k <= chunkpos.z + 8; k++) {
                        if (this.hasLegacyStart(j, k, s)) {
                            longlist.add(ChunkPos.asLong(j, k));
                        }
                    }
                }

                compoundtag2.putLongArray(s, longlist.toLongArray());
            }
        }

        compoundtag1.put("References", compoundtag2);
        compoundtag.put("Structures", compoundtag1);
        p_71327_.put("Level", compoundtag);
        return p_71327_;
    }

    private boolean hasLegacyStart(int p_71315_, int p_71316_, String p_71317_) {
        return !this.hasLegacyData
            ? false
            : this.dataMap.get(p_71317_) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(p_71317_)).hasStartIndex(ChunkPos.asLong(p_71315_, p_71316_));
    }

    private boolean isUnhandledStructureStart(int p_71312_, int p_71313_) {
        if (!this.hasLegacyData) {
            return false;
        } else {
            for (String s : this.currentKeys) {
                if (this.dataMap.get(s) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(s)).hasUnhandledIndex(ChunkPos.asLong(p_71312_, p_71313_))) {
                    return true;
                }
            }

            return false;
        }
    }

    private CompoundTag updateStructureStart(CompoundTag p_71329_, ChunkPos p_71330_) {
        CompoundTag compoundtag = p_71329_.getCompoundOrEmpty("Level");
        CompoundTag compoundtag1 = compoundtag.getCompoundOrEmpty("Structures");
        CompoundTag compoundtag2 = compoundtag1.getCompoundOrEmpty("Starts");

        for (String s : this.currentKeys) {
            Long2ObjectMap<CompoundTag> long2objectmap = this.dataMap.get(s);
            if (long2objectmap != null) {
                long i = p_71330_.toLong();
                if (this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(s)).hasUnhandledIndex(i)) {
                    CompoundTag compoundtag3 = long2objectmap.get(i);
                    if (compoundtag3 != null) {
                        compoundtag2.put(s, compoundtag3);
                    }
                }
            }
        }

        compoundtag1.put("Starts", compoundtag2);
        compoundtag.put("Structures", compoundtag1);
        p_71329_.put("Level", compoundtag);
        return p_71329_;
    }

    private void populateCaches(@Nullable DimensionDataStorage p_71321_) {
        if (p_71321_ != null) {
            for (String s : this.legacyKeys) {
                CompoundTag compoundtag = new CompoundTag();

                try {
                    compoundtag = p_71321_.readTagFromDisk(s, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES, 1493).getCompoundOrEmpty("data").getCompoundOrEmpty("Features");
                    if (compoundtag.isEmpty()) {
                        continue;
                    }
                } catch (IOException ioexception) {
                }

                compoundtag.forEach(
                    (p_391048_, p_391049_) -> {
                        if (p_391049_ instanceof CompoundTag compoundtag1) {
                            long $$4x = ChunkPos.asLong(compoundtag1.getIntOr("ChunkX", 0), compoundtag1.getIntOr("ChunkZ", 0));
                            ListTag listtag = compoundtag1.getListOrEmpty("Children");
                            if (!listtag.isEmpty()) {
                                Optional<String> optional = listtag.getCompound(0).flatMap(p_391056_ -> p_391056_.getString("id"));
                                optional.map(LEGACY_TO_CURRENT_MAP::get).ifPresent(p_391058_ -> compoundtag1.putString("id", p_391058_));
                            }

                            compoundtag1.getString("id")
                                .ifPresent(
                                    p_391052_ -> this.dataMap.computeIfAbsent(p_391052_, p_71335_ -> new Long2ObjectOpenHashMap<>()).put($$4x, compoundtag1)
                                );
                        }
                    }
                );
                String s1 = s + "_index";
                StructureFeatureIndexSavedData structurefeatureindexsaveddata = p_71321_.computeIfAbsent(StructureFeatureIndexSavedData.type(s1));
                if (structurefeatureindexsaveddata.getAll().isEmpty()) {
                    StructureFeatureIndexSavedData structurefeatureindexsaveddata1 = new StructureFeatureIndexSavedData();
                    this.indexMap.put(s, structurefeatureindexsaveddata1);
                    compoundtag.forEach(
                        (p_391054_, p_391055_) -> {
                            if (p_391055_ instanceof CompoundTag compoundtag1) {
                                structurefeatureindexsaveddata1.addIndex(
                                    ChunkPos.asLong(compoundtag1.getIntOr("ChunkX", 0), compoundtag1.getIntOr("ChunkZ", 0))
                                );
                            }
                        }
                    );
                } else {
                    this.indexMap.put(s, structurefeatureindexsaveddata);
                }
            }
        }
    }

    public static LegacyStructureDataHandler getLegacyStructureHandler(ResourceKey<Level> p_71332_, @Nullable DimensionDataStorage p_71333_) {
        if (p_71332_ == Level.OVERWORLD) {
            return new LegacyStructureDataHandler(
                p_71333_,
                ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"),
                ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument")
            );
        } else if (p_71332_ == Level.NETHER) {
            List<String> list1 = ImmutableList.of("Fortress");
            return new LegacyStructureDataHandler(p_71333_, list1, list1);
        } else if (p_71332_ == Level.END) {
            List<String> list = ImmutableList.of("EndCity");
            return new LegacyStructureDataHandler(p_71333_, list, list);
        } else {
            throw new RuntimeException(String.format(Locale.ROOT, "Unknown dimension type : %s", p_71332_));
        }
    }
}