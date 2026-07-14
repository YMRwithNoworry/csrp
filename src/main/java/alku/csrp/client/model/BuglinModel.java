package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.BuglinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BuglinModel extends GeoModel<BuglinEntity> {
    private static final ResourceLocation MODEL = id("geo/buglin.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/entity/buglin.png");
    private static final ResourceLocation ANIMATIONS = id("animations/buglin.animation.json");

    @Override
    public ResourceLocation getModelResource(BuglinEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BuglinEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BuglinEntity animatable) {
        return ANIMATIONS;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, path);
    }
}
