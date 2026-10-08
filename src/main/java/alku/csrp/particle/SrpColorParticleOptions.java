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
 * Coloured particle payload for the original {@code ParticleDot}, {@code ParticleFlash} and
 * {@code ParticleBlood}, which took their {@code (r, g, b)} or variant index as extra constructor
 * arguments ({@code ParticleSpawner#spawnParticle}).
 *
 * <p>1.12.2 sent those extras through its own particle packet; 26.3 sends a {@code ParticleOptions}
 * instance instead, so the values travel as part of the payload. The {@code variant} field carries
 * the original {@code ParticleBlood} type (0 falling, 1 rising).</p>
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
