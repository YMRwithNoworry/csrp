package alku.csrp.client.renderer;

import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.SimAdventurerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/** Preserves the legacy shrink animation while an Assimilated Adventurer melts. */
public final class SimAdventurerRenderer extends ParasiteGeoRenderer<SimAdventurerEntity> {
    public SimAdventurerRenderer(EntityRendererProvider.Context context) {
        super(context, new PrimitiveParasiteModel<>("sim_adventurer"));
        shadowRadius = 0.5F;
    }

    @Override
    public void preRender(PoseStack poseStack, SimAdventurerEntity entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        float scale = entity.getRenderScale(partialTick);
        poseStack.scale(1.0F, scale, 1.0F);
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
    }
}
