package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockTressesHair}
 * (out109 {@code block/BlockTressesHair.java}) — registered as {@code srparasites:tresses_hair}
 * ({@code init/SRPBlocks.java:633}).
 *
 * <p>{@code BlockTressesHair extends BlockDoublePlant} ({@code BlockTressesHair.java:17}) with
 * hardness 0.0F, non-opaque rendering and one support rule: the block below must belong to the
 * {@code srparasites} namespace ({@code isSRPBlock},
 * {@code BlockTressesHair.java:40-51}).  The 26.3 {@link DoublePlantBlock} supplies the
 * {@code half=lower/upper} state that {@code blockstates/tresses_hair.json} expects.</p>
 */
public final class TressesHairBlock extends DoublePlantBlock {
    public TressesHairBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return HirsuteHairBlock.isSrpBlock(state);
    }
}
