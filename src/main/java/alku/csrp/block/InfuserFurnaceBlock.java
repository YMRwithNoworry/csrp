package alku.csrp.block;

import alku.csrp.block.entity.InfuserFurnaceBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Port of SRP 1.10.9 {@code block/BlockInfuserFurnace}: a {@code BlockContainer} with a single
 * {@code FACING} property and no lit state.
 *
 * <p>The original declared {@code FACING = BlockHorizontal.FACING}, returned
 * {@code placer.getHorizontalFacing().getOpposite()} from {@code getStateForPlacement}, and its
 * blockstate carries the four {@code facing=*} variants — so the ported block has to be directional for
 * the model to sit the right way round.</p>
 */
public final class InfuserFurnaceBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public InfuserFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** {@code BlockInfuserFurnace.getStateForPlacement}: face the placer. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfuserFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : (level1, pos, state1, blockEntity) ->
                        InfuserFurnaceBlockEntity.serverTick(level1, pos, state1, blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof InfuserFurnaceBlockEntity furnace)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.openMenu(furnace);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
