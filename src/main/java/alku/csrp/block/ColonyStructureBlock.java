package alku.csrp.block;

import alku.csrp.world.ColonyStructureGenerator;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Grid-aligned construction marker placed by workers and expanded into a colony building. */
public final class ColonyStructureBlock extends Block {
    public ColonyStructureBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SrpCoreBlock.ACTIVE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SrpCoreBlock.ACTIVE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this) && state.getValue(SrpCoreBlock.ACTIVE) > 0
                && state.getValue(SrpCoreBlock.ACTIVE) < 3) {
            level.scheduleTick(pos, this, 20 + level.getRandom().nextInt(81));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(SrpCoreBlock.ACTIVE);
        if (stage <= 0 || stage >= 3) {
            return;
        }
        if (SrpWorldData.get(level).nearestColonyInConstructionRange(pos) == null) {
            level.removeBlock(pos, false);
            return;
        }
        if (ColonyStructureGenerator.generateBuilding(level, pos, stage, random)) {
            level.setBlock(pos, defaultBlockState().setValue(SrpCoreBlock.ACTIVE, 3), Block.UPDATE_ALL);
        } else {
            level.scheduleTick(pos, this, 100);
        }
    }
}
