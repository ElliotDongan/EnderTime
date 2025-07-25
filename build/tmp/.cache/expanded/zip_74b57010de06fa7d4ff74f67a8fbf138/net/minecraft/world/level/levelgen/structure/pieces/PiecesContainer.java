package net.minecraft.world.level.levelgen.structure.pieces;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.slf4j.Logger;

public record PiecesContainer(List<StructurePiece> pieces) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation JIGSAW_RENAME = ResourceLocation.withDefaultNamespace("jigsaw");
    private static final Map<ResourceLocation, ResourceLocation> RENAMES = ImmutableMap.<ResourceLocation, ResourceLocation>builder()
        .put(ResourceLocation.withDefaultNamespace("nvi"), JIGSAW_RENAME)
        .put(ResourceLocation.withDefaultNamespace("pcp"), JIGSAW_RENAME)
        .put(ResourceLocation.withDefaultNamespace("bastionremnant"), JIGSAW_RENAME)
        .put(ResourceLocation.withDefaultNamespace("runtime"), JIGSAW_RENAME)
        .build();

    public PiecesContainer(final List<StructurePiece> pieces) {
        this.pieces = List.copyOf(pieces);
    }

    public boolean isEmpty() {
        return this.pieces.isEmpty();
    }

    public boolean isInsidePiece(BlockPos p_192752_) {
        for (StructurePiece structurepiece : this.pieces) {
            if (structurepiece.getBoundingBox().isInside(p_192752_)) {
                return true;
            }
        }

        return false;
    }

    public Tag save(StructurePieceSerializationContext p_192750_) {
        ListTag listtag = new ListTag();

        for (StructurePiece structurepiece : this.pieces) {
            listtag.add(structurepiece.createTag(p_192750_));
        }

        return listtag;
    }

    public static PiecesContainer load(ListTag p_192754_, StructurePieceSerializationContext p_192755_) {
        List<StructurePiece> list = Lists.newArrayList();

        for (int i = 0; i < p_192754_.size(); i++) {
            CompoundTag compoundtag = p_192754_.getCompoundOrEmpty(i);
            String s = compoundtag.getStringOr("id", "").toLowerCase(Locale.ROOT);
            ResourceLocation resourcelocation = ResourceLocation.parse(s);
            ResourceLocation resourcelocation1 = RENAMES.getOrDefault(resourcelocation, resourcelocation);
            StructurePieceType structurepiecetype = BuiltInRegistries.STRUCTURE_PIECE.getValue(resourcelocation1);
            if (structurepiecetype == null) {
                LOGGER.error("Unknown structure piece id: {}", resourcelocation1);
            } else {
                try {
                    StructurePiece structurepiece = structurepiecetype.load(p_192755_, compoundtag);
                    list.add(structurepiece);
                } catch (Exception exception) {
                    LOGGER.error("Exception loading structure piece with id {}", resourcelocation1, exception);
                }
            }
        }

        return new PiecesContainer(list);
    }

    public BoundingBox calculateBoundingBox() {
        return StructurePiece.createBoundingBox(this.pieces.stream());
    }
}