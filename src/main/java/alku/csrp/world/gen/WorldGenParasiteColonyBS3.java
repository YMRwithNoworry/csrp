package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteColonyBS3}.
 *
 * <p>The smallest building: two dense-walled bulges, the first on a radius-3 circle with a 2/10
 * inner height and a 2-block tip, the second on a radius-6 circle with a 4/2 inner height and a
 * 4-block tip.</p>
 */
public final class WorldGenParasiteColonyBS3 extends WorldGenParasiteColonyBase {
    public WorldGenParasiteColonyBS3(int stage) {
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

        int radius = 3;
        double theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, radius, theta);
        int missing = 40;
        generateSphere(level, posss, 2, 10, rand, false, 3, false, 2, 1, 2,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);

        posss = enter;
        int var12 = 6;
        theta = rand.nextDouble() * 2.0D * Math.PI;
        posss = getCirclePoint(posss, var12, theta);
        generateSphere(level, posss, 2, 10, rand, false, 3, false, 4, 2, 4,
                ParasiteGenContext.DENSE_WALL, ParasiteGenContext.RUBBLE_BRICKS,
                ParasiteGenContext.AIR, missing);
        return true;
    }
}
