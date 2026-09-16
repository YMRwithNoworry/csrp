package alku.csrp.client.renderer;

import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.client.model.SimHumanModel;
import alku.csrp.entity.SimHumanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * SimHuman (特殊人形感染体) 的渲染器
 */
public final class SimHumanRenderer extends ParasiteGeoRenderer<SimHumanEntity, SimHumanModel> {
    public SimHumanRenderer(EntityRendererProvider.Context context) {
        super(context, new SimHumanModel());
        this.shadowRadius = 0.6F;
    }

    @Override
    protected void scale(LegacyMobRenderState state, PoseStack poseStack) {
        SimHumanEntity entity = (SimHumanEntity) state.legacyEntity;
        if (entity.isMelting()) {
            poseStack.scale(1.0F, entity.getMeltRenderScale(state.partialTick), 1.0F);
        }
        super.scale(state, poseStack);
    }
}
