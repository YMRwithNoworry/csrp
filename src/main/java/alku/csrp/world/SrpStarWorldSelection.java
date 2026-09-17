package alku.csrp.world;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Transfers the two 1.10.9 cold-star world-creation toggles (fractured terrain, mushroom trees) from
 * the create-world screen to the integrated server exactly once.
 *
 * <p>This mirrors the existing {@link SrpStarTypeSelection} / {@link SrpMeteorSelection}
 * {@code AtomicReference} pattern; those classes are deliberately left untouched.
 *
 * <p>Slice 1: skeleton only. {@link SrpWorldData#initialize} already consumes it (both toggles default
 * to {@code true}, matching 1.10.9). Wiring an actual UI button into
 * {@code alku.csrp.client.SrpDifficultyScreenEvents} is the optional slice 6.
 */
public final class SrpStarWorldSelection {
    private static final AtomicReference<Boolean> PENDING_FRACTURED_TERRAIN = new AtomicReference<>();
    private static final AtomicReference<Boolean> PENDING_MUSHROOM_TREES = new AtomicReference<>();

    private SrpStarWorldSelection() {
    }

    /** Stage both toggles at once; call before the integrated server creates the world. */
    public static void stage(boolean fracturedTerrain, boolean mushroomTrees) {
        PENDING_FRACTURED_TERRAIN.set(fracturedTerrain);
        PENDING_MUSHROOM_TREES.set(mushroomTrees);
    }

    /** 1.10.9 default for {@code fracturedTerrain} is {@code true}. */
    public static boolean consumeFracturedTerrainOrDefault() {
        Boolean value = PENDING_FRACTURED_TERRAIN.getAndSet(null);
        return value == null ? true : value;
    }

    /** 1.10.9 default for {@code mushroomTrees} is {@code true}. */
    public static boolean consumeMushroomTreesOrDefault() {
        Boolean value = PENDING_MUSHROOM_TREES.getAndSet(null);
        return value == null ? true : value;
    }

    /** Discard any staged value (e.g. the world-creation screen was cancelled). */
    public static void clear() {
        PENDING_FRACTURED_TERRAIN.set(null);
        PENDING_MUSHROOM_TREES.set(null);
    }

    // TODO(C, optional slice 6): once SrpDifficultyScreenEvents stages these toggles, the two booleans
    // must also be cleared on cancel; see SrpMeteorSelection for the existing cancel handling.
}
