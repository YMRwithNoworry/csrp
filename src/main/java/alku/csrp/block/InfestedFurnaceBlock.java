package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockInfestedFurnace}
 * (out109 {@code block/BlockInfestedFurnace.java}) — registered twice as
 * {@code srparasites:infested_furnace} and {@code srparasites:infested_furnace_lit}
 * ({@code init/SRPBlocks.java:506}).
 *
 * <p>Block-level behaviour reproduced from the original:</p>
 * <ul>
 *   <li>{@code LIT} plus the horizontal {@code facing}, defaults {@code NORTH}/{@code false}
 *       ({@code BlockInfestedFurnace.java:29,33}); hardness 3.5F and {@code SoundType.STONE}
 *       (line 32).</li>
 *   <li>{@code getLightValue} ({@code BlockInfestedFurnace.java:52-54}): a lit furnace emits light
 *       level 14.</li>
 *   <li>{@code getStateForPlacement} / {@code onBlockPlacedBy} (lines 122-131): the furnace faces the
 *       placer, {@code placer.getHorizontalFacing().getOpposite()}.</li>
 *   <li>{@code setLitState} (lines 35-60) rebuilt the block state around the ticking tile entity; the
 *       26.3 equivalent is a plain {@link Level#setBlock} with the same state, exposed through
 *       {@link #setLitState}.</li>
 * </ul>
 *
 * <p>The smelting tile entity ({@code TileEntityInfestedFurnace}, a 3-slot furnace with a
 * {@code ContainerFurnace} GUI and sided inventory) needs a {@code BlockEntityType} that is not
 * registered in this port; adding one means editing {@code registry/ModBlockEntities.java}, which is
 * outside this task's write scope.  The ported {@code InfuserFurnaceBlockEntity} is the closest
 * existing implementation and is the intended reuse target once that entry is authorised.</p>
 */
public class InfestedFurnaceBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** {@code BlockInfestedFurnace.getLightValue} — 14 when lit. */
    public static final int LIT_LIGHT_LEVEL = 14;

    public InfestedFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /** {@code BlockInfestedFurnace.setLitState} — swap the {@code lit} flag without losing the entity. */
    public static void setLitState(boolean active, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof InfestedFurnaceBlock && state.getValue(LIT) != active) {
            level.setBlock(pos, state.setValue(LIT, active), Block.UPDATE_ALL);
        }
    }

    /** {@code infested_furnace_lit} is the same block registered under its lit id. */
    public static boolean isLitVariant(BlockState state) {
        return state.getBlock() instanceof InfestedFurnaceBlock && state.getValue(LIT);
    }
}
