package alku.csrp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class BiomassParticle extends TextureSheetParticle {
    private BiomassParticle(ClientLevel level, double x, double y, double z,
                            double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ);
        xd = velocityX;
        yd = velocityY;
        zd = velocityZ;
        quadSize = 0.3F;
        lifetime = 45;
        friction = 0.98F;
        gravity = -0.05F;
        setSprite(sprites.get(random.nextInt(4), 3));
    }

    @Override
    public void tick() {
        super.tick();
        if (age >= 25) {
            quadSize = Math.max(0.02F, quadSize - 0.01F);
            rCol = Math.max(0.0F, rCol - 0.03F);
            gCol = Math.max(0.0F, gCol - 0.02F);
            bCol = Math.max(0.0F, bCol - 0.025F);
        }
    }

    @Override
    protected int getLightColor(float partialTick) {
        float progress = Mth.clamp((age + partialTick) / lifetime, 0.0F, 1.0F);
        int light = super.getLightColor(partialTick);
        int block = Math.min(240, (light & 0xFF) + (int) (progress * 240.0F));
        return block | (light >> 16 & 0xFF) << 16;
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
            return new BiomassParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites);
        }
    }
}
