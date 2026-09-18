package alku.csrp.world.gen;

import alku.csrp.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of 1.10.9 {@code WorldGenParasiteBigBall}.
 *
 * <p>Places the {@code ballbig} template 11..16 blocks above the position (offset +6/+6 because the
 * template is 13x13) and then grows three of four huge trunk roots that snake downwards until they
 * bury themselves in solid ground.</p>
 */
public final class WorldGenParasiteBigBall {
    private static final int HEIGHT = 12;
    private static final Identifier TEMPLATE = Identifier.fromNamespaceAndPath("csrp", "ballbig");

    private WorldGenParasiteBigBall() {
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

        int extra = random.nextInt(6) + 2;
        // WorldGenCustomStructures.generateInPosition(..., offsetX 6, offsetY 0, offsetZ 6)
        StructurePlacer.place(level, TEMPLATE, position.above(9 + extra).offset(6, 0, 6), random);

        BlockPos center = new BlockPos(position.getX(), position.getY(), position.getZ());
        boolean skip = true;

        for (int z = 0; z <= 3; z++) {
            if (random.nextDouble() <= 0.15D && skip) {
                skip = false;
                continue;
            }
            BlockPos root = steps(center.above(12 + extra), z, 4);
            root = placeColumn(level, root, 3);
            int direction = random.nextInt(4);
            boolean grow = true;
            boolean side = false;
            int guard = 0;

            while (grow && guard++ < 512) {
                int length = random.nextInt(4) + 2;
                root = directionToGrow(root.below(), direction, side);
                if (random.nextDouble() <= 0.2D) {
                    BlockPos branch = directionToGrow(root, direction, !side);
                    placeColumn(level, directionToGrow(branch, direction, !side), length);
                }
                BlockPos end = placeColumn(level, root, length);
                BlockPos next = directionToGrow(end.below(), direction, !side);
                root = placeColumn(level, next, random.nextInt(2) + 2);
                side = !side;
                grow = ParasiteGenContext.isSolidGround(level, root);
                direction = random.nextInt(4);
            }
        }
        return true;
    }

    /**
     * {@code getDirectionRoot}: 0 = south, 1 = west, 2 = north, anything else = east.
     */
    private static BlockPos steps(BlockPos center, int direction, int times) {
        return switch (direction) {
            case 0 -> center.south(times);
            case 1 -> center.west(times);
            case 2 -> center.north(times);
            default -> center.east(times);
        };
    }

    /** The original {@code placeColumn} walked downwards and decremented its height first. */
    private static BlockPos placeColumn(ServerLevel level, BlockPos pos, int height) {
        int remaining = height - 1;
        BlockPos current = pos;
        while (remaining > 0) {
            placeTrunk(level, current);
            current = current.below();
            remaining--;
        }
        placeTrunk(level, current);
        return current;
    }

    private static void placeTrunk(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos,
                alku.csrp.registry.ModBlocks.PARASITETRUNK.get().defaultBlockState());
    }

    /** {@code directionToGrow}; {@code reverse} flips north/south and east/west. */
    private static BlockPos directionToGrow(BlockPos pos, int choice, boolean reverse) {
        int resolved = reverse ? (choice + 2) % 4 : choice;
        return ParasiteGenContext.horizontal(pos, resolved);
    }
}
