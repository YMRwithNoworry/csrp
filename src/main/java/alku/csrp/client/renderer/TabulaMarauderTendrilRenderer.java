package alku.csrp.client.renderer;

import alku.csrp.entity.MarauderTendrilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Direct Tabula renderer for detached marauder tendrils. */
public final class TabulaMarauderTendrilRenderer extends TabulaMobRenderer<MarauderTendrilEntity> {
    public TabulaMarauderTendrilRenderer(EntityRendererProvider.Context context) {
        super(context, "marauder_tendril", 0.2F);
    }

    @Override
    public void render(MarauderTendrilEntity tendril, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (tendril.isAttached()) return;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        super.render(tendril, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
