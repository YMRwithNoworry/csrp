package alku.csrp.client.renderer;

import alku.csrp.client.model.BuglinModel;
import alku.csrp.entity.BuglinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BuglinRenderer extends GeoEntityRenderer<BuglinEntity> {
    public BuglinRenderer(EntityRendererProvider.Context context) {
        super(context, new BuglinModel());
        this.shadowRadius = 0.25F;
    }
}
