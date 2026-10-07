package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.entity.Parasite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Port of {@code SRPEventHandlerBus#setNewCreatureTask} (out109 lines 1389-1397), called from
 * {@code #onEntitySpawn} for every non-parasite {@code EntityCreature}.
 *
 * <p>The original gave ordinary creatures a parasite relationship from the
 * {@code SRPConfig.entitiesWillAttack} / {@code entitiesWillAvoid} string lists
 * ({@code ParasiteEventEntity#checkName}, i.e. {@code contains} matching with an inverted flag):</p>
 * <ul>
 *   <li>{@code entitiesWillAttack} (default {@code minecraft:villager_golem}) -&gt; an
 *       {@code EntityAINearestAttackableTarget} against parasites at priority 5;</li>
 *   <li>otherwise {@code entitiesWillAvoid} (default {@code minecraft:villager}) -&gt; an
 *       {@code EntityAIAvoidEntity} for parasites at distance 12, walk/sprint speed 0.8.</li>
 * </ul>
 *
 * <p>26.3's goals take a {@code Class<T>}, so the parasite interface is matched through the
 * targeting predicate instead of a class literal. The original had no "already applied" guard, but
 * 26.3 can fire {@code EntityJoinLevelEvent} more than once per entity, so an equivalent
 * persistent-data flag keeps the goals from stacking.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class CreatureParasiteTaskEvents {
    private static final String APPLIED = "csrp_creature_parasite_tasks_applied";
    /** Original {@code new EntityAIAvoidEntity(entity, EntityParasiteBase.class, 12.0F, 0.8, 0.8)}. */
    private static final float AVOID_DISTANCE = 12.0F;
    private static final double AVOID_WALK_SPEED = 0.8D;
    private static final double AVOID_SPRINT_SPEED = 0.8D;
    /** Original goal priority 5. */
    private static final int GOAL_PRIORITY = 5;

    private CreatureParasiteTaskEvents() {
    }

    @SubscribeEvent
    public static void applyCreatureParasiteTasks(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof PathfinderMob mob)
                || mob instanceof Parasite
                || mob.getPersistentData().getBooleanOr(APPLIED, false)) {
            return;
        }
        mob.getPersistentData().putBoolean(APPLIED, true);
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        if (Config.mobHuntsParasites(id)) {
            mob.targetSelector.addGoal(GOAL_PRIORITY, new NearestAttackableTargetGoal<>(mob, LivingEntity.class,
                    10, true, false, (target, serverLevel) -> target instanceof Parasite));
        } else if (Config.mobFleesParasites(id)) {
            mob.goalSelector.addGoal(GOAL_PRIORITY, new AvoidEntityGoal<LivingEntity>(mob, LivingEntity.class,
                    target -> target instanceof Parasite,
                    AVOID_DISTANCE, AVOID_WALK_SPEED, AVOID_SPRINT_SPEED, target -> true));
        }
    }
}
