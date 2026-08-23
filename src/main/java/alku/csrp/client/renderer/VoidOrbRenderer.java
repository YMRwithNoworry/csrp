package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.VoidOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class VoidOrbRenderer extends EntityRenderer<VoidOrbEntity> {
    private static final ResourceLocation CORE_TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/orbvoid.png");
    private static final ResourceLocation AURA_TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/orbvoid_armor.png");
    private static final float VOID_ORB_DIAMETER = 2.4F;
    private static final float SPHERE_RADIUS = VOID_ORB_DIAMETER * 0.5F;
    private static final int SPHERE_STACKS = 18;
    private static final int SPHERE_SLICES = 18;
    private static final int FULL_BRIGHT = 0xF000F0;

    public VoidOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(VoidOrbEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        float scale = entity.getRenderScale(partialTick);
        float pulse = 1.0F + Mth.sin(age * 0.35F) * 0.05F;

        poseStack.pushPose();
        poseStack.scale(scale * pulse, scale * pulse, scale * pulse);
        poseStack.mulPose(Axis.YP.rotation(age * 0.07F));
        renderSphere(poseStack, bufferSource.getBuffer(RenderType.entityTranslucentEmissive(CORE_TEXTURE)),
                SPHERE_RADIUS, 0.88F, 0.95F, 1.0F, 235);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(scale * 1.12F, scale * 1.12F, scale * 1.12F);
        poseStack.mulPose(Axis.YP.rotation(-age * 0.09F));
        renderSphere(poseStack, bufferSource.getBuffer(RenderType.entityTranslucentEmissive(AURA_TEXTURE)),
                SPHERE_RADIUS, 0.55F, 0.70F, 1.0F, 145);
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
    public ResourceLocation getTextureLocation(VoidOrbEntity entity) {
        return CORE_TEXTURE;
    }
}
