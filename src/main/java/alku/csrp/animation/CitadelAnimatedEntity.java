package alku.csrp.animation;

import alku.csrp.network.CitadelAnimationTriggerPayload;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Common-side contract for entities rendered by the Citadel Tabula runtime. */
public interface CitadelAnimatedEntity {
    Map<CitadelAnimatedEntity, CitadelAnimationCache> FALLBACK_CACHES =
            Collections.synchronizedMap(new WeakHashMap<>());

    void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers);

    default CitadelAnimationCache getCitadelAnimationCache() {
        return FALLBACK_CACHES.computeIfAbsent(this, ignored -> new CitadelAnimationCache());
    }

    default void triggerAnim(String controller, String animation) {
        if (!(this instanceof Entity entity)) {
            return;
        }
        getCitadelAnimationCache().trigger(controller, animation, entity.tickCount);
        if (!entity.level().isClientSide) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,
                    new CitadelAnimationTriggerPayload(entity.getId(), controller, animation));
        }
    }
}
