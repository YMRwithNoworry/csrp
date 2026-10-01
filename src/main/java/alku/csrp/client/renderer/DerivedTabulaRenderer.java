package alku.csrp.client.renderer;

import alku.csrp.client.model.tabula.TabulaTextureResolver;
import alku.csrp.entity.DerivedParasiteEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;

/**
 * Tabula renderer for derived parasites whose models are Java exports instead of GeckoLib geo files.
 *
 * <p>{@link DerivedParasiteRenderer} restores the legacy cosmical pass for GeckoLib derived mobs:
 * the shadow clone is drawn as a translucent silhouette, and a shadow-state mob periodically
 * flickers as a dark copy of itself. Kirin and Draconite render through {@code ModelTabula_*}
 * classes, so they need the same treatment here. Without it their shadow clones fall back to
 * {@link TabulaMobRenderer#getTextureLocation} and show up as a solid second copy of the original
 * mob, and the cosmical flicker is missing entirely.</p>
 */
public class DerivedTabulaRenderer<T extends DerivedParasiteEntity> extends TabulaMobRenderer<T> {
    /** Legacy {@code RenderCosmical.renderModel} drew clones with the cosmical texture at 0.5 alpha. */
    private static final float SHADOW_CLONE_ALPHA = 0.5F;
    /** Legacy {@code preRenderCallback}/{@code preRenderCallbackCosmical} scaled both passes by 1.2. */
    private static final float SHADOW_RENDER_SCALE = 1.2F;

    private final ResourceLocation shadowTexture;

    public DerivedTabulaRenderer(EntityRendererProvider.Context context, String modelId, float shadowRadius) {
        this(context, modelId, modelId + "_shadow", shadowRadius);
    }

    public DerivedTabulaRenderer(EntityRendererProvider.Context context, String modelId,
            String shadowTextureId, float shadowRadius) {
        super(context, modelId, shadowRadius);
        this.shadowTexture = TabulaTextureResolver.texture(shadowTextureId);
        addLayer(new ShadowFlickerLayer<>(this, this.shadowTexture));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.isShadowClone() ? shadowTexture : super.getTextureLocation(entity);
    }

    @Override
    protected RenderType getRenderType(T entity, boolean isBodyVisible, boolean isVisibleToPlayer,
            boolean isGlowing) {
        if (entity.isShadowClone()) {
            // Legacy RenderCosmical.renderModel only drew the clone while the mob itself would draw.
            if (isBodyVisible || isVisibleToPlayer) {
                // Unlit silhouette: no culling so the black cosmical texture reads as a flat shadow.
                return RenderType.entityTranslucent(shadowTexture);
            }
            return isGlowing ? RenderType.outline(shadowTexture) : null;
        }
        return super.getRenderType(entity, isBodyVisible, isVisibleToPlayer, isGlowing);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isShadowClone()) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        // The vanilla model pipeline always writes alpha 1, so the translucent clone alpha has to be
        // injected into the vertices instead of the render type.
        poseStack.pushPose();
        poseStack.scale(SHADOW_RENDER_SCALE, SHADOW_RENDER_SCALE, SHADOW_RENDER_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack,
                new ShadowAlphaBufferSource(bufferSource, SHADOW_CLONE_ALPHA), packedLight);
        poseStack.popPose();
    }

    /**
     * Periodic cosmical pass for the non-clone mob: while a derived parasite sits in its shadow
     * state the legacy renderer re-drew the model with the cosmical texture at
     * {@link DerivedParasiteEntity#getShadowRenderAlpha(float)}, which is what makes the mob flicker
     * as a dark silhouette.
     */
    private static final class ShadowFlickerLayer<T extends DerivedParasiteEntity>
            extends RenderLayer<T, EntityModel<T>> {
        private final ResourceLocation shadowTexture;

        private ShadowFlickerLayer(RenderLayerParent<T, EntityModel<T>> renderer,
                ResourceLocation shadowTexture) {
            super(renderer);
            this.shadowTexture = shadowTexture;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                float netHeadYaw, float headPitch) {
            float alpha = entity.getShadowRenderAlpha(partialTick);
            // Vanilla calls layers even for invisible entities, so skip those to avoid leaving a
            // free-floating black silhouette behind.
            if (entity.isInvisible() || entity.isShadowClone() || !entity.isShadowed()
                    || alpha <= 0.0F) {
                return;
            }

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(shadowTexture));
            poseStack.pushPose();
            poseStack.scale(SHADOW_RENDER_SCALE, SHADOW_RENDER_SCALE, SHADOW_RENDER_SCALE);
            getParentModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, alpha);
            poseStack.popPose();
        }
    }

    private static final class ShadowAlphaBufferSource implements MultiBufferSource {
        private final MultiBufferSource parent;
        private final float alpha;

        private ShadowAlphaBufferSource(MultiBufferSource parent, float alpha) {
            this.parent = parent;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new ShadowAlphaVertexConsumer(parent.getBuffer(renderType), alpha);
        }
    }

    private static final class ShadowAlphaVertexConsumer extends VertexConsumerWrapper {
        private final float alpha;

        private ShadowAlphaVertexConsumer(VertexConsumer parent, float alpha) {
            super(parent);
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return super.color(red, green, blue, scale(alpha));
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            super.defaultColor(red, green, blue, scale(alpha));
        }

        private int scale(int alpha) {
            return Mth.clamp(Math.round(alpha * this.alpha), 0, 255);
        }
    }
}
