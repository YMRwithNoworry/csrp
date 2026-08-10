package alku.csrp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class GoreCloudParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float originalSize;

    private GoreCloudParticle(ClientLevel level, double x, double y, double z,
                              double velocityX, double velocityY, double velocityZ,
                              SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        xd = xd * 0.1D + velocityX;
        yd = yd * 0.1D + velocityY;
        zd = zd * 0.1D + velocityZ;
        setColor(127.0F / 255.0F, 0.0F, 0.0F);
        quadSize *= 0.75F;
        quadSize *= 2.5F;
        originalSize = quadSize * 0.5F;
        lifetime = (int) ((int) (8.0D / (random.nextDouble() * 0.8D + 0.3D)) * 2.5F);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
        }
        setSpriteFromAge(sprites);
        move(xd, yd, zd);
        xd *= 0.96F;
        yd *= 0.96F;
        zd *= 0.96F;

        Player player = level.getNearestPlayer(x, y, z, 2.0D, false);
        if (player != null && y > player.getBoundingBox().minY) {
            y += (player.getBoundingBox().minY - y) * 0.2D;
            yd += (player.getDeltaMovement().y - yd) * 0.2D;
            setPos(x, y, z);
        }
        if (onGround) {
            xd *= 0.7F;
            zd *= 0.7F;
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float growth = Mth.clamp((age + partialTick) / lifetime * 32.0F, 0.0F, 1.0F);
        return originalSize * growth;
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
            return new GoreCloudParticle(level, x, y, z,
                    velocityX, velocityY, velocityZ, sprites);
        }
    }
}
