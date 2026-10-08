package alku.csrp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Port of {@code client/particle/ParticleRage} (out109): a shrinking, fading three-frame rage puff. */
public final class RageParticle extends SingleQuadParticle {
    private RageParticle(ClientLevel level, double x, double y, double z,
                         double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ, sprites.get(0, 2));
        quadSize = 2.0F;
        lifetime = 30;
        if (velocityY != 0.0D || velocityX != 0.0D || velocityZ != 0.0D) {
            xd = velocityX;
            yd = velocityY;
            zd = velocityZ;
        }
        int textureIndex = 0;
        if (random.nextFloat() <= 0.5F) {
            textureIndex = 1;
        } else if (random.nextFloat() <= 0.5F) {
            textureIndex = 2;
        }
        setSprite(sprites.get(textureIndex, 2));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        int currentAge = age++;
        float sway = (float) (random.nextDouble() - random.nextDouble());
        float drift = (float) ((random.nextDouble() - random.nextDouble()) * 0.1D);
        float shade = (float) (random.nextDouble() * 0.2D);
        alpha -= 0.05F;
        quadSize -= 0.08F;
        setColor(1.0F - shade, 1.0F - shade, 1.0F - shade);
        y -= 0.01F + drift;
        x += drift;
        z += drift;
        roll = 0.01F * currentAge * sway;
        if (age++ >= lifetime) {
            remove();
        }
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ,
                                       RandomSource random) {
            return new RageParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites);
        }
    }
}
