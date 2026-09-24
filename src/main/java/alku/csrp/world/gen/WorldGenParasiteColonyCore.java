package alku.csrp.world.gen;

import alku.csrp.block.SrpCoreBlock;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteColonyCore}.
 *
 * <p>The colony heart: a red-stained ground disc, a tapered 12..14 high feeler-stain tower, six DNA
 * helices of flesh/sackflesh/bone/rubble, four stacked bone/flesh/dense-walled pods hung on a circle
 * of radius 7/13 and the heart block itself.  The original spawned it from
 * {@code ParasiteEventWorld#generateColony} at the 26-block grid position after a floor scan of 100
 * blocks; {@link alku.csrp.world.ColonyStructureGenerator#generateCore} is the 26.3 entry point.</p>
 *
 * <p>Faithful details kept from the original: the {@code posss.below(7)} tower offset, the
 * {@code Math.min(8)}/{@code Math.max(5)} radius clamp, the {@code int} division in
 * {@code double spa = height / sec}, the {@code --kil} decrement inside the argument list, the
 * radius-7/13 circle points from {@code atm} and the final coin flip that either places the heart at
 * the entrance or grows one more pod first.</p>
 */
public final class WorldGenParasiteColonyCore extends WorldGenParasiteColonyBase {
    public WorldGenParasiteColonyCore(int stage) {
        super(stage);
        this.wall = ParasiteGenContext.DENSE_WALL;
        this.tacle = ParasiteGenContext.STAIN_FEELER;
        this.floor = ParasiteGenContext.STAIN_DIRT;
    }

    @Override
    public boolean generate(ServerLevel level, RandomSource rand, BlockPos posss) {
        BlockPos enter = posss;
        replaceCircleGround(level, posss.below(), 12, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(2), 12, ParasiteGenContext.STAIN_RED);
        replaceCircleGround(level, posss.below(3), 12, ParasiteGenContext.STAIN_RED);

        int missing = 40;
        posss = posss.below(7);
        int height = 12 + rand.nextInt(3);
        int xx = 2;
        int zz = 2;
        int tic = 6;
        int cool = 3;
        int changeX = 0;

        for (int i = 0; i < height; i++) {
            changeX--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(3) == 0) {
                    xx = Math.min(8, xx + 2);
                    zz = Math.min(8, zz + 2);
                } else {
                    xx = Math.max(5, xx - 1);
                    zz = Math.max(5, zz - 1);
                }
                changeX = cool;
            }

            generateCircle(ParasiteGenContext.RUBBLE_FLESH, ParasiteGenContext.STAIN_FLESH, level,
                    level.getRandom(), posss, xx, zz, 1, 20000, 6);
            int hx = xx - tic;
            int hz = zz - tic;
            generateCircle(ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.BONE_BLOCK, level,
                    level.getRandom(), posss, hx, hz, 1, 20000, 6);
            posss = posss.above();
        }

        int bonusH = rand.nextInt(5);
        height = 10 + rand.nextInt(5);
        int kil = 3;
        int sec = 2;
        double spa = height / sec;
        posss = posss.below(3);
        generateDNAHelix(ParasiteGenContext.STAIN_FLESH, level, level.getRandom(), posss, kil, sec, spa);
        generateDNAHelix(ParasiteGenContext.STAIN_SACKFLESH, level, level.getRandom(), posss.above(),
                kil, sec, spa);
        generateDNAHelix(ParasiteGenContext.RUBBLE_BONE, level, level.getRandom(), posss.above(2),
                kil, sec, spa);
        generateDNAHelix(ParasiteGenContext.BONE_BLOCK, level, level.getRandom(), posss.above(3),
                --kil, sec, spa);
        kil += 2;
        int var79 = 2;
        generateDNAHelix(ParasiteGenContext.STAIN_FEELER, level, level.getRandom(), posss, kil, var79, spa);

        posss = posss.above(height);
        generateSphere(level, posss, 8, 6, rand, false, 6, false, 2, 1, 5,
                ParasiteGenContext.RUBBLE_FLESH, ParasiteGenContext.STAIN_FLESH,
                ParasiteGenContext.AIR, missing);
        posss = posss.above(22);
        BlockPos atm = posss;

        int radius = 7;
        double theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, radius, theta);
        int var40 = 7;
        xx = 1;
        zz = 2;
        int var55 = 1;
        int var60 = 1;
        changeX = 0;
        int changeZ = 0;

        for (int i = 0; i < var40; i++) {
            changeX--;
            changeZ--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(2) == 0) {
                    xx = Math.min(2, xx + 1);
                    zz = Math.min(1, zz + 1);
                } else {
                    xx = Math.max(1, xx - 1);
                    zz = Math.max(1, zz - 1);
                }
                changeX = var60;
            }

            generateCircle(ParasiteGenContext.RUBBLE_BONE, ParasiteGenContext.STAIN_FLESH, level,
                    level.getRandom(), posss, xx, zz, 1, 20000, 6);
            int hx = xx - var55;
            int hz = zz - var55;
            generateCircle(ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.BONE_BLOCK, level,
                    level.getRandom(), posss, hx, hz, 1, 20000, 6);
            posss = posss.above();
        }

        generateSphere(level, posss, 3, 3, rand, false, 3, false, 2, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);

        posss = atm;
        int var80 = 7;
        theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, var80, theta);
        var40 = 27;
        xx = 1;
        zz = 1;
        var55 = 1;
        var60 = 1;
        changeX = 0;
        changeZ = 0;

        for (int i = 0; i < var40; i++) {
            changeX--;
            changeZ--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(2) == 0) {
                    xx = Math.min(2, xx + 1);
                    zz = Math.min(1, zz + 1);
                } else {
                    xx = Math.max(1, xx - 1);
                    zz = Math.max(1, zz - 1);
                }
                changeX = var60;
            }

            generateCircle(ParasiteGenContext.RUBBLE_FLESH, ParasiteGenContext.STAIN_FLESH, level,
                    level.getRandom(), posss, xx, zz, 1, 20000, 6);
            int hx = xx - var55;
            int hz = zz - var55;
            generateCircle(ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.BONE_BLOCK, level,
                    level.getRandom(), posss, hx, hz, 1, 20000, 6);
            posss = posss.above();
        }

        generateSphere(level, posss, 3, 3, rand, false, 3, false, 2, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);

        posss = atm;
        var80 = 13;
        theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, var80, theta);
        var40 = 10;
        xx = 1;
        zz = 1;
        var55 = 1;
        var60 = 1;
        changeX = 0;
        changeZ = 0;

        for (int i = 0; i < var40; i++) {
            changeX--;
            changeZ--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(2) == 0) {
                    xx = Math.min(2, xx + 1);
                    zz = Math.min(1, zz + 1);
                } else {
                    xx = Math.max(1, xx - 1);
                    zz = Math.max(1, zz - 1);
                }
                changeX = var60;
            }

            generateCircle(ParasiteGenContext.RUBBLE_BONE, ParasiteGenContext.STAIN_FLESH, level,
                    level.getRandom(), posss, xx, zz, 1, 20000, 6);
            int hx = xx - var55;
            int hz = zz - var55;
            generateCircle(ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.BONE_BLOCK, level,
                    level.getRandom(), posss, hx, hz, 1, 20000, 6);
            posss = posss.above();
        }

        generateSphere(level, posss, 3, 3, rand, false, 3, false, 2, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);

        posss = atm;
        posss = posss.above(7);
        var80 = 2;
        var40 = 8;
        xx = 1;
        zz = 2;
        var55 = 1;
        var60 = 1;
        changeX = 0;
        changeZ = 0;

        for (int i = 0; i < var40; i++) {
            changeX--;
            changeZ--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(2) == 0) {
                    xx = Math.min(3, xx + 1);
                    zz = Math.min(2, zz + 1);
                } else {
                    xx = Math.max(1, xx - 1);
                    zz = Math.max(1, zz - 1);
                }
                changeX = var60;
            }

            generateCircle(ParasiteGenContext.RUBBLE_BONE, ParasiteGenContext.STAIN_FLESH, level,
                    level.getRandom(), posss, xx, zz, 1, 20000, 6);
            int hx = xx - var55;
            int hz = zz - var55;
            generateCircle(ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.BONE_BLOCK, level,
                    level.getRandom(), posss, hx, hz, 1, 20000, 6);
            posss = posss.above();
        }

        generateSphere(level, posss, 3, 3, rand, false, 3, false, 2, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);

        posss = posss.above(10);
        if (rand.nextBoolean()) {
            placeCore(level, enter, 1);
            return true;
        }

        var80 = 4;
        theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, var80, theta);
        var40 = 7;
        xx = 1;
        zz = 1;
        var55 = 1;
        var60 = 1;
        changeX = 0;
        changeZ = 0;

        for (int i = 0; i < var40; i++) {
            changeX--;
            changeZ--;
            if (level.getRandom().nextInt(2) == 0 && changeX <= 0) {
                if (level.getRandom().nextInt(2) == 0) {
                    xx = Math.min(2, xx + 1);
                    zz = Math.min(1, zz + 1);
                } else {
                    xx = Math.max(1, xx - 1);
                    zz = Math.max(1, zz - 1);
                }
                changeX = var60;
            }

            generateCircle(ParasiteGenContext.RUBBLE_BONE, ParasiteGenContext.STAIN_FLESH, level,
                    level.getRandom(), posss, xx, zz, 1, 20000, 6);
            int hx = xx - var55;
            int hz = zz - var55;
            generateCircle(ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.BONE_BLOCK, level,
                    level.getRandom(), posss, hx, hz, 1, 20000, 6);
            posss = posss.above();
        }

        generateSphere(level, posss, 3, 3, rand, false, 3, false, 2, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);
        placeCore(level, enter, 1);
        return true;
    }

    /** {@code placeCore}: the active colony heart, the block {@code ParasiteEventWorld} keyed on. */
    private void placeCore(ServerLevel level, BlockPos pos, int stage) {
        BlockState heart = ModBlocks.COLONYHEART.get().defaultBlockState()
                .setValue(SrpCoreBlock.ACTIVE, stage);
        placeBlock(level, pos, heart);
    }
}
