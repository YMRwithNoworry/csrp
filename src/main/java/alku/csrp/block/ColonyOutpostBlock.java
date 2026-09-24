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
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockColonyCore} as registered
 * for {@code srparasites:colonyoutpost} ({@code init/SRPBlocks.java:486}:
 * {@code new BlockColonyCore(Material.ROCK, "colonyoutpost", 30.0F, true, true, 1200.0F)}).
 *
 * <p>The 26.3 port already carries the equivalent behaviour in
 * {@link ColonyStructureBlock} — the grid-aligned construction marker that workers place and that
 * expands into a colony building over the {@code active} 0..3 stages.  That class is {@code final},
 * so this dedicated subclass mirrors its tick contract for the legacy id (stage 0..3, colony range
 * validation, {@link ColonyStructureGenerator#generateBuilding} on the scheduled tick) while keeping
 * the hardening values of the original: hardness 30.0F, resistance 1200.0F.</p>
 */
public final class ColonyOutpostBlock extends Block {
    /** Shared with {@link SrpCoreBlock} / {@link ColonyStructureBlock}. */
    public static final IntegerProperty ACTIVE = SrpCoreBlock.ACTIVE;

    public ColonyOutpostBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this) && state.getValue(ACTIVE) > 0
                && state.getValue(ACTIVE) < 3) {
            level.scheduleTick(pos, this, 20 + level.getRandom().nextInt(81));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(ACTIVE);
        if (stage <= 0 || stage >= 3) {
            return;
        }
        if (SrpWorldData.get(level).nearestColonyInConstructionRange(pos) == null) {
            level.removeBlock(pos, false);
            return;
        }
        if (ColonyStructureGenerator.generateBuilding(level, pos, stage, random)) {
            level.setBlock(pos, defaultBlockState().setValue(ACTIVE, 3), Block.UPDATE_ALL);
        } else {
            level.scheduleTick(pos, this, 100);
        }
    }
}
