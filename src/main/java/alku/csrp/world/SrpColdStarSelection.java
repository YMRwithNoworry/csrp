package alku.csrp.world;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Transfers the two 1.10.9 cold-star create-world toggles
 * ({@code GuiSRPWorldSettings.BTN_MUSHROOM_TREES} / {@code BTN_FRACTURED_TERRAIN}) from the
 * create-world screen to the integrated server once, mirroring {@link SrpMeteorSelection}.
 */
public final class SrpColdStarSelection {
    private static final AtomicReference<Boolean> PENDING_FRACTURED = new AtomicReference<>();
    private static final AtomicReference<Boolean> PENDING_MUSHROOM_TREES = new AtomicReference<>();

    private SrpColdStarSelection() {
    }

    public static void stage(boolean fracturedTerrain, boolean mushroomTrees) {
        PENDING_FRACTURED.set(fracturedTerrain);
        PENDING_MUSHROOM_TREES.set(mushroomTrees);
    }

    /** @return the staged choice, or {@code null} when the creator never opened the option. */
    public static Boolean consumeFractured() {
        return PENDING_FRACTURED.getAndSet(null);
    }

    public static Boolean consumeMushroomTrees() {
        return PENDING_MUSHROOM_TREES.getAndSet(null);
    }
}
