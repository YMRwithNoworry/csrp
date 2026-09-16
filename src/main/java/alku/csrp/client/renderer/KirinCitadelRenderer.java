package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.KirinCitadelModel;
import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.entity.KirinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Citadel renderer for the original Tabula-exported Kirin model. */
public final class KirinCitadelRenderer
        extends MobRenderer<KirinEntity, LegacyMobRenderState, KirinCitadelModel> {
    private static final Identifier COSMIC_HACKING_TEXTURE = Identifier.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/layer/cosmichasking.png");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/kirin.png");
    private static final Identifier SHADOW_TEXTURE = Identifier.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/kirin_shadow.png");

    public KirinCitadelRenderer(EntityRendererProvider.Context context) {
        super(context, new KirinCitadelModel(), 1.1F);
        addLayer(new ShadowLayer(this));
        addLayer(new CosmicHackingLayer(this));
        addLayer(new LaserChargeLayer(this));
    }

    @Override
    public LegacyMobRenderState createRenderState() {
        return new LegacyMobRenderState();
    }

    @Override
    public void extractRenderState(KirinEntity entity, LegacyMobRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.legacyEntity = entity;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Identifier getTextureLocation(LegacyMobRenderState state) {
        KirinEntity entity = (KirinEntity) state.legacyEntity;
        return entity.isShadowClone() ? SHADOW_TEXTURE : TEXTURE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void submit(LegacyMobRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        KirinEntity entity = (KirinEntity) state.legacyEntity;
        poseStack.pushPose();
        if (entity.isShadowClone()) {
            poseStack.scale(1.2F, 1.2F, 1.2F);
        }
        super.submit(state, poseStack, collector, cameraState);
        poseStack.popPose();

        if (entity.isLaserFiring()) {
            Entity target = entity.level().getEntity(entity.getLaserTargetId());
            if (target instanceof LivingEntity living && living.isAlive()) {
                DerivedParasiteRenderer.renderKirinBeam(entity, living, state.partialTick, poseStack,
                        collector);
            }
        }
    }

    private static final class ShadowLayer extends RenderLayer<LegacyMobRenderState, KirinCitadelModel> {
        private ShadowLayer(KirinCitadelRenderer renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                LegacyMobRenderState state, float yRot, float xRot) {
            if (!(state.legacyEntity instanceof KirinEntity entity)) {
                return;
            }
            float alpha = entity.getShadowRenderAlpha(state.partialTick);
            if (entity.isShadowClone() || !entity.isShadowed() || alpha <= 0.0F) {
                return;
            }
            int alphaByte = Math.min(255, Math.max(0, Math.round(alpha * 255.0F)));
            int color = alphaByte << 24 | 0xFFFFFF;
            RenderType shadowRenderType = RenderTypes.entityTranslucent(SHADOW_TEXTURE);
            poseStack.pushPose();
            poseStack.scale(1.2F, 1.2F, 1.2F);
            collector.submitModel(getParentModel(), state, poseStack, shadowRenderType, lightCoords,
                    OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
            poseStack.popPose();
        }
    }

    private static final class CosmicHackingLayer
            extends RenderLayer<LegacyMobRenderState, KirinCitadelModel> {
        private CosmicHackingLayer(KirinCitadelRenderer renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                LegacyMobRenderState state, float yRot, float xRot) {
            if (!(state.legacyEntity instanceof KirinEntity entity)) {
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

    private static final class LaserChargeLayer
            extends RenderLayer<LegacyMobRenderState, KirinCitadelModel> {
        private final KirinCitadelRenderer renderer;

        private LaserChargeLayer(KirinCitadelRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                LegacyMobRenderState state, float yRot, float xRot) {
            if (!(state.legacyEntity instanceof KirinEntity entity) || !entity.isLaserCharging()) {
                return;
            }
            RenderType glowType = RenderTypes
                    .entityTranslucentEmissive(renderer.getTextureLocation(state));
            collector.submitModel(getParentModel(), state, poseStack, glowType,
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0x99FF48C4, null,
                    state.outlineColor);
        }
    }
}
