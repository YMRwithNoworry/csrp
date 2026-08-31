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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 麒麟斩击渲染（原版 RenderKirinSlash 为程序化光刃，无贴图）：
 * 沿斩击方向绘制两片交叉渐变光刃，长度随生长值展开，淡出期透明。
 */
public class KirinSlashRenderer extends EntityRenderer<KirinSlashEntity> {
    private static final float BLADE_HALF_WIDTH = 1.1F;
    private static final float FADE_IN_TICKS = 5.0F;
    private static final float FADE_OUT_TICKS = 18.0F;

    public KirinSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(KirinSlashEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        float visibleAge = entity.tickCount + partialTicks - entity.getDelayTicks();
        if (visibleAge <= 0.0F) {
            return;
        }
        float growth = entity.getGrowth(partialTicks);
        if (growth <= 0.0F) {
            return;
        }
        float life = Math.max(1.0F, entity.getLife());
        float fadeIn = Math.min(visibleAge / FADE_IN_TICKS, 1.0F);
        float fadeOut = 1.0F;
        if (visibleAge > life - FADE_OUT_TICKS) {
            fadeOut = 1.0F - Math.min((visibleAge - (life - FADE_OUT_TICKS)) / FADE_OUT_TICKS, 1.0F);
        }
        float alpha = Mth.clamp(Math.min(fadeIn, fadeOut), 0.0F, 1.0F);
        float hitPopScale = 1.0F;
        float hitPopWidthScale = 1.0F;
        float hitPopAlphaBoost = 1.0F;
        if (entity.isHitPopping()) {
            float progress = Mth.clamp((entity.getHitPopAge() + partialTicks)
                    / (float) Math.max(1, entity.getHitPopTicks()), 0.0F, 1.0F);
            hitPopScale = 1.0F - progress * 0.82F;
            hitPopWidthScale = 1.0F - progress * 0.92F;
            float flash = 1.0F - Math.abs(progress - 0.18F) / 0.18F;
            hitPopAlphaBoost = (1.0F - progress) * (1.0F + Mth.clamp(flash, 0.0F, 1.0F) * 1.8F);
        }
        alpha *= hitPopAlphaBoost;
        if (entity.isFading()) {
            alpha *= 0.35F;
        }
        if (alpha <= 0.01F || hitPopScale <= 0.02F || hitPopWidthScale <= 0.02F) {
            return;
        }
        float length = entity.getLength() * growth * hitPopScale;
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(entity.getYaw()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(entity.getPitch()));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(entity.getRoll()));

        drawCrossLayer(poseStack, consumer, length, 0.52F * hitPopWidthScale,
                0.42F, 0.86F, 1.0F, 0.13F * alpha);
        drawCrossLayer(poseStack, consumer, length, 0.20F * hitPopWidthScale,
                0.42F, 0.86F, 1.0F, 0.42F * alpha);
        drawCrossLayer(poseStack, consumer, length, 0.055F * hitPopWidthScale,
                1.0F, 1.0F, 1.0F, 0.90F * alpha);
        poseStack.popPose();
    }

    private static void drawCrossLayer(PoseStack poseStack, VertexConsumer consumer, float length,
            float width, float red, float green, float blue, float alpha) {
        for (int blade = 0; blade < 2; blade++) {
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F * blade));
            drawForwardPlane(poseStack.last().pose(), consumer, width, length, red, green, blue, alpha);
            poseStack.popPose();
        }
    }

    private static void drawForwardPlane(Matrix4f matrix, VertexConsumer consumer, float width, float length,
            float red, float green, float blue, float alpha) {
        float halfWidth = width * 0.5F;
        int segments = 18;
        for (int index = 0; index < segments; index++) {
            float t0 = (float) index / segments;
            float t1 = (float) (index + 1) / segments;
            float z0 = length * t0;
            float z1 = length * t1;
            float a0 = alpha * edgeFade(t0);
            float a1 = alpha * edgeFade(t1);
            vertex(consumer, matrix, -halfWidth, 0.0F, z0, red, green, blue, a0);
            vertex(consumer, matrix, halfWidth, 0.0F, z0, red, green, blue, a0);
            vertex(consumer, matrix, halfWidth, 0.0F, z1, red, green, blue, a1);
            vertex(consumer, matrix, -halfWidth, 0.0F, z1, red, green, blue, a1);
        }
    }

    private static float edgeFade(float position) {
        float fade = Mth.clamp(Math.min(position, 1.0F - position) * 2.0F, 0.0F, 1.0F);
        return fade * fade * (3.0F - 2.0F * fade);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
            float red, float green, float blue, float alpha) {
        org.joml.Vector3f pos = new org.joml.Vector3f();
        matrix.transformPosition(x, y, z, pos);
        consumer.addVertex(pos.x, pos.y, pos.z)
                .setColor(red, green, blue, Mth.clamp(alpha, 0.0F, 1.0F));
    }

    @Override
    public ResourceLocation getTextureLocation(KirinSlashEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/kirin_slash.png");
    }

    @Override
    public boolean shouldRender(KirinSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
            double camX, double camY, double camZ) {
        Vec3 extent = entity.getSlashDirection().scale(entity.getLength());
        AABB slashBounds = entity.getBoundingBox().expandTowards(extent).inflate(BLADE_HALF_WIDTH);
        return super.shouldRender(entity, frustum, camX, camY, camZ)
                || frustum.isVisible(slashBounds);
    }
}
