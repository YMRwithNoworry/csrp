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

    private static String animationName(Entity entity, String requestedAction) {
        String resourceId = animationResourceId(entity);
        String action = switch (requestedAction) {
            case "run", "fly" -> "walk";
            case "spawn", "rush", "throw", "smash", "swipe" -> "attack";
            case "animation" -> "idle";
            default -> requestedAction;
        };

        // These are the project-only fallback resources that were not
        // present in the extracted SRP resource set and retain short keys.
        if (resourceId.equals("abo_head") || resourceId.equals("marauder_tendril")) {
            return action;
        }
        return "animation." + resourceId + "." + action;
    }

    private static String animationResourceId(Entity entity) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String id = key.getPath();
        // The original SRP resource uses the historical "dragone" spelling,
        // while csrp keeps its existing registry ID for save compatibility.
        return id.equals("sim_dragonhead") ? "sim_dragonehead" : id;
    }
}
