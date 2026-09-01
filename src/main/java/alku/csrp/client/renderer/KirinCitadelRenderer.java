package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.KirinCitadelModel;
import alku.csrp.entity.KirinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Citadel renderer for the original Tabula-exported Kirin model. */
public final class KirinCitadelRenderer extends MobRenderer<KirinEntity, KirinCitadelModel> {
    private static final ResourceLocation COSMIC_HACKING_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/layer/cosmichasking.png");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/kirin.png");
    private static final ResourceLocation SHADOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/kirin_shadow.png");

    public KirinCitadelRenderer(EntityRendererProvider.Context context) {
        super(context, new KirinCitadelModel(), 1.1F);
        addLayer(new ShadowLayer(this));
        addLayer(new CosmicHackingLayer(this));
        addLayer(new LaserChargeLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(KirinEntity entity) {
        return entity.isShadowClone() ? SHADOW_TEXTURE : TEXTURE;
    }

    @Override
    public void render(KirinEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        if (entity.isShadowClone()) {
            poseStack.scale(1.2F, 1.2F, 1.2F);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();

        if (entity.isLaserFiring()) {
            Entity target = entity.level().getEntity(entity.getLaserTargetId());
            if (target instanceof LivingEntity living && living.isAlive()) {
                DerivedParasiteRenderer.renderKirinBeam(entity, living, partialTick, poseStack, bufferSource);
            }
        }
    }

    private static final class ShadowLayer extends RenderLayer<KirinEntity, KirinCitadelModel> {
        private ShadowLayer(KirinCitadelRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                KirinEntity entity, float limbSwing, float limbSwingAmount, float partialTick,
                float ageInTicks, float netHeadYaw, float headPitch) {
            float alpha = entity.getShadowRenderAlpha(partialTick);
            if (entity.isShadowClone() || !entity.isShadowed() || alpha <= 0.0F) {
                return;
            }
            int alphaByte = Math.min(255, Math.max(0, Math.round(alpha * 255.0F)));
            int color = alphaByte << 24 | 0xFFFFFF;
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(SHADOW_TEXTURE));
            poseStack.pushPose();
            poseStack.scale(1.2F, 1.2F, 1.2F);
            getParentModel().renderToBuffer(poseStack, consumer, packedLight,
                    LivingEntityRenderer.getOverlayCoords(entity, 0.0F), color);
            poseStack.popPose();
        }
    }

    private static final class CosmicHackingLayer extends RenderLayer<KirinEntity, KirinCitadelModel> {
        private CosmicHackingLayer(KirinCitadelRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                KirinEntity entity, float limbSwing, float limbSwingAmount, float partialTick,
                float ageInTicks, float netHeadYaw, float headPitch) {
            if (!entity.isShadowed() || entity.isShadowClone() || !entity.isNeuralLinkActive()) {
                return;
            }
            float age = entity.tickCount + partialTick;
            RenderType type = RenderType.energySwirl(COSMIC_HACKING_TEXTURE, age * 0.01F, age * 0.01F);
            getParentModel().renderToBuffer(poseStack, bufferSource.getBuffer(type),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFF80FF);
        }
    }

    private static final class LaserChargeLayer extends RenderLayer<KirinEntity, KirinCitadelModel> {
        private LaserChargeLayer(KirinCitadelRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                KirinEntity entity, float limbSwing, float limbSwingAmount, float partialTick,
                float ageInTicks, float netHeadYaw, float headPitch) {
            if (!entity.isLaserCharging()) {
                return;
            }
            RenderType type = RenderType.entityTranslucentEmissive(getTextureLocation(entity));
            getParentModel().renderToBuffer(poseStack, bufferSource.getBuffer(type),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0x99FF48C4);
        }
    }
}
