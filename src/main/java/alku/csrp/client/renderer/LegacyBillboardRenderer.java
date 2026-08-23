package alku.csrp.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Small camera-facing quad used by SRP's legacy projectile-only renderers. */
public class LegacyBillboardRenderer<T extends Entity> extends EntityRenderer<T> {
    private final ResourceLocation texture;
    private final float width;
    private final float height;

    public LegacyBillboardRenderer(EntityRendererProvider.Context context, ResourceLocation texture,
                                   float width, float height) {
        super(context);
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;
        vertex(vertices, pose, -halfWidth, -halfHeight, 0.0F, 1.0F, packedLight);
        vertex(vertices, pose, halfWidth, -halfHeight, 1.0F, 1.0F, packedLight);
        vertex(vertices, pose, halfWidth, halfHeight, 1.0F, 0.0F, packedLight);
        vertex(vertices, pose, -halfWidth, halfHeight, 0.0F, 0.0F, packedLight);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y,
                               float u, float v, int packedLight) {
        vertices.vertex(pose.pose(), x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
