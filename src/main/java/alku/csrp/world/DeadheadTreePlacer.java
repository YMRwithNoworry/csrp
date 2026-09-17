package alku.csrp.world;

import alku.csrp.registry.ModBlocks;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

/**
 * Places the four {@code csrp:deadhead_tree_large_*} NBT structures, ported from SRParasites
 * 1.10.9 {@code world/gen/feature/WorldGenDeadheadTreeStructure}.
 *
 * <p>Deliberately lives in {@code alku.csrp.world} so it can reuse the package-private
 * {@link MeteorStructureLoader#load(ServerLevel, String)} path (which also rewrites the legacy
 * {@code srparasites:} ids inside the NBT payload).
 *
 * <p>1.12.2 {@code Template.func_186266_a} has no 1.20.1 equivalent; the verified substitute is
 * {@code StructureTemplate.calculateRelativePosition(StructurePlaceSettings, BlockPos)} (javap on the
 * official mapped jar), which applies mirror+rotation and clamps the result into the template's
 * bounds exactly like the {@code TemplateTemplate} helper did.
 */
public final class DeadheadTreePlacer {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** 1.10.9 {@code SOURCE_SEARCH_RADIUS}. */
    private static final int SOURCE_SEARCH_RADIUS = 6;
    /** 1.10.9 {@code ROOT_MAX_DEPTH}. */
    private static final int ROOT_MAX_DEPTH = 48;
    /** 1.10.9 {@code ROOT_LEAF_START_DEPTH}. */
    private static final int ROOT_LEAF_START_DEPTH = 5;
    /** 1.10.9 {@code ROOT_WANDER_CHANCE}. */
    private static final float ROOT_WANDER_CHANCE = 0.22F;
    /** 1.10.9 {@code ROOT_LEAF_CHANCE}. */
    private static final float ROOT_LEAF_CHANCE = 0.28F;
    /** 1.10.9 {@code LOWER_TRUNK_SEARCH_DEPTH}. */
    private static final int LOWER_TRUNK_SEARCH_DEPTH = 96;
    /** 1.10.9 {@code LOWER_TRUNK_SEARCH_RADIUS}. */
    private static final int LOWER_TRUNK_SEARCH_RADIUS = 5;
    private static final Direction[] ROOT_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
    /** 1.10.9 {@code TREES}: name + sapling anchor, in declaration order. */
    private static final TreeTemplate[] TREES = {
            new TreeTemplate("deadhead_tree_large_1", new BlockPos(4, 0, 5)),
            new TreeTemplate("deadhead_tree_large_2", new BlockPos(5, 0, 5)),
            new TreeTemplate("deadhead_tree_large_3", new BlockPos(4, 0, 5)),
            new TreeTemplate("deadhead_tree_large_4", new BlockPos(6, 0, 6))
    };

    private DeadheadTreePlacer() {
    }

    /**
     * 1.10.9 {@code WorldGenDeadheadTreeStructure.generate}.
     *
     * @param position     the "sapling" position (the anchor is subtracted from it)
     * @param mushroomMode 1.10.9 {@code mushroomMode}: stack onto deadhead leaves when the anchor
     *                     already sits on them, and skip the floating-root pass
     * @param rootMode     1.10.9 {@code rootMode}: grow downward roots below the lowest trunk layer
     * @return true when a structure was placed
     */
    public static boolean place(ServerLevel level, BlockPos position, RandomSource random,
            boolean mushroomMode, boolean rootMode) {
        if (level.getServer() == null) {
            return false;
        }
        boolean stackingOnDeadheadLeaves = mushroomMode
                && ColdStarTreeHandler.isDeadheadLeaves(level.getBlockState(position));
        TreeTemplate selected = TREES[random.nextInt(TREES.length)];
        Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        StructureTemplate template = MeteorStructureLoader.load(level, selected.name());
        if (template == null) {
            LOGGER.error("Missing deadhead structure structures/{}.nbt", selected.name());
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setIgnoreEntities(true)
                .setKeepLiquids(true)
                .setRandom(random);
        BlockPos origin = position.offset(
                StructureTemplate.calculateRelativePosition(settings, selected.anchor()));
        BlockPos[] bounds = transformedBounds(template, rotation, origin);
        BlockPos min = bounds[0];
        BlockPos max = bounds[1];
        if (min.getY() < level.getMinBuildHeight() || max.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        if (!level.hasChunksAt(min, max)) {
            return false;
        }

        List<PreservedBlock> preservedIce = capturePreservedIce(level, min, max);
        template.placeInWorld(level, origin, origin, settings, random, Block.UPDATE_CLIENTS);
        restorePreservedIce(level, preservedIce);
        if (rootMode && !mushroomMode) {
            growFloatingRoots(level, random, min, max);
        }
        if (stackingOnDeadheadLeaves) {
            connectStackedTree(level, position, min, max);
        }
        return true;
    }

    /** 1.10.9 {@code getTransformedBounds} over the template's eight corners. */
    private static BlockPos[] transformedBounds(StructureTemplate template, Rotation rotation,
            BlockPos origin) {
        var size = template.getSize();
        int x1 = Math.max(0, size.getX() - 1);
        int y1 = Math.max(0, size.getY() - 1);
        int z1 = Math.max(0, size.getZ() - 1);
        BlockPos[] corners = {
                new BlockPos(0, 0, 0), new BlockPos(x1, 0, 0), new BlockPos(0, y1, 0),
                new BlockPos(0, 0, z1), new BlockPos(x1, y1, 0), new BlockPos(x1, 0, z1),
                new BlockPos(0, y1, z1), new BlockPos(x1, y1, z1)
        };
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos corner : corners) {
            BlockPos transformed = StructureTemplate.transform(corner, Mirror.NONE, rotation, BlockPos.ZERO)
                    .offset(origin);
            minX = Math.min(minX, transformed.getX());
            minY = Math.min(minY, transformed.getY());
            minZ = Math.min(minZ, transformed.getZ());
            maxX = Math.max(maxX, transformed.getX());
            maxY = Math.max(maxY, transformed.getY());
            maxZ = Math.max(maxZ, transformed.getZ());
        }
        return new BlockPos[] { new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ) };
    }

