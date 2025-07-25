package net.minecraft.util.random;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.util.ExtraCodecs;
import org.slf4j.Logger;

public record Weighted<T>(T value, int weight) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public Weighted(T value, int weight) {
        if (weight < 0) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Weight should be >= 0"));
        } else {
            if (weight == 0 && SharedConstants.IS_RUNNING_IN_IDE) {
                LOGGER.warn("Found 0 weight, make sure this is intentional!");
            }

            this.value = value;
            this.weight = weight;
        }
    }

    public static <E> Codec<Weighted<E>> codec(Codec<E> p_396145_) {
        return codec(p_396145_.fieldOf("data"));
    }

    public static <E> Codec<Weighted<E>> codec(MapCodec<E> p_391567_) {
        return RecordCodecBuilder.create(
            p_391586_ -> p_391586_.group(
                    p_391567_.forGetter((Function<Weighted<E>, E>)(Weighted::value)),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("weight").forGetter(Weighted::weight)
                )
                .apply(p_391586_, (BiFunction<E, Integer, Weighted<E>>)(Weighted::new))
        );
    }

    public <U> Weighted<U> map(Function<T, U> p_394740_) {
        return new Weighted<>(p_394740_.apply(this.value()), this.weight);
    }
}