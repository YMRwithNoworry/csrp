package alku.csrp.block;

import alku.csrp.block.entity.InfuserFurnaceBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

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
 * {@code ContainerFurnace} GUI and sided inventory) is wired to the ported
 * {@link InfuserFurnaceBlockEntity}, which is the closest existing implementation: the legacy ids
 * {@code csrp:infested_furnace} / {@code csrp:infested_furnace_lit} are registered against the
 * dedicated {@code csrp:legacy_infested_furnace} block-entity type in
 * {@code registry/ModBlockEntities.java}, so a placed legacy furnace is a working container.</p>
 */
public class InfestedFurnaceBlock extends HorizontalDirectionalBlock implements EntityBlock {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfuserFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : (level1, pos, state1, blockEntity) ->
                        InfuserFurnaceBlockEntity.serverTick(level1, pos, state1, blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof InfuserFurnaceBlockEntity furnace)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.openMenu(furnace);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    /** {@code BlockInfestedFurnace.setLitState} — swap the {@code lit} flag without losing the entity. */
    public static void setLitState(boolean active, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof InfestedFurnaceBlock && state.getValue(LIT) != active) {
            // UPDATE_ALL would recreate the block entity; the original kept its inventory on relight.
            level.setBlock(pos, state.setValue(LIT, active), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
        }
    }

    /** {@code infested_furnace_lit} is the same block registered under its lit id. */
    public static boolean isLitVariant(BlockState state) {
        return state.getBlock() instanceof InfestedFurnaceBlock && state.getValue(LIT);
    }
}
