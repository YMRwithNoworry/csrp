package alku.csrp.world;

import java.util.concurrent.atomic.AtomicReference;

/** Transfers the create-world meteor choice to the integrated server once. */
public final class SrpMeteorSelection {
    private static final AtomicReference<Boolean> PENDING = new AtomicReference<>();

    private SrpMeteorSelection() {
    }

    public static void stage(boolean enabled) {
        PENDING.set(enabled);
    }

    public static boolean consumeOrDefault(boolean defaultValue) {
        Boolean enabled = PENDING.getAndSet(null);
        return enabled == null ? defaultValue : enabled;
    }
}
