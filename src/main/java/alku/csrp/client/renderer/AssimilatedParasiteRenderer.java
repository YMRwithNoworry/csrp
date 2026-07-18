package alku.csrp.client.renderer;

import alku.csrp.client.model.AssimilatedParasiteModel;
import alku.csrp.entity.AssimilatedParasiteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class AssimilatedParasiteRenderer extends GeoEntityRenderer<AssimilatedParasiteEntity> {
    public AssimilatedParasiteRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new AssimilatedParasiteModel());
        this.shadowRadius = shadowRadius;
    }
}
