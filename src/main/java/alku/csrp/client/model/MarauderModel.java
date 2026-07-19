package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.MarauderEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;

/** Legacy Esor model with tendril bones hidden after their matching hitbox breaks. */
public final class MarauderModel extends ParasiteGeoModel<MarauderEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "geo/marauder.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/marauder.png");
    private static final ResourceLocation HARDENED_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/marauder_hardened.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "animations/marauder.animation.json");

    @Override
    public ResourceLocation getModelResource(MarauderEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MarauderEntity animatable) {
        return animatable.isHardenedVariant() ? HARDENED_TEXTURE : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MarauderEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(MarauderEntity animatable, long instanceId,
                                    AnimationState<MarauderEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        getBone("taclejointLA0").ifPresent(bone -> bone.setHidden(!animatable.isLeftTendrilAttached()));
        getBone("taclejointRA0").ifPresent(bone -> bone.setHidden(!animatable.isRightTendrilAttached()));
    }
}
