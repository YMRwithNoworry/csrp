package alku.csrp.client.renderer;

import alku.csrp.client.model.MarauderTendrilModel;
import alku.csrp.entity.MarauderTendrilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Attached tendrils are hitboxes only; the body model already renders them. */
public final class MarauderTendrilRenderer extends GeoEntityRenderer<MarauderTendrilEntity> {
    public MarauderTendrilRenderer(EntityRendererProvider.Context context) {
        super(context, new MarauderTendrilModel());
        shadowRadius = 0.2F;
    }

    @Override
    public void preRender(PoseStack poseStack, MarauderTendrilEntity entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
    }

    @Override
    public void render(MarauderTendrilEntity tendril, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (!tendril.isAttached()) {
            super.render(tendril, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }
}
