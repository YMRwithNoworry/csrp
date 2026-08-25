package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.KirinSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class KirinSlashRenderer extends EntityRenderer<KirinSlashEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Csrp.MODID, "textures/entity/kirin.png");

    public KirinSlashRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(KirinSlashEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float growth = entity.getGrowth(partialTick);
        if (growth <= 0.0F) return;
        float length = entity.getSlashLength() * growth;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getSlashYaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getSlashPitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getSlashRoll()));
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        float width = 0.32F;
        quad(vertices, poseStack.last(), 0.0F, -width, length, width, packedLight);
        quad(vertices, poseStack.last(), -width, 0.0F, width, length, packedLight);
        poseStack.popPose();
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose pose, float x1, float z1,
                             float x2, float z2, int packedLight) {
        vertex(vertices, pose, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, packedLight);
        vertex(vertices, pose, x1, 0.0F, z1, 0.0F, 0.0F, packedLight);
        vertex(vertices, pose, x2, 0.0F, z2, 1.0F, 0.0F, packedLight);
        vertex(vertices, pose, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, packedLight);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int packedLight) {
        vertices.vertex(pose.pose(), x, y, z).color(255, 255, 255, 220).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(KirinSlashEntity entity) { return TEXTURE; }
}
