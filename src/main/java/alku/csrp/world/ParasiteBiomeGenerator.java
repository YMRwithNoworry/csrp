package alku.csrp.world;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Procedural port of the parasite biome: around an active Biome Heart it
 * spreads infected ground, residue plants, thornshades, alveoli growth,
 * parasite material blocks and small Dead Blood pools.
 */
public final class ParasiteBiomeGenerator {
    private static final int RADIUS = 32;

    private ParasiteBiomeGenerator() {
    }

    public static void generateAround(ServerLevel level, BlockPos center, int stage) {
        RandomSource random = level.getRandom();
        int placements = 4 + stage * 3;
        for (int attempt = 0; attempt < placements * 3; attempt++) {
            if (placements <= 0) {
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
            }
        }
        if (stage >= 2 && random.nextInt(4) == 0) {
            placeBloodPool(level, center, random);
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

    private static void placeBloodPool(ServerLevel level, BlockPos center, RandomSource random) {
        int radius = 2 + random.nextInt(2);
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
                    level.setBlock(pos, ModBlocks.DEAD_BLOOD.get().defaultBlockState(), 3);
                }
            }
        }
    }
}
