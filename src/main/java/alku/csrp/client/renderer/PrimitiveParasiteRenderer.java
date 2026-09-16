package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.animation.CitadelAnimatedEntity;
import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.CarrierEntity;
import alku.csrp.entity.MeltableAssimilated;
import alku.csrp.entity.PrimitiveVariantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class PrimitiveParasiteRenderer<T extends Mob & CitadelAnimatedEntity>
        extends ParasiteGeoRenderer<T, PrimitiveParasiteModel<T>> {
    private static final Identifier YELLOWEYE_GLOW_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/pri_yelloweye_glow.png");
    private static final Identifier YELLOWEYE_HEAVY_GLOW_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/pri_yelloweye_heavy_glow.png");
    private static final Identifier GUARDIAN_BEAM_TEXTURE = Identifier.withDefaultNamespace(
            "textures/entity/guardian_beam.png");
    private static final RenderType GUARDIAN_BEAM_RENDER_TYPE = RenderTypes.entityTranslucentEmissive(
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
    @SuppressWarnings("unchecked")
    protected void scale(LegacyMobRenderState state, PoseStack poseStack) {
        T entity = (T) state.legacyEntity;
        float partialTick = state.partialTick;
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
        super.scale(state, poseStack);
    }

    @Override
    public void submit(LegacyMobRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        if (!(state.legacyEntity instanceof AdaptedVariantEntity arachnida) || !arachnida.isAdaptedArachnida()
                || arachnida.getArachnidaStatus() != 3) {
            return;
        }
        LivingEntity target = arachnida.getArachnidaTetherTarget();
        if (target != null) {
            renderArachnidaBeam(arachnida, target, state.partialTick, poseStack, collector);
        }
    }

    private static void renderArachnidaBeam(AdaptedVariantEntity arachnida, LivingEntity target,
                                             float partialTick, PoseStack poseStack,
                                             SubmitNodeCollector collector) {
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
        poseStack.rotateDegrees(Axis.YP, (Mth.HALF_PI - yaw) * Mth.RAD_TO_DEG);
        poseStack.rotateDegrees(Axis.XP, pitch * Mth.RAD_TO_DEG);

        collector.submitCustomGeometry(poseStack, GUARDIAN_BEAM_RENDER_TYPE, (pose, consumer) -> {
            renderBeamRibbon(consumer, pose, spin, beamLength, startV, endV);
            renderBeamRibbon(consumer, pose, spin + Mth.HALF_PI, beamLength, startV, endV);
            renderBeamCap(consumer, pose, spin, beamLength, (arachnida.tickCount & 1) == 0 ? 0.5F : 0.0F);
        });
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
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static final class YelloweyeGlowLayer<T extends Mob & CitadelAnimatedEntity>
            extends RenderLayer<LegacyMobRenderState, PrimitiveParasiteModel<T>> {
        private YelloweyeGlowLayer(PrimitiveParasiteRenderer<T> renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                LegacyMobRenderState state, float yRot, float xRot) {
            if (!(state.legacyEntity instanceof PrimitiveVariantEntity yelloweye)
                    || !yelloweye.isPrimitiveYelloweye()) {
                return;
            }
            Identifier texture = yelloweye.getYelloweyeSkin() == 7
                    ? YELLOWEYE_HEAVY_GLOW_TEXTURE : YELLOWEYE_GLOW_TEXTURE;
            RenderType glowType = RenderTypes.eyes(texture);
            collector.submitModel(getParentModel(), state, poseStack, glowType, LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, null, state.outlineColor);
        }
    }
}
