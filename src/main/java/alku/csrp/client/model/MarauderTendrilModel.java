package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.MarauderTendrilEntity;
import net.minecraft.resources.ResourceLocation;

/** Model used by Marauder's detached and stationary support tendrils. */
public final class MarauderTendrilModel extends ParasiteGeoModel<MarauderTendrilEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(Csrp.MODID,
            "geo/marauder_tendril.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/marauder_tendril.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(Csrp.MODID,
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
