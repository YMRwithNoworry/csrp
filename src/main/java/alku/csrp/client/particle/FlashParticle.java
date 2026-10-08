package alku.csrp.client.particle;

import alku.csrp.particle.SrpColorParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

/** Port of {@code client/particle/ParticleFlash} (out109): a fast expanding coloured flash. */
public final class FlashParticle extends SingleQuadParticle {
    private final int red;
    private final int green;
    private final int blue;

    private FlashParticle(ClientLevel level, double x, double y, double z,
                          double velocityX, double velocityY, double velocityZ,
                          SpriteSet sprites, int red, int green, int blue) {
        super(level, x, y, z, velocityX, velocityY, velocityZ, sprites.get(0, 0));
        this.red = red;
        this.green = green;
        this.blue = blue;
        quadSize = 6.0F;
        lifetime = 8;
        if (velocityY != 0.0D || velocityX != 0.0D || velocityZ != 0.0D) {
            xd = velocityX;
            yd = velocityY;
            zd = velocityZ;
        }
    }

    @Override
    public void tick() {
        int currentAge = age++;
        float rotation = 20.0F;
        if (currentAge >= lifetime) {
            remove();
            return;
        }
        quadSize = 8.0F / (currentAge * 0.5F);
        alpha = currentAge / 0.1F;
        roll = (float) (Math.sin(rotation * currentAge) * Math.cos(rotation * currentAge));
        setColor(currentAge * red, currentAge * green, currentAge * blue);
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
            return new FlashParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites,
                    options.red(), options.green(), options.blue());
        }
    }
}
