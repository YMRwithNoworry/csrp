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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Direct port of SRParasites 1.10.9's {@code RenderKirinSlash}.
 *
 * <p>The original drew an untextured additive "forward cross" slash: two 18-segment planes (one
 * across X, one across Y) growing along local +Z, in three nested widths - an outer pale blue
 * glow, a mid blue, and a white core - fading in over 5 ticks, out over the last 18 ticks of the
 * slice's life, and flashing/scaling away while the hit pop plays. The additive SRC_ALPHA/ONE,
 * no-cull, no-depth-write, no-texture state maps onto vanilla's lightning render type.</p>
 */
public final class KirinSlashRenderer extends EntityRenderer<KirinSlashEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/kirin.png");

    private static final int SEGMENTS = 18;
    private static final float FADE_IN_TICKS = 5.0F;
    private static final float FADE_OUT_TICKS = 18.0F;
    private static final float OUTER_WIDTH = 0.52F;
    private static final float MID_WIDTH = 0.2F;
    private static final float CORE_WIDTH = 0.055F;
    private static final float OUTER_ALPHA = 0.13F;
    private static final float MID_ALPHA = 0.42F;
    private static final float CORE_ALPHA = 0.9F;
    private static final float SLASH_RED = 0.42F;
    private static final float SLASH_GREEN = 0.86F;
    private static final float SLASH_BLUE = 1.0F;

    public KirinSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(KirinSlashEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float visibleAge = entity.tickCount + partialTick - entity.getDelayTicks();
        if (visibleAge <= 0.0F) {
            return;
        }
        float life = Math.max(1.0F, entity.getLife());
        float growth = entity.getGrowth(partialTick);
        if (growth <= 0.0F) {
            return;
        }

        float fadeIn = Math.min(visibleAge / FADE_IN_TICKS, 1.0F);
        float fadeOut = 1.0F;
        if (visibleAge > life - FADE_OUT_TICKS) {
            fadeOut = 1.0F - Math.min((visibleAge - (life - FADE_OUT_TICKS)) / FADE_OUT_TICKS, 1.0F);
        }
        float alphaBase = Math.max(0.0F, Math.min(fadeIn, fadeOut));

        float popScale = 1.0F;
        float popWidthScale = 1.0F;
        float popAlphaBoost = 1.0F;
        if (entity.isHitPopping()) {
            float progress = Math.min((entity.getHitPopAge() + partialTick)
                    / Math.max(1.0F, entity.getHitPopTicks()), 1.0F);
            popScale = 1.0F - progress * 0.82F;
            popWidthScale = 1.0F - progress * 0.92F;
            float flash = 1.0F - Math.abs(progress - 0.18F) / 0.18F;
            flash = Mth.clamp(flash, 0.0F, 1.0F);
            popAlphaBoost = (1.0F - progress) * (1.0F + flash * 1.8F);
        }

        alphaBase *= popAlphaBoost;
        if (alphaBase <= 0.01F || popScale <= 0.02F || popWidthScale <= 0.02F) {
            return;
        }

        float length = entity.getSlashLength() * growth * popScale;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getSlashYaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getSlashPitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getSlashRoll()));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        drawCrossSlash(poseStack, consumer, OUTER_WIDTH * popWidthScale, length,
                SLASH_RED, SLASH_GREEN, SLASH_BLUE, OUTER_ALPHA * alphaBase);
        drawCrossSlash(poseStack, consumer, MID_WIDTH * popWidthScale, length,
                SLASH_RED, SLASH_GREEN, SLASH_BLUE, MID_ALPHA * alphaBase);
        drawCrossSlash(poseStack, consumer, CORE_WIDTH * popWidthScale, length,
                1.0F, 1.0F, 1.0F, CORE_ALPHA * alphaBase);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void drawCrossSlash(PoseStack poseStack, VertexConsumer consumer, float width,
            float length, float red, float green, float blue, float alpha) {
        drawPlane(poseStack, consumer, width, length, red, green, blue, alpha, true);
        drawPlane(poseStack, consumer, width, length, red, green, blue, alpha, false);
    }

    /** {@code acrossX} picks the original's plane X (spanning X) or plane Y (spanning Y). */
    private static void drawPlane(PoseStack poseStack, VertexConsumer consumer, float width,
            float length, float red, float green, float blue, float alpha, boolean acrossX) {
        float halfWidth = width * 0.5F;
        PoseStack.Pose pose = poseStack.last();
        for (int index = 0; index < SEGMENTS; index++) {
            float t0 = index / (float) SEGMENTS;
            float t1 = (index + 1) / (float) SEGMENTS;
            float z0 = length * t0;
            float z1 = length * t1;
            float alpha0 = alpha * edgeFade(t0);
            float alpha1 = alpha * edgeFade(t1);
            if (acrossX) {
                quad(consumer, pose, -halfWidth, 0.0F, z0, halfWidth, 0.0F, z0,
                        halfWidth, 0.0F, z1, -halfWidth, 0.0F, z1, red, green, blue, alpha0, alpha1);
            } else {
                quad(consumer, pose, 0.0F, -halfWidth, z0, 0.0F, halfWidth, z0,
                        0.0F, halfWidth, z1, 0.0F, -halfWidth, z1, red, green, blue, alpha0, alpha1);
            }
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float red, float green, float blue, float alpha0, float alpha1) {
        consumer.vertex(pose.pose(), x0, y0, z0).color(red, green, blue, alpha0).endVertex();
        consumer.vertex(pose.pose(), x1, y1, z1).color(red, green, blue, alpha0).endVertex();
        consumer.vertex(pose.pose(), x2, y2, z2).color(red, green, blue, alpha1).endVertex();
        consumer.vertex(pose.pose(), x3, y3, z3).color(red, green, blue, alpha1).endVertex();
    }

    /** Original {@code getBeamEdgeFade}: smoothstep so both ends of the slice taper away. */
    private static float edgeFade(float t) {
        float edgeFade = Math.min(t, 1.0F - t) * 2.0F;
        edgeFade = edgeFade * edgeFade * (3.0F - 2.0F * edgeFade);
        return Mth.clamp(edgeFade, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(KirinSlashEntity entity) {
        return TEXTURE;
    }
}
