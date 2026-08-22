package alku.csrp.block;

import alku.csrp.block.entity.RelayTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Redirects interaction with the tower's upper sections to its terminal. */
public final class RelayTowerPartBlock extends Block {
    private final int terminalOffset;

    public RelayTowerPartBlock(int terminalOffset, Properties properties) {
        super(properties);
        this.terminalOffset = terminalOffset;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        BlockPos terminalPos = pos.below(terminalOffset);
        if (level.getBlockEntity(terminalPos) instanceof RelayTerminalBlockEntity relay && relay.isFormed()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.openMenu(relay);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }
}
