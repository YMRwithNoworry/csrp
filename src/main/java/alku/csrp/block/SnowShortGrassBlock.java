package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Skeleton for the {@code snow_short_grass} half of SRParasites 1.10.9 {@code BlockSnowGrass}
 * ({@code tallGrass = false}).
 *
 * <p>1.10.9 {@code SHORT_GRASS_AABB = new AxisAlignedBB(0.1, 0.0, 0.1, 0.9, 1.0, 0.9)}.
 * See {@link SnowGrassBlock} for the properties and the full TODO(C) list.
 */
public class SnowShortGrassBlock extends SnowGrassBlock {
    /** 1.10.9 short-grass box: 0.1/0.0/0.1 → 0.9/1.0/0.9. */
    private static final VoxelShape SHAPE = Block.box(0.1D, 0.0D, 0.1D, 0.9D, 1.0D, 0.9D);

    public SnowShortGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
