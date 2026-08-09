package alku.csrp.client.renderer;

import alku.csrp.client.model.TendrilModel;
import alku.csrp.entity.TendrilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public final class TendrilRenderer extends ParasiteGeoRenderer<TendrilEntity> {
    public TendrilRenderer(EntityRendererProvider.Context context) {
        super(context, new TendrilModel());
        shadowRadius = 0.3F;
    }

    @Override
    public void preRender(PoseStack poseStack, TendrilEntity entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
    }
}
