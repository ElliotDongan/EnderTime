package net.minecraft.client.resources.metadata.language;

import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record LanguageMetadataSection(Map<String, LanguageInfo> languages) {
    public static final Codec<String> LANGUAGE_CODE_CODEC = Codec.string(1, 16);
    public static final Codec<LanguageMetadataSection> CODEC = Codec.unboundedMap(LANGUAGE_CODE_CODEC, LanguageInfo.CODEC)
        .xmap(LanguageMetadataSection::new, LanguageMetadataSection::languages);
    public static final MetadataSectionType<LanguageMetadataSection> TYPE = new MetadataSectionType<>("language", CODEC);
}