package alku.csrp.world;

import alku.csrp.world.gen.WorldGenParasiteColonyB1;
import alku.csrp.world.gen.WorldGenParasiteColonyB2;
import alku.csrp.world.gen.WorldGenParasiteColonyB3;
import alku.csrp.world.gen.WorldGenParasiteColonyB4;
import alku.csrp.world.gen.WorldGenParasiteColonyBS1;
import alku.csrp.world.gen.WorldGenParasiteColonyBS2;
import alku.csrp.world.gen.WorldGenParasiteColonyBS3;
import alku.csrp.world.gen.WorldGenParasiteColonyBS4;
import alku.csrp.world.gen.WorldGenParasiteColonyBase;
import alku.csrp.world.gen.WorldGenParasiteColonyCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * The 26.3 entry point of the legacy colony system, replacing the earlier procedural placeholder.
 *
 * <p>Two callers reach it and both keep their existing signatures:
 * {@link alku.csrp.block.ColonyStructureBlock} (the port of {@code BlockColonyStructure}, whose
 * {@code updateTick} chose the building) calls {@link #generateBuilding} and
 * {@link alku.csrp.world.SrpCoreSystems} (the port of {@code ParasiteEventWorld#generateColony})
 * calls {@link #generateCore}.</p>
 */
public final class ColonyStructureGenerator {
    private ColonyStructureGenerator() {
    }

    /**
     * Port of {@code ParasiteEventWorld#generateColony}: the original snapped the position to a
     * 26-block grid, scanned down up to 100 blocks for a floor and then ran
     * {@code new WorldGenParasiteColonyCore(false, 1)} at that position.  The heart ends up on the
     * foundation itself, which is exactly what {@code placeCore(enter)} did.
     */
    public static BlockPos generateCore(ServerLevel level, BlockPos foundation, RandomSource random) {
        new WorldGenParasiteColonyCore(1).generate(level, random, foundation);
        return foundation;
    }

    /**
     * Port of {@code BlockColonyStructure#updateTick}: the live selection table of 1.10.9.
     *
     * <p>The original nested a second {@code switch (rand.nextInt(3))} inside the metadata switch and
     * wrote a {@code case 3} branch in the stage-1 table.  Because {@code nextInt(3)} only yields
     * 0..2 that branch (which built a B4) was unreachable, and it is reproduced here verbatim rather
     * than "fixed" — B4 and BS4 are otherwise only reachable through the dead
     * {@code onBlockActivated} switch of the original, so they are exposed explicitly through
     * {@link ColonyBuilding} instead of being silently dropped.</p>
     */
    public static boolean generateBuilding(ServerLevel level, BlockPos origin, int stage, RandomSource random) {
        WorldGenParasiteColonyBase building;
        switch (stage) {
            case 1 -> {
                switch (random.nextInt(3)) {
                    case 1 -> building = new WorldGenParasiteColonyB3(2);
                    case 2 -> building = new WorldGenParasiteColonyB2(2);
                    case 3 -> building = new WorldGenParasiteColonyB4(2);
                    default -> building = new WorldGenParasiteColonyB1(2);
                }
            }
            case 2 -> {
                switch (random.nextInt(3)) {
                    case 1 -> building = new WorldGenParasiteColonyBS1(2);
                    case 2 -> building = new WorldGenParasiteColonyBS3(2);
                    default -> building = new WorldGenParasiteColonyBS2(2);
                }
            }
            default -> {
                return false;
            }
        }
        building.generate(level, random, origin);
        return true;
    }

    /** Places one specific building; the original only ever reached B1..B3 and BS1..BS3 implicitly. */
    public static void place(ServerLevel level, BlockPos origin, ColonyBuilding building, int stage,
            RandomSource random) {
        building.create(stage).generate(level, random, origin);
    }

    /**
     * The eight legacy colony buildings.  {@code B4} and {@code BS4} were dead code in 1.10.9 (their
     * only instantiations sat in unreachable switch branches), so a caller that wants them has to ask
     * for them by name.
     */
    public enum ColonyBuilding {
        B1, B2, B3, B4, BS1, BS2, BS3, BS4;

        public WorldGenParasiteColonyBase create(int stage) {
            return switch (this) {
                case B1 -> new WorldGenParasiteColonyB1(stage);
                case B2 -> new WorldGenParasiteColonyB2(stage);
                case B3 -> new WorldGenParasiteColonyB3(stage);
                case B4 -> new WorldGenParasiteColonyB4(stage);
                case BS1 -> new WorldGenParasiteColonyBS1(stage);
                case BS2 -> new WorldGenParasiteColonyBS2(stage);
                case BS3 -> new WorldGenParasiteColonyBS3(stage);
                case BS4 -> new WorldGenParasiteColonyBS4(stage);
            };
        }
    }
}
