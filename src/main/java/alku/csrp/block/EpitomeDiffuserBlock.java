package alku.csrp.block;

import alku.csrp.Csrp;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockEpitomeInfestationWarpDiffuser}
 * (out109 {@code block/BlockEpitomeInfestationWarpDiffuser.java}) — registered as
 * {@code srparasites:epitome_infestation_warp_diffuser} (hardness 15.0F, resistance 120.0F, light
 * emission 0.4F → light level 6, flesh sound).
 *
 * <p>The original flooded a bounded BFS over every {@code srparasites} block within a 256-block cube
 * of the diffuser and deleted them at up to {@code MAX_BLOCKS_PER_TICK = 8192} per tick, capped at
 * {@code MAX_BLOCKS_TOTAL = 2000000} blocks ({@code BlockEpitomeInfestationWarpDiffuser.java:7-10},
 * {@code processJob}, lines 74-107).  Right-clicking starts the job; a second right-click while a job
 * for the same position is running is rejected ({@code BlockEpitomeInfestationWarpDiffuser.java:26-46}).</p>
 *
 * <p>The port keeps the same constants, the same "one job per world+position" keying and the same
 * BFS, driven from the scheduled tick instead of the 1.12.2 {@code updateTick}.</p>
 */
public final class EpitomeDiffuserBlock extends Block {
    private static final int RADIUS = 256;
    private static final int MAX_BLOCKS_PER_TICK = 8192;
    private static final int MAX_BLOCKS_TOTAL = 2_000_000;
    /** {@code BlockEpitomeInfestationWarpDiffuser.func_149715_a(0.4F)} → level 6. */
    public static final int LIGHT_LEVEL = 6;

    private static final Map<String, DiffusionJob> JOBS = new HashMap<>();

    public EpitomeDiffuserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        String key = jobKey(level, pos);
        if (JOBS.containsKey(key)) {
            return InteractionResult.CONSUME;
        }
        JOBS.put(key, new DiffusionJob(pos));
        level.scheduleTick(pos, this, 1);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        String key = jobKey(level, pos);
        DiffusionJob job = JOBS.get(key);
        if (job == null) {
            return;
        }
        processJob(level, job);
        if (!job.finished && !job.queue.isEmpty() && job.removed < MAX_BLOCKS_TOTAL) {
            level.scheduleTick(pos, this, 1);
        } else {
            JOBS.remove(key);
            if (job.removed > 0) {
                level.levelEvent(2001, pos, Block.getId(state));
            }
        }
    }

    private static void processJob(ServerLevel level, DiffusionJob job) {
        int removedThisTick = 0;
        while (!job.queue.isEmpty() && removedThisTick < MAX_BLOCKS_PER_TICK
                && job.removed < MAX_BLOCKS_TOTAL) {
            BlockPos current = BlockPos.of(job.queue.poll());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = current.offset(dx, dy, dz);
                        long key = next.asLong();
                        if (!job.visited.add(key) || !isWithinRadius(job.origin, next)
                                || !level.isLoaded(next)) {
                            continue;
                        }
                        if (isRemovableSrpBlock(level, next)) {
                            level.setBlock(next, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                                    Block.UPDATE_ALL);
                            job.queue.add(key);
                            job.removed++;
                            if (++removedThisTick >= MAX_BLOCKS_PER_TICK
                                    || job.removed >= MAX_BLOCKS_TOTAL) {
                                return;
                            }
                        }
                    }
                }
            }
        }
        job.finished = job.queue.isEmpty();
    }

    private static boolean isWithinRadius(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= RADIUS
                && Math.abs(pos.getY() - origin.getY()) <= RADIUS
                && Math.abs(pos.getZ() - origin.getZ()) <= RADIUS;
    }

    /** {@code isRemovableSRPBlock} — any block in the {@code srparasites} namespace except itself. */
    private static boolean isRemovableSrpBlock(ServerLevel level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof EpitomeDiffuserBlock) {
            return false;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && Csrp.MODID.equals(id.getNamespace());
    }

    private static String jobKey(Level level, BlockPos pos) {
        return level.dimension().identifier() + ":" + pos.asLong();
    }

    /** One diffusion run; mirrors the original's private {@code DiffusionJob}. */
    private static final class DiffusionJob {
        private final BlockPos origin;
        private final Queue<Long> queue = new ArrayDeque<>();
        private final Set<Long> visited = new HashSet<>();
        private int removed;
        private boolean finished;

        private DiffusionJob(BlockPos origin) {
            this.origin = origin;
            this.queue.add(origin.asLong());
            this.visited.add(origin.asLong());
        }
    }
}
