package alku.csrp.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Legacy {@code EntityAIGetFollowers(parent, version, range)} (version 1).
 *
 * <p>Every twenty ticks a parasite that has no leader of its own and no target looks for a nearby
 * parasite without a leader and makes it follow: the original scanned a box inflated by
 * {@code (range, 2, range)}, required line of sight, a live candidate without a follower, and the
 * legacy numeric-type gate {@code getParasiteType() < 31} - the port replaced the numeric type
 * table with per-family wiring, so that gate is implicit here.
 *
 * <p>Version 1 with a range of 16 is what every infected/feral/hijacked class registers
 * ({@code EntityInfCow.java:74}, {@code EntityFerVillager.java:56}, {@code EntityHiSkeleton.java:52},
 * {@code EntityDorpa.java:61}, {@code EntityInfHuman.java:122}); the adapted family uses version 3
 * with a range of 32 instead.
 */
public final class RecruitFollowersGoal extends Goal {
    /** Legacy check cadence. */
    private static final int CHECK_INTERVAL_TICKS = 20;
    /** Legacy vertical half-extent of the search box. */
    private static final double SEARCH_HEIGHT = 2.0D;

    private final Mob leader;
    private final int searchRange;

    public RecruitFollowersGoal(Mob leader, int searchRange) {
        this.leader = leader;
        this.searchRange = searchRange;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return leader.tickCount % CHECK_INTERVAL_TICKS == 0
                && ParasiteFollowGoal.getLeader(leader) == null
                && leader.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        for (Mob candidate : leader.level().getEntitiesOfClass(Mob.class,
                leader.getBoundingBox().inflate(searchRange, SEARCH_HEIGHT, searchRange),
                mob -> mob != leader && mob instanceof Parasite && mob.isAlive()
                        && ParasiteFollowGoal.getLeader(mob) == null
                        && leader.hasLineOfSight(mob))) {
            ParasiteFollowGoal.setLeader(candidate, leader);
            // The original stopped after the first recruit.
            break;
        }
    }
}
