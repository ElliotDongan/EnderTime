package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public class WorldPresetTags {
    public static final TagKey<WorldPreset> NORMAL = create("normal");
    public static final TagKey<WorldPreset> EXTENDED = create("extended");

    private WorldPresetTags() {
    }

    private static TagKey<WorldPreset> create(String p_216058_) {
        return TagKey.create(Registries.WORLD_PRESET, ResourceLocation.withDefaultNamespace(p_216058_));
    }

    public static TagKey<WorldPreset> create(String namepsace, String path) {
        return create(ResourceLocation.fromNamespaceAndPath(namepsace, path));
    }

    public static TagKey<WorldPreset> create(ResourceLocation name) {
        return TagKey.create(Registries.WORLD_PRESET, name);
    }
}
