package alku.csrp.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/** Legacy EntityAIGetFollowers leadership assignment shared by parasite families. */
final class ParasiteRecruitFollowersGoal extends Goal {
    private final Mob recruiter;
    private final int candidateRankLimit;
    private final int replaceLeaderRankLimit;
    private final int searchRange;

    ParasiteRecruitFollowersGoal(Mob recruiter, int version, int searchRange) {
        this.recruiter = recruiter;
        this.searchRange = searchRange;
        candidateRankLimit = switch (version) {
            case 1 -> 31;
            case 2, 3 -> 41;
            case 4 -> 61;
            default -> throw new IllegalArgumentException("Unsupported follower recruitment version: " + version);
        };
        replaceLeaderRankLimit = switch (version) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 40;
            case 4 -> 60;
            default -> throw new IllegalArgumentException("Unsupported follower recruitment version: " + version);
        };
    }

    @Override
    public boolean canUse() {
        return recruiter.tickCount % 20 == 0 && recruiter.getTarget() == null
                && ParasiteFollowGoal.getLeader(recruiter) == null;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        for (Mob follower : recruiter.level().getEntitiesOfClass(Mob.class,
                recruiter.getBoundingBox().inflate(searchRange, 2.0D, searchRange),
                candidate -> candidate != recruiter && candidate instanceof Parasite
                        && candidate.isAlive()
                        && ParasiteFollowGoal.commandRank(candidate) < candidateRankLimit)) {
            if (!recruiter.hasLineOfSight(follower)) {
                continue;
            }
            Mob leader = ParasiteFollowGoal.getLeader(follower);
            if (leader == null || ParasiteFollowGoal.commandRank(leader) <= replaceLeaderRankLimit) {
                ParasiteFollowGoal.setLeader(follower, recruiter);
                return;
            }
        }
    }
}
