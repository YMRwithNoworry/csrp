package alku.csrp.client.renderer;

import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.AirscrewEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renders a visible tether for each creature currently captured by an Airscrew. */
public final class AirscrewRenderer extends GeoEntityRenderer<AirscrewEntity> {
    public AirscrewRenderer(EntityRendererProvider.Context context) {
        super(context, new PrimitiveParasiteModel<>("airscrew"));
        shadowRadius = 0.8F;
    }

    @Override
    public void render(AirscrewEntity airscrew, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(airscrew, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        for (LivingEntity target : airscrew.getPullTargetsForRendering()) {
            renderLeash(airscrew, partialTick, poseStack, bufferSource, target);
        }
    }
}
