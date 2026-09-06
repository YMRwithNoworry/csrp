package alku.csrp.client.renderer;

import alku.csrp.entity.BurrowingVariantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;

/** Direct Tabula renderer for parasites that sink while burrowing. */
public final class TabulaBurrowingRenderer<T extends BurrowingVariantEntity> extends TabulaMobRenderer<T> {
    private final float sinkDistance;

    public TabulaBurrowingRenderer(EntityRendererProvider.Context context, String id,
                                   float shadowRadius, float sinkDistance) {
        super(context, id, shadowRadius);
        this.sinkDistance = sinkDistance;
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return !entity.isFullyBurrowed() && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    protected void applyScale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.translate(0.0D, -entity.getBurrowDepth(partialTick) * sinkDistance, 0.0D);
        super.applyScale(entity, poseStack, partialTick);
    }
}
