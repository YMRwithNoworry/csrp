package alku.csrp.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Keeps the world-generation decoration passes from force-loading chunks.
 *
 * <p>{@code Level#getBlockState}, {@code Level#getHeightmapPos}, {@code Level#getHeight} and
 * {@code Level#setBlock} all resolve their chunk through {@code Level#getChunk(x, z,
 * ChunkStatus.FULL, true)}, which <em>synchronously loads</em> a missing chunk. When that happens
 * inside a {@code ChunkEvent.Load} dispatch the forced load posts {@code ChunkEvent.Load} for the
 * neighbour, the handler runs inline (because {@code MinecraftServer#execute} executes immediately
 * when it is already on the server thread) and the decoration pass re-enters itself:
 *
 * <pre>
 * ColdStarTreeHandler.findNormalTreePosition
 *   -&gt; Level.getHeightmapPos -&gt; ChunkMap load -&gt; ChunkEvent.Load
 *   -&gt; StarBiomeGenerationEvents.convertNewChunk -&gt; ColdStarTreeHandler.decorate
 *   -&gt; ColdStarTreeHandler.findNormalTreePosition -&gt; ...
 * </pre>
 *
 * <p>That recursion is unbounded and ends in a {@link StackOverflowError} while the world is
 * generating. Every position a decoration pass reads or writes must therefore already be inside a
 * loaded chunk; {@link #isLoaded} and {@link #isLoadedArea} are the non-loading checks that make
 * that possible ({@code Level#hasChunk} resolves through {@code ChunkStatus.FULL} with
 * {@code requireChunk = false}).
 */
final class GenerationChunkGuard {
    private GenerationChunkGuard() {
    }

    /** True when the chunk holding {@code (x, z)} is loaded, so touching it cannot force a load. */
    static boolean isLoaded(ServerLevel level, int x, int z) {
        return level.hasChunk(x >> 4, z >> 4);
    }

    /** True when the chunk holding {@code pos} is loaded. */
    static boolean isLoaded(ServerLevel level, BlockPos pos) {
        return isLoaded(level, pos.getX(), pos.getZ());
    }

    /** True when every chunk between {@code min} and {@code max} (both inclusive) is loaded. */
    static boolean isLoadedArea(ServerLevel level, BlockPos min, BlockPos max) {
        return level.hasChunksAt(min, max);
    }
}
