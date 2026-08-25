package alku.csrp.world;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;

public final class MeteorImpactGenerator {
    private static final int MIN_SAFE_Y_OFFSET = 5;

    private MeteorImpactGenerator() {
    }

    public static BlockPos surface(ServerLevel level, BlockPos around) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, around.getX(), around.getZ()) - 1;
        return new BlockPos(around.getX(), Math.max(level.getMinBuildHeight() + MIN_SAFE_Y_OFFSET, y), around.getZ());
    }

    public static void generateMain(ServerLevel level, BlockPos impact, RandomSource random) {
        float steepness = 0.25F + random.nextFloat() * 0.75F;
        double yaw = random.nextDouble() * Math.PI * 2.0D;
        double directionX = -Math.sin(yaw);
        double directionZ = Math.cos(yaw);
        carveTunnel(level, impact.below(3), 10, 30, directionX, -steepness, directionZ);

        int radius = 40;
        int depth = Math.round(radius * (0.4F + random.nextFloat() * 0.2F));
        carveBowl(level, impact, radius, depth, random);
        scorch(level, impact, radius, random);
        ejecta(level, impact, radius, directionX, directionZ, random);
        microCraters(level, impact, radius, directionX, directionZ, random);

        int bottomY = Math.max(level.getMinBuildHeight() + MIN_SAFE_Y_OFFSET, impact.getY() - depth + 1);
        BlockPos bodyCenter = new BlockPos(impact.getX(), bottomY + 8, impact.getZ());
        meteorBody(level, bodyCenter, random);
        deadBloodPool(level, new BlockPos(impact.getX(), bottomY, impact.getZ()), Math.max(8, radius / 4));
        scheduleNearbyWater(level, impact, radius, depth);
    }

    public static void generateFragment(ServerLevel level, BlockPos impact, RandomSource random) {
        int radius = 3 + random.nextInt(3);
        carveBowl(level, impact, radius, Math.max(2, radius / 2), random);
        BlockPos body = surface(level, impact).above(1);
        fragmentBody(level, body, radius, random);
        int fires = 18 + random.nextInt(18);
        for (int index = 0; index < fires; index++) {
            int x = random.nextInt(21) - 10;
            int z = random.nextInt(21) - 10;
            if (x * x + z * z <= 100) {
                placeFire(level, surface(level, impact.offset(x, 0, z)).above());
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
                double normalized = Math.sqrt(distanceSqr) / radius;
                int localTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        center.getX() + x, center.getZ() + z) - 1;
                int floorY = Math.max(minimumY,
                        localTop - (int) Math.round(depth * (1.0D - normalized) * (1.0D - normalized)));
                for (int y = localTop; y > floorY; y--) {
                    clear(level, new BlockPos(center.getX() + x, y, center.getZ() + z));
                }
                BlockPos floor = new BlockPos(center.getX() + x, floorY, center.getZ() + z);
                if (normalized > 0.94D) {
                    replace(level, floor, residue());
                } else if (normalized < 0.3D || random.nextInt(4) == 0) {
                    replace(level, floor, random.nextBoolean() ? stain() : biomass());
                }
            }
        }
    }

    private static void carveTunnel(ServerLevel level, BlockPos start, int radius, int length,
                                    double directionX, double directionY, double directionZ) {
        Vec direction = new Vec(directionX, directionY, directionZ).normalize();
        int radiusSqr = radius * radius;
        for (int step = 0; step <= length; step++) {
            BlockPos center = start.offset((int) Math.round(direction.x * step),
                    (int) Math.round(direction.y * step), (int) Math.round(direction.z * step));
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x * x + y * y + z * z <= radiusSqr) {
                            clear(level, center.offset(x, y, z));
                        }
                    }
                }
            }
        }
    }

    private static void scorch(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        int inner = Math.round(radius * 1.15F);
        int outer = Math.round(radius * 1.45F);
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
        int minimum = Math.round(radius * 1.1F);
        int maximum = Math.round(radius * 3.2F);
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
            BlockPos point = center.offset(x, 0, z);
            int smallRadius = 2 + random.nextInt(3);
            carveBowl(level, surface(level, point), smallRadius, 2, random);
        }
    }

    private static void meteorBody(ServerLevel level, BlockPos center, RandomSource random) {
        int radius = 11;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = x * x / 121.0D + y * y / 81.0D + z * z / 121.0D;
                    if (distance > 1.0D) {
                        continue;
                    }
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state;
                    if (distance > 0.68D) {
                        state = random.nextInt(5) == 0 ? stain() : residue();
                    } else if (random.nextInt(30) == 0) {
                        state = loot(random);
                    } else {
                        state = random.nextBoolean() ? rubble() : biomass();
                    }
                    set(level, pos, state);
                }
            }
        }
    }

    private static void fragmentBody(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        int yRadius = Math.max(2, radius - 1);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -yRadius; y <= yRadius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = x * x / (double) (radius * radius)
                            + y * y / (double) (yRadius * yRadius) + z * z / (double) (radius * radius);
                    if (distance <= 1.0D && random.nextInt(7) != 0) {
                        set(level, center.offset(x, y, z), distance > 0.55D ? residue() : biomass());
                    }
                }
            }
        }
    }

    private static void deadBloodPool(ServerLevel level, BlockPos center, int radius) {
        int radiusSqr = radius * radius;
        int dryCoreSqr = 9;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distanceSqr = x * x + z * z;
                if (distanceSqr <= radiusSqr && distanceSqr > dryCoreSqr) {
                    BlockPos pos = center.offset(x, 0, z);
                    if (level.getBlockState(pos).isAir()) {
                        set(level, pos, ModBlocks.DEAD_BLOOD.get().defaultBlockState());
                    }
                }
            }
        }
    }

    private static void scheduleNearbyWater(ServerLevel level, BlockPos center, int radius, int depth) {
        int range = Math.round(radius * 1.8F) + 12;
        int minY = Math.max(level.getMinBuildHeight() + 1, center.getY() - depth - 6);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + 24);
        for (int x = -range; x <= range; x += 2) {
            for (int z = -range; z <= range; z += 2) {
                for (int y = minY; y <= maxY; y += 2) {
                    BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty()) {
                        level.scheduleTick(pos, state.getFluidState().getType(), 1);
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
        if (!level.isInWorldBounds(pos) || pos.getY() <= level.getMinBuildHeight() + MIN_SAFE_Y_OFFSET
                || level.getBlockEntity(pos) != null) {
            return;
        }
        BlockState existing = level.getBlockState(pos);
        if (!existing.isAir() && existing.getDestroySpeed(level, pos) >= 0.0F) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
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

    private static BlockState biomass() {
        return ModBlocks.BIOMASS_BLOCK.get().defaultBlockState();
    }

    private static BlockState rubble() {
        return ModBlocks.INFESTED_RUBBLE.get().defaultBlockState();
    }

    private static BlockState residue() {
        return ModBlocks.RESIDUE_BRICKS.get().defaultBlockState();
    }

    private static BlockState loot(RandomSource random) {
        return switch (random.nextInt(10)) {
            case 0 -> ModBlocks.PARASITE_LOOT_RARE.get().defaultBlockState();
            case 1, 2, 3 -> ModBlocks.PARASITE_LOOT_UNCOMMON.get().defaultBlockState();
            default -> ModBlocks.PARASITE_LOOT_COMMON.get().defaultBlockState();
        };
    }

    private record Vec(double x, double y, double z) {
        private Vec normalize() {
            double length = Math.sqrt(x * x + y * y + z * z);
            return length < 1.0E-6D ? new Vec(0.0D, -1.0D, 0.0D)
                    : new Vec(x / length, y / length, z / length);
        }
    }
}
