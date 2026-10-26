package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 恢复异化花朵放入空花盆的交互。 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class PottedFlowerEvents {
    private PottedFlowerEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getFace() == Direction.UP
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.FLOWER_POT)
                || !event.getItemStack().is(ModItems.ASSIMILATED_BLOSSOM.get())) {
            return;
        }

        Level level = event.getLevel();
        if (!level.isClientSide()) {
            level.setBlock(event.getPos(),
                    ModBlocks.legacyBlock("potted_assimilated_blossom").get().defaultBlockState(), 3);
            if (!event.getEntity().getAbilities().instabuild) {
                event.getItemStack().shrink(1);
            }
        }
        event.setCancellationResult(level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        event.setCanceled(true);
    }
}
