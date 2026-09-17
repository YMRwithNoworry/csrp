package alku.csrp.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Cold-star fractured terrain, ported verbatim from SRParasites 1.10.9
 * {@code world/star/SRPFracturedTerrainHandler}.
 *
 * <p>The 1.12.2 original hooked {@code PopulateChunkEvent.Pre(HIGHEST)}. 1.20.1 has no
 * {@code PopulateChunkEvent}, so this runs from {@link StarBiomeGenerationEvents#convertNewChunk} for
 * newly generated chunks of a COLD star world, <b>before</b> the biome remap (PLAN.md §5.5).
 *
 * <p>All constants and all arithmetic ({@code mix64}, {@code hashCell}, {@code latticeNoise},
 * {@code coherentNoise}, {@code value}, {@code smoothStep}, {@code lerp}, the plate jitter/offset
 * formulas, the crack and collision-ridge geometry) are copied character for character. Only the
 * Minecraft-facing pieces change:
 * <ul>
 *   <li>{@code Material} tests become block/tag tests (1.20.1 has no {@code Material}).</li>
 *   <li>Hard-coded heights {@code 4 / 238 / 244 / 250 / 255} become offsets from
 *       {@code getMinBuildHeight()}/{@code getMaxBuildHeight()} (PLAN.md R14).</li>
 *   <li>{@code chunk.func_76603_b()} (recompute heightmaps) has no direct call: verified that
 *       {@code LevelChunk.setBlockState} maintains {@code ChunkAccess.heightmaps} itself
 *       (PLAN.md T5 resolution).</li>
 *   <li>{@code BlockTags.SNOWY} does not exist in 1.20.1 (verified) — {@code BlockTags.SNOW} covers
 *       the snow block family plus the snow layer instead.</li>
 * </ul>
 */
final class FracturedTerrainHandler {
    private static final int PLATE_SIZE = 96;
    private static final int PLATE_JITTER = 28;
    private static final double CRACK_WIDTH = 2.25;
    private static final double COLLISION_WIDTH = 4.75;
    private static final int MIN_RAVINE_DEPTH = 20;
    private static final int MAX_RAVINE_DEPTH = 52;
    private static final int MIN_COLLISION_RISE = 7;
    private static final int MAX_COLLISION_RISE = 19;
    private static final int MIN_PLATE_OFFSET = -12;
    private static final int MAX_PLATE_OFFSET = 16;

    private FracturedTerrainHandler() {
    }

    /** 1.10.9 {@code onPopulatePre} + {@code fractureChunk}, scoped to one already-loaded chunk. */
    static void fractureChunk(ServerLevel level, LevelChunk chunk) {
        long seed = level.getSeed();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = (chunkX << 4) + localX;
                int worldZ = (chunkZ << 4) + localZ;
                PlateSample sample = samplePlate(seed, worldX, worldZ);
                int topY = findSurfaceY(chunk, localX, localZ, minY, maxY);
                if (topY <= minY + 4) {
                    continue;
                }
                BlockState originalSurface = chunk.getBlockState(new BlockPos(worldX, topY, worldZ));
                int plateOffset = getPlateOffset(sample.primaryHash());
                int roughness = getSurfaceRoughness(seed, worldX, worldZ);
                int targetY = clamp(topY + plateOffset + roughness, minY + 4, maxY - 2);
                boolean onBoundary = sample.boundaryGap() <= COLLISION_WIDTH;
                if (!onBoundary) {
                    reshapePlateSurface(chunk, worldX, worldZ, topY, targetY, originalSurface, minY);
                } else {
                    long boundaryHash = mix64(sample.primaryHash()
                            ^ Long.rotateLeft(sample.secondaryHash(), 21) ^ 9172280023384029625L);
                    boolean collisionRidge = value(boundaryHash, 100) < 36;
                    if (collisionRidge) {
                        int rise = MIN_COLLISION_RISE + value(boundaryHash >>> 11, 13);
                        double closeness = 1.0D - Math.min(1.0D, sample.boundaryGap() / COLLISION_WIDTH);
                        int collisionY = clamp(targetY + (int) Math.round(rise * closeness), minY + 4,
                                maxY - 2);
                        reshapePlateSurface(chunk, worldX, worldZ, topY, collisionY, originalSurface, minY);
                        addCollisionTeeth(chunk, seed, worldX, worldZ, collisionY, boundaryHash, maxY - 2);
                    } else {
                        reshapePlateSurface(chunk, worldX, worldZ, topY, targetY, originalSurface, minY);
                        carvePlateCrack(chunk, seed, worldX, worldZ, targetY, sample, boundaryHash, minY);
                    }
                }
            }
        }
    }

    /** 1.10.9 {@code samplePlate}. */
    private static PlateSample samplePlate(long seed, int worldX, int worldZ) {
        int cellX = Math.floorDiv(worldX, PLATE_SIZE);
        int cellZ = Math.floorDiv(worldZ, PLATE_SIZE);
        double nearest = Double.MAX_VALUE;
        double second = Double.MAX_VALUE;
        long primaryHash = 0L;
        long secondaryHash = 0L;
        for (int gx = cellX - 1; gx <= cellX + 1; gx++) {
            for (int gz = cellZ - 1; gz <= cellZ + 1; gz++) {
                long plateHash = hashCell(seed, gx, gz);
                int jitterX = value(plateHash, PLATE_JITTER * 2 + 1) - PLATE_JITTER;
                int jitterZ = value(plateHash >>> 17, PLATE_JITTER * 2 + 1) - PLATE_JITTER;
                double centerX = gx * (double) PLATE_SIZE + PLATE_SIZE / 2.0D + jitterX;
                double centerZ = gz * (double) PLATE_SIZE + PLATE_SIZE / 2.0D + jitterZ;
                double dx = worldX - centerX;
                double dz = worldZ - centerZ;
                double distance = dx * dx + dz * dz;
                if (distance < nearest) {
                    second = nearest;
                    secondaryHash = primaryHash;
                    nearest = distance;
                    primaryHash = plateHash;
                } else if (distance < second) {
                    second = distance;
                    secondaryHash = plateHash;
                }
            }
        }
        double nearestDistance = Math.sqrt(nearest);
        double secondDistance = Math.sqrt(second);
        return new PlateSample(primaryHash, secondaryHash,
                Math.max(0.0D, secondDistance - nearestDistance));
    }

    /** 1.10.9 {@code getPlateOffset}. */
    private static int getPlateOffset(long plateHash) {
        int offset = MIN_PLATE_OFFSET + value(plateHash >>> 7, MAX_PLATE_OFFSET - MIN_PLATE_OFFSET);
        if (value(plateHash >>> 31, 100) < 16) {
            int extra = 6 + value(plateHash >>> 39, 9);
            offset += (plateHash & 1L) == 0L ? extra : -extra;
        }
        return clamp(offset, -20, 24);
    }

    /** 1.10.9 {@code getSurfaceRoughness}. */
    private static int getSurfaceRoughness(long seed, int worldX, int worldZ) {
        double broad = coherentNoise(seed, worldX, worldZ, 32, -3335678366873096957L);
        double detail = coherentNoise(seed, worldX, worldZ, 14, -7723592293110705685L);
        return (int) Math.round(broad * 1.25D + detail * 0.45D);
    }

    /** 1.10.9 {@code reshapePlateSurface}. */
    private static void reshapePlateSurface(LevelChunk chunk, int worldX, int worldZ, int originalTopY,
            int targetY, BlockState originalSurface, int minY) {
        if (!isTerrainSurface(originalSurface) || targetY == originalTopY) {
            return;
        }
        SurfaceProfile profile = getProfile(originalSurface);
        if (targetY > originalTopY) {
            for (int y = originalTopY + 1; y <= targetY; y++) {
                BlockState state;
                if (y == targetY) {
                    state = profile.surface();
                } else if (y >= targetY - 3) {
                    state = profile.filler();
                } else {
                    state = profile.core();
                }
                setBlock(chunk, worldX, y, worldZ, state);
            }
            return;
        }
        for (int y = originalTopY; y > targetY; y--) {
            BlockState existing = getBlock(chunk, worldX, y, worldZ);
            if (!canCarveTerrain(existing)) {
                break;
            }
            setBlock(chunk, worldX, y, worldZ, Blocks.AIR.defaultBlockState());
        }
        BlockState target = getBlock(chunk, worldX, targetY, worldZ);
        if (!canReplaceTerrainTop(target)) {
            return;
        }
        setBlock(chunk, worldX, targetY, worldZ, profile.surface());
        for (int depth = 1; depth <= 3 && targetY - depth > minY + 1; depth++) {
            BlockState below = getBlock(chunk, worldX, targetY - depth, worldZ);
            if (!canReplaceTerrainTop(below)) {
                break;
            }
            setBlock(chunk, worldX, targetY - depth, worldZ, profile.filler());
        }
    }

    /** 1.10.9 {@code carvePlateCrack}. */
    private static void carvePlateCrack(LevelChunk chunk, long seed, int worldX, int worldZ, int surfaceY,
            PlateSample sample, long boundaryHash, int minY) {
        double width = CRACK_WIDTH + value(boundaryHash >>> 8, 100) / 100.0D * 1.75D;
        if (sample.boundaryGap() > width) {
            return;
        }
        double edgeWarp = coherentNoise(seed, worldX, worldZ, 13, -7723592293110705685L) * 1.15D;
        if (sample.boundaryGap() + edgeWarp > width) {
            return;
        }
        int depth = MIN_RAVINE_DEPTH + value(boundaryHash >>> 19, MAX_RAVINE_DEPTH - MIN_RAVINE_DEPTH + 1);
        int bottomY = Math.max(minY + 4, surfaceY - depth);
        for (int y = surfaceY; y >= bottomY; y--) {
            BlockState state = getBlock(chunk, worldX, y, worldZ);
            if (!canCarveTerrain(state)) {
                if (state.liquid()) {
                    break;
                }
                continue;
            }
            setBlock(chunk, worldX, y, worldZ, Blocks.AIR.defaultBlockState());
        }
        if (value(boundaryHash >>> 43, 100) < 38) {
            addCrackShelf(chunk, worldX, worldZ, surfaceY, bottomY, boundaryHash);
        }
    }

    /** 1.10.9 {@code addCrackShelf}. */
    private static void addCrackShelf(LevelChunk chunk, int worldX, int worldZ, int surfaceY, int bottomY,
            long hash) {
        int shelfY = bottomY + Math.max(3, (surfaceY - bottomY) / 2);
        if (shelfY >= surfaceY - 2) {
            return;
        }
        BlockState state = getBlock(chunk, worldX, shelfY, worldZ);
        if (state.isAir()) {
            setBlock(chunk, worldX, shelfY, worldZ, value(hash >>> 51, 2) == 0
                    ? Blocks.STONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
        }
    }

    /** 1.10.9 {@code addCollisionTeeth}: the original caps the loop at {@code surfaceY + i < 250}. */
    private static void addCollisionTeeth(LevelChunk chunk, long seed, int worldX, int worldZ, int surfaceY,
            long boundaryHash, int ceilingY) {
        double ridgeNoise = coherentNoise(seed, worldX, worldZ, 11,
                boundaryHash ^ -2643881736870682267L);
        if (ridgeNoise < 0.28D) {
            return;
        }
        int extra = 2 + (int) Math.round(Math.min(1.0D, (ridgeNoise - 0.28D) / 0.72D) * 6.0D);
        for (int i = 1; i <= extra && surfaceY + i < ceilingY; i++) {
            setBlock(chunk, worldX, surfaceY + i, worldZ, Blocks.STONE.defaultBlockState());
        }
    }

    /** 1.10.9 {@code findSurfaceY}. */
    private static int findSurfaceY(LevelChunk chunk, int localX, int localZ, int minY, int maxY) {
        int height = Math.min(maxY - 1, Math.max(minY + 1,
                chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) - 1));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = height; y > minY + 1; y--) {
            pos.set(localX, y, localZ);
            BlockState state = chunk.getBlockState(pos);
            if (!state.isAir() && !state.is(Blocks.SNOW)) {
                return y;
            }
        }
        return minY;
    }

    /** 1.10.9 {@code getProfile}, with the 1.12.2 block ids mapped to their 1.20.1 counterparts. */
    private static SurfaceProfile getProfile(BlockState surface) {
        if (surface.is(Blocks.GRASS_BLOCK)) {
            return new SurfaceProfile(Blocks.GRASS_BLOCK.defaultBlockState(),
                    Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState());
        }
        if (surface.is(BlockTags.DIRT)) {
            return new SurfaceProfile(surface, Blocks.DIRT.defaultBlockState(),
                    Blocks.STONE.defaultBlockState());
        }
        if (surface.is(BlockTags.SAND)) {
            return new SurfaceProfile(surface, surface, Blocks.SANDSTONE.defaultBlockState());
        }
        if (surface.is(Blocks.GRAVEL)) {
            return new SurfaceProfile(surface, surface, Blocks.STONE.defaultBlockState());
        }
        if (surface.is(Blocks.ICE) || surface.is(Blocks.PACKED_ICE)) {
            return new SurfaceProfile(surface, Blocks.PACKED_ICE.defaultBlockState(),
                    Blocks.STONE.defaultBlockState());
        }
        if (surface.is(Blocks.SNOW_BLOCK) || surface.is(BlockTags.SNOW)) {
            return new SurfaceProfile(surface, Blocks.SNOW_BLOCK.defaultBlockState(),
                    Blocks.STONE.defaultBlockState());
        }
        return new SurfaceProfile(surface, Blocks.STONE.defaultBlockState(),
                Blocks.STONE.defaultBlockState());
    }

    /** 1.10.9 {@code isTerrainSurface}. */
    private static boolean isTerrainSurface(BlockState state) {
        if (state.is(Blocks.BEDROCK) || state.liquid()) {
            return false;
        }
        return isTerrainMaterial(state);
    }

    /** 1.10.9 {@code canCarveTerrain}. */
    private static boolean canCarveTerrain(BlockState state) {
        if (state.is(Blocks.BEDROCK) || state.liquid() || state.hasBlockEntity()) {
            return false;
        }
        return isTerrainMaterial(state);
    }

    /** 1.10.9 {@code canReplaceTerrainTop}. */
    private static boolean canReplaceTerrainTop(BlockState state) {
        if (state.hasBlockEntity()) {
            return false;
        }
        return canCarveTerrain(state) || state.isAir();
    }

    /**
     * The 1.10.9 {@code Material} whitelist (ROCK / GROUND / SAND / SNOW / ICE / PACKED_ICE /
     * CRAFTED_SNOW / GLASS? no) mapped to tags and blocks:
     * {@code BASE_STONE_OVERWORLD ∪ DIRT ∪ SAND ∪ SNOW ∪ ICE ∪ GRAVEL/CLAY/MOSS_BLOCK/DEEPSLATE}
     * (PLAN.md §5.5). {@code BlockTags.SNOWY} is deliberately <b>not</b> used — it does not exist in
     * 1.20.1.
     */
    private static boolean isTerrainMaterial(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.SNOW)
                || state.is(BlockTags.ICE)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.DEEPSLATE);
    }

    private static BlockState getBlock(LevelChunk chunk, int worldX, int y, int worldZ) {
        return chunk.getBlockState(new BlockPos(worldX, y, worldZ));
    }

    private static void setBlock(LevelChunk chunk, int worldX, int y, int worldZ, BlockState state) {
        chunk.setBlockState(new BlockPos(worldX, y, worldZ), state, false);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= -4658895280553007687L;
        value ^= value >>> 27;
        value *= -7723592293110705685L;
        return value ^ value >>> 31;
    }

    private static int value(long hash, int bound) {
        if (bound <= 1) {
            return 0;
        }
        int mixed = (int) (hash ^ hash >>> 32);
        return Math.floorMod(mixed, bound);
    }

    private static long hashCell(long seed, int cellX, int cellZ) {
        return mix64(seed ^ cellX * 341873128712L ^ cellZ * 132897987541L ^ -7046029254386353131L);
    }

    private static double latticeNoise(long seed, int cellX, int cellZ, long salt) {
        long hash = mix64(seed ^ salt ^ cellX * 341873128712L ^ cellZ * 132897987541L);
        long positive = hash & Long.MAX_VALUE;
        double unit = positive % 1000000L / 999999.0D;
        return unit * 2.0D - 1.0D;
    }

    private static double coherentNoise(long seed, int worldX, int worldZ, int scale, long salt) {
        int cellX = Math.floorDiv(worldX, scale);
        int cellZ = Math.floorDiv(worldZ, scale);
        int localX = Math.floorMod(worldX, scale);
        int localZ = Math.floorMod(worldZ, scale);
        double tx = smoothStep((double) localX / scale);
        double tz = smoothStep((double) localZ / scale);
        double v00 = latticeNoise(seed, cellX, cellZ, salt);
        double v10 = latticeNoise(seed, cellX + 1, cellZ, salt);
        double v01 = latticeNoise(seed, cellX, cellZ + 1, salt);
        double v11 = latticeNoise(seed, cellX + 1, cellZ + 1, salt);
        double top = lerp(v00, v10, tx);
        double bottom = lerp(v01, v11, tx);
        return lerp(top, bottom, tz);
    }

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private record PlateSample(long primaryHash, long secondaryHash, double boundaryGap) {
    }

    private record SurfaceProfile(BlockState surface, BlockState filler, BlockState core) {
    }
}
