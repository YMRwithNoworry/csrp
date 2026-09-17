package alku.csrp.celestial.client;

import alku.csrp.world.SrpStarType;

public final class StarWorldClientState {
    private static SrpStarType starType = SrpStarType.NORMAL;

    private StarWorldClientState() {
    }

    public static SrpStarType starType() {
        return starType;
    }

    /** True while the client is in a cold (K-type cold star) world; drives the blizzard. */
    public static boolean isCold() {
        return starType == SrpStarType.COLD;
    }

    /** True while the client is in a warm star world. */
    public static boolean isWarm() {
        return starType == SrpStarType.WARM;
    }

    public static void update(SrpStarType value) {
        starType = value;
    }

    public static void clear() {
        starType = SrpStarType.NORMAL;
    }
}
