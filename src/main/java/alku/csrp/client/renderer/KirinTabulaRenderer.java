package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.KirinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Kirin renderer: adds the original {@code RenderKirin#renderJudgementCutAura} on top of the shared
 * derived-parasite Tabula renderer.
 */
public final class KirinTabulaRenderer extends DerivedTabulaRenderer<KirinEntity> {
    private KirinTabulaRenderer(EntityRendererProvider.Context context, String modelId, float shadowRadius) {
        super(context, modelId, shadowRadius);
    }

    public static KirinTabulaRenderer create(EntityRendererProvider.Context context, String modelId,
            float shadowRadius) {
        KirinTabulaRenderer renderer = new KirinTabulaRenderer(context, modelId, shadowRadius);
        renderer.addLayer(new JudgementCutAuraLayer(renderer));
        return renderer;
    }

    @Override
    protected void applyScale(KirinEntity entity, PoseStack poseStack, float partialTick) {
        super.applyScale(entity, poseStack, partialTick);
        poseStack.translate(0.0D, -1.75D, 0.0D);
    }

    /**
     * Original aura: an untextured additive white copy of the model that fades in and pulses over
     * the 60-tick judgement cut charge, then expands and fades out over the 24-tick trailing aura.
     */
    private static final class JudgementCutAuraLayer
            extends RenderLayer<KirinEntity, EntityModel<KirinEntity>> {
        private static final ResourceLocation WHITE = new ResourceLocation(Csrp.MODID,
                "textures/entity/layer/white.png");
        private static final float CHARGE_TICKS = 60.0F;
        private static final float FADE_IN_FRACTION = 0.28F;
        private static final float AURA_TICKS = 24.0F;

        private JudgementCutAuraLayer(RenderLayerParent<KirinEntity, EntityModel<KirinEntity>> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                KirinEntity entity, float limbSwing, float limbSwingAmount, float partialTick,
                float ageInTicks, float netHeadYaw, float headPitch) {
            if (!entity.isChargingJudgementCut()) {
                return;
            }

            float alpha;
            float scale;
            float chargeTicks = entity.getJudgementCutChargeTicks();
            if (chargeTicks > 0.0F) {
                float progress = 1.0F - Mth.clamp(chargeTicks / CHARGE_TICKS, 0.0F, 1.0F);
                float fadeIn = smoothstep(Mth.clamp(progress / FADE_IN_FRACTION, 0.0F, 1.0F));
                float pulse = 0.5F + 0.5F * Mth.sin((entity.tickCount + progress * 20.0F) * 0.45F);
                alpha = (0.06F + pulse * 0.11F) * fadeIn;
                scale = 1.015F + progress * 0.085F + pulse * 0.01F;
            } else {
                float endProgress = smoothstep(
                        1.0F - Mth.clamp(entity.getJudgementCutAuraEndTicks() / AURA_TICKS, 0.0F, 1.0F));
                alpha = 0.22F * (1.0F - endProgress);
                scale = 1.1F + endProgress * 0.9F;
            }
            if (alpha <= 0.01F) {
                return;
            }

            // The original disabled texturing and used additive blending; a flat white texture with
            // vanilla's additive, unlit "eyes" render type reproduces that state.
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.eyes(WHITE));
            poseStack.pushPose();
            poseStack.scale(scale, scale, scale);
            getParentModel().renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
            poseStack.popPose();
        }

        private static float smoothstep(float t) {
            return t * t * (3.0F - 2.0F * t);
        }
    }
}
