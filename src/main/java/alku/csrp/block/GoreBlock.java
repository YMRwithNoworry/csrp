package alku.csrp.block;

import alku.csrp.world.RemainsMigration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Legacy block-id compatibility only; placement immediately becomes a non-colliding remains entity. */
public class GoreBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1.6D, 0.0D, 1.6D, 14.4D, 12.8D, 14.4D);

    public GoreBlock(BlockBehaviour.Properties properties) {
        super(properties.noCollission().noOcclusion());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (level instanceof ServerLevel serverLevel) {
            RemainsMigration.convert(serverLevel, pos, state);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }
}
