package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.ScaryOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ScaryOrbRenderer extends EntityRenderer<ScaryOrbEntity> {
    private static final ResourceLocation CORE_TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/orbscary.png");
    private static final ResourceLocation AURA_TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/orbscary_armor.png");
    private static final float ORB_DIAMETER = 2.4F;
    private static final float SPHERE_RADIUS = ORB_DIAMETER * 0.5F;
    private static final int SPHERE_STACKS = 18;
    private static final int SPHERE_SLICES = 18;
    private static final int FULL_BRIGHT = 0xF000F0;

    public ScaryOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(ScaryOrbEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        float appear = Mth.clamp(age / 10.0F, 0.25F, 1.0F);
        float pulse = 1.0F + Mth.sin(age * 0.3F) * 0.08F;

        poseStack.pushPose();
        poseStack.scale(appear * pulse, appear * pulse, appear * pulse);
        poseStack.mulPose(Axis.YP.rotation(age * 0.05F));
        renderSphere(poseStack, bufferSource.getBuffer(RenderType.entityTranslucentEmissive(CORE_TEXTURE)),
                SPHERE_RADIUS, 1.0F, 1.0F, 1.0F, 220);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(appear * 1.12F, appear * 1.12F, appear * 1.12F);
        poseStack.mulPose(Axis.YP.rotation(-age * 0.07F));
        renderSphere(poseStack, bufferSource.getBuffer(RenderType.entityTranslucentEmissive(AURA_TEXTURE)),
                SPHERE_RADIUS, 1.0F, 1.0F, 1.0F, 135);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderSphere(PoseStack poseStack, VertexConsumer consumer, float radius,
                                     float red, float green, float blue, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        for (int stack = 0; stack < SPHERE_STACKS; stack++) {
            float v0 = stack / (float) SPHERE_STACKS;
            float v1 = (stack + 1) / (float) SPHERE_STACKS;
            float phi0 = (float) Math.PI * v0;
            float phi1 = (float) Math.PI * v1;
            for (int slice = 0; slice < SPHERE_SLICES; slice++) {
                float u0 = slice / (float) SPHERE_SLICES;
                float u1 = (slice + 1) / (float) SPHERE_SLICES;
                float theta0 = (float) (Math.PI * 2.0D * u0);
                float theta1 = (float) (Math.PI * 2.0D * u1);
                renderVertex(pose, consumer, radius, phi0, theta0, u0, v0, red, green, blue, alpha);
                renderVertex(pose, consumer, radius, phi1, theta0, u0, v1, red, green, blue, alpha);
                renderVertex(pose, consumer, radius, phi1, theta1, u1, v1, red, green, blue, alpha);
                renderVertex(pose, consumer, radius, phi0, theta1, u1, v0, red, green, blue, alpha);
            }
        }
    }

    private static void renderVertex(PoseStack.Pose pose, VertexConsumer consumer, float radius,
                                     float phi, float theta, float u, float v,
                                     float red, float green, float blue, int alpha) {
        float x = Mth.sin(phi) * Mth.cos(theta);
        float y = Mth.cos(phi);
        float z = Mth.sin(phi) * Mth.sin(theta);
        consumer.vertex(pose.pose(), x * radius, y * radius, z * radius)
                .color((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(pose.normal(), x, y, z).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ScaryOrbEntity entity) {
        return CORE_TEXTURE;
    }
}
