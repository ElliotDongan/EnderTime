package net.minecraft.client.resources.sounds;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BiomeAmbientSoundsHandler implements AmbientSoundHandler {
    private static final int LOOP_SOUND_CROSS_FADE_TIME = 40;
    private static final float SKY_MOOD_RECOVERY_RATE = 0.001F;
    private final LocalPlayer player;
    private final SoundManager soundManager;
    private final BiomeManager biomeManager;
    private final RandomSource random;
    private final Object2ObjectArrayMap<Biome, BiomeAmbientSoundsHandler.LoopSoundInstance> loopSounds = new Object2ObjectArrayMap<>();
    private Optional<AmbientMoodSettings> moodSettings = Optional.empty();
    private Optional<AmbientAdditionsSettings> additionsSettings = Optional.empty();
    private float moodiness;
    @Nullable
    private Biome previousBiome;

    public BiomeAmbientSoundsHandler(LocalPlayer p_119639_, SoundManager p_119640_, BiomeManager p_119641_) {
        this.random = p_119639_.level().getRandom();
        this.player = p_119639_;
        this.soundManager = p_119640_;
        this.biomeManager = p_119641_;
    }

    public float getMoodiness() {
        return this.moodiness;
    }

    @Override
    public void tick() {
        this.loopSounds.values().removeIf(AbstractTickableSoundInstance::isStopped);
        Biome biome = this.biomeManager.getNoiseBiomeAtPosition(this.player.getX(), this.player.getY(), this.player.getZ()).value();
        if (biome != this.previousBiome) {
            this.previousBiome = biome;
            this.moodSettings = biome.getAmbientMood();
            this.additionsSettings = biome.getAmbientAdditions();
            this.loopSounds.values().forEach(BiomeAmbientSoundsHandler.LoopSoundInstance::fadeOut);
            biome.getAmbientLoop().ifPresent(p_263342_ -> this.loopSounds.compute(biome, (p_405031_, p_405032_) -> {
                if (p_405032_ == null) {
                    p_405032_ = new BiomeAmbientSoundsHandler.LoopSoundInstance((SoundEvent)p_263342_.value());
                    this.soundManager.play(p_405032_);
                }

                p_405032_.fadeIn();
                return (BiomeAmbientSoundsHandler.LoopSoundInstance)p_405032_;
            }));
        }

        this.additionsSettings.ifPresent(p_405033_ -> {
            if (this.random.nextDouble() < p_405033_.getTickChance()) {
                this.soundManager.play(SimpleSoundInstance.forAmbientAddition(p_405033_.getSoundEvent().value()));
            }
        });
        this.moodSettings
            .ifPresent(
                p_274718_ -> {
                    Level level = this.player.level();
                    int i = p_274718_.getBlockSearchExtent() * 2 + 1;
                    BlockPos blockpos = BlockPos.containing(
                        this.player.getX() + this.random.nextInt(i) - p_274718_.getBlockSearchExtent(),
                        this.player.getEyeY() + this.random.nextInt(i) - p_274718_.getBlockSearchExtent(),
                        this.player.getZ() + this.random.nextInt(i) - p_274718_.getBlockSearchExtent()
                    );
                    int j = level.getBrightness(LightLayer.SKY, blockpos);
                    if (j > 0) {
                        this.moodiness -= j / 15.0F * 0.001F;
                    } else {
                        this.moodiness = this.moodiness - (float)(level.getBrightness(LightLayer.BLOCK, blockpos) - 1) / p_274718_.getTickDelay();
                    }

                    if (this.moodiness >= 1.0F) {
                        double d0 = blockpos.getX() + 0.5;
                        double d1 = blockpos.getY() + 0.5;
                        double d2 = blockpos.getZ() + 0.5;
                        double d3 = d0 - this.player.getX();
                        double d4 = d1 - this.player.getEyeY();
                        double d5 = d2 - this.player.getZ();
                        double d6 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
                        double d7 = d6 + p_274718_.getSoundPositionOffset();
                        SimpleSoundInstance simplesoundinstance = SimpleSoundInstance.forAmbientMood(
                            p_274718_.getSoundEvent().value(),
                            this.random,
                            this.player.getX() + d3 / d6 * d7,
                            this.player.getEyeY() + d4 / d6 * d7,
                            this.player.getZ() + d5 / d6 * d7
                        );
                        this.soundManager.play(simplesoundinstance);
                        this.moodiness = 0.0F;
                    } else {
                        this.moodiness = Math.max(this.moodiness, 0.0F);
                    }
                }
            );
    }

    @OnlyIn(Dist.CLIENT)
    public static class LoopSoundInstance extends AbstractTickableSoundInstance {
        private int fadeDirection;
        private int fade;

        public LoopSoundInstance(SoundEvent p_119658_) {
            super(p_119658_, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.relative = true;
        }

        @Override
        public void tick() {
            if (this.fade < 0) {
                this.stop();
            }

            this.fade = this.fade + this.fadeDirection;
            this.volume = Mth.clamp(this.fade / 40.0F, 0.0F, 1.0F);
        }

        public void fadeOut() {
            this.fade = Math.min(this.fade, 40);
            this.fadeDirection = -1;
        }

        public void fadeIn() {
            this.fade = Math.max(0, this.fade);
            this.fadeDirection = 1;
        }
    }
}