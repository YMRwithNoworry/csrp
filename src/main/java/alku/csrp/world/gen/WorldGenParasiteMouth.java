package alku.csrp.world.gen;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Port of 1.10.9 {@code WorldGenParasiteMouth}.
 *
 * <p>Writes a "mouth" mound at the position and then, if the floor scan finds a spot up to seven
 * blocks away, a second one.  Each mound bails out unless its 5x5x3 envelope is entirely air,
 * carves an air pocket downwards while laying a 5-wide "flesh" perimeter, and caps the bottom with
 * flesh stain.</p>
 */
public final class WorldGenParasiteMouth {
    private WorldGenParasiteMouth() {
    }

    public static boolean generate(ServerLevel level, RandomSource random, BlockPos position) {
        generateMouth(level, random, position);
        int offset = random.nextInt(7) + 1;
        if (random.nextBoolean()) {
            offset *= -1;
        }
        int x = offset + position.getX();
        int z = position.getZ();
        if (x == 7) {
            int second = random.nextInt(7) + 1;
            if (random.nextBoolean()) {
                second *= -1;
            }
            z += second;
        } else {
            int fixed = 7;
            if (random.nextBoolean()) {
                fixed *= -1;
            }
            z += fixed;
        }

        BlockPos floor = ParasiteFloorScan.getFloor(level, new BlockPos(x, position.getY(), z), 5);
        if (floor != null) {
            generateMouth(level, random, floor);
        }
        return true;
    }

    private static void generateMouth(ServerLevel level, RandomSource random, BlockPos position) {
        for (int yyy = 0; yyy <= 2; yyy++) {
            for (int xs = -2; xs <= 2; xs++) {
                for (int zs = -2; zs <= 2; zs++) {
                    BlockPos probe = new BlockPos(position.getX() + xs, position.getY() + yyy,
                            position.getZ() + zs);
                    if (!ParasiteGenContext.get(level, probe).isAir()) {
                        return;
                    }
                }
            }
        }

        perimeter(level, position, 1, 2);
        perimeter(level, position, 2, 1);
        BlockPos current = position;
        int depth = 7;

        int guard = 0;
        while ((!ParasiteGenContext.isSolidGround(level, current.below()) || depth >= 0)
                && current.below().getY() > 2 && guard++ < 512) {
            depth--;
            ParasiteGenContext.setAir(level, current);
            current = current.below();
            perimeter(level, current, 5, 1);
        }

        placeFlesh(level, current);
    }

    private static void perimeter(ServerLevel level, BlockPos position, int type, int area) {
        for (int i = 0; i <= 3; i++) {
            BlockPos helper = steps(position, i, area);
            placeFleshColumn(level, helper.below(), 2);
            if (type == 5) {
                placeFlesh(level, helper);
            } else if (type == 1) {
                placeFleshStair(level, helper, i, true, 0);
            } else {
                placeMouth(level, helper);
            }

            BlockPos anchor = helper;
            for (int o = 0; o < 2; o++) {
                BlockPos side = o == 0
                        ? ParasiteGenContext.horizontal(anchor, (i + 1) % 4)
                        : ParasiteGenContext.horizontal(anchor, (i + 3) % 4);
                placeFleshColumn(level, side.below(), 2);
                if (type == 5) {
                    placeFlesh(level, side);
                } else if (type == 1) {
                    placeFleshStair(level, side, i, true, 0);
                } else {
                    placeMouth(level, side);
                }
            }
        }
    }

    private static void placeMouth(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ModBlocks.PARASITE_MOUTH.get().defaultBlockState());
    }

    private static void placeFlesh(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.STAIN_FLESH);
    }

    /**
     * {@code placeFleshStair}: the offset axis selects the stair facing (0 = south, 1 = west,
     * 2 = north, 3 = east) and a bottom stair also pulls a 2-block flesh column below it.
     */
    private static void placeFleshStair(ServerLevel level, BlockPos pos, int direction, boolean bottom,
            int times) {
        BlockPos target = steps(pos, direction, times);
        Direction facing = switch (Math.floorMod(direction, 4)) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
        BlockState stair = ModBlocks.legacyBlock("parasitestain_fleshstairs").get()
                .defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.HALF, bottom ? Half.BOTTOM : Half.TOP);
        ParasiteGenContext.setBlock(level, target, stair);
        if (bottom) {
            placeFleshColumn(level, target.below(), 2);
        }
    }

    /** The original {@code placeColumn} wrote {@code height} flesh blocks downwards. */
    private static void placeFleshColumn(ServerLevel level, BlockPos pos, int height) {
        BlockPos current = pos;
        int currentY = pos.getY();
        int remaining = height;
        while (remaining > 0 && currentY > 2) {
            currentY--;
            placeFlesh(level, current);
            current = current.below();
            remaining--;
        }
    }

    /** {@code getDirectionRoot}: 0 = south, 1 = west, 2 = north, anything else = east. */
    private static BlockPos steps(BlockPos center, int direction, int times) {
        return switch (direction) {
            case 0 -> center.south(times);
            case 1 -> center.west(times);
            case 2 -> center.north(times);
            default -> center.east(times);
        };
    }
}
