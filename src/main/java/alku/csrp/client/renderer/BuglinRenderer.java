package alku.csrp.client.renderer;

import alku.csrp.client.model.BuglinModel;
import alku.csrp.entity.BuglinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class BuglinRenderer extends ParasiteGeoRenderer<BuglinEntity> {
    public BuglinRenderer(EntityRendererProvider.Context context) {
        super(context, new BuglinModel());
        this.shadowRadius = 0.25F;
    }
}
