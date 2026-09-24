package alku.csrp.block;

import net.minecraft.world.level.block.VineBlock;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockVineBase}
 * (out109 {@code block/BlockVineBase.java}) — registered as {@code srparasites:parasitetendril}
 * ({@code init/SRPBlocks.java:966}, hardness 0.5F, creative, {@code tickRandom = true}).
 *
 * <p>The original was literally two lines of behaviour on top of vanilla {@code BlockVine}: a
 * registry name, a hardness, the {@code tickRandom} flag and the creative tab
 * ({@code BlockVineBase.java:17-23}).  The 26.3 {@link VineBlock} already carries the
 * {@code north/east/south/west/up} state and the spreading random tick, so only the properties
 * differ — {@code blockstates/parasitetendril.json} is a vanilla {@code multipart} vine file.</p>
 */
public final class ParasiteTendrilBlock extends VineBlock {
    public ParasiteTendrilBlock(Properties properties) {
        super(properties.randomTicks());
    }
}
