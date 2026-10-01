package alku.csrp.world;

import alku.csrp.Config;
import alku.csrp.config.GeneralConfig;
import alku.csrp.config.WorldConfig;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.CommonModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of the original {@code SRPWorldParasiteSpawner}.
 *
 * <p>The original did not feed parasites through the biome monster list while its custom spawner was
 * active: it replaced the world spawn call with its own routine that walked the 17x17 chunk area
 * around every player, drew a weighted entry from the SRP phase table, and spawned it under SRP's own
 * caps. The first reconstruction instead only injected candidates into vanilla's {@code MONSTER} list,
 * so parasites competed with every vanilla monster for one shared mob cap; because that cap is
 * computed per chunk and is normally already saturated, parasites almost never appeared.
 *
 * <p>This restores the original pipeline:
 *
 * <ul>
 *   <li>a 17x17 chunk neighbourhood with the border ring excluded
 *   <li>a shuffled chunk list and {@code ceil(rand*4)} attempts per cluster position, three times plus
 *       a 30% chance of a fourth pass
 *   <li>the "no player within 24 blocks" and 576-block-from-world-spawn exclusions
 *   <li>SRP table selection rather than the biome monster list
 *   <li>the {@code IN_AIR} 70% rejection and nearest-player height relocation
 *   <li>the original per-chunk {@code getMaxSpawnPackSize} early exit
 * </ul>
 *
 * <p>Deliberate mappings: the original's per-dimension caps lived in its {@code CheckSpawn} handler
 * and are applied here by {@link SpawnCounts}; and the original let aquatic parasites override
 * {@code isNotColliding} to drop the "hitbox contains liquid" requirement, reproduced by
 * {@link #passesSpawnChecks}.
 */
public final class SrpWorldParasiteSpawner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SrpWorldParasiteSpawner.class);

    private static final int SPAWN_CHUNK_RADIUS = 8;
    private static final int MIN_SPAWN_DISTANCE = 24;
    private static final int CLUSTER_ATTEMPTS = 3;
    private static final float EXTRA_CLUSTER_CHANCE = 0.30F;
    private static final int AIR_SPAWN_REJECT_PERCENT = 70;
    /** Original {@code worker > 10} gate for the Worker (Kol) caste. */
    private static final int WORKER_CAP = 10;
    /**
     * Original {@code SRPSpawning} warm-up. {@code findChunksForSpawning} dispatches on
     * {@code SRPConfigWorld.originActivated}, which defaults to true, and that origin variant uses
     * {@code lock > 7}; the plain variant's {@code lock > 40} only applies when origins are off.
     */
    private static final int WARMUP_PASSES = 7;

    private static final ResourceLocation WORKER_ID = ResourceLocation.parse("csrp:worker");
    private static final ResourceLocation GNAT_ID = ResourceLocation.parse("csrp:gnat");
    private static final ResourceLocation LICE_ID = ResourceLocation.parse("csrp:lice");

    /** Original {@code SRPSpawning.totalParasites} plus its warm-up counter. */
    private static boolean totalParasites;
    private static int lock;
    private static final Set<ChunkPos> ELIGIBLE_CHUNKS = new HashSet<>();

    private SrpWorldParasiteSpawner() {
    }

    /**
     * Original {@code findChunksForSpawning}. NeoForge has no world-level equivalent of 1.12.2's
     * {@code WorldEntitySpawner.findChunksForSpawning}, so this runs once per level tick from
     * {@link EvolutionEvents}.
     *
     * @return the number of parasites added to the level this pass
     */
    public static int findChunksForSpawning(ServerLevel level) {
        if (!GeneralConfig.allowMobs()
                || !WorldConfig.dimensionAllowsNaturalSpawning(level)
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || level.getServer().getWorldData().isDebugWorld()) {
            return 0;
        }
        if (!totalParasites) {
            if (++lock > WARMUP_PASSES) {
                totalParasites = true;
                lock = 0;
            }
            return 0;
        }

        // The original enforced this cap from its CheckSpawn handler with Result.DENY; here the same
        // outcome is reached by not starting a pass while the dimension is at its cap.
        SpawnCounts counts = SpawnCounts.of(level);
        if (!counts.underTotalCap(level)) {
            return 0;
        }

        ELIGIBLE_CHUNKS.clear();
        collectEligibleChunks(level);
        if (ELIGIBLE_CHUNKS.isEmpty()) {
            return 0;
        }

        BlockPos worldSpawn = level.getSharedSpawnPos();
        List<ChunkPos> shuffled = new ArrayList<>(ELIGIBLE_CHUNKS);
        shuffle(shuffled, level.random);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        int total = 0;
        chunkLoop:
        for (ChunkPos chunkPos : shuffled) {
            BlockPos start = getRandomChunkPosition(level, chunkPos.x, chunkPos.z);
            int startX = start.getX();
            int startY = start.getY();
            int startZ = start.getZ();
            if (level.getBlockState(start).isRedstoneConductor(level, start)) {
                continue;
            }

            // The original tracked a per-chunk pack count separately from the running total.
            int packSize = 0;
            int clusterAttempts = CLUSTER_ATTEMPTS
                    + (level.random.nextFloat() < EXTRA_CLUSTER_CHANCE ? 1 : 0);
            for (int cluster = 0; cluster < clusterAttempts; cluster++) {
                int x = startX;
                int z = startZ;
                MobSpawnSettings.SpawnerData spawnData = null;
                SpawnGroupData groupData = null;
                int attempts = Mth.ceil(level.random.nextFloat() * 4.0F);

                for (int attempt = 0; attempt < attempts; attempt++) {
                    x += level.random.nextInt(6) - level.random.nextInt(6);
                    z += level.random.nextInt(6) - level.random.nextInt(6);
                    int y = startY;
                    mutable.set(x, y, z);
                    float centerX = x + 0.5F;
                    float centerZ = z + 0.5F;

                    // Original gate: no living player within 24 blocks, and at least 576 blocks
                    // away from the shared world spawn.
                    if (level.hasNearbyAlivePlayer(centerX, y, centerZ, MIN_SPAWN_DISTANCE)
                            || worldSpawn.distToCenterSqr(centerX, y, centerZ) < 576.0D) {
                        continue;
                    }
                    if (spawnData == null) {
                        spawnData = pickSpawnEntry(level, mutable);
                        if (spawnData == null) {
                            break;
                        }
                    }

                    BlockPos spawnPos = repositionForAirSpawn(level, mutable, spawnData, centerX, y, centerZ);
                    if (!isValidSpawnPosition(level, spawnData, spawnPos)) {
                        continue;
                    }
                    if (!counts.allows(level, spawnData.type)) {
                        continue;
                    }
                    Mob mob = createParasite(level, spawnData);
                    if (mob == null) {
                        return total;
                    }
                    mob.moveTo(centerX, spawnPos.getY(), centerZ, level.random.nextFloat() * 360.0F, 0.0F);
                    if (!passesSpawnChecks(level, mob, spawnData.type)) {
                        continue;
                    }

                    // Fires FinalizeSpawnEvent, so the port's legacy light/spawnDays validity gate
                    // (ParasiteCombatRules#enforceLegacySpawnValidity) still applies.
                    groupData = EventHooks.finalizeMobSpawn(mob, level,
                            level.getCurrentDifficultyAt(mob.blockPosition()),
                            MobSpawnType.NATURAL, groupData);
                    if (mob.isSpawnCancelled()) {
                        continue;
                    }
                    counts.record(spawnData.type);
                    packSize++;
                    total++;
                    level.addFreshEntityWithPassengers(mob);
                    if (packSize >= EventHooks.getMaxSpawnClusterSize(mob)) {
                        continue chunkLoop;
                    }
                }
            }
        }
        return total;
    }

    /** Fisher-Yates over the level's own random source (the original used {@code Collections.shuffle}). */
    private static <T> void shuffle(List<T> list, net.minecraft.util.RandomSource random) {
        for (int i = list.size(); i > 1; i--) {
            int j = random.nextInt(i);
            T tmp = list.get(i - 1);
            list.set(i - 1, list.get(j));
            list.set(j, tmp);
        }
    }

    /** The original 17x17 walk; the outer ring was excluded, leaving the inner 15x15. */
    private static void collectEligibleChunks(ServerLevel level) {
        for (Player player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            int centerX = player.getBlockX() >> 4;
            int centerZ = player.getBlockZ() >> 4;
            for (int dx = -SPAWN_CHUNK_RADIUS; dx <= SPAWN_CHUNK_RADIUS; dx++) {
                for (int dz = -SPAWN_CHUNK_RADIUS; dz <= SPAWN_CHUNK_RADIUS; dz++) {
                    boolean border = dx == -SPAWN_CHUNK_RADIUS || dx == SPAWN_CHUNK_RADIUS
                            || dz == -SPAWN_CHUNK_RADIUS || dz == SPAWN_CHUNK_RADIUS;
                    if (border) {
                        continue;
                    }
                    ChunkPos chunkPos = new ChunkPos(dx + centerX, dz + centerZ);
                    if (ELIGIBLE_CHUNKS.contains(chunkPos)
                            || !level.getWorldBorder().isWithinBounds(chunkPos)) {
                        continue;
                    }
                    if (level.getChunkSource().isPositionTicking(chunkPos.toLong())) {
                        ELIGIBLE_CHUNKS.add(chunkPos);
                    }
                }
            }
        }
    }

    /**
     * Pick a usable terrain position inside the chunk. The old port sampled Y uniformly from the
     * build floor to the surface, which made almost every ground attempt start inside a solid block
     * and left natural parasites effectively limited to the few caves hit by chance.
     */
    private static BlockPos getRandomChunkPosition(ServerLevel level, int chunkX, int chunkZ) {
        int x = chunkX * 16 + level.random.nextInt(16);
        int z = chunkZ * 16 + level.random.nextInt(16);
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        int surface = chunk == null
                ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
                : chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int y = Mth.clamp(surface, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        return new BlockPos(x, y, z);
    }

    /**
     * Original {@code SRPSpawning.getSpawns} plus {@code WeightedRandom.getRandomItem}. Phase and
     * ubiquitous-development gating, including the phase -1 EIV requirement and the 50% UD roll,
     * lives in {@link NaturalSpawnTables}.
     */
    private static MobSpawnSettings.SpawnerData pickSpawnEntry(ServerLevel level, BlockPos pos) {
        var entries = NaturalSpawnTables.select(level, pos);
        if (entries.isEmpty()) {
            return null;
        }
        List<MobSpawnSettings.SpawnerData> usable = new ArrayList<>(entries.size());
        for (MobSpawnSettings.SpawnerData entry : entries) {
            if (EvolutionSystem.crossDimensionUnlocked(level, pathOf(entry.type))) {
                usable.add(entry);
            }
        }
        if (usable.isEmpty()) {
            return null;
        }
        return WeightedRandom.getRandomItem(level.random, usable).orElse(null);
    }

    /**
     * Original {@code IN_AIR} branch: 70% of air attempts are dropped, and survivors are relocated
     * around the nearest player's height. The original's {@code getClosestPlayer} here deliberately
     * picks the nearest player <em>farther</em> than 24 blocks.
     */
    private static BlockPos repositionForAirSpawn(ServerLevel level, BlockPos.MutableBlockPos pos,
                                                  MobSpawnSettings.SpawnerData spawnData,
                                                  double centerX, int y, double centerZ) {
        if (!isAirSpawnType(spawnData.type)) {
            return pos;
        }
        if (level.random.nextFloat() * 100.0F <= AIR_SPAWN_REJECT_PERCENT) {
            return pos;
        }
        Player nearest = nearestPlayerBeyond(level, centerX, y, centerZ, MIN_SPAWN_DISTANCE);
        if (nearest == null) {
            return pos;
        }
        double base = Math.max(nearest.getY(), level.getSeaLevel());
        base = Math.min(base, WorldConfig.spawnerSkyLimitUp());
        int randomOffset = level.random.nextInt(21) - 10;
        pos.set(pos.getX(), (int) base + randomOffset, pos.getZ());
        return pos;
    }

    /** Original {@code getClosestPlayer}: nearest player farther than {@code distance} blocks. */
    private static Player nearestPlayerBeyond(ServerLevel level, double x, double y, double z, double distance) {
        double best = -1.0D;
        Player result = null;
        double threshold = distance * distance;
        for (Player player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double d = player.distanceToSqr(x, y, z);
            if (d > threshold && (best == -1.0D || d < best)) {
                best = d;
                result = player;
            }
        }
        return result;
    }

    /** Original {@code canCreatureTypeSpawnAtLocation}: border plus placement and body checks. */
    private static boolean isValidSpawnPosition(ServerLevel level, MobSpawnSettings.SpawnerData spawnData, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        EntityType<?> type = spawnData.type;
        if (isAirSpawnType(type)) {
            return isEmptyColumnBlock(level, pos.below())
                    && isEmptyColumnBlock(level, pos)
                    && isEmptyColumnBlock(level, pos.above());
        }
        if (!SpawnPlacements.isSpawnPositionOk(type, level, pos)) {
            return false;
        }
        AABB box = type.getSpawnAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        return level.noCollision(box);
    }

    private static boolean isEmptyColumnBlock(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        return fluid.isEmpty()
                && NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, fluid, EntityType.PIG);
    }

    /**
     * Original {@code EntityParasiteBase.func_70058_J} ({@code isNotColliding}), reached after the
     * original's {@code ForgeEventFactory.canEntitySpawn} call. NeoForge's
     * {@code EventHooks#checkSpawnPosition} performs exactly that pairing — it fires the position
     * check event (which the port uses for its table and phase gates) and then falls back to
     * {@code checkSpawnRules} plus {@code checkSpawnObstruction}.
     *
     * <p>The aquatic parasites are the exception: their original classes overrode
     * {@code isNotColliding} to drop the "hitbox contains liquid" requirement, while
     * {@code Mob#checkSpawnObstruction} still applies it. Those two types therefore get the same
     * event and rules, but the obstruction test is limited to {@code isUnobstructed}.
     */
    private static boolean passesSpawnChecks(ServerLevel level, Mob mob, EntityType<?> type) {
        if (!isWaterSpawnType(type)) {
            return EventHooks.checkSpawnPosition(mob, level, MobSpawnType.NATURAL);
        }
        MobSpawnEvent.PositionCheck event =
                new MobSpawnEvent.PositionCheck(mob, level, MobSpawnType.NATURAL, null);
        NeoForge.EVENT_BUS.post(event);
        if (event.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL) {
            return false;
        }
        if (event.getResult() == MobSpawnEvent.PositionCheck.Result.SUCCEED) {
            return true;
        }
        return mob.checkSpawnRules(level, MobSpawnType.NATURAL) && level.isUnobstructed(mob);
    }

    private static Mob createParasite(ServerLevel level, MobSpawnSettings.SpawnerData spawnData) {
        try {
            if (spawnData.type.create(level) instanceof Mob mob && mob instanceof Parasite) {
                return mob;
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to create parasite {}", BuiltInRegistries.ENTITY_TYPE.getKey(spawnData.type), exception);
        }
        return null;
    }

    private static boolean isAirSpawnType(EntityType<?> type) {
        return CommonModEvents.AIR_SPAWN_IDS.contains(pathOf(type));
    }

    private static boolean isWaterSpawnType(EntityType<?> type) {
        return CommonModEvents.WATER_SPAWN_IDS.contains(pathOf(type));
    }

    private static String pathOf(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id == null ? "" : id.getPath();
    }

    /** Clears the warm-up lock so a fresh world does not inherit the previous world's state. */
    public static void reset() {
        totalParasites = false;
        lock = 0;
        ELIGIBLE_CHUNKS.clear();
        SpawnCounts.clearCache();
    }

    /**
     * The original per-dimension caps from {@code DimensionHandler.onSpawn}: the natural spawn cap,
     * the Gnat/Lice cap, the Worker gate, and the aquatic and flying caps. The original rescanned the
     * loaded entity list on every spawn candidate; this port samples it once per pass at most, and
     * reuses the sample for a short window so a busy world is not walked once per tick.
     */
    private static final class SpawnCounts {
        /** Ticks a sampled entity census stays valid before it is rebuilt. */
        private static final int SAMPLE_CACHE_TICKS = 20;
        private static final Map<ResourceKey<Level>, Cached> CACHE = new HashMap<>();

        private int total;
        private int gnats;
        private int workers;
        private int water;
        private int air;

        private record Cached(SpawnCounts counts, long expiresAt) {
        }

        static void clearCache() {
            CACHE.clear();
        }

        static SpawnCounts of(ServerLevel level) {
            long now = level.getGameTime();
            Cached cached = CACHE.get(level.dimension());
            if (cached != null && cached.expiresAt > now) {
                return new SpawnCounts(cached.counts);
            }
            SpawnCounts counts = new SpawnCounts();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob mob && mob instanceof Parasite) {
                    counts.record(mob.getType());
                }
            }
            CACHE.put(level.dimension(), new Cached(new SpawnCounts(counts), now + SAMPLE_CACHE_TICKS));
            return counts;
        }

        SpawnCounts() {
        }

        SpawnCounts(SpawnCounts other) {
            this.total = other.total;
            this.gnats = other.gnats;
            this.workers = other.workers;
            this.water = other.water;
            this.air = other.air;
        }

        void record(EntityType<?> type) {
            total++;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id != null && (id.equals(GNAT_ID) || id.equals(LICE_ID))) {
                gnats++;
            }
            if (id != null && id.equals(WORKER_ID)) {
                workers++;
            }
            if (isWaterSpawnType(type)) {
                water++;
            } else if (isAirSpawnType(type)) {
                air++;
            }
        }

        /**
         * The original denied a spawn when {@code count > worldSpawningMobCap + players}, so a
         * dimension may hold up to and including the cap. This port's config documents 0 as
         * "disable the cap", matching how {@code EvolutionEvents} already treats it.
         */
        boolean underTotalCap(ServerLevel level) {
            int cap = WorldConfig.naturalMobCap(level);
            return cap <= 0 || total <= cap;
        }

        boolean allows(ServerLevel level, EntityType<?> type) {
            if (!underTotalCap(level)) {
                return false;
            }
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (isGnatLike(id) && exceeds(gnats, Config.worldGnatCap())) {
                return false;
            }
            if (id != null && id.equals(WORKER_ID) && exceeds(workers, WORKER_CAP)) {
                return false;
            }
            if (isWaterSpawnType(type) && exceeds(water, WorldConfig.naturalWaterMobCap())) {
                return false;
            }
            return !isAirSpawnType(type) || !exceeds(air, WorldConfig.naturalAirMobCap());
        }

        private static boolean isGnatLike(ResourceLocation id) {
            return id != null && (id.equals(GNAT_ID) || id.equals(LICE_ID));
        }

        /** The original tests {@code count > cap} for these categorical caps. */
        private static boolean exceeds(int count, int cap) {
            return count > cap;
        }
    }
}
