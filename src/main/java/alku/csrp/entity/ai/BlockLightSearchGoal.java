package alku.csrp.entity.ai;

import alku.csrp.Csrp;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Legacy {@code EntityAIBlockLight}: a parasite that has no attack target walks to the closest light
 * or redstone source around it and eats through it. The goal only exists for generations that unlock
 * the block-search gene (index 7) and it respects {@code mobGriefing}.
 */
public final class BlockLightSearchGoal extends Goal {
    /** Legacy retry interval of {@code func_75250_a}. */
    private static final int RETRY_TICKS = 40;
    /** Legacy break progress multiplier ({@code float multiplier = 10.0F} in {@code func_75249_e}). */
    private static final float BREAK_MULTIPLIER = 10.0F;
    private static final double REACH_SQR = 5.0D;
    private static final int ABANDON_IDLE_TICKS = 240;
    /** Legacy scan bounds: {@code -range..range} horizontally and {@code -4..height} vertically. */
    private static final int VERTICAL_DOWN = 4;

    private final Mob parent;
    private final int range;
    private final int lightTrigger;
    /** Legacy {@code cant} list: sources this parasite already gave up on. */
    private final List<Long> rejected = new ArrayList<>();
    private int ticks;
    private int progress;
    private int idle;
    private int neededTime;
    private int previousProgress = -1;
    private int previousDistance = -1;
    private BlockPos target;
    private Block block;

    public BlockLightSearchGoal(Mob parent) {
        this(parent, 20, 5);
    }

    public BlockLightSearchGoal(Mob parent, int range, int lightTrigger) {
        this.parent = parent;
        this.range = Math.max(1, range);
        this.lightTrigger = Math.max(1, lightTrigger);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        ticks++;
        if (!generationAllowsBlockSearch()) {
            return false;
        }
        if (ticks < RETRY_TICKS) {
            return false;
        }
        ticks = 0;
        if (parent.getTarget() != null || !parent.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }
        target = findSource();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && parent.level().getBlockState(target).getBlock() == block
                && parent.getTarget() == null;
    }

    @Override
    public void start() {
        progress = 0;
        idle = 0;
        BlockState state = parent.level().getBlockState(target);
        block = state.getBlock();
        // Legacy neededTime = state.getDestroySpeed(world, pos) * 10, never below one progress tick.
        neededTime = Math.max(1, (int) (state.getDestroySpeed(parent.level(), target) * BREAK_MULTIPLIER));
    }

    @Override
    public void stop() {
        parent.getNavigation().stop();
        previousProgress = -1;
        target = null;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        if (parent.isPassenger()) {
            stop();
            return;
        }
        double distance = target.distToCenterSqr(parent.getX(), parent.getY(), parent.getZ());
        if (distance > REACH_SQR) {
            parent.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.1D);
        }
        idle++;
        if ((int) Math.round(distance) == previousDistance) {
            idle++;
        } else {
            idle = 0;
        }
        if (idle >= ABANDON_IDLE_TICKS) {
            long key = target.asLong();
            if (!rejected.contains(key)) {
                rejected.add(key);
            }
            stop();
            ticks += 30;
            return;
        }
        previousDistance = (int) Math.round(distance);
        if (distance > REACH_SQR) {
            return;
        }
        parent.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 0.0D);
        progress++;
        idle = 0;
        int stage = (int) ((float) progress / neededTime);
        if (stage != previousProgress) {
            parent.level().destroyBlockProgress(parent.getId(), target, stage);
            previousProgress = stage;
        }
        if (progress >= neededTime) {
            if (ForgeEventFactory.onEntityDestroyBlock(parent, target, parent.level().getBlockState(target))) {
                parent.level().destroyBlock(target, true);
            } else {
                long key = target.asLong();
                if (!rejected.contains(key)) {
                    rejected.add(key);
                }
                stop();
            }
            progress = 0;
            ticks += 30;
        }
    }

    private boolean generationAllowsBlockSearch() {
        return parent.level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).blockSearch();
    }

    /** Legacy {@code findSource()}: the closest allowed light or circuit block inside the scan box. */
    private BlockPos findSource() {
        if (parent.level().getBrightness(LightLayer.BLOCK, parent.blockPosition()) < lightTrigger
                && parent.getRandom().nextInt(3) != 0) {
            return null;
        }
        if (parent.isPassenger()) {
            return null;
        }
        BlockPos origin = parent.blockPosition();
        int height = (int) Math.floor(parent.getBbHeight());
        BlockPos closest = null;
        double closestDistance = 40000.0D;
        for (int x = -range; x <= range; x++) {
            for (int y = -VERTICAL_DOWN; y <= height; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    if (rejected.contains(candidate.asLong()) || !isValidSource(candidate)) {
                        continue;
                    }
                    double distance = candidate.distToCenterSqr(parent.getX(), parent.getY(), parent.getZ());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closest = candidate;
                    }
                }
            }
        }
        return closest;
    }

    /**
     * Legacy block filter: no liquids, no portals or end gateways, no parasite blocks and nothing
     * unbreakable, and the block has to be a light source above the trigger level or a redstone circuit.
     */
    private boolean isValidSource(BlockPos pos) {
        BlockState state = parent.level().getBlockState(pos);
        if (state.isAir() || state.liquid()) {
            return false;
        }
        if (state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)
                || state.is(Blocks.END_PORTAL_FRAME)) {
            return false;
        }
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(Csrp.MODID)) {
            return false;
        }
        if (state.getDestroySpeed(parent.level(), pos) < 0.0F) {
            return false;
        }
        return state.getLightEmission() >= lightTrigger || isCircuit(state);
    }

    /** Legacy {@code Material.CIRCUITS} check, which the light level test alone would miss. */
    private static boolean isCircuit(BlockState state) {
        return state.getBlock() instanceof RedStoneWireBlock
                || state.getBlock() instanceof DiodeBlock
                || state.getBlock() instanceof LeverBlock
                || state.getBlock() instanceof ButtonBlock
                || state.getBlock() instanceof DaylightDetectorBlock
                || state.getBlock() instanceof TripWireHookBlock;
    }
}
