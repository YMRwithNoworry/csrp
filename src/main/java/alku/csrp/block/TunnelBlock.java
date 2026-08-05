package alku.csrp.block;

import alku.csrp.entity.BuglinEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A Rupter-made burrow that periodically releases Buglins. */
public final class TunnelBlock extends Block {
    public static final MapCodec<TunnelBlock> CODEC = simpleCodec(TunnelBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

    public TunnelBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected MapCodec<? extends TunnelBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL
                || !level.hasChunksAt(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
            return;
        }

        AABB tunnelArea = new AABB(pos).inflate(1.0D);
        if (!level.getEntitiesOfClass(BuglinEntity.class, tunnelArea).isEmpty()) {
            return;
        }

        AABB populationArea = new AABB(pos).inflate(16.0D);
        int parasiteCount = level.getEntitiesOfClass(Mob.class, populationArea,
                mob -> mob instanceof Parasite).size();
        if (parasiteCount <= 10) {
            spawnBuglin(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getDifficulty() != Difficulty.PEACEFUL
                && level.hasChunksAt(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            spawnBuglin(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    private static void spawnBuglin(ServerLevel level, BlockPos pos) {
        BuglinEntity buglin = ModEntities.BUGLIN.get().create(level);
        if (buglin == null) {
            return;
        }
        buglin.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(buglin);
    }
}
