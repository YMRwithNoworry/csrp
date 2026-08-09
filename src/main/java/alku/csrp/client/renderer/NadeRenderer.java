package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.NadeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class NadeRenderer extends EntityRenderer<NadeEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/monster/nade.png");

    public NadeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(NadeEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float halfWidth = entity.getRenderWidth() * 0.5F;
        float height = entity.getRenderHeight();
        poseStack.pushPose();
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        vertex(vertices, pose, -halfWidth, 0.0F, 0.0F, 1.0F, packedLight);
        vertex(vertices, pose, halfWidth, 0.0F, 1.0F, 1.0F, packedLight);
        vertex(vertices, pose, halfWidth, height, 1.0F, 0.0F, packedLight);
        vertex(vertices, pose, -halfWidth, height, 0.0F, 0.0F, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
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

    @Override
    public ResourceLocation getTextureLocation(NadeEntity entity) {
        return TEXTURE;
    }
}
