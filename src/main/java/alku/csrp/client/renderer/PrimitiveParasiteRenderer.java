package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.animation.CitadelAnimatedEntity;
import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.CarrierEntity;
import alku.csrp.entity.MeltableAssimilated;
import alku.csrp.entity.PrimitiveVariantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class PrimitiveParasiteRenderer<T extends Mob & CitadelAnimatedEntity>
        extends ParasiteGeoRenderer<T> {
    private static final ResourceLocation YELLOWEYE_GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/pri_yelloweye_glow.png");
    private static final ResourceLocation YELLOWEYE_HEAVY_GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/pri_yelloweye_heavy_glow.png");
    private static final ResourceLocation GUARDIAN_BEAM_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/guardian_beam.png");
    private static final RenderType GUARDIAN_BEAM_RENDER_TYPE = RenderType.entityTranslucentEmissive(
            GUARDIAN_BEAM_TEXTURE);
    private static final float BEAM_RADIUS = 0.2F;
    private static final int BEAM_RED = 220;
    private static final int BEAM_GREEN = 188;
    private static final int BEAM_BLUE = 128;

    public PrimitiveParasiteRenderer(EntityRendererProvider.Context context, String id, float shadowRadius) {
        super(context, new PrimitiveParasiteModel<>(id));
        this.shadowRadius = shadowRadius;
        if ("pri_yelloweye".equals(id)) {
            addLayer(new YelloweyeGlowLayer<>(this));
        }
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        if (entity instanceof MeltableAssimilated meltable && meltable.isMelting()) {
            poseStack.scale(1.0F, meltable.getMeltRenderScale(partialTick), 1.0F);
        }
        if (entity instanceof CarrierEntity carrier) {
            float swell = carrier.getSwellProgress(partialTick);
            float pulse = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
            swell = Mth.clamp(swell, 0.0F, 1.0F);
            swell *= swell;
            swell *= swell;
            float horizontalScale = (1.0F + swell * 0.4F) * pulse;
            float verticalScale = (1.0F + swell * 0.1F) / pulse;
            poseStack.scale(horizontalScale, verticalScale, horizontalScale);
        }
        super.scale(entity, poseStack, partialTick);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        if (!(entity instanceof AdaptedVariantEntity arachnida) || !arachnida.isAdaptedArachnida()
                || arachnida.getArachnidaStatus() != 3) {
            return;
        }
        LivingEntity target = arachnida.getArachnidaTetherTarget();
        if (target != null) {
            renderArachnidaBeam(arachnida, target, partialTick, poseStack, bufferSource);
        }
    }

    private static void renderArachnidaBeam(AdaptedVariantEntity arachnida, LivingEntity target,
                                             float partialTick, PoseStack poseStack,
                                             MultiBufferSource bufferSource) {
        Vec3 renderOrigin = arachnida.getPosition(partialTick);
        Vec3 start = arachnida.getEyePosition(partialTick).subtract(renderOrigin);
        Vec3 end = target.getPosition(partialTick).add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(renderOrigin);
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        if (distance < 0.01D) {
            return;
        }

        Vec3 normalized = direction.scale(1.0D / distance);
        float beamLength = (float) distance + 1.0F;
        float pitch = (float) Math.acos(normalized.y);
        float yaw = (float) Math.atan2(normalized.z, normalized.x);
        float age = arachnida.tickCount + partialTick;
        float startV = -1.0F + age * 0.5F % 1.0F;
        float endV = beamLength * 2.5F + startV;
        float spin = age * -0.075F;

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
        poseStack.mulPose(Axis.YP.rotationDegrees((Mth.HALF_PI - yaw) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * Mth.RAD_TO_DEG));

        VertexConsumer consumer = bufferSource.getBuffer(GUARDIAN_BEAM_RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        renderBeamRibbon(consumer, pose, spin, beamLength, startV, endV);
        renderBeamRibbon(consumer, pose, spin + Mth.HALF_PI, beamLength, startV, endV);
        renderBeamCap(consumer, pose, spin, beamLength, (arachnida.tickCount & 1) == 0 ? 0.5F : 0.0F);
        poseStack.popPose();
    }

    private static void renderBeamRibbon(VertexConsumer consumer, PoseStack.Pose pose, float angle,
                                         float beamLength, float startV, float endV) {
        float x = Mth.cos(angle) * BEAM_RADIUS;
        float z = Mth.sin(angle) * BEAM_RADIUS;
        float oppositeX = -x;
        float oppositeZ = -z;
        beamVertex(consumer, pose, x, beamLength, z, 0.4999F, endV);
        beamVertex(consumer, pose, x, 0.0F, z, 0.4999F, startV);
        beamVertex(consumer, pose, oppositeX, 0.0F, oppositeZ, 0.0F, startV);
        beamVertex(consumer, pose, oppositeX, beamLength, oppositeZ, 0.0F, endV);
    }

    private static void renderBeamCap(VertexConsumer consumer, PoseStack.Pose pose, float spin,
                                      float beamLength, float vOffset) {
        float radius = 0.282F;
        beamVertex(consumer, pose, Mth.cos(spin + Mth.PI * 0.75F) * radius, beamLength,
                Mth.sin(spin + Mth.PI * 0.75F) * radius, 0.5F, vOffset + 0.5F);
        beamVertex(consumer, pose, Mth.cos(spin + Mth.PI * 0.25F) * radius, beamLength,
                Mth.sin(spin + Mth.PI * 0.25F) * radius, 1.0F, vOffset + 0.5F);
        beamVertex(consumer, pose, Mth.cos(spin + Mth.PI * 1.75F) * radius, beamLength,
                Mth.sin(spin + Mth.PI * 1.75F) * radius, 1.0F, vOffset);
        beamVertex(consumer, pose, Mth.cos(spin + Mth.PI * 1.25F) * radius, beamLength,
                Mth.sin(spin + Mth.PI * 1.25F) * radius, 0.5F, vOffset);
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                                   float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static final class YelloweyeGlowLayer<T extends Mob & CitadelAnimatedEntity>
            extends RenderLayer<T, AdvancedEntityModel<T>> {
        private YelloweyeGlowLayer(PrimitiveParasiteRenderer<T> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                T entity, float limbSwing, float limbSwingAmount, float partialTick,
                float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(entity instanceof PrimitiveVariantEntity yelloweye) || !yelloweye.isPrimitiveYelloweye()) {
                return;
            }
            ResourceLocation texture = yelloweye.getYelloweyeSkin() == 7
                    ? YELLOWEYE_HEAVY_GLOW_TEXTURE : YELLOWEYE_GLOW_TEXTURE;
            RenderType glowType = RenderType.eyes(texture);
            getParentModel().renderToBuffer(poseStack, bufferSource.getBuffer(glowType),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
    }
}
