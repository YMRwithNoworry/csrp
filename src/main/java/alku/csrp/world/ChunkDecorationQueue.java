package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.world.gen.HarlequinRockBushGen;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the ported parasite biome decoration exactly once per newly generated chunk.
 *
 * <p>{@code ChunkEvent.Load} fires <em>inside</em> the chunk's {@code FULL} generation step: the
 * NeoForge javadoc states that the chunk has not been promoted yet and that interacting with the
 * level there can deadlock the server.  The event handler therefore only records the chunk position,
 * and the actual decoration happens on the next server tick, when the chunk is guaranteed to be
 * fully loaded.</p>
 *
 * <p>The per-chunk marker lives in {@link SrpWorldData}
 * ({@link SrpWorldData#markChunkDecorated(int, int)}), so a chunk that was already decorated in an
 * earlier session is never decorated twice — this also covers chunks generated while the mod was
 * absent or while the star type changed.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ChunkDecorationQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(Csrp.MODID + "/decoration");

    /** Chunks queued for this tick's drain; the set de-duplicates repeated load events. */
    private static final Set<Long> QUEUED = new HashSet<>();
    private static final Deque<Pending> PENDING = new ArrayDeque<>();

    /** How long a queued chunk keeps being retried before it is dropped (200 ticks = 10 s). */
    private static final long RETRY_WINDOW_TICKS = 200L;
    /** Drain budget per tick; a non-parasite chunk costs one biome sample and is discarded. */
    private static final int MAX_DRAIN_PER_TICK = 64;

    private ChunkDecorationQueue() {
    }

    private record Pending(int chunkX, int chunkZ, long queuedAtTick) {
    }

    /** The sea-level sample used to pre-filter chunks without queueing the whole world. */
    private static final int SAMPLE_Y = 64;

    /** Queue a chunk for decoration; callable from {@code ChunkEvent.Load}. */
    public static void enqueue(ServerLevel level, int chunkX, int chunkZ) {
        long key = ChunkPos.pack(chunkX, chunkZ);
        synchronized (QUEUED) {
            if (!QUEUED.add(key)) {
                return;
            }
            PENDING.add(new Pending(chunkX, chunkZ, level.getGameTime()));
        }
    }

    /**
     * {@code ChunkEvent.Load} fires for every new chunk in the world, so the cheap sea-level sample
     * is used to skip the ones that are not parasite biomes — the full surface scan happens when the
     * chunk is actually decorated.
     *
     * <p>A cold/warm star chunk is queued as well: {@link StarBiomeGenerationEvents} converts the
     * vanilla biomes of every new chunk and this listener may run before that conversion.  The
     * drain re-checks the biome, so a chunk that did not end up parasite is dropped immediately.</p>
     */
    public static void enqueueIfParasite(ServerLevel level, LevelChunk chunk) {
        BlockPos sample = new BlockPos(chunk.getPos().getMinBlockX() + 8, SAMPLE_Y,
                chunk.getPos().getMinBlockZ() + 8);
        if (ParasiteBiomeDecorator.parasiteBiomeAt(chunk, sample) != null) {
            enqueue(level, chunk.getPos().x(), chunk.getPos().z());
            return;
        }
        if (SrpWorldData.get(level).starType() != SrpStarType.NORMAL) {
            enqueue(level, chunk.getPos().x(), chunk.getPos().z());
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        enqueueIfParasite(level, event.getChunk());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (PENDING.isEmpty()) {
            return;
        }
        int drained = 0;
        while (drained < MAX_DRAIN_PER_TICK) {
            Pending pending;
            synchronized (QUEUED) {
                pending = PENDING.poll();
            }
            if (pending == null) {
                return;
            }
            drained++;
            long key = ChunkPos.pack(pending.chunkX(), pending.chunkZ());
            if (level.getGameTime() - pending.queuedAtTick() > RETRY_WINDOW_TICKS) {
                synchronized (QUEUED) {
                    QUEUED.remove(key);
                }
                continue;
            }
            if (!decorate(level, pending)) {
                // Not loaded yet: put it back at the tail so one stuck chunk cannot block the rest.
                synchronized (QUEUED) {
                    PENDING.add(pending);
                }
                continue;
            }
            synchronized (QUEUED) {
                QUEUED.remove(key);
            }
        }
    }

    /** @return {@code true} when the chunk needs no further attempt. */
    private static boolean decorate(ServerLevel level, Pending pending) {
        if (!level.hasChunk(pending.chunkX(), pending.chunkZ())) {
            return false;
        }
        LevelChunk chunk = level.getChunk(pending.chunkX(), pending.chunkZ());
        ParasiteBiomeDecorator.BiomeSurface surface = ParasiteBiomeDecorator.surfaceOf(chunk);
        if (surface == null) {
            return true;
        }
        SrpWorldData data = SrpWorldData.get(level);
        if (!data.markChunkDecorated(pending.chunkX(), pending.chunkZ())) {
            return true;
        }
        try {
            ParasiteBiomeDecorator.decorate(level, chunk, surface);
            if (surface.flask() == ParasiteBiomeDecorator.Flask.HARLEQUIN) {
                HarlequinRockBushGen.generate(level, level.getRandom(), chunk.getPos());
            }
        } catch (RuntimeException error) {
            // A crash inside chunk generation takes the server down; decoration is cosmetic, so a
            // failure is logged and the chunk is left alone (its marker stays set).
            LOGGER.error("Parasite biome decoration failed for chunk {} {}",
                    pending.chunkX(), pending.chunkZ(), error);
        }
        chunk.markUnsaved();
        return true;
    }
}
