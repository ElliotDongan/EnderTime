package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;

public class FlatLevelGeneratorPresetTags {
    public static final TagKey<FlatLevelGeneratorPreset> VISIBLE = create("visible");

    private FlatLevelGeneratorPresetTags() {
    }

    private static TagKey<FlatLevelGeneratorPreset> create(String p_215852_) {
        return TagKey.create(Registries.FLAT_LEVEL_GENERATOR_PRESET, ResourceLocation.withDefaultNamespace(p_215852_));
    }

    public static TagKey<FlatLevelGeneratorPreset> create(String namepsace, String path) {
        return create(ResourceLocation.fromNamespaceAndPath(namepsace, path));
    }

    public static TagKey<FlatLevelGeneratorPreset> create(ResourceLocation name) {
        return TagKey.create(Registries.FLAT_LEVEL_GENERATOR_PRESET, name);
    }
}
