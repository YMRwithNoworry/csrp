package alku.csrp.world.star;

import alku.csrp.Csrp;
import alku.csrp.network.MsgSyncStarType;
import alku.csrp.world.SrpWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Port of the 1.10.9 {@code SRPStarTypeSyncHandler}: push the overworld star type to a player on
 * login, respawn and dimension change.
 *
 * <p>1.12.2 used the Forge {@code PlayerLoggedInEvent} / {@code PlayerRespawnEvent} /
 * {@code PlayerChangedDimensionEvent} trio, which 26.3 still provides as
 * {@link PlayerEvent} subclasses. The star type is read from {@link SrpWorldData} on the overworld
 * - the same source {@code world/SrpStarWorldEvents} uses for its pre-existing
 * {@code StarWorldStatePayload} - and written into the shared
 * {@code celestial.client.StarWorldClientState}, so the extra channel is idempotent rather than a
 * competing source of truth.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SRPStarTypeSyncHandler {
    private SRPStarTypeSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        send(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        send(event.getEntity());
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        send(event.getEntity());
    }

    private static void send(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.level().getServer() == null) {
            return;
        }
        ServerLevel overworld = serverPlayer.level().getServer().overworld();
        if (overworld == null) {
            return;
        }
        MsgSyncStarType.send(serverPlayer, SrpWorldData.get(overworld).starType());
    }
}
