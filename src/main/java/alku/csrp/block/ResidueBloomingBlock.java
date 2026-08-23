package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Residue sprouts that attach to exposed residue-block faces. */
public final class ResidueBloomingBlock extends DirectionalBlock {

    public ResidueBloomingBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        return level.getBlockState(support).is(ModBlocks.RESIDUE_BLOCK.get());
    }

    @Override
public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level,
                              BlockPos pos, RandomSource random) {
        if (random.nextInt(8) != 0) {
            return;
        }
        Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos support = pos.relative(direction);
        if (!level.getBlockState(support).is(ModBlocks.RESIDUE_BLOCK.get())) {
            return;
        }
        Direction face = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos target = support.relative(face);
        BlockState blooming = defaultBlockState().setValue(FACING, face);
        if (level.getBlockState(target).isAir() && blooming.canSurvive(level, target)) {
            level.setBlock(target, blooming, Block.UPDATE_CLIENTS);
        }
    }
}
