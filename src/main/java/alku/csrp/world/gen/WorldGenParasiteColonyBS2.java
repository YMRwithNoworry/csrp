package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteColonyBS2}.
 *
 * <p>Three separate 28, 47 and 17 high towers, each anchored on the same {@code enter} point through
 * circles of radius 7, 7 and 2 and each capped by a dense-walled 3/3 pod.  The two ground discs at
 * {@code y - 2} and {@code y - 3} are radius 8 rather than the 12 the other buildings use.</p>
 */
public final class WorldGenParasiteColonyBS2 extends WorldGenParasiteColonyBase {
    public WorldGenParasiteColonyBS2(int stage) {
        super(stage);
        this.floor = ParasiteGenContext.STAIN_DIRT;
        this.wall = ParasiteGenContext.DENSE_WALL;
    }

    @Override
    public boolean generate(ServerLevel level, RandomSource rand, BlockPos posss) {
        BlockPos enter = posss;
        replaceCircleGround(level, posss.below(), 12, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(2), 8, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(3), 8, ParasiteGenContext.STAIN_RED);

        int missing = 40;

        tower(level, rand, posss, 7, 28, 1, 2, 1, 2, missing);
        tower(level, rand, enter, 7, 47, 1, 2, 1, 1, missing);
        tower(level, rand, enter, 2, 17, 1, 2, 1, 1, missing);
        return true;
    }

    /**
     * The original inlined the same loop three times, differing only in the circle radius, the height,
     * the initial {@code zz} and the clamps (28 uses {@code xx <= 2} with {@code zz <= 1}; 47 and 17
     * start from {@code zz = 1}).  Kept as one helper with those arguments so the three calls stay
     * comparable to the original.
     */
    private void tower(ServerLevel level, RandomSource rand, BlockPos posss, int radius, int height,
            int tic, int clampX, int clampZ, int initialZ, int missing) {
        double theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, radius, theta);
        int xx = 1;
        int zz = initialZ;
        int cool = 1;
        int changeX = 0;

        for (int i = 0; i < height; i++) {
            changeX--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(2) == 0) {
                    xx = Math.min(clampX, xx + 1);
                    zz = Math.min(clampZ, zz + 1);
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
    }
}
