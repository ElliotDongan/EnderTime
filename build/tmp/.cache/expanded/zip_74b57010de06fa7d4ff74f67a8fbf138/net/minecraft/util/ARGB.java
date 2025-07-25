package net.minecraft.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ARGB {
    public static int alpha(int p_362339_) {
        return p_362339_ >>> 24;
    }

    public static int red(int p_363530_) {
        return p_363530_ >> 16 & 0xFF;
    }

    public static int green(int p_362707_) {
        return p_362707_ >> 8 & 0xFF;
    }

    public static int blue(int p_367010_) {
        return p_367010_ & 0xFF;
    }

    public static int color(int p_365053_, int p_365624_, int p_367179_, int p_364375_) {
        return p_365053_ << 24 | p_365624_ << 16 | p_367179_ << 8 | p_364375_;
    }

    public static int color(int p_368038_, int p_364189_, int p_366166_) {
        return color(255, p_368038_, p_364189_, p_366166_);
    }

    public static int color(Vec3 p_368690_) {
        return color(as8BitChannel((float)p_368690_.x()), as8BitChannel((float)p_368690_.y()), as8BitChannel((float)p_368690_.z()));
    }

    public static int multiply(int p_368908_, int p_362670_) {
        if (p_368908_ == -1) {
            return p_362670_;
        } else {
            return p_362670_ == -1
                ? p_368908_
                : color(
                    alpha(p_368908_) * alpha(p_362670_) / 255,
                    red(p_368908_) * red(p_362670_) / 255,
                    green(p_368908_) * green(p_362670_) / 255,
                    blue(p_368908_) * blue(p_362670_) / 255
                );
        }
    }

    public static int scaleRGB(int p_364590_, float p_365829_) {
        return scaleRGB(p_364590_, p_365829_, p_365829_, p_365829_);
    }

    public static int scaleRGB(int p_368386_, float p_366859_, float p_367328_, float p_364459_) {
        return color(
            alpha(p_368386_),
            Math.clamp((long)((int)(red(p_368386_) * p_366859_)), 0, 255),
            Math.clamp((long)((int)(green(p_368386_) * p_367328_)), 0, 255),
            Math.clamp((long)((int)(blue(p_368386_) * p_364459_)), 0, 255)
        );
    }

    public static int scaleRGB(int p_366038_, int p_368003_) {
        return color(
            alpha(p_366038_),
            Math.clamp((long)red(p_366038_) * p_368003_ / 255L, 0, 255),
            Math.clamp((long)green(p_366038_) * p_368003_ / 255L, 0, 255),
            Math.clamp((long)blue(p_366038_) * p_368003_ / 255L, 0, 255)
        );
    }

    public static int greyscale(int p_362330_) {
        int i = (int)(red(p_362330_) * 0.3F + green(p_362330_) * 0.59F + blue(p_362330_) * 0.11F);
        return color(i, i, i);
    }

    public static int lerp(float p_368280_, int p_363975_, int p_368594_) {
        int i = Mth.lerpInt(p_368280_, alpha(p_363975_), alpha(p_368594_));
        int j = Mth.lerpInt(p_368280_, red(p_363975_), red(p_368594_));
        int k = Mth.lerpInt(p_368280_, green(p_363975_), green(p_368594_));
        int l = Mth.lerpInt(p_368280_, blue(p_363975_), blue(p_368594_));
        return color(i, j, k, l);
    }

    public static int opaque(int p_363480_) {
        return p_363480_ | 0xFF000000;
    }

    public static int transparent(int p_366691_) {
        return p_366691_ & 16777215;
    }

    public static int color(int p_362407_, int p_368043_) {
        return p_362407_ << 24 | p_368043_ & 16777215;
    }

    public static int color(float p_407846_, int p_406600_) {
        return as8BitChannel(p_407846_) << 24 | p_406600_ & 16777215;
    }

    public static int white(float p_361606_) {
        return as8BitChannel(p_361606_) << 24 | 16777215;
    }

    public static int colorFromFloat(float p_365014_, float p_365331_, float p_361446_, float p_367224_) {
        return color(as8BitChannel(p_365014_), as8BitChannel(p_365331_), as8BitChannel(p_361446_), as8BitChannel(p_367224_));
    }

    public static Vector3f vector3fFromRGB24(int p_368966_) {
        float f = red(p_368966_) / 255.0F;
        float f1 = green(p_368966_) / 255.0F;
        float f2 = blue(p_368966_) / 255.0F;
        return new Vector3f(f, f1, f2);
    }

    public static int average(int p_368446_, int p_366831_) {
        return color(
            (alpha(p_368446_) + alpha(p_366831_)) / 2,
            (red(p_368446_) + red(p_366831_)) / 2,
            (green(p_368446_) + green(p_366831_)) / 2,
            (blue(p_368446_) + blue(p_366831_)) / 2
        );
    }

    public static int as8BitChannel(float p_367233_) {
        return Mth.floor(p_367233_ * 255.0F);
    }

    public static float alphaFloat(int p_376586_) {
        return from8BitChannel(alpha(p_376586_));
    }

    public static float redFloat(int p_375781_) {
        return from8BitChannel(red(p_375781_));
    }

    public static float greenFloat(int p_375888_) {
        return from8BitChannel(green(p_375888_));
    }

    public static float blueFloat(int p_377428_) {
        return from8BitChannel(blue(p_377428_));
    }

    private static float from8BitChannel(int p_370155_) {
        return p_370155_ / 255.0F;
    }

    public static int toABGR(int p_368147_) {
        return p_368147_ & -16711936 | (p_368147_ & 0xFF0000) >> 16 | (p_368147_ & 0xFF) << 16;
    }

    public static int fromABGR(int p_369336_) {
        return toABGR(p_369336_);
    }

    public static int setBrightness(int p_409846_, float p_408870_) {
        int i = red(p_409846_);
        int j = green(p_409846_);
        int k = blue(p_409846_);
        int l = alpha(p_409846_);
        int i1 = Math.max(Math.max(i, j), k);
        int j1 = Math.min(Math.min(i, j), k);
        float f = i1 - j1;
        float f1;
        if (i1 != 0) {
            f1 = f / i1;
        } else {
            f1 = 0.0F;
        }

        float f2;
        if (f1 == 0.0F) {
            f2 = 0.0F;
        } else {
            float f3 = (i1 - i) / f;
            float f4 = (i1 - j) / f;
            float f5 = (i1 - k) / f;
            if (i == i1) {
                f2 = f5 - f4;
            } else if (j == i1) {
                f2 = 2.0F + f3 - f5;
            } else {
                f2 = 4.0F + f4 - f3;
            }

            f2 /= 6.0F;
            if (f2 < 0.0F) {
                f2++;
            }
        }

        if (f1 == 0.0F) {
            i = j = k = Math.round(p_408870_ * 255.0F);
            return color(l, i, j, k);
        } else {
            float f8 = (f2 - (float)Math.floor(f2)) * 6.0F;
            float f9 = f8 - (float)Math.floor(f8);
            float f10 = p_408870_ * (1.0F - f1);
            float f6 = p_408870_ * (1.0F - f1 * f9);
            float f7 = p_408870_ * (1.0F - f1 * (1.0F - f9));
            switch ((int)f8) {
                case 0:
                    i = Math.round(p_408870_ * 255.0F);
                    j = Math.round(f7 * 255.0F);
                    k = Math.round(f10 * 255.0F);
                    break;
                case 1:
                    i = Math.round(f6 * 255.0F);
                    j = Math.round(p_408870_ * 255.0F);
                    k = Math.round(f10 * 255.0F);
                    break;
                case 2:
                    i = Math.round(f10 * 255.0F);
                    j = Math.round(p_408870_ * 255.0F);
                    k = Math.round(f7 * 255.0F);
                    break;
                case 3:
                    i = Math.round(f10 * 255.0F);
                    j = Math.round(f6 * 255.0F);
                    k = Math.round(p_408870_ * 255.0F);
                    break;
                case 4:
                    i = Math.round(f7 * 255.0F);
                    j = Math.round(f10 * 255.0F);
                    k = Math.round(p_408870_ * 255.0F);
                    break;
                case 5:
                    i = Math.round(p_408870_ * 255.0F);
                    j = Math.round(f10 * 255.0F);
                    k = Math.round(f6 * 255.0F);
            }

            return color(l, i, j, k);
        }
    }
}