package alku.csrp.entity;

import alku.csrp.Csrp;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Transient Dispatcher storage delay shared by the port's separate parasite base classes. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DispatcherStorageCooldown {
    private static final int DELAY = 55;
    private static final Map<Mob, Integer> REMAINING = new WeakHashMap<>();

    private DispatcherStorageCooldown() {
    }

    public static boolean canStore(Mob mob) {
        return REMAINING.getOrDefault(mob, 0) == 0;
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob instanceof Parasite)
                || !(mob.level() instanceof ServerLevel)) {
            return;
        }
        int remaining = REMAINING.getOrDefault(mob, 0);
        if (mob.getX() != mob.xo || mob.getZ() != mob.zo) {
            remaining = DELAY;
        }
        if (remaining > 1) {
            REMAINING.put(mob, remaining - 1);
        } else {
            REMAINING.remove(mob);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel)) {
            return;
        }
        if (event.getEntity() instanceof Mob victim && victim instanceof Parasite) {
            REMAINING.put(victim, DELAY);
        }
        if ((event.getSource().is(DamageTypes.MOB_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK_NO_AGGRO))
                && event.getSource().getEntity() instanceof Mob attacker && attacker instanceof Parasite) {
            REMAINING.put(attacker, DELAY);
        }
    }
}
