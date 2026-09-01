package alku.csrp.client.renderer;

import alku.csrp.client.model.RupterModel;
import alku.csrp.entity.RupterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class RupterRenderer extends ParasiteGeoRenderer<RupterEntity> {
    public RupterRenderer(EntityRendererProvider.Context context) {
        super(context, new RupterModel());
        this.shadowRadius = 0.45F;
    }

    @Override
    public void render(RupterEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        if (entity.isOverheatCharging()) {
            double time = entity.tickCount + partialTick;
            poseStack.translate(
                    Math.sin(time * 3.7D) * 0.09D,
                    Math.sin(time * 5.3D) * 0.045D,
                    Math.cos(time * 4.1D) * 0.09D);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
