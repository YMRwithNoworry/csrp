package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class InfestationPurifierBlock extends Block {
    public InfestationPurifierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel) {
            purify(serverLevel, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state,
            Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
            BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void purify(ServerLevel level, BlockPos origin) {
        BlockPos.betweenClosedStream(origin.offset(-16, -16, -16), origin.offset(16, 16, 16)).forEach(pos -> {
            BlockState current = level.getBlockState(pos);
            Block replacement = current.is(ModBlocks.INFESTED_STAIN) ? Blocks.DIRT
                    : current.is(ModBlocks.INFESTED_TRUNK) ? Blocks.OAK_LOG
                    : current.is(ModBlocks.INFESTED_RUBBLE) || current.is(ModBlocks.INFESTED_COBBLESTONE)
                            ? Blocks.STONE
                    : current.is(ModBlocks.INFESTED_SAND) ? Blocks.SAND
                    : current.is(ModBlocks.INFESTED_PLANKS) ? Blocks.OAK_PLANKS : null;
            if (replacement != null) {
                level.setBlock(pos, replacement.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        level.removeBlock(origin, false);
        level.levelEvent(2001, origin, Block.getId(Blocks.SPONGE.defaultBlockState()));
    }
}
