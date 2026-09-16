package alku.csrp.client.renderer;

import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.BurrowingVariantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Sinks burrowing parasite models before their hidden underground movement. */
public final class BurrowingParasiteRenderer<T extends BurrowingVariantEntity>
        extends ParasiteGeoRenderer<T, PrimitiveParasiteModel<T>> {
    private final float sinkDistance;

    public BurrowingParasiteRenderer(EntityRendererProvider.Context context, String id,
            float shadowRadius, float sinkDistance) {
        super(context, new PrimitiveParasiteModel<>(id));
        this.shadowRadius = shadowRadius;
        this.sinkDistance = sinkDistance;
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ,
            float partialTick) {
        return !entity.isFullyBurrowed()
                && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ, partialTick);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void scale(LegacyMobRenderState state, PoseStack poseStack) {
        T entity = (T) state.legacyEntity;
        poseStack.translate(0.0D, -entity.getBurrowDepth(state.partialTick) * sinkDistance, 0.0D);
        super.scale(state, poseStack);
    }
}
