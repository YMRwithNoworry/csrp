package alku.csrp.entity;

import alku.csrp.Csrp;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Applies the {@link AssimilationSpawnGate} contract to natural spawning.
 *
 * <p>Mirrors the {@code EntityCanSpawn} branch of {@code SRPSpawning} (out109
 * {@code init/SRPSpawning.java:538-546}): a simulated/hijacked type must have been assimilated
 * {@code neededAssimilation(key)} times in the world before it may spawn naturally again.
 * {@code MobSpawnEvent.PositionCheck} is the 26.3 hook that runs before the mob is added
 * ({@code NaturalSpawner.isValidPositionForMob}).
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class AssimilationSpawnGateEvents {
    private AssimilationSpawnGateEvents() {
    }

    @SubscribeEvent
    public static void gateAssimilationSpawns(MobSpawnEvent.PositionCheck event) {
        EntitySpawnReason reason = event.getSpawnType();
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (AssimilationSpawnGate.blocksNaturalSpawn(level, event.getEntity())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
