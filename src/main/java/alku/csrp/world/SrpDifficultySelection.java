package alku.csrp.world;

import java.util.concurrent.atomic.AtomicReference;

/** Transfers a create-world selection to the integrated server exactly once. */
public final class SrpDifficultySelection {
    private static final AtomicReference<SrpDifficulty> PENDING = new AtomicReference<>();

    private SrpDifficultySelection() {
    }

    public static void stage(SrpDifficulty difficulty) {
        PENDING.set(difficulty);
    }

    public static SrpDifficulty consumeOrDefault() {
        SrpDifficulty difficulty = PENDING.getAndSet(null);
        return difficulty == null ? SrpDifficulty.NORMAL : difficulty;
    }
}
