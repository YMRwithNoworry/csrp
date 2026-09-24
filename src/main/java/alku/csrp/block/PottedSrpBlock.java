package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockPottedSRPFlower}
 * (out109 {@code block/BlockPottedSRPFlower.java}) — the four pot compatibility ids
 * ({@code potted_assimilated_blossom}, {@code potted_consumed_assimilated_blossom},
 * {@code consumed_pot}, {@code infested_pot}).
 *
 * <p>The original was a full cube-shaped {@code Block} with a pot-sized box
 * {@code AABB(0.3125, 0.0, 0.3125, 0.6875, 0.375, 0.6875)}
 * ({@code BlockPottedSRPFlower.java:14,33}), hardness 0.0F, {@code SoundType.STONE}, non-opaque and
 * non-full-cube rendering, and it dropped only itself ({@code getDrops}, line 44) while
 * {@code harvestBlock} removed the block without the flower spreading
 * ({@code BlockPottedSRPFlower.java:52-55}).</p>
 */
public final class PottedSrpBlock extends Block {
    /** {@code AABB} = AABB(0.3125, 0.0, 0.3125, 0.6875, 0.375, 0.6875). */
    private static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);

    public PottedSrpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
