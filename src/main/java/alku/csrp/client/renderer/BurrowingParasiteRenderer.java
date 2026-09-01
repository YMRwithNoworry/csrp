package alku.csrp.client.renderer;

import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.BurrowingVariantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Sinks burrowing parasite models before their hidden underground movement. */
public final class BurrowingParasiteRenderer<T extends BurrowingVariantEntity> extends ParasiteGeoRenderer<T> {
    private final float sinkDistance;

    public BurrowingParasiteRenderer(EntityRendererProvider.Context context, String id,
            float shadowRadius, float sinkDistance) {
        super(context, new PrimitiveParasiteModel<>(id));
        this.shadowRadius = shadowRadius;
        this.sinkDistance = sinkDistance;
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return !entity.isFullyBurrowed() && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.translate(0.0D, -entity.getBurrowDepth(partialTick) * sinkDistance, 0.0D);
        super.scale(entity, poseStack, partialTick);
    }
}
