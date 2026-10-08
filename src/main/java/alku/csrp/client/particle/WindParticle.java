package alku.csrp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Port of {@code client/particle/ParticleWind} (out109): a thirteen-frame wind puff that drifts and
 * wobbles for 52 ticks.
 */
public final class WindParticle extends SingleQuadParticle {
    private static final int FRAMES = 13;
    private final SpriteSet sprites;

    private WindParticle(ClientLevel level, double x, double y, double z,
                         double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ, sprites.get(0, FRAMES - 1));
        this.sprites = sprites;
        quadSize = 2.0F;
        lifetime = 52;
        hasPhysics = true;
        alpha = 0.5F;
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
        if (age++ >= lifetime) {
            remove();
            return;
        }
        int currentAge = age++;
        y = y + currentAge * yd;
        double wobble = currentAge * 0.005D;
        x = x + (xd + (random.nextDouble() * wobble - random.nextDouble() * wobble));
        z = z + (zd + (random.nextDouble() * wobble - random.nextDouble() * wobble));
        int frame = Math.min(FRAMES - 1, currentAge / 4);
        setSprite(sprites.get(frame, FRAMES - 1));
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
            return new WindParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites);
        }
    }
}
