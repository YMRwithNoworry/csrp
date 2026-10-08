package alku.csrp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Port of the 1.12.2 {@code client/particle/ParticleSpore} (out109): a four-frame white spore that
 * grows from nothing to 0.7 over 175 ticks, then shrinks and fades until 200 ticks.
 */
public final class SporeParticle extends SingleQuadParticle {
    private static final int FRAMES = 4;
    private static final int GROW_END = 175;

    private SporeParticle(ClientLevel level, double x, double y, double z,
                          double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ, sprites.get(0, FRAMES - 1));
        xd = velocityX * 0.2D + (random.nextDouble() * 2.0D - 1.0D) * 0.02D;
        yd = velocityY * 0.2D + (random.nextDouble() * 2.0D - 1.0D) * 0.02D;
        zd = velocityZ * 0.2D + (random.nextDouble() * 2.0D - 1.0D) * 0.02D;
        quadSize = 0.0F;
        lifetime = 200;
        gravity = 0.0F;
        hasPhysics = false;
        float shade = (float) (1.0D - random.nextDouble() * 0.5D);
        alpha = 0.6F;
        setColor(shade, shade, shade);
        int textureIndex = 0;
        if (random.nextFloat() <= 0.5F) {
            textureIndex = 1;
        } else if (random.nextFloat() <= 0.5F) {
            textureIndex = 2;
        } else if (random.nextFloat() <= 0.5F) {
            textureIndex = 3;
        }
        setSprite(sprites.get(textureIndex, FRAMES - 1));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (quadSize < 0.7F && age < GROW_END) {
            quadSize += 0.05F;
        }
        if (age++ >= lifetime) {
            remove();
            return;
        }
        if (age >= GROW_END) {
            quadSize -= 0.0278F;
            alpha -= 0.01F;
        }
        yd -= 0.04D * gravity;
        move(xd, yd, zd);
        xd *= 0.98F;
        zd *= 0.98F;
        if (onGround) {
            xd *= 0.7F;
            zd *= 0.7F;
            yd += 1.0E-5D;
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
            return new SporeParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites);
        }
    }
}
