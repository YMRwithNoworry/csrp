package alku.csrp.animation;

/** Factory for per-entity Citadel animation caches. */
public final class CitadelAnimationUtil {
    private CitadelAnimationUtil() {
    }

    public static CitadelAnimationCache createInstanceCache(Object owner) {
        return new CitadelAnimationCache();
    }
}
