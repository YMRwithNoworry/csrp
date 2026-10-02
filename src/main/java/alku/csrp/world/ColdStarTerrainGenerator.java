package alku.csrp.world;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class ColdStarTerrainGenerator {
    private static final long SALT = 0x4CF5AD432745937FL;

    private ColdStarTerrainGenerator() {
    }

    public static void generate(ServerLevel level, ChunkAccess chunk, boolean fracturedTerrain, boolean mushroomTrees) {
        if (ColdStarVillageGenerator.isVillageArea(level.getSeed(), chunk.getPos())) {
            return;
        }
        RandomSource random = RandomSource.create(level.getSeed() ^ chunk.getPos().toLong() * SALT);
        int originX = chunk.getPos().getMinBlockX();
        int originZ = chunk.getPos().getMinBlockZ();

        if (fracturedTerrain) {
            fracture(level, random, originX, originZ);
            scatterBarrenGround(level, random, originX, originZ);
        }
        if (mushroomTrees && random.nextInt(7) == 0) {
            growMushroomTree(level, random, originX, originZ);
        }
        chunk.setUnsaved(true);
    }

    private static void fracture(ServerLevel level, RandomSource random, int originX, int originZ) {
        int x = originX + 3 + random.nextInt(10);
        int z = originZ + 3 + random.nextInt(10);
        int stepX = random.nextBoolean() ? 1 : 0;
        int stepZ = stepX == 1 ? (random.nextBoolean() ? 1 : -1) : 1;
        int remaining = stepX == 1
                ? Math.min(originX + 15 - x, stepZ == 1 ? originZ + 15 - z : z - originZ)
                : originZ + 15 - z;
        int length = Math.min(5 + random.nextInt(7), remaining + 1);
        for (int step = 0; step < length; step++) {
            int centerX = x + step * stepX;
            int centerZ = z + step * stepZ;
            int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, centerX, centerZ) - 1;
            int depth = 4 + random.nextInt(8);
            for (int offset = -1; offset <= 1; offset++) {
                int carveX = centerX + (stepX == 1 ? 0 : offset);
                int carveZ = centerZ + (stepX == 1 ? offset : 0);
                for (int y = Math.max(level.getMinBuildHeight() + 1, groundY - depth); y <= groundY; y++) {
                    BlockPos pos = new BlockPos(carveX, y, carveZ);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.STONE) || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                            || state.is(Blocks.GRAVEL) || state.is(Blocks.DEEPSLATE)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static void scatterBarrenGround(ServerLevel level, RandomSource random, int originX, int originZ) {
        for (int i = 0; i < 5; i++) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockPos pos = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(pos);
            if (ground.is(Blocks.GRASS_BLOCK)) {
                level.setBlock(pos, random.nextBoolean() ? Blocks.COARSE_DIRT.defaultBlockState()
                        : Blocks.PODZOL.defaultBlockState(), 2);
            }
        }
    }

    private static void growMushroomTree(ServerLevel level, RandomSource random, int originX, int originZ) {
        int x = originX + 3 + random.nextInt(10);
        int z = originZ + 3 + random.nextInt(10);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos base = new BlockPos(x, y, z);
        BlockState ground = level.getBlockState(base.below());
        if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.DIRT) && !ground.is(Blocks.PODZOL)
                && !ground.is(Blocks.COARSE_DIRT) && !ground.is(Blocks.MYCELIUM)) {
            return;
        }
        int trunkHeight = 4 + random.nextInt(3);
        BlockState trunk = ModBlocks.DEADHEAD_TRUNK.get().defaultBlockState();
        for (int dy = 0; dy < trunkHeight; dy++) {
            level.setBlock(base.above(dy), trunk, 2);
        }
        BlockState leaves = ModBlocks.DEADHEAD_LEAVES.get().defaultBlockState()
                .setValue(BlockStateProperties.PERSISTENT, true);
        int crownY = trunkHeight - 1;
        for (int dy = -1; dy <= 2; dy++) {
            int radius = dy == 2 ? 1 : dy == -1 ? 2 : 3;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && random.nextBoolean()) {
                        continue;
                    }
                    BlockPos pos = base.offset(dx, crownY + dy, dz);
                    if (level.isEmptyBlock(pos)) {
                        level.setBlock(pos, leaves, 2);
                    }
                }
            }
        }
    }
}
