package alku.csrp.client.renderer;

import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.SimAdventurerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Preserves the legacy shrink animation while an Assimilated Adventurer melts. */
public final class SimAdventurerRenderer
        extends ParasiteGeoRenderer<SimAdventurerEntity, PrimitiveParasiteModel<SimAdventurerEntity>> {
    public SimAdventurerRenderer(EntityRendererProvider.Context context) {
        super(context, new PrimitiveParasiteModel<>("sim_adventurer"));
        shadowRadius = 0.5F;
    }

    @Override
    protected void scale(LegacyMobRenderState state, PoseStack poseStack) {
        SimAdventurerEntity entity = (SimAdventurerEntity) state.legacyEntity;
        float scale = entity.getRenderScale(state.partialTick);
        poseStack.scale(1.0F, scale, 1.0F);
        super.scale(state, poseStack);
    }
}
