package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.entity.DraconiteEntity;
import alku.csrp.entity.KirinEntity;
import alku.csrp.network.BlizzardReversePayload;
import alku.csrp.network.CsrpNetwork;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Server side of the cold-star blizzard direction sync. Port of 1.10.9's
 * {@code SRPBlizzardDerivedHandler}.
 *
 * <p>Every tenth player tick, a player standing in the overworld of a {@link SrpStarType#COLD} world
 * with a living derived parasite ({@link DraconiteEntity}, {@link KirinEntity}) within 100 blocks gets
 * {@code reverse = true}; otherwise {@code false}. A packet is only sent when the result differs from
 * the last value sent to that player — the original keeps exactly the same {@code Map<UUID, Boolean>}
 * cache and drops the entry on logout.
 *
 * <p>1.12.2 → 1.20.1 mapping: {@code EntityHeblu} → {@link DraconiteEntity} (the project's direct Citadel
 * port of 1.10.8's Heblu), {@code EntityKirin} → {@link KirinEntity};
 * {@code world.getEntitiesWithinAABB(Class, AABB)} → {@code level.getEntitiesOfClass(Class, AABB)};
 * {@code getDistanceSq(entity)} → {@code distanceToSqr(entity)};
 * {@code world.provider.getDimension() == 0} → {@code level.dimension() == Level.OVERWORLD};
 * {@code ticksExisted % 10} → {@code tickCount % 10}.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SrpBlizzardDerivedHandler {
    /** 1.10.9 {@code RANGE} — in blocks. */
    private static final double RANGE = 100.0;
    /** 1.10.9 {@code RANGE_SQ} — squared block range, compared against {@code distanceToSqr}. */
    private static final double RANGE_SQ = 10000.0;
    private static final int CHECK_INTERVAL_TICKS = 10;

    private static final Map<UUID, Boolean> LAST_STATE = new HashMap<>();

    private SrpBlizzardDerivedHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        boolean reverse = shouldReverse(player, level);
        UUID id = player.getUUID();
        Boolean previous = LAST_STATE.get(id);
        if (previous == null || previous != reverse) {
            LAST_STATE.put(id, reverse);
            CsrpNetwork.sendToPlayer(player, new BlizzardReversePayload(reverse));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_STATE.remove(event.getEntity().getUUID());
    }

    /**
     * 1.10.9 {@code shouldReverse}: overworld only, cold star only, then any living
     * {@link DraconiteEntity} or {@link KirinEntity} within {@code RANGE_SQ}.
     */
    private static boolean shouldReverse(ServerPlayer player, ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }
        if (SrpWorldData.get(level).starType() != SrpStarType.COLD) {
            return false;
        }
        AABB area = player.getBoundingBox().inflate(RANGE);
        if (hasLivingWithin(player, level.getEntitiesOfClass(DraconiteEntity.class, area))) {
            return true;
        }
        return hasLivingWithin(player, level.getEntitiesOfClass(KirinEntity.class, area));
    }

    private static boolean hasLivingWithin(ServerPlayer player, List<? extends LivingEntity> candidates) {
        for (LivingEntity candidate : candidates) {
            if (candidate.isAlive() && player.distanceToSqr(candidate) <= RANGE_SQ) {
                return true;
            }
        }
        return false;
    }
}
