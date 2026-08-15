package alku.csrp.celestial.client;

import alku.csrp.world.SrpStarType;

public final class StarWorldClientState {
    private static SrpStarType starType = SrpStarType.NORMAL;

    private StarWorldClientState() {
    }

    public static SrpStarType starType() {
        return starType;
    }

    public static void update(SrpStarType value) {
        starType = value;
    }

    public static void clear() {
        starType = SrpStarType.NORMAL;
    }
}
