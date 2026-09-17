package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenDeadheadTreeStructure}.
 *
 * <p>The 1.12.2 class extended {@code WorldGenerator} and placed {@code deadhead_tree_large_1..4}
 * templates through {@code TemplateManager}.  On 26.3 templates are placed through
 * {@link StructurePlacer}; the four sapling anchors from the original are kept so the tree lands on
 * the sapling that grew it, and the two optional modes are preserved:</p>
 * <ul>
 *   <li>{@code mushroomMode} — a tree that grows on top of deadhead leaves links its trunk down
 *       through the leaves to the canopy below.</li>
 *   <li>{@code rootMode} — the lowest trunk layer grows "hanging" roots downwards, sprouting
 *       deadhead leaves along the way.</li>
 * </ul>
 */
public final class DeadheadTreeGen {
    private static final int SOURCE_SEARCH_RADIUS = 6;
    private static final int LOWER_TRUNK_SEARCH_RADIUS = 5;
    private static final int LOWER_TRUNK_SEARCH_DEPTH = 96;
    private static final int ROOT_MAX_DEPTH = 48;
    private static final int ROOT_LEAF_START_DEPTH = 5;
    private static final float ROOT_WANDER_CHANCE = 0.22F;
    private static final float ROOT_LEAF_CHANCE = 0.28F;

    private static final Direction[] ROOT_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private record TreeTemplate(String name, BlockPos saplingAnchor) {
    }

    private static final TreeTemplate[] TREES = {
            new TreeTemplate("deadhead_tree_large_1", new BlockPos(4, 0, 5)),
            new TreeTemplate("deadhead_tree_large_2", new BlockPos(5, 0, 5)),
            new TreeTemplate("deadhead_tree_large_3", new BlockPos(4, 0, 5)),
            new TreeTemplate("deadhead_tree_large_4", new BlockPos(6, 0, 6))
    };

    private DeadheadTreeGen() {
    }

    /** Original {@code WorldGenDeadheadTreeStructure(boolean notify)} used by the deadhead sapling. */
    public static boolean grow(ServerLevel level, RandomSource random, BlockPos pos) {
        return grow(level, random, pos, false, false);
    }

    public static boolean grow(ServerLevel level, RandomSource random, BlockPos pos, boolean mushroomMode,
            boolean rootMode) {
        boolean stackingOnDeadheadLeaves = mushroomMode && isDeadheadLeaves(level.getBlockState(pos));
        TreeTemplate selected = TREES[random.nextInt(TREES.length)];
        // Rotation is deliberately not applied: the anchor alignment cannot be validated without
        // loading the template first, so the trees always place unrotated (the original rotated
        // them, which is a purely cosmetic difference).
        boolean placed = StructurePlacer.place(level,
                Identifier.fromNamespaceAndPath(Csrp.MODID, selected.name()),
                pos, random, selected.saplingAnchor(), Rotation.NONE);
        if (!placed) {
            return false;
        }

        if (rootMode && !mushroomMode) {
            growFloatingRoots(level, random, pos);
        }
        if (stackingOnDeadheadLeaves) {
            connectStackedTree(level, pos);
        }
        return true;
    }

    private static boolean isDeadheadLeaves(BlockState state) {
        return state.is(ModBlocks.DEADHEAD_LEAVES.get());
    }

    private static boolean isDeadheadTrunk(BlockState state) {
        // 1.12.2 tested the DEADHEAD metadata variant of the parasite trunk; the 26.3 port collapsed
        // every trunk variant into one id, so the block identity is the whole test.
        return state.is(ModBlocks.PARASITETRUNK.get());
    }

    private static BlockState deadheadTrunkState() {
        return ModBlocks.PARASITETRUNK.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
    }

    // ---------------------------------------------------------------------------------------------
    // Hang roots (rootMode)
    // ---------------------------------------------------------------------------------------------

    private static void growFloatingRoots(ServerLevel level, RandomSource random, BlockPos anchor) {
        // The original scanned the placed template's bounding box for its lowest trunk layer.  The
        // modern template manager does not hand back that bounding box here, so the search is redone
        // around the sapling instead: find the lowest trunk block in a vertical column search
        // starting just under the sapling.
        BlockPos start = findLowestTrunkLayer(level, anchor);
        if (start != null) {
            growSingleRoot(level, random, start);
        }
    }

