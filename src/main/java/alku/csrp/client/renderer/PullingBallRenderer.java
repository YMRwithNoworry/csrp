package alku.csrp.client.renderer;

import alku.csrp.entity.PullingBallEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class PullingBallRenderer extends EntityRenderer<PullingBallEntity, EntityRenderState> {
    public PullingBallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
