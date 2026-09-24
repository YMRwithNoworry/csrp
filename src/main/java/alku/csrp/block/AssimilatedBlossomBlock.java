package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockSRPFlower}
 * (out109 {@code block/BlockSRPFlower.java}) — registered as {@code srparasites:assimilated_blossom}
 * ({@code init/SRPBlocks.java:513}).
 *
 * <p>The original was a plain {@code BlockBush} whose only overrides were
 * {@code canSustainBush} (grass / ground / sand / clay materials —
 * {@code BlockSRPFlower.java:20-23}), non-opaque and non-full cube rendering, a {@code CUTOUT_MIPPED}
 * render layer, and an unconditional "always render each side" answer. Everything else — the
 * {@code FLESH} sound type, the zero hardness and the {@code BlockBush} cross shape — comes from
 * the properties supplied by the registry, exactly as here.</p>
 */
public final class AssimilatedBlossomBlock extends BushBlock {
    public AssimilatedBlossomBlock(Properties properties) {
        super(properties);
    }

    /**
     * Original {@code func_185514_i}: {@code Material.GRASS}, {@code Material.GROUND},
     * {@code Material.SAND} or {@code Material.CLAY}.
     */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.FARMLAND);
    }
}
