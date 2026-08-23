package alku.csrp.client.renderer;

import alku.csrp.client.model.AssimilatedParasiteModel;
import alku.csrp.entity.AssimilatedParasiteEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public final class AssimilatedParasiteRenderer extends ParasiteGeoRenderer<AssimilatedParasiteEntity> {
    public AssimilatedParasiteRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new AssimilatedParasiteModel());
        this.shadowRadius = shadowRadius;
    }

    @Override
    public void preRender(PoseStack poseStack, AssimilatedParasiteEntity entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (entity.isMelting()) {
            poseStack.scale(1.0F, entity.getMeltRenderScale(partialTick), 1.0F);
        }
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
    }
}
