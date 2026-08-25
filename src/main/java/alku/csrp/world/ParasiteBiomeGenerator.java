package alku.csrp.world;

import alku.csrp.infection.InfestationSpreadLimiter;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Procedural port of the parasite biome: around an active Biome Heart it
 * spreads infected ground, residue plants, thornshades, alveoli growth,
 * parasite material blocks and small Dead Blood pools.
 */
public final class ParasiteBiomeGenerator {
    static final int RADIUS = 32;

    private ParasiteBiomeGenerator() {
    }

    public static void generateAround(ServerLevel level, BlockPos center, int stage) {
        RandomSource random = level.getRandom();
        int placements = 4 + stage * 3;
        for (int attempt = 0; attempt < placements * 3; attempt++) {
            if (placements <= 0 || !InfestationSpreadLimiter.canSpread(level,
                    InfestationSpreadLimiter.Type.BIOME)) {
                break;
            }
            BlockPos candidate = center.offset(
                    random.nextInt(RADIUS * 2 + 1) - RADIUS,
                    0,
                    random.nextInt(RADIUS * 2 + 1) - RADIUS);
            candidate = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
            if (candidate.distSqr(center) > (double) RADIUS * RADIUS) {
                continue;
            }
            if (placeVegetation(level, candidate, random)) {
                placements--;
                InfestationSpreadLimiter.record(level, InfestationSpreadLimiter.Type.BIOME, 1);
            }
        }
        if (stage >= 2 && random.nextInt(4) == 0
                && InfestationSpreadLimiter.canSpread(level, InfestationSpreadLimiter.Type.BIOME)) {
            InfestationSpreadLimiter.record(level, InfestationSpreadLimiter.Type.BIOME,
                    placeBloodPool(level, center, random));
        }
        applyParasiteBiome(level, center, stage);
    }

    /** 原版 4 个寄生群系（Boils/Demen/Harlequinn/Shrouded）：感染成熟后按区块连片替换。 */
    private static final List<ResourceKey<Biome>> PARASITE_BIOMES = List.of(
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("csrp", "srp_boils")),
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("csrp", "srp_demen")),
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("csrp", "srp_harlequinn")),
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("csrp", "srp_shrouded")));

    private static void applyParasiteBiome(ServerLevel level, BlockPos center, int stage) {
        if (stage < 2) {
            return;
        }
        Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        int chunkRadius = (RADIUS + 15) >> 4;
        for (int offsetX = -chunkRadius; offsetX <= chunkRadius; offsetX++) {
            for (int offsetZ = -chunkRadius; offsetZ <= chunkRadius; offsetZ++) {
                int chunkX = (center.getX() >> 4) + offsetX;
                int chunkZ = (center.getZ() >> 4) + offsetZ;
                ResourceKey<Biome> key = PARASITE_BIOMES.get(
                        Math.floorMod(chunkX * 31 + chunkZ, PARASITE_BIOMES.size()));
                Holder<Biome> biome = registry.getHolderOrThrow(key);
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (LevelChunkSection section : chunk.getSections()) {
                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }
                    BiomeChunkSectionAccessor accessor = (BiomeChunkSectionAccessor) section;
                    for (int x = 0; x < 4; x++) {
                        for (int y = 0; y < 4; y++) {
                            for (int z = 0; z < 4; z++) {
                                accessor.csrp$setBiome(x, y, z, biome);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean placeVegetation(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState existing = level.getBlockState(pos);
        if (!existing.isAir() && !existing.canBeReplaced()) {
            return false;
        }
        int roll = random.nextInt(100);
        BlockState state;
        if (roll < 28) {
            state = ModBlocks.INFESTED_STAIN.get().defaultBlockState();
            level.setBlock(pos.below(), state, 3);
            return true;
        }
        if (roll < 46) {
            state = ModBlocks.RESIDUE_PLANTS.get().defaultBlockState();
        } else if (roll < 62) {
            state = ModBlocks.THORNSHADE.get().defaultBlockState();
        } else if (roll < 74) {
            state = ModBlocks.ALVEOLI_GROWTH.get().defaultBlockState();
        } else if (roll < 82) {
            state = ModBlocks.LOCS_BLOCK.get().defaultBlockState();
        } else if (roll < 89) {
            state = ModBlocks.BLADDER_SAC.get().defaultBlockState();
        } else if (roll < 95) {
            state = ModBlocks.GOTHSHROOM.get().defaultBlockState();
        } else if (roll < 98) {
            state = ModBlocks.HARLESKINN_BLOCK.get().defaultBlockState();
        } else {
            state = ModBlocks.POLAND_SKIN_BLOCK.get().defaultBlockState();
        }
        level.setBlock(pos, state, 3);
        return true;
    }

    private static int placeBloodPool(ServerLevel level, BlockPos center, RandomSource random) {
        int radius = 2 + random.nextInt(2);
        int placed = 0;
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                center.offset(random.nextInt(25) - 12, 0, random.nextInt(25) - 12));
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) {
                    continue;
                }
                BlockPos pos = surface.offset(x, 0, z);
                if (level.getBlockState(pos).canBeReplaced()
                        && !level.getBlockState(pos.below()).isAir()) {
                    if (level.setBlock(pos, ModBlocks.DEAD_BLOOD.get().defaultBlockState(), 3)) {
                        placed++;
                    }
                }
            }
        }
        return placed;
    }
}
