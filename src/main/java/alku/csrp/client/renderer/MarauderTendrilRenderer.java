package alku.csrp.client.renderer;

import alku.csrp.client.model.MarauderTendrilModel;
import alku.csrp.entity.MarauderTendrilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Attached tendrils are hitboxes only; the body model already renders them. */
public final class MarauderTendrilRenderer extends GeoEntityRenderer<MarauderTendrilEntity> {
    public MarauderTendrilRenderer(EntityRendererProvider.Context context) {
        super(context, new MarauderTendrilModel());
        shadowRadius = 0.2F;
    }

    @Override
    public void render(MarauderTendrilEntity tendril, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (!tendril.isAttached()) {
            super.render(tendril, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }
}
