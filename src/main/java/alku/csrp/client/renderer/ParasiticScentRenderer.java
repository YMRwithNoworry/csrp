package alku.csrp.client.renderer;

import alku.csrp.entity.ParasiticScentEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/** The Scent is represented by its boss bar and sound rather than world geometry. */
public final class ParasiticScentRenderer extends EntityRenderer<ParasiticScentEntity> {
    public ParasiticScentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ParasiticScentEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
