package alku.csrp.client.renderer;

import alku.csrp.client.model.NexusParasiteModel;
import alku.csrp.entity.NexusParasiteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class NexusParasiteRenderer extends ParasiteGeoRenderer<NexusParasiteEntity> {
    public NexusParasiteRenderer(EntityRendererProvider.Context context, String id, float shadowRadius) {
        super(context, new NexusParasiteModel(id));
        this.shadowRadius = shadowRadius;
    }
}
