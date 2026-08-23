package alku.csrp.block;

import alku.csrp.block.entity.ParasiticCystBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Gluttonous Cyst: a 36-slot container that slowly devours its contents and
 * turns into a Vacuous Cyst once empty. Its hardness is too high for
 * parasites to break, while players can mine it normally.
 */
public final class GluttonousCystBlock extends Block implements EntityBlock {
    public GluttonousCystBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ParasiticCystBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide ? null
                : (level1, pos, state1, blockEntity) ->
                        ParasiticCystBlockEntity.serverTick(level1, pos, state1, blockEntity);
    }

    @Override
public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ParasiticCystBlockEntity cyst)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            player.openMenu(cyst);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
            boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ParasiticCystBlockEntity cyst) {
            Containers.dropContents(level, pos, cyst);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
