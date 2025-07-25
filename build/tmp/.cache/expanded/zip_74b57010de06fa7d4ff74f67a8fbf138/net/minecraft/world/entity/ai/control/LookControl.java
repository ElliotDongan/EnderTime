package net.minecraft.world.entity.ai.control;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class LookControl implements Control {
    protected final Mob mob;
    protected float yMaxRotSpeed;
    protected float xMaxRotAngle;
    protected int lookAtCooldown;
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;

    public LookControl(Mob p_24945_) {
        this.mob = p_24945_;
    }

    public void setLookAt(Vec3 p_24965_) {
        this.setLookAt(p_24965_.x, p_24965_.y, p_24965_.z);
    }

    public void setLookAt(Entity p_148052_) {
        this.setLookAt(p_148052_.getX(), p_148052_.getEyeY(), p_148052_.getZ());
    }

    public void setLookAt(Entity p_24961_, float p_24962_, float p_24963_) {
        this.setLookAt(p_24961_.getX(), p_24961_.getEyeY(), p_24961_.getZ(), p_24962_, p_24963_);
    }

    public void setLookAt(double p_24947_, double p_24948_, double p_24949_) {
        this.setLookAt(p_24947_, p_24948_, p_24949_, this.mob.getHeadRotSpeed(), this.mob.getMaxHeadXRot());
    }

    public void setLookAt(double p_24951_, double p_24952_, double p_24953_, float p_24954_, float p_24955_) {
        this.wantedX = p_24951_;
        this.wantedY = p_24952_;
        this.wantedZ = p_24953_;
        this.yMaxRotSpeed = p_24954_;
        this.xMaxRotAngle = p_24955_;
        this.lookAtCooldown = 2;
    }

    public void tick() {
        if (this.resetXRotOnTick()) {
            this.mob.setXRot(0.0F);
        }

        if (this.lookAtCooldown > 0) {
            this.lookAtCooldown--;
            this.getYRotD().ifPresent(p_359087_ -> this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, p_359087_, this.yMaxRotSpeed));
            this.getXRotD().ifPresent(p_405414_ -> this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), p_405414_, this.xMaxRotAngle)));
        } else {
            this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, 10.0F);
        }

        this.clampHeadRotationToBody();
    }

    protected void clampHeadRotationToBody() {
        if (!this.mob.getNavigation().isDone()) {
            this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, this.mob.getMaxHeadYRot());
        }
    }

    protected boolean resetXRotOnTick() {
        return true;
    }

    public boolean isLookingAtTarget() {
        return this.lookAtCooldown > 0;
    }

    public double getWantedX() {
        return this.wantedX;
    }

    public double getWantedY() {
        return this.wantedY;
    }

    public double getWantedZ() {
        return this.wantedZ;
    }

    protected Optional<Float> getXRotD() {
        double d0 = this.wantedX - this.mob.getX();
        double d1 = this.wantedY - this.mob.getEyeY();
        double d2 = this.wantedZ - this.mob.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        return !(Math.abs(d1) > 1.0E-5F) && !(Math.abs(d3) > 1.0E-5F)
            ? Optional.empty()
            : Optional.of((float)(-(Mth.atan2(d1, d3) * 180.0F / (float)Math.PI)));
    }

    protected Optional<Float> getYRotD() {
        double d0 = this.wantedX - this.mob.getX();
        double d1 = this.wantedZ - this.mob.getZ();
        return !(Math.abs(d1) > 1.0E-5F) && !(Math.abs(d0) > 1.0E-5F)
            ? Optional.empty()
            : Optional.of((float)(Mth.atan2(d1, d0) * 180.0F / (float)Math.PI) - 90.0F);
    }
}