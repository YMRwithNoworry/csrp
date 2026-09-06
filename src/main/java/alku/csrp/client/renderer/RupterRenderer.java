package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.tabula.inborn.ModelMudo;
import alku.csrp.entity.RupterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Citadel renderer backed directly by the original ModelMudo Tabula Java model. */
public final class RupterRenderer extends ParasiteMobRenderer<RupterEntity, ModelMudo> {
    public RupterRenderer(EntityRendererProvider.Context context) {
        super(context, new ModelMudo(), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(RupterEntity entity) {
        RupterEntity.BehaviorVariant behaviorVariant = entity.getBehaviorVariant();
        String suffix = behaviorVariant == RupterEntity.BehaviorVariant.NORMAL
                ? entity.getTextureVariant().suffix()
                : behaviorVariant.suffix();
        return new ResourceLocation(Csrp.MODID, "textures/entity/rupter" + suffix + ".png");
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
