package alku.csrp.client.renderer;

import alku.csrp.entity.KirinSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 麒麟斩击渲染（原版 RenderKirinSlash 为程序化光刃，无贴图）：
 * 沿斩击方向绘制两片交叉渐变光刃，长度随生长值展开，淡出期透明。
 */
public class KirinSlashRenderer extends EntityRenderer<KirinSlashEntity> {
    private static final float BLADE_HALF_WIDTH = 1.1F;

    public KirinSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(KirinSlashEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        float growth = entity.getGrowth(partialTicks);
        if (growth <= 0.0F) {
            return;
        }
        float length = Math.max(1.0F, entity.getLength() * growth);
        float alpha = entity.getRenderAlpha(partialTicks);
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entity.getYaw()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(entity.getPitch()));

        for (int blade = 0; blade < 2; blade++) {
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F * blade));
            Matrix4f matrix = poseStack.last().pose();
            quad(consumer, matrix,
                    0.0F, -BLADE_HALF_WIDTH, 0.0F,
                    length, -BLADE_HALF_WIDTH * 0.35F, 0.0F,
                    length, BLADE_HALF_WIDTH * 0.35F, 0.0F,
                    0.0F, BLADE_HALF_WIDTH, 0.0F, alpha);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4, float alpha) {
        vertex(consumer, matrix, x1, y1, z1, 0.35F, alpha);
        vertex(consumer, matrix, x2, y2, z2, 0.95F, alpha * 0.9F);
        vertex(consumer, matrix, x3, y3, z3, 1.0F, alpha * 0.9F);
        vertex(consumer, matrix, x4, y4, z4, 0.35F, alpha);
        vertex(consumer, matrix, x4, y4, z4, 0.35F, alpha);
        vertex(consumer, matrix, x3, y3, z3, 1.0F, alpha * 0.9F);
        vertex(consumer, matrix, x2, y2, z2, 0.95F, alpha * 0.9F);
        vertex(consumer, matrix, x1, y1, z1, 0.35F, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
            float brightness, float alpha) {
        org.joml.Vector3f pos = new org.joml.Vector3f();
        matrix.transformPosition(x, y, z, pos);
        consumer.addVertex(pos.x, pos.y, pos.z)
                .setColor(brightness, 0.65F + brightness * 0.2F, 1.0F, Mth.clamp(alpha, 0.0F, 1.0F));
    }

    @Override
    public ResourceLocation getTextureLocation(KirinSlashEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/kirin_slash.png");
    }

    @Override
    public boolean shouldRender(KirinSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
            double camX, double camY, double camZ) {
        Vec3 end = entity.position().add(entity.getSlashDirection().scale(entity.getLength()));
        return super.shouldRender(entity, frustum, camX, camY, camZ)
                || frustum.isVisible(entity.getBoundingBox().expandTowards(
                        entity.getSlashDirection().scale(entity.getLength())).move(end.subtract(entity.position())));
    }
}