    /** 1.10.9 {@code capturePreservedIce}. */
    private static List<PreservedBlock> capturePreservedIce(ServerLevel level, BlockPos min,
            BlockPos max) {
        List<PreservedBlock> preserved = new ArrayList<>();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (isPreservedIce(state)) {
                        preserved.add(new PreservedBlock(pos, state));
                    }
                }
            }
        }
        return preserved;
    }

    /** 1.10.9 {@code restorePreservedIce}. */
    private static void restorePreservedIce(ServerLevel level, List<PreservedBlock> preserved) {
        for (PreservedBlock entry : preserved) {
            level.setBlock(entry.pos(), entry.state(), 2);
        }
    }

    /** 1.10.9 {@code isPreservedIce}. */
    private static boolean isPreservedIce(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
    }

    /** 1.10.9 {@code growFloatingRoots}. */
    private static void growFloatingRoots(ServerLevel level, RandomSource random, BlockPos min,
            BlockPos max) {
        for (BlockPos start : findLowestTrunkLayer(level, min, max)) {
            growSingleRoot(level, random, start);
        }
    }

    /** 1.10.9 {@code findLowestTrunkLayer}: scan layer by layer, stopping at the first hit. */
    private static List<BlockPos> findLowestTrunkLayer(ServerLevel level, BlockPos min, BlockPos max) {
        List<BlockPos> result = new ArrayList<>();
        for (int y = min.getY(); y <= max.getY(); y++) {
            result.clear();
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (ColdStarTreeHandler.isDeadheadTrunk(level.getBlockState(pos))) {
                        result.add(pos);
                    }
                }
            }
            if (!result.isEmpty()) {
                return new ArrayList<>(result);
            }
        }
        return result;
    }

    /** 1.10.9 {@code growSingleRoot}. */
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

    /** 1.10.9 {@code addRootLeaves}. */
    private static void addRootLeaves(ServerLevel level, RandomSource random, List<BlockPos> rootBlocks) {
        if (rootBlocks.size() <= ROOT_LEAF_START_DEPTH) {
            return;
        }
        BlockState leaves = ModBlocks.DEADHEAD_LEAVES.get().defaultBlockState();
        for (int i = ROOT_LEAF_START_DEPTH; i < rootBlocks.size(); i++) {
            if (random.nextFloat() >= ROOT_LEAF_CHANCE) {
                continue;
            }
            BlockPos rootPos = rootBlocks.get(i);
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

    /** 1.10.9 {@code canPlaceRootLeaf}. */
    private static boolean canPlaceRootLeaf(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (ColdStarTreeHandler.isDeadheadTrunk(state) || state.is(BlockTags.LEAVES)) {
            return false;
        }
        return level.isEmptyBlock(pos) || state.canBeReplaced();
    }

    /** 1.10.9 {@code isRootGround}. */
    private static boolean isRootGround(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight()) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (ColdStarTreeHandler.isDeadheadTrunk(state)) {
            return true;
        }
        if (state.is(BlockTags.LEAVES)) {
            return false;
        }
        if (level.isEmptyBlock(pos) || state.canBeReplaced()) {
            return false;
        }
        return state.blocksMotion() || state.liquid();
    }

    /** 1.10.9 {@code canRootReplace}. */
    private static boolean canRootReplace(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (level.isEmptyBlock(pos)) {
            return true;
        }
        return state.is(BlockTags.LEAVES) || state.canBeReplaced();
    }

    /** 1.10.9 {@code placeRootTrunk}. */
    private static void placeRootTrunk(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ColdStarTreeHandler.deadheadTrunkState(), 2);
    }

    /** 1.10.9 {@code connectStackedTree}. */
    private static void connectStackedTree(ServerLevel level, BlockPos anchor, BlockPos min, BlockPos max) {
        BlockPos upperTrunk = findLowestUpperTrunk(level, anchor, min, max);
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
        BlockState trunkState = ColdStarTreeHandler.deadheadTrunkState();
        for (BlockPos pos : path) {
            if (!ColdStarTreeHandler.isDeadheadTrunk(level.getBlockState(pos))) {
                level.setBlock(pos, trunkState, 2);
            }
        }
    }

    /** 1.10.9 {@code findLowestUpperTrunk}. */
    private static BlockPos findLowestUpperTrunk(ServerLevel level, BlockPos anchor, BlockPos min,
            BlockPos max) {
        int minX = Math.max(min.getX(), anchor.getX() - SOURCE_SEARCH_RADIUS);
        int maxX = Math.min(max.getX(), anchor.getX() + SOURCE_SEARCH_RADIUS);
        int minZ = Math.max(min.getZ(), anchor.getZ() - SOURCE_SEARCH_RADIUS);
        int maxZ = Math.min(max.getZ(), anchor.getZ() + SOURCE_SEARCH_RADIUS);
        int minY = Math.max(min.getY(), anchor.getY());
        int maxY = max.getY();
        for (int y = minY; y <= maxY; y++) {
            BlockPos bestAtY = null;
            long bestAtYDistance = Long.MAX_VALUE;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!ColdStarTreeHandler.isDeadheadTrunk(level.getBlockState(pos))) {
                        continue;
                    }
                    long dx = x - anchor.getX();
                    long dz = z - anchor.getZ();
                    long distance = dx * dx + dz * dz;
                    if (distance < bestAtYDistance) {
                        bestAtYDistance = distance;
                        bestAtY = pos;
                    }
                }
            }
            if (bestAtY != null) {
                return bestAtY;
            }
        }
        return null;
    }

    /** 1.10.9 {@code findHighestLowerTrunk}. */
    private static BlockPos findHighestLowerTrunk(ServerLevel level, BlockPos upperTrunk) {
        int radius = LOWER_TRUNK_SEARCH_RADIUS;
        int minY = Math.max(level.getMinBuildHeight(), upperTrunk.getY() - LOWER_TRUNK_SEARCH_DEPTH);
        for (int y = upperTrunk.getY() - 1; y >= minY; y--) {
            BlockPos bestAtY = null;
            long bestDistance = Long.MAX_VALUE;
            for (int x = upperTrunk.getX() - radius; x <= upperTrunk.getX() + radius; x++) {
                for (int z = upperTrunk.getZ() - radius; z <= upperTrunk.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!ColdStarTreeHandler.isDeadheadTrunk(level.getBlockState(pos))) {
                        continue;
                    }
                    long dx = x - upperTrunk.getX();
                    long dz = z - upperTrunk.getZ();
                    long distance = dx * dx + dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestAtY = pos;
                    }
                }
            }
            if (bestAtY != null) {
                return bestAtY;
            }
        }
        return null;
    }

    /** 1.10.9 {@code buildDownwardPath}. */
    private static List<BlockPos> buildDownwardPath(BlockPos upper, BlockPos lower) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = upper;
        int totalDrop = Math.max(1, upper.getY() - lower.getY());
        for (int step = 1; cursor.getY() > lower.getY(); step++) {
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

    /** 1.10.9 {@code canCarveConnection}. */
    private static boolean canCarveConnection(ServerLevel level, List<BlockPos> path, BlockPos targetTrunk) {
        for (BlockPos pos : path) {
            BlockState state = level.getBlockState(pos);
            if (pos.equals(targetTrunk) || ColdStarTreeHandler.isDeadheadTrunk(state)
                    || level.isEmptyBlock(pos) || state.is(net.minecraft.tags.BlockTags.LEAVES)
                    || state.canBeReplaced()) {
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

    private record TreeTemplate(String name, BlockPos anchor) {
    }

    private record PreservedBlock(BlockPos pos, BlockState state) {
    }
}
