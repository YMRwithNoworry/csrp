package alku.csrp.block;

import alku.csrp.block.DeadheadGrassShortBlock.DeadheadVineSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of SRParasites 1.10.9 {@code BlockDeadheadGrassTall}: the long deadhead vine.  The original
 * used a {@code TOP/BOTTOM} metadata pair that behaved like a double plant (neighbour-chained, two
 * blocks tall, the lower half filled in by {@code onBlockPlacedBy}).  Here that is expressed with an
 * explicit {@code part} property so the 1.10.9 blockstate keys survive.
 */
public class DeadheadGrassTallBlock extends BushBlock {
    public static final EnumProperty<TallPart> PART = EnumProperty.create("part", TallPart.class);

    private static final VoxelShape SHAPE = Block.column(14.0D, 0.0D, 16.0D);

    public DeadheadGrassTallBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, TallPart.UPPER));
    }

    public enum TallPart implements StringRepresentable {
        LOWER("lower"),
        UPPER("upper");

        private final String name;

        TallPart(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.getY() >= level.getMaxY()) {
            return null;
        }
        return defaultBlockState().setValue(PART, TallPart.UPPER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && state.getValue(PART) == TallPart.UPPER) {
            BlockPos below = pos.below();
            if (level.isEmptyBlock(below)) {
                level.setBlock(below, defaultBlockState().setValue(PART, TallPart.LOWER), 3);
            }
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(PART) == TallPart.UPPER) {
            return DeadheadVineSupport.isDeadheadSupport(level, pos.above());
        }
        BlockState above = level.getBlockState(pos.above());
        return above.is(this) && above.getValue(PART) == TallPart.UPPER;
    }

    /**
     * The original re-checked the pairing in {@code canBlockStay}; 26.3 splits that into
     * {@code updateShape} (shape changes) and {@code affectNeighborsAfterRemoval} (block removed),
     * which is what the two overrides below implement.
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
            RandomSource random) {
        if (state.getValue(PART) == TallPart.LOWER && directionToNeighbour == Direction.UP
                && (!neighbourState.is(this) || neighbourState.getValue(PART) != TallPart.UPPER)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(PART) == TallPart.UPPER && directionToNeighbour == Direction.DOWN
                && (!neighbourState.is(this) || neighbourState.getValue(PART) != TallPart.LOWER)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState,
                random);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        BlockPos other = state.getValue(PART) == TallPart.UPPER ? pos.below() : pos.above();
        BlockState otherState = level.getBlockState(other);
        if (otherState.is(this)) {
            level.levelEvent(2001, other, Block.getId(otherState));
            level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
            level.playSound(null, other, SoundType.GRASS.getBreakSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.AIR && !hasCollision;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }
}
