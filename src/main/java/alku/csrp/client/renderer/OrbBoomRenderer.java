package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.OrbBoomEntity;
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

public final class OrbBoomRenderer extends EntityRenderer<OrbBoomEntity, EntityRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/scary_orb.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE);

    public OrbBoomRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        float halfWidth = state.boundingBoxWidth * 0.5F;
        float halfHeight = state.boundingBoxHeight * 0.5F;
        poseStack.pushPose();
        poseStack.translate(0.0D, halfHeight, 0.0D);
        poseStack.rotate(camera.orientation);
        poseStack.rotateDegrees(Axis.YP, 180.0F);
        int packedLight = state.lightCoords;
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, vertices) -> {
            vertex(vertices, pose, -halfWidth, -halfHeight, 0.0F, 1.0F, packedLight);
            vertex(vertices, pose, halfWidth, -halfHeight, 1.0F, 1.0F, packedLight);
            vertex(vertices, pose, halfWidth, halfHeight, 1.0F, 0.0F, packedLight);
            vertex(vertices, pose, -halfWidth, halfHeight, 0.0F, 0.0F, packedLight);
        });
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y,
                               float u, float v, int packedLight) {
        vertices.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 210)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
