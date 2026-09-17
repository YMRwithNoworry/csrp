package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Skeleton for the {@code snow_tall_grass} half of SRParasites 1.10.9 {@code BlockSnowGrass}
 * ({@code tallGrass = true}).
 *
 * <p>1.10.9 {@code TALL_GRASS_AABB = new AxisAlignedBB(0.1, 0.0, 0.1, 0.9, 2.0, 0.9)} — note that
 * 1.12.2 used this as a <b>single-block</b> block whose box merely reached two blocks up
 * ({@code isPassable} = true, {@code noCollission()}); the 1.20.1 port keeps that original geometry.
 * See {@link SnowGrassBlock} for the properties and the full TODO(C) list.
 */
public class SnowTallGrassBlock extends SnowGrassBlock {
    /** 1.10.9 tall-grass box: 0.1/0.0/0.1 → 0.9/2.0/0.9. */
    private static final VoxelShape SHAPE = Block.box(0.1D, 0.0D, 0.1D, 0.9D, 2.0D, 0.9D);

    public SnowTallGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
