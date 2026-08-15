package alku.csrp.world;

import java.util.concurrent.atomic.AtomicReference;

/** Transfers the selected star type from world creation to the integrated server once. */
public final class SrpStarTypeSelection {
    private static final AtomicReference<SrpStarType> PENDING = new AtomicReference<>();

    private SrpStarTypeSelection() {
    }

    public static void stage(SrpStarType starType) {
        PENDING.set(starType);
    }

    public static SrpStarType consumeOrDefault() {
        SrpStarType starType = PENDING.getAndSet(null);
        return starType == null ? SrpStarType.NORMAL : starType;
    }
}
