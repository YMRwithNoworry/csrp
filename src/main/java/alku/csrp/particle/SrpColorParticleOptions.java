package alku.csrp.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Payload for original particles whose constructor carried extra values: {@code ParticleDot},
 * {@code ParticleFlash}, {@code ParticleBlood}, {@code ParticleMultipleGore}, and
 * {@code ParticleRGBSmoke}. The RGB fields colour GCLOUD; {@code variant} carries the GSPLASH
 * texture family (0 assimilated, 1 pure, 2 primitive, 3 adapted, 4 vomit) or the BLOOD motion type.
 *
 * <p>1.12.2 sent these extras through its own particle packet; 26.3 sends a {@code ParticleOptions}
 * instance instead, so they travel as part of the particle payload.</p>
 */
public record SrpColorParticleOptions(ParticleType<SrpColorParticleOptions> type,
                                      int red, int green, int blue, int variant) implements ParticleOptions {
    public static MapCodec<SrpColorParticleOptions> codec(ParticleType<SrpColorParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("r").forGetter(SrpColorParticleOptions::red),
                Codec.INT.fieldOf("g").forGetter(SrpColorParticleOptions::green),
                Codec.INT.fieldOf("b").forGetter(SrpColorParticleOptions::blue),
                Codec.INT.fieldOf("variant").forGetter(SrpColorParticleOptions::variant)
        ).apply(instance, (red, green, blue, variant) ->
                new SrpColorParticleOptions(type, red, green, blue, variant)));
    }

    public static StreamCodec<? super RegistryFriendlyByteBuf, SrpColorParticleOptions> streamCodec(
            ParticleType<SrpColorParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, SrpColorParticleOptions::red,
                ByteBufCodecs.INT, SrpColorParticleOptions::green,
                ByteBufCodecs.INT, SrpColorParticleOptions::blue,
                ByteBufCodecs.INT, SrpColorParticleOptions::variant,
                (red, green, blue, variant) -> new SrpColorParticleOptions(type, red, green, blue, variant));
    }

    @Override
    public ParticleType<?> getType() {
        return type;
    }
}
