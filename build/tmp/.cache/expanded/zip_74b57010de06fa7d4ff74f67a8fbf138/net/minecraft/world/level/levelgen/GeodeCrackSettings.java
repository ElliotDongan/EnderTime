package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;

public class GeodeCrackSettings {
    public static final Codec<GeodeCrackSettings> CODEC = RecordCodecBuilder.create(
        p_158334_ -> p_158334_.group(
                GeodeConfiguration.CHANCE_RANGE.fieldOf("generate_crack_chance").orElse(1.0).forGetter(p_158340_ -> p_158340_.generateCrackChance),
                Codec.doubleRange(0.0, 5.0).fieldOf("base_crack_size").orElse(2.0).forGetter(p_158338_ -> p_158338_.baseCrackSize),
                Codec.intRange(0, 10).fieldOf("crack_point_offset").orElse(2).forGetter(p_158336_ -> p_158336_.crackPointOffset)
            )
            .apply(p_158334_, GeodeCrackSettings::new)
    );
    public final double generateCrackChance;
    public final double baseCrackSize;
    public final int crackPointOffset;

    public GeodeCrackSettings(double p_158330_, double p_158331_, int p_158332_) {
        this.generateCrackChance = p_158330_;
        this.baseCrackSize = p_158331_;
        this.crackPointOffset = p_158332_;
    }
}