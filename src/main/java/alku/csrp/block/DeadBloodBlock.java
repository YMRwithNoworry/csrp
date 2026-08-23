package alku.csrp.block;

import alku.csrp.registry.ModFluids;
import alku.csrp.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Dead Blood liquid block. Glass bottles collect Dead Blood Fluid; buckets
 * work through the vanilla BucketPickup interface.
 */
public final class DeadBloodBlock extends LiquidBlock {
    public DeadBloodBlock(Properties properties) {
        super(ModFluids.DEADBLOOD.get(), properties);
    }

    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.GLASS_BOTTLE) && state.getFluidState().isSource()) {
            if (!level.isClientSide) {
                ItemStack filled = new ItemStack(ModItems.DEADBLOOD_FLUID.get());
                if (player.getAbilities().instabuild) {
                    player.getInventory().add(filled);
                } else {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, filled);
                    } else {
                        player.getInventory().add(filled);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hitResult);
    }
}
