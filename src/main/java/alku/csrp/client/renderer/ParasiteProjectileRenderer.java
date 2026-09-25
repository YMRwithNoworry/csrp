package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.ParasiteProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class ParasiteProjectileRenderer
        extends EntityRenderer<ParasiteProjectileEntity, ParasiteProjectileRenderer.State> {
    /**
     * Fallback for every projectile mode without a dedicated texture.  The original's scary-orb ball uses
     * {@code srparasites:textures/entity/monster/orbscary.png}; the previous value referenced a
     * {@code scary_orb.png} that was never shipped, so those projectiles drew the missing-texture
     * checkerboard.
     */
    private static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/orbscary.png");
    private static final Identifier LENCIA_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/lencia.png");
    private static final Identifier ELVIA_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/elvia.png");
    private static final Identifier NADE_PROJECTILE_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/nade.png");
    private static final Identifier SPINEBALL_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/spineball.png");
    private static final Identifier YELLOWEYE_NADE_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/monster/nade.png");
    private static final Identifier ALAFHA_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/alafha.png");
    private static final Identifier ANGED_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/anged.png");
    private static final Identifier BIOMASS_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/biomass.png");

    public ParasiteProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ParasiteProjectileEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.yelloweyeNadeArmed = entity.isYelloweyeNadeArmed();
        state.billboard = entity.shouldRenderAsBillboard();
        state.renderWidth = entity.getRenderWidth();
        state.renderHeight = entity.getRenderHeight();
        state.texture = getTextureLocation(entity);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        if (state.yelloweyeNadeArmed) {
            renderYelloweyeNade(state, poseStack, submitNodeCollector);
            super.submit(state, poseStack, submitNodeCollector, camera);
            return;
        }
        if (!state.billboard) {
            super.submit(state, poseStack, submitNodeCollector, camera);
            return;
        }
        float halfWidth = state.renderWidth * 0.5F;
        float halfHeight = state.renderHeight * 0.5F;
        poseStack.pushPose();
        poseStack.rotate(camera.orientation);
        poseStack.rotateDegrees(Axis.YP, 180.0F);
        RenderType type = RenderTypes.entityCutout(state.texture);
        submitNodeCollector.submitCustomGeometry(poseStack, type, (pose, vertices) -> {
            vertex(vertices, pose, -halfWidth, -halfHeight, 0.0F, 1.0F, state.lightCoords);
            vertex(vertices, pose, halfWidth, -halfHeight, 1.0F, 1.0F, state.lightCoords);
            vertex(vertices, pose, halfWidth, halfHeight, 1.0F, 0.0F, state.lightCoords);
            vertex(vertices, pose, -halfWidth, halfHeight, 0.0F, 0.0F, state.lightCoords);
        });
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void renderYelloweyeNade(State state, PoseStack poseStack,
                                            SubmitNodeCollector submitNodeCollector) {
        float halfWidth = state.renderWidth * 0.5F;
        float height = state.renderHeight;
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        RenderType type = RenderTypes.entityCutout(YELLOWEYE_NADE_TEXTURE);
        submitNodeCollector.submitCustomGeometry(poseStack, type, (pose, vertices) -> {
            quad(vertices, pose, -halfWidth, 0.0F, -halfWidth, halfWidth, height, -halfWidth,
                    0.25F, 0.5F, 0.5F, 1.0F, 0.0F, 0.0F, -1.0F, state.lightCoords);
            quad(vertices, pose, halfWidth, 0.0F, halfWidth, -halfWidth, height, halfWidth,
                    0.75F, 0.5F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, state.lightCoords);
            quadX(vertices, pose, -halfWidth, 0.0F, halfWidth, height,
                    0.0F, 0.5F, 0.25F, 1.0F, -1.0F, 0.0F, 0.0F, state.lightCoords);
            quadX(vertices, pose, halfWidth, 0.0F, -halfWidth, height,
                    0.5F, 0.5F, 0.75F, 1.0F, 1.0F, 0.0F, 0.0F, state.lightCoords);
            quadY(vertices, pose, height, -halfWidth, halfWidth,
                    0.25F, 0.0F, 0.5F, 0.5F, 0.0F, 1.0F, 0.0F, state.lightCoords);
            quadY(vertices, pose, 0.0F, halfWidth, -halfWidth,
                    0.5F, 0.0F, 0.75F, 0.5F, 0.0F, -1.0F, 0.0F, state.lightCoords);
        });
        poseStack.popPose();
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose pose,
                             float x1, float y1, float z, float x2, float y2, float ignoredZ,
                             float u1, float v1, float u2, float v2,
                             float nx, float ny, float nz, int packedLight) {
        cubeVertex(vertices, pose, x1, y1, z, u1, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x2, y1, z, u2, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x2, y2, z, u2, v1, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x1, y2, z, u1, v1, nx, ny, nz, packedLight);
    }

    private static void quadX(VertexConsumer vertices, PoseStack.Pose pose,
                              float x, float y1, float z1, float y2,
                              float u1, float v1, float u2, float v2,
                              float nx, float ny, float nz, int packedLight) {
        cubeVertex(vertices, pose, x, y1, z1, u1, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x, y1, -z1, u2, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x, y2, -z1, u2, v1, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x, y2, z1, u1, v1, nx, ny, nz, packedLight);
    }

    private static void quadY(VertexConsumer vertices, PoseStack.Pose pose,
                              float y, float first, float second,
                              float u1, float v1, float u2, float v2,
                              float nx, float ny, float nz, int packedLight) {
        cubeVertex(vertices, pose, first, y, first, u1, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, second, y, first, u2, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, second, y, second, u2, v1, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, first, y, second, u1, v1, nx, ny, nz, packedLight);
    }

    private static void cubeVertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z,
                                   float u, float v, float nx, float ny, float nz, int packedLight) {
        vertices.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y,
                               float u, float v, int packedLight) {
        vertices.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private Identifier getTextureLocation(ParasiteProjectileEntity entity) {
        return switch (entity.getMode()) {
            case LENCIA_BALL -> LENCIA_TEXTURE;
            case ELVIA_BALL -> ELVIA_TEXTURE;
            case ELVIA_NADE, ACID -> NADE_PROJECTILE_TEXTURE;
            case YELLOWEYE_SPINE -> SPINEBALL_TEXTURE;
            case YELLOWEYE_NADE -> entity.isYelloweyeNadeArmed()
                    ? YELLOWEYE_NADE_TEXTURE : NADE_PROJECTILE_TEXTURE;
            case ALAFHA_BALL -> ALAFHA_TEXTURE;
            case ANGED_BALL -> ANGED_TEXTURE;
            case BIOMASS_BALL -> BIOMASS_TEXTURE;
            default -> DEFAULT_TEXTURE;
        };
    }

    /** Per-frame snapshot of the projectile's billboard state. */
    public static final class State extends EntityRenderState {
        public float yRot;
        public boolean yelloweyeNadeArmed;
        public boolean billboard;
        public float renderWidth;
        public float renderHeight;
        public Identifier texture;
    }
}
