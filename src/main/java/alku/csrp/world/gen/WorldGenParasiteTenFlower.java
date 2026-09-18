package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of 1.10.9 {@code WorldGenParasiteTenFlower}.
 *
 * <p>A 12-block-tall "ten flower": a 3x3 crown that sinks to the ground, four feeler arms that walk
 * outwards with two side twigs each, a central column and four secondary crowns.  Every block it
 * writes is the {@code parasitestain} "feeler" variant.</p>
 */
public final class WorldGenParasiteTenFlower {
    private static final int HEIGHT = 12;

    private WorldGenParasiteTenFlower() {
    }

    public static boolean generate(ServerLevel level, RandomSource random, BlockPos position) {
        if (ParasiteGenContext.get(level, position.below()).isAir()) {
            return false;
        }
        if (position.getY() < 1 || position.getY() + HEIGHT + 1 > level.getMaxY()) {
            return false;
        }

        boolean clear = true;
        for (int y = position.getY(); y <= position.getY() + 1 + HEIGHT && clear; y++) {
            for (int x = position.getX() - 4; x <= position.getX() + 4 && clear; x++) {
                for (int z = position.getZ() - 4; z <= position.getZ() + 4 && clear; z++) {
                    BlockPos probe = new BlockPos(x, y, z);
                    if (!ParasiteGenContext.inWorld(level, probe)
                            || !ParasiteGenContext.isReplaceable(level, probe)) {
                        clear = false;
                    }
                }
            }
        }
        if (!clear) {
            return false;
        }

        BlockPos current = position;
        int guard = 0;
        while (!ParasiteGenContext.isSolidGround(level, current.below())
                && current.below().getY() >= 1 && guard++ < 512) {
            current = current.below();
            crown(level, current);
        }

        current = position;
        int lag = random.nextInt(2) + 1;
        for (int yyy = 0; yyy <= lag; yyy++) {
            crown(level, current);
            current = current.above();
        }

        current = current.below();

        for (int dir = 0; dir <= 3; dir++) {
            placeTrunk(level, steps(current, dir, 2));
        }

        current = current.above();

        for (int dir = 0; dir <= 3; dir++) {
            BlockPos arm = current;
            int limit = random.nextInt(2) + 3;
            for (int times = 1; times <= limit; times++) {
                arm = steps(arm, dir, 1);
                placeTrunk(level, arm);
            }
            arm = arm.above();
            placeTrunk(level, arm);
            arm = steps(arm, dir, 1);
            placeTrunk(level, arm);
            BlockPos anchor = arm;

            for (int help = 1; help <= 3; help += 2) {
                int sideDir = (dir + help) % 4;
                BlockPos side = steps(anchor, sideDir, 1).above();
                side = placeColumn(level, side, random.nextInt(3) + 1);
                int extra = (help != 1 || (dir != 0 && dir != 3)) ? 1 : 0;
                if (dir == 2 && help == 3) {
                    extra = 0;
                }
                if (dir == 1 && help == 3) {
                    extra = 0;
                }
                BlockPos twig = directionToGrow(side.below(), dir * 10 + extra, true);
                twig = placeColumn(level, twig, random.nextInt(3) + 1);
                twig = directionToGrow(twig, dir * 10 + extra, true);
                placeColumn(level, twig.below(), random.nextInt(3) + 1);
            }
        }

        current = placeColumn(level, current, random.nextInt(4) + 4);

        for (int dir = 0; dir <= 3; dir++) {
            BlockPos growing = placeColumn(level, steps(current, dir, 1), random.nextInt(3) + 2);
            growing = growing.below(2);
            for (int kkk = 0; kkk <= 3; kkk++) {
                if (kkk == 0) {
                    placeColumn(level, steps(growing, dir, 1), 1);
                    growing = growing.above();
                }
                growing = steps(growing, dir, kkk == 0 ? 2 : 1);
                growing = placeColumn(level, growing, random.nextInt(4) + 1);
            }
        }
        return true;
    }

    /** The 3x3 crown of feeler stain the original wrote around every sink step. */
    private static void crown(ServerLevel level, BlockPos center) {
        for (int xs = -1; xs <= 1; xs++) {
            for (int zs = -1; zs <= 1; zs++) {
                placeTrunk(level, new BlockPos(center.getX() + xs, center.getY(), center.getZ() + zs));
            }
        }
    }

    private static void placeTrunk(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.STAIN_FEELER);
    }

    /**
     * {@code getDirectionRoot(center, direction, times)}: 0 = south, 1 = west, 2 = north,
     * anything else = east.
     */
    private static BlockPos steps(BlockPos center, int direction, int times) {
        return switch (direction) {
            case 0 -> center.south(times);
            case 1 -> center.west(times);
            case 2 -> center.north(times);
            default -> center.east(times);
        };
    }

    /** The original {@code placeColumn} decremented its height before walking the column. */
    private static BlockPos placeColumn(ServerLevel level, BlockPos pos, int height) {
        BlockPos current = pos;
        for (int i = 0; i < height - 1; i++) {
            placeTrunk(level, current);
            current = current.above();
        }
        placeTrunk(level, current);
        return current.above();
    }

    /** {@code directionToGrow(..., sideCurse = true)}: up one block, then two diagonal steps. */
    private static BlockPos directionToGrow(BlockPos pos, int choice, boolean sideCurse) {
        BlockPos base = pos.above();
        if (!sideCurse) {
            return switch (choice) {
                case 0 -> base.north();
                case 1 -> base.east();
                case 2 -> base.south();
                default -> base.west();
            };
        }
        return ParasiteGenContext.sideCurse(base, choice);
    }
}
