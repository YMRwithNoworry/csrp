package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The thin parasite wood block from SRP 1.10.8.
 *
 * <p>It behaves like a six-way connected web: a narrow central pillar is
 * always rendered and horizontal arms are added for every adjacent solid
 * block.  The old implementation used six boolean block-state properties;
 * keeping those properties here makes the original blockstate/model assets
 * usable on modern versions.</p>
 */
public class ParasiteThinBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final VoxelShape CENTER = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    private static final VoxelShape NORTH_ARM = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 10.0D);
    private static final VoxelShape EAST_ARM = Block.box(6.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    private static final VoxelShape SOUTH_ARM = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_ARM = Block.box(0.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);

    public ParasiteThinBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(defaultBlockState()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return stateForConnections(defaultBlockState(), level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return switch (direction) {
            case NORTH -> state.setValue(NORTH, canConnect(level, neighborPos, direction));
            case EAST -> state.setValue(EAST, canConnect(level, neighborPos, direction));
            case SOUTH -> state.setValue(SOUTH, canConnect(level, neighborPos, direction));
            case WEST -> state.setValue(WEST, canConnect(level, neighborPos, direction));
            case UP -> state.setValue(UP, canConnect(level, neighborPos, direction));
            case DOWN -> state.setValue(DOWN, canConnect(level, neighborPos, direction));
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_ARM);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_ARM);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_ARM);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_ARM);
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static BlockState stateForConnections(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(NORTH, canConnect(level, pos.north(), Direction.NORTH))
                .setValue(EAST, canConnect(level, pos.east(), Direction.EAST))
                .setValue(SOUTH, canConnect(level, pos.south(), Direction.SOUTH))
                .setValue(WEST, canConnect(level, pos.west(), Direction.WEST))
                .setValue(UP, canConnect(level, pos.above(), Direction.UP))
                .setValue(DOWN, canConnect(level, pos.below(), Direction.DOWN));
    }

    private static boolean canConnect(BlockGetter level, BlockPos neighborPos, Direction direction) {
        BlockState neighbor = level.getBlockState(neighborPos);
        return neighbor.getBlock() instanceof ParasiteThinBlock
                || neighbor.isFaceSturdy(level, neighborPos, direction.getOpposite(), SupportType.FULL);
    }
}
