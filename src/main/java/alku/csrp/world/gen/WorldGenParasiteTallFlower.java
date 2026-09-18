package alku.csrp.world.gen;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of 1.10.9 {@code WorldGenParasiteTallFlower}.
 *
 * <p>A 25-block-tall flower: a 3x3 "flesh" base that sinks from the position down to the ground, a
 * central plant column, plant fingers that walk in the four cardinal directions, a petal cap and
 * four petal arms that droop two blocks below the head.</p>
 */
public final class WorldGenParasiteTallFlower {
    private static final int HEIGHT = 25;

    private WorldGenParasiteTallFlower() {
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
            for (int x = position.getX() - 2; x <= position.getX() + 2 && clear; x++) {
                for (int z = position.getZ() - 2; z <= position.getZ() + 2 && clear; z++) {
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

        // Original: ParasiteTrunk with VARIANT = PLANT.
        BlockState plant = ModBlocks.PARASITETRUNK_PLANT.get().defaultBlockState();
        BlockState petal = ParasiteGenContext.STAIN_FEELER;
        BlockState base = ParasiteGenContext.STAIN_FLESH;

        // Root: sink to the floor, thickening the 3x3 flesh base on the way down.
        int lag = 0;
        BlockPos current = position;
        int guard = 0;
        while (!ParasiteGenContext.isSolidGround(level, current.below())
                && current.below().getY() >= 1 && guard++ < 512) {
            current = current.below();
            for (int yyy = 0; yyy <= lag; yyy++) {
                for (int xs = -1; xs <= 1; xs++) {
                    for (int zs = -1; zs <= 1; zs++) {
                        ParasiteGenContext.setBlock(level,
                                new BlockPos(current.getX() + xs, current.getY(), current.getZ() + zs), base);
                    }
                }
            }
        }

        current = position;
        lag = random.nextInt(2) + 1;
        for (int yyy = 0; yyy < lag; yyy++) {
            for (int xs = -1; xs <= 1; xs++) {
                for (int zs = -1; zs <= 1; zs++) {
                    ParasiteGenContext.setBlock(level,
                            new BlockPos(current.getX() + xs, current.getY(), current.getZ() + zs), base);
                }
            }
            current = current.above();
        }

        current = current.below();
        current = current.above();
        ParasiteGenContext.setBlock(level, current, base);
        for (int dir = 0; dir <= 3; dir++) {
            ParasiteGenContext.setBlock(level, steps(current, dir, 1), base);
        }

        current = placeColumn(level, current, 5, plant);
        if (random.nextInt(8) != 0) {
            current = horizontal(current, random.nextInt(4));
        }
        current = placeColumn(level, current, 5, plant);
        if (random.nextInt(8) != 0) {
            current = horizontal(current, random.nextInt(4));
        }
        current = placeColumn(level, current, 5, plant);
        if (random.nextInt(5) != 0) {
            if (random.nextInt(8) != 0) {
                current = horizontal(current, random.nextInt(4));
            }
            current = placeColumn(level, current, 5, plant);
        }

        for (int dir = 0; dir <= 3; dir++) {
            ParasiteGenContext.setBlock(level, steps(current, dir, 1), petal);
        }

        current = placeColumn(level, current, 1, petal);
        for (int yyy = 0; yyy < 2; yyy++) {
            for (int xs = -1; xs <= 1; xs++) {
                for (int zs = -1; zs <= 1; zs++) {
                    ParasiteGenContext.setBlock(level,
                            new BlockPos(current.getX() + xs, current.getY(), current.getZ() + zs), petal);
                }
            }
            current = current.above();
        }
        ParasiteGenContext.setBlock(level, current, petal);

        for (int dir = 0; dir <= 3; dir++) {
            BlockPos arm = steps(current.below(2), dir, 2);
            arm = placeColumn(level, arm, 4, petal);
            arm = steps(arm, dir, 1);
            arm = placeColumn(level, arm, 2, petal);
            arm = steps(arm, dir, 1);
            placeColumn(level, arm, 1, petal);
        }

        for (int dir = 0; dir <= 3; dir++) {
            BlockPos arm = steps(current.below(2), dir, 2);
            arm = horizontal(arm, (dir + 1) % 4);
            arm = horizontal(arm, (dir + 1) % 4);
            placeColumn(level, arm, 3, petal);
        }
        return true;
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

    private static BlockPos horizontal(BlockPos pos, int choice) {
        return switch (choice) {
            case 0 -> pos.south();
            case 1 -> pos.west();
            case 2 -> pos.north();
            default -> pos.east();
        };
    }

    private static BlockPos placeColumn(ServerLevel level, BlockPos pos, int height, BlockState state) {
        BlockPos current = pos;
        for (int i = 0; i < height; i++) {
            ParasiteGenContext.setBlock(level, current, state);
            current = current.above();
        }
        return current;
    }
}
