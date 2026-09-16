package alku.csrp.client.renderer;

import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.client.model.MarauderTendrilModel;
import alku.csrp.entity.MarauderTendrilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Attached tendrils are hitboxes only; the body model already renders them. */
public final class MarauderTendrilRenderer
        extends ParasiteGeoRenderer<MarauderTendrilEntity, MarauderTendrilModel> {
    public MarauderTendrilRenderer(EntityRendererProvider.Context context) {
        super(context, new MarauderTendrilModel());
        shadowRadius = 0.2F;
    }

    @Override
    protected void scale(LegacyMobRenderState state, PoseStack poseStack) {
        poseStack.rotateDegrees(Axis.YP, 180.0F);
        super.scale(state, poseStack);
    }

    @Override
    public boolean shouldRender(MarauderTendrilEntity tendril, Frustum frustum, double cameraX, double cameraY,
                                double cameraZ, float partialTick) {
        return !tendril.isAttached()
                && super.shouldRender(tendril, frustum, cameraX, cameraY, cameraZ, partialTick);
    }
}
