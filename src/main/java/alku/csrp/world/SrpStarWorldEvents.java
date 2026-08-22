package alku.csrp.world;

import alku.csrp.network.CsrpNetwork;
import alku.csrp.Csrp;
import alku.csrp.celestial.network.StarWorldStatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;

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
        CsrpNetwork.sendToPlayer(player, new StarWorldStatePayload(starType));
    }
}
