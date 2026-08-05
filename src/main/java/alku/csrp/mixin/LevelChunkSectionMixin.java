package alku.csrp.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSectionMixin implements BiomeChunkSectionAccessor {
    @Shadow
    private PalettedContainerRO<Holder<Biome>> biomes;

    @Override
    public void csrp$setBiome(int x, int y, int z, Holder<Biome> biome) {
        if (this.biomes instanceof PalettedContainer<Holder<Biome>> container) {
            container.getAndSet(x, y, z, biome);
        }
    }
}
