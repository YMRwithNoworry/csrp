package alku.csrp.client.particle;

import alku.csrp.particle.SrpColorParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

/**
 * Port of {@code client/particle/ParticleBlood} (out109): three-frame blood. Variant 0 falls under
 * gravity, variant 1 rises with a small drift; both switch to the landing frame on the ground.
 */
public final class BloodParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final int variant;
    private final float drift = (float) ((Math.random() - Math.random()) * 0.05D);

    private BloodParticle(ClientLevel level, double x, double y, double z,
                          double velocityX, double velocityY, double velocityZ,
                          SpriteSet sprites, int variant) {
        super(level, x, y, z, velocityX, velocityY, velocityZ, sprites.get(0, 2));
        this.sprites = sprites;
        this.variant = variant;
        quadSize = 2.0F;
        lifetime = 40;
        alpha = 0.9F;
        if (variant == 1) {
            xd = drift;
            yd = velocityY;
            zd = drift;
        }
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        int currentAge = age++;
        move(xd, yd, zd);
        if (variant == 0) {
            yd -= 0.5D;
        }
        if (variant == 1) {
            lifetime = 80;
            xo = x;
            yo = y;
            zo = z;
            yd = 0.31D;
            xd += drift * 0.5D;
            zd += drift * 0.5D;
            gravity = 0.04F;
        }
        if (currentAge >= 4) {
            setSprite(sprites.get(1, 2));
        }
        if (onGround) {
            setSprite(sprites.get(2, 2));
            xd /= 10.0D;
            zd /= 10.0D;
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
            return new BloodParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites,
                    options.variant());
        }
    }
}
