package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class SimpleParticleType extends ParticleType<SimpleParticleType> implements ParticleOptions {
    private final MapCodec<SimpleParticleType> codec = MapCodec.unit(this::getType);
    private final StreamCodec<RegistryFriendlyByteBuf, SimpleParticleType> streamCodec = StreamCodec.unit(this);

    public SimpleParticleType(boolean p_123837_) {
        super(p_123837_);
    }

    public SimpleParticleType getType() {
        return this;
    }

    @Override
    public MapCodec<SimpleParticleType> codec() {
        return this.codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SimpleParticleType> streamCodec() {
        return this.streamCodec;
    }
}