package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteColonyB1}.
 *
 * <p>A 22..24 high wobbling bone/flesh tower with a 2/3 chance of a hanging flesh pod on top and
 * three concentric DNA helices of bone block and flesh stain below it.</p>
 */
public final class WorldGenParasiteColonyB1 extends WorldGenParasiteColonyBase {
    public WorldGenParasiteColonyB1(int stage) {
        super(stage);
        this.floor = ParasiteGenContext.STAIN_DIRT;
    }

    @Override
    public boolean generate(ServerLevel level, RandomSource rand, BlockPos posss) {
        replaceCircleGround(level, posss.below(), 12, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(2), 12, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(3), 12, ParasiteGenContext.STAIN_RED);

        int missing = 40;
        int height = 22 + rand.nextInt(3);
        int xx = 2;
        int zz = 2;
        int tic = 2;
        int cool = 3;
        int changeX = 0;

        for (int i = 0; i < height; i++) {
            changeX--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(3) == 0) {
                    xx = Math.min(9, xx + 2);
                    zz = Math.min(9, zz + 2);
                } else {
                    xx = Math.max(3, xx - 1);
                    zz = Math.max(3, zz - 1);
                }
                changeX = cool;
            }

            generateCircle(ParasiteGenContext.RUBBLE_BONE, ParasiteGenContext.STAIN_FLESH, level,
                    level.getRandom(), posss, xx, zz, 1, 20000, 6);
            int hx = xx - tic;
            int hz = zz - tic;
            generateCircle(ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.BONE_BLOCK, level,
                    level.getRandom(), posss, hx, hz, 1, 20000, 6);
            posss = posss.above();
        }

        BlockPos he = posss;
        int aa = zz;
        int bonusH = rand.nextInt(5);
        posss = posss.above(18 + bonusH - zz / 2 * 2);
        if (rand.nextBoolean()) {
            generateSphere(level, posss, zz + 1, 2, rand, false, 1, false, 1, 1, 5,
                    ParasiteGenContext.RUBBLE_FLESH, ParasiteGenContext.STAIN_FLESH,
                    ParasiteGenContext.AIR, missing);
        }

        generateDNAHelix(ParasiteGenContext.BONE_BLOCK, level, level.getRandom(), he.below(4), aa - 2, 2,
                11 + bonusH);
        generateDNAHelix(ParasiteGenContext.STAIN_FLESH, level, level.getRandom(), he.below(3), aa - 2, 2,
                11 + bonusH);
        generateDNAHelix(ParasiteGenContext.BONE_BLOCK, level, level.getRandom(), he.below(2), aa - 2, 2,
                11 + bonusH);
        return true;
    }
}
