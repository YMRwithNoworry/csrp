package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.ScaryOrbEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class ScaryOrbRenderer extends EntityRenderer<ScaryOrbEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/scary_orb.png");

    public ScaryOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override public ResourceLocation getTextureLocation(ScaryOrbEntity entity) {
        return TEXTURE;
    }
}
