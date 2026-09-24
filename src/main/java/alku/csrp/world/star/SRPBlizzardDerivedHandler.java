package alku.csrp.world.star;

import alku.csrp.Csrp;
import alku.csrp.entity.KirinEntity;
import alku.csrp.network.MsgSyncBlizzardReverse;
import alku.csrp.world.SrpStarType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Port of the 1.10.9 {@code SRPBlizzardDerivedHandler}: while a player stands within 100 blocks of
 * a Heblu or a Kirin on the cold star, the blizzard wind reverses.
 *
 * <p>1.12.2 ran this on the Forge {@code PlayerTickEvent(Phase.END)} every 10 ticks and only sent a
 * packet when the flag changed; both behaviours are preserved (26.3 uses
 * {@link PlayerTickEvent.Post} and {@link MsgSyncBlizzardReverse}). The original looked the derived
 * monsters up through {@code World#getEntitiesWithinAABB(EntityHeblu.class, ...)} / the Kirin
 * equivalent - {@link KirinEntity} exists in this port, {@code EntityHeblu} does not yet, so the
 * Heblu half is resolved by registry id ({@code csrp:heblu}) and simply stays absent until that
 * entity lands. The star type comes from {@link GenLayerSRPDynamicStar} so the check uses the same
 * generation-time source as the rest of the star features.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SRPBlizzardDerivedHandler {
    private static final double RANGE = 100.0D;
    private static final double RANGE_SQ = RANGE * RANGE;
    private static final int CHECK_INTERVAL = 10;
    /**
     * 1.10.9 registers the mob id {@code draconite} against the class {@code EntityHeblu}
     * ({@code init/SRPEntities.java:385}), so the derived handler must resolve {@code csrp:draconite}.
     * Looking up {@code csrp:heblu} always failed and left this handler's Kirin/Heblu pass inert.
     */
    private static final ResourceKey<EntityType<?>> HEBLU_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Csrp.MODID, "draconite"));
    private static final Map<UUID, Boolean> LAST_STATE = new HashMap<>();

    private static boolean hebluResolved;
    private static EntityType<?> hebluType;

    private SRPBlizzardDerivedHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        boolean reverse = shouldReverse(player);
        UUID id = player.getUUID();
        Boolean previous = LAST_STATE.get(id);
        if (previous == null || previous != reverse) {
            LAST_STATE.put(id, reverse);
            MsgSyncBlizzardReverse.send(player, reverse);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_STATE.remove(event.getEntity().getUUID());
    }

    private static boolean shouldReverse(ServerPlayer player) {
        ServerLevel level = player.level();
        if (level.dimension() != Level.OVERWORLD
                || GenLayerSRPDynamicStar.activeGenerationStarType(level) != SrpStarType.COLD) {
            return false;
        }
        AABB area = player.getBoundingBox().inflate(RANGE);
        for (KirinEntity kirin : level.getEntitiesOfClass(KirinEntity.class, area)) {
            if (kirin.isAlive() && player.distanceToSqr(kirin) <= RANGE_SQ) {
                return true;
            }
        }
        EntityType<?> heblu = hebluType(level);
        if (heblu == null) {
            return false;
        }
        List<? extends Entity> heblus = level.getEntities(heblu, area,
                entity -> entity.isAlive() && player.distanceToSqr(entity) <= RANGE_SQ);
        return !heblus.isEmpty();
    }

    /** 1.12.2 referenced {@code EntityHeblu} directly; the port resolves it by id until it exists. */
    private static EntityType<?> hebluType(ServerLevel level) {
        if (!hebluResolved) {
            hebluResolved = true;
            Optional<EntityType<?>> resolved = level.registryAccess()
                    .lookupOrThrow(Registries.ENTITY_TYPE)
                    .get(HEBLU_KEY)
                    .map(holder -> holder.value());
            hebluType = resolved.orElse(null);
        }
        return hebluType;
    }
}
