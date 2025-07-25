package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RidingMinecartSoundInstance extends AbstractTickableSoundInstance {
    private static final float VOLUME_MIN = 0.0F;
    private static final float VOLUME_MAX = 0.75F;
    private final Player player;
    private final AbstractMinecart minecart;
    private final boolean underwaterSound;

    public RidingMinecartSoundInstance(Player p_174940_, AbstractMinecart p_174941_, boolean p_174942_) {
        super(p_174942_ ? SoundEvents.MINECART_INSIDE_UNDERWATER : SoundEvents.MINECART_INSIDE, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.player = p_174940_;
        this.minecart = p_174941_;
        this.underwaterSound = p_174942_;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
    }

    @Override
    public boolean canPlaySound() {
        return !this.minecart.isSilent();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (this.minecart.isRemoved() || !this.player.isPassenger() || this.player.getVehicle() != this.minecart) {
            this.stop();
        } else if (this.underwaterSound != this.player.isUnderWater()) {
            this.volume = 0.0F;
        } else {
            float f = (float)this.minecart.getDeltaMovement().horizontalDistance();
            boolean flag = !this.minecart.isOnRails() && this.minecart.getBehavior() instanceof NewMinecartBehavior;
            if (f >= 0.01F && !flag) {
                this.volume = Mth.clampedLerp(0.0F, 0.75F, f);
            } else {
                this.volume = 0.0F;
            }
        }
    }
}