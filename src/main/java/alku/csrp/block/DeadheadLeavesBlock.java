package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class DeadheadLeavesBlock extends LeavesBlock {
    public static final BooleanProperty SNOWY = BlockStateProperties.SNOWY;

    public DeadheadLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SNOWY, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(SNOWY, isSnowy(context.getLevel(), context.getClickedPos()));
    }

    @Override
public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        BlockState currentState = level.getBlockState(pos);
        if (currentState.is(this)) {
            boolean snowy = isSnowy(level, pos);
            if (currentState.getValue(SNOWY) != snowy) {
                level.setBlock(pos, currentState.setValue(SNOWY, snowy), 2);
            }
        }
    }

    @Override
public void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SNOWY);
    }

    private static boolean isSnowy(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(Blocks.SNOW) || above.is(Blocks.SNOW_BLOCK);
    }
}
