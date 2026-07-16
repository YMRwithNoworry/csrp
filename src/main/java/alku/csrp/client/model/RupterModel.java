package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.RupterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RupterModel extends GeoModel<RupterEntity> {
    private static final ResourceLocation MODEL = id("geo/rupter.geo.json");
    private static final ResourceLocation ANIMATIONS = id("animations/rupter.animation.json");

    @Override
    public ResourceLocation getModelResource(RupterEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RupterEntity animatable) {
        RupterEntity.BehaviorVariant behaviorVariant = animatable.getBehaviorVariant();
        String suffix = behaviorVariant == RupterEntity.BehaviorVariant.NORMAL
                ? animatable.getTextureVariant().suffix()
                : behaviorVariant.suffix();
        return id("textures/entity/rupter" + suffix + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(RupterEntity animatable) {
        return ANIMATIONS;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, path);
    }
}
