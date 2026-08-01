package alku.csrp.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Grid-aligned colony structure foundation placed by workers. */
public final class ColonyStructureBlock extends Block {
    public ColonyStructureBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SrpCoreBlock.ACTIVE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SrpCoreBlock.ACTIVE);
    }
}
