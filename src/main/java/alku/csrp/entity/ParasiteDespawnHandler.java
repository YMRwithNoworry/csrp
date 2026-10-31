package alku.csrp.entity;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/** The original Dispatcher link is transient and refers to the actual deploying parent. */
public final class ParasiteDespawnHandler {
    private static final Map<Mob, WeakReference<NexusParasiteEntity>> DISPATCHER_OWNERS = new WeakHashMap<>();

    private ParasiteDespawnHandler() {
    }

    static void setDispatcherOwner(Mob mob, NexusParasiteEntity owner) {
        if (mob instanceof Parasite && owner != null) {
            DISPATCHER_OWNERS.put(mob, new WeakReference<>(owner));
        }
    }

    public static void onNaturalDespawn(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !(mob instanceof Parasite) || !mob.isAlive()) {
            return;
        }
        ParasiteBlockInventory.launchCyst(level, mob);
        WeakReference<NexusParasiteEntity> reference = DISPATCHER_OWNERS.remove(mob);
        NexusParasiteEntity parent = reference == null ? null : reference.get();
        if (parent != null && parent.isAlive() && parent.level() == level) {
            Entity loadedParent = level.getEntity(parent.getUUID());
            if (loadedParent == parent && parent.isDispatcherFamily()) {
                parent.storeDispatcherParasite(mob);
            }
        }
    }
}
