package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasiteTrunk}
 * (out109 {@code block/BlockParasiteTrunk.java:32,123}) — registered as
 * {@code srparasites:parasitetrunk}.
 *
 * <p>The original is a {@code BlockRotatedPillar} whose only special behaviour is
 * {@code canSustainLeaves(IBlockState, IBlockAccess, BlockPos)} returning {@code true}
 * ({@code BlockParasiteTrunk.java:123-125}), which is what keeps the deadhead leaves attached: the
 * deadhead tree generator writes {@code parasitestain} / {@code parasiterubble} soil and then a trunk
 * column of {@code parasitetrunk}, and the leaves only survive while such a trunk is within the
 * seven-block decay radius.</p>
 *
 * <p>26.3 removed {@code Block#canSustainLeaves}; leaf decay is driven by
 * {@link net.minecraft.world.level.block.LeavesBlock} counting the distance to a block in the
 * {@code minecraft:logs} tag.  Because adding the id to that tag needs a data-pack entry outside this
 * task's write scope, the SRP trunk is instead recognised explicitly by
 * {@link DeadheadLeavesBlock}, which reproduces the original's seven-block BFS.
 * {@link #sustainsDeadheadLeaves(BlockState)} is the modern replacement for
 * {@code canSustainLeaves}.</p>
 */
public class ParasiteTrunkBlock extends RotatedPillarBlock {
    public ParasiteTrunkBlock(Properties properties) {
        super(properties);
    }

    /** 26.3 stand-in for the original's {@code canSustainLeaves} (BlockParasiteTrunk.java:123). */
    public static boolean sustainsDeadheadLeaves(BlockState state) {
        return state.getBlock() instanceof ParasiteTrunkBlock
                || state.is(net.minecraft.tags.BlockTags.LOGS);
    }

    /** Mirrors the original's wood check used by {@code spreadBiomeBlockTrunk}. */
    public static boolean isSrpWood(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof ParasiteTrunkBlock
                || (state.is(net.minecraft.tags.BlockTags.LOGS)
                        && state.isFaceSturdy(level, pos, Direction.UP));
    }

    /** Helper kept so the registry can reuse the same properties as the other SRP wood blocks. */
    static Block unusedAnchor() {
        return null;
    }
}
