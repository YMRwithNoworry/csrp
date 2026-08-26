package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Allows solid blocks to be placed against a Dead Blood fluid block when clicked directly. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DeadBloodPlacementEvents {
    private DeadBloodPlacementEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
                || !event.getLevel().getBlockState(event.getPos()).is(ModBlocks.DEAD_BLOOD.get())) {
            return;
        }

        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        InteractionResult result = blockItem.place(new BlockPlaceContext(
                event.getEntity(), event.getHand(), stack, event.getHitVec()));
        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
        }
    }
}
