package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.block.SrpCoreBlock;
import alku.csrp.infection.BlockInfestation;
import alku.csrp.registry.ModBlocks;
import alku.csrp.world.SrpWorldData.ColonyEntry;
import alku.csrp.world.SrpWorldData.NodeEntry;
import alku.csrp.world.SrpWorldData.VectorEntry;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Keeps SRP core blocks, persistent records, and daily vector growth in sync. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SrpCoreSystems {
    private static final long DAY_TICKS = 24_000L;
    private static final int VECTOR_HEALTH_CAP = 2_000_000_000;
    private static final int VECTOR_RADIUS_CAP = 2_000_000;
    private static final int MAX_NODES = 20;
    private static final int MAX_COLONIES = 20;
    private static final int NODE_MIN_DISTANCE = 10_000;
    private static final int COLONY_MIN_DISTANCE = 2_000;
    private static final int VECTOR_MIN_DISTANCE = 10_000;
    private static final double VECTOR_DAILY_HEALTH = 0.3D;
    private static final double VECTOR_DAILY_RADIUS = 1.35D;
    private static final double[] PHASE_HEALTH_BONUS = {
            0.0D, 0.01D, 0.02D, 0.05D, 0.1D, 0.2D, 0.3D, 0.4D, 0.5D, 0.6D, 0.7D
    };
    private static final double[] PHASE_RADIUS_BONUS = {
            0.0D, 0.005D, 0.01D, 0.02D, 0.05D, 0.1D, 0.2D, 0.3D, 0.4D, 0.5D, 0.65D
    };
    private static final int[] VECTOR_POINT_CAP = {
            100, 200, 600, 3_500, 25_000, 500_000,
            3_000_000, 50_000_000, 100_000_000, 150_000_000, Integer.MAX_VALUE
    };
    private static final int[] VECTOR_CAP = {1, 2, 2, 3, 3, 4, 4, 4, 5, 5, 5};
    private static final int[] VECTOR_HEALTH_MULTIPLIER = {1, 3, 5, 10, 40, 100, 350, 900, 1_300, 2_700, 5_500};

    private SrpCoreSystems() {
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % 200L == 0L) {
            checkCoresAndSpread(level);
        }
        if (level.getGameTime() > 0L && level.getGameTime() % DAY_TICKS == 0L) {
            advanceDailyState(level);
        }
    }

    public static boolean placeNode(ServerLevel level, BlockPos pos, int type) {
        SrpWorldData data = SrpWorldData.get(level);
        if (data.nodes().size() >= MAX_NODES || tooCloseToNode(data, pos, NODE_MIN_DISTANCE)) {
            return false;
        }
        data.setNode(pos, 1, type);
        if (level.setBlock(pos, ModBlocks.BIOMEHEART.get().defaultBlockState()
                .setValue(SrpCoreBlock.ACTIVE, 1), Block.UPDATE_ALL)) {
            BlockInfestation.infestAround(level, pos, 1);
            return true;
        }
        data.removeNode(pos);
        return false;
    }

    public static boolean placeColony(ServerLevel level, BlockPos pos) {
        SrpWorldData data = SrpWorldData.get(level);
        if (data.colonies().size() >= MAX_COLONIES || tooCloseToColony(data, pos, COLONY_MIN_DISTANCE)) {
            return false;
        }
        data.setColony(pos);
        if (level.setBlock(pos, ModBlocks.COLONYHEART.get().defaultBlockState()
                .setValue(SrpCoreBlock.ACTIVE, 1), Block.UPDATE_ALL)) {
            BlockInfestation.infestAround(level, pos, 1);
            return true;
        }
        data.rollbackColony(pos);
        return false;
    }

    public static boolean removeNode(ServerLevel level, BlockPos pos) {
        boolean removed = SrpWorldData.get(level).removeNode(pos);
        if (level.getBlockState(pos).is(ModBlocks.BIOMEHEART.get())) {
            level.removeBlock(pos, false);
            removed = true;
        }
        return removed;
    }

    public static boolean removeColony(ServerLevel level, BlockPos pos) {
        boolean removed = SrpWorldData.get(level).removeColony(pos);
        if (level.getBlockState(pos).is(ModBlocks.COLONYHEART.get())) {
            level.removeBlock(pos, false);
            removed = true;
        }
        return removed;
    }

    public static void clearNodes(ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        for (NodeEntry entry : new ArrayList<>(data.nodes())) {
            if (level.getBlockState(entry.pos()).is(ModBlocks.BIOMEHEART.get())) {
                level.removeBlock(entry.pos(), false);
            }
        }
        data.clearNodes();
    }

    public static void clearColonies(ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        for (ColonyEntry entry : new ArrayList<>(data.colonies())) {
            if (level.getBlockState(entry.pos()).is(ModBlocks.COLONYHEART.get())) {
                level.removeBlock(entry.pos(), false);
            }
        }
        data.clearColonies();
    }

    /** Original return keys: 1 regular vector, 2 outbreak, 6 too close, 7 cap reached. */
    public static int placeVector(ServerLevel level, BlockPos pos, int health, int radius) {
        SrpWorldData data = SrpWorldData.get(level);
        int phase = data.evolutionPhase();
        int phaseIndex = Math.max(0, Math.min(10, phase));
        if (data.vectors().size() >= (phase == -1 ? 1 : VECTOR_CAP[phaseIndex])) {
            return 7;
        }
        if (tooCloseToVector(data, pos, VECTOR_MIN_DISTANCE)) {
            return 6;
        }
        long adjustedHealth = (long) health * (phase == -1 ? 10 : VECTOR_HEALTH_MULTIPLIER[phaseIndex]);
        data.setVector(pos, (int) Math.min(VECTOR_HEALTH_CAP, adjustedHealth),
                Math.min(VECTOR_RADIUS_CAP, radius));
        return phase == -1 ? 2 : 1;
    }

    public static boolean removeVector(ServerLevel level, BlockPos pos) {
        SrpWorldData data = SrpWorldData.get(level);
        VectorEntry closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (VectorEntry entry : data.vectors()) {
            double distance = entry.pos().distSqr(pos);
            if (distance <= (double) entry.radius() * entry.radius() && distance < closestDistance) {
                closest = entry;
                closestDistance = distance;
            }
        }
        return closest != null && data.removeVector(closest.pos());
    }

    private static void checkCoresAndSpread(ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        for (NodeEntry entry : new ArrayList<>(data.nodes())) {
            if (!level.hasChunkAt(entry.pos())) {
                continue;
            }
            BlockState state = level.getBlockState(entry.pos());
            if (!state.is(ModBlocks.BIOMEHEART.get()) || state.getValue(SrpCoreBlock.ACTIVE) <= 0) {
                data.removeNode(entry.pos());
                continue;
            }
            int stage = nodeStage(entry.age());
            updateActiveState(level, entry.pos(), ModBlocks.BIOMEHEART.get(), stage);
            BlockInfestation.infestAround(level, entry.pos(), stage);
        }
        for (ColonyEntry entry : new ArrayList<>(data.colonies())) {
            if (!level.hasChunkAt(entry.pos())) {
                continue;
            }
            BlockState state = level.getBlockState(entry.pos());
            if (!state.is(ModBlocks.COLONYHEART.get()) || state.getValue(SrpCoreBlock.ACTIVE) <= 0) {
                data.removeColony(entry.pos());
                continue;
            }
            int stage = colonyStage(entry.points());
            updateActiveState(level, entry.pos(), ModBlocks.COLONYHEART.get(), stage);
            BlockInfestation.infestAround(level, entry.pos(), stage);
        }
    }

    private static void advanceDailyState(ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        for (NodeEntry entry : new ArrayList<>(data.nodes())) {
            int age = entry.age() + 1;
            int stage = nodeStage(age);
            data.updateNode(entry.pos(), age, entry.type());
            updateActiveState(level, entry.pos(), ModBlocks.BIOMEHEART.get(), stage);
        }
        for (ColonyEntry entry : new ArrayList<>(data.colonies())) {
            int points = Math.min(Config.colonyPointCap(), entry.points() + 1);
            data.updateColony(entry.pos(), points);
            updateActiveState(level, entry.pos(), ModBlocks.COLONYHEART.get(), colonyStage(points));
        }
        growVectors(level, data);
    }

    private static void growVectors(ServerLevel level, SrpWorldData data) {
        int phase = data.evolutionPhase();
        int phaseIndex = Math.max(0, Math.min(10, phase));
        long totalHealth = 0L;
        for (VectorEntry entry : new ArrayList<>(data.vectors())) {
            int radius = saturatingGrowth(entry.radius(), entry.radius(),
                    VECTOR_DAILY_RADIUS + PHASE_RADIUS_BONUS[phaseIndex], VECTOR_RADIUS_CAP);
            int health = saturatingGrowth(entry.health(), radius,
                    VECTOR_DAILY_HEALTH + PHASE_HEALTH_BONUS[phaseIndex], VECTOR_HEALTH_CAP);
            data.updateVector(entry.pos(), health, radius);
            totalHealth = Math.min(Integer.MAX_VALUE, totalHealth + health);
        }
        if (totalHealth > 0L) {
            double rate = phase == -1 ? 0.01D : 0.15D;
            int points = (int) Math.min(VECTOR_POINT_CAP[phaseIndex], Math.floor(totalHealth * rate));
            if (points > 0) {
                data.addEvolutionPoints(level, points, true);
            }
        }
    }

    private static int saturatingGrowth(int current, int basis, double rate, int cap) {
        long growth = (long) Math.floor(basis * rate);
        return (int) Math.min(cap, Math.max(1L, (long) current + growth));
    }

    private static void updateActiveState(ServerLevel level, BlockPos pos, Block block, int stage) {
        if (!level.hasChunkAt(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(block) && state.getValue(SrpCoreBlock.ACTIVE) != stage) {
            level.setBlock(pos, state.setValue(SrpCoreBlock.ACTIVE, stage), Block.UPDATE_ALL);
        }
    }

    private static int nodeStage(int age) {
        if (age >= 40) return 3;
        return age >= 10 ? 2 : 1;
    }

    private static int colonyStage(int points) {
        int cap = Config.colonyPointCap();
        if (points * 3 >= cap * 2) return 3;
        return points * 3 >= cap ? 2 : 1;
    }

    private static boolean tooCloseToNode(SrpWorldData data, BlockPos pos, int distance) {
        double minimum = (double) distance * distance;
        return data.nodes().stream().anyMatch(entry -> entry.pos().distSqr(pos) <= minimum);
    }

    private static boolean tooCloseToColony(SrpWorldData data, BlockPos pos, int distance) {
        double minimum = (double) distance * distance;
        return data.colonies().stream().anyMatch(entry -> entry.pos().distSqr(pos) <= minimum);
    }

    private static boolean tooCloseToVector(SrpWorldData data, BlockPos pos, int distance) {
        double minimum = (double) distance * distance;
        return data.vectors().stream().anyMatch(entry -> entry.pos().distSqr(pos) <= minimum);
    }
}
