package alku.csrp.world;

import alku.csrp.Csrp;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class StarBiomeGenerationEvents {
    private static final Set<ResourceKey<Biome>> OCEANS = Set.of(
            Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN,
            Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
            Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN);
    private static final Set<ResourceKey<Biome>> COLD_FORESTS = Set.of(
            Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.TAIGA, Biomes.JUNGLE,
            Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.CHERRY_GROVE);
    private static final Set<ResourceKey<Biome>> MOUNTAINS = Set.of(
            Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
            Biomes.WINDSWEPT_SAVANNA, Biomes.MEADOW, Biomes.GROVE, Biomes.SNOWY_SLOPES,
            Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS,
            Biomes.SAVANNA_PLATEAU, Biomes.WOODED_BADLANDS, Biomes.ERODED_BADLANDS);

    private StarBiomeGenerationEvents() {
    }

    @SubscribeEvent
    public static void convertNewChunk(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        SrpStarType starType = SrpWorldData.get(level).starType();
        if (starType == SrpStarType.NORMAL) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        replaceBiomes(level, chunk, starType);
        if (starType == SrpStarType.WARM) {
            dryWarmStarChunk(chunk);
        } else if (ColdStarVillageGenerator.isVillageChunk(level.getSeed(), chunk.getPos())) {
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;
            level.getServer().execute(() -> ColdStarVillageGenerator.generate(level, chunkX, chunkZ));
        }
        chunk.setUnsaved(true);
    }

    private static void replaceBiomes(ServerLevel level, ChunkAccess chunk, SrpStarType starType) {
        Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        for (LevelChunkSection section : chunk.getSections()) {
            BiomeChunkSectionAccessor mutable = (BiomeChunkSectionAccessor) section;
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    for (int z = 0; z < 4; z++) {
                        Holder<Biome> source = section.getNoiseBiome(x, y, z);
                        ResourceKey<Biome> sourceKey = source.unwrapKey().orElse(Biomes.PLAINS);
                        ResourceKey<Biome> targetKey = starType == SrpStarType.COLD
                                ? coldTarget(sourceKey) : warmTarget(sourceKey);
                        mutable.csrp$setBiome(x, y, z, biomes.getHolderOrThrow(targetKey));
                    }
                }
            }
        }
    }

    private static ResourceKey<Biome> coldTarget(ResourceKey<Biome> source) {
        if (OCEANS.contains(source)) return Biomes.FROZEN_OCEAN;
        if (source == Biomes.RIVER) return Biomes.FROZEN_RIVER;
        if (source == Biomes.BEACH || source == Biomes.STONY_SHORE) return Biomes.SNOWY_BEACH;
        if (COLD_FORESTS.contains(source)) return Biomes.SNOWY_TAIGA;
        if (MOUNTAINS.contains(source) || source == Biomes.BADLANDS) return Biomes.FROZEN_PEAKS;
        if (source == Biomes.ICE_SPIKES) return Biomes.ICE_SPIKES;
        if (source == Biomes.SNOWY_TAIGA) return Biomes.SNOWY_TAIGA;
        if (source == Biomes.SNOWY_BEACH) return Biomes.SNOWY_BEACH;
        return Biomes.SNOWY_PLAINS;
    }

    private static ResourceKey<Biome> warmTarget(ResourceKey<Biome> source) {
        if (source == Biomes.RIVER || source == Biomes.FROZEN_RIVER) return Biomes.DESERT;
        if (OCEANS.contains(source) || source == Biomes.MUSHROOM_FIELDS) return Biomes.BADLANDS;
        if (source == Biomes.BEACH || source == Biomes.SNOWY_BEACH) return Biomes.STONY_SHORE;
        if (source == Biomes.DESERT) return Biomes.DESERT;
        if (source == Biomes.BADLANDS) return Biomes.BADLANDS;
        if (source == Biomes.ERODED_BADLANDS) return Biomes.ERODED_BADLANDS;
        if (MOUNTAINS.contains(source)) return Biomes.WOODED_BADLANDS;
        return Biomes.SAVANNA;
    }

    private static void dryWarmStarChunk(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 63; y > 48; y--) {
                    pos.set(startX + x, y, startZ + z);
                    var state = chunk.getBlockState(pos);
                    if (state.getFluidState().is(FluidTags.WATER) || state.is(Blocks.ICE)) {
                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
    }
}
