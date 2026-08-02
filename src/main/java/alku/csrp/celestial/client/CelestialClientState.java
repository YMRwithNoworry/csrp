package alku.csrp.celestial.client;

import java.util.Set;

public final class CelestialClientState {
    private static Set<String> active = Set.of();
    private static long nightIndex;
    private static long syncedGameTime;

    private CelestialClientState() {
    }

    public static void update(Set<String> ids, long night, long gameTime) {
        active = Set.copyOf(ids);
        nightIndex = night;
        syncedGameTime = gameTime;
    }

    public static Set<String> active() { return active; }
    public static boolean isActive(String id) { return active.contains(id); }
    public static long nightIndex() { return nightIndex; }
    public static long syncedGameTime() { return syncedGameTime; }
}
