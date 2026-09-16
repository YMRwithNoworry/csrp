package alku.csrp.block;

import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Shared active stage state used by SRP biome and colony cores. */
public abstract class SrpCoreBlock extends Block {
    public static final IntegerProperty ACTIVE = IntegerProperty.create("active", 0, 3);

    protected SrpCoreBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        if (state.getValue(ACTIVE) > 0) {
            removeRecord(level, pos);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    protected abstract void removeRecord(Level level, BlockPos pos);
}
