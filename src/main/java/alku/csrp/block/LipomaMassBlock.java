package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockLipomaMass}
 * (out109 {@code block/BlockLipomaMass.java}) — registered as {@code srparasites:lipoma_mass}
 * ({@code init/SRPBlocks.java:634}).
 *
 * <p>Unlike every other SRP plant this one hangs: {@code BlockLipomaMass.func_176196_c} /
 * {@code func_180671_f} ({@code BlockLipomaMass.java:34-50}) required the block <em>above</em> to be
 * an SRP block that is solid on its lower face, and a neighbour update removes the mass once that
 * stops being true.</p>
 */
public final class LipomaMassBlock extends BushBlock {
    public LipomaMassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState support = level.getBlockState(above);
        return HirsuteHairBlock.isSrpBlock(support)
                && support.isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.above()), level, pos.above());
    }

    /** {@code BlockLipomaMass.func_189540_a} — {@code world.destroyBlock(pos, true)}. */
    @Override
    protected void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.level.block.Block neighbourBlock,
            net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }
}
