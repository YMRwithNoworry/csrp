package alku.csrp.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockDermoidCyst}
 * (out109 {@code block/BlockDermoidCyst.java}) — registered as {@code srparasites:dermoid_cyst}
 * ({@code init/SRPBlocks.java}, hardness 2.5F, {@code tickRandom = true}, flesh sound).
 *
 * <p>Reproduced behaviour: the horizontal {@code facing} metadata with {@code NORTH} as the default
 * ({@code BlockDermoidCyst.java:5,10}), the rotation/mirror transforms
 * ({@code BlockDermoidCyst.java:53-59}) and placement facing away from the placer
 * ({@code BlockDermoidCyst.java:22-24}).</p>
 *
 * <p>The original also opened a {@code TileEntityDermoidCyst} inventory
 * ({@code BlockDermoidCyst.java:34-42}) and played {@code FLESH_GROWL} on removal (line 44).  The
 * container tile entity has no {@code BlockEntityType} in this port yet — registering one requires a
 * {@code registry/ModBlockEntities.java} entry that is outside this task's write scope — so the
 * inventory half is a documented follow-up; the block-state contract and the placement/rotation
 * behaviour are complete.</p>
 */
public final class DermoidCystBlock extends HorizontalDirectionalBlock {
    public DermoidCystBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** {@code BlockDermoidCyst.getStateForPlacement} — {@code placer.getHorizontalFacing()}. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Unused placeholder kept so the shape constant of the original stays documented. */
    static VoxelShape fullCubeShape() {
        return Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    }
}
