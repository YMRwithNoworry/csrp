package alku.csrp.client.renderer;

import alku.csrp.client.model.MarauderModel;
import alku.csrp.entity.MarauderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class MarauderRenderer extends GeoEntityRenderer<MarauderEntity> {
    public MarauderRenderer(EntityRendererProvider.Context context) {
        super(context, new MarauderModel());
        shadowRadius = 1.1F;
    }
}
