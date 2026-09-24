package alku.csrp.block;

import alku.csrp.Csrp;
import alku.csrp.infection.BlockInfestation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

/**
 * Port of the 1.12.2 slab family — {@code BlockSlabBase} / {@code BlockSlabRubble} /
 * {@code BlockSlabStain} (out109 {@code block/slabs/BlockSlabBase.java},
 * {@code block/slabs/BlockSlabRubble.java}, {@code block/slabs/BlockSlabStain.java}) and
 * {@code BlockHarleskinnSlab} (out109 {@code block/BlockHarleskinnSlab.java}).
 *
 * <p>All of them are {@code BlockSlab}s: the "half" ids carry the vanilla {@code HALF}
 * ({@code type}) metadata and the "*_slab_double" / "*slabdouble" ids are the same shape with
 * {@code isDouble == true} ({@code BlockSlabRubble.BlockSlabRubbleDouble.func_176552_j()} returns
 * true, {@code BlockHarleskinnSlab.java:17-19}).  26.3 folds both into one {@link SlabBlock} whose
 * {@code type} property already spans {@code bottom}/{@code top}/{@code double}, which is exactly
 * what every {@code *_slab*.json} blockstate in this repository expects.</p>
 *
 * <p>All four 1.12.2 constructors passed {@code tickRandom = true} and several of them inherited the
 * {@code BlockWallBase}-style neighbour check, so the block picks up
 * {@link InfestedSlabBlock}'s "convert while touching infestation" tick — the ported equivalent of
 * {@code BeckonBlockInfestation.beckonInfestation(world, pos, rand, 1, false)}
 * ({@code BlockWallBase.java:44-51}).</p>
 */
public class LegacySlabBlock extends SlabBlock {
    public LegacySlabBlock(Properties properties) {
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
        if (touchingAnyInfestation(level, pos)) {
            BlockInfestation.infestAround(level, pos, 1);
            level.scheduleTick(pos, this, 20);
        }
    }

    private void scheduleCheck(Level level, BlockPos pos, int delay) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, delay);
        }
    }

    /** {@code BlockWallBase.touchingAnyInfestation} — any neighbour in the SRP infestation family. */
    static boolean touchingAnyInfestation(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            Block block = level.getBlockState(pos.relative(direction)).getBlock();
            if (block instanceof InfestedBlock || block instanceof ParasiteSpreadingBlock) {
                return true;
            }
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null && Csrp.MODID.equals(id.getNamespace()) && id.getPath().contains("infest")) {
                return true;
            }
        }
        return false;
    }
}
