package alku.csrp.celestial;

import alku.csrp.Csrp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class CelestialCatalog {
    private static final Map<String, CelestialDefinition> BY_ID = new LinkedHashMap<>();

    public static final List<CelestialDefinition> ALL = List.of(
            basic("mercury", "solar/mercury.png", 0, 10, 0.40F, false, true, 1.85F, 2, 2, 1.2F, 20, 137, 31),
            basic("mars", "solar/mars.png", 0, 10, 0.35F, false, true, 1, 2, 2, 1, 20, 211, 42),
            basic("jupiter", "solar/jupiter.png", 0, 10, 0.25F, false, true, 2, 2, 2, 0.35F, 20, 73, 47),
            basic("saturn", "solar/saturn.png", 0, 10, 0.22F, false, true, 1.55F, 2, 2, 0.32F, 20, 304, 35),
            basic("uranus", "solar/uranus.png", 0, 10, 1.0F, false, true, 1.15F, 2, 2, 0.15F, 20, 169, 20),
            basic("neptune", "solar/neptune.png", 0, 10, 0.15F, false, true, 1.1F, 2, 2, 0.22F, 20, 255, 53),
            basic("pluto", "solar/pluto.png", 0, 10, 0.08F, false, true, 1, 1, 0.1F, 0, 20, 25, 18),
            basic("venus", "solar/venus.png", 0, 10, 0.75F, false, true, 2.1F, 2, 2, 0.18F, 20, 112, 48),
            orbit("blip", "bld_planet.png", 0, 10, 1, true, false, 4, 1, 0.1F, false, 0,
                    false, 1, 20, 0, 60, CelestialDefinition.OrbitPath.RING, 360, 60, 60, 9000, false),
            basic("pulse", "star1.png", 3, 10, 1, false, true, 10, 1, 0.1F, 0, 0, 40, 45),
            basic("eight", "eight.png", 4, 10, 0.005F, false, true, 20, 1, 0.1F, 0, 0, 180, 30),
            basic("twenty_seven", "twenty_seven.png", 2, 10, 0.01F, false, true, 90, 1, 1, 0, 20, 250, 50),
            orbit("three", "three.png", 5, 10, 0.15F, false, false, 6, 1, 0.1F, false, 4,
                    false, 1, 15, 60, 40, CelestialDefinition.OrbitPath.RING, 140, 30, 45, 12000, false),
            orbit("eighty_three", "eighty_three.png", 7, 10, 0.05F, false, true, 5, 1, 0.1F, false, 0,
                    true, 4, 20, 310, 70, CelestialDefinition.OrbitPath.NONE, 0, 0, 0, 0, false),
            orbit("four_comet", "four_comet.png", 3, 10, 0.35F, false, false, 6, 1, 0.1F, true, 0,
                    true, 3, 0, 0, 15, CelestialDefinition.OrbitPath.ARC, 180, 5, 25, 900, true),
            basic("arrow", "tetrahedron.png", 0, 10, 0.005F, false, true, 4, 2, 2, 0.35F, 20, 90, 47),
            basic("dark_days", "black_sky.png", 0, 10, 0.0005F, false, true, 1, 1, 0, 0, 20, 0, 90));

    static {
        ALL.forEach(definition -> BY_ID.put(definition.id(), definition));
    }

    private CelestialCatalog() {
    }

    public static CelestialDefinition get(String id) {
        return BY_ID.get(id);
    }

    public static boolean contains(String id) {
        return BY_ID.containsKey(id);
    }

    private static CelestialDefinition basic(String id, String texture, int minPhase, int maxPhase,
            float chance, boolean followsStars, boolean stationary, float size, float baseOpacity,
            float extraOpacity, float rotation, int frameTime, float yaw, float pitch) {
        return orbit(id, texture, minPhase, maxPhase, chance, followsStars, stationary, size,
                baseOpacity, extraOpacity, false, rotation, false, 1, frameTime, yaw, pitch,
                CelestialDefinition.OrbitPath.NONE, 0, 0, 0, 0, false);
    }

    private static CelestialDefinition orbit(String id, String texture, int minPhase, int maxPhase,
            float chance, boolean followsStars, boolean stationary, float size, float baseOpacity,
            float extraOpacity, boolean fastStreak, float rotation, boolean animated, int frameCount,
            int frameTime, float yaw, float pitch, CelestialDefinition.OrbitPath path, float yawRange,
            float pitchMin, float pitchMax, float period, boolean oneShot) {
        return new CelestialDefinition(id, ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                "textures/celestial/" + texture), minPhase, maxPhase, chance, followsStars, stationary,
                size, baseOpacity, extraOpacity, fastStreak, rotation, animated, frameCount, frameTime,
                yaw, pitch, path, yawRange, pitchMin, pitchMax, period, oneShot);
    }
}
