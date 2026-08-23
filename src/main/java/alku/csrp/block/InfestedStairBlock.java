package alku.csrp.block;

import alku.csrp.infection.BlockInfestation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Infested stairs; SRP 1.10.8 explicitly disables spread from stair variants. */
public final class InfestedStairBlock extends StairBlock {

    private final BlockState baseState;

    public InfestedStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
        this.baseState = baseState;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Legacy stair variants always spread as stage zero.
        BlockInfestation.spread(level, pos, 0, random);
    }

}
