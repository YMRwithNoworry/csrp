package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.ParasiteProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class ParasiteProjectileRenderer extends EntityRenderer<ParasiteProjectileEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/scary_orb.png");

    public ParasiteProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ParasiteProjectileEntity entity) {
        return TEXTURE;
    }
}
