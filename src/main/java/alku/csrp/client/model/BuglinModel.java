package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.BuglinEntity;
import net.minecraft.resources.ResourceLocation;

public final class BuglinModel extends ParasiteGeoModel<BuglinEntity> {
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
