package alku.csrp.entity;

import net.minecraft.world.entity.LivingEntity;

/** Owner contract shared by parasites whose pullball projectile can tether a target. */
public interface PullingBallOwner {
    boolean isAlive();

    boolean captureTarget(LivingEntity target);

    boolean isValidPullTarget(LivingEntity target);
}
