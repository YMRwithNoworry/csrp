package alku.csrp.client.renderer;

import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.client.model.RupterModel;
import alku.csrp.entity.RupterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public final class RupterRenderer extends ParasiteGeoRenderer<RupterEntity, RupterModel> {
    public RupterRenderer(EntityRendererProvider.Context context) {
        super(context, new RupterModel());
        this.shadowRadius = 0.45F;
    }

    @Override
    public void submit(LegacyMobRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        RupterEntity entity = (RupterEntity) state.legacyEntity;
        poseStack.pushPose();
        if (entity.isOverheatCharging()) {
            double time = entity.tickCount + state.partialTick;
            poseStack.translate(
                    Math.sin(time * 3.7D) * 0.09D,
                    Math.sin(time * 5.3D) * 0.045D,
                    Math.cos(time * 4.1D) * 0.09D);
        }
        super.submit(state, poseStack, collector, cameraState);
        poseStack.popPose();
    }
}
