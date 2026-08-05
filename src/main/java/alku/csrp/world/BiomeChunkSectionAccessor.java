package alku.csrp.world;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * Allows code to replace biome cells inside a chunk section (implemented by
 * {@link LevelChunkSectionMixin}).
 */
public interface BiomeChunkSectionAccessor {
    void csrp$setBiome(int x, int y, int z, Holder<Biome> biome);
}
