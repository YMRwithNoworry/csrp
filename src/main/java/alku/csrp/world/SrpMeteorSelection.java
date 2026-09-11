package alku.csrp.world;

import java.util.concurrent.atomic.AtomicReference;

/** Transfers the meteor-infection choice from world creation to the integrated server once. */
public final class SrpMeteorSelection {
    private static final AtomicReference<SrpMeteorMode> PENDING = new AtomicReference<>();

    private SrpMeteorSelection() {
    }

    public static void stage(SrpMeteorMode mode) {
        PENDING.set(mode);
    }

    /** @return the staged mode, or {@code null} when the creator never opened the option. */
    public static SrpMeteorMode consume() {
        return PENDING.getAndSet(null);
    }
}
