package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.AssimilatedEndermanEntity;
import alku.csrp.entity.FeralEndermanEntity;
import alku.csrp.entity.MarauderizedEndermanEntity;
import alku.csrp.registry.ModItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Removes every Eye of the Beholder from a player slain by an Enderman
 * variant, mimicking ownership transfer back to the beholder.
 */
@EventBusSubscriber(modid = Csrp.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PearlEvents {
    private PearlEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        var source = event.getSource().getEntity();
        if (!(source instanceof FeralEndermanEntity)
                && !(source instanceof AssimilatedEndermanEntity)
                && !(source instanceof MarauderizedEndermanEntity)) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.PEARL.get())) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }
}
