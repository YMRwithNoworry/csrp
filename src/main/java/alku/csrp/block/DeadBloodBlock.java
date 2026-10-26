package alku.csrp.block;

import alku.csrp.fluid.DeadBloodFluid;
import alku.csrp.registry.ModFluids;
import alku.csrp.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
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

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand == InteractionHand.MAIN_HAND && stack.is(Items.GLASS_BOTTLE)
                && state.getFluidState().getType() instanceof DeadBloodFluid) {
            if (!level.isClientSide()) {
                ItemStack filled = new ItemStack(ModItems.DEADBLOOD_FLUID.get());
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, filled);
                } else if (!player.getInventory().add(filled)) {
                    player.drop(filled, false, Prediction.SERVER_ONLY);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.9F, 1.0F);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
