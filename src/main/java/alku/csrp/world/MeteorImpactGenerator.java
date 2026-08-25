package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;

public final class MeteorImpactGenerator {
    private static final int MIN_SAFE_Y_OFFSET = 5;

    private MeteorImpactGenerator() {
    }

    public static BlockPos surface(ServerLevel level, BlockPos around) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, around.getX(), around.getZ()) - 1;
        return new BlockPos(around.getX(), Math.max(level.getMinBuildHeight() + MIN_SAFE_Y_OFFSET, y), around.getZ());
    }

    public static void generateMain(ServerLevel level, BlockPos impact, RandomSource random) {
        stainArrivalColumn(level, impact.below(10), 35, 20);
        carveArrivalVolume(level, impact, random);
        float steepness = 0.25F + random.nextFloat() * 0.75F;
        double yaw = random.nextFloat() * Math.PI * 2.0D;
        double directionX = -Math.sin(yaw);
        double directionZ = Math.cos(yaw);
        TunnelResult tunnel = carveTunnel(level, impact.below(3), 10, 30,
                directionX, -steepness, directionZ);

        int radius = 40;
        int depth = (int) (radius * (0.4F + random.nextFloat() * 0.2F));
        if (tunnel.anyBroken()) {
            depth = Math.max(depth, impact.getY() - tunnel.lowestY() + 3);
        }
        radius = Math.max(radius, (int) (depth * 1.6F));
        clearVegetation(level, impact, radius * 2, depth);
        carveBowl(level, impact, radius, depth, random);
        scorch(level, impact, radius, random);
        ejecta(level, impact, radius, directionX, directionZ, random);
        microCraters(level, impact, radius, directionX, directionZ, random);

        int bottomY = Math.max(level.getMinBuildHeight() + MIN_SAFE_Y_OFFSET, impact.getY() - depth + 1);
        deadBloodPool(level, new BlockPos(impact.getX(), bottomY, impact.getZ()),
                Math.max(4, radius / 6), 10, 4);
        BlockPos structureOrigin = new BlockPos(impact.getX(), bottomY, impact.getZ())
                .above(14).offset(-24, 0, -24);
        if (MeteorStructureLoader.place(level, "meteor", structureOrigin, random)) {
            postProcessMainStructure(level, structureOrigin.offset(22, 14, 22), random);
        }
        scheduleNearbyWater(level, impact, radius, depth);
    }

    public static void generateFragment(ServerLevel level, BlockPos impact, RandomSource random) {
        String structure = switch (random.nextInt(9)) {
            case 1 -> "meteor_fragment_large2";
            case 2 -> "meteor_fragment_large3";
            case 3 -> "meteor_fragment_small1";
            case 4 -> "meteor_fragment_small2";
            case 5 -> "meteor_fragment_small3";
            case 6 -> "meteor_fragment_small4";
            case 7 -> "meteor_fragment_small5";
            case 8 -> "meteor_fragment_small6";
            default -> "meteor_fragment_large1";
        };
        MeteorStructureLoader.place(level, structure, impact, random);
        int fires = 18 + random.nextInt(18);
        for (int index = 0; index < fires; index++) {
            int x = random.nextInt(21) - 10;
            int z = random.nextInt(21) - 10;
            if (x * x + z * z <= 100) {
                placeFire(level, surface(level, impact.offset(x, 0, z)).above());
            }
        }
    }

    private static void stainArrivalColumn(ServerLevel level, BlockPos start, int radius, int height) {
        int radiusSqr = radius * radius;
        for (int y = 0; y < height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z > radiusSqr) {
                        continue;
                    }
                    BlockPos pos = start.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (!state.isAir() && !id.getNamespace().equals(Csrp.MODID)) {
                        replace(level, pos, stain());
                    }
                }
            }
        }
    }

    private static void carveArrivalVolume(ServerLevel level, BlockPos impact, RandomSource random) {
        int y = Math.max(impact.getY() - 10, level.getMinBuildHeight() + 86);
        int radius = 1;
        for (int layer = 0; layer < 80; layer++, y++, radius++) {
            carveArrivalLayer(level, impact.getX(), y, impact.getZ(), radius, random);
        }
        for (int layer = 0; layer < 80; layer++, y++) {
            carveArrivalLayer(level, impact.getX(), y, impact.getZ(), radius, random);
        }
        for (int layer = 0; layer < 6; layer++, y++, radius--) {
            carveArrivalLayer(level, impact.getX(), y, impact.getZ(), radius, random);
        }
    }

    private static void carveArrivalLayer(ServerLevel level, int centerX, int y, int centerZ,
                                           int radius, RandomSource random) {
        carveArrivalDisk(level, centerX, y, centerZ, radius, 2, random);
        carveArrivalDisk(level, centerX, y, centerZ, radius - 2, 50_000, random);
    }

    private static void carveArrivalDisk(ServerLevel level, int centerX, int y, int centerZ,
                                          int radius, int incomplete, RandomSource random) {
        if (radius <= 0 || y <= level.getMinBuildHeight() + MIN_SAFE_Y_OFFSET
                || y >= level.getMaxBuildHeight()) {
            return;
        }
        int radiusSqr = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radiusSqr && random.nextInt(incomplete) != 0) {
                    BlockPos pos = new BlockPos(centerX + x, y, centerZ + z);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void carveBowl(ServerLevel level, BlockPos center, int radius, int depth, RandomSource random) {
        int radiusSqr = radius * radius;
        int minimumY = level.getMinBuildHeight() + MIN_SAFE_Y_OFFSET;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distanceSqr = x * x + z * z;
                if (distanceSqr > radiusSqr) {
                    continue;
                }
                double distance = Math.sqrt(distanceSqr);
                int localTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                        center.getX() + x, center.getZ() + z);
                int floorY = Math.max(minimumY,
                        localTop - (int) Math.round(depth * Math.pow(1.0D - distance / radius, 2.0D)));
                for (int y = localTop - 1; y > floorY; y--) {
                    clear(level, new BlockPos(center.getX() + x, y, center.getZ() + z));
                }
                BlockPos floor = new BlockPos(center.getX() + x, floorY, center.getZ() + z);
                int coreRadius = Math.max(2, (int) (radius * 0.28F));
                if (distanceSqr <= coreRadius * coreRadius) {
                    replace(level, floor, random.nextInt(3) == 0 ? stain() : cookedFlesh());
                } else if (distance >= radius - 1.5D) {
                    replace(level, floor, residue());
                } else if (distance >= radius * 0.55D && random.nextInt(4) == 0) {
                    replace(level, floor, stain());
                }
            }
        }
    }

    private static TunnelResult carveTunnel(ServerLevel level, BlockPos start, int radius, int length,
                                            double directionX, double directionY, double directionZ) {
        Vec direction = new Vec(directionX, directionY, directionZ).normalize();
        int radiusSqr = radius * radius;
        boolean anyBroken = false;
        int lowestY = Integer.MAX_VALUE;
        for (int step = 0; step <= length; step++) {
            BlockPos center = start.offset((int) Math.round(direction.x * step),
                    (int) Math.round(direction.y * step), (int) Math.round(direction.z * step));
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x * x + y * y + z * z <= radiusSqr) {
                            BlockPos pos = center.offset(x, y, z);
                            if (clearAndReport(level, pos)) {
                                anyBroken = true;
                                lowestY = Math.min(lowestY, pos.getY());
                            }
                        }
                    }
                }
            }
        }
        return new TunnelResult(anyBroken, anyBroken ? lowestY : start.getY());
    }

    private static void clearVegetation(ServerLevel level, BlockPos center, int radius, int depth) {
        int radiusSqr = radius * radius;
        int minimumY = Math.max(level.getMinBuildHeight() + MIN_SAFE_Y_OFFSET,
                center.getY() - depth - 12);
        int maximumY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + 50);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radiusSqr) {
                    continue;
                }
                for (int y = minimumY; y <= maximumY; y++) {
                    BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();
                    if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || isWooden(state)
                            || block instanceof BushBlock || block instanceof VineBlock) {
                        clear(level, pos);
                    }
                }
            }
        }
    }

    private static void scorch(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        int inner = (int) (radius * 1.15F);
        int outer = (int) (radius * 1.45F);
        for (int x = -outer; x <= outer; x++) {
            for (int z = -outer; z <= outer; z++) {
                int distanceSqr = x * x + z * z;
                if (distanceSqr >= inner * inner && distanceSqr <= outer * outer && random.nextInt(3) == 0) {
                    replace(level, surface(level, center.offset(x, 0, z)), stain());
                }
            }
        }
    }

    private static void ejecta(ServerLevel level, BlockPos center, int radius, double directionX,
                               double directionZ, RandomSource random) {
        int minimum = (int) (radius * 1.1F);
        int maximum = (int) (radius * 3.2F);
        for (int index = 0; index < radius * 30; index++) {
            double distance = minimum + random.nextInt(Math.max(1, maximum - minimum));
            double spread = (random.nextDouble() - 0.5D) * 1.25D;
            int x = (int) Math.round(directionX * distance - directionZ * distance * spread);
            int z = (int) Math.round(directionZ * distance + directionX * distance * spread);
            replace(level, surface(level, center.offset(x, 0, z)), random.nextInt(5) == 0 ? stain() : rubble());
        }
    }

    private static void microCraters(ServerLevel level, BlockPos center, int radius, double directionX,
                                     double directionZ, RandomSource random) {
        for (int index = 0; index < Math.max(3, radius / 3); index++) {
            double distance = radius * (1.2D + random.nextDouble() * 2.0D);
            double spread = (random.nextDouble() - 0.5D) * 1.6D;
            int x = (int) Math.round(directionX * distance - directionZ * distance * spread);
            int z = (int) Math.round(directionZ * distance + directionX * distance * spread);
            int smallRadius = 2 + random.nextInt(3);
            int radiusSqr = smallRadius * smallRadius;
            for (int localX = -smallRadius; localX <= smallRadius; localX++) {
                for (int localZ = -smallRadius; localZ <= smallRadius; localZ++) {
                    if (localX * localX + localZ * localZ <= radiusSqr && random.nextInt(3) == 0) {
                        BlockPos point = center.offset(x + localX, 0, z + localZ);
                        replace(level, surface(level, point), stain());
                    }
                }
            }
        }
    }

    private static void postProcessMainStructure(ServerLevel level, BlockPos center, RandomSource random) {
        int range = 11;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    Block block = level.getBlockState(pos).getBlock();
                    if (block instanceof AbstractGlassBlock || block instanceof IronBarsBlock) {
                        set(level, pos, stain());
                        continue;
                    }
                    if (block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK
                            || block == Blocks.DIAMOND_BLOCK) {
                        set(level, pos, lootForMarker(block, random));
                        set(level, pos.above(), ModBlocks.DEAD_BLOOD.get().defaultBlockState());
                    }
                }
            }
        }
    }

    private static BlockState lootForMarker(Block marker, RandomSource random) {
        int rareRoll = marker == Blocks.DIAMOND_BLOCK ? 4 : marker == Blocks.GOLD_BLOCK ? 7 : 10;
        int uncommonRoll = marker == Blocks.DIAMOND_BLOCK ? 2 : marker == Blocks.GOLD_BLOCK ? 3 : 4;
        if (random.nextInt(rareRoll) == 0) {
            return ModBlocks.PARASITE_LOOT_RARE.get().defaultBlockState();
        }
        if (random.nextInt(uncommonRoll) == 0) {
            return ModBlocks.PARASITE_LOOT_UNCOMMON.get().defaultBlockState();
        }
        return ModBlocks.PARASITE_LOOT_COMMON.get().defaultBlockState();
    }

    private static void deadBloodPool(ServerLevel level, BlockPos center, int radius,
                                      int dryCoreRadius, int height) {
        int radiusSqr = radius * radius;
        int dryCoreSqr = dryCoreRadius * dryCoreRadius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distanceSqr = x * x + z * z;
                if (distanceSqr <= radiusSqr && distanceSqr > dryCoreSqr) {
                    for (int y = 0; y <= height; y++) {
                        BlockPos pos = center.offset(x, y, z);
                        if (level.getBlockState(pos).isAir()) {
                            set(level, pos, ModBlocks.DEAD_BLOOD.get().defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static void scheduleNearbyWater(ServerLevel level, BlockPos center, int radius, int depth) {
        int range = (int) (radius * 1.8F) + 12;
        int minY = Math.max(level.getMinBuildHeight() + 1, center.getY() - depth - 6);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + 24);
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
                    BlockState state = level.getBlockState(pos);
                    if (state.getFluidState().is(FluidTags.WATER)) {
                        level.scheduleTick(pos, state.getFluidState().getType(), 1);
                        level.updateNeighborsAt(pos, state.getBlock());
                    }
                }
            }
        }
    }

    private static void placeFire(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        clearAndReport(level, pos);
    }

    private static boolean clearAndReport(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || pos.getY() <= level.getMinBuildHeight() + MIN_SAFE_Y_OFFSET
                || level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        if (!existing.isAir() && existing.getDestroySpeed(level, pos) >= 0.0F) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }

    private static void replace(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.isInWorldBounds(pos) || level.getBlockEntity(pos) != null) {
            return;
        }
        BlockState existing = level.getBlockState(pos);
        if (existing.getDestroySpeed(level, pos) >= 0.0F) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.isInWorldBounds(pos) && level.getBlockEntity(pos) == null
                && level.getBlockState(pos).getDestroySpeed(level, pos) >= 0.0F) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private static BlockState stain() {
        return ModBlocks.INFESTED_STAIN.get().defaultBlockState();
    }

    private static BlockState cookedFlesh() {
        return ModBlocks.COOKED_FLESH.get().defaultBlockState();
    }

    private static BlockState rubble() {
        return ModBlocks.INFESTED_RUBBLE.get().defaultBlockState();
    }

    private static BlockState residue() {
        return ModBlocks.RESIDUE_BRICKS.get().defaultBlockState();
    }

    private static boolean isWooden(BlockState state) {
        return state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_BUTTONS)
                || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.WOODEN_PRESSURE_PLATES) || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_TRAPDOORS);
    }

    private record Vec(double x, double y, double z) {
        private Vec normalize() {
            double length = Math.sqrt(x * x + y * y + z * z);
            return length < 1.0E-6D ? new Vec(0.0D, -1.0D, 0.0D)
                    : new Vec(x / length, y / length, z / length);
        }
    }

    private record TunnelResult(boolean anyBroken, int lowestY) {
    }
}
