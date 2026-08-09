package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.OrbBoomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class OrbBoomRenderer extends EntityRenderer<OrbBoomEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/scary_orb.png");

    public OrbBoomRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(OrbBoomEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float halfWidth = entity.getBbWidth() * 0.5F;
        float halfHeight = entity.getBbHeight() * 0.5F;
        poseStack.pushPose();
        poseStack.translate(0.0D, halfHeight, 0.0D);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        vertex(vertices, pose, -halfWidth, -halfHeight, 0.0F, 1.0F, packedLight);
        vertex(vertices, pose, halfWidth, -halfHeight, 1.0F, 1.0F, packedLight);
        vertex(vertices, pose, halfWidth, halfHeight, 1.0F, 0.0F, packedLight);
        vertex(vertices, pose, -halfWidth, halfHeight, 0.0F, 0.0F, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
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

    @Override
    public ResourceLocation getTextureLocation(OrbBoomEntity entity) {
        return TEXTURE;
    }
}