    private static BlockPos findLowestTrunkLayer(ServerLevel level, BlockPos anchor) {
        for (int y = anchor.getY(); y >= Math.max(1, anchor.getY() - 64); y--) {
            for (int x = anchor.getX() - SOURCE_SEARCH_RADIUS; x <= anchor.getX() + SOURCE_SEARCH_RADIUS; x++) {
                for (int z = anchor.getZ() - SOURCE_SEARCH_RADIUS; z <= anchor.getZ() + SOURCE_SEARCH_RADIUS; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isDeadheadTrunk(level.getBlockState(pos))) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static void growSingleRoot(ServerLevel level, RandomSource random, BlockPos start) {
        BlockPos cursor = start;
        List<BlockPos> rootBlocks = new ArrayList<>();
        int verticalDepth = 0;

        for (int i = 0; i < ROOT_MAX_DEPTH; i++) {
            BlockPos below = cursor.below();
            if (isRootGround(level, below) || !canRootReplace(level, below)) {
                break;
            }
            placeRootTrunk(level, below);
            rootBlocks.add(below);
            cursor = below;
            verticalDepth++;
            if (random.nextFloat() < ROOT_WANDER_CHANCE) {
                Direction direction = ROOT_DIRECTIONS[random.nextInt(ROOT_DIRECTIONS.length)];
                BlockPos side = cursor.relative(direction);
                if (canRootReplace(level, side) && !isRootGround(level, side.below())) {
                    placeRootTrunk(level, side);
                    rootBlocks.add(side);
                    cursor = side;
                }
            }
        }

        if (verticalDepth > ROOT_LEAF_START_DEPTH) {
            addRootLeaves(level, random, rootBlocks);
        }
    }

    private static void addRootLeaves(ServerLevel level, RandomSource random, List<BlockPos> rootBlocks) {
        if (rootBlocks.size() <= ROOT_LEAF_START_DEPTH) {
            return;
        }
        BlockState leaves = ModBlocks.DEADHEAD_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true);
        List<BlockPos> snapshot = new ArrayList<>(rootBlocks);
        for (int i = ROOT_LEAF_START_DEPTH; i < snapshot.size(); i++) {
            if (random.nextFloat() >= ROOT_LEAF_CHANCE) {
                continue;
            }
            BlockPos rootPos = snapshot.get(i);
            int attempts = 1 + random.nextInt(2);
            for (int j = 0; j < attempts; j++) {
                Direction direction = ROOT_DIRECTIONS[random.nextInt(ROOT_DIRECTIONS.length)];
                BlockPos leafPos = rootPos.relative(direction);
                if (canPlaceRootLeaf(level, leafPos)) {
                    level.setBlock(leafPos, leaves, 2);
                }
            }
        }
    }

    private static boolean canPlaceRootLeaf(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinY() || pos.getY() >= level.getMaxY()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (isDeadheadTrunk(state)) {
            return false;
        }
        if (state.getBlock() instanceof LeavesBlock) {
            return false;
        }
        return state.isAir() || state.canBeReplaced();
    }

    private static boolean isRootGround(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinY()) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (isDeadheadTrunk(state)) {
            return true;
        }
        if (state.getBlock() instanceof LeavesBlock) {
            return false;
        }
        if (state.isAir() || state.canBeReplaced()) {
            return false;
        }
        return state.getFluidState().isEmpty();
    }

    private static boolean canRootReplace(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinY() || pos.getY() >= level.getMaxY()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        return state.getBlock() instanceof LeavesBlock || state.canBeReplaced();
    }

    private static void placeRootTrunk(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, deadheadTrunkState(), 2);
    }

    // ---------------------------------------------------------------------------------------------
    // Stacked trees (mushroomMode)
    // ---------------------------------------------------------------------------------------------

    /**
     * Original {@code connectStackedTree}: when a tree is grown on an existing deadhead canopy, walk
     * down from the new trunk to the lower canopy trunk and fill the gap with deadhead wood, which
     * produces the mushroom-like stacked silhouette.
     */
    private static void connectStackedTree(ServerLevel level, BlockPos anchor) {
        BlockPos upperTrunk = findLowestUpperTrunk(level, anchor);
        if (upperTrunk == null) {
            return;
        }
        BlockPos lowerTrunk = findHighestLowerTrunk(level, upperTrunk);
        if (lowerTrunk == null) {
            return;
        }
        List<BlockPos> path = buildDownwardPath(upperTrunk, lowerTrunk);
        if (!canCarveConnection(level, path, lowerTrunk)) {
            return;
        }
        BlockState trunkState = deadheadTrunkState();
        for (BlockPos pos : path) {
            if (!isDeadheadTrunk(level.getBlockState(pos))) {
                level.setBlock(pos, trunkState, 2);
            }
        }
    }

