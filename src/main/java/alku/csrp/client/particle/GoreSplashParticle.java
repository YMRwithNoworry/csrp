package alku.csrp.client.particle;

import alku.csrp.particle.SrpColorParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

/** 原件 ParticleMultipleGore：按族选择肉块／呕吐物贴图，并保留重力与淡出行为。 */
public final class GoreSplashParticle extends SingleQuadParticle {
    private GoreSplashParticle(ClientLevel level, double x, double y, double z,
                              double velocityX, double velocityY, double velocityZ,
                              SpriteSet sprites, int textureFamily) {
        super(level, x, y, z, velocityX, velocityY, velocityZ, sprites.get(0, 17));
        xd = velocityX;
        yd = velocityY;
        zd = velocityZ;
        setSize(0.01F, 0.01F);
        quadSize = 0.1F;
        gravity = 0.04F;
        lifetime = 20 * (random.nextInt(3) + 1);
        setSprite(sprites.get(selectTexture(textureFamily, random), 17));
    }

    private static int selectTexture(int textureFamily, RandomSource random) {
        int family = Math.max(0, Math.min(4, textureFamily));
        int familyStart = family == 4 ? 12 : family * 3;
        if (family < 4) {
            if (random.nextDouble() <= 0.5D) {
                return familyStart + 1;
            }
            return random.nextDouble() <= 0.25D ? familyStart + 2 : familyStart;
        }
        for (int texture = 1; texture < 6; texture++) {
            if (random.nextDouble() <= 0.5D) {
                return familyStart + texture;
            }
        }
        return familyStart;
    }

    @Override
    public void tick() {
        int previousAge = age++;
        float remainingLifetime = lifetime;
        xo = x;
        yo = y;
        zo = z;
        yd -= 0.04D;
        move(xd, yd, zd);
        xd *= 0.98F;
        yd *= 0.98F;
        zd *= 0.98F;
        if (remainingLifetime - previousAge <= 5.0F && alpha >= 0.0F) {
            alpha = Math.max(0.0F, alpha - 0.05F);
        }
        if (lifetime-- <= 0) {
            remove();
        }
        if (onGround) {
            xd *= 0.7F;
            zd *= 0.7F;
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
            return new GoreSplashParticle(level, x, y, z, velocityX, velocityY, velocityZ,
                    sprites, options.variant());
        }
    }
}
