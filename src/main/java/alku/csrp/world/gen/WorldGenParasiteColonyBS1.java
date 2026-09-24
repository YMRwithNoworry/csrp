package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteColonyBS1}.
 *
 * <p>The stage-2 "small" building: an 8 high bone/flesh tower at radius 4, a dense-walled pod and,
 * on the coin flip, a 7 high second tower at radius 4.</p>
 *
 * <p>The original also carried two private helpers, {@code placeWallsBottom} and
 * {@code placeWallsTopIn}, which no code path ever called (they are not referenced anywhere in the
 * 1.10.9 sources, including {@code func_180709_b} itself).  They are dead code and are not ported;
 * the wall/column/vine logic they contained is already covered by
 * {@link WorldGenParasiteColonyBase#placeColumn}, {@code directionToGrow} and {@code addVines}.</p>
 */
public final class WorldGenParasiteColonyBS1 extends WorldGenParasiteColonyBase {
    public WorldGenParasiteColonyBS1(int stage) {
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
        int radius = 4;
        double theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, radius, theta);
        int height = 8;
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
            return true;
        }

        int var22 = 4;
        theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, var22, theta);
        int var24 = 7;
        xx = 1;
        zz = 1;
        int var27 = 1;
        int var28 = 1;
        changeX = 0;

        for (int i = 0; i < var24; i++) {
            changeX--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(2) == 0) {
                    xx = Math.min(2, xx + 1);
                    zz = Math.min(1, zz + 1);
                } else {
                    xx = Math.max(1, xx - 1);
                    zz = Math.max(1, zz - 1);
                }
                changeX = var28;
            }

            generateCircle(ParasiteGenContext.RUBBLE_BONE, ParasiteGenContext.STAIN_FLESH, level,
                    level.getRandom(), posss, xx, zz, 1, 20000, 6);
            int hx = xx - var27;
            int hz = zz - var27;
            generateCircle(ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.BONE_BLOCK, level,
                    level.getRandom(), posss, hx, hz, 1, 20000, 6);
            posss = posss.above();
        }

        generateSphere(level, posss, 3, 3, rand, false, 3, false, 2, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);
        return true;
    }
}
