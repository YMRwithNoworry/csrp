package alku.csrp.world;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Server-side cold-star deadhead tree decoration, ported from SRParasites 1.10.9
 * world/star/SRPColdStarTreeHandler.
 *
 * <p>The 1.12.2 original hooked {@code DecorateBiomeEvent.Decorate(TREE)} and DENY-ed the vanilla
 * tree pass. 1.20.1 has no equivalent event (biome decoration runs through
 * {@code BiomeGenerationSettings} / {@code FeatureSorter}), so this is instead invoked from
 * {@link StarBiomeGenerationEvents#convertNewChunk} for newly generated chunks of a COLD star world,
 * mirroring the existing {@link ColdStarVillageGenerator} pattern.
 *
 * <p><b>UNVERIFIED (PLAN.md R7):</b> the original read the per-biome tree count from
 * {@code Biome.spawnList} plus an extra-tree chance. 1.20.1 biome generation settings expose feature
 * lists rather than a plain tree count, so this port substitutes an equivalent constant pair
 * ({@link #BASE_TREE_COUNT} = 5, {@link #EXTRA_TREE_CHANCE} = 0.08F) instead of querying
 * {@code BiomeGenerationSettings#getFeatures()} (unreliable here because feature lists are no longer
 * grouped per placement step at chunk-load time).
 */
public final class ColdStarTreeHandler {
    /** 1.10.9 {@code NORMAL_TREE_DENSITY} — density multiplier applied when mushroom trees are off. */
    private static final float NORMAL_TREE_DENSITY = 0.5F;
    /** 1.10.9 {@code TREE_SNOW_RADIUS}. */
    private static final int TREE_SNOW_RADIUS = 7;
    /** 1.10.9 {@code TREE_SNOW_VERTICAL_SEARCH}. */
    private static final int TREE_SNOW_VERTICAL_SEARCH = 8;
    /** UNVERIFIED substitute for the 1.12.2 per-biome tree count (see class javadoc). */
    private static final int BASE_TREE_COUNT = 5;
    /** UNVERIFIED substitute for {@code Biome.getWorldGenSettings().field_189870_A}. */
    private static final float EXTRA_TREE_CHANCE = 0.08F;
    /** Salt keeping the tree RNG independent from {@link ColdStarVillageGenerator}. */
    private static final long TREE_SALT = 7_713_221_990_341L;

    private ColdStarTreeHandler() {
    }

    /**
     * 1.10.9 {@code onDecorateTree}, restricted to one chunk. Must be called from the server thread
     * with the target chunk loaded.
     */
    public static void decorate(ServerLevel level, int chunkX, int chunkZ) {
        if (level.dimension() != Level.OVERWORLD
                || SrpWorldData.get(level).starType() != SrpStarType.COLD
                || !level.hasChunk(chunkX, chunkZ)) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        RandomSource random = RandomSource.create(level.getSeed()
                ^ chunkPos.toLong() * 0x9E3779B97F4A7C15L ^ TREE_SALT);
        BlockPos chunkOrigin = new BlockPos(chunkX << 4, 0, chunkZ << 4);

        int treeCount = BASE_TREE_COUNT;
        if (random.nextFloat() < EXTRA_TREE_CHANCE) {
            treeCount++;
        }
        boolean mushroomTrees = SrpWorldData.get(level).mushroomTreesEnabled();
        if (!mushroomTrees) {
            treeCount = reduceNormalTreeDensity(treeCount, random);
        }

        for (int i = 0; i < treeCount; i++) {
            int x = random.nextInt(16) + 8;
            int z = random.nextInt(16) + 8;
            // 1.12.2 decorated the 16x16 area starting at chunkOrigin + 8, which reaches into the
            // neighbouring chunk. Reading or writing there from inside a ChunkEvent.Load dispatch
            // would synchronously load that chunk and re-enter this pass until the stack overflows
            // (see GenerationChunkGuard), so a sample that lands in a chunk which is not loaded is
            // wrapped back into the chunk being decorated. Tree count and the uniform distribution
            // are preserved; only the half-chunk offset is given up for those samples.
            if (!GenerationChunkGuard.isLoaded(level, chunkOrigin.getX() + x, chunkOrigin.getZ() + z)) {
                x &= 15;
                z &= 15;
            }
            BlockPos column = chunkOrigin.offset(x, 0, z);
            if (mushroomTrees) {
                BlockPos treePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
                BlockPos generationPos = treePos.below();
                if (DeadheadTreePlacer.place(level, generationPos, random, true, false)) {
                    addSnowUnderTree(level, generationPos);
                }
            } else {
                BlockPos treePos = findNormalTreePosition(level, column);
                if (treePos != null) {
                    int sinkDepth = 1 + random.nextInt(3);
                    BlockPos generationPos = treePos.below(sinkDepth);
                    if (DeadheadTreePlacer.place(level, generationPos, random, false, true)) {
                        addSnowUnderTree(level, treePos);
                    }
                }
            }
        }
    }

    /** 1.10.9 {@code reduceNormalTreeDensity}: stochastic halving. */
    private static int reduceNormalTreeDensity(int originalCount, RandomSource random) {
        if (originalCount <= 0) {
            return 0;
        }
        float scaled = originalCount * NORMAL_TREE_DENSITY;
        int whole = (int) scaled;
        float remainder = scaled - whole;
        if (random.nextFloat() < remainder) {
            whole++;
        }
        return whole;
    }

    /** 1.10.9 {@code addSnowUnderTree}: blanket a radius-7 disc with snow grass and snow layers. */
    private static void addSnowUnderTree(ServerLevel level, BlockPos treeSurfacePos) {
        int centerX = treeSurfacePos.getX();
        int centerZ = treeSurfacePos.getZ();
        int expectedGroundY = treeSurfacePos.getY() - 1;
        for (int dx = -TREE_SNOW_RADIUS; dx <= TREE_SNOW_RADIUS; dx++) {
            for (int dz = -TREE_SNOW_RADIUS; dz <= TREE_SNOW_RADIUS; dz++) {
                if (dx * dx + dz * dz > TREE_SNOW_RADIUS * TREE_SNOW_RADIUS) {
                    continue;
                }
                // The radius crosses the chunk border; never force-load the neighbour (GenerationChunkGuard).
                if (!GenerationChunkGuard.isLoaded(level, centerX + dx, centerZ + dz)) {
                    continue;
                }
                BlockPos ground = findGroundForSnow(level, centerX + dx, centerZ + dz, expectedGroundY);
                if (ground == null) {
                    continue;
                }
                BlockPos snowPos = ground.above();
                if (replaceVegetationWithSnowGrass(level, snowPos)) {
                    continue;
                }
                BlockState at = level.getBlockState(snowPos);
                if (at.is(Blocks.SNOW)) {
                    repairOrphanedDoublePlant(level, snowPos);
                } else if ((level.isEmptyBlock(snowPos) || at.canBeReplaced())
                        && Blocks.SNOW.defaultBlockState().canSurvive(level, snowPos)) {
                    level.setBlock(snowPos, Blocks.SNOW.defaultBlockState(), 2);
                }
            }
        }
    }

    /** 1.10.9 {@code replaceVegetationWithSnowGrass}. */
    private static boolean replaceVegetationWithSnowGrass(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (isDoubleGrass(block)) {
            BlockPos lowerPos;
            BlockPos upperPos;
            if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                upperPos = pos;
                lowerPos = pos.below();
            } else {
                lowerPos = pos;
                upperPos = pos.above();
            }
            BlockState lowerState = level.getBlockState(lowerPos);
            BlockState upperState = level.getBlockState(upperPos);
            if (lowerState.getBlock() == block || upperState.getBlock() == block) {
                level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(lowerPos, ModBlocks.SNOW_TALL_GRASS.get().defaultBlockState(), 2);
                ensureSnowCoveredGround(level, lowerPos.below());
                return true;
            }
        }
        if (block == Blocks.GRASS || block == Blocks.FERN) {
            level.setBlock(pos, ModBlocks.SNOW_SHORT_GRASS.get().defaultBlockState(), 2);
            ensureSnowCoveredGround(level, pos.below());
            return true;
        }
        return false;
    }

    /** 1.10.9 {@code repairOrphanedDoublePlant}: a stray snow layer over a half-grown tall grass. */
    private static void repairOrphanedDoublePlant(ServerLevel level, BlockPos snowPos) {
        BlockPos upperPos = snowPos.above();
        BlockState upperState = level.getBlockState(upperPos);
        if (isDoubleGrass(upperState.getBlock())
                && upperState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(snowPos, ModBlocks.SNOW_TALL_GRASS.get().defaultBlockState(), 2);
            ensureSnowCoveredGround(level, snowPos.below());
        }
    }

    /** 1.10.9 {@code ensureSnowCoveredGround}. */
    private static void ensureSnowCoveredGround(ServerLevel level, BlockPos groundPos) {
        if (level.getBlockState(groundPos).is(Blocks.GRASS_BLOCK)) {
            level.setBlock(groundPos, ModBlocks.SNOW_COVERED_GRASS.get().defaultBlockState(), 2);
        }
    }

    /** 1.10.9 {@code findGroundForSnow}. */
    private static BlockPos findGroundForSnow(ServerLevel level, int x, int z, int expectedGroundY) {
        int maxY = Math.min(level.getMaxBuildHeight() - 2, expectedGroundY + TREE_SNOW_VERTICAL_SEARCH);
        int minY = Math.max(level.getMinBuildHeight() + 1, expectedGroundY - TREE_SNOW_VERTICAL_SEARCH);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = maxY; y >= minY; y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (level.isEmptyBlock(pos) || state.canBeReplaced() || isTreeOrLeaves(state)) {
                continue;
            }
            if (isForbiddenSurface(state)) {
                return null;
            }
            return state.isSolid() ? pos.immutable() : null;
        }
        return null;
    }

    /** 1.10.9 {@code findNormalTreePosition}. */
    private static BlockPos findNormalTreePosition(ServerLevel level, BlockPos column) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        if (top.getY() <= level.getMinBuildHeight() + 1) {
            return null;
        }
        BlockPos cursor = top.below();
        BlockState state = level.getBlockState(cursor);
        if (isTreeOrLeaves(state) || isForbiddenSurface(state)) {
            return null;
        }
        for (int guard = 0; guard < 8 && cursor.getY() > level.getMinBuildHeight() + 1
                && isSurfaceDecoration(cursor, state); guard++) {
            cursor = cursor.below();
            state = level.getBlockState(cursor);
        }
        if (isTreeOrLeaves(state) || !isValidNormalGround(state)) {
            return null;
        }
        BlockPos treePos = cursor.above();
        BlockState atTreePos = level.getBlockState(treePos);
        return !level.isEmptyBlock(treePos) && !atTreePos.canBeReplaced() ? null : treePos;
    }

    /** 1.10.9 {@code isSurfaceDecoration}: snow layers and replaceable vegetation. */
    private static boolean isSurfaceDecoration(BlockPos pos, BlockState state) {
        if (state.is(Blocks.SNOW)) {
            return true;
        }
        return !state.liquid() && state.canBeReplaced();
    }

    /** 1.10.9 {@code isTreeOrLeaves}. */
    private static boolean isTreeOrLeaves(BlockState state) {
        return state.is(ModBlocks.PARASITETRUNK.get()) || state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES);
    }

    /** 1.10.9 {@code isForbiddenSurface}: ice, packed ice, blue ice and liquids. */
    private static boolean isForbiddenSurface(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.liquid();
    }

    /** 1.10.9 {@code isValidNormalGround}: grass, dirt, podzol or the mod's snow covered grass. */
    private static boolean isValidNormalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL)
                || state.is(ModBlocks.SNOW_COVERED_GRASS.get());
    }

    private static boolean isDoubleGrass(Block block) {
        return block == Blocks.TALL_GRASS || block == Blocks.LARGE_FERN;
    }

    /** Shared with {@link DeadheadTreePlacer}: {@code csrp:parasitetrunk_deadhead} with {@code axis = Y}. */
    static BlockState deadheadTrunkState() {
        return ModBlocks.PARASITETRUNK_DEADHEAD.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Axis.Y);
    }

    /**
     * 死头树干判定。
     *
     * <p>1.10.9 用 {@code parasitetrunk} 的 {@code variant=DEADHEAD} 属性判定；本工程用
     * **独立方块** {@code csrp:parasitetrunk_deadhead} 承载同一语义（不给 parasitetrunk
     * 加 variant 属性，否则其状态空间从 3 扩到 15、5 个既有 blockstate 都要补组合）。
     * 枯骸树结构 NBT 的树干 id 已由 {@code scripts/convert-deadhead-tree-nbt.py} 转换为
     * 本方块，因此这里按方块身份判定即可，与 1.10.9 等价。
     *
     * <p>Public so that the deadhead grass blocks in {@code alku.csrp.block} can reuse the
     * same support test.
     */
    public static boolean isDeadheadTrunk(BlockState state) {
        return state.is(ModBlocks.PARASITETRUNK_DEADHEAD.get());
    }

    /** Snow-y deadhead leaves; also part of the deadhead support rule for the hanging vines. */
    public static boolean isDeadheadLeaves(BlockState state) {
        return state.is(ModBlocks.DEADHEAD_LEAVES.get());
    }

    /**
     * Registry-id based "deadhead" fallback, matching 1.10.9's
     * {@code path.contains("deadhead") || path.contains("dead_head")} rule. Shared with the
     * deadhead grass blocks in {@code alku.csrp.block}.
     */
    public static boolean hasDeadheadId(Block block) {
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return path.contains("deadhead") || path.contains("dead_head");
    }
}
