package alku.csrp.world.gen;

import alku.csrp.registry.ModBlocks;
import alku.csrp.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of 1.10.9 {@code WorldGenParasiteBall}.
 *
 * <p>Places the {@code ball} template 2..5 blocks above the position (offset +3/+3 because the
 * template is 7x7) and then grows up to three of four thin roots down to the floor, capped by a
 * 3x3 crown of thin wood.</p>
 */
public final class WorldGenParasiteBall {
    private static final int HEIGHT = 12;
    private static final Identifier TEMPLATE = Identifier.fromNamespaceAndPath("csrp", "ball");

    private WorldGenParasiteBall() {
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

        int extra = random.nextInt(4);
        // WorldGenCustomStructures.generateInPosition(..., offsetX 3, offsetY 0, offsetZ 3)
        BlockPos templatePos = position.above(2 + extra).offset(3, 0, 3);
        StructurePlacer.place(level, TEMPLATE, templatePos, random);

        BlockPos center = new BlockPos(position.getX(), position.getY(), position.getZ());
        boolean skip = true;

        for (int z = 1; z <= 4; z++) {
            if (random.nextDouble() <= 0.5D && skip) {
                skip = false;
                continue;
            }
            BlockPos root = steps(center.above(3 + extra), z, 3);
            placeThin(level, root);
            root = root.below();
            placeThin(level, root);
            root = steps(root, z, 1);
            placeThin(level, root);

            int guard = 0;
            while (!ParasiteGenContext.isSolidGround(level, root.below()) && guard++ < 512) {
                root = root.below();
                placeThin(level, root);
            }

            for (int j = 1; j <= 4; j++) {
                BlockPos side = steps(root, j, 1);
                if (!ParasiteGenContext.isSolidGround(level, side)) {
                    placeThin(level, side);
                }
            }
        }
        return true;
    }

    private static void placeThin(ServerLevel level, BlockPos pos) {
        BlockState state = ParasiteGenContext.isAir(level, pos)
                ? ModBlocks.PARASITETHIN.get().defaultBlockState()
                : ModBlocks.PARASITETRUNK.get().defaultBlockState();
        ParasiteGenContext.setBlock(level, pos, state);
    }

    /** {@code getDirectionRoot}: 1 = south, 2 = west, 3 = north, anything else = east. */
    private static BlockPos steps(BlockPos center, int direction, int times) {
        return switch (direction) {
            case 1 -> center.south(times);
            case 2 -> center.west(times);
            case 3 -> center.north(times);
            default -> center.east(times);
        };
    }
}
