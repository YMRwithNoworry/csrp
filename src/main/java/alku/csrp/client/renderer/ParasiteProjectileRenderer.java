package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.ParasiteProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ParasiteProjectileRenderer extends EntityRenderer<ParasiteProjectileEntity> {
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/scary_orb.png");
    private static final ResourceLocation LENCIA_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/lencia.png");
    private static final ResourceLocation ELVIA_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/elvia.png");
    private static final ResourceLocation NADE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/nade.png");

    public ParasiteProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ParasiteProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (!entity.shouldRenderAsBillboard()) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        float halfWidth = entity.getRenderWidth() * 0.5F;
        float halfHeight = entity.getRenderHeight() * 0.5F;
        poseStack.pushPose();
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
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
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ParasiteProjectileEntity entity) {
        return switch (entity.getMode()) {
            case LENCIA_BALL -> LENCIA_TEXTURE;
            case ELVIA_BALL -> ELVIA_TEXTURE;
            case ELVIA_NADE -> NADE_TEXTURE;
            case ACID -> NADE_TEXTURE;
            default -> DEFAULT_TEXTURE;
        };
    }
}
