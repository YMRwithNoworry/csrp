package alku.csrp.client.renderer;

import alku.csrp.client.model.RupterModel;
import alku.csrp.entity.RupterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class RupterRenderer extends ParasiteGeoRenderer<RupterEntity> {
    public RupterRenderer(EntityRendererProvider.Context context) {
        super(context, new RupterModel());
        this.shadowRadius = 0.45F;
    }
}
