package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.PreeminentParasiteEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;

public final class PrimitiveParasiteModel<T extends Mob & GeoEntity> extends ParasiteGeoModel<T> {
    private static final ResourceLocation CARRIER_VARIANT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/vestare.png");
    private static final ResourceLocation HAUNTER_VARIANT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/pheonsp1.png");
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public PrimitiveParasiteModel(String id) {
        model = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "geo/" + id + ".geo.json");
        texture = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/" + id + ".png");
        animation = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "animations/" + id + ".animation.json");
    }

    @Override public ResourceLocation getModelResource(T animatable) { return model; }
    @Override
    public ResourceLocation getTextureResource(T animatable) {
        if (animatable instanceof PreeminentParasiteEntity preeminent && preeminent.isCarrierVariant()) {
            return CARRIER_VARIANT_TEXTURE;
        }
        if (animatable instanceof PreeminentParasiteEntity preeminent && preeminent.isHaunterVariant()) {
            return HAUNTER_VARIANT_TEXTURE;
        }
        return texture;
    }
    @Override public ResourceLocation getAnimationResource(T animatable) { return animation; }
}
