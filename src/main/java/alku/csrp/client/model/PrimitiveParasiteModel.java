package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.PreeminentParasiteEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationState;

public final class PrimitiveParasiteModel<T extends Mob & GeoEntity> extends ParasiteGeoModel<T> {
    private static final ResourceLocation CARRIER_VARIANT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/vestare.png");
    private static final ResourceLocation HAUNTER_VARIANT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/pheonsp1.png");
    private static final ResourceLocation BOLSTER_BERSERKER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/ada_bolster_berserker.png");
    private static final ResourceLocation BOLSTER_VIRULENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/ada_bolster_virulent.png");
    private static final ResourceLocation BOLSTER_BREACHER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/ada_bolster_breacher.png");
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
        if (animatable instanceof AdaptedVariantEntity adapted && adapted.isAdaptedBolster()) {
            return switch (adapted.getBolsterVariant()) {
                case BERSERKER -> BOLSTER_BERSERKER_TEXTURE;
                case VIRULENT -> BOLSTER_VIRULENT_TEXTURE;
                case BREACHER -> BOLSTER_BREACHER_TEXTURE;
                default -> texture;
            };
        }
        return texture;
    }
    @Override public ResourceLocation getAnimationResource(T animatable) { return animation; }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        if (animatable instanceof AdaptedVariantEntity adapted && adapted.isAdaptedBolster()) {
            getBone("jointMLT0").ifPresent(bone -> bone.setHidden(!adapted.isLeftBolsterTendrilAttached()));
            getBone("jointMRT0").ifPresent(bone -> bone.setHidden(!adapted.isRightBolsterTendrilAttached()));
        }
    }
}
