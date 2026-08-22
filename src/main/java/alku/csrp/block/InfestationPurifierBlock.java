package alku.csrp.block;

import alku.csrp.world.BlockPurification;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
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
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state,
            Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
            BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    private static void purify(ServerLevel level, BlockPos origin) {
        BlockPos.betweenClosedStream(origin.offset(-16, -16, -16), origin.offset(16, 16, 16)).forEach(pos -> {
            BlockPurification.purify(level, pos);
        });
        level.removeBlock(origin, false);
        level.levelEvent(2001, origin, Block.getId(Blocks.SPONGE.defaultBlockState()));
    }
}
