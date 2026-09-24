package alku.csrp.block;

import alku.csrp.infection.BlockInfestation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockWallBase}
 * (out109 {@code block/BlockWallBase.java}) — the six {@code *_wall} compatibility ids
 * ({@code bruisewood_plank_wall}, {@code consumed_plank_wall}, {@code goth_plank_wall},
 * {@code parasitecanister_bag_wall}, {@code parasiteplank_deadhead_wall},
 * {@code parasitestain_flesh_wall}).
 *
 * <p>{@code BlockWallBase extends BlockWall} and only added one behaviour on top: the block is
 * {@code tickRandom = true} and, while any of its six neighbours is an SRP infestation block, it
 * schedules a 10-tick re-check and runs {@code BeckonBlockInfestation.beckonInfestation(world, pos,
 * rand, 1, false)} with a 20-tick follow-up ({@code BlockWallBase.java:23-51}).  That maps onto
 * {@link BlockInfestation#infestAround} with stage 1, the same call the ported
 * {@link InfestedSlabBlock} uses.</p>
 *
 * <p>The {@code tickRandom}/{@code dropsItems} constructor flags of the original are not needed on
 * 26.3: every wall id ships a vanilla-shaped {@code multipart} blockstate (up / north / east / south
 * / west), which {@link WallBlock} already provides.</p>
 */
public class LegacyWallBlock extends WallBlock {
    public LegacyWallBlock(Properties properties) {
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
