package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.tabula.pure.ModelEsor;
import alku.csrp.entity.MarauderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Citadel renderer backed directly by the original ModelEsor Tabula Java model. */
public final class MarauderRenderer extends ParasiteMobRenderer<MarauderEntity, ModelEsor> {
    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/marauder.png");
    private static final ResourceLocation HARDENED_TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/marauder_hardened.png");

    public MarauderRenderer(EntityRendererProvider.Context context) {
        super(context, new ModelEsor(), 1.1F);
    }

    @Override
    public ResourceLocation getTextureLocation(MarauderEntity entity) {
        return entity.isHardenedVariant() ? HARDENED_TEXTURE : DEFAULT_TEXTURE;
    }
}
