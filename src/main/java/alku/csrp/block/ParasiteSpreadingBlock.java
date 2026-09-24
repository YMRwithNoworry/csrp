package alku.csrp.block;

import alku.csrp.infection.BlockInfestation;
import alku.csrp.infection.InfestationSpreadLimiter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasiteSpreading}
 * (out109 {@code block/BlockParasiteSpreading.java}).
 *
 * <p>The original block had two personalities, selected by its {@code infested} constructor
 * argument:</p>
 * <ul>
 *   <li>{@code infested == false} (stains, harlequinn grass): the random tick only ran while the
 *       block still sat inside a live parasite biome, and it spread that biome's dirt/stone
 *       replacement into the surrounding blocks ({@code spreadBiomeBlockStain}).</li>
 *   <li>{@code infested == true} (bloody ice, infested leaves, ...): half of the random ticks
 *       converted a neighbour through {@code canInfestBlock} and the remaining 5&nbsp;% refreshed
 *       the same conversion with a higher stage.</li>
 * </ul>
 *
 * <p>Both halves map onto the ported conversion machinery: {@code canInfestBlock} is
 * {@link BlockInfestation#spread(ServerLevel, BlockPos, int, RandomSource)} (the port of
 * {@code BeckonBlockInfestation.beckonInfestation}) and the biome-stain branch is
 * {@link BlockInfestation#infestAround(ServerLevel, BlockPos, int, InfestationSpreadLimiter.Type)}
 * with the {@link InfestationSpreadLimiter.Type#BIOME} budget the original used for stains.</p>
 *
 * <p>The original also guarded every tick with {@code world.isAreaLoaded(pos, 3)}; 26.3 keeps the
 * same guard through {@link ServerLevel#isAreaLoaded(BlockPos, int)} so a random tick near an
 * unloaded chunk edge can never force a chunk load.</p>
 */
public class ParasiteSpreadingBlock extends Block {
    /** Original {@code rand.nextDouble() < 0.5} branch of {@code updateTick}. */
    private static final double INFEST_CHANCE = 0.5D;
    /** Original {@code rand.nextDouble() <= 0.05} refresh branch of {@code updateTick}. */
    private static final double INFEST_REFRESH_CHANCE = 0.05D;
    /** Stage used by the refresh branch, mirroring the block's own 1.12.2 metadata. */
    private static final int REFRESH_STAGE = 1;

    private final boolean infested;

    public ParasiteSpreadingBlock(Properties properties, boolean infested) {
        super(properties.randomTicks());
        this.infested = infested;
    }

    /** The original's {@code isInfestedBlock} field. */
    public final boolean isInfestedBlock() {
        return infested;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 3)) {
            return;
        }
        if (infested) {
            if (random.nextDouble() < INFEST_CHANCE) {
                BlockInfestation.spread(level, pos, 0, random);
            } else if (random.nextDouble() <= INFEST_REFRESH_CHANCE) {
                BlockInfestation.spread(level, pos, REFRESH_STAGE, random);
            }
        } else {
            BlockInfestation.infestAround(level, pos, 0, InfestationSpreadLimiter.Type.BIOME);
        }
    }
}
