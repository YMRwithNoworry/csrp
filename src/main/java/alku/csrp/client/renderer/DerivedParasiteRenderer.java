package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.DerivedParasiteEntity;
import alku.csrp.entity.KirinEntity;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Restores the translucent cosmical shadow pass used by legacy derived parasites. */
public final class DerivedParasiteRenderer<T extends DerivedParasiteEntity>
        extends ParasiteGeoRenderer<T, PrimitiveParasiteModel<T>> {
    private static final Identifier COSMIC_HACKING_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/layer/cosmichasking.png");
    /**
     * Vanilla moved the guardian beam texture into a per-mob folder in 26.3
     * ({@code textures/entity/guardian/guardian_beam.png}).  The pre-26.3 path resolves to nothing, which
     * made the Kirin's target beam render as the missing-texture checkerboard.
     */
    private static final Identifier GUARDIAN_BEAM_TEXTURE = Identifier.withDefaultNamespace(
            "textures/entity/guardian/guardian_beam.png");
    private static final RenderType GUARDIAN_BEAM_RENDER_TYPE = RenderTypes.entityTranslucentEmissive(
            GUARDIAN_BEAM_TEXTURE);
    private static final int BEAM_SIDES = 8;
    private static final float BEAM_RADIUS = 0.282F;
    private static final int BEAM_RED = 78;
    private static final int BEAM_GREEN = 156;
    private static final int BEAM_BLUE = 250;
    private static final int KIRIN_BEAM_RED = 255;
    private static final int KIRIN_BEAM_GREEN = 72;
    private static final int KIRIN_BEAM_BLUE = 196;

    private final Identifier shadowTexture;

    public DerivedParasiteRenderer(EntityRendererProvider.Context context, String id, String shadowTexture,
            float shadowRadius) {
        super(context, new PrimitiveParasiteModel<>(id));
        this.shadowTexture = Identifier.fromNamespaceAndPath(Csrp.MODID,
                "textures/entity/" + shadowTexture + ".png");
        this.shadowRadius = shadowRadius;
        addLayer(new ShadowLayer<>(this, this.shadowTexture));
        addLayer(new CosmicHackingLayer<>(this));
        addLayer(new KirinLaserChargeLayer<>(this));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Identifier getTextureLocation(LegacyMobRenderState state) {
        T entity = (T) state.legacyEntity;
        return entity.isShadowClone() ? shadowTexture : super.getTextureLocation(state);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RenderType getRenderType(LegacyMobRenderState state, boolean bodyVisible, boolean forceTransparent,
            boolean appearGlowing) {
        // Iris cannot reliably map NeoForge's unlit translucent shader used by the shared model.
        // The normal derived textures are binary-alpha, so keep the body on the vanilla entity
        // cutout path while reserving translucency for the actual shadow clone/effect passes.
        T entity = (T) state.legacyEntity;
        return entity.isShadowClone() ? RenderTypes.entityTranslucent(shadowTexture)
                : super.getRenderType(state, bodyVisible, forceTransparent, appearGlowing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void submit(LegacyMobRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        T entity = (T) state.legacyEntity;
        poseStack.pushPose();
        if (entity.isShadowClone()) {
            poseStack.scale(1.2F, 1.2F, 1.2F);
        }
        super.submit(state, poseStack, collector, cameraState);
        poseStack.popPose();

        float partialTick = state.partialTick;
        if (entity.isShadowed() && !entity.isShadowClone()) {
            for (int targetId : entity.getNeuralTargetIds()) {
                Entity target = entity.level().getEntity(targetId);
                if (target instanceof LivingEntity living && living.isAlive()) {
                    renderNeuralBeam(entity, living, partialTick, poseStack, collector);
                }
            }
        }
        if (entity instanceof KirinEntity kirin && kirin.isLaserFiring()) {
            Entity target = entity.level().getEntity(kirin.getLaserTargetId());
            if (target instanceof LivingEntity living && living.isAlive()) {
                renderBeam(entity, living, partialTick, poseStack, collector,
                        KIRIN_BEAM_RED, KIRIN_BEAM_GREEN, KIRIN_BEAM_BLUE, BEAM_RADIUS * 1.35F);
            }
        }
    }

    private static void renderNeuralBeam(DerivedParasiteEntity parasite, LivingEntity target, float partialTick,
            PoseStack poseStack, SubmitNodeCollector collector) {
        renderBeam(parasite, target, partialTick, poseStack, collector,
                BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_RADIUS);
    }

    static void renderKirinBeam(DerivedParasiteEntity parasite, LivingEntity target, float partialTick,
            PoseStack poseStack, SubmitNodeCollector collector) {
        renderBeam(parasite, target, partialTick, poseStack, collector,
                KIRIN_BEAM_RED, KIRIN_BEAM_GREEN, KIRIN_BEAM_BLUE, BEAM_RADIUS * 1.35F);
    }

    private static void renderBeam(DerivedParasiteEntity parasite, LivingEntity target, float partialTick,
            PoseStack poseStack, SubmitNodeCollector collector, int red, int green, int blue, float radius) {
        Vec3 renderOrigin = parasite.getPosition(partialTick);
        Vec3 start = parasite.getEyePosition(partialTick).subtract(renderOrigin);
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
        float age = parasite.tickCount + partialTick;
        float textureOffset = age * 0.5F % 1.0F;
        float startV = -1.0F + textureOffset;
        float endV = beamLength * 2.5F + startV;

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
        poseStack.rotateDegrees(Axis.YP, (Mth.HALF_PI - yaw) * Mth.RAD_TO_DEG);
        poseStack.rotateDegrees(Axis.XP, pitch * Mth.RAD_TO_DEG);

        float spin = age * -0.075F;
        collector.submitCustomGeometry(poseStack, GUARDIAN_BEAM_RENDER_TYPE, (pose, consumer) -> {
            for (int side = 0; side < BEAM_SIDES; side++) {
                float progress = side / (float) BEAM_SIDES;
                float nextProgress = (side + 1) / (float) BEAM_SIDES;
                float angle = spin + progress * Mth.TWO_PI;
                float nextAngle = spin + nextProgress * Mth.TWO_PI;
                float x = Mth.cos(angle) * radius;
                float z = Mth.sin(angle) * radius;
                float nextX = Mth.cos(nextAngle) * radius;
                float nextZ = Mth.sin(nextAngle) * radius;

                beamVertex(consumer, pose, x, beamLength, z, progress, endV, red, green, blue);
                beamVertex(consumer, pose, x, 0.0F, z, progress, startV, red, green, blue);
                beamVertex(consumer, pose, nextX, 0.0F, nextZ, nextProgress, startV, red, green, blue);
                beamVertex(consumer, pose, nextX, beamLength, nextZ, nextProgress, endV, red, green, blue);
            }
        });
        poseStack.popPose();
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int red, int green, int blue) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static final class ShadowLayer<T extends DerivedParasiteEntity>
            extends RenderLayer<LegacyMobRenderState, PrimitiveParasiteModel<T>> {
        private final Identifier texture;

        private ShadowLayer(DerivedParasiteRenderer<T> renderer, Identifier texture) {
            super(renderer);
            this.texture = texture;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                LegacyMobRenderState state, float yRot, float xRot) {
            if (!(state.legacyEntity instanceof DerivedParasiteEntity entity)) {
                return;
            }
            float alpha = entity.getShadowRenderAlpha(state.partialTick);
            if (entity.isShadowClone() || !entity.isShadowed() || alpha <= 0.0F) {
                return;
            }

            RenderType shadowRenderType = RenderTypes.entityTranslucent(texture);
            int alphaByte = Math.min(255, Math.max(0, Math.round(alpha * 255.0F)));
            int colour = alphaByte << 24 | 0xFFFFFF;
            poseStack.pushPose();
            poseStack.scale(1.2F, 1.2F, 1.2F);
            collector.submitModel(getParentModel(), state, poseStack, shadowRenderType, lightCoords,
                    OverlayTexture.NO_OVERLAY, colour, null, state.outlineColor);
            poseStack.popPose();
        }
    }

    private static final class CosmicHackingLayer<T extends DerivedParasiteEntity>
            extends RenderLayer<LegacyMobRenderState, PrimitiveParasiteModel<T>> {
        private CosmicHackingLayer(DerivedParasiteRenderer<T> renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                LegacyMobRenderState state, float yRot, float xRot) {
            if (!(state.legacyEntity instanceof DerivedParasiteEntity entity)) {
                return;
            }
            if (!entity.isShadowed() || entity.isShadowClone() || !entity.isNeuralLinkActive()) {
                return;
            }

            float age = entity.tickCount + state.partialTick;
            RenderType hackingRenderType = RenderTypes.energySwirl(COSMIC_HACKING_TEXTURE,
                    age * 0.01F, age * 0.01F);
            collector.submitModel(getParentModel(), state, poseStack, hackingRenderType,
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFF80FF, null,
                    state.outlineColor);
        }
    }

    private static final class KirinLaserChargeLayer<T extends DerivedParasiteEntity>
            extends RenderLayer<LegacyMobRenderState, PrimitiveParasiteModel<T>> {
        private final DerivedParasiteRenderer<T> renderer;

        private KirinLaserChargeLayer(DerivedParasiteRenderer<T> renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                LegacyMobRenderState state, float yRot, float xRot) {
            if (!(state.legacyEntity instanceof KirinEntity kirin) || !kirin.isLaserCharging()) {
                return;
            }
            Identifier texture = renderer.getTextureLocation(state);
            RenderType glowType = RenderTypes.entityTranslucentEmissive(texture);
            collector.submitModel(getParentModel(), state, poseStack, glowType, LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 0x99FF48C4, null, state.outlineColor);
        }
    }
}
