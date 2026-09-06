package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.AirscrewEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Direct Tabula renderer that retains Airscrew's guardian-style tethers. */
public final class TabulaAirscrewRenderer extends TabulaMobRenderer<AirscrewEntity> {
    private static final ResourceLocation TETHER_TEXTURE = new ResourceLocation(
            Csrp.MODID, "textures/entity/airscrew_tether.png");
    private static final RenderType TETHER_RENDER_TYPE = RenderType.entityTranslucentEmissive(TETHER_TEXTURE);
    private static final int TETHER_SIDES = 8;
    private static final float TETHER_RADIUS = 0.282F;

    public TabulaAirscrewRenderer(EntityRendererProvider.Context context) {
        super(context, "airscrew", 0.8F);
    }

    @Override
    public boolean shouldRender(AirscrewEntity airscrew, Frustum frustum, double cameraX, double cameraY,
                                double cameraZ) {
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.hasEffect(alku.csrp.registry.ModMobEffects.BRAINING.get())) {
            return false;
        }
        if (super.shouldRender(airscrew, frustum, cameraX, cameraY, cameraZ)) {
            return true;
        }
        Vec3 start = tetherStart(airscrew, 1.0F);
        for (LivingEntity target : airscrew.getPullTargetsForRendering()) {
            if (frustum.isVisible(new AABB(start, tetherEnd(target, 1.0F)).inflate(TETHER_RADIUS))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(AirscrewEntity airscrew, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(airscrew, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        for (LivingEntity target : airscrew.getPullTargetsForRendering()) {
            renderTether(airscrew, target, partialTick, poseStack, bufferSource);
        }
    }

    private static void renderTether(AirscrewEntity airscrew, LivingEntity target, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource) {
        Vec3 start = tetherStart(airscrew, partialTick);
        Vec3 end = tetherEnd(target, partialTick);
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        if (distance < 0.01D) return;

        Vec3 normalized = direction.scale(1.0D / distance);
        float beamLength = (float) distance + 1.0F;
        float pitch = (float) Math.acos(normalized.y);
        float yaw = (float) Math.atan2(normalized.z, normalized.x);
        float age = airscrew.tickCount + partialTick;
        float startV = -1.0F + age * 0.5F % 1.0F;
        float endV = beamLength * 2.5F + startV;
        float pulse = 0.82F + (Mth.sin(age * 0.5F) + 1.0F) * 0.09F;
        int red = (int) (255.0F * pulse);
        int green = (int) (223.0F * pulse);
        int blue = (int) (64.0F * pulse);

        poseStack.pushPose();
        poseStack.translate(0.0D, airscrew.getTetherMouthHeight(), 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees((Mth.HALF_PI - yaw) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * Mth.RAD_TO_DEG));
        VertexConsumer consumer = bufferSource.getBuffer(TETHER_RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        float spin = age * -0.075F;
        for (int side = 0; side < TETHER_SIDES; side++) {
            float progress = side / (float) TETHER_SIDES;
            float nextProgress = (side + 1) / (float) TETHER_SIDES;
            float angle = spin + progress * Mth.TWO_PI;
            float nextAngle = spin + nextProgress * Mth.TWO_PI;
            float x = Mth.cos(angle) * TETHER_RADIUS;
            float z = Mth.sin(angle) * TETHER_RADIUS;
            float nextX = Mth.cos(nextAngle) * TETHER_RADIUS;
            float nextZ = Mth.sin(nextAngle) * TETHER_RADIUS;
            vertex(consumer, pose, x, beamLength, z, red, green, blue, progress, endV);
            vertex(consumer, pose, x, 0.0F, z, red, green, blue, progress, startV);
            vertex(consumer, pose, nextX, 0.0F, nextZ, red, green, blue, nextProgress, startV);
            vertex(consumer, pose, nextX, beamLength, nextZ, red, green, blue, nextProgress, endV);
        }
        poseStack.popPose();
    }

    private static Vec3 tetherStart(AirscrewEntity airscrew, float partialTick) {
        return airscrew.getTetherMouthPosition(partialTick);
    }

    private static Vec3 tetherEnd(LivingEntity target, float partialTick) {
        return target.getPosition(partialTick).add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               int red, int green, int blue, float u, float v) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(red, green, blue, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }
}
