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
            case "func_78087_a.getDigging" -> "get_dig_model.get_digging_1";
            case "animation" -> "idle";
            default -> requestedAction;
        };

        if (action.equals("attack") && !hasShortAttackAnimation(resourceId)
                && usesShortAnimationKeys(resourceId)) {
            return "walk";
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
            default -> id;
        };
    }
}
