package alku.csrp.block;

import alku.csrp.infection.BlockInfestation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockHarleskinnFence}
 * (out109 {@code block/BlockHarleskinnFence.java}) — the two fence compatibility ids
 * ({@code harleskinn_fence}, {@code bruisewood_fence}).
 *
 * <p>Like {@link LegacyWallBlock}, the original was a plain {@code BlockFence} with
 * {@code tickRandom = true} plus the shared "touch any infestation → beckon" tick
 * ({@code BlockHarleskinnFence.java:16-55}), hardness 2.0F and the flesh sound.  The vanilla
 * {@link FenceBlock} provides the {@code north/east/south/west} state the {@code multipart}
 * blockstates expect.</p>
 */
public class LegacyFenceBlock extends FenceBlock {
    public LegacyFenceBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        scheduleCheck(level, pos, 10);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        scheduleCheck(level, pos, 10);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tick(state, level, pos, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (LegacySlabBlock.touchingAnyInfestation(level, pos)) {
            BlockInfestation.infestAround(level, pos, 1);
            level.scheduleTick(pos, this, 20);
        }
    }

    private void scheduleCheck(Level level, BlockPos pos, int delay) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, delay);
        }
    }
}
