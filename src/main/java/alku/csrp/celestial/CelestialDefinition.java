package alku.csrp.celestial;

import net.minecraft.resources.Identifier;

public record CelestialDefinition(
        String id, Identifier texture, int minPhase, int maxPhase, float chance,
        boolean followsStars, boolean stationary, float size, float baseOpacity, float extraOpacity,
        boolean fastStreak, float rotationSpeed, boolean animated, int frameCount, int frameTimeTicks,
        float yaw, float pitch, OrbitPath orbitPath, float orbitYawRange, float orbitPitchMin,
        float orbitPitchMax, float orbitPeriodTicks, boolean oneShotOrbit) {
    public enum OrbitPath {
        NONE, RING, ARC
    }

    public boolean allowsPhase(int phase) {
        return phase >= minPhase && phase <= maxPhase;
    }
}
