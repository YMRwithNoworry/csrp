package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Skeleton for SRParasites 1.10.9 {@code BlockSnowCoveredGrass} (a grass block that is permanently
 * snow-covered and reverts to vanilla grass once the snow grass above it is gone).
 *
 * <p>Reference: {@code _srp-orig/decomp-1.10.9/.../block/BlockSnowCoveredGrass.java} —
 * {@code extends BlockGrass}, hardness {@code 0.6F}, {@code getActualState} forces
 * {@code SNOWY = true}, drops dirt, pick-block gives vanilla grass, reverts on neighbour change and
 * during random ticks when no snow grass sits above.
 *
 * <p>1.20.1 mapping notes (already verified, see PLAN.md §4.3 / §10.1):
 * <ul>
 *   <li>{@code GrassBlock extends SpreadingSnowyDirtBlock extends SnowyDirtBlock}; the inherited
 *       {@code SnowyDirtBlock.SNOWY} ({@code BooleanProperty}) is therefore usable directly, and the
 *       {@code snowy} state is already part of {@code GrassBlock}'s state definition — this class must
 *       <b>not</b> re-add it.</li>
 *   <li>Hardness {@code 0.6F} maps to {@code Properties.strength(0.6F)} and the 1.10.9 class'
 *       {@code updateTick} maps to {@code Properties.randomTicks()} +
 *       {@code randomTick(BlockState, ServerLevel, BlockPos, RandomSource)}.</li>
 *   <li>1.10.9 {@code getActualState} has no 1.20.1 equivalent; C must force {@code SNOWY = true} in
 *       {@code getStateForPlacement(BlockPlaceContext)} and keep it that way in {@code randomTick}.</li>
 *   <li>No {@code BlockItem} is registered for this block, matching 1.10.9.</li>
 * </ul>
 *
 * <p>Slice 1: subclass wiring only. The behavioural overrides below are TODO(C).
 */
public class SnowCoveredGrassBlock extends GrassBlock {
    public SnowCoveredGrassBlock(Properties properties) {
        super(properties);
    }

    /**
     * 1.10.9 {@code updateTick}: revert to vanilla grass when no snow grass is above, otherwise run the
     * vanilla grass spreading logic. TODO(C): implement the guard, then call
     * {@code super.randomTick(state, level, pos, random)}.
     *
     * <p>Overriding this already compiles and behaves exactly like vanilla {@code GrassBlock} in
     * slice 1, which is the intended no-op skeleton.
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
    }

    // TODO(C): override getStateForPlacement(BlockPlaceContext) to return
    //   defaultBlockState().setValue(SnowyDirtBlock.SNOWY, true) (1.10.9 getActualState equivalent).
    // TODO(C): override neighborChanged(BlockState, Level, BlockPos, Block, BlockPos, boolean) to
    //   revert to Blocks.GRASS_BLOCK.defaultBlockState() with flag 3 when the block above is neither
    //   ModBlocks.SNOW_TALL_GRASS nor ModBlocks.SNOW_SHORT_GRASS.
    // TODO(C): override playerDestroy(...) so the block drops Items.DIRT, and getCloneItemStack(...)
    //   so pick-block yields Items.GRASS_BLOCK — both matching 1.10.9.
}
