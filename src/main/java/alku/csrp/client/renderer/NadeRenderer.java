package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.NadeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class NadeRenderer extends EntityRenderer<NadeEntity, NadeRenderer.NadeRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/monster/nade.png");

    public NadeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public NadeRenderState createRenderState() {
        return new NadeRenderState();
    }

    @Override
    public void extractRenderState(NadeEntity entity, NadeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.halfWidth = entity.getRenderWidth() * 0.5F;
        state.height = entity.getRenderHeight();
    }

    @Override
    public void submit(NadeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotate(camera.orientation);
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F));
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE),
                (pose, vertices) -> {
                    vertex(vertices, pose, -state.halfWidth, 0.0F, 0.0F, 1.0F, state.lightCoords);
                    vertex(vertices, pose, state.halfWidth, 0.0F, 1.0F, 1.0F, state.lightCoords);
                    vertex(vertices, pose, state.halfWidth, state.height, 1.0F, 0.0F, state.lightCoords);
                    vertex(vertices, pose, -state.halfWidth, state.height, 0.0F, 0.0F, state.lightCoords);
                });
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
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

    /** Per-frame snapshot of the data the geometry submission needs. */
    public static final class NadeRenderState extends EntityRenderState {
        public float halfWidth;
        public float height;
    }
}
