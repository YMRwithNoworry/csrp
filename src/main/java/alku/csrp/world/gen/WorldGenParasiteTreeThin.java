package alku.csrp.world.gen;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of 1.10.9 {@code WorldGenParasiteTreeThin}.
 *
 * <p>A 16-block-tall thin parasite tree: a vertical column of {@code parasitethin} with four arms
 * grown north/east/south/west from the top, each arm branching with its own 1-in-3 and 1-in-5
 * re-rolls.</p>
 */
public final class WorldGenParasiteTreeThin {
    private static final int HEIGHT = 16;

    private WorldGenParasiteTreeThin() {
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
            int radius = 1;
            if (y == position.getY()) {
                radius = 0;
            }
            if (y >= position.getY() + 1 + HEIGHT - 2) {
                radius = 2;
            }
            for (int x = position.getX() - radius; x <= position.getX() + radius && clear; x++) {
                for (int z = position.getZ() - radius; z <= position.getZ() + radius && clear; z++) {
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
        while (!ParasiteGenContext.isSolidGround(level, current.below())
                && current.below().getY() >= 1) {
            current = current.below();
            placeTrunk(level, current);
        }

        current = position;
        current = placeColumn(level, current, 3, random, 0.0D, 4);
        grow(level, current.north(), random, 1);
        grow(level, current.east(), random, 2);
        grow(level, current.south(), random, 3);
        grow(level, current.west(), random, 4);
        return true;
    }

    private static void placeThin(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ModBlocks.PARASITETHIN.get().defaultBlockState());
    }

    private static void placeCanister(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ModBlocks.CANISTER_ACTIVE.get().defaultBlockState());
    }

    private static void placeTrunk(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ModBlocks.PARASITETRUNK.get().defaultBlockState());
    }

    /**
     * {@code placeColumn(world, pos, times, rand, extraChance, minimum)} — the count is re-rolled as
     * {@code rand.nextInt(times) + minimum} and a column of that many thin blocks is stacked up.
     */
    private static BlockPos placeColumn(ServerLevel level, BlockPos pos, int times, RandomSource random,
            double extraChance, int minimum) {
        int current = pos.getY();
        int limit = current;
        BlockPos newPos = pos;
        int height = random.nextInt(times) + minimum;
        if (ParasiteGenContext.get(level, pos.below()).isAir()
                && random.nextDouble() <= extraChance) {
            placeCanister(level, pos.below());
        }

        while (current < limit + height && minimum != 1) {
            placeThin(level, newPos);
            newPos = newPos.above();
            current++;
        }

        placeThin(level, newPos);
        return newPos;
    }

    private static void grow(ServerLevel level, BlockPos pos, RandomSource random, int direction) {
        BlockPos current = placeColumn(level, pos, 2, random, 0.3D, 2);
        if (random.nextInt(3) == 0) {
            current = directionToGrow(current, direction);
            current = placeColumn(level, current, 3, random, 0.5D, 2);
            if (random.nextInt(5) == 0) {
                current = directionToGrow(current, direction);
                placeColumn(level, current, 3, random, 0.7D, 2);
            } else {
                current = directionToGrow(current, direction);
                placeColumn(level, current, 1, random, 0.7D, 1);
            }
        } else {
            current = directionToGrow(current, direction);
            placeColumn(level, current, 1, random, 0.5D, 1);
        }
    }

    /** 1 = north, 2 = east, 3 = south, anything else = west (the original {@code grow} mapping). */
    private static BlockPos directionToGrow(BlockPos pos, int choice) {
        return switch (choice) {
            case 1 -> pos.north();
            case 2 -> pos.east();
            case 3 -> pos.south();
            default -> pos.west();
        };
    }
}
