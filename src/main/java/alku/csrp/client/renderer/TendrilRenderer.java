package alku.csrp.client.renderer;

import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.client.model.TendrilModel;
import alku.csrp.entity.TendrilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class TendrilRenderer extends ParasiteGeoRenderer<TendrilEntity, TendrilModel> {
    public TendrilRenderer(EntityRendererProvider.Context context) {
        super(context, new TendrilModel());
        shadowRadius = 0.3F;
    }

    @Override
    protected void scale(LegacyMobRenderState state, PoseStack poseStack) {
        poseStack.rotateDegrees(Axis.YP, 180.0F);
        super.scale(state, poseStack);
    }
}
