package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.tabula.primitive.ModelBano;
import alku.csrp.entity.PrimitiveVariantEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Citadel renderer backed directly by the original ModelBano Tabula Java model. */
public final class BolsterRenderer extends ParasiteMobRenderer<PrimitiveVariantEntity, ModelBano> {
    private static final ResourceLocation DEFAULT_TEXTURE = texture("pri_bolster.png");
    private static final ResourceLocation VIRULENT_TEXTURE = texture("banov.png");
    private static final ResourceLocation HEAVY_TEXTURE = texture("banoh.png");

    public BolsterRenderer(EntityRendererProvider.Context context) {
        super(context, new ModelBano(), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrimitiveVariantEntity entity) {
        return switch (entity.getBolsterSkin()) {
            case 5 -> VIRULENT_TEXTURE;
            case 7 -> HEAVY_TEXTURE;
            default -> DEFAULT_TEXTURE;
        };
    }

    private static ResourceLocation texture(String file) {
        return new ResourceLocation(Csrp.MODID, "textures/entity/" + file);
    }
}
