package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.entity.AssimilatedEndermanEntity;
import alku.csrp.entity.FeralEndermanEntity;
import alku.csrp.entity.MarauderizedEndermanEntity;
import alku.csrp.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

/**
 * Removes dropped Eyes of the Beholder when their owner is slain by a
 * beholder. With keepInventory enabled no drops exist and no eyes are lost.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class PearlEvents {
    private PearlEvents() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player) || !Config.pearlDestroyedOnBeholderKill()) {
            return;
        }
        var source = event.getSource().getEntity();
        if (!(source instanceof FeralEndermanEntity)
                && !(source instanceof AssimilatedEndermanEntity)
                && !(source instanceof MarauderizedEndermanEntity)) {
            return;
        }
        event.getDrops().removeIf(drop -> drop.getItem().is(ModItems.PEARL.get()));
    }
}
