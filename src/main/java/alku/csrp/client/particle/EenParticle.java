package alku.csrp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Port of {@code client/particle/ParticleEen} (out109): a red spore that travels from its spawn point
 * towards {@code origin + velocity} while falling with a quartic curve.
 */
public final class EenParticle extends SingleQuadParticle {
    private final double coordX;
    private final double coordY;
    private final double coordZ;

    private EenParticle(ClientLevel level, double x, double y, double z,
                        double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ, sprites.get(0, 0));
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.coordX = x;
        this.coordY = y;
        this.coordZ = z;
        this.xo = x + velocityX;
        this.yo = y + velocityY;
        this.zo = z + velocityZ;
        this.setPos(xo, yo, zo);
        this.lifetime = (int) (random.nextDouble() * 10.0D) + 30;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        float progress = (float) age / lifetime;
        float remaining = 1.0F - progress;
        float fall = 1.0F - remaining;
        fall *= fall;
        fall *= fall;
        x = coordX + xd * remaining;
        y = coordY + yd * remaining - fall * 1.2F;
        z = coordZ + zd * remaining;
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
            return new EenParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites);
        }
    }
}
