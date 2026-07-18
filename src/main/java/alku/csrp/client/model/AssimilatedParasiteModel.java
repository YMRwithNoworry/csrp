package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.AssimilatedParasiteEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Selects the correct assimilated resource set and legacy visual variants. */
public final class AssimilatedParasiteModel extends GeoModel<AssimilatedParasiteEntity> {
    @Override
    public ResourceLocation getModelResource(AssimilatedParasiteEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                "geo/" + animatable.getKind().name().toLowerCase(java.util.Locale.ROOT).replaceFirst("^", "sim_")
                        + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AssimilatedParasiteEntity animatable) {
        return animatable.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(AssimilatedParasiteEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                "animations/" + animatable.getKind().name().toLowerCase(java.util.Locale.ROOT).replaceFirst("^", "sim_")
                        + ".animation.json");
    }
}
