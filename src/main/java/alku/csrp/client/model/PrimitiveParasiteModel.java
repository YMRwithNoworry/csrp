package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.PrimitiveParasiteEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class PrimitiveParasiteModel<T extends PrimitiveParasiteEntity> extends GeoModel<T> {
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public PrimitiveParasiteModel(String id) {
        model = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "geo/" + id + ".geo.json");
        texture = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/" + id + ".png");
        animation = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "animations/" + id + ".animation.json");
    }

    @Override public ResourceLocation getModelResource(T animatable) { return model; }
    @Override public ResourceLocation getTextureResource(T animatable) { return texture; }
    @Override public ResourceLocation getAnimationResource(T animatable) { return animation; }
}
