package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Residue sprouts that attach to exposed residue-block faces. */
public final class ResidueBloomingBlock extends DirectionalBlock {
    public static final MapCodec<ResidueBloomingBlock> CODEC = simpleCodec(ResidueBloomingBlock::new);

    public ResidueBloomingBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        return level.getBlockState(support).is(ModBlocks.RESIDUE_BLOCK);
    }

    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level,
                              BlockPos pos, RandomSource random) {
        if (random.nextInt(8) != 0) {
            return;
        }
        Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos support = pos.relative(direction);
        if (!level.getBlockState(support).is(ModBlocks.RESIDUE_BLOCK)) {
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
