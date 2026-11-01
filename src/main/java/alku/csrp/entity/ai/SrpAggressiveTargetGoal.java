package alku.csrp.entity.ai;

import alku.csrp.Config;
import alku.csrp.entity.Parasite;
import alku.csrp.infection.InfectionMechanics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Shared high-pressure target search for every SRP mob. It deliberately does not
 * require line of sight so parasites keep pressure through darkness and cover.
 */
public final class SrpAggressiveTargetGoal extends Goal {
    private static final double MINIMUM_SEARCH_RANGE = 64.0D;
    private static final int RETARGET_INTERVAL_TICKS = 5;

    private final Mob owner;
    private LivingEntity selected;
    private int nextSearchTick;

    public SrpAggressiveTargetGoal(Mob owner) {
        this.owner = owner;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!Config.mobAttackingEnabled() || !owner.isAlive() || owner.isNoAi()
                || owner.getTarget() != null && isValidTarget(owner, owner.getTarget())
                && owner.distanceToSqr(owner.getTarget()) <= searchRange(owner) * searchRange(owner)) {
            return false;
        }
        if (owner.tickCount < nextSearchTick) {
            return false;
        }
        selected = findTarget();
        nextSearchTick = owner.tickCount + RETARGET_INTERVAL_TICKS;
        return selected != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = owner.getTarget();
        if (!Config.mobAttackingEnabled() || !isValidTarget(owner, target)
                || owner.distanceToSqr(target) > searchRange(owner) * searchRange(owner)) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        if (selected != null && isValidTarget(owner, selected)
                && owner.distanceToSqr(selected) <= searchRange(owner) * searchRange(owner)) {
            owner.setTarget(selected);
        }
        selected = null;
    }

    @Override
    public void stop() {
        LivingEntity target = owner.getTarget();
        if (!isValidTarget(owner, target) || owner.distanceToSqr(target) > searchRange(owner) * searchRange(owner)) {
            owner.setTarget(null);
        }
        selected = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public static boolean isValidTarget(Mob owner, LivingEntity target) {
        return target != null && target != owner && target.isAlive() && !target.isSpectator()
                && !(target instanceof Parasite) && !InfectionMechanics.isHiddenAssimilated(target)
                && (target instanceof Mob || target instanceof Player)
                && !(target instanceof Player player && player.getAbilities().instabuild)
                && !owner.isAlliedTo(target) && owner.isWithinRestriction(target.blockPosition());
    }

    private LivingEntity findTarget() {
        double range = searchRange(owner);
        AABB searchArea = owner.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = owner.level().getEntitiesOfClass(LivingEntity.class, searchArea,
                target -> isValidTarget(owner, target));
        candidates.sort(Comparator.comparingDouble(owner::distanceToSqr));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static double searchRange(Mob owner) {
        AttributeInstance followRange = owner.getAttribute(Attributes.FOLLOW_RANGE);
        return Math.max(MINIMUM_SEARCH_RANGE, followRange == null ? MINIMUM_SEARCH_RANGE : followRange.getValue());
    }
}
