package alku.csrp.client.renderer;

import alku.csrp.client.model.BuglinModel;
import alku.csrp.entity.BuglinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class BuglinRenderer extends ParasiteGeoRenderer<BuglinEntity> {
    public BuglinRenderer(EntityRendererProvider.Context context) {
        super(context, new BuglinModel());
        // Legacy shadow radius of the original buglin renderer.
        this.shadowRadius = 0.2F;
    }
}
