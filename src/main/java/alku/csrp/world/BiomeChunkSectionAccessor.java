package alku.csrp.world;

import java.lang.reflect.Field;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;

/**
 * Allows code to replace biome cells inside a chunk section (implemented by
 * {@link LevelChunkSectionMixin}).
 */
public final class BiomeChunkSectionAccessor {
    private static final Field BIOMES_FIELD = findBiomesField();

    private BiomeChunkSectionAccessor() {
    }

    public static void setBiome(LevelChunkSection section, int x, int y, int z,
            Holder<Biome> biome) {
        try {
            Object value = BIOMES_FIELD.get(section);
            if (value instanceof PalettedContainerRO<?> readOnly
                    && readOnly instanceof PalettedContainer<?> container) {
                @SuppressWarnings("unchecked")
                PalettedContainer<Holder<Biome>> typed =
                        (PalettedContainer<Holder<Biome>>) container;
                typed.getAndSet(x, y, z, biome);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access LevelChunkSection biomes", e);
        }
    }

    private static Field findBiomesField() {
        for (String name : new String[] {"biomes", "f_187995_"}) {
            try {
                Field field = LevelChunkSection.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new IllegalStateException("LevelChunkSection biomes field not found");
    }
}
