package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.DerivedParasiteEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/** Restores the translucent cosmical shadow pass used by legacy derived parasites. */
public final class DerivedParasiteRenderer<T extends DerivedParasiteEntity> extends ParasiteGeoRenderer<T> {
    private final ResourceLocation shadowTexture;

    public DerivedParasiteRenderer(EntityRendererProvider.Context context, String id, String shadowTexture,
            float shadowRadius) {
        super(context, new PrimitiveParasiteModel<>(id));
        this.shadowTexture = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                "textures/entity/" + shadowTexture + ".png");
        this.shadowRadius = shadowRadius;
        addRenderLayer(new ShadowLayer<>(this, this.shadowTexture));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.isShadowClone() ? shadowTexture : super.getTextureLocation(entity);
    }

    @Override
    public RenderType getRenderType(T entity, ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return entity.isShadowClone() ? RenderType.entityTranslucent(shadowTexture)
                : super.getRenderType(entity, texture, bufferSource, partialTick);
    }

    @Override
    public Color getRenderColor(T entity, float partialTick, int packedLight) {
        return entity.isShadowClone() ? Color.ofRGBA(255, 255, 255, 128)
                : super.getRenderColor(entity, partialTick, packedLight);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        if (entity.isShadowClone()) {
            poseStack.scale(1.2F, 1.2F, 1.2F);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    private static final class ShadowLayer<T extends DerivedParasiteEntity> extends GeoRenderLayer<T> {
        private final ResourceLocation texture;

        private ShadowLayer(DerivedParasiteRenderer<T> renderer, ResourceLocation texture) {
            super(renderer);
            this.texture = texture;
        }

        @Override
        public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, RenderType renderType,
                MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                int packedOverlay) {
            float alpha = entity.getShadowRenderAlpha(partialTick);
            if (entity.isShadowClone() || !entity.isShadowed() || alpha <= 0.0F) {
                return;
            }

            RenderType shadowRenderType = RenderType.entityTranslucent(texture);
            int alphaByte = Math.min(255, Math.max(0, Math.round(alpha * 255.0F)));
            int colour = alphaByte << 24 | 0xFFFFFF;
            poseStack.pushPose();
            poseStack.scale(1.2F, 1.2F, 1.2F);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, shadowRenderType,
                    bufferSource.getBuffer(shadowRenderType), partialTick, packedLight, packedOverlay, colour);
            poseStack.popPose();
        }
    }
}
