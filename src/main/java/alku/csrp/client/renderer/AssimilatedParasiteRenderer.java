package alku.csrp.client.renderer;

import alku.csrp.client.model.AssimilatedParasiteModel;
import alku.csrp.entity.AssimilatedParasiteEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class AssimilatedParasiteRenderer extends ParasiteGeoRenderer<AssimilatedParasiteEntity> {
    public AssimilatedParasiteRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new AssimilatedParasiteModel());
        this.shadowRadius = shadowRadius;
    }

    @Override
    protected void scale(AssimilatedParasiteEntity entity, PoseStack poseStack, float partialTick) {
        if (entity.isMelting()) {
            poseStack.scale(1.0F, entity.getMeltRenderScale(partialTick), 1.0F);
        }
        super.scale(entity, poseStack, partialTick);
    }
}