    private static BlockPos findLowestUpperTrunk(ServerLevel level, BlockPos anchor) {
        int minX = anchor.getX() - SOURCE_SEARCH_RADIUS;
        int maxX = anchor.getX() + SOURCE_SEARCH_RADIUS;
        int minZ = anchor.getZ() - SOURCE_SEARCH_RADIUS;
        int maxZ = anchor.getZ() + SOURCE_SEARCH_RADIUS;

        for (int y = anchor.getY(); y >= Math.max(level.getMinY(), anchor.getY() - 64); y--) {
            BlockPos best = null;
            long bestDistance = Long.MAX_VALUE;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!isDeadheadTrunk(level.getBlockState(pos))) {
                        continue;
                    }
                    long dx = x - anchor.getX();
                    long dz = z - anchor.getZ();
                    long distance = dx * dx + dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private static BlockPos findHighestLowerTrunk(ServerLevel level, BlockPos upperTrunk) {
        int minY = Math.max(level.getMinY() + 1, upperTrunk.getY() - LOWER_TRUNK_SEARCH_DEPTH);
        for (int y = upperTrunk.getY() - 1; y >= minY; y--) {
            BlockPos best = null;
            long bestDistance = Long.MAX_VALUE;
            for (int x = upperTrunk.getX() - LOWER_TRUNK_SEARCH_RADIUS;
                    x <= upperTrunk.getX() + LOWER_TRUNK_SEARCH_RADIUS; x++) {
                for (int z = upperTrunk.getZ() - LOWER_TRUNK_SEARCH_RADIUS;
                        z <= upperTrunk.getZ() + LOWER_TRUNK_SEARCH_RADIUS; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!isDeadheadTrunk(level.getBlockState(pos))) {
                        continue;
                    }
                    long dx = x - upperTrunk.getX();
                    long dz = z - upperTrunk.getZ();
                    long distance = dx * dx + dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private static List<BlockPos> buildDownwardPath(BlockPos upper, BlockPos lower) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = upper;
        int totalDrop = Math.max(1, upper.getY() - lower.getY());

        int step = 1;
        while (cursor.getY() > lower.getY()) {
            cursor = cursor.below();
            addUnique(path, cursor);
            double progress = Math.min(1.0D, (double) step / totalDrop);
            int desiredX = (int) Math.round(upper.getX() + (lower.getX() - upper.getX()) * progress);
            int desiredZ = (int) Math.round(upper.getZ() + (lower.getZ() - upper.getZ()) * progress);
            while (cursor.getX() != desiredX) {
                cursor = cursor.offset(Integer.compare(desiredX, cursor.getX()), 0, 0);
                addUnique(path, cursor);
            }
            while (cursor.getZ() != desiredZ) {
                cursor = cursor.offset(0, 0, Integer.compare(desiredZ, cursor.getZ()));
                addUnique(path, cursor);
            }
            step++;
        }
        while (cursor.getX() != lower.getX()) {
            cursor = cursor.offset(Integer.compare(lower.getX(), cursor.getX()), 0, 0);
            addUnique(path, cursor);
        }
        while (cursor.getZ() != lower.getZ()) {
            cursor = cursor.offset(0, 0, Integer.compare(lower.getZ(), cursor.getZ()));
            addUnique(path, cursor);
        }
        return path;
    }

    private static boolean canCarveConnection(ServerLevel level, List<BlockPos> path, BlockPos targetTrunk) {
        for (BlockPos pos : path) {
            BlockState state = level.getBlockState(pos);
            if (pos.equals(targetTrunk) || isDeadheadTrunk(state) || state.isAir()
                    || state.getBlock() instanceof LeavesBlock || state.canBeReplaced()) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static void addUnique(List<BlockPos> path, BlockPos pos) {
        if (path.isEmpty() || !path.get(path.size() - 1).equals(pos)) {
            path.add(pos);
        }
    }
}
