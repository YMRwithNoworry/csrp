package alku.csrp.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animation.RawAnimation;

/** Resolves the fully-qualified animation names emitted by the SRP extractor. */
final class ParasiteAnimations {
    private ParasiteAnimations() {
    }

    static RawAnimation loop(Entity entity, String action) {
        return RawAnimation.begin().thenLoop(animationName(entity, action));
    }

    static RawAnimation play(Entity entity, String action) {
        return RawAnimation.begin().thenPlay(animationName(entity, action));
    }

    /** Navigation can report movement while an entity is blocked; require actual displacement. */
    static boolean isMoving(Entity entity, boolean animationMoving) {
        return animationMoving && entity.getDeltaMovement().lengthSqr() > 0.0004D;
    }

    private static String animationName(Entity entity, String requestedAction) {
        String resourceId = animationResourceId(entity);
        String action = switch (requestedAction) {
            case "run", "fly" -> "walk";
            case "spawn", "throw", "smash", "swipe" -> "attack";
            // CruxB keeps the original controller function names, while the
            // extracted Bedrock resource only exposes Crux's attack clip.
            case "melee_attack", "ranged_attack", "burst" -> "attack";
            case "func_78087_a.getDigging" -> "get_dig_model.get_digging_1";
            case "animation" -> "idle";
            default -> requestedAction;
        };

        if (action.equals("attack") && !hasShortAttackAnimation(resourceId)
                && usesShortAnimationKeys(resourceId)) {
            return "walk";
        }

        // Several primitive entities encode their attack as a parasite-status
        // pose instead of publishing a generic `attack` clip.
        if (action.equals("attack")) {
            action = switch (resourceId) {
                case "pri_arachnida" -> "walk.get_parasite_status_2";
                case "pri_manducater" -> "idle.get_parasite_status_1";
                case "pri_reeker" -> "idle.get_parasite_status_1";
                case "sim_dragone" -> "walk.get_parasite_status_2";
                case "dispatcher_sii" -> "idle";
                default -> action;
            };
        }

        // These extracted short-key resources omit the controller aliases
        // used by the original Java models; keep the original call sites but
        // resolve them to the available clips.
        if (resourceId.equals("marauder") && action.contains("get_parasite_status")) {
            action = "skill";
        } else if (resourceId.equals("pri_summoner") && action.equals("summon")) {
            action = "run";
        }

        // These are the project-only fallback resources that were not
        // present in the extracted SRP resource set and retain short keys.
        if (usesShortAnimationKeys(resourceId)) {
            return action;
        }
        return "animation." + resourceId + "." + action;
    }

    private static boolean usesShortAnimationKeys(String resourceId) {
        return resourceId.equals("abo_head") || resourceId.equals("marauder_tendril")
                || resourceId.equals("marauder") || resourceId.equals("movingflesh")
                || resourceId.equals("pri_summoner") || resourceId.equals("sim_cow")
                || resourceId.equals("sim_cowhead") || resourceId.equals("sim_pig")
                || resourceId.equals("inf_sheep") || resourceId.equals("inf_sheep_head")
                || resourceId.equals("inf_villager");
    }

    private static boolean hasShortAttackAnimation(String resourceId) {
        return resourceId.equals("abo_head") || resourceId.equals("marauder");
    }

    private static String animationResourceId(Entity entity) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String id = key.getPath();
        // The original SRP resource uses the historical "dragone" spelling,
        // while csrp keeps its existing registry ID for save compatibility.
        return switch (id) {
            case "sim_dragonhead" -> "sim_dragonehead";
            case "dispatcher_tentacle" -> "dispatcherten";
            case "crux_incomplete" -> "crux";
            default -> id;
        };
    }
}
