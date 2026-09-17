package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of SRParasites 1.10.9 {@code BlockSnowGrass}.  The original registered one class twice
 * ({@code snow_short_grass} with {@code tallGrass = false}, {@code snow_tall_grass} with
 * {@code true}); here the shared behaviour lives in this base class and the two registered shapes
 * are {@link SnowShortGrassBlock} / {@link SnowTallGrassBlock}.
 *
 * <p>Behaviour carried over from 1.12.2: placing snow grass converts the vanilla grass block
 * underneath into snow-covered grass, breaking it converts that block back, and the block rolls a
 * grass seed one time in eight.</p>
 */
public abstract class SnowGrassBlock extends BushBlock {
    protected SnowGrassBlock(Properties properties) {
        super(properties);
    }

    protected abstract VoxelShape shape();

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos.below(), ModBlocks.SNOW_COVERED_GRASS.get().defaultBlockState(), 3);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        level.playSound(null, pos, SoundType.SNOW.getBreakSound(), SoundSource.BLOCKS, 0.65F, 1.0F);
        BlockPos below = pos.below();
        if (level.getBlockState(below).is(ModBlocks.SNOW_COVERED_GRASS.get())) {
            level.setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }
    }

    /** Original {@code getDrops}: one in eight snow grass blocks yields a grass seed. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            popResource(level, pos, new ItemStack(Items.WHEAT_SEEDS));
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.AIR && !hasCollision;
    }
}
