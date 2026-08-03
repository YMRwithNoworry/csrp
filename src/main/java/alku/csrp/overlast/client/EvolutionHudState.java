package alku.csrp.overlast.client;

import alku.csrp.overlast.network.EvolutionHudPayload;

public final class EvolutionHudState {
    private static EvolutionHudPayload state = new EvolutionHudPayload(0, 0, 0, 800, false);
    private static boolean enabled = true;

    private EvolutionHudState() {
    }

    public static EvolutionHudPayload state() {
        return state;
    }

    public static boolean shouldRender() {
        return enabled && state.visible();
    }

    public static void update(EvolutionHudPayload payload) {
        state = payload;
    }

    public static void toggle() {
        enabled = !enabled;
    }
}
