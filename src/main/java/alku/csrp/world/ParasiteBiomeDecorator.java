package alku.csrp.world;

import alku.csrp.registry.ModBlocks;
import alku.csrp.world.gen.ParasiteGenContext;
import alku.csrp.world.gen.WorldGenParasiteBall;
import alku.csrp.world.gen.WorldGenParasiteBigBall;
import alku.csrp.world.gen.WorldGenParasiteBush;
import alku.csrp.world.gen.WorldGenParasiteMouth;
import alku.csrp.world.gen.WorldGenParasiteSpine;
import alku.csrp.world.gen.WorldGenParasiteTallFlower;
import alku.csrp.world.gen.WorldGenParasiteTenFlower;
import alku.csrp.world.gen.WorldGenParasiteTree;
import alku.csrp.world.gen.WorldGenParasiteTreeThin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Port of the 1.10.9 {@code BiomeParasiteDecorator}'s per-chunk decoration plus the per-biome
 * behaviour of {@code BiomeParasiteBase} / {@code Boils} / {@code Demen} / {@code Harlequin} /
 * {@code Shrouded}.
 *
 * <p>1.12.2 decorated parasite chunks through {@code BiomeDecorator#decorate}, which no longer exists
 * in 26.3 (biome decoration now lives in the biome JSON {@code features} list — and this project's
 * four SRP biome files ship an empty list).  Because the SRP biomes are stamped onto chunks after
 * terrain generation, the decoration is replayed explicitly:</p>
 *
 * <ol>
 *   <li>{@link StarBiomeGenerationEvents} queues every newly generated chunk whose centre is one of
 *       the four SRP biomes.</li>
 *   <li>The queue is drained on a later server tick — never inside {@code ChunkEvent.Load}, whose
 *       documentation warns that the chunk has not reached {@code ChunkStatus.FULL} yet and that
 *       interacting with the level there can deadlock the server.</li>
 *   <li>Each chunk is decorated exactly once; {@link SrpWorldData} keeps a persisted per-region bit
 *       as the durable marker.</li>
 * </ol>
 *
 * <p><b>The decoration table.</b>  All four biomes set the identical {@code BiomeDecorator}
 * counters, so the only per-biome differences are the tree generator (thin 1-in-3, else plain), the
 * grass generator (POP / EYE) and the Shrouded flora spawners.  Forge's decoration event types map
 * onto the counters as follows:</p>
 *
 * <pre>
 * slot            counter                                     feature
 * TREE            k1 = 1 (+0 if random.nextFloat() &lt; 0.0F)   WorldGenParasiteTree / TreeThin
 * BIG_SHROOM      1                                           WorldGenParasiteBigBall
 * FLOWERS         4                                           biome flower table -&gt; bush POP/EYE
 * GRASS           15                                          1/10 biome grass, else bush(TENDRIL, 4)
 * DEAD_BUSH       2                                           bush(EYE, 2)
 * SHROOM          0                                           (none)
 * REED            0 (+ a hard-coded 10)                       (none)
 * LILYPAD         0                                           (none)
 * PUMPKIN         1/32 per chunk                              WorldGenParasiteBall
 * CACTUS          15                                          bush(TENDRIL, 4)
 * SAND/CLAY/...   0                                           (none)
 * LAKE_WATER      50                                          WorldGenLiquids(WATER)
 * LAKE_LAVA       20                                          WorldGenLiquids(LAVA)
 * </pre>
 */
public final class ParasiteBiomeDecorator {
    private static final int CHUNK_SIZE = 16;
    private static final long RANDOM_SALT = 0x9E3779B97F4A7C15L;
    /** Sea level; every SRP biome covers the full vertical column, so one sample per column is enough. */
    private static final int SAMPLE_Y = 64;

    private static final ResourceKey<Biome> SHROUDED = key("srp_shrouded");
    /** Public because {@code HarlequinRockBushGen} tests the biome of each candidate position. */
    public static final ResourceKey<Biome> HARLEQUIN_BIOME = key("srp_harlequinn");
    private static final ResourceKey<Biome> BOILS = key("srp_boils");
    private static final ResourceKey<Biome> DEMEN = key("srp_demen");

    private static final WorldGenParasiteBush BUSH_POP = WorldGenParasiteBush.of("pop", 2);
    private static final WorldGenParasiteBush BUSH_EYE = WorldGenParasiteBush.of("eye", 1);

    private ParasiteBiomeDecorator() {
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("csrp", path));
    }

    /** Which of the two 1.10.9 palettes (if any) applies to a chunk. */
    public enum Flask {
        /** Boils/Demen: the original left every block table null, so nothing is converted. */
        NONE,
        HARLEQUIN,
        SHROUDED
    }

    public record BiomeSurface(Flask flask, boolean shroudedFlora) {
    }

    // ---------------------------------------------------------------------------------------------
    // Chunk detection and entry point
    // ---------------------------------------------------------------------------------------------

    /** @return the SRP biome covering {@code pos}, or {@code null} for a normal biome. */
    public static ResourceKey<Biome> parasiteBiomeAt(LevelChunk chunk, BlockPos pos) {
        Holder<Biome> holder = chunk.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
        return holder.unwrapKey().orElse(null);
    }

    public static boolean isParasiteChunk(LevelChunk chunk) {
        return findSurface(chunk) != null;
    }

    /**
     * @return the decoration profile for this chunk, or {@code null} when the chunk does not carry
     *         one of the four SRP biomes.
     */
    public static BiomeSurface surfaceOf(LevelChunk chunk) {
        return findSurface(chunk);
    }

    /**
     * Looks for the first chunk column whose biome is one of the four SRP biomes.  A chunk can only
     * hold one biome in this project (the star conversion and the parasite biome generator both
     * rewrite whole sections), so sampling a 4x4 grid at sea level is enough; a mixed chunk falls
     * back to its first parasite column.
     */
    private static BiomeSurface findSurface(LevelChunk chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < CHUNK_SIZE; x += 4) {
            for (int z = 0; z < CHUNK_SIZE; z += 4) {
                BlockPos sample = new BlockPos(minX + x, SAMPLE_Y, minZ + z);
                BiomeSurface surface = toSurface(parasiteBiomeAt(chunk, sample));
                if (surface != null) {
                    return surface;
                }
            }
        }
        return null;
    }

    private static BiomeSurface toSurface(ResourceKey<Biome> biome) {
        if (biome == null) {
            return null;
        }
        if (biome.equals(HARLEQUIN_BIOME)) {
            return new BiomeSurface(Flask.HARLEQUIN, false);
        }
        if (biome.equals(SHROUDED)) {
            return new BiomeSurface(Flask.SHROUDED, true);
        }
        if (biome.equals(BOILS) || biome.equals(DEMEN)) {
            return new BiomeSurface(Flask.NONE, false);
        }
        return null;
    }

    /**
     * Replays the 1.10.9 decoration order for one chunk.  The layout runs from a random seeded with
     * the chunk position so a chunk always decorates the same way, mirroring the original
     * {@code BiomeDecorator} random being derived from the chunk seed.
     */
    public static void decorate(ServerLevel level, LevelChunk chunk, BiomeSurface surface) {
        RandomSource random = RandomSource.create(level.getSeed()
                ^ net.minecraft.world.level.ChunkPos.pack(chunk.getPos().x(), chunk.getPos().z()) * RANDOM_SALT);
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();

        decorateTrees(level, random, minX, minZ);
        decorateBigShroom(level, random, minX, minZ);
        decorateFlowers(level, random, minX, minZ);
        decorateGrass(level, random, minX, minZ);
        decorateDeadBush(level, random, minX, minZ);
        decorateLilyPads(level, random, minX, minZ);
        decorateShrooms(level, random, minX, minZ);
        decorateReeds(level, random, minX, minZ);
        decoratePumpkin(level, random, minX, minZ);
        decorateCactus(level, random, minX, minZ);
        decorateWaterLakes(level, random, minX, minZ);
        decorateLavaLakes(level, random, minX, minZ);

        convertChunk(level, chunk, surface, random, minX, minZ);

        if (surface.shroudedFlora()) {
            decorateShroudedFlora(level, chunk, random, minX, minZ);
        }
        chunk.markUnsaved();
    }

    // ---------------------------------------------------------------------------------------------
    // Decoration slots
    // ---------------------------------------------------------------------------------------------

    /** TREE: k1 = 1 tree (the {@code field_189870_A = 0.0F} bonus roll can never add one). */
    private static void decorateTrees(ServerLevel level, RandomSource random, int minX, int minZ) {
        int count = 1;
        if (random.nextFloat() < 0.0F) {
            count++;
        }
        for (int i = 0; i < count; i++) {
            BlockPos column = at(random, minX, minZ);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (random.nextInt(3) == 0) {
                WorldGenParasiteTreeThin.generate(level, random, surface);
            } else {
                WorldGenParasiteTree.generate(level, random, surface);
            }
        }
    }

    /** BIG_SHROOM: 1 attempt, placed at the raw 2D surface height like the original. */
    private static void decorateBigShroom(ServerLevel level, RandomSource random, int minX, int minZ) {
        BlockPos column = at(random, minX, minZ);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        WorldGenParasiteBigBall.generate(level, random, new BlockPos(column.getX(), y, column.getZ()));
    }

    /**
     * FLOWERS: 4 attempts at a random {@code y} below {@code surface + 32}.  The original re-rolled
     * the vanilla flower table ({@code Biome#pickRandomFlower}) and placed a bush only when the
     * chosen flower was not air; the modern flower table lives in biome JSON and is not readable
     * here, so the port substitutes the biome's own grass generator
     * ({@code func_76730_b}: POP half the time, EYE otherwise).
     */
    private static void decorateFlowers(ServerLevel level, RandomSource random, int minX, int minZ) {
        for (int i = 0; i < 4; i++) {
            BlockPos column = at(random, minX, minZ);
            int bound = Math.max(1, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    column.getX(), column.getZ()) + 32);
            int y = random.nextInt(bound);
            if (y <= level.getMinY() || y >= level.getMaxY() - 1) {
                continue;
            }
            grassGenerator(random).generate(level, random, new BlockPos(column.getX(), y, column.getZ()));
        }
    }

    /** GRASS: 15 attempts, 1 in 10 the biome grass generator, otherwise bush(TENDRIL, 4). */
    private static void decorateGrass(ServerLevel level, RandomSource random, int minX, int minZ) {
        for (int i = 0; i < 15; i++) {
            BlockPos column = at(random, minX, minZ);
            int bound = doubledHeight(level, column);
            if (bound <= 0) {
                continue;
            }
            int y = random.nextInt(bound);
            if (y <= level.getMinY() || y >= level.getMaxY() - 1) {
                continue;
            }
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            if (random.nextInt(10) == 0) {
                grassGenerator(random).generate(level, random, pos);
            } else {
                WorldGenParasiteBush.of("tendril", 4).generate(level, random, pos);
            }
        }
    }

    /** DEAD_BUSH: 2 attempts of bush(EYE, 2) at the doubled height. */
    private static void decorateDeadBush(ServerLevel level, RandomSource random, int minX, int minZ) {
        for (int i = 0; i < 2; i++) {
            BlockPos column = at(random, minX, minZ);
            int bound = doubledHeight(level, column);
            if (bound <= 0) {
                continue;
            }
            int y = random.nextInt(bound);
            if (y <= level.getMinY() || y >= level.getMaxY() - 1) {
                continue;
            }
            BUSH_EYE.generate(level, random, new BlockPos(column.getX(), y, column.getZ()));
        }
    }

    /** LILYPAD: counter 0 in every parasite biome; kept so the slot order stays complete. */
    private static void decorateLilyPads(ServerLevel level, RandomSource random, int minX, int minZ) {
        for (int i = 0; i < 0; i++) {
            BlockPos column = at(random, minX, minZ);
            int bound = doubledHeight(level, column);
            if (bound <= 0) {
                continue;
            }
            BlockPos pos = new BlockPos(column.getX(), random.nextInt(bound), column.getZ());
            int guard = 0;
            while (pos.getY() > level.getMinY() + 1 && guard++ < 512
                    && ParasiteGenContext.get(level, pos.below()).isAir()) {
                pos = pos.below();
            }
            placeLilyPad(level, pos);
        }
    }

    /** SHROOM: counter 0 in every parasite biome; kept so the slot order stays complete. */
    private static void decorateShrooms(ServerLevel level, RandomSource random, int minX, int minZ) {
        for (int i = 0; i < 0; i++) {
            if (random.nextInt(4) != 0) {
                continue;
            }
            BlockPos column = at(random, minX, minZ);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            placeBushShroom(level, new BlockPos(column.getX(), y, column.getZ()));
        }
    }

    /** REED: counter 0, plus the hard-coded 10 extra rolls the original always ran. */
    private static void decorateReeds(ServerLevel level, RandomSource random, int minX, int minZ) {
        for (int i = 0; i < 10; i++) {
            BlockPos column = at(random, minX, minZ);
            int bound = doubledHeight(level, column);
            if (bound <= 0) {
                continue;
            }
            int y = random.nextInt(bound);
            if (y <= level.getMinY() || y >= level.getMaxY() - 1) {
                continue;
            }
            placeReed(level, new BlockPos(column.getX(), y, column.getZ()));
        }
    }

    /** PUMPKIN: 1 in 32 per chunk, {@code WorldGenParasiteBall}. */
    private static void decoratePumpkin(ServerLevel level, RandomSource random, int minX, int minZ) {
        if (random.nextInt(32) != 0) {
            return;
        }
        BlockPos column = at(random, minX, minZ);
        int bound = doubledHeight(level, column);
        if (bound <= 0) {
            return;
        }
        int y = random.nextInt(bound);
        if (y <= level.getMinY() || y >= level.getMaxY() - 1) {
            return;
        }
        WorldGenParasiteBall.generate(level, random, new BlockPos(column.getX(), y, column.getZ()));
    }

    /** CACTUS: 15 attempts of bush(TENDRIL, 4) at the doubled height. */
    private static void decorateCactus(ServerLevel level, RandomSource random, int minX, int minZ) {
        for (int i = 0; i < 15; i++) {
            BlockPos column = at(random, minX, minZ);
            int bound = doubledHeight(level, column);
            if (bound <= 0) {
                continue;
            }
            int y = random.nextInt(bound);
            if (y <= level.getMinY() || y >= level.getMaxY() - 1) {
                continue;
            }
            WorldGenParasiteBush.of("tendril", 4)
                    .generate(level, random, new BlockPos(column.getX(), y, column.getZ()));
        }
    }

    /** LAKE_WATER: 50 attempts, {@code WorldGenLiquids(Blocks.WATER)}. */
    private static void decorateWaterLakes(ServerLevel level, RandomSource random, int minX, int minZ) {
        for (int i = 0; i < 50; i++) {
            BlockPos column = at(random, minX, minZ);
            int bound = random.nextInt(248) + 8;
            if (bound <= 0) {
                continue;
            }
            int y = random.nextInt(bound);
            if (y <= level.getMinY() || y >= level.getMaxY() - 1) {
                continue;
            }
            placeLake(level, new BlockPos(column.getX(), y, column.getZ()), Blocks.WATER);
        }
    }

    /** LAKE_LAVA: 20 attempts with the original triple-nested random bound. */
    private static void decorateLavaLakes(ServerLevel level, RandomSource random, int minX, int minZ) {
        for (int i = 0; i < 20; i++) {
            BlockPos column = at(random, minX, minZ);
            int y = random.nextInt(random.nextInt(random.nextInt(240) + 8) + 8);
            if (y <= level.getMinY() || y >= level.getMaxY() - 1) {
                continue;
            }
            placeLake(level, new BlockPos(column.getX(), y, column.getZ()), Blocks.LAVA);
        }
    }

    private static WorldGenParasiteBush grassGenerator(RandomSource random) {
        return random.nextInt(2) == 0 ? BUSH_POP : BUSH_EYE;
    }

    private static int doubledHeight(ServerLevel level, BlockPos column) {
        return Math.max(1, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                column.getX(), column.getZ()) * 2);
    }

    /** {@code chunkPos.add(rand.nextInt(16) + 8, 0, rand.nextInt(16) + 8)} — may reach one chunk over. */
    private static BlockPos at(RandomSource random, int minX, int minZ) {
        return new BlockPos(minX + random.nextInt(CHUNK_SIZE) + 8, 0,
                minZ + random.nextInt(CHUNK_SIZE) + 8);
    }

    // ---------------------------------------------------------------------------------------------
    // Shrouded flora (BiomeParasiteShrouded#spawnGenFeatureParasite / spawnGenRoofParasite)
    // ---------------------------------------------------------------------------------------------

    /**
     * The {@code convertBlock} grass branch called the two flora spawners for every converted grass
     * block.  The 1.10.9 implementation was a probability ladder; the port seeds it from the chunk
     * random and runs it once per converted column (the exact per-block roll would be far heavier
     * than the original's terrain-time call and is not reproducible without the original's RNG
     * stream, which the moved decoration cannot share).
     */
    private static void decorateShroudedFlora(ServerLevel level, LevelChunk chunk, RandomSource random,
            int minX, int minZ) {
        int attempts = 24;
        for (int i = 0; i < attempts; i++) {
            BlockPos column = at(random, minX, minZ);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            spawnShroudedFlora(level, random, pos);
            spawnShroudedRoofFlora(level, random, pos.below());
        }
    }

    /** {@code srp_shrouded} only: the other three biomes left this method empty. */
    public static void spawnShroudedFlora(ServerLevel level, RandomSource random, BlockPos position) {
        BlockPos pos = position;
        double bonus = 0.0D;
        BlockState here = ParasiteGenContext.get(level, pos);
        if (!here.isAir() && here.getFluidState().is(FluidTags.WATER)) {
            BlockPos floor = ParasiteGenContext.floor(level, pos, 10);
            bonus = 3.0E-5D;
            if (floor == null) {
                return;
            }
            pos = floor;
        }

        if (random.nextDouble() < 0.0015D) {
            if (random.nextDouble() < 0.2D) {
                if (!WorldGenParasiteTreeThin.generate(level, random, pos)
                        && ParasiteGenContext.isAir(level, pos)) {
                    ParasiteGenContext.setBlock(level, pos, saplingState("tree"));
                }
            } else if (!WorldGenParasiteTree.generate(level, random, pos)
                    && ParasiteGenContext.isAir(level, pos)) {
                ParasiteGenContext.setBlock(level, pos, saplingState("treethin"));
            }
        } else if (random.nextDouble() < 0.001D) {
            if (!WorldGenParasiteTallFlower.generate(level, random, pos)
                    && ParasiteGenContext.isAir(level, pos)) {
                ParasiteGenContext.setBlock(level, pos, saplingState("flowertall"));
            }
        } else if (random.nextDouble() < 5.0E-4D) {
            WorldGenParasiteSpine.generate(level, random, pos);
        } else if (random.nextDouble() < 1.0E-4D) {
            WorldGenParasiteTenFlower.generate(level, random, pos);
        } else if (random.nextDouble() < 7.0E-5D + bonus) {
            WorldGenParasiteBall.generate(level, random, pos);
        } else if (random.nextDouble() < 2.0E-5D + bonus) {
            WorldGenParasiteBigBall.generate(level, random, pos);
        } else {
            if (random.nextInt(500) == 0) {
                WorldGenParasiteBush.of("tendril", 4).generate(level, random, pos);
            }
            if (random.nextInt(250) == 0) {
                switch (random.nextInt(3)) {
                    case 0 -> WorldGenParasiteBush.of("pop", 1).generate(level, random, pos);
                    case 1 -> WorldGenParasiteBush.of("eye", 2).generate(level, random, pos);
                    default -> WorldGenParasiteBush.of("tooh", 3).generate(level, random, pos);
                }
            }
            if (random.nextDouble() < 3.0E-4D) {
                WorldGenParasiteMouth.generate(level, random, pos);
            }
        }
    }

    /** {@code spawnGenRoofParasite}: a chain of {@code parasitebush} BINE blocks hanging downwards. */
    public static void spawnShroudedRoofFlora(ServerLevel level, RandomSource random, BlockPos position) {
        BlockPos pos = position;
        if (!ParasiteGenContext.isAir(level, pos)) {
            return;
        }
        ParasiteGenContext.setBlock(level, pos, vineState());
        pos = pos.below();
        if (!ParasiteGenContext.isAir(level, pos)) {
            return;
        }
        ParasiteGenContext.setBlock(level, pos, vineState());
        pos = pos.below();
        if (random.nextInt(2) != 0) {
            return;
        }
        if (!ParasiteGenContext.isAir(level, pos)) {
            return;
        }
        ParasiteGenContext.setBlock(level, pos, vineState());
        pos = pos.below();
        if (random.nextInt(2) != 0) {
            return;
        }
        if (!ParasiteGenContext.isAir(level, pos)) {
            return;
        }
        ParasiteGenContext.setBlock(level, pos, vineState());
        pos = pos.below();
        if (random.nextInt(2) != 0) {
            return;
        }
        if (!ParasiteGenContext.isAir(level, pos)) {
            return;
        }
        ParasiteGenContext.setBlock(level, pos, vineState());
    }

    private static BlockState saplingState(String variant) {
        return ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasitesapling").get(), variant);
    }

    private static BlockState vineState() {
        return ParasiteGenContext.variantState(ParasiteGenContext.PARASITE_BUSH, "bine");
    }

    // ---------------------------------------------------------------------------------------------
    // Small vanilla-feature stand-ins
    // ---------------------------------------------------------------------------------------------

    private static void placeLilyPad(ServerLevel level, BlockPos pos) {
        if (!ParasiteGenContext.get(level, pos).isAir()
                || !ParasiteGenContext.get(level, pos.below()).getFluidState().is(FluidTags.WATER)) {
            return;
        }
        ParasiteGenContext.setBlock(level, pos, Blocks.LILY_PAD.defaultBlockState());
    }

    private static void placeBushShroom(ServerLevel level, BlockPos pos) {
        if (!ParasiteGenContext.get(level, pos).isAir()
                || !ParasiteGenContext.isSolidGround(level, pos.below())) {
            return;
        }
        ParasiteGenContext.setBlock(level, pos, ModBlocks.GOTHSHROOM.get().defaultBlockState());
    }

    /**
     * The old {@code WorldGenReed}: sugar cane needs an adjacent water block and grows 2..3 tall.
     * The vanilla feature is not reachable from here, so its conditions are inlined.
     */
    private static void placeReed(ServerLevel level, BlockPos pos) {
        if (!ParasiteGenContext.get(level, pos).isAir()) {
            return;
        }
        boolean waterBelow = ParasiteGenContext.get(level, pos.below()).getFluidState().is(FluidTags.WATER);
        if (!waterBelow && !ParasiteGenContext.isSolidGround(level, pos.below())) {
            return;
        }
        boolean nextToWater = waterBelow
                || ParasiteGenContext.get(level, pos.north()).getFluidState().is(FluidTags.WATER)
                || ParasiteGenContext.get(level, pos.south()).getFluidState().is(FluidTags.WATER)
                || ParasiteGenContext.get(level, pos.east()).getFluidState().is(FluidTags.WATER)
                || ParasiteGenContext.get(level, pos.west()).getFluidState().is(FluidTags.WATER);
        if (!nextToWater) {
            return;
        }
        int height = 2 + level.getRandom().nextInt(2);
        for (int i = 0; i < height; i++) {
            BlockPos cane = pos.above(i);
            if (!ParasiteGenContext.get(level, cane).isAir()) {
                break;
            }
            ParasiteGenContext.setBlock(level, cane, Blocks.SUGAR_CANE.defaultBlockState());
        }
    }

    /**
     * The old {@code WorldGenLiquids} blob.  The vanilla feature is not reachable from here, so the
     * port writes the same 3x3x5 shell of fluid it produced.
     */
    private static void placeLake(ServerLevel level, BlockPos pos, Block fluid) {
        for (int dy = -1; dy <= 4; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos target = pos.offset(dx, dy, dz);
                    if (!ParasiteGenContext.inWorld(level, target)) {
                        continue;
                    }
                    if (Math.abs(dx) == 1 && Math.abs(dz) == 1 && level.getRandom().nextBoolean()) {
                        continue;
                    }
                    BlockState existing = ParasiteGenContext.get(level, target);
                    Block block = existing.getBlock();
                    if (block != Blocks.AIR && block != Blocks.STONE && block != Blocks.DIRT
                            && block != Blocks.GRAVEL && block != Blocks.SAND
                            && block != Blocks.SANDSTONE && !existing.canBeReplaced()) {
                        continue;
                    }
                    ParasiteGenContext.setBlock(level, target, fluid.defaultBlockState());
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Per-biome block conversion (BiomeParasite* tables + convertBlock)
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code BiomeParasiteBase#convertBlock} for every storage position of the chunk.  Only blocks
     * the 1.12.2 tables replaced are touched; the Shrouded table is the {@code srp_shrouded} palette,
     * the Harlequin table the {@code srp_harlequinn} palette, and Boils/Demen keep their terrain
     * (the original left all of their block getters null).
     */
    private static void convertChunk(ServerLevel level, LevelChunk chunk, BiomeSurface surface,
            RandomSource random, int minX, int minZ) {
        if (surface.flask() == Flask.NONE) {
            return;
        }
        boolean harlequin = surface.flask() == Flask.HARLEQUIN;
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = minX + x;
                int worldZ = minZ + z;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
                int limit = Math.min(maxY - 1, top);
                for (int y = Math.max(minY, limit - 96); y <= limit; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    BlockState state = ParasiteGenContext.get(level, pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (harlequin) {
                        convertHarlequin(level, pos, state, random, worldX, y, worldZ);
                    } else {
                        convertShrouded(level, pos, state, random, worldX, y, worldZ);
                    }
                }
            }
        }
    }

    /** Harlequin palette: harlequinn_grass ground, harleskinn stone, alveoli leaves, feeler plants. */
    private static void convertHarlequin(ServerLevel level, BlockPos pos, BlockState state,
            RandomSource random, int worldX, int y, int worldZ) {
        Block block = state.getBlock();

        if (block == ModBlocks.INFESTED_STAIN.get()) {
            ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.HARLEQUINN_GRASS);
            return;
        }
        if (ParasiteGenContext.isDirtMaterial(block) || ParasiteGenContext.isSand(block)
                || ParasiteGenContext.isSandstone(block) || ParasiteGenContext.isGravel(block)) {
            ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.HARLEQUINN_GRASS);
            return;
        }
        if (ParasiteGenContext.isStoneMaterial(block) || block == Blocks.COBBLESTONE
                || block == Blocks.MOSSY_COBBLESTONE) {
            ParasiteGenContext.setBlock(level, pos, ModBlocks.HARLESKINN_BLOCK.get().defaultBlockState());
            return;
        }
        if (block == Blocks.OBSIDIAN) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "obsidian"));
            return;
        }
        if (ParasiteGenContext.isBrick(block)) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "bricks"));
            return;
        }
        if (block == ModBlocks.INFESTED_TRUNK.get() || ParasiteGenContext.isWoodLike(level, pos)) {
            ParasiteGenContext.setBlock(level, pos, ModBlocks.PARASITETRUNK.get().defaultBlockState());
            return;
        }
        if (ParasiteGenContext.isLeaves(level, pos)) {
            ParasiteGenContext.setBlock(level, pos, ModBlocks.ALVEOLI.get().defaultBlockState());
            if (random.nextInt(100) < 30 && ParasiteGenContext.get(level, pos.below()).isAir()) {
                ParasiteGenContext.setBlock(level, pos.below(),
                        ModBlocks.ALVEOLI_GROWTH.get().defaultBlockState());
            }
            return;
        }
        if (ParasiteGenContext.isPlant(level, pos)) {
            ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.STAIN_FLESH);
            return;
        }
        if (ParasiteGenContext.isIce(block)) {
            ParasiteGenContext.setBlock(level, pos,
                    ModBlocks.legacyBlock("bloodyice").get().defaultBlockState());
            return;
        }
        if (ParasiteGenContext.isIron(level, pos)) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "metal"));
            return;
        }
        if (ParasiteGenContext.isWoodMaterial(level, pos)) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "wood"));
        }
    }

    /** Shrouded palette: dirt/mud/sand/gravel stain, stone rubble, plants and ice replaced. */
    private static void convertShrouded(ServerLevel level, BlockPos pos, BlockState state,
            RandomSource random, int worldX, int y, int worldZ) {
        Block block = state.getBlock();

        if (block == ModBlocks.INFESTED_STAIN.get()) {
            ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.STAIN_DIRT);
            return;
        }
        if (block == ModBlocks.INFESTED_SAND.get()) {
            ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.STAIN_MUD);
            return;
        }
        if (ParasiteGenContext.isDirtMaterial(block)) {
            ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.STAIN_DIRT);
            if (block == Blocks.GRASS_BLOCK && random.nextInt(100) < 30) {
                spawnShroudedFlora(level, random, new BlockPos(worldX, y + 1, worldZ));
                spawnShroudedRoofFlora(level, random, new BlockPos(worldX, y, worldZ));
            }
            return;
        }
        if (ParasiteGenContext.isSand(block)) {
            ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.STAIN_MUD);
            return;
        }
        if (ParasiteGenContext.isGravel(block)) {
            ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.STAIN_SACKFLESH);
            return;
        }
        if (ParasiteGenContext.isSandstone(block)) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "flesh"));
            return;
        }
        if (block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "bone"));
            return;
        }
        if (ParasiteGenContext.isStoneMaterial(block) || ParasiteGenContext.isOre(block)) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "stone"));
            return;
        }
        if (block == Blocks.OBSIDIAN) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "obsidian"));
            return;
        }
        if (ParasiteGenContext.isBrick(block)) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "bricks"));
            return;
        }
        if (block == ModBlocks.INFESTED_TRUNK.get() || ParasiteGenContext.isWoodLike(level, pos)) {
            ParasiteGenContext.setBlock(level, pos, ModBlocks.PARASITETRUNK.get().defaultBlockState());
            return;
        }
        if (ParasiteGenContext.isLeaves(level, pos)) {
            // The Shrouded leaves entry was "minecraft:air": the original removed them.
            ParasiteGenContext.setAir(level, pos);
            return;
        }
        if (ParasiteGenContext.isPlant(level, pos)) {
            ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.STAIN_FLESH);
            return;
        }
        if (ParasiteGenContext.isIce(block)) {
            ParasiteGenContext.setBlock(level, pos,
                    ModBlocks.legacyBlock("bloodyice").get().defaultBlockState());
            return;
        }
        if (ParasiteGenContext.isIron(level, pos)) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "metal"));
            return;
        }
        if (ParasiteGenContext.isWoodMaterial(level, pos)) {
            ParasiteGenContext.setBlock(level, pos,
                    ParasiteGenContext.variantState(ModBlocks.legacyBlock("parasiterubble").get(), "wood"));
        }
    }
}
