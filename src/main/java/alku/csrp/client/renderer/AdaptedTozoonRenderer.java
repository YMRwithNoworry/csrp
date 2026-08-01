package alku.csrp.client.renderer;

import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.AdaptedVariantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/** Recreates the adapted Tozoon's visible dive and emergence around its underground movement. */
public final class AdaptedTozoonRenderer extends ParasiteGeoRenderer<AdaptedVariantEntity> {
    public AdaptedTozoonRenderer(EntityRendererProvider.Context context) {
        super(context, new PrimitiveParasiteModel<>("ada_tozoon"));
        shadowRadius = 0.7F;
    }

    @Override
    public boolean shouldRender(AdaptedVariantEntity entity, Frustum frustum,
            double cameraX, double cameraY, double cameraZ) {
        return !entity.isFullyBurrowed() && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public void preRender(PoseStack poseStack, AdaptedVariantEntity entity, BakedGeoModel model,
            MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, int colour) {
        poseStack.translate(0.0D, -entity.getBurrowDepth(partialTick) * 1.4D, 0.0D);
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
    }
}
