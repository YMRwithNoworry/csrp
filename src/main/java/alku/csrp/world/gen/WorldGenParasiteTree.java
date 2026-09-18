package alku.csrp.world.gen;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of 1.10.9 {@code WorldGenParasiteTree}.
 *
 * <p>A 20-block-tall parasite tree: two leaning trunks that are walked down to the ground, three
 * columns of up to 7 trunk blocks with a 30% early exit each, and branches that sprout from the
 * third layer of every grown column.  Canisters are placed underneath a column when the block below
 * is air (30% + {@code extraChance}).</p>
 */
public final class WorldGenParasiteTree {
    private static final int HEIGHT = 20;

    private WorldGenParasiteTree() {
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

        // Two leaning trunks: walk each down to the ground, filling the column on the way.
        BlockPos current = directionToGrow(position, random.nextInt(4), false);
        placeTrunk(level, current);
        while (!ParasiteGenContext.isSolidGround(level, current.below())
                && current.below().getY() >= 1) {
            current = current.below();
            placeTrunk(level, current);
        }

        int first = random.nextInt(4);
        current = directionToGrow(position, (first + 1), false);
        placeTrunk(level, current);
        while (!ParasiteGenContext.isSolidGround(level, current.below())
                && current.below().getY() >= 1) {
            current = current.below();
            placeTrunk(level, current);
        }

        current = position;
        while (!ParasiteGenContext.isSolidGround(level, current.below())
                && current.below().getY() >= 1) {
            current = current.below();
            placeTrunk(level, current);
        }

        // The original re-rolled "first" here; the value is only used for the first column offset.
        current = position;
        current = placeColumn(level, current, random.nextInt(3) + 5, random, -1.0D);
        first = random.nextInt(4);
        current = directionToGrow(current, first, false);
        current = placeColumn(level, current, random.nextInt(3) + 5, random, 0.3D);
        if (random.nextDouble() <= 0.3D) {
            return true;
        }

        current = directionToGrow(current, random.nextInt(4), false);
        current = placeColumn(level, current, random.nextInt(3) + 5, random, 0.3D);
        if (random.nextDouble() <= 0.3D) {
            return true;
        }

        current = directionToGrow(current, random.nextInt(4), false);
        placeColumn(level, current, random.nextInt(3) + 5, random, 0.3D);
        return true;
    }

    private static void placeTrunk(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ModBlocks.PARASITETRUNK.get().defaultBlockState());
    }

    /** The 1.10.9 canister variant ({@code SAC}/{@code LUMP}) maps onto the single modern id. */
    private static void placeCanister(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ModBlocks.CANISTER_ACTIVE.get().defaultBlockState());
    }

    private static BlockPos placeColumn(ServerLevel level, BlockPos pos, int height, RandomSource random,
            double extraChance) {
        int current = pos.getY();
        int limit = current;
        int times = 0;
        BlockPos newPos = pos;

        if (ParasiteGenContext.get(level, pos.below()).isAir()
                && random.nextDouble() <= 0.3D + extraChance) {
            placeCanister(level, pos.below());
        }

        while (current < limit + height) {
            if (times == 2 && extraChance != -1.0D) {
                int one = random.nextInt(4);
                int two = random.nextInt(4);
                if (one == two) {
                    one++;
                }
                addBranches(level, newPos, random, random.nextInt(3) + 1, one);
                addBranches(level, newPos, random, random.nextInt(3) + 1, two);
            }
            placeTrunk(level, newPos);
            newPos = newPos.above();
            current++;
            times++;
        }

        placeTrunk(level, newPos);
        return newPos;
    }

    private static void addBranches(ServerLevel level, BlockPos pos, RandomSource random, int size,
            int direction) {
        BlockPos branch = directionToGrow(pos, direction, false);
        placeTrunk(level, branch);
        branch = branch.above();
        branch = directionToGrow(branch, direction, false);
        int current = 0;
        int curve = direction * 10 + random.nextInt(2);

        while (current < size) {
            placeTrunk(level, branch);
            if (random.nextDouble() <= 0.25D) {
                placeCanister(level, branch.below());
            }
            if (random.nextDouble() <= 0.5D) {
                branch = directionToGrow(branch, direction, false);
            } else {
                branch = directionToGrow(branch, curve, true);
            }
            current++;
        }

        branch = branch.below();
        placeTrunk(level, branch);
        if (random.nextDouble() <= 0.25D) {
            placeCanister(level, branch.below());
        }

        if (random.nextDouble() <= 0.5D) {
            if (random.nextDouble() <= 0.5D) {
                branch = directionToGrow(branch.below(), direction, false);
            } else {
                branch = directionToGrow(branch.below(), curve, true);
            }
            placeTrunk(level, branch);
            if (random.nextDouble() <= 0.25D) {
                placeCanister(level, branch.below());
            }
        }
    }

    /** {@code directionToGrow}; 0 = north, 1 = east, 2 = south, 3 = west for the plain form. */
    private static BlockPos directionToGrow(BlockPos pos, int choice, boolean sideCurse) {
        if (sideCurse) {
            return ParasiteGenContext.sideCurse(pos, choice);
        }
        return ParasiteGenContext.horizontal(pos, choice);
    }
}
