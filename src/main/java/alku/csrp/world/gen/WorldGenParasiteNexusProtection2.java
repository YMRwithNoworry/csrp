package alku.csrp.world.gen;

import alku.csrp.Csrp;
import alku.csrp.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteNexusProtection2}.
 *
 * <p>Places two stone "beckon" pillar templates on a circle of radius 6 and 9 around the nexus.
 * The template name follows the original roll exactly: a size of 2..5 followed by the per-size
 * suffix table, i.e. {@code beckon_2x2_1}, {@code beckon_3x3_1.._5}, {@code beckon_4x4_1.._2} and
 * {@code beckon_5x5_1}.  All six files already exist under
 * {@code data/csrp/structure}.</p>
 *
 * <p>Deviation: the original built its angle from an unseeded {@code new Random()} while drawing the
 * template name from {@code world.rand}.  The port takes a single {@link RandomSource} so a
 * generation is reproducible from the caller's seed; this only changes the angle's source, not the
 * distribution or the template roll order.</p>
 */
public final class WorldGenParasiteNexusProtection2 extends WorldGenParasiteColonyBase {
    /** {@code ParasiteEventEntity#getFloor(world, pos, 7)}. */
    private static final int FLOOR_RANGE = 7;

    public WorldGenParasiteNexusProtection2(int stage) {
        super(stage);
        this.wall = ParasiteGenContext.DENSE_WALL;
        this.tacle = ParasiteGenContext.STAIN_FEELER;
        this.floor = ParasiteGenContext.STAIN_DIRT;
    }

    @Override
    public boolean generate(ServerLevel level, RandomSource rand, BlockPos posss) {
        BlockState pillarBlock = ParasiteGenContext.STONE;
        generateRandomPillar(level, posss.below(1), 6, 5, pillarBlock);
        generateRandomPillar(level, posss.below(1), 9, 9, pillarBlock);
        return true;
    }

    /** {@code generateRandomPillar}: pick a point on the circle, drop to the floor, place a template. */
    public void generateRandomPillar(ServerLevel level, BlockPos center, int radius, int height,
            BlockState blockState) {
        double theta = level.getRandom().nextDouble() * 2.0D * Math.PI;
        BlockPos basePos = getCirclePoint(center, radius, theta);
        basePos = ParasiteGenContext.floor(level, basePos, FLOOR_RANGE);
        if (basePos == null) {
            return;
        }
        basePos = basePos.below();
        String out = "beckon_";
        int outt = level.getRandom().nextInt(4) + 2;
        out = out + outt + "x" + outt;
        switch (outt) {
            case 2 -> out = out + "_1";
            case 3 -> out = out + "_" + (level.getRandom().nextInt(5) + 1);
            case 4 -> out = out + "_" + (level.getRandom().nextInt(2) + 1);
            default -> out = out + "_1";
        }
        StructurePlacer.place(level, Identifier.fromNamespaceAndPath(Csrp.MODID, out), basePos,
                level.getRandom());
    }
}
