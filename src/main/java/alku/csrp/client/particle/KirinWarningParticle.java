package alku.csrp.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class KirinWarningParticle extends TextureSheetParticle {
    private final float sizeBlocks;
    private final float yaw;

    private KirinWarningParticle(ClientLevel level, double x, double y, double z,
                                 float sizeBlocks, float yaw, SpriteSet sprites) {
        super(level, x, y, z);
        this.sizeBlocks = Math.max(0.0F, sizeBlocks);
        this.yaw = yaw;
        lifetime = 1;
        hasPhysics = false;
        gravity = 0.0F;
        setSize(this.sizeBlocks, this.sizeBlocks);
        pickSprite(sprites);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        float x = (float) (Mth.lerp(partialTick, xo, this.x) - camera.getPosition().x);
        float y = (float) (Mth.lerp(partialTick, yo, this.y) - camera.getPosition().y);
        float z = (float) (Mth.lerp(partialTick, zo, this.z) - camera.getPosition().z);
        float halfSize = sizeBlocks * 0.5F;
        float cosine = Mth.cos(yaw);
        float sine = Mth.sin(yaw);
        int light = getLightColor(partialTick);
        int alpha = (int) (this.alpha * 255.0F);

        renderVertex(consumer, x, y, z, -halfSize, -halfSize, cosine, sine, getU0(), getV0(), light, alpha);
        renderVertex(consumer, x, y, z, -halfSize, halfSize, cosine, sine, getU0(), getV1(), light, alpha);
        renderVertex(consumer, x, y, z, halfSize, halfSize, cosine, sine, getU1(), getV1(), light, alpha);
        renderVertex(consumer, x, y, z, halfSize, -halfSize, cosine, sine, getU1(), getV0(), light, alpha);
    }

    private static void renderVertex(VertexConsumer consumer, float centerX, float y, float centerZ,
                                     float x, float z, float cosine, float sine, float u, float v,
                                     int light, int alpha) {
        consumer.addVertex(centerX + x * cosine - z * sine, y, centerZ + x * sine + z * cosine)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setLight(light);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
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
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double sizeBlocks, double yaw, double ignored) {
            return new KirinWarningParticle(level, x, y, z, (float) sizeBlocks, (float) yaw, sprites);
        }
    }
}
