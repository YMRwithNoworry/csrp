package alku.csrp.world;

import alku.csrp.block.SrpCoreBlock;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

/** Procedural port of the legacy colony core and its 13/26-block grid buildings. */
public final class ColonyStructureGenerator {
    private ColonyStructureGenerator() {
    }

    public static BlockPos generateCore(ServerLevel level, BlockPos foundation, RandomSource random) {
        replaceGround(level, foundation.below(), 12);

        int towerHeight = 18 + random.nextInt(5);
        for (int y = 0; y <= towerHeight; y++) {
            double wave = Math.sin(y * 0.55D) * 1.4D;
            int radius = Mth.clamp(4 + Mth.floor(wave), 3, 6);
            hollowRing(level, foundation.above(y), radius, radius,
                    y % 5 == 0 ? dense() : rubble(), flesh());
        }
        helix(level, foundation.above(2), 6.0D, towerHeight + 5, 2, bone());
        helix(level, foundation.above(3), 6.0D, towerHeight + 5, 2, flesh(), Math.PI);

        BlockPos heart = foundation.above(towerHeight + 5);
        hollowSphere(level, heart, 8, 6, 8, dense(), flesh());
        hollowSphere(level, heart, 5, 4, 5, rubble(), Blocks.CAVE_AIR.defaultBlockState());
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2.0D + random.nextDouble() * 0.45D;
            BlockPos end = foundation.offset(Mth.floor(Math.cos(angle) * 11.0D), 0,
                    Mth.floor(Math.sin(angle) * 11.0D));
            organicLine(level, heart.below(2), end, 2, bone());
        }
        set(level, heart, ModBlocks.COLONYHEART.get().defaultBlockState()
                .setValue(SrpCoreBlock.ACTIVE, 1), true);
        return heart;
    }

    public static boolean generateBuilding(ServerLevel level, BlockPos origin, int stage, RandomSource random) {
        if (stage == 1) {
            switch (random.nextInt(4)) {
                case 0 -> generateSpiralTower(level, origin, random);
                case 1 -> generateHelixTower(level, origin, random);
                case 2 -> generateChamberTower(level, origin, random);
                default -> generatePodChain(level, origin, random);
            }
            return true;
        }
        if (stage == 2) {
            switch (random.nextInt(3)) {
                case 0 -> generateWatchPost(level, origin, random);
                case 1 -> generateSpireCluster(level, origin, random);
                default -> generateTwinPods(level, origin, random);
            }
            return true;
        }
        return false;
    }

    private static void generateSpiralTower(ServerLevel level, BlockPos origin, RandomSource random) {
        replaceGround(level, origin.below(), 12);
        int height = 22 + random.nextInt(3);
        for (int y = 0; y < height; y++) {
            int radius = Mth.clamp(5 + Mth.floor(Math.sin(y * 0.34D) * 3.0D), 3, 9);
            hollowRing(level, origin.above(y), radius, radius, bone(), flesh());
        }
        BlockPos crown = origin.above(height + 7 + random.nextInt(5));
        hollowSphere(level, crown, 6, 4, 6, rubble(), flesh());
        helix(level, crown.below(4), 4.0D, 13, 2, bone());
    }

    private static void generateHelixTower(ServerLevel level, BlockPos origin, RandomSource random) {
        replaceGround(level, origin.below(), 12);
        int height = 22 + random.nextInt(10);
        helix(level, origin, 3.0D, height, 2, flesh());
        helix(level, origin.above(1), 3.0D, height, 2, bone(), Math.PI);
        for (int y = 0; y < height; y += 3) {
            hollowRing(level, origin.above(y), 4, 4, y % 6 == 0 ? dense() : rubble(), flesh());
        }
        hollowSphere(level, origin.above(height), 5, 4, 5, dense(), flesh());
    }

    private static void generateChamberTower(ServerLevel level, BlockPos origin, RandomSource random) {
        replaceGround(level, origin.below(), 12);
        BlockPos cursor = origin;
        int[] heights = {12, 20, 15};
        for (int height : heights) {
            for (int y = 0; y < height; y++) {
                hollowRing(level, cursor.above(y), 2 + random.nextInt(2), 2, bone(), flesh());
            }
            cursor = cursor.above(height);
            hollowSphere(level, cursor, 4, 3, 4, dense(), flesh());
            cursor = cursor.offset(random.nextInt(9) - 4, 5, random.nextInt(9) - 4);
        }
        makeEntrance(level, origin.above(1), 5);
    }

    private static void generatePodChain(ServerLevel level, BlockPos origin, RandomSource random) {
        replaceGround(level, origin.below(), 12);
        BlockPos cursor = origin.above(3);
        for (int i = 0; i < 3; i++) {
            int radius = 4 + random.nextInt(2);
            hollowSphere(level, cursor, radius, 3, radius, dense(), flesh());
            BlockPos next = cursor.offset(random.nextInt(9) - 4, 12 + random.nextInt(5), random.nextInt(9) - 4);
            organicLine(level, cursor, next, 2, bone());
            cursor = next;
        }
        makeEntrance(level, cursor, 5);
    }

    private static void generateWatchPost(ServerLevel level, BlockPos origin, RandomSource random) {
        replaceGround(level, origin.below(), 10);
        BlockPos first = polarOffset(origin, 4, random.nextDouble() * Math.PI * 2.0D);
        growSpire(level, first, 8, 2);
        hollowSphere(level, first.above(8), 3, 3, 3, dense(), flesh());
        if (random.nextBoolean()) {
            BlockPos second = polarOffset(origin, 4, random.nextDouble() * Math.PI * 2.0D);
            growSpire(level, second, 7, 2);
            hollowSphere(level, second.above(7), 3, 3, 3, dense(), flesh());
        }
    }

    private static void generateSpireCluster(ServerLevel level, BlockPos origin, RandomSource random) {
        replaceGround(level, origin.below(), 10);
        for (int i = 0; i < 3; i++) {
            BlockPos base = polarOffset(origin, 3 + i * 2, random.nextDouble() * Math.PI * 2.0D);
            int height = i == 0 ? 28 : 18 + random.nextInt(12);
            growSpire(level, base, height, i == 0 ? 2 : 1);
            hollowSphere(level, base.above(height), 3, 3, 3, dense(), flesh());
        }
    }

    private static void generateTwinPods(ServerLevel level, BlockPos origin, RandomSource random) {
        replaceGround(level, origin.below(), 10);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        BlockPos first = polarOffset(origin, 3, angle);
        BlockPos second = polarOffset(origin, 6, angle + Math.PI);
        hollowSphere(level, first.above(2), 3, 8, 3, dense(), flesh());
        hollowSphere(level, second.above(3), 4, 8, 4, dense(), flesh());
        organicLine(level, first, second, 1, bone());
    }

    private static void growSpire(ServerLevel level, BlockPos base, int height, int thickness) {
        for (int y = 0; y < height; y++) {
            int radius = Math.max(1, thickness + (int) Math.round(Math.sin(y * 0.4D)));
            hollowRing(level, base.above(y), radius, radius, bone(), flesh());
        }
    }

    private static void replaceGround(ServerLevel level, BlockPos center, int radius) {
        int radiusSq = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radiusSq) {
                    continue;
                }
                BlockPos column = center.offset(x, 0, z);
                for (int depth = 0; depth < 3; depth++) {
                    BlockPos target = column.below(depth);
                    if (!level.getBlockState(target).isAir()) {
                        set(level, target, depth == 0 ? stain() : flesh(), false);
                    }
                }
            }
        }
    }

    private static void hollowRing(ServerLevel level, BlockPos center, int radiusX, int radiusZ,
            BlockState shell, BlockState inner) {
        int rx = Math.max(1, radiusX);
        int rz = Math.max(1, radiusZ);
        for (int x = -rx; x <= rx; x++) {
            for (int z = -rz; z <= rz; z++) {
                double distance = x * x / (double) (rx * rx) + z * z / (double) (rz * rz);
                if (distance > 1.0D) {
                    continue;
                }
                set(level, center.offset(x, 0, z), distance >= 0.55D ? shell : inner, false);
            }
        }
    }

    private static void hollowSphere(ServerLevel level, BlockPos center, int radiusX, int radiusY,
            int radiusZ, BlockState shell, BlockState inner) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    double distance = x * x / (double) (radiusX * radiusX)
                            + y * y / (double) (radiusY * radiusY)
                            + z * z / (double) (radiusZ * radiusZ);
                    if (distance <= 1.0D) {
                        set(level, center.offset(x, y, z), distance >= 0.62D ? shell : inner, false);
                    }
                }
            }
        }
    }

    private static void helix(ServerLevel level, BlockPos base, double radius, int height,
            int strands, BlockState state) {
        helix(level, base, radius, height, strands, state, 0.0D);
    }

    private static void helix(ServerLevel level, BlockPos base, double radius, int height,
            int strands, BlockState state, double phase) {
        for (int y = 0; y < height; y++) {
            for (int strand = 0; strand < strands; strand++) {
                double angle = phase + y * Math.PI / 4.0D + strand * Math.PI * 2.0D / strands;
                BlockPos point = base.offset(Mth.floor(Math.cos(angle) * radius), y,
                        Mth.floor(Math.sin(angle) * radius));
                set(level, point, state, false);
                set(level, point.above(), state, false);
            }
        }
    }

    private static void organicLine(ServerLevel level, BlockPos from, BlockPos to, int radius, BlockState state) {
        int steps = Math.max(1, Math.max(Math.abs(to.getX() - from.getX()),
                Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ()))));
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            BlockPos point = new BlockPos(
                    Mth.floor(Mth.lerp(progress, from.getX(), to.getX())),
                    Mth.floor(Mth.lerp(progress, from.getY(), to.getY())),
                    Mth.floor(Mth.lerp(progress, from.getZ(), to.getZ())));
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x * x + y * y + z * z <= radius * radius) {
                            set(level, point.offset(x, y, z), state, false);
                        }
                    }
                }
            }
        }
    }

    private static void makeEntrance(ServerLevel level, BlockPos center, int length) {
        for (int z = 0; z <= length; z++) {
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    set(level, center.offset(x, y, z), Blocks.CAVE_AIR.defaultBlockState(), false);
                }
            }
        }
    }

    private static BlockPos polarOffset(BlockPos center, int radius, double angle) {
        return center.offset(Mth.floor(Math.cos(angle) * radius), 0, Mth.floor(Math.sin(angle) * radius));
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state, boolean force) {
        if (!level.isInWorldBounds(pos) || level.getBlockEntity(pos) != null) {
            return;
        }
        BlockState existing = level.getBlockState(pos);
        if (!force && (existing.is(ModBlocks.COLONYHEART.get()) || existing.is(ModBlocks.BIOMEHEART.get())
                || existing.is(ModBlocks.PARASITE_STRUCTURE.get()))) {
            return;
        }
        float hardness = existing.getDestroySpeed(level, pos);
        if (force || existing.isAir() || existing.canBeReplaced() || hardness >= 0.0F && hardness <= 7.0F) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private static BlockState stain() {
        return ModBlocks.INFESTED_STAIN.get().defaultBlockState();
    }

    private static BlockState flesh() {
        return ModBlocks.BIOMASS_BLOCK.get().defaultBlockState();
    }

    private static BlockState rubble() {
        return ModBlocks.INFESTED_RUBBLE.get().defaultBlockState();
    }

    private static BlockState dense() {
        return ModBlocks.RESIDUE_BRICKS.get().defaultBlockState();
    }

    private static BlockState bone() {
        return ModBlocks.HIVESTONE_DEBRIS.get().defaultBlockState();
    }
}
