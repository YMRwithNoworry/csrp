package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class DeadheadLeavesBlock extends LeavesBlock {
    public static final BooleanProperty SNOWY = BlockStateProperties.SNOWY;

    /**
     * {@code BlockDeadheadLeaves}: {@code int decayDistance = 7}
     * (out109 {@code block/BlockDeadheadLeaves.java:28}).
     */
    public static final int DECAY_DISTANCE = 7;
    private static final int MAX_VANILLA_DISTANCE = 7;

    public DeadheadLeavesBlock(BlockBehaviour.Properties properties) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
        registerDefaultState(defaultBlockState().setValue(SNOWY, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(SNOWY, isSnowy(context.getLevel(), context.getClickedPos()));
    }

    /**
     * Reproduces {@code BlockDeadheadLeaves.func_180650_b}: the original ran its own seven-block BFS
     * seeded by blocks answering {@code canSustainLeaves} — in this port the SRP trunk
     * ({@code BlockParasiteTrunk.java:123}) — and removed the leaf once nothing sustain-able was in
     * range.  26.3 expresses that through {@link LeavesBlock#DISTANCE}, so the distance is recomputed
     * against the SRP trunk (or a vanilla log) before the vanilla decay check runs.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int distance = distanceToSustainSupport(level, pos);
        if (distance != state.getValue(DISTANCE)) {
            state = state.setValue(DISTANCE, distance);
            level.setBlock(pos, state, 2);
        }
        boolean snowy = isSnowy(level, pos);
        if (state.getValue(SNOWY) != snowy) {
            state = state.setValue(SNOWY, snowy);
            level.setBlock(pos, state, 2);
        }
        super.randomTick(state, level, pos, random);
    }

    /** Original seven-block decay scan ({@code decayDistance = 7}). */
    static int distanceToSustainSupport(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        int best = MAX_VANILLA_DISTANCE;
        for (BlockPos candidate : BlockPos.betweenClosed(
                pos.offset(-DECAY_DISTANCE, -DECAY_DISTANCE, -DECAY_DISTANCE),
                pos.offset(DECAY_DISTANCE, DECAY_DISTANCE, DECAY_DISTANCE))) {
            if (!ParasiteTrunkBlock.sustainsDeadheadLeaves(level.getBlockState(candidate))) {
                continue;
            }
            int distance = Math.max(1,
                    (int) Math.round(Math.sqrt(candidate.distSqr(pos))));
            if (distance < best) {
                best = distance;
            }
        }
        return Math.min(MAX_VANILLA_DISTANCE, best);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SNOWY);
    }

    private static boolean isSnowy(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(Blocks.SNOW) || above.is(Blocks.SNOW_BLOCK);
    }
}
