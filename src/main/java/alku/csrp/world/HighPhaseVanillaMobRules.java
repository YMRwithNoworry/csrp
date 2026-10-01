package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.infection.InfectionMechanics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** User policy: phases eight through ten retain parasites, but not vanilla mobs. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class HighPhaseVanillaMobRules {
    private static final int START_PHASE = 8;
    private static final int CLEANUP_INTERVAL_TICKS = 20;

    private HighPhaseVanillaMobRules() {
    }

    public static boolean active(ServerLevel level) {
        return SrpWorldData.get(level).evolutionPhase() >= START_PHASE;
    }

    private static boolean isVanillaMob(Entity entity) {
        return entity instanceof Mob && !(entity instanceof Parasite)
                && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("minecraft");
    }

    public static boolean isBlocked(ServerLevel level, Entity entity) {
        return isVanillaMob(entity) && active(level);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void filterSpawnCandidates(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !active(level)) {
            return;
        }
        for (var entry : List.copyOf(event.getSpawnerDataList())) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).getNamespace().equals("minecraft")) {
                event.removeSpawnerData(entry);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventSpawn(MobSpawnEvent.PositionCheck event) {
        if (event.getLevel() instanceof ServerLevel level && isBlocked(level, event.getEntity())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventJoining(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isBlocked(level, event.getEntity())) {
            return;
        }
        // Loaded disguises must join before safely restoring their actual parasite body below.
        if (event.getEntity() instanceof Mob mob && InfectionMechanics.isHiddenAssimilated(mob)) {
            return;
        }
        event.setCanceled(true);
        event.getEntity().discard();
    }

    @SubscribeEvent
    public static void clearExistingMobs(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getGameTime() % CLEANUP_INTERVAL_TICKS != 0L || !active(level)) {
            return;
        }
        List<Mob> blocked = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (isVanillaMob(entity)) {
                blocked.add((Mob) entity);
            }
        }
        // Removal and disguise restoration mutate entity storage, so iterate a snapshot.
        for (Mob mob : blocked) {
            if (mob.isRemoved()) {
                continue;
            }
            if (InfectionMechanics.isHiddenAssimilated(mob)) {
                InfectionMechanics.revealHiddenAssimilated(mob, null);
            }
            mob.discard();
        }
    }
}
