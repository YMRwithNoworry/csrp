package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.PullingBallEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class PullingBallRenderer extends LegacyBillboardRenderer<PullingBallEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/projectile/pullingweb.png");

    public PullingBallRenderer(EntityRendererProvider.Context context) {
        super(context, TEXTURE, 1.0F, 1.0F);
    }
}
