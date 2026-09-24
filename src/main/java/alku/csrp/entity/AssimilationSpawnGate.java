package alku.csrp.entity;

import alku.csrp.config.MobsConfig;
import alku.csrp.world.SrpWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Port of SRParasites 1.10.9 {@code entity.ai.misc.EntityCanSpawn}.
 *
 * <p>In 1.12.2 the interface exposed two ints: {@code getIDSpawn()} (a counter key) and
 * {@code canSpawnByIDData()} (the required count). {@code SRPSpawning} then denied natural spawning
 * while the world counter was still below the requirement:
 *
 * <pre>
 * // out109/init/SRPSpawning.java:538-546
 * if (parasite instanceof EntityCanSpawn parasiteSus) {
 *     if (sopa.getNumberIDDataSpawn(parasiteSus.getIDSpawn()) &lt; parasiteSus.canSpawnByIDData()) {
 *         event.setResult(Result.DENY);
 *         return;
 *     }
 * }
 * </pre>
 *
 * <p>26.3 replaces the numeric counter key with the entity id string used by
 * {@link MobsConfig#neededAssimilation(String)} and {@link SrpWorldData#assimilationCount(String)}.
 * The comparison direction is preserved exactly: a type must have been assimilated
 * {@code neededAssimilation(key)} times before it may spawn naturally again, and {@code -1} disables
 * the gate (the original {@code 0} behaves the same way because {@code count &lt; 0} is never true).
 */
public interface AssimilationSpawnGate {
    /**
     * Original {@code getIDSpawn()} expressed as the 1.10.9 assimilated-count key, or {@code null}
     * when the entity is not gated.
     *
     * <p>Deliberately defaulted to {@code null}: only the types that really implemented
     * {@code EntityCanSpawn} in 1.10.9 need to override it, and a class that merely inherits the
     * marker (for example the {@code Marauderized*} family, which never implemented
     * {@code EntityCanSpawn}) stays ungated instead of breaking the build.
     */
    default String assimilationSpawnKey() {
        return null;
    }

    /** Original {@code canSpawnByIDData()}. */
    default int canSpawnByIDData() {
        String key = assimilationSpawnKey();
        return key == null ? -1 : MobsConfig.neededAssimilation(key);
    }

    /** {@code true} when the world has not assimilated this type often enough yet. */
    default boolean blockedByAssimilationCount(ServerLevel level) {
        int threshold = canSpawnByIDData();
        return threshold >= 0 && SrpWorldData.get(level).assimilationCount(assimilationSpawnKey()) < threshold;
    }

    /** Original {@code SRPSpawning:538-546} expressed as an allow flag. */
    default boolean assimilationGateAllows(ServerLevel level) {
        return !blockedByAssimilationCount(level);
    }

    /** Convenience for the natural-spawn gate. */
    static boolean blocksNaturalSpawn(ServerLevel level, Entity entity) {
        return entity instanceof AssimilationSpawnGate gate && gate.blockedByAssimilationCount(level);
    }

    /**
     * Original {@code SRPSaveData.addNumberIDDataSpawn(entityout.getParasiteIDRegister())}
     * (out109 {@code util/ParasiteEventEntity.java:645/889/992}), called on every successful
     * assimilation conversion.
     */
    static void recordAssimilation(ServerLevel level, Entity converted) {
        if (converted instanceof AssimilationSpawnGate gate) {
            String key = gate.assimilationSpawnKey();
            if (key != null) {
                SrpWorldData.get(level).addAssimilationCount(key);
            }
        }
    }
}
