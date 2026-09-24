package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockHirsuteHair}
 * (out109 {@code block/BlockHirsuteHair.java}) — registered as {@code srparasites:hirsute_hair}
 * ({@code init/SRPBlocks.java:632}).
 *
 * <p>The original was a {@code BlockBush} with {@code AABB(0.1, 0, 0.1, 0.9, 0.9, 0.9)}
 * ({@code BlockHirsuteHair.java:18,42}), hardness 0.0F, the flesh sound, non-opaque rendering, and a
 * single support rule: the block below must belong to the {@code srparasites} namespace
 * ({@code isSRPBlock}, {@code BlockHirsuteHair.java:46-57}).</p>
 */
public final class HirsuteHairBlock extends BushBlock {
    public HirsuteHairBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isSrpBlock(state);
    }

    /** {@code BlockHirsuteHair.isSRPBlock} — any block registered under {@code srparasites}. */
    static boolean isSrpBlock(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && "srparasites".equals(id.getNamespace());
    }
}
