package alku.csrp.world.gen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Port of 1.10.9 {@code WorldGenParasiteSpine}.
 *
 * <p>A 28-block-tall pair of "spine" threads of {@code parasiterubble} that bury into the ground,
 * then climb in mirror steps: each stage places two columns, a top slab under the earlier step and
 * the stair/slab ornaments of the original {@code placeStair} helper.</p>
 *
 * <p>The original wrote out each of the five ascending stages by hand with the same five calls.  The
 * port keeps that exact call order — one loop over the per-stage slab budgets, with every stage's
 * {@code randomG} pre-rolled so the {@code RandomSource} call order still matches the 1.10.9 code.</p>
 */
public final class WorldGenParasiteSpine {
    private static final int HEIGHT = 28;
    /** Per stage: slab budget, {@code randomG} re-roll bound, {@code randomG} re-roll minimum. */
    private static final int[][] STAGES = {
            {14, 4, 3},
            {7, 5, 4},
            {7, 5, 4},
            {2, 5, 4},
            {0, 5, 4}
    };

    private WorldGenParasiteSpine() {
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

        int partner = random.nextInt(4);
        int atmm = (partner + 1) % 4;

        // Bury the two threads: every step writes both threads plus a 4-way rubble collar.
        BlockPos current = position;
        BlockPos second = horizontal(current, partner);
        int guard = 0;
        while (!ParasiteGenContext.isSolidGround(level, current.below())
                && current.below().getY() >= 1 && guard++ < 512) {
            current = current.below();
            second = second.below();
            placeTrunk(level, current);
            placeTrunk(level, second);
            for (int z = 0; z <= 3; z++) {
                placeTrunk(level, horizontal(current, z));
                placeTrunk(level, horizontal(second, z));
            }
        }

        current = position;
        second = horizontal(current, partner);

        placeStair(level, current, atmm, true, 1, false);
        current = placeColumn(level, current, 4, partner, 27, true);
        placeStair(level, current, atmm, true, 0, false);
        placeColumn(level, horizontal(position, (atmm + 2) % 4), 5, partner, -1, false);
        placeStair(level, second, atmm, true, 1, false);
        second = placeColumn(level, second, 4, (partner + 2) % 4, 27, true);
        placeStair(level, second, atmm, true, 0, false);
        placeColumn(level, horizontal(horizontal(position, partner), (atmm + 2) % 4), 5, partner, -1, false);
        placeStair(level, second.below(4), partner, true, 1, false);
        placeStair(level, current.below(4), (partner + 2) % 4, true, 1, false);

        List<SpineThread> threads = new ArrayList<>();
        threads.add(new SpineThread(current, partner));
        threads.add(new SpineThread(second, (partner + 2) % 4));

        // The original re-rolled randomG once per stage, before any of that stage's column writes;
        // pre-rolling keeps the RandomSource call order identical to the 1.10.9 code.
        int[] heights = new int[STAGES.length];
        heights[0] = random.nextInt(4) + 3;
        for (int stage = 1; stage < STAGES.length; stage++) {
            heights[stage] = random.nextInt(STAGES[stage][1]) + STAGES[stage][2];
        }

        for (int stage = 0; stage < STAGES.length; stage++) {
            int slabs = STAGES[stage][0];

            for (SpineThread thread : threads) {
                BlockPos point = stepUpAndSide(thread.pos(), atmm);
                if (stage > 0) {
                    placeStair(level, point.below(), (atmm + 2) % 4, false, 0, false);
                }
                point = placeColumn(level, point, heights[stage], thread.direction(), slabs, true);
                placeStair(level, point, atmm, true, 0, false);
                thread.move(point);
            }
        }
        return true;
    }

    /** Mutable cursor for one of the two spine threads. */
    private static final class SpineThread {
        private BlockPos pos;
        private final int direction;

        private SpineThread(BlockPos pos, int direction) {
            this.pos = pos;
            this.direction = direction;
        }

        BlockPos pos() {
            return pos;
        }

        int direction() {
            return direction;
        }

        void move(BlockPos next) {
            this.pos = next;
        }
    }

    /** {@code directionToGrow(pos.above(), dir, true)} — up one, then two diagonal steps. */
    private static BlockPos stepUpAndSide(BlockPos pos, int direction) {
        return ParasiteGenContext.sideCurse(pos.above(), direction);
    }

    private static void placeTrunk(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos,
                alku.csrp.registry.ModBlocks.legacyBlock("parasiterubble").get().defaultBlockState());
    }

    private static BlockPos placeColumn(ServerLevel level, BlockPos pos, int height, int direction, int slabs,
            boolean stair) {
        BlockPos current = pos;
        int times = 0;
        while (times < height) {
            if (times % 2 == 0 && stair) {
                if (slabs > 20) {
                    placeStair(level, current, direction, false, 2, false);
                    slabs -= 10;
                } else if (slabs > 10) {
                    slabs -= 10;
                    placeStair(level, current, direction, false, 2, false);
                    placeStair(level, current.above(), (direction + 2) % 4, true, -2, false);
                    placeStair(level, current.above(), direction, false, 3, slabs > 5);
                } else {
                    placeStair(level, current, direction, false, 2, slabs > 0);
                    slabs--;
                }
            }
            placeTrunk(level, current);
            current = current.above();
            times++;
        }
        return current;
    }

    /**
     * {@code placeStair(world, pos, direction, bottom, times, slab)}.
     *
     * <p>{@code direction} selects the offset axis (0 = south, 1 = west, 2 = north, 3 = east) and the
     * stair facing is {@code direction - 2} as in the original: 0/1/2/3 -> south/west/north/east.</p>
     */
    private static void placeStair(ServerLevel level, BlockPos pos, int direction, boolean bottom, int times,
            boolean slab) {
        BlockPos target = steps(pos, direction, times);
        BlockState stair = stairState(direction, bottom);
        ParasiteGenContext.setBlock(level, target, stair);
        if (!bottom && slab) {
            ParasiteGenContext.setBlock(level, steps(pos, direction, times + 1), slabState());
        }
    }

    private static BlockState stairState(int direction, boolean bottom) {
        Direction facing = switch (Math.floorMod(direction, 4)) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
        return alku.csrp.registry.ModBlocks.legacyBlock("parasiterubblebonestairs").get()
                .defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.HALF, bottom ? Half.BOTTOM : Half.TOP);
    }

    private static BlockState slabState() {
        return alku.csrp.registry.ModBlocks.PARASITERUBBLESLABHALF_BONE.get().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.TOP);
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

    private static BlockPos horizontal(BlockPos pos, int choice) {
        return switch (Math.floorMod(choice, 4)) {
            case 0 -> pos.north();
            case 1 -> pos.east();
            case 2 -> pos.south();
            default -> pos.west();
        };
    }
}
