package alku.csrp.client.renderer;

import alku.csrp.client.model.AssimilatedParasiteModel;
import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.entity.AssimilatedParasiteEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class AssimilatedParasiteRenderer
        extends ParasiteGeoRenderer<AssimilatedParasiteEntity, AssimilatedParasiteModel> {
    public AssimilatedParasiteRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new AssimilatedParasiteModel());
        this.shadowRadius = shadowRadius;
    }

    @Override
    protected void scale(LegacyMobRenderState state, PoseStack poseStack) {
        AssimilatedParasiteEntity entity = (AssimilatedParasiteEntity) state.legacyEntity;
        if (entity.isMelting()) {
            poseStack.scale(1.0F, entity.getMeltRenderScale(state.partialTick), 1.0F);
        }
        super.scale(state, poseStack);
    }
}
