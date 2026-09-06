package alku.csrp.client.renderer;

import alku.csrp.entity.MovingFleshEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** Direct Tabula renderer preserving Moving Flesh growth/evolution scaling. */
public final class TabulaMovingFleshRenderer extends TabulaMobRenderer<MovingFleshEntity> {
    public TabulaMovingFleshRenderer(EntityRendererProvider.Context context) {
        super(context, "movingflesh", 0.2F);
    }

    @Override
    protected void applyScale(MovingFleshEntity entity, PoseStack poseStack, float partialTick) {
        float baseScale = entity.getRenderScale(partialTick);
        float flash = entity.getEvolutionFlashIntensity(partialTick);
        if (flash > 0.0F) {
            float vibration = 1.0F + Mth.sin(flash * 100.0F) * flash * 0.01F;
            poseStack.scale(baseScale * (1.0F + flash * 0.4F) * vibration,
                    baseScale * (1.0F + flash * 0.1F) / vibration,
                    baseScale * (1.0F + flash * 0.4F) * vibration);
        } else {
            poseStack.scale(baseScale, baseScale, baseScale);
        }
        super.applyScale(entity, poseStack, partialTick);
    }
}
