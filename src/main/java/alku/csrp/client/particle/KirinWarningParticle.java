package alku.csrp.client.particle;

import alku.csrp.Csrp;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

public final class KirinWarningParticle extends SingleQuadParticle {
    private static final Identifier ADDITIVE_PIPELINE_ID = Identifier.fromNamespaceAndPath(
            Csrp.MODID, "pipeline/kirin_warning_additive");
    private static final RenderPipeline ADDITIVE_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(ADDITIVE_PIPELINE_ID)
            .withColorTargetState(new ColorTargetState(
                    new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE)))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();
    private static final SingleQuadParticle.Layer ADDITIVE_LAYER = new SingleQuadParticle.Layer(
            true, TextureAtlas.LOCATION_PARTICLES, ADDITIVE_PIPELINE, RenderPipelines.OIT_PARTICLE);

    private final float sizeBlocks;
    private final float yaw;

    private KirinWarningParticle(ClientLevel level, double x, double y, double z,
                                 float sizeBlocks, float yaw, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.sizeBlocks = Math.max(0.0F, sizeBlocks);
        this.yaw = yaw;
        lifetime = 3;
        hasPhysics = false;
        gravity = 0.0F;
        setSize(this.sizeBlocks, this.sizeBlocks);
        setSprite(sprites.get(random));
    }

    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTick) {
        float x = (float) (Mth.lerp(partialTick, xo, this.x) - camera.position().x());
        float y = (float) (Mth.lerp(partialTick, yo, this.y) - camera.position().y());
        float z = (float) (Mth.lerp(partialTick, zo, this.z) - camera.position().z());
        float halfSize = sizeBlocks * 0.5F;
        Quaternionf rotation = new Quaternionf();
        rotation.rotationYXZ(-yaw, (float) (-Math.PI / 2.0D), 0.0F);
        int light = getLightCoords(partialTick);

        particleTypeRenderState.add(ADDITIVE_LAYER, x, y, z,
                rotation.x, rotation.y, rotation.z, rotation.w,
                halfSize, getU0(), getU1(), getV0(), getV1(),
                ARGB.colorFromFloat(this.alpha, 1.0F, 1.0F, 1.0F), light);
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 0xF000F0;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return ADDITIVE_LAYER;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double sizeBlocks, double yaw, double ignored, RandomSource random) {
            return new KirinWarningParticle(level, x, y, z, (float) sizeBlocks, (float) yaw, sprites);
        }
    }
}
