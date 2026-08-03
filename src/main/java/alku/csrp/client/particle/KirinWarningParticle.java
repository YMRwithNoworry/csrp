package alku.csrp.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class KirinWarningParticle extends TextureSheetParticle {
    private static final ParticleRenderType ADDITIVE_RENDER_TYPE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "csrp:kirin_warning_additive";
        }
    };

    private final float sizeBlocks;
    private final float yaw;

    private KirinWarningParticle(ClientLevel level, double x, double y, double z,
                                 float sizeBlocks, float yaw, SpriteSet sprites) {
        super(level, x, y, z);
        this.sizeBlocks = Math.max(0.0F, sizeBlocks);
        this.yaw = yaw;
        lifetime = 3;
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
        return ADDITIVE_RENDER_TYPE;
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
