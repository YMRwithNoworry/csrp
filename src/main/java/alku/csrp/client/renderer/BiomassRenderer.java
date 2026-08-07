package alku.csrp.client.renderer;

import alku.csrp.client.model.BiomassModel;
import alku.csrp.entity.BiomassEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class BiomassRenderer extends ParasiteGeoRenderer<BiomassEntity> {
    public BiomassRenderer(EntityRendererProvider.Context context) {
        super(context, new BiomassModel());
        shadowRadius = 0.5F;
    }
}
