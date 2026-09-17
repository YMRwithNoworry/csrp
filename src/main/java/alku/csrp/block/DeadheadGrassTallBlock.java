package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Skeleton for SRParasites 1.10.9 {@code BlockDeadheadGrassTall} (long deadhead vines, two blocks tall).
 *
 * <p>Reference: {@code _srp-orig/decomp-1.10.9/.../block/BlockDeadheadGrassTall.java} —
 * hardness {@code 0.0F} (1.20.1 {@code instabreak()}), {@code SoundType.GRASS}, cutout render layer,
 * shearable, no drops, default part = TOP, placing the top half also places the bottom half.
 *
 * <p>1.12.2 used a custom {@code PropertyEnum<EnumPart>} named {@code part} with values
 * {@code TOP}/{@code BOTTOM}. 1.20.1 reuses {@link BlockStateProperties#DOUBLE_BLOCK_HALF}, whose
 * property name is {@code half} and whose serialized values are {@code upper}/{@code lower}
 * (verified: {@code DoubleBlockHalf.getSerializedName()}), <b>not</b> {@code part=top|bottom}.
 * The blockstate json variant keys written in slice 2 must therefore be {@code half=upper} /
 * {@code half=lower}.
 *
 * <p>Deliberately {@code extends BushBlock} and <b>not</b> {@code DoublePlantBlock}: the latter would
 * bring in a {@code WATERLOGGED} property that the original block does not have.
 */
public class DeadheadGrassTallBlock extends BushBlock {
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

    // TODO(C): override canSurvive / mayPlaceOn for the DEADHEAD-support rule, and mirror 1.10.9's
    // "UPPER requires the block below to be this block with part=lower" / "LOWER requires the block
    // above to be this block with part=upper" logic via updateShape.
    // TODO(C): override getStateForPlacement (1.10.9 always returns TOP/UPPER) and setPlacedBy
    // (Level, BlockPos, BlockState, LivingEntity, ItemStack) so that a server-side placement also
    // places the LOWER half below when that position is air.
    // TODO(C): mirror DoublePlantBlock's break linkage: playerWillDestroy(Level, BlockPos, BlockState,
    // Player) plus a locally re-implemented equivalent of
    // DoublePlantBlock.preventCreativeDropFromBottomPart (that helper is protected static in
    // DoublePlantBlock and is NOT reachable from a plain BushBlock subclass).
    // TODO(C): implement IForgeShearable (signatures verified on TallGrassBlock) and suppress drops.
}
