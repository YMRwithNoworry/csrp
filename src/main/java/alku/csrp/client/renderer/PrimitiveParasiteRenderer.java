package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.MeltableAssimilated;
import alku.csrp.entity.PrimitiveVariantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class PrimitiveParasiteRenderer<T extends Mob & GeoEntity> extends ParasiteGeoRenderer<T> {
    private static final ResourceLocation YELLOWEYE_GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/pri_yelloweye_glow.png");
    private static final ResourceLocation YELLOWEYE_HEAVY_GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/pri_yelloweye_heavy_glow.png");

    public PrimitiveParasiteRenderer(EntityRendererProvider.Context context, String id, float shadowRadius) {
        super(context, new PrimitiveParasiteModel<>(id));
        this.shadowRadius = shadowRadius;
        if ("pri_yelloweye".equals(id)) {
            addRenderLayer(new YelloweyeGlowLayer<>(this));
        }
    }

    @Override
    public void preRender(PoseStack poseStack, T entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        if (entity instanceof MeltableAssimilated meltable && meltable.isMelting()) {
            poseStack.scale(1.0F, meltable.getMeltRenderScale(partialTick), 1.0F);
        }
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
    }

    private static final class YelloweyeGlowLayer<T extends Mob & GeoEntity> extends GeoRenderLayer<T> {
        private YelloweyeGlowLayer(PrimitiveParasiteRenderer<T> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, RenderType renderType,
                           MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                           int packedLight, int packedOverlay) {
            if (!(entity instanceof PrimitiveVariantEntity yelloweye) || !yelloweye.isPrimitiveYelloweye()) {
                return;
            }
            ResourceLocation texture = yelloweye.getYelloweyeSkin() == 7
                    ? YELLOWEYE_HEAVY_GLOW_TEXTURE : YELLOWEYE_GLOW_TEXTURE;
            RenderType glowType = RenderType.eyes(texture);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, glowType,
                    bufferSource.getBuffer(glowType), partialTick, LightTexture.FULL_BRIGHT,
                    packedOverlay, 0xFFFFFFFF);
        }
    }
}
