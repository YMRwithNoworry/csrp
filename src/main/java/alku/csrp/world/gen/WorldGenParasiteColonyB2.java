package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteColonyB2}.
 *
 * <p>No tower body: five stacked DNA helices of flesh, sackflesh, bone rubble, bone block and feeler
 * stain over a red ground disc, then one coin flip that either stops or pays out a
 * {@code generateSphere(4, 3, ..., random = true)} feeler pod — the only colony building whose
 * sphere uses the {@code random} growth flag and the 1/20 inverted-tip roll.</p>
 */
public final class WorldGenParasiteColonyB2 extends WorldGenParasiteColonyBase {
    public WorldGenParasiteColonyB2(int stage) {
        super(stage);
        this.floor = ParasiteGenContext.STAIN_DIRT;
    }

    @Override
    public boolean generate(ServerLevel level, RandomSource rand, BlockPos posss) {
        replaceCircleGround(level, posss.below(), 12, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(2), 12, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(3), 12, ParasiteGenContext.STAIN_RED);

        int missing = 40;
        int height = 22 + rand.nextInt(10);
        int kil = 3;
        int sec = 2;
        double spa = height / sec;

        generateDNAHelix(ParasiteGenContext.STAIN_FLESH, level, level.getRandom(), posss, kil, sec, spa);
        generateDNAHelix(ParasiteGenContext.STAIN_SACKFLESH, level, level.getRandom(), posss.above(),
                kil, sec, spa);
        generateDNAHelix(ParasiteGenContext.RUBBLE_BONE, level, level.getRandom(), posss.above(2),
                kil, sec, spa);
        generateDNAHelix(ParasiteGenContext.BONE_BLOCK, level, level.getRandom(), posss.above(3),
                --kil, sec, spa);
        kil += 2;
        int var14 = 2;
        generateDNAHelix(ParasiteGenContext.STAIN_FEELER, level, level.getRandom(), posss, kil, var14, spa);

        posss = posss.above(height);
        if (rand.nextBoolean()) {
            return true;
        }

        generateSphere(level, posss, 4, 3, rand, rand.nextInt(20) == 0, 4, true, 1, 3, 2,
                ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.STAIN_FEELER,
                ParasiteGenContext.AIR, missing);
        return true;
    }
}
