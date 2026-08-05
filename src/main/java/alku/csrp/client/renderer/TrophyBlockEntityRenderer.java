package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.block.TrophyBlock;
import alku.csrp.block.entity.TrophyBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renders the animated Void/Boom Orb suspended above its trophy base. */
public final class TrophyBlockEntityRenderer implements BlockEntityRenderer<TrophyBlockEntity> {
    private static final ResourceLocation VOID_CORE = texture("orbvoid.png");
    private static final ResourceLocation VOID_AURA = texture("orbvoid_armor.png");
    private static final ResourceLocation BOOM_CORE = texture("orbboom.png");
    private static final ResourceLocation BOOM_AURA = texture("orbboom_armor.png");
    private static final int STACKS = 14;
    private static final int SLICES = 14;
    private static final int FULL_BRIGHT = 0xF000F0;

    public TrophyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TrophyBlockEntity trophy, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(trophy.getBlockState().getBlock() instanceof TrophyBlock block)) {
            return;
        }
        boolean voidOrb = block.kind() == TrophyBlock.Kind.VOID;
        float age = (trophy.getLevel() == null ? 0L : trophy.getLevel().getGameTime()) + partialTick;
        float pulse = 1.0F + Mth.sin(age * 0.12F) * 0.05F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.35D, 0.5D);
        poseStack.mulPose(Axis.YP.rotation(age * 0.035F));
        poseStack.scale(pulse, pulse, pulse);
        renderSphere(poseStack, bufferSource.getBuffer(RenderType.entityTranslucentEmissive(
                voidOrb ? VOID_CORE : BOOM_CORE)), 0.34F, 235);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.35D, 0.5D);
        poseStack.mulPose(Axis.YP.rotation(-age * 0.05F));
        renderSphere(poseStack, bufferSource.getBuffer(RenderType.entityTranslucentEmissive(
                voidOrb ? VOID_AURA : BOOM_AURA)), 0.39F, 125);
        poseStack.popPose();
    }

    private static void renderSphere(PoseStack poseStack, VertexConsumer consumer, float radius, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        for (int stack = 0; stack < STACKS; stack++) {
            float v0 = stack / (float) STACKS;
            float v1 = (stack + 1) / (float) STACKS;
            float phi0 = (float) Math.PI * v0;
            float phi1 = (float) Math.PI * v1;
            for (int slice = 0; slice < SLICES; slice++) {
                float u0 = slice / (float) SLICES;
                float u1 = (slice + 1) / (float) SLICES;
                float theta0 = (float) (Math.PI * 2.0D * u0);
                float theta1 = (float) (Math.PI * 2.0D * u1);
                vertex(pose, consumer, radius, phi0, theta0, u0, v0, alpha);
                vertex(pose, consumer, radius, phi1, theta0, u0, v1, alpha);
                vertex(pose, consumer, radius, phi1, theta1, u1, v1, alpha);
                vertex(pose, consumer, radius, phi0, theta1, u1, v0, alpha);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float radius,
            float phi, float theta, float u, float v, int alpha) {
        float x = Mth.sin(phi) * Mth.cos(theta);
        float y = Mth.cos(phi);
        float z = Mth.sin(phi) * Mth.sin(theta);
        consumer.addVertex(pose, x * radius, y * radius, z * radius)
                .setColor(255, 255, 255, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT)
                .setNormal(pose, x, y, z);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/" + name);
    }
}
