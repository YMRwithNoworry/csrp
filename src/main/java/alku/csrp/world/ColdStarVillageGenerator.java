package alku.csrp.world;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ColdStarVillageGenerator {
    private static final int DISTANCE = 20;
    private static final int SEPARATION = 5;
    private static final int SALT = 10_387_312;

    private ColdStarVillageGenerator() {
    }

    public static boolean isVillageChunk(long seed, ChunkPos chunkPos) {
        int gridX = Math.floorDiv(chunkPos.x, DISTANCE);
        int gridZ = Math.floorDiv(chunkPos.z, DISTANCE);
        RandomSource random = RandomSource.create(seed
                + gridX * 341_873_128_712L + gridZ * 132_897_987_541L + SALT);
        int range = DISTANCE - SEPARATION;
        int candidateX = gridX * DISTANCE + random.nextInt(range);
        int candidateZ = gridZ * DISTANCE + random.nextInt(range);
        return chunkPos.x == candidateX && chunkPos.z == candidateZ;
    }

    public static void generate(ServerLevel level, int chunkX, int chunkZ) {
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD
                || SrpWorldData.get(level).starType() != SrpStarType.COLD
                || !level.hasChunk(chunkX, chunkZ)) {
            return;
        }
        int centerX = (chunkX << 4) + 8;
        int centerZ = (chunkZ << 4) + 8;
        BlockPos center = surface(level, centerX, centerZ);
        if (!validSite(level, center)) {
            return;
        }

        RandomSource random = RandomSource.create(level.getSeed() ^ new ChunkPos(chunkX, chunkZ).toLong());
        buildWell(level, center);
        int[][] offsets = {{-15, -10}, {13, -9}, {-13, 13}, {14, 12}};
        int houses = 0;
        for (int[] offset : offsets) {
            BlockPos houseCenter = surface(level, centerX + offset[0], centerZ + offset[1]);
            if (validHouseSite(level, houseCenter) && buildHouse(level, houseCenter, random.nextBoolean())) {
                houses++;
            }
        }
        buildWall(level, center, 25, 20);
        spawnVillagers(level, center, Math.max(2, houses + 1));
    }

    private static BlockPos surface(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private static boolean validSite(ServerLevel level, BlockPos center) {
        if (center.getY() <= level.getMinBuildHeight() + 4) return false;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = -8; x <= 8; x += 4) {
            for (int z = -8; z <= 8; z += 4) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        center.getX() + x, center.getZ() + z);
                min = Math.min(min, y);
                max = Math.max(max, y);
                if (!level.getFluidState(new BlockPos(center.getX() + x, y - 1, center.getZ() + z)).isEmpty()) {
                    return false;
                }
            }
        }
        return max - min <= 8;
    }

    private static boolean validHouseSite(ServerLevel level, BlockPos center) {
        return center.getY() > level.getMinBuildHeight() + 4
                && level.getFluidState(center.below()).isEmpty();
    }

    private static void buildWell(ServerLevel level, BlockPos center) {
        BlockState stone = ModBlocks.INFESTED_COBBLESTONE.get().defaultBlockState();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.offset(x, -1, z);
                level.setBlock(pos, stone, 2);
                if (Math.abs(x) < 2 && Math.abs(z) < 2) {
                    level.setBlock(pos.above(), Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }
        for (int x : new int[]{-2, 2}) {
            for (int z : new int[]{-2, 2}) {
                for (int y = 0; y < 4; y++) {
                    level.setBlock(center.offset(x, y, z), stone, 2);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    level.setBlock(center.offset(x, 4, z), ModBlocks.DEADHEAD_PLANKS.get().defaultBlockState(), 2);
                }
            }
        }
    }

    private static boolean buildHouse(ServerLevel level, BlockPos center, boolean turn) {
        BlockState plank = ModBlocks.DEADHEAD_PLANKS.get().defaultBlockState();
        BlockState leaves = ModBlocks.DEADHEAD_LEAVES.get().defaultBlockState();
        BlockState window = Blocks.GLASS_PANE.defaultBlockState();
        int halfX = turn ? 4 : 3;
        int halfZ = turn ? 3 : 4;
        for (int x = -halfX; x <= halfX; x++) {
            for (int z = -halfZ; z <= halfZ; z++) {
                BlockPos floor = center.offset(x, -1, z);
                level.setBlock(floor, plank, 2);
                for (int y = 0; y <= 4; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    boolean edge = Math.abs(x) == halfX || Math.abs(z) == halfZ;
                    if (y == 4) {
                        level.setBlock(pos, leaves, 2);
                    } else if (edge) {
                        boolean door = z == -halfZ && x == 0 && y < 2;
                        boolean glass = y == 2 && ((Math.abs(x) == halfX && z == 0)
                                || (Math.abs(z) == halfZ && x != 0));
                        level.setBlock(pos, door ? Blocks.AIR.defaultBlockState() : glass ? window : plank, 2);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        level.setBlock(center.offset(0, 0, halfZ - 1), Blocks.CHEST.defaultBlockState(), 2);
        level.setBlock(center.offset(1, 0, halfZ - 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        return true;
    }

    private static void buildWall(ServerLevel level, BlockPos center, int halfX, int halfZ) {
        BlockState wall = ModBlocks.INFESTED_RUBBLE.get().defaultBlockState();
        for (int x = -halfX; x <= halfX; x++) {
            placeWallColumn(level, center.getX() + x, center.getZ() - halfZ, wall);
            placeWallColumn(level, center.getX() + x, center.getZ() + halfZ, wall);
        }
        for (int z = -halfZ + 1; z < halfZ; z++) {
            placeWallColumn(level, center.getX() - halfX, center.getZ() + z, wall);
            placeWallColumn(level, center.getX() + halfX, center.getZ() + z, wall);
        }
    }

    private static void placeWallColumn(ServerLevel level, int x, int z, BlockState wall) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int dy = 0; dy < 3; dy++) {
            level.setBlock(new BlockPos(x, y + dy, z), wall, 2);
        }
    }

    private static void spawnVillagers(ServerLevel level, BlockPos center, int count) {
        for (int i = 0; i < count; i++) {
            Villager villager = EntityType.VILLAGER.create(level);
            if (villager == null) continue;
            villager.moveTo(center.getX() + 0.5D + i % 3, center.getY() + 1.0D,
                    center.getZ() + 0.5D + i / 3, 0.0F, 0.0F);
            villager.finalizeSpawn(level, level.getCurrentDifficultyAt(center),
                    MobSpawnType.STRUCTURE, null);
            level.addFreshEntity(villager);
        }
    }
}
