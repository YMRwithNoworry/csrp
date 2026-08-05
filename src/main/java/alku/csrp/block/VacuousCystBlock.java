package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Empty parasite cyst. Serves no purpose and randomly disappears over time.
 */
public final class VacuousCystBlock extends Block {
    public VacuousCystBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.3F) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
