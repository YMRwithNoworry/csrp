package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Lightweight renderer for a Citadel/Tabula model with a fixed entity texture. */
public final class TabulaParasiteRenderer<T extends Mob, M extends EntityModel<T>>
        extends ParasiteMobRenderer<T, M> {
    private final ResourceLocation texture;

    public TabulaParasiteRenderer(EntityRendererProvider.Context context, M model,
                                   String textureId, float shadowRadius) {
        super(context, model, shadowRadius);
        this.texture = new ResourceLocation(Csrp.MODID, "textures/entity/" + textureId + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
