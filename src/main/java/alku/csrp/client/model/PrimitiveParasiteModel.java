package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.AncientParasiteEntity;
import alku.csrp.entity.AssimilatedDragonEntity;
import alku.csrp.entity.AssimilatedEndermanEntity;
import alku.csrp.entity.BurrowingVariantEntity;
import alku.csrp.entity.CarrierEntity;
import alku.csrp.entity.ManglerEntity;
import alku.csrp.entity.MarauderizedCowEntity;
import alku.csrp.entity.PreeminentParasiteEntity;
import alku.csrp.entity.PrimitiveVariantEntity;
import alku.csrp.entity.PureParasiteEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public final class PrimitiveParasiteModel<T extends Mob & GeoEntity> extends ParasiteGeoModel<T> {
    private static final ResourceLocation CARRIER_HEAVY_VARIANT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/ratholone.png");
    private static final ResourceLocation CARRIER_FLYING_VARIANT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/butholone.png");
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
    private static final ResourceLocation BOLSTER_VIRULENT_PRIMITIVE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/banov.png");
    private static final ResourceLocation BOLSTER_HEAVY_PRIMITIVE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/banoh.png");
    private static final ResourceLocation MANDUCATER_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/hullh.png");
    private static final ResourceLocation ARACHNIDA_VIRULENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/ada_arachnida_virulent.png");
    private static final ResourceLocation ARACHNIDA_BLEEDING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/ada_arachnida_bleeding.png");
    private static final ResourceLocation ARACHNIDA_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/ada_arachnida_heavy.png");
    private static final ResourceLocation SHRIMP_FED_ENDERMAN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/sim_enderman_ariral.png");
    private static final ResourceLocation VARIANT_ENDERMAN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/sim_enderman_variant.png");
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
    private static final ResourceLocation DEVOURER_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/pri_devourer_heavy.png");
    private static final ResourceLocation YELLOWEYE_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/pri_yelloweye_heavy.png");
    private static final ResourceLocation MANGLER_VIRAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/nuuhv.png");
    private static final ResourceLocation MANGLER_BLEEDING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/nuuhb.png");
    private static final ResourceLocation MANGLER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/nuuh.png");
    private static final ResourceLocation GRUNT_VIRULENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/flogv.png");
    private static final ResourceLocation GRUNT_BLEEDING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/flogb.png");
    private static final ResourceLocation GRUNT_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/flogh.png");
    private static final ResourceLocation OMBOO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/omboo.png");
    private static final ResourceLocation OMBOO_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/ombooh.png");
    private static final ResourceLocation MONARCH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/orch.png");
    private static final ResourceLocation MONARCH_SKIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/orchsp1.png");
    private static final ResourceLocation MONARCH_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/orchh.png");
    private static final ResourceLocation OVERSEER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/alafha.png");
    private static final ResourceLocation OVERSEER_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/alafhah.png");
    private static final ResourceLocation VIGILANTE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/anged.png");
    private static final ResourceLocation VIGILANTE_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/angedh.png");
    private static final ResourceLocation WARDEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/ganro.png");
    private static final ResourceLocation WARDEN_HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/ganroh.png");
    private static final ResourceLocation MARAUDERIZED_COW_RAGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/mar_cow_desert.png");
    private static final String[] OMBOO_PULSE_GROUP_ONE = {"mpop6", "jointp7", "mpop8", "mpop16", "mpop5"};
    private static final String[] OMBOO_PULSE_GROUP_TWO = {"jointp11", "mpop1", "mpop13", "mpop19"};
    private static final String[] OMBOO_PULSE_GROUP_THREE = {"jointp17", "jointp18", "mpop4", "jointp2", "mpop3"};
    private static final String[] OMBOO_PULSE_GROUP_FOUR = {"mpop9", "mpop12", "jointp15", "mpop10", "mpop14"};
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
        if (animatable instanceof CarrierEntity carrier && carrier.getSkin() == 1) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(carrier.getType()).getPath();
            if (id.equals("carrier_heavy")) {
                return CARRIER_HEAVY_VARIANT_TEXTURE;
            }
            if (id.equals("carrier_flying")) {
                return CARRIER_FLYING_VARIANT_TEXTURE;
            }
        }
        if (animatable instanceof AssimilatedEndermanEntity enderman && enderman.isShrimpFed()) {
            return SHRIMP_FED_ENDERMAN_TEXTURE;
        }
        if (animatable instanceof AssimilatedEndermanEntity enderman && enderman.getTextureVariant() == 1) {
            return VARIANT_ENDERMAN_TEXTURE;
        }
        if (animatable instanceof MarauderizedCowEntity cow && cow.isRageVariant()) {
            return MARAUDERIZED_COW_RAGE_TEXTURE;
        }
        if (animatable instanceof ManglerEntity mangler) {
            return switch (mangler.getVariant()) {
                case ManglerEntity.VIRAL_VARIANT -> MANGLER_VIRAL_TEXTURE;
                case ManglerEntity.BLEEDING_VARIANT -> MANGLER_BLEEDING_TEXTURE;
                default -> MANGLER_TEXTURE;
            };
        }
        if (animatable instanceof PureParasiteEntity pure
                && pure.getKind() == PureParasiteEntity.Kind.GRUNT) {
            return switch (pure.getGruntSkin()) {
                case 5 -> GRUNT_VIRULENT_TEXTURE;
                case 6 -> GRUNT_BLEEDING_TEXTURE;
                case 7 -> GRUNT_HEAVY_TEXTURE;
                default -> texture;
            };
        }
        if (animatable instanceof PureParasiteEntity pure
                && pure.getKind() == PureParasiteEntity.Kind.BOMBER_LIGHT) {
            return pure.getOmbooSkin() == 7 ? OMBOO_HEAVY_TEXTURE : OMBOO_TEXTURE;
        }
        if (animatable instanceof PureParasiteEntity pure
                && pure.getKind() == PureParasiteEntity.Kind.MONARCH) {
            return switch (pure.getMonarchSkin()) {
                case 1 -> MONARCH_SKIN_TEXTURE;
                case 7 -> MONARCH_HEAVY_TEXTURE;
                default -> MONARCH_TEXTURE;
            };
        }
        if (animatable instanceof PureParasiteEntity pure
                && pure.getKind() == PureParasiteEntity.Kind.OVERSEER) {
            return pure.getOverseerSkin() == 7 ? OVERSEER_HEAVY_TEXTURE : OVERSEER_TEXTURE;
        }
        if (animatable instanceof PureParasiteEntity pure
                && pure.getKind() == PureParasiteEntity.Kind.VIGILANTE) {
            return pure.getVigilanteSkin() == 7 ? VIGILANTE_HEAVY_TEXTURE : VIGILANTE_TEXTURE;
        }
        if (animatable instanceof PureParasiteEntity pure
                && pure.getKind() == PureParasiteEntity.Kind.WARDEN) {
            return pure.getWardenSkin() == 7 ? WARDEN_HEAVY_TEXTURE : WARDEN_TEXTURE;
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
        if (animatable instanceof AdaptedVariantEntity adapted && adapted.isAdaptedArachnida()) {
            return switch (adapted.getArachnidaSkin()) {
                case 5 -> ARACHNIDA_VIRULENT_TEXTURE;
                case 6 -> ARACHNIDA_BLEEDING_TEXTURE;
                case 7 -> ARACHNIDA_HEAVY_TEXTURE;
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
        if (animatable instanceof PrimitiveVariantEntity primitive && primitive.isPrimitiveBolster()) {
            return switch (primitive.getBolsterSkin()) {
                case 5 -> BOLSTER_VIRULENT_PRIMITIVE_TEXTURE;
                case 7 -> BOLSTER_HEAVY_PRIMITIVE_TEXTURE;
                default -> texture;
            };
        }
        if (animatable instanceof PrimitiveVariantEntity primitive && primitive.isPrimitiveManducater()
                && primitive.getManducaterSkin() == 7) {
            return MANDUCATER_HEAVY_TEXTURE;
        }
        if (animatable instanceof PrimitiveVariantEntity primitive && primitive.isPrimitiveDevourer()
                && primitive.getDevourerSkin() == 7) {
            return DEVOURER_HEAVY_TEXTURE;
        }
        if (animatable instanceof PrimitiveVariantEntity primitive && primitive.isPrimitiveYelloweye()
                && primitive.getYelloweyeSkin() == 7) {
            return YELLOWEYE_HEAVY_TEXTURE;
        }
        return texture;
    }
    @Override public ResourceLocation getAnimationResource(T animatable) { return animation; }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        if (animatable instanceof AdaptedVariantEntity adapted) {
            applyAdaptedTendrilVisibility(adapted);
        }
        if (animatable instanceof PureParasiteEntity pure && pure.getKind() == PureParasiteEntity.Kind.VIGILANTE) {
            setHidden("taclejointUL1", !pure.isLeftVigilanteTendrilAttached());
            setHidden("taclejointUR1", !pure.isRightVigilanteTendrilAttached());
        }
        if (animatable instanceof PureParasiteEntity pure
                && pure.getKind() == PureParasiteEntity.Kind.BOMBER_LIGHT) {
            applyOmbooPulse(pure, animationState.getPartialTick());
        }
        if (animatable instanceof AncientParasiteEntity ancient
                && ancient.getKind() == AncientParasiteEntity.Kind.DREADNAUT) {
            setHidden("bodytenbaseUR", !ancient.isDreadnautTendrilAttached(1));
            setHidden("bodytenbaseUL", !ancient.isDreadnautTendrilAttached(2));
            setHidden("bodytenbaseRA", !ancient.isDreadnautTendrilAttached(3));
            setHidden("bodytenbaseLA", !ancient.isDreadnautTendrilAttached(4));
        }
        if (animatable instanceof AssimilatedDragonEntity dragon) {
            setHidden("jointLW1", !dragon.hasLeftWing());
            setHidden("jointRW1", !dragon.hasRightWing());
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

    private void applyAdaptedTendrilVisibility(AdaptedVariantEntity entity) {
        String left;
        String right;
        switch (entity.getKind()) {
            case LONGARMS, MANDUCATER, SUMMONER -> {
                left = "taclejointL1";
                right = "taclejointR1";
            }
            case REEKER -> {
                left = "taclejointL";
                right = "taclejointR";
            }
            case BOLSTER -> {
                left = "jointMLT0";
                right = "jointMRT0";
            }
            default -> {
                return;
            }
        }
        setHidden(left, !entity.isLeftTendrilAttached());
        setHidden(right, !entity.isRightTendrilAttached());
    }

    private void applyOmbooPulse(PureParasiteEntity entity, float partialTick) {
        float ageInTicks = entity.tickCount + partialTick;
        applyUniformScale(1.0F + Mth.sin(ageInTicks * 0.08F) * 0.05F, OMBOO_PULSE_GROUP_ONE);
        applyUniformScale(1.0F + Mth.sin(ageInTicks * 0.13F) * 0.06F, OMBOO_PULSE_GROUP_TWO);
        applyUniformScale(1.0F + Mth.sin(ageInTicks * 0.33F) * 0.02F, OMBOO_PULSE_GROUP_THREE);
        applyUniformScale(1.0F + Mth.sin(ageInTicks * 0.23F) * 0.04F, OMBOO_PULSE_GROUP_FOUR);
    }

    private void applyUniformScale(float scale, String[] boneNames) {
        for (String boneName : boneNames) {
            getBone(boneName).ifPresent(bone -> {
                bone.setScaleX(scale);
                bone.setScaleY(scale);
                bone.setScaleZ(scale);
            });
        }
    }

    private void setHidden(String boneName, boolean hidden) {
        getBone(boneName).ifPresent(bone -> bone.setHidden(hidden));
    }
}
