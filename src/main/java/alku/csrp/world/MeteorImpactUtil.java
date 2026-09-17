package alku.csrp.world;

import alku.csrp.Csrp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/**
 * Port of SRParasites 1.10.8 {@code WorldGenMeteorImpactUtil}. The original class carved craters,
 * scorched rings, ejecta and angled tunnels for the meteor infection world event.
 */
public final class MeteorImpactUtil {
    private static final int MIN_CARVE_Y = 5;
    private static final Map<ResourceKey<Level>, List<PendingStructure>> PENDING = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> MAIN_METEOR_CENTERS = new HashMap<>();

    private MeteorImpactUtil() {
    }

    /** Original {@code World#getTopSolidOrLiquidBlock}: first air block above the top solid/liquid block. */
    public static BlockPos topSolidOrLiquid(ServerLevel level, BlockPos pos) {
        level.getChunkAt(pos);
        int x = pos.getX();
        int z = pos.getZ();
        int top = Math.min(level.getMaxBuildHeight() - 1,
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 16);
        for (int y = top; y >= level.getMinBuildHeight(); y--) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if ((state.blocksMotion() && !state.is(BlockTags.LEAVES))
                    || state.getFluidState().is(Fluids.WATER)) {
                return new BlockPos(x, y + 1, z);
            }
        }
        return new BlockPos(x, level.getMinBuildHeight(), z);
    }

    public static void tickPendingStructures(ServerLevel level) {
        List<PendingStructure> list = PENDING.get(level.dimension());
        if (list == null || list.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        list.removeIf(pending -> {
            if (now < pending.executeAt()) {
                return false;
            }
            StructurePlacer.place(level,
                    Identifier.fromNamespaceAndPath(Csrp.MODID, pending.name()),
                    pending.origin().offset(pending.offX(), pending.offY(), pending.offZ()),
                    RandomSource.create(pending.seed()));
            return true;
        });
        if (list.isEmpty()) {
            PENDING.remove(level.dimension());
        }
    }

    public static void scheduleDelayedStructure(ServerLevel level, RandomSource random, String name,
            BlockPos origin, int offX, int offY, int offZ, int delayTicks) {
        PENDING.computeIfAbsent(level.dimension(), key -> new ArrayList<>())
                .add(new PendingStructure(name, origin.immutable(), offX, offY, offZ,
                        level.getGameTime() + Math.max(1, delayTicks), random.nextLong()));
    }

    public static void markMainMeteor(ServerLevel level, BlockPos center) {
        synchronized (MAIN_METEOR_CENTERS) {
            Set<Long> set = MAIN_METEOR_CENTERS.computeIfAbsent(level.dimension(), key -> new HashSet<>());
            set.add(center.asLong());
            if (set.size() > 512) {
                set.clear();
            }
        }
    }

    public static boolean isNearMainMeteor(ServerLevel level, BlockPos pos, int minDist) {
        Set<Long> set;
        synchronized (MAIN_METEOR_CENTERS) {
            set = MAIN_METEOR_CENTERS.get(level.dimension());
        }
        if (set == null || set.isEmpty()) {
            return false;
        }
        int minDistSq = minDist * minDist;
        for (Long value : set) {
            BlockPos center = BlockPos.of(value);
            int dx = center.getX() - pos.getX();
            int dz = center.getZ() - pos.getZ();
            if (dx * dx + dz * dz <= minDistSq) {
                return true;
            }
        }
        return false;
    }

    public static void carveCraterBowl(ServerLevel level, RandomSource random, BlockPos surface,
            int radius, int depth, float steepness, BlockState rim, BlockState stain, BlockState cooked) {
        int cx = surface.getX();
        int cz = surface.getZ();
        int coreR = Math.max(2, (int) (radius * 0.28F));
        int coreRR = coreR * coreR;
        int radiusSq = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int d2 = x * x + z * z;
                if (d2 > radiusSq) {
                    continue;
                }
                BlockPos colTop = topSolidOrLiquid(level, new BlockPos(cx + x, surface.getY(), cz + z));
                int topY = colTop.getY();
                double dist = Math.sqrt(d2);
                double t = dist / radius;
                double bowl = 1.0D - t;
                double curve = bowl * bowl;
                int cut = topY - (int) Math.round(depth * curve);

                for (int y = topY; y > cut && y > MIN_CARVE_Y; y--) {
                    BlockPos p = new BlockPos(cx + x, y, cz + z);
                    if (!level.isLoaded(p)) {
                        break;
                    }
                    BlockState state = level.getBlockState(p);
                    if (!state.isAir() && p.getY() > MIN_CARVE_Y) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }

                BlockPos top = topSolidOrLiquid(level, new BlockPos(cx + x, surface.getY(), cz + z)).below();
                if (!level.isLoaded(top)) {
                    continue;
                }
                if (d2 <= coreRR) {
                    level.setBlock(top, random.nextInt(3) == 0 ? stain : cooked, 2);
                } else if (dist >= radius - 1.5D) {
                    level.setBlock(top, rim, 2);
                } else if (dist >= radius * 0.55D && random.nextInt(4) == 0) {
                    level.setBlock(top, stain, 2);
                }
            }
        }
    }

    public static void scorchRings(ServerLevel level, RandomSource random, BlockPos surface,
            int radius, BlockState stain) {
        int cx = surface.getX();
        int cz = surface.getZ();
        int ring1 = (int) (radius * 1.15F);
        int ring2 = (int) (radius * 1.45F);

        for (int x = -ring2; x <= ring2; x++) {
            for (int z = -ring2; z <= ring2; z++) {
                int d2 = x * x + z * z;
                if (d2 < ring1 * ring1 || d2 > ring2 * ring2 || random.nextInt(3) != 0) {
                    continue;
                }
                BlockPos top = topSolidOrLiquid(level, new BlockPos(cx + x, surface.getY(), cz + z)).below();
                if (level.isLoaded(top)) {
                    level.setBlock(top, stain, 2);
                }
            }
        }
    }

    public static void spawnEjecta(ServerLevel level, RandomSource random, BlockPos surface, int radius,
            double dirX, double dirZ, BlockState rubble, BlockState stain) {
        int cx = surface.getX();
        int cz = surface.getZ();
        int max = (int) (radius * 3.2F);
        int min = (int) (radius * 1.1F);

        for (int i = 0; i < radius * 30; i++) {
            double dist = min + random.nextInt(Math.max(1, max - min));
            double spread = (random.nextDouble() - 0.5D) * 1.25D;
            double px = dirX * dist + -dirZ * dist * spread;
            double pz = dirZ * dist + dirX * dist * spread;
            int x = cx + (int) Math.round(px);
            int z = cz + (int) Math.round(pz);
            BlockPos top = topSolidOrLiquid(level, new BlockPos(x, surface.getY(), z)).below();
            if (!level.isLoaded(top)) {
                continue;
            }
            level.setBlock(top, random.nextInt(5) == 0 ? stain : rubble, 2);
        }
    }

    public static void microCraters(ServerLevel level, RandomSource random, BlockPos surface, int radius,
            double dirX, double dirZ, BlockState stain) {
        int cx = surface.getX();
        int cz = surface.getZ();
        int count = Math.max(3, radius / 3);

        for (int i = 0; i < count; i++) {
            double dist = radius * (1.2D + random.nextDouble() * 2.0D);
            double spread = (random.nextDouble() - 0.5D) * 1.6D;
            double px = dirX * dist + -dirZ * dist * spread;
            double pz = dirZ * dist + dirX * dist * spread;
            int x0 = cx + (int) Math.round(px);
            int z0 = cz + (int) Math.round(pz);
            int r = 2 + random.nextInt(3);
            int rr = r * r;

            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z > rr || random.nextInt(3) != 0) {
                        continue;
                    }
                    BlockPos top = topSolidOrLiquid(level,
                            new BlockPos(x0 + x, surface.getY(), z0 + z)).below();
                    if (level.isLoaded(top)) {
                        level.setBlock(top, stain, 2);
                    }
                }
            }
        }
    }

    public static TunnelResult carveAngledTunnel(ServerLevel level, BlockPos center, int radius,
            int length, double dirX, double dirY, double dirZ) {
        double mag = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (mag < 1.0E-6D) {
            return new TunnelResult(false, 0, 0, 0, 0);
        }
        dirX /= mag;
        dirY /= mag;
        dirZ /= mag;
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        boolean any = false;
        int firstY = 0;
        int lastY = 0;
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;

        for (int t = 0; t <= length; t++) {
            int px = cx + (int) Math.round(dirX * t);
            int py = cy + (int) Math.round(dirY * t);
            int pz = cz + (int) Math.round(dirZ * t);
            TunnelResultSlice slice = carveSphereCount(level, new BlockPos(px, py, pz), radius);
            if (!slice.brokeAny()) {
                continue;
            }
            if (!any) {
                any = true;
                firstY = slice.firstBrokenY();
            }
            lastY = slice.lastBrokenY();
            low = Math.min(low, slice.lowestY());
            high = Math.max(high, slice.highestY());
        }

        return any ? new TunnelResult(true, firstY, lastY, low, high)
                : new TunnelResult(false, 0, 0, 0, 0);
    }

    public static void clearVegetationInArea(ServerLevel level, BlockPos center, int radius,
            int yMin, int yMax) {
        int cx = center.getX();
        int cz = center.getZ();
        int rr = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > rr) {
                    continue;
                }
                int ax = cx + x;
                int az = cz + z;
                for (int y = yMin; y <= yMax; y++) {
                    BlockPos p = new BlockPos(ax, y, az);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(p);
                    if (!state.isAir() && p.getY() > MIN_CARVE_Y && isVegetation(level, p, state)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** Original {@code updateWaterAfterImpact}: re-trigger fluid updates around the new crater. */
    public static void updateWaterAfterImpact(ServerLevel level, BlockPos surface, int radius, int depth) {
        int cx = surface.getX();
        int cy = surface.getY();
        int cz = surface.getZ();
        int r = Math.max(8, (int) (radius * 1.8F) + 12);
        int yMin = Math.max(1, cy - depth - 6);
        int yMax = Math.min(level.getMaxBuildHeight() - 1, cy + 24);

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = yMin; y <= yMax; y++) {
                    BlockPos p = new BlockPos(cx + x, y, cz + z);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(p);
                    if (state.getFluidState().is(Fluids.WATER)) {
                        level.scheduleTick(p, state.getFluidState().getType(), 1);
                        level.updateNeighborsAt(p, state.getBlock());
                        continue;
                    }
                    if (!state.isAir()) {
                        continue;
                    }
                    for (Direction direction : Direction.values()) {
                        BlockPos neighbour = p.relative(direction);
                        BlockState neighbourState = level.getBlockState(neighbour);
                        if (!neighbourState.getFluidState().is(Fluids.WATER)) {
                            continue;
                        }
                        level.scheduleTick(neighbour, neighbourState.getFluidState().getType(), 1);
                        level.updateNeighborsAt(neighbour, neighbourState.getBlock());
                    }
                }
            }
        }
    }

    private static TunnelResultSlice carveSphereCount(ServerLevel level, BlockPos center, int radius) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int rr = radius * radius;
        boolean any = false;
        int firstY = 0;
        int lastY = 0;
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > rr) {
                        continue;
                    }
                    BlockPos p = new BlockPos(cx + x, cy + y, cz + z);
                    if (!level.isLoaded(p) || p.getY() <= MIN_CARVE_Y) {
                        continue;
                    }
                    BlockState state = level.getBlockState(p);
                    if (state.isAir()) {
                        continue;
                    }
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    if (!any) {
                        any = true;
                        firstY = p.getY();
                    }
                    lastY = p.getY();
                    low = Math.min(low, p.getY());
                    high = Math.max(high, p.getY());
                }
            }
        }

        return any ? new TunnelResultSlice(true, firstY, lastY, low, high)
                : new TunnelResultSlice(false, 0, 0, 0, 0);
    }

    private static boolean isVegetation(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(Blocks.VINE)) {
            return true;
        }
        if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS)) {
            return true;
        }
        return state.getSoundType(level, pos, null) == SoundType.WOOD;
    }

    private record PendingStructure(String name, BlockPos origin, int offX, int offY, int offZ,
            long executeAt, long seed) {
    }

    public record TunnelResult(boolean anyBroken, int firstBrokenY, int lastBrokenY,
            int lowestY, int highestY) {
    }

    private record TunnelResultSlice(boolean brokeAny, int firstBrokenY, int lastBrokenY,
            int lowestY, int highestY) {
    }
}
