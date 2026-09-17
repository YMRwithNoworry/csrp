package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import alku.csrp.world.ColdStarTreeHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IForgeShearable;

/**
 * SRParasites 1.10.9 {@code BlockDeadheadGrassTall} (long deadhead vines, two blocks tall).
 *
 * <p>Reference: {@code _srp-orig/decomp-1.10.9/.../block/BlockDeadheadGrassTall.java} —
 * hardness {@code 0.0F} (1.20.1 {@code instabreak()}), {@code SoundType.GRASS}, cutout render layer,
 * shearable, no drops, default part = TOP, placing the top half also places the bottom half.
 *
 * <p>1.12.2 used a custom {@code PropertyEnum<EnumPart>} named {@code part} with values
 * {@code TOP}/{@code BOTTOM}. 1.20.1 reuses {@link BlockStateProperties#DOUBLE_BLOCK_HALF}, whose
 * property name is {@code half} and whose serialized values are {@code upper}/{@code lower}
 * (verified: {@code DoubleBlockHalf.getSerializedName()}), <b>not</b> {@code part=top|bottom}.
 * The blockstate json variant keys must therefore be {@code half=upper} / {@code half=lower}.
 *
 * <p>Deliberately {@code extends BushBlock} and <b>not</b> {@code DoublePlantBlock}: the latter would
 * bring in a {@code WATERLOGGED} property that the original block does not have. The break linkage of
 * {@code DoublePlantBlock} is re-implemented locally because
 * {@code DoublePlantBlock.preventCreativeDropFromBottomPart} is {@code protected static} and thus
 * unreachable from a plain {@code BushBlock} subclass (PLAN.md T27).
 */
public class DeadheadGrassTallBlock extends BushBlock implements IForgeShearable {
    public static final EnumProperty<DoubleBlockHalf> PART = BlockStateProperties.DOUBLE_BLOCK_HALF;

    /**
     * 1.10.9 {@code TALL_TOP_AABB = new AxisAlignedBB(0.0, -1.0, 0.0, 1.0, 1.0, 1.0)} — this is the
     * shape returned for the <b>BOTTOM</b> state (the block the two-tall hitbox is anchored on).
     */
    private static final VoxelShape SHAPE_LOWER = Block.box(0.0D, -1.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    /**
     * 1.10.9 {@code TALL_BOTTOM_AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 2.0, 1.0)} — this is the
     * shape returned for the <b>TOP</b> state (measured in the top block's own coordinate space).
     */
    private static final VoxelShape SHAPE_UPPER = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 2.0D, 1.0D);

    public DeadheadGrassTallBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, DoubleBlockHalf.UPPER));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == DoubleBlockHalf.UPPER ? SHAPE_UPPER : SHAPE_LOWER;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    /** 1.10.9 {@code getStateForPlacement} always yields TOP (UPPER). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PART, DoubleBlockHalf.UPPER);
    }

    /** 1.10.9 {@code onBlockPlacedBy}: fill in the bottom half when it is empty. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && level.isEmptyBlock(pos.below())) {
            level.setBlock(pos.below(), defaultBlockState().setValue(PART, DoubleBlockHalf.LOWER), 3);
        }
    }

    /**
     * 1.10.9 {@code canPlaceBlockAt}/{@code canBlockStay}:
     * <ul>
     *   <li>UPPER needs a valid deadhead support above plus this same block with {@code half=lower}
     *       directly below;</li>
     *   <li>LOWER needs this same block with {@code half=upper} directly above.</li>
     * </ul>
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(PART) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return isValidDeadheadSupport(level.getBlockState(pos.above()))
                    && below.getBlock() == this
                    && below.getValue(PART) == DoubleBlockHalf.LOWER;
        }
        BlockState above = level.getBlockState(pos.above());
        return above.getBlock() == this && above.getValue(PART) == DoubleBlockHalf.UPPER;
    }

    /** The default {@code BushBlock.mayPlaceOn} floor rule does not apply to this hanging plant. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    /** Break linkage: removing one half removes the other. */
    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction,
            BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(PART);
        boolean verticalCheck = direction.getAxis() == net.minecraft.core.Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER ? direction == net.minecraft.core.Direction.UP
                        : direction == net.minecraft.core.Direction.DOWN);
        if (verticalCheck) {
            return neighborState.getBlock() == this && neighborState.getValue(PART) != half
                    ? state : Blocks.AIR.defaultBlockState();
        }
        if (half == DoubleBlockHalf.UPPER && direction == net.minecraft.core.Direction.UP
                && !isValidDeadheadSupport(neighborState)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /** 1.10.9 {@code isValidDeadheadSupport}, shared verbatim with the short variant. */
    private static boolean isValidDeadheadSupport(BlockState supportState) {
        Block supportBlock = supportState.getBlock();
        if (ColdStarTreeHandler.isDeadheadTrunk(supportState)) {
            return true;
        }
        if (ColdStarTreeHandler.isDeadheadLeaves(supportState)) {
            return true;
        }
        return ColdStarTreeHandler.hasDeadheadId(supportBlock) && supportState.isSolid();
    }

    /**
     * Mirrors {@code DoublePlantBlock.playerWillDestroy}: destroying one half destroys the other, and
     * a creative-mode player who breaks the bottom half must not duplicate the item (the local
     * re-implementation of {@code DoublePlantBlock.preventCreativeDropFromBottomPart}, which is
     * {@code protected static} upstream — PLAN.md T27).
     */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(PART);
        BlockPos otherPos = half == DoubleBlockHalf.UPPER ? pos.below() : pos.above();
        BlockState otherState = level.getBlockState(otherPos);
        if (otherState.getBlock() == this && otherState.getValue(PART) != half) {
            if (!level.isClientSide && player.isCreative()) {
                level.destroyBlock(otherPos, false, player);
            } else {
                level.destroyBlock(otherPos, true, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    /** 1.10.9 {@code isShearable} — the vine is always shearable. */
    @Override
    public boolean isShearable(ItemStack item, Level level, BlockPos pos) {
        return true;
    }

    /** 1.10.9 {@code onSheared}: yields the block itself as an item. */
    @Override
    public List<ItemStack> onSheared(Player player, ItemStack item, Level level, BlockPos pos, int fortune) {
        return List.of(new ItemStack(ModBlocks.DEADHEAD_GRASS_TALL.get()));
    }

    /** 1.10.9 {@code getItemDropped} returned {@code null} / {@code quantityDropped} = 0: no drops. */
    @Override
    public List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return List.of();
    }

    /** 1.10.9 {@code getPickBlock} → the block's own item. */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.DEADHEAD_GRASS_TALL.get());
    }
}
