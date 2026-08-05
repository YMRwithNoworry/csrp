package alku.csrp.block;

import net.minecraft.world.level.block.HalfTransparentBlock;

/**
 * Parasitic fog produced around Dispatcher nests. It cannot be mined and is
 * only removed by a Fog Nullifier.
 */
public final class FogBlock extends HalfTransparentBlock {
    public FogBlock(Properties properties) {
        super(properties);
    }
}
