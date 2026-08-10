package alku.csrp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class AssimilationSplashParticle extends TextureSheetParticle {
    private AssimilationSplashParticle(ClientLevel level, double x, double y, double z,
                                        double velocityX, double velocityY, double velocityZ,
                                        SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ);
        xd = velocityX;
        yd = velocityY;
        zd = velocityZ;
        setSize(0.01F, 0.01F);
        quadSize = 0.1F;
        gravity = 1.0F;
        friction = 0.98F;
        lifetime = 20 * (random.nextInt(3) + 1);
        int textureIndex = 0;
        if (random.nextFloat() <= 0.5F) {
            textureIndex = 1;
        } else if (random.nextFloat() <= 0.25F) {
            textureIndex = 2;
        }
        setSprite(sprites.get(textureIndex, 2));
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
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new AssimilationSplashParticle(level, x, y, z,
                    velocityX, velocityY, velocityZ, sprites);
        }
    }
}
