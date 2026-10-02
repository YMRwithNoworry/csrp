package alku.csrp.world;

import java.util.concurrent.atomic.AtomicReference;

public final class SrpColdStarSelection {
    private static final AtomicReference<Settings> NEXT = new AtomicReference<>();

    private SrpColdStarSelection() {
    }

    public static void stage(Settings settings) {
        NEXT.set(settings);
    }

    public static Settings consumeOrDefault() {
        Settings settings = NEXT.getAndSet(null);
        return settings == null ? Settings.DEFAULT : settings;
    }

    public record Settings(boolean fracturedTerrain, boolean mushroomTrees, boolean extremeSnow) {
        public static final Settings DEFAULT = new Settings(true, true, false);
    }
}
