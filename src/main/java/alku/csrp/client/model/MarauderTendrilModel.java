package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.MarauderTendrilEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Model used by Marauder's detached and stationary support tendrils. */
public final class MarauderTendrilModel extends GeoModel<MarauderTendrilEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "geo/marauder_tendril.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/marauder_tendril.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "animations/marauder_tendril.animation.json");

    @Override
    public ResourceLocation getModelResource(MarauderTendrilEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MarauderTendrilEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MarauderTendrilEntity animatable) {
        return ANIMATION;
    }
}
