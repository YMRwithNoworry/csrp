package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteNexusProtection3}.
 *
 * <p>The third nexus protection: a radius-8 ring of 64 fungus/feeler pillars at height 1..5, then
 * shrinking radius-7..0 rings of 7..14 high fog pillars, the two inner discs at {@code y - 5} and a
 * felt disc under the whole ring.  In the original this was the Rooter nexus ability
 * ({@code EntityPRooter} line 142).</p>
 *
 * <p>The original's second loop is {@code while (radius > 0) { for (pos : getCirclePoints(posss,
 * --radius, steps)) ... } }, which visits radii 7, 6, ..., 0 — the port keeps that pre-decrement so
 * the extra radius-0 pass over the centre survives.</p>
 */
public final class WorldGenParasiteNexusProtection3 extends WorldGenParasiteColonyBase {
    private static final int RING_STEPS = 64;

    public WorldGenParasiteNexusProtection3(int stage) {
        super(stage);
        this.wall = ParasiteGenContext.DENSE_WALL;
        this.tacle = ParasiteGenContext.STAIN_FEELER;
        this.floor = ParasiteGenContext.STAIN_DIRT;
    }

    @Override
    public boolean generate(ServerLevel level, RandomSource rand, BlockPos posss) {
        int radius = 8;
        int steps = RING_STEPS;
        BlockState pillarBlock = ParasiteGenContext.RUBBLE_FUNGUS;
        BlockState pillarBlock2 = ParasiteGenContext.STAIN_FEELER;

        for (BlockPos pos : getCirclePoints(posss, radius, steps)) {
            int min = 1;
            int max = 5;
            generatePillar(level, pos, level.getRandom().nextInt(max - min + 1) + min, pillarBlock,
                    pillarBlock2);
        }

        pillarBlock = ParasiteGenContext.FOG;

        while (radius > 0) {
            for (BlockPos pos : getCirclePoints(posss, --radius, steps)) {
                int min = 7;
                int max = 14;
                generatePillar(level, pos, level.getRandom().nextInt(max - min + 1) + min, pillarBlock,
                        pillarBlock);
            }
        }

        generateCircle(ParasiteGenContext.STAIN_FEELER, ParasiteGenContext.STAIN_FLESH, level, rand,
                posss.below(5), 8, 8, 5, 2, 0);
        generateCircle(ParasiteGenContext.FOG, ParasiteGenContext.FOG, level, rand,
                posss.below(5), 4, 4, 5, 20_000_000, 0);
        replaceCircleGround(level, posss.below(), 8, pillarBlock2);
        return true;
    }
}
