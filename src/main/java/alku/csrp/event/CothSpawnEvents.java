package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.config.WorldConfig;
import alku.csrp.entity.Parasite;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.world.SrpWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Port of the Call-of-the-Hive half of {@code SRPEventHandlerBus#onEntitySpawn}
 * (out109 lines 1089-1135) together with {@code #writeCOTHTag} and {@code #setCOTH} (1137-1443).
 *
 * <p>The original applied COTH to freshly spawned living entities in two ways:
 * <ol>
 *   <li>if the node system is active and the entity stands inside a node's effect range
 *       ({@code nearestHeartAge(pos, false, 0) != -1}), it always received COTH for 3600 ticks;</li>
 *   <li>otherwise, non-parasites without the {@code srpcothimmunity} tag rolled the per-phase
 *       {@code mobSpawningCOTHChanceOne..Eight} table (phases 1..5 are 0.0, 6/7/8 are 0.2/0.4/0.8,
 *       and 9/10 have no case at all).</li>
 * </ol>
 *
 * <p>The 1.12.2 {@code srpcothimmunity} tag only existed to keep the handler from re-rolling for the
 * same entity; this port uses an equivalent persistent-data flag. Immunity itself is already
 * modelled by {@link InfectionMechanics#isCothImmune}.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class CothSpawnEvents {
    private static final String APPLIED = "csrp_coth_spawn_applied";
    /** Original {@code new PotionEffect(COTH_E, 3600, 0, false, false)}. */
    private static final int SPAWN_COTH_TICKS = 3600;

    private CothSpawnEvents() {
    }

    @SubscribeEvent
    public static void applyCothOnSpawn(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity entity)
                || entity instanceof Player) {
            return;
        }
        if (entity.getPersistentData().getBooleanOr(APPLIED, false)) {
            return;
        }
        entity.getPersistentData().putBoolean(APPLIED, true);
        if (!Config.cothActive()) {
            return;
        }
        if (!(entity instanceof Parasite)) {
            InfectionMechanics.initializeCothImmunity(entity);
        }

        SrpWorldData data = SrpWorldData.get(level);
        if (WorldConfig.nodesActivated() && data.nearestHeartAge(entity.blockPosition()) != -1) {
            InfectionMechanics.applyCothEffect(entity, null, SPAWN_COTH_TICKS, 0, false, false);
            return;
        }
        // Original: the phase roll only ran for non-parasites without the immunity tag.
        if (entity instanceof Parasite || InfectionMechanics.isCothImmune(entity)) {
            return;
        }
        if (level.getRandom().nextDouble() < Config.mobSpawningCothChance(data.evolutionPhase())) {
            InfectionMechanics.applyCothEffect(entity, null, SPAWN_COTH_TICKS, 0, false, false);
        }
    }
}
