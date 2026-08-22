package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.PullingBallEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class PullingBallRenderer extends EntityRenderer<PullingBallEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/pulling_ball.png");

    public PullingBallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PullingBallEntity entity) {
        return TEXTURE;
    }
}
