package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.DryFoliageColor;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class Biome {
    public static final Codec<Biome> DIRECT_CODEC = RecordCodecBuilder.create(
        p_220544_ -> p_220544_.group(
                Biome.ClimateSettings.CODEC.forGetter(p_151717_ -> p_151717_.modifiableBiomeInfo().getOriginalBiomeInfo().climateSettings()), // FORGE: Patch codec to ignore field redirect coremods.
                BiomeSpecialEffects.CODEC.fieldOf("effects").forGetter(p_220550_ -> p_220550_.modifiableBiomeInfo().getOriginalBiomeInfo().effects()), // FORGE: Patch codec to ignore field redirect coremods.
                BiomeGenerationSettings.CODEC.forGetter(p_220548_ -> p_220548_.generationSettings),
                MobSpawnSettings.CODEC.forGetter(p_220546_ -> p_220546_.mobSettings)
            )
            .apply(p_220544_, Biome::new)
    );
    public static final Codec<Biome> NETWORK_CODEC = RecordCodecBuilder.create(
        p_220540_ -> p_220540_.group(
                Biome.ClimateSettings.CODEC.forGetter(p_220542_ -> p_220542_.climateSettings),
                BiomeSpecialEffects.CODEC.fieldOf("effects").forGetter(p_220538_ -> p_220538_.specialEffects)
            )
            .apply(p_220540_, (p_220535_, p_220536_) -> new Biome(p_220535_, p_220536_, BiomeGenerationSettings.EMPTY, MobSpawnSettings.EMPTY))
    );
    public static final Codec<Holder<Biome>> CODEC = RegistryFileCodec.create(Registries.BIOME, DIRECT_CODEC);
    public static final Codec<HolderSet<Biome>> LIST_CODEC = RegistryCodecs.homogeneousList(Registries.BIOME, DIRECT_CODEC);
    private static final PerlinSimplexNoise TEMPERATURE_NOISE = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(1234L)), ImmutableList.of(0));
    static final PerlinSimplexNoise FROZEN_TEMPERATURE_NOISE = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(3456L)), ImmutableList.of(-2, -1, 0));
    @Deprecated(
        forRemoval = true
    )
    public static final PerlinSimplexNoise BIOME_INFO_NOISE = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(2345L)), ImmutableList.of(0));
    private static final int TEMPERATURE_CACHE_SIZE = 1024;
    /** @deprecated Redirected to {@link #getModifiedClimateSettings()} by field_to_method.js */
    private final Biome.ClimateSettings climateSettings;
    private final BiomeGenerationSettings generationSettings;
    /** @deprecated Redirected to {@link #getModifiedSpecialEffects()} by field_to_method.js */
    private final MobSpawnSettings mobSettings;
    private final BiomeSpecialEffects specialEffects;
    private final ThreadLocal<Long2FloatLinkedOpenHashMap> temperatureCache = ThreadLocal.withInitial(() -> Util.make(() -> {
        Long2FloatLinkedOpenHashMap long2floatlinkedopenhashmap = new Long2FloatLinkedOpenHashMap(1024, 0.25F) {
            @Override
            protected void rehash(int p_47580_) {
            }
        };
        long2floatlinkedopenhashmap.defaultReturnValue(Float.NaN);
        return long2floatlinkedopenhashmap;
    }));
    private final net.minecraftforge.common.world.ModifiableBiomeInfo modifiableBiomeInfo;

    Biome(Biome.ClimateSettings p_220530_, BiomeSpecialEffects p_220531_, BiomeGenerationSettings p_220532_, MobSpawnSettings p_220533_) {
        this.climateSettings = p_220530_;
        this.generationSettings = p_220532_;
        this.mobSettings = p_220533_;
        this.specialEffects = p_220531_;
        this.modifiableBiomeInfo = new net.minecraftforge.common.world.ModifiableBiomeInfo(new net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo(p_220530_, p_220531_, p_220532_, p_220533_)); // FORGE: cache original biome info on construction so we can bypass our field read coremods where necessary
    }

    public int getSkyColor() {
        return this.specialEffects.getSkyColor();
    }

    public MobSpawnSettings getMobSettings() {
        return this.modifiableBiomeInfo().get().mobSpawnSettings();
    }

    public boolean hasPrecipitation() {
        return this.climateSettings.hasPrecipitation();
    }

    public Biome.Precipitation getPrecipitationAt(BlockPos p_265163_, int p_366614_) {
        if (!this.hasPrecipitation()) {
            return Biome.Precipitation.NONE;
        } else {
            return this.coldEnoughToSnow(p_265163_, p_366614_) ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN;
        }
    }

    private float getHeightAdjustedTemperature(BlockPos p_47529_, int p_368747_) {
        float f = this.climateSettings.temperatureModifier.modifyTemperature(p_47529_, this.getBaseTemperature());
        int i = p_368747_ + 17;
        if (p_47529_.getY() > i) {
            float f1 = (float)(TEMPERATURE_NOISE.getValue(p_47529_.getX() / 8.0F, p_47529_.getZ() / 8.0F, false) * 8.0);
            return f - (f1 + p_47529_.getY() - i) * 0.05F / 40.0F;
        } else {
            return f;
        }
    }

    @Deprecated
    private float getTemperature(BlockPos p_47506_, int p_365043_) {
        long i = p_47506_.asLong();
        Long2FloatLinkedOpenHashMap long2floatlinkedopenhashmap = this.temperatureCache.get();
        float f = long2floatlinkedopenhashmap.get(i);
        if (!Float.isNaN(f)) {
            return f;
        } else {
            float f1 = this.getHeightAdjustedTemperature(p_47506_, p_365043_);
            if (long2floatlinkedopenhashmap.size() == 1024) {
                long2floatlinkedopenhashmap.removeFirstFloat();
            }

            long2floatlinkedopenhashmap.put(i, f1);
            return f1;
        }
    }

    public boolean shouldFreeze(LevelReader p_47478_, BlockPos p_47479_) {
        return this.shouldFreeze(p_47478_, p_47479_, true);
    }

    public boolean shouldFreeze(LevelReader p_47481_, BlockPos p_47482_, boolean p_47483_) {
        if (this.warmEnoughToRain(p_47482_, p_47481_.getSeaLevel())) {
            return false;
        } else {
            if (p_47481_.isInsideBuildHeight(p_47482_.getY()) && p_47481_.getBrightness(LightLayer.BLOCK, p_47482_) < 10) {
                BlockState blockstate = p_47481_.getBlockState(p_47482_);
                FluidState fluidstate = p_47481_.getFluidState(p_47482_);
                if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
                    if (!p_47483_) {
                        return true;
                    }

                    boolean flag = p_47481_.isWaterAt(p_47482_.west())
                        && p_47481_.isWaterAt(p_47482_.east())
                        && p_47481_.isWaterAt(p_47482_.north())
                        && p_47481_.isWaterAt(p_47482_.south());
                    if (!flag) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public boolean coldEnoughToSnow(BlockPos p_198905_, int p_362496_) {
        return !this.warmEnoughToRain(p_198905_, p_362496_);
    }

    public boolean warmEnoughToRain(BlockPos p_198907_, int p_362136_) {
        return this.getTemperature(p_198907_, p_362136_) >= 0.15F;
    }

    public boolean shouldMeltFrozenOceanIcebergSlightly(BlockPos p_198909_, int p_365025_) {
        return this.getTemperature(p_198909_, p_365025_) > 0.1F;
    }

    public boolean shouldSnow(LevelReader p_47520_, BlockPos p_47521_) {
        if (this.warmEnoughToRain(p_47521_, p_47520_.getSeaLevel())) {
            return false;
        } else {
            if (p_47520_.isInsideBuildHeight(p_47521_.getY()) && p_47520_.getBrightness(LightLayer.BLOCK, p_47521_) < 10) {
                BlockState blockstate = p_47520_.getBlockState(p_47521_);
                if ((blockstate.isAir() || blockstate.is(Blocks.SNOW)) && Blocks.SNOW.defaultBlockState().canSurvive(p_47520_, p_47521_)) {
                    return true;
                }
            }

            return false;
        }
    }

    public BiomeGenerationSettings getGenerationSettings() {
        return this.modifiableBiomeInfo().get().generationSettings();
    }

    public int getFogColor() {
        return this.specialEffects.getFogColor();
    }

    public int getGrassColor(double p_47465_, double p_47466_) {
        int i = this.getBaseGrassColor();
        return this.specialEffects.getGrassColorModifier().modifyColor(p_47465_, p_47466_, i);
    }

    private int getBaseGrassColor() {
        Optional<Integer> optional = this.specialEffects.getGrassColorOverride();
        return optional.isPresent() ? optional.get() : this.getGrassColorFromTexture();
    }

    private int getGrassColorFromTexture() {
        double d0 = Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
        double d1 = Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
        return GrassColor.get(d0, d1);
    }

    public int getFoliageColor() {
        return this.specialEffects.getFoliageColorOverride().orElseGet(this::getFoliageColorFromTexture);
    }

    private int getFoliageColorFromTexture() {
        double d0 = Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
        double d1 = Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
        return FoliageColor.get(d0, d1);
    }

    public int getDryFoliageColor() {
        return this.specialEffects.getDryFoliageColorOverride().orElseGet(this::getDryFoliageColorFromTexture);
    }

    private int getDryFoliageColorFromTexture() {
        double d0 = Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
        double d1 = Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
        return DryFoliageColor.get(d0, d1);
    }

    public float getBaseTemperature() {
        return this.climateSettings.temperature;
    }

    public BiomeSpecialEffects getSpecialEffects() {
        return this.specialEffects;
    }

    public int getWaterColor() {
        return this.specialEffects.getWaterColor();
    }

    public int getWaterFogColor() {
        return this.specialEffects.getWaterFogColor();
    }

    public Optional<AmbientParticleSettings> getAmbientParticle() {
        return this.specialEffects.getAmbientParticleSettings();
    }

    public Optional<Holder<SoundEvent>> getAmbientLoop() {
        return this.specialEffects.getAmbientLoopSoundEvent();
    }

    public Optional<AmbientMoodSettings> getAmbientMood() {
        return this.specialEffects.getAmbientMoodSettings();
    }

    public Optional<AmbientAdditionsSettings> getAmbientAdditions() {
        return this.specialEffects.getAmbientAdditionsSettings();
    }

    public Optional<WeightedList<Music>> getBackgroundMusic() {
        return this.specialEffects.getBackgroundMusic();
    }

    public float getBackgroundMusicVolume() {
        return this.specialEffects.getBackgroundMusicVolume();
    }

    /**
     * {@return Cache of original biome data and biome data modified by biome modifiers}
     * Modified biome data is set by server after datapacks and serverconfigs load.
     * Climate and effects field reads are coremodded to redirect to this.
     **/
    public net.minecraftforge.common.world.ModifiableBiomeInfo modifiableBiomeInfo() {
        return this.modifiableBiomeInfo;
    }

    /**
     * {@return The biome's climate settings, with modifications if called after modifiers are applied in server init.}
     * Note: This method is called by our field_to_method coremod. Which redirects {@link #climateSettings} to this method at load time.
     */
    public ClimateSettings getModifiedClimateSettings() {
        return this.modifiableBiomeInfo().get().climateSettings();
    }

    /**
     * {@return The biome's client effects, with modifications if called after modifiers are applied in server init.}
     * Note: This methods is called by our field_to_method coremod. Which redirects {@link #specialEffects} to this method at load time.
     */
    public BiomeSpecialEffects getModifiedSpecialEffects() {
        return this.modifiableBiomeInfo().get().effects();
    }

    public static class BiomeBuilder {
        private boolean hasPrecipitation = true;
        @Nullable
        private Float temperature;
        private Biome.TemperatureModifier temperatureModifier = Biome.TemperatureModifier.NONE;
        @Nullable
        private Float downfall;
        @Nullable
        private BiomeSpecialEffects specialEffects;
        @Nullable
        private MobSpawnSettings mobSpawnSettings;
        @Nullable
        private BiomeGenerationSettings generationSettings;

        public Biome.BiomeBuilder hasPrecipitation(boolean p_265480_) {
            this.hasPrecipitation = p_265480_;
            return this;
        }

        public Biome.BiomeBuilder temperature(float p_47610_) {
            this.temperature = p_47610_;
            return this;
        }

        public Biome.BiomeBuilder downfall(float p_47612_) {
            this.downfall = p_47612_;
            return this;
        }

        public Biome.BiomeBuilder specialEffects(BiomeSpecialEffects p_47604_) {
            this.specialEffects = p_47604_;
            return this;
        }

        public Biome.BiomeBuilder mobSpawnSettings(MobSpawnSettings p_47606_) {
            this.mobSpawnSettings = p_47606_;
            return this;
        }

        public Biome.BiomeBuilder generationSettings(BiomeGenerationSettings p_47602_) {
            this.generationSettings = p_47602_;
            return this;
        }

        public Biome.BiomeBuilder temperatureAdjustment(Biome.TemperatureModifier p_47600_) {
            this.temperatureModifier = p_47600_;
            return this;
        }

        public Biome build() {
            if (this.temperature != null && this.downfall != null && this.specialEffects != null && this.mobSpawnSettings != null && this.generationSettings != null) {
                return new Biome(
                    new Biome.ClimateSettings(this.hasPrecipitation, this.temperature, this.temperatureModifier, this.downfall), this.specialEffects, this.generationSettings, this.mobSpawnSettings
                );
            } else {
                throw new IllegalStateException("You are missing parameters to build a proper biome\n" + this);
            }
        }

        @Override
        public String toString() {
            return "BiomeBuilder{\nhasPrecipitation="
                + this.hasPrecipitation
                + ",\ntemperature="
                + this.temperature
                + ",\ntemperatureModifier="
                + this.temperatureModifier
                + ",\ndownfall="
                + this.downfall
                + ",\nspecialEffects="
                + this.specialEffects
                + ",\nmobSpawnSettings="
                + this.mobSpawnSettings
                + ",\ngenerationSettings="
                + this.generationSettings
                + ",\n}";
        }
    }

    public record ClimateSettings(boolean hasPrecipitation, float temperature, Biome.TemperatureModifier temperatureModifier, float downfall) {
        public static final MapCodec<Biome.ClimateSettings> CODEC = RecordCodecBuilder.mapCodec(
            p_264995_ -> p_264995_.group(
                    Codec.BOOL.fieldOf("has_precipitation").forGetter(p_264996_ -> p_264996_.hasPrecipitation),
                    Codec.FLOAT.fieldOf("temperature").forGetter(p_151737_ -> p_151737_.temperature),
                    Biome.TemperatureModifier.CODEC
                        .optionalFieldOf("temperature_modifier", Biome.TemperatureModifier.NONE)
                        .forGetter(p_151735_ -> p_151735_.temperatureModifier),
                    Codec.FLOAT.fieldOf("downfall").forGetter(p_151733_ -> p_151733_.downfall)
                )
                .apply(p_264995_, Biome.ClimateSettings::new)
        );
    }

    public static enum Precipitation implements StringRepresentable {
        NONE("none"),
        RAIN("rain"),
        SNOW("snow");

        public static final Codec<Biome.Precipitation> CODEC = StringRepresentable.fromEnum(Biome.Precipitation::values);
        private final String name;

        private Precipitation(final String p_311702_) {
            this.name = p_311702_;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static enum TemperatureModifier implements StringRepresentable {
        NONE("none") {
            @Override
            public float modifyTemperature(BlockPos p_47767_, float p_47768_) {
                return p_47768_;
            }
        },
        FROZEN("frozen") {
            @Override
            public float modifyTemperature(BlockPos p_47774_, float p_47775_) {
                double d0 = Biome.FROZEN_TEMPERATURE_NOISE.getValue(p_47774_.getX() * 0.05, p_47774_.getZ() * 0.05, false) * 7.0;
                double d1 = Biome.BIOME_INFO_NOISE.getValue(p_47774_.getX() * 0.2, p_47774_.getZ() * 0.2, false);
                double d2 = d0 + d1;
                if (d2 < 0.3) {
                    double d3 = Biome.BIOME_INFO_NOISE.getValue(p_47774_.getX() * 0.09, p_47774_.getZ() * 0.09, false);
                    if (d3 < 0.8) {
                        return 0.2F;
                    }
                }

                return p_47775_;
            }
        };

        private final String name;
        public static final Codec<Biome.TemperatureModifier> CODEC = StringRepresentable.fromEnum(Biome.TemperatureModifier::values);

        public abstract float modifyTemperature(BlockPos p_47754_, float p_47755_);

        TemperatureModifier(final String p_47745_) {
            this.name = p_47745_;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
