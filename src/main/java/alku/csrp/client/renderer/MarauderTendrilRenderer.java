package alku.csrp.client.renderer;

import alku.csrp.client.model.MarauderTendrilModel;
import alku.csrp.entity.MarauderTendrilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Attached tendrils are hitboxes only; the body model already renders them. */
public final class MarauderTendrilRenderer extends ParasiteGeoRenderer<MarauderTendrilEntity> {
    public MarauderTendrilRenderer(EntityRendererProvider.Context context) {
        super(context, new MarauderTendrilModel());
        shadowRadius = 0.2F;
    }

    @Override
    protected void scale(MarauderTendrilEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.rotateDegrees(Axis.YP, 180.0F);
        super.scale(entity, poseStack, partialTick);
    }

    @Override
    public boolean shouldRender(MarauderTendrilEntity tendril, Frustum frustum, double cameraX, double cameraY,
                                double cameraZ, float partialTick) {
        return !tendril.isAttached()
                && super.shouldRender(tendril, frustum, cameraX, cameraY, cameraZ, partialTick);
    }
}
