package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.AssimilatedEndermanEntity;
import alku.csrp.entity.BurrowingVariantEntity;
import alku.csrp.entity.PreeminentParasiteEntity;
import alku.csrp.entity.PrimitiveVariantEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

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
    private static final ResourceLocation SHRIMP_FED_ENDERMAN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/sim_enderman_ariral.png");
    private static final ResourceLocation REEKER_FRAGILE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/noglasp1.png");
    private static final ResourceLocation REEKER_VIRULENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/noglav.png");
    private static final ResourceLocation REEKER_BERSERKER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/noglab.png");
    private static final ResourceLocation REEKER_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/noglah.png");
    private static final ResourceLocation REEKER_RICARDO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/ricardo.png");
    private static final ResourceLocation REEKER_RICARDO_BALD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/ricardo_bald.png");
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
        if (animatable instanceof AssimilatedEndermanEntity enderman && enderman.isShrimpFed()) {
            return SHRIMP_FED_ENDERMAN_TEXTURE;
        }
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
        if (animatable instanceof PrimitiveVariantEntity primitive && primitive.isPrimitiveReeker()) {
            if (primitive.isRicardoBald()) {
                return REEKER_RICARDO_BALD_TEXTURE;
            }
            if (primitive.isRicardoVariant()) {
                return REEKER_RICARDO_TEXTURE;
            }
            return switch (primitive.getReekerSkin()) {
                case 1 -> REEKER_FRAGILE_TEXTURE;
                case 5 -> REEKER_VIRULENT_TEXTURE;
                case 6 -> REEKER_BERSERKER_TEXTURE;
                case 7 -> REEKER_HEAVY_TEXTURE;
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
        if (animatable instanceof AssimilatedEndermanEntity enderman) {
            getBone("mouth").ifPresent(bone -> bone.setHidden(enderman.isShrimpFed()));
        }
        if (animatable instanceof BurrowingVariantEntity burrowing) {
            applyBodySegmentVisibility(burrowing);
        }
    }

    @Override
    protected boolean shouldDampenMovingRotation(T animatable, GeoBone bone) {
        return !(animatable instanceof AssimilatedEndermanEntity && bone.getName().equals("mainbody"));
    }

    private void applyBodySegmentVisibility(BurrowingVariantEntity entity) {
        int body = entity.getBodyNumber();
        boolean tail = entity.isBodyTail();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        if (!id.endsWith("burrower") && !id.endsWith("tozoon")) {
            return;
        }
        setHidden("jointT", !tail);
        setHidden("jointH", body > 0);

        if (id.endsWith("burrower")) {
            if (id.startsWith("ada_")) {
                setHidden("bra", !tail);
                setHidden("bla", !tail);
            }
            return;
        }

        boolean primitive = id.startsWith("pri_");
        boolean alternatingLimbs = !tail && body > 0 && (body & 1) == 1;
        if (primitive) {
            setHidden("jointFL1", !alternatingLimbs);
            setHidden("jointFR1", !alternatingLimbs);
            setHidden("jointML1", tail || alternatingLimbs);
            setHidden("jointMR1", tail || alternatingLimbs);
            setHidden("jointBL1", !alternatingLimbs);
            setHidden("jointBR1", !alternatingLimbs);
        } else {
            setHidden("jointULA1", !alternatingLimbs);
            setHidden("jointURA1", !alternatingLimbs);
            setHidden("jointMLA1", tail || alternatingLimbs);
            setHidden("jointMRA1", tail || alternatingLimbs);
            setHidden("jointDLA1", !alternatingLimbs);
            setHidden("jointDRA1", !alternatingLimbs);
        }
    }

    private void setHidden(String boneName, boolean hidden) {
        getBone(boneName).ifPresent(bone -> bone.setHidden(hidden));
    }
}
