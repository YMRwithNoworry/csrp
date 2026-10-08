package alku.csrp.client.particle;

import alku.csrp.particle.SrpColorParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

/** Port of {@code client/particle/ParticleDot} (out109): a coloured dot that fades in and out. */
public final class DotParticle extends SingleQuadParticle {
    private DotParticle(ClientLevel level, double x, double y, double z,
                        double velocityX, double velocityY, double velocityZ,
                        SpriteSet sprites, int red, int green, int blue) {
        super(level, x, y, z, velocityX, velocityY, velocityZ, sprites.get(0, 0));
        quadSize = 2.0F;
        lifetime = 40;
        alpha = 0.0F;
        setColor(red / 255.0F, green / 255.0F, blue / 255.0F);
        if (velocityY != 0.0D || velocityX != 0.0D || velocityZ != 0.0D) {
            xd = velocityX;
            yd = velocityY;
            zd = velocityZ;
        }
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        int currentAge = age++;
        float maxAge = lifetime;
        y = yo + currentAge * 0.01D;
        if (currentAge > 20) {
            alpha = (maxAge - currentAge) / maxAge;
        } else if (currentAge <= 5 && currentAge != 1) {
            alpha = (currentAge - maxAge) / maxAge;
        } else {
            alpha = 1.0F;
        }
        if (age++ >= lifetime) {
            remove();
        }
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SrpColorParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SrpColorParticleOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ,
                                       RandomSource random) {
            return new DotParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites,
                    options.red(), options.green(), options.blue());
        }
    }
}
