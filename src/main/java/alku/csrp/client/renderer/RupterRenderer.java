package alku.csrp.client.renderer;

import alku.csrp.client.model.RupterModel;
import alku.csrp.entity.RupterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class RupterRenderer extends GeoEntityRenderer<RupterEntity> {
    public RupterRenderer(EntityRendererProvider.Context context) {
        super(context, new RupterModel());
        this.shadowRadius = 0.45F;
    }
}
