package alku.csrp.client.renderer;

import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.MovingFleshEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/** Applies the accumulated merge scale to the high-version Moving Flesh model. */
public final class MovingFleshRenderer extends ParasiteGeoRenderer<MovingFleshEntity> {
    public MovingFleshRenderer(EntityRendererProvider.Context context) {
        super(context, new PrimitiveParasiteModel<>("movingflesh"));
        shadowRadius = 0.2F;
    }

    @Override
    public void preRender(PoseStack poseStack, MovingFleshEntity entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        float scale = entity.getRenderScale(partialTick);
        poseStack.scale(scale, scale, scale);
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
    }
}
