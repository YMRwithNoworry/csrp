package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.HiGolemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

/**
 * Port of {@code SRPEventHandlerBus#onEntityMount} (out109 lines 1374-1386).
 *
 * <p>Once a HiGolem grabs a living player, the original refused to let a non-creative, still-alive
 * player dismount it; the only ways out were dying or switching to creative.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class HiGolemMountEvents {
    private HiGolemMountEvents() {
    }

    @SubscribeEvent
    public static void preventDismount(EntityMountEvent event) {
        if (!event.isDismounting()
                || !(event.getEntityBeingMounted() instanceof HiGolemEntity)
                || !(event.getEntityMounting() instanceof Player player)) {
            return;
        }
        if (!player.getAbilities().instabuild && player.getHealth() > 0.0F) {
            event.setCanceled(true);
        }
    }
}
