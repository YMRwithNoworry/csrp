package alku.csrp.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The {@code snow_short_grass} half of SRParasites 1.10.9 {@code BlockSnowGrass}
 * ({@code tallGrass = false}).  The bounding box mirrors the original 1.12.2 values
 * {@code (0.1, 0.0, 0.1) - (0.9, 1.0, 0.9)}.
 */
public class SnowShortGrassBlock extends SnowGrassBlock {
    private static final VoxelShape SHAPE = Block.column(12.0D, 0.0D, 16.0D);

    public SnowShortGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape shape() {
        return SHAPE;
    }
}
