package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.celestial.network.StarWorldStatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Csrp.MODID)
public final class SrpStarWorldEvents {
    private SrpStarWorldEvents() {
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    public static void sync(ServerPlayer player) {
        SrpStarType starType = SrpWorldData.get(player.serverLevel().getServer().overworld()).starType();
        PacketDistributor.sendToPlayer(player, new StarWorldStatePayload(starType));
    }
}
