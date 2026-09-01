package alku.csrp.client.renderer;

import alku.csrp.client.model.TendrilModel;
import alku.csrp.entity.TendrilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class TendrilRenderer extends ParasiteGeoRenderer<TendrilEntity> {
    public TendrilRenderer(EntityRendererProvider.Context context) {
        super(context, new TendrilModel());
        shadowRadius = 0.3F;
    }

    @Override
    protected void scale(TendrilEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        super.scale(entity, poseStack, partialTick);
    }
}
