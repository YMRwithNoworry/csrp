package alku.csrp.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * {@link ParticleType} for the coloured originals ({@code ParticleDot} / {@code ParticleFlash} /
 * {@code ParticleBlood}); the {@code (r, g, b, variant)} payload lives in
 * {@link SrpColorParticleOptions}.
 */
public class SrpColorParticleType extends ParticleType<SrpColorParticleOptions> {
    public SrpColorParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<SrpColorParticleOptions> codec() {
        return SrpColorParticleOptions.codec(this);
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, SrpColorParticleOptions> streamCodec() {
        return SrpColorParticleOptions.streamCodec(this);
    }
}
