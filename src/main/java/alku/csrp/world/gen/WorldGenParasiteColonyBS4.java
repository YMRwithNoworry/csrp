package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteColonyBS4}.
 *
 * <p>A 22..24 high bone/flesh tower with the 9/3 widening clamps, a single bone-block DNA helix
 * hanging under it and a 1/2 flesh pod above.  Different from {@code WorldGenParasiteColonyB1}, which
 * uses the same tower shape but three helices and no pod-hanging rule.</p>
 */
public final class WorldGenParasiteColonyBS4 extends WorldGenParasiteColonyBase {
    public WorldGenParasiteColonyBS4(int stage) {
        super(stage);
        this.floor = ParasiteGenContext.STAIN_DIRT;
        this.wall = ParasiteGenContext.DENSE_WALL;
    }

    @Override
    public boolean generate(ServerLevel level, RandomSource rand, BlockPos posss) {
        replaceCircleGround(level, posss.below(), 12, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(2), 8, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(3), 8, ParasiteGenContext.STAIN_RED);

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

        generateDNAHelix(ParasiteGenContext.BONE_BLOCK, level, level.getRandom(), he.below(3), aa - 2, 2,
                11 + bonusH);
        return true;
    }
}
