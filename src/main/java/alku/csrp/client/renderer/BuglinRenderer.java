package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.tabula.inborn.ModelLodo;
import alku.csrp.entity.BuglinEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Citadel renderer backed directly by the original ModelLodo Tabula Java model. */
public final class BuglinRenderer extends ParasiteMobRenderer<BuglinEntity, ModelLodo> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/buglin.png");

    public BuglinRenderer(EntityRendererProvider.Context context) {
        super(context, new ModelLodo(), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(BuglinEntity entity) {
        return TEXTURE;
    }
}
