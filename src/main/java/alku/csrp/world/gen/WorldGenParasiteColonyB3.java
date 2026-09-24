package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteColonyB3}.
 *
 * <p>The default outpost body (the original used it for stage 1 and for the case-2 outpost roll): a
 * dense-walled 4/3 sphere base, a 20 high bone/flesh tower at radius 8, a second pod, and either an
 * entrance dug towards {@code enter} or a further 15 high tower at radius 4 before the entrance.</p>
 */
public final class WorldGenParasiteColonyB3 extends WorldGenParasiteColonyBase {
    public WorldGenParasiteColonyB3(int stage) {
        super(stage);
        this.floor = ParasiteGenContext.STAIN_DIRT;
    }

    @Override
    public boolean generate(ServerLevel level, RandomSource rand, BlockPos posss) {
        BlockPos enter = posss;
        replaceCircleGround(level, posss.below(), 12, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(2), 12, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(3), 12, ParasiteGenContext.STAIN_RED);

        int missing = 40;
        generateSphere(level, posss, 4, 3, rand, false, 6, false, 2, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);

        posss = posss.above(12);
        int radius = 8;
        double theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, radius, theta);
        int height = 20;
        int xx = 1;
        int zz = 2;
        int tic = 1;
        int cool = 1;
        int changeX = 0;

        for (int i = 0; i < height; i++) {
            changeX--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(2) == 0) {
                    xx = Math.min(3, xx + 1);
                    zz = Math.min(2, zz + 1);
                } else {
                    xx = Math.max(1, xx - 1);
                    zz = Math.max(1, zz - 1);
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

        generateSphere(level, posss, 3, 3, rand, false, 3, false, 2, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);

        posss = posss.above(10);
        if (rand.nextBoolean()) {
            addEntrance(level, rand, enter, 5);
            return true;
        }

        int var23 = 4;
        theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, var23, theta);
        int var25 = 15;
        xx = 1;
        zz = 1;
        int var28 = 1;
        int var29 = 1;
        changeX = 0;

        for (int i = 0; i < var25; i++) {
            changeX--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(2) == 0) {
                    xx = Math.min(2, xx + 1);
                    zz = Math.min(1, zz + 1);
                } else {
                    xx = Math.max(1, xx - 1);
                    zz = Math.max(1, zz - 1);
                }
                changeX = var29;
            }

            generateCircle(ParasiteGenContext.RUBBLE_BONE, ParasiteGenContext.STAIN_FLESH, level,
                    level.getRandom(), posss, xx, zz, 1, 20000, 6);
            int hx = xx - var28;
            int hz = zz - var28;
            generateCircle(ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.BONE_BLOCK, level,
                    level.getRandom(), posss, hx, hz, 1, 20000, 6);
            posss = posss.above();
        }

        generateSphere(level, posss, 3, 3, rand, false, 3, false, 2, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);
        addEntrance(level, rand, enter, 5);
        return true;
    }
}
