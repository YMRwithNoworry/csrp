package alku.csrp.client.model.tabula;

import alku.csrp.Csrp;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.AssimilatedEndermanEntity;
import alku.csrp.entity.CarrierEntity;
import alku.csrp.entity.ManglerEntity;
import alku.csrp.entity.MarauderizedCowEntity;
import alku.csrp.entity.PreeminentParasiteEntity;
import alku.csrp.entity.PrimitiveVariantEntity;
import alku.csrp.entity.PureParasiteEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Keeps the original SRP skin/variant selection while models move to Citadel. */
public final class TabulaTextureResolver {
    private TabulaTextureResolver() {
    }

    public static ResourceLocation resolve(Entity entity, String modelId) {
        ResourceLocation texture = texture(modelId);
        if (entity instanceof CarrierEntity carrier && carrier.getSkin() == 1) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(carrier.getType()).getPath();
            if (id.equals("carrier_heavy")) return texture("ratholone");
            if (id.equals("carrier_flying")) return texture("butholone");
        }
        if (entity instanceof AssimilatedEndermanEntity enderman && enderman.isShrimpFed()) {
            return texture("sim_enderman_ariral");
        }
        if (entity instanceof AssimilatedEndermanEntity enderman && enderman.getTextureVariant() == 1) {
            return texture("sim_enderman_variant");
        }
        if (entity instanceof MarauderizedCowEntity cow && cow.isRageVariant()) {
            return texture("mar_cow_desert");
        }
        if (entity instanceof ManglerEntity mangler) {
            return switch (mangler.getVariant()) {
                case ManglerEntity.VIRAL_VARIANT -> texture("nuuhv");
                case ManglerEntity.BLEEDING_VARIANT -> texture("nuuhb");
                default -> texture("nuuh");
            };
        }
        if (entity instanceof PureParasiteEntity pure) {
            return resolvePure(pure, modelId, texture);
        }
        if (entity instanceof PreeminentParasiteEntity preeminent) {
            if (preeminent.isCarrierVariant()) return texture("vestare");
            if (preeminent.isHaunterVariant()) return texture("pheonsp1");
        }
        if (entity instanceof AdaptedVariantEntity adapted) {
            if (adapted.isAdaptedBolster()) {
                return switch (adapted.getBolsterVariant()) {
                    case BERSERKER -> texture("ada_bolster_berserker");
                    case VIRULENT -> texture("ada_bolster_virulent");
                    case BREACHER -> texture("ada_bolster_breacher");
                    default -> texture;
                };
            }
            if (adapted.isAdaptedArachnida()) {
                return switch (adapted.getArachnidaSkin()) {
                    case 5 -> texture("ada_arachnida_virulent");
                    case 6 -> texture("ada_arachnida_bleeding");
                    case 7 -> texture("ada_arachnida_heavy");
                    default -> texture;
                };
            }
        }
        if (entity instanceof PrimitiveVariantEntity primitive) {
            if (primitive.isPrimitiveReeker()) {
                if (primitive.isRicardoBald()) return texture("ricardo_bald");
                if (primitive.isRicardoVariant()) return texture("ricardo");
                return switch (primitive.getReekerSkin()) {
                    case 1 -> texture("noglasp1");
                    case 5 -> texture("noglav");
                    case 6 -> texture("noglab");
                    case 7 -> texture("noglah");
                    default -> texture;
                };
            }
            if (primitive.isPrimitiveBolster()) {
                return switch (primitive.getBolsterSkin()) {
                    case 5 -> texture("banov");
                    case 7 -> texture("banoh");
                    default -> texture;
                };
            }
            if (primitive.isPrimitiveManducater() && primitive.getManducaterSkin() == 7) {
                return texture("hullh");
            }
            if (primitive.isPrimitiveDevourer() && primitive.getDevourerSkin() == 7) {
                return texture("pri_devourer_heavy");
            }
            if (primitive.isPrimitiveYelloweye() && primitive.getYelloweyeSkin() == 7) {
                return texture("pri_yelloweye_heavy");
            }
        }
        return texture;
    }

    private static ResourceLocation resolvePure(PureParasiteEntity pure, String modelId,
                                                 ResourceLocation fallback) {
        return switch (pure.getKind()) {
            case GRUNT -> switch (pure.getGruntSkin()) {
                case 5 -> texture("monster/flogv");
                case 6 -> texture("monster/flogb");
                case 7 -> texture("monster/flogh");
                default -> fallback;
            };
            case BOMBER_LIGHT -> pure.getOmbooSkin() == 7
                    ? texture("monster/ombooh") : texture("monster/omboo");
            case MONARCH -> switch (pure.getMonarchSkin()) {
                case 1 -> texture("monster/orchsp1");
                case 7 -> texture("monster/orchh");
                default -> texture("monster/orch");
            };
            case OVERSEER -> pure.getOverseerSkin() == 7
                    ? texture("monster/alafhah") : texture("monster/alafha");
            case VIGILANTE -> pure.getVigilanteSkin() == 7
                    ? texture("monster/angedh") : texture("monster/anged");
            case WARDEN -> pure.getWardenSkin() == 7
                    ? texture("monster/ganroh") : texture("monster/ganro");
            default -> fallback;
        };
    }

    public static ResourceLocation texture(String id) {
        return new ResourceLocation(Csrp.MODID, "textures/entity/" + id + ".png");
    }
}
