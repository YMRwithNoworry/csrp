package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.ai.SrpAggressiveTargetGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Installs the shared SRP-vs-non-SRP aggression layer on every parasite mob. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SrpAggressionEvents {
    private static final Set<Mob> INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());

    private SrpAggressionEvents() {
    }

    @SubscribeEvent
    public static void installAggression(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)
                || !(event.getEntity() instanceof PathfinderMob mob)
                || !(mob instanceof Parasite)
                || !INSTALLED.add(mob)) {
            return;
        }

        mob.targetSelector.addGoal(0, new SrpAggressiveTargetGoal(mob));
        mob.goalSelector.addGoal(5, new MeleeAttackGoal(mob, 1.35D, true));
    }
}
