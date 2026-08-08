package alku.csrp.entity;

import net.minecraft.world.entity.LivingEntity;

/** Owner contract shared by parasites whose pullball projectile can tether a target. */
public interface PullingBallOwner {
    boolean isAlive();

    boolean captureTarget(LivingEntity target);

    boolean isValidPullTarget(LivingEntity target);

    /** Maximum distance used when a projectile scans for its capture target. */
    default double pullProjectileCaptureRadius() {
        return 0.7D;
    }

    /** Speed multiplier applied on the projectile's fifth tick. */
    default double pullProjectileAccelerationMultiplier() {
        return 2.0D;
    }

    /** Projectile lifetime in ticks; zero keeps the projectile alive until it hits something. */
    default int pullProjectileMaxAge() {
        return 80;
    }
}
