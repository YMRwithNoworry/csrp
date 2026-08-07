package alku.csrp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** The original 18-step intro, loop and outro animation used by parasite fog. */
public final class CoolerFogParticle extends TextureSheetParticle {
    private static final int FRAME_COUNT = 9;
    private final SpriteSet sprites;
    private final double horizontalSpeed;
    private final double verticalSpeed;

    private CoolerFogParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.horizontalSpeed = random.nextDouble() * 0.01D - random.nextDouble() * 0.01D;
        this.verticalSpeed = random.nextDouble() * 0.005D - random.nextDouble() * 0.005D;
        this.quadSize = 10.0F + random.nextFloat() * 0.5F;
        this.lifetime = 144;
        this.alpha = 0.9F;
        this.hasPhysics = false;
        setSprite(sprites.get(0, FRAME_COUNT - 1));
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

        setPos(x + horizontalSpeed, y - verticalSpeed, z + horizontalSpeed);
        setSprite(sprites.get(frameForAge(age), FRAME_COUNT - 1));
    }

    private static int frameForAge(int age) {
        int step = Math.min(age / 8, 17);
        return switch (step) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            case 5, 9 -> 5;
            case 6, 10 -> 6;
            case 7, 11 -> 7;
            case 8, 12 -> 8;
            case 13 -> 4;
            case 14 -> 3;
            case 15 -> 2;
            case 16 -> 1;
            default -> 0;
        };
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
                double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new CoolerFogParticle(level, x, y, z, sprites);
        }
    }
}
