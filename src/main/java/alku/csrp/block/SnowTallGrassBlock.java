package alku.csrp.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The {@code snow_tall_grass} half of SRParasites 1.10.9 {@code BlockSnowGrass}
 * ({@code tallGrass = true}).  The original was a single block with a two-block-tall bounding box
 * ({@code (0.1, 0.0, 0.1) - (0.9, 2.0, 0.9)}); 26.3 has no single-block two-high plant, so the
 * model carries the upper half and the shape stays the upper half of that box.
 */
public class SnowTallGrassBlock extends SnowGrassBlock {
    private static final VoxelShape SHAPE = Block.column(12.0D, 0.0D, 16.0D);

    public SnowTallGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape shape() {
        return SHAPE;
    }
}
