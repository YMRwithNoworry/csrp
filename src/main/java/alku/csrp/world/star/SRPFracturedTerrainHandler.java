package alku.csrp.world.star;

import alku.csrp.Csrp;
import alku.csrp.world.SrpStarType;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Port of the 1.10.9 {@code SRPFracturedTerrainHandler}: the cold-star overworld is split into
 * drifting 96-block plates; plate borders become ravines or collision ridges, and plate interiors
 * are offset vertically with coherent-noise roughness.
 *
 * <p>The generator itself is deterministic hash/noise arithmetic and was ported verbatim. Only the
 * hosting hook changed: 1.12.2 ran on {@code PopulateChunkEvent.Pre(EventPriority.HIGHEST)}, which
 * 26.3 no longer has. The replacement is {@link ChunkEvent.Load} filtered with
 * {@link ChunkEvent.Load#isNewChunk()} - the same hook the port already uses in
 * {@code world/StarBiomeGenerationEvents}, and the point where the generated {@link LevelChunk}
 * exists but has not been handed to players yet. The 1.12.2 {@code Material} whitelist became a
 * block-tag / block-set predicate, and the absolute y limits (4 / 238 / 244 / 250 in a 0..255
 * world) became {@code level.getMinY()} / {@code level.getMaxY()} relative ones.</p>
 *
 * <p><b>Default off.</b> The feature only runs when the world data says so:
 * {@link SrpWorldData#fracturedTerrainEnabled()} requires the cold star <em>and</em> an explicit
 * opt-in recorded from the create-world screen ({@code SrpColdStarSelection}), defaulting to
 * {@code false} exactly like the original {@code BTN_FRACTURED_TERRAIN} toggle.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SRPFracturedTerrainHandler {
    private static final int PLATE_SIZE = 96;
    private static final int PLATE_JITTER = 28;
    private static final double CRACK_WIDTH = 2.25D;
    private static final double COLLISION_WIDTH = 4.75D;
    private static final int MIN_RAVINE_DEPTH = 20;
    private static final int MAX_RAVINE_DEPTH = 52;
    private static final int MIN_COLLISION_RISE = 7;
    private static final int MAX_COLLISION_RISE = 19;
    private static final int MIN_PLATE_OFFSET = -12;
    private static final int MAX_PLATE_OFFSET = 16;
    /** 1.12.2 clamped to 4 / 238 / 244 / 250 in a 0..255 world; these are the same margins. */
    private static final int FLOOR_MARGIN = 4;
    private static final int CEILING_MARGIN = 17;
    private static final int COLLISION_CEILING_MARGIN = 11;
    private static final int TEETH_CEILING_MARGIN = 5;

    /** Worldgen-style write: no neighbour updates and no client sync for a chunk nobody has seen yet. */
    private static final int WRITE_FLAGS = 0;

    private SRPFracturedTerrainHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        SrpWorldData data = SrpWorldData.get(level);
        if (!data.fracturedTerrainEnabled()
                || GenLayerSRPDynamicStar.activeGenerationStarType(level) != SrpStarType.COLD) {
            return;
        }
        fractureChunk(level, event.getChunk());
    }

    private static void fractureChunk(ServerLevel level, LevelChunk chunk) {
        long seed = level.getSeed();
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        int floorY = level.getMinY() + FLOOR_MARGIN;
        int roofY = level.getMaxY() - CEILING_MARGIN;
        boolean[] touchedColumns = new boolean[256];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = (chunkX << 4) + localX;
                int worldZ = (chunkZ << 4) + localZ;
                PlateSample sample = samplePlate(seed, worldX, worldZ);
                int topY = findSurfaceY(chunk, pos, localX, localZ, worldX, worldZ);
                if (topY <= floorY) {
                    continue;
                }
                BlockState originalSurface = chunk.getBlockState(pos.set(worldX, topY, worldZ));
                int plateOffset = getPlateOffset(sample.primaryHash());
                int targetY = clamp(topY + plateOffset + getSurfaceRoughness(seed, worldX, worldZ),
                        floorY, roofY);
                touchedColumns[localZ << 4 | localX] = true;

                if (sample.boundaryGap() > COLLISION_WIDTH) {
                    reshapePlateSurface(chunk, pos, worldX, worldZ, topY, targetY, originalSurface);
                    continue;
                }

                long boundaryHash = mix64(sample.primaryHash()
                        ^ Long.rotateLeft(sample.secondaryHash(), 21) ^ 9172280023384029625L);
                if (value(boundaryHash, 100) < 36) {
                    int rise = MIN_COLLISION_RISE
                            + value(boundaryHash >>> 11, MAX_COLLISION_RISE - MIN_COLLISION_RISE + 1);
                    double closeness = 1.0D - Math.min(1.0D,
                            sample.boundaryGap() / COLLISION_WIDTH);
                    int collisionY = clamp(targetY + (int) Math.round(rise * closeness), floorY,
                            roofY + CEILING_MARGIN - COLLISION_CEILING_MARGIN);
                    reshapePlateSurface(chunk, pos, worldX, worldZ, topY, collisionY, originalSurface);
                    addCollisionTeeth(chunk, pos, seed, worldX, worldZ, collisionY, boundaryHash,
                            level.getMaxY() - TEETH_CEILING_MARGIN);
                } else {
                    reshapePlateSurface(chunk, pos, worldX, worldZ, topY, targetY, originalSurface);
                    carvePlateCrack(chunk, pos, seed, worldX, worldZ, targetY, sample, boundaryHash,
                            floorY);
                }
            }
        }

        chunk.markUnsaved();
        relightTouchedColumns(level, chunk, touchedColumns, pos);
    }

    /**
     * 1.12.2 called {@code Chunk#generateSkylightMap()} plus {@code setLightCorrect(true)} after
     * fracturing. In 26.3 the chunk is already at {@code FULL} status when this runs, so the
     * equivalent is to re-check the light of every column this handler moved - without it a freshly
     * carved ravine keeps the sky light of the solid column it replaced.
     */
    private static void relightTouchedColumns(ServerLevel level, LevelChunk chunk,
            boolean[] touchedColumns, BlockPos.MutableBlockPos pos) {
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                if (!touchedColumns[localZ << 4 | localX]) {
                    continue;
                }
                int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
                level.getChunkSource().getLightEngine()
                        .checkBlock(pos.set((chunkX << 4) + localX, topY, (chunkZ << 4) + localZ));
            }
        }
    }

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

        return new PlateSample(primaryHash, secondaryHash,
                Math.max(0.0D, Math.sqrt(second) - Math.sqrt(nearest)));
    }

    private static int getPlateOffset(long plateHash) {
        int offset = MIN_PLATE_OFFSET
                + value(plateHash >>> 7, (MAX_PLATE_OFFSET - MIN_PLATE_OFFSET) * 2 + 1);
        if (value(plateHash >>> 31, 100) < 16) {
            int extra = 6 + value(plateHash >>> 39, 9);
            offset += (plateHash & 1L) == 0L ? extra : -extra;
        }
        return clamp(offset, -20, 24);
    }

    private static int getSurfaceRoughness(long seed, int worldX, int worldZ) {
        double broad = coherentNoise(seed, worldX, worldZ, 32, -3335678366873096957L);
        double detail = coherentNoise(seed, worldX, worldZ, 14, -7723592293110705685L);
        return (int) Math.round(broad * 1.25D + detail * 0.45D);
    }

    private static void reshapePlateSurface(LevelChunk chunk, BlockPos.MutableBlockPos pos,
            int worldX, int worldZ, int originalTopY, int targetY, BlockState originalSurface) {
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
                setBlock(chunk, pos, worldX, y, worldZ, state);
            }
            return;
        }

        for (int y = originalTopY; y > targetY; y--) {
            if (!canCarveTerrain(getBlock(chunk, pos, worldX, y, worldZ))) {
                break;
            }
            setBlock(chunk, pos, worldX, y, worldZ, Blocks.AIR.defaultBlockState());
        }

        if (canReplaceTerrainTop(getBlock(chunk, pos, worldX, targetY, worldZ))) {
            setBlock(chunk, pos, worldX, targetY, worldZ, profile.surface());
            for (int depth = 1; depth <= 3 && targetY - depth > chunk.getMinY() + 1; depth++) {
                if (!canReplaceTerrainTop(getBlock(chunk, pos, worldX, targetY - depth, worldZ))) {
                    break;
                }
                setBlock(chunk, pos, worldX, targetY - depth, worldZ, profile.filler());
            }
        }
    }

    private static void carvePlateCrack(LevelChunk chunk, BlockPos.MutableBlockPos pos, long seed,
            int worldX, int worldZ, int surfaceY, PlateSample sample, long boundaryHash, int floorY) {
        double width = CRACK_WIDTH + value(boundaryHash >>> 8, 100) / 100.0D * 1.75D;
        if (sample.boundaryGap() > width) {
            return;
        }
        double edgeWarp = coherentNoise(seed, worldX, worldZ, 13, -7723592293110705685L) * 1.15D;
        if (sample.boundaryGap() + edgeWarp > width) {
            return;
        }
        int depth = MIN_RAVINE_DEPTH
                + value(boundaryHash >>> 19, MAX_RAVINE_DEPTH - MIN_RAVINE_DEPTH + 1);
        int bottomY = Math.max(floorY, surfaceY - depth);

        for (int y = surfaceY; y >= bottomY; y--) {
            BlockState state = getBlock(chunk, pos, worldX, y, worldZ);
            if (!canCarveTerrain(state)) {
                if (state.liquid()) {
                    break;
                }
                continue;
            }
            setBlock(chunk, pos, worldX, y, worldZ, Blocks.AIR.defaultBlockState());
        }

        if (value(boundaryHash >>> 43, 100) < 38) {
            addCrackShelf(chunk, pos, worldX, worldZ, surfaceY, bottomY, boundaryHash);
        }
    }

    private static void addCrackShelf(LevelChunk chunk, BlockPos.MutableBlockPos pos, int worldX,
            int worldZ, int surfaceY, int bottomY, long hash) {
        int shelfY = bottomY + Math.max(3, (surfaceY - bottomY) / 2);
        if (shelfY >= surfaceY - 2) {
            return;
        }
        if (!getBlock(chunk, pos, worldX, shelfY, worldZ).isAir()) {
            return;
        }
        setBlock(chunk, pos, worldX, shelfY, worldZ,
                value(hash >>> 51, 2) == 0 ? Blocks.STONE.defaultBlockState()
                        : Blocks.GRAVEL.defaultBlockState());
    }

    private static void addCollisionTeeth(LevelChunk chunk, BlockPos.MutableBlockPos pos, long seed,
            int worldX, int worldZ, int surfaceY, long boundaryHash, int roofY) {
        double ridgeNoise = coherentNoise(seed, worldX, worldZ, 11,
                boundaryHash ^ -2643881736870682267L);
        if (ridgeNoise < 0.28D) {
            return;
        }
        int extra = 2 + (int) Math.round(Math.min(1.0D, (ridgeNoise - 0.28D) / 0.72D) * 6.0D);
        for (int i = 1; i <= extra && surfaceY + i < roofY; i++) {
            setBlock(chunk, pos, worldX, surfaceY + i, worldZ, Blocks.STONE.defaultBlockState());
        }
    }

    private static int findSurfaceY(LevelChunk chunk, BlockPos.MutableBlockPos pos, int localX,
            int localZ, int worldX, int worldZ) {
        int height = Mth.clamp(chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) - 1,
                chunk.getMinY() + 1, chunk.getMaxY());
        for (int y = height; y > chunk.getMinY() + 1; y--) {
            BlockState state = chunk.getBlockState(pos.set(worldX, y, worldZ));
            if (!state.isAir() && !state.is(Blocks.SNOW)) {
                return y;
            }
        }
        return chunk.getMinY() + 1;
    }

    /**
     * 1.12.2 branched on {@code Blocks} identities.
     *
     * <p>The only two entries whose 1.12.2 identity is ambiguous are the pairs
     * {@code field_150432_aD}/{@code field_150403_cj} and {@code field_150433_aE}: the port treats
     * them as the ice pair ({@code ICE}/{@code PACKED_ICE}) and the snow block, matching the rest of
     * the port's snow/ice mapping (see {@code block/ParasiteRubbleBlock}), which is also the only
     * interpretation that makes sense for a table that is used exclusively on the cold star.</p>
     */
    private static SurfaceProfile getProfile(BlockState surface) {
        Block block = surface.getBlock();
        if (block == Blocks.GRASS_BLOCK) {
            return new SurfaceProfile(Blocks.GRASS_BLOCK.defaultBlockState(),
                    Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState());
        }
        if (block == Blocks.DIRT) {
            return new SurfaceProfile(surface, Blocks.DIRT.defaultBlockState(),
                    Blocks.STONE.defaultBlockState());
        }
        if (block == Blocks.SAND) {
            return new SurfaceProfile(surface, surface, Blocks.SANDSTONE.defaultBlockState());
        }
        if (block == Blocks.GRAVEL) {
            return new SurfaceProfile(surface, surface, Blocks.STONE.defaultBlockState());
        }
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE) {
            return new SurfaceProfile(surface, Blocks.PACKED_ICE.defaultBlockState(),
                    Blocks.STONE.defaultBlockState());
        }
        if (block == Blocks.SNOW || block == Blocks.SNOW_BLOCK) {
            return new SurfaceProfile(surface, Blocks.SNOW_BLOCK.defaultBlockState(),
                    Blocks.STONE.defaultBlockState());
        }
        return new SurfaceProfile(surface, Blocks.STONE.defaultBlockState(),
                Blocks.STONE.defaultBlockState());
    }

    private static boolean isTerrainSurface(BlockState state) {
        if (state.is(Blocks.BEDROCK) || state.liquid()) {
            return false;
        }
        return isTerrainMaterial(state);
    }

    private static boolean canCarveTerrain(BlockState state) {
        if (state.is(Blocks.BEDROCK) || state.liquid() || state.hasBlockEntity()) {
            return false;
        }
        return isTerrainMaterial(state);
    }

    private static boolean canReplaceTerrainTop(BlockState state) {
        return state.hasBlockEntity() ? false : canCarveTerrain(state) || state.isAir();
    }

    /**
     * 26.3 replacement for the 1.12.2 {@code Material} whitelist
     * (GROUND / GRASS / SAND / CLAY / SNOW / CRAFTED_SNOW / ICE / PACKED_ICE): the dirt and sand
     * block tags plus the explicit surface blocks those materials covered.
     */
    private static boolean isTerrainMaterial(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.FROSTED_ICE);
    }

    private static BlockState getBlock(LevelChunk chunk, BlockPos.MutableBlockPos pos, int worldX,
            int y, int worldZ) {
        return chunk.getBlockState(pos.set(worldX, y, worldZ));
    }

    private static void setBlock(LevelChunk chunk, BlockPos.MutableBlockPos pos, int worldX, int y,
            int worldZ, BlockState state) {
        chunk.setBlockState(pos.set(worldX, y, worldZ), state, WRITE_FLAGS);
    }

    private static double coherentNoise(long seed, int worldX, int worldZ, int scale, long salt) {
        int cellX = Math.floorDiv(worldX, scale);
        int cellZ = Math.floorDiv(worldZ, scale);
        double tx = smoothStep((double) Math.floorMod(worldX, scale) / scale);
        double tz = smoothStep((double) Math.floorMod(worldZ, scale) / scale);
        double v00 = latticeNoise(seed, cellX, cellZ, salt);
        double v10 = latticeNoise(seed, cellX + 1, cellZ, salt);
        double v01 = latticeNoise(seed, cellX, cellZ + 1, salt);
        double v11 = latticeNoise(seed, cellX + 1, cellZ + 1, salt);
        return lerp(lerp(v00, v10, tx), lerp(v01, v11, tx), tz);
    }

    private static double latticeNoise(long seed, int cellX, int cellZ, long salt) {
        long hash = mix64(seed ^ salt ^ cellX * 341873128712L ^ cellZ * 132897987541L);
        long positive = hash & Long.MAX_VALUE;
        return (positive % 1000000L) / 999999.0D * 2.0D - 1.0D;
    }

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static long hashCell(long seed, int cellX, int cellZ) {
        return mix64(seed ^ cellX * 341873128712L ^ cellZ * 132897987541L ^ -7046029254386353131L);
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record PlateSample(long primaryHash, long secondaryHash, double boundaryGap) {
    }

    private record SurfaceProfile(BlockState surface, BlockState filler, BlockState core) {
    }
}
