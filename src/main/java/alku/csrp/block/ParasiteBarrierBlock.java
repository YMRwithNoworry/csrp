package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasiteBarrier}
 * (out109 {@code block/BlockParasiteBarrier.java}) — registered as
 * {@code srparasites:parasite_barrier} ({@code init/SRPBlocks.java:232}).
 *
 * <p>Reproduced behaviour: an indestructible, explosion-proof, immovable barrier with
 * hardness {@code -1.0F} and resistance {@code 6000000.0F}
 * ({@code BlockParasiteBarrier.java:18-20}); no bounding box ({@code func_180646_a} returns
 * {@code NULL_AABB}, line 76), full-cube rendering, and
 * {@code canEntityDestroy == false} / {@code canDropFromExplosion == false}
 * ({@code BlockParasiteBarrier.java:90-96}).  Its configurable chunk radius lived in
 * {@code TileEntityParasiteBarrier}; that tile entity belongs to the ported relay/barrier machinery
 * and needs a {@code registry/ModBlockEntities.java} entry that is outside this task's write scope,
 * so the block registers without one and the radius configuration is a documented follow-up.</p>
 */
public final class ParasiteBarrierBlock extends Block {
    public ParasiteBarrierBlock(Properties properties) {
        super(properties);
    }

    /** {@code BlockParasiteBarrier.func_180646_a} — no collision box at all. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    /** {@code canEntityDestroy} / {@code canDropFromExplosion} — never destroyed. */
    @Override
    public float getExplosionResistance() {
        return 6_000_000.0F;
    }
}
