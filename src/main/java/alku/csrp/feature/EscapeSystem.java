package alku.csrp.feature;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.entity.Parasite;
import alku.csrp.network.EscapeOfferPayload;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Port of SRP 1.10.9 {@code feature/EscapeOnDeathHandler} + {@code feature/EscapeRespawnHandler}.
 *
 * <p>Dying to parasites more than {@code THRESHOLD} times inside a one-minute window flags the player
 * with {@code srp_offer_escape}; the client then shows the escape button on the death screen
 * ({@code EscapeDeathScreenEvents}).  Pressing it sends {@code EscapeRequestPayload}, which sets
 * {@code srp_escape_pending}; on respawn the player is teleported to a random safe spot between
 * {@code escapeMinDistance} and {@code escapeMaxDistance} blocks away, breaking the death loop.</p>
 *
 * <p>Flags live in the player's persistent data, which is exactly what the original stored in the
 * {@code PlayerPersisted} compound.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class EscapeSystem {
    /** {@code EscapeOnDeathHandler.WINDOW_MS}. */
    private static final long WINDOW_MS = 60_000L;
    /** {@code EscapeOnDeathHandler.THRESHOLD}. */
    private static final int THRESHOLD = 5;
    private static final String OFFER_KEY = "srp_offer_escape";
    private static final String WINDOW_KEY = "srp_death_window";
    private static final String PENDING_KEY = "srp_escape_pending";
    /** {@code EscapeRespawnHandler.findSafeRandom(..., 24)}. */
    private static final int SAFE_TRIES = 24;

    private static final Map<UUID, Deque<Long>> STREAKS = new HashMap<>();

    private EscapeSystem() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!WorldConfig.escapeEnabled() || !isParasiteCause(event.getSource())) {
            return;
        }
        long nowMs = player.level().getGameTime() * 50L;
        boolean offer = registerParasiteDeath(player.getUUID(), nowMs);
        CompoundTag data = player.getPersistentData();
        data.putBoolean(OFFER_KEY, offer);
        data.putLong(WINDOW_KEY, nowMs);
        EscapeOfferPayload.send(player, offer);
    }

    /**
     * Core of {@code EscapeOnDeathHandler.onDeath}: record one parasite-caused death and report whether
     * the player has now died more than {@code THRESHOLD} times inside {@code WINDOW_MS}.
     *
     * <p>Split out from the event handler so the streak rule can be exercised at runtime without a
     * player (NeoForge refuses custom payloads to GameTest mock players).</p>
     */
    public static boolean registerParasiteDeath(UUID playerId, long nowMs) {
        Deque<Long> streak = STREAKS.computeIfAbsent(playerId, key -> new ArrayDeque<>());
        streak.addLast(nowMs);
        while (!streak.isEmpty() && nowMs - streak.peekFirst() > WINDOW_MS) {
            streak.removeFirst();
        }
        return streak.size() > THRESHOLD;
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.getBooleanOr(PENDING_KEY, false)) {
            return;
        }
        data.putBoolean(PENDING_KEY, false);
        clearOffer(player);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int min = Math.max(0, WorldConfig.escapeMinDistance());
        int max = Math.max(min, WorldConfig.escapeMaxDistance());
        BlockPos target = findSafeRandom(level, player.blockPosition(), min, max, SAFE_TRIES);
        if (target != null) {
            player.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        }
    }

    /** {@code C2SRequestEscape}: mark this player as escaping so the respawn hook teleports them. */
    public static void markEscapePending(ServerPlayer player) {
        player.getPersistentData().putBoolean(PENDING_KEY, true);
    }

    /** {@code EscapeOnDeathHandler.clearOffer}. */
    public static void clearOffer(ServerPlayer player) {
        player.getPersistentData().putBoolean(OFFER_KEY, false);
        EscapeOfferPayload.send(player, false);
    }

    /**
     * {@code EscapeOnDeathHandler.isSrparasitesCause}: the original accepted a direct/causing entity whose
     * class lived in the mod package, or a damage type whose name carried the mod id.  In 26.3 that is a
     * parasite entity, or a damage type registered in the mod's namespace.
     */
    private static boolean isParasiteCause(DamageSource source) {
        if (source == null) {
            return false;
        }
        if (source.getDirectEntity() instanceof Parasite || source.getEntity() instanceof Parasite) {
            return true;
        }
        return source.typeHolder().unwrapKey()
                .map(key -> Csrp.MODID.equals(key.identifier().getNamespace()))
                .orElse(false);
    }

    /**
     * {@code EscapeRespawnHandler.findSafeRandom}: pick a random angle and distance, take the surface
     * there and accept it only when the two blocks above the ground are free air.
     */
    private static BlockPos findSafeRandom(ServerLevel level, BlockPos origin, int min, int max, int tries) {
        for (int attempt = 0; attempt < tries; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
            int distance = min + level.getRandom().nextInt(Math.max(1, max - min + 1));
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
            if (isSafe(level, feet)) {
                return feet;
            }
        }
        return null;
    }

    /**
     * {@code EscapeRespawnHandler.isSafe}: standable ground that is not a cactus, with clear air for the
     * feet and the head and no liquid in either space.
     */
    private static boolean isSafe(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinY()) {
            return false;
        }
        BlockPos groundPos = feet.below();
        BlockState ground = level.getBlockState(groundPos);
        if (!ground.isFaceSturdy(level, groundPos, Direction.UP) || ground.is(Blocks.CACTUS)) {
            return false;
        }
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(feet.above());
        if (!feetState.isAir() || !headState.isAir()) {
            return false;
        }
        return !feetState.liquid() && !headState.liquid();
    }
}
