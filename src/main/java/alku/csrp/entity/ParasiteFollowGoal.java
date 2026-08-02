package alku.csrp.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Runtime-only leadership and following used by the legacy parasite base AI. */
final class ParasiteFollowGoal extends Goal {
    private static final double START_DISTANCE_SQR = 16.0D * 16.0D;
    private static final double STOP_DISTANCE_SQR = 6.0D * 6.0D;
    private static final Map<Mob, UUID> LEADERS = new WeakHashMap<>();

    private final Mob follower;
    private Mob leader;
    private int pathRecalculationTicks;
    private float previousWaterMalus;

    ParasiteFollowGoal(Mob follower) {
        this.follower = follower;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    static void setLeader(Mob follower, Mob leader) {
        if (!(follower instanceof Parasite) || leader != null && !(leader instanceof Parasite)) {
            return;
        }
        if (leader == null) {
            LEADERS.remove(follower);
        } else {
            LEADERS.put(follower, leader.getUUID());
        }
    }

    static Mob getLeader(Mob follower) {
        UUID leaderId = LEADERS.get(follower);
        if (leaderId == null || !(follower.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(leaderId);
        if (entity instanceof Mob mob && entity instanceof Parasite && mob.isAlive()) {
            return mob;
        }
        LEADERS.remove(follower);
        return null;
    }

    static int commandRank(Mob parasite) {
        if (parasite instanceof PreeminentParasiteEntity preeminent) {
            return preeminent.getKind() == PreeminentParasiteEntity.Kind.CARRIER_COLONY ? 31 : 61;
        }
        if (parasite instanceof AdaptedVariantEntity) {
            return 41;
        }
        if (parasite instanceof PureParasiteEntity || parasite instanceof MarauderEntity
                || parasite instanceof AbominationEntity) {
            return 51;
        }
        if (parasite instanceof DerivedParasiteEntity) {
            return 71;
        }
        if (parasite instanceof ArchitectEntity || parasite instanceof AncientPodEntity) {
            return 61;
        }
        if (parasite instanceof DreadnautTentacleEntity) {
            return 62;
        }
        if (parasite instanceof AncientParasiteEntity || parasite instanceof NexusParasiteEntity
                || parasite instanceof DeterrentParasiteEntity) {
            return 41;
        }
        return 20;
    }

    @Override
    public boolean canUse() {
        Mob candidate = getLeader(follower);
        if (candidate == null || follower.distanceToSqr(candidate) < START_DISTANCE_SQR) {
            return false;
        }
        leader = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (leader == null || !leader.isAlive()) {
            setLeader(follower, null);
            return false;
        }
        return !follower.getNavigation().isDone() && follower.distanceToSqr(leader) > STOP_DISTANCE_SQR;
    }

    @Override
    public void start() {
        pathRecalculationTicks = 0;
        previousWaterMalus = follower.getPathfindingMalus(PathType.WATER);
        follower.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    @Override
    public void stop() {
        leader = null;
        follower.getNavigation().stop();
        follower.setPathfindingMalus(PathType.WATER, previousWaterMalus);
    }

    @Override
    public void tick() {
        if (leader == null) {
            return;
        }
        follower.getLookControl().setLookAt(leader, 10.0F, follower.getMaxHeadXRot());
        LivingEntity target = follower.getTarget();
        if (target != null && !(target instanceof Parasite) && leader.getTarget() == null) {
            leader.setTarget(target);
        }
        if (--pathRecalculationTicks <= 0) {
            pathRecalculationTicks = 10;
            if (!follower.getNavigation().moveTo(leader, 1.3D)) {
                setLeader(follower, null);
            }
        }
    }
}
