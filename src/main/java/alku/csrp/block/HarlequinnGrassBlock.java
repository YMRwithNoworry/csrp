package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasiteSpreading} entry
 * registered as {@code srparasites:harlequinn_grass}
 * ({@code init/SRPBlocks.java:624}, hardness 0.6F, infested = {@code false}).
 *
 * <p>The original's {@code updateTick} ({@code BlockParasiteSpreading.java:31-40}) had a special case
 * ahead of the generic spreading logic:</p>
 * <pre>
 * if (this == SRPBlocks.HarlequinnGrass) {
 *     if (hasSnowAbove) { setBlock(pos, SRPBlocks.LocsBlock); return; }
 * }
 * </pre>
 * <p>Snow covered harlequinn grass therefore turns into the {@code locs_block} that the port already
 * registers as {@link ModBlocks#LOCS_BLOCK}.  Everything else is the parent
 * {@link ParasiteSpreadingBlock} behaviour with {@code infested = false}.</p>
 */
public final class HarlequinnGrassBlock extends ParasiteSpreadingBlock {
    /** {@code world.isAreaLoaded(pos, 3)} guard of the original tick. */
    private static final int AREA_LOAD_RADIUS = 3;

    public HarlequinnGrassBlock(Properties properties) {
        super(properties, false);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isAreaLoaded(pos, AREA_LOAD_RADIUS) && hasSnowAbove(level, pos)) {
            level.setBlock(pos, ModBlocks.LOCS_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        super.randomTick(state, level, pos, random);
    }

    /** {@code Blocks.SNOW_LAYER || Blocks.SNOW} — {@code BlockParasiteSpreading.java:35}. */
    static boolean hasSnowAbove(ServerLevel level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(Blocks.SNOW) || above.is(Blocks.SNOW_BLOCK);
    }
}
