package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteColonyB4}.
 *
 * <p>The stacked-pod variant: a dense-walled 4/3 sphere, two more 5/3 and 3/3 pods at +16 each and
 * a radius-6/3 jump between them, then either an entrance or one last pod offset by 3.</p>
 */
public final class WorldGenParasiteColonyB4 extends WorldGenParasiteColonyBase {
    public WorldGenParasiteColonyB4(int stage) {
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
        generateSphere(level, posss, 4, 3, rand, false, 7, false, 3, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);

        posss = posss.above(16);
        int radius = 6;
        double theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, radius, theta);
        generateSphere(level, posss, 5, 3, rand, false, 3, false, 2, 1, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);

        posss = posss.above(16);
        if (rand.nextBoolean()) {
            addEntrance(level, rand, enter, 5);
            return true;
        }

        int var13 = 3;
        theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, var13, theta);
        generateSphere(level, posss, 3, 3, rand, false, 3, false, 2, 2, 5,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);
        addEntrance(level, rand, enter, 5);
        return true;
    }
}
