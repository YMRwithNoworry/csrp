package alku.csrp.client.renderer;

import alku.csrp.client.model.SimHumanModel;
import alku.csrp.entity.SimHumanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * SimHuman (特殊人形感染体) 的渲染器
 */
public final class SimHumanRenderer extends ParasiteGeoRenderer<SimHumanEntity> {
    public SimHumanRenderer(EntityRendererProvider.Context context) {
        super(context, new SimHumanModel());
        this.shadowRadius = 0.6F;
    }

    @Override
    public void preRender(PoseStack poseStack, SimHumanEntity entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        if (entity.isMelting()) {
            poseStack.scale(1.0F, entity.getMeltRenderScale(partialTick), 1.0F);
        }
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
    }
}
