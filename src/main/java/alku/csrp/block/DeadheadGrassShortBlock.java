package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of SRParasites 1.10.9 {@code BlockDeadheadGrassShort}: a short deadhead vine that grows
 * only against deadhead wood (trunks / leaves).  The original drove its five textures from block
 * metadata computed from the position hash; on 26.3 the texture is frozen into the block state at
 * placement time, which keeps old NBT that stores {@code texture=0..4} loadable.
 */
public class DeadheadGrassShortBlock extends BushBlock {
    public static final IntegerProperty TEXTURE = IntegerProperty.create("texture", 0, 4);
    private static final VoxelShape SHAPE = Block.column(12.0D, 0.0D, 13.0D);

    public DeadheadGrassShortBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TEXTURE, 0));
    }

    /** Original {@code getActualState} hash, applied once at placement instead of every render. */
    public static int textureAt(BlockPos pos) {
        long hash = pos.getX() * 3129871L ^ pos.getZ() * 116129781L ^ pos.getY();
        hash = hash * hash * 42317861L + hash * 11L;
        return (int) ((hash >>> 16) % 5L);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            state = defaultBlockState();
        }
        return state.setValue(TEXTURE, textureAt(context.getClickedPos()));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return DeadheadVineSupport.isDeadheadSupport(level, pos.above());
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
        builder.add(TEXTURE);
    }

    /** Shared deadhead support test used by both deadhead grass shapes. */
    static final class DeadheadVineSupport {
        private DeadheadVineSupport() {
        }

        static boolean isDeadheadSupport(BlockGetter level, BlockPos supportPos) {
            BlockState supportState = level.getBlockState(supportPos);
            if (supportState.is(ModBlocks.PARASITETRUNK.get())) {
                // 1.12.2 keyed deadhead wood off the block metadata variant; the port collapsed the
                // trunk into a single id, so any parasite trunk supports deadhead vines.
                return true;
            }
            if (supportState.is(ModBlocks.DEADHEAD_LEAVES.get())) {
                return true;
            }
            Identifier id = BuiltInRegistries.BLOCK.getKey(supportState.getBlock());
            if (id == null) {
                return false;
            }
            String path = id.getPath().toLowerCase(Locale.ROOT);
            return path.contains("deadhead") || path.contains("dead_head");
        }
    }
}
