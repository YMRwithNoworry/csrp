package alku.csrp.entity.ai;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Target search used by the legacy primitive and adapted parasite variants.
 * It deliberately searches players separately from ordinary mobs so both
 * target classes can run at the same priority, matching the original AI.
 */
public final class PrimitiveAdaptedTargetGoal extends Goal {
    private static final int MAX_UNSEEN_TICKS = 60;
    private static final double DEFAULT_RANGE = 16.0D;

    private final Mob owner;
    private final boolean players;
    private final Predicate<LivingEntity> candidateFilter;
    private final boolean checkSight;
    private LivingEntity selected;
    private int unseenTicks;

    public PrimitiveAdaptedTargetGoal(Mob owner, boolean players,
                                      Predicate<LivingEntity> candidateFilter,
                                      boolean checkSight) {
        this.owner = owner;
        this.players = players;
        this.candidateFilter = candidateFilter;
        this.checkSight = checkSight;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!owner.isAlive() || owner.getTarget() != null && owner.getTarget().isAlive()) {
            return false;
        }
        double range = range();
        selected = players ? findPlayer(range) : findMob(range);
        return selected != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = owner.getTarget();
        if (!isSuitable(target, false) || owner.distanceToSqr(target) > range() * range()) {
            return false;
        }
        if (checkSight && !owner.hasLineOfSight(target)) {
            unseenTicks++;
            if (unseenTicks > MAX_UNSEEN_TICKS) {
                return false;
            }
        } else {
            unseenTicks = 0;
        }
        return true;
    }

    @Override
    public void start() {
        if (selected != null && owner.distanceToSqr(selected) <= range() * range()) {
            owner.setTarget(selected);
        }
        selected = null;
        unseenTicks = 0;
    }

    @Override
    public void stop() {
        owner.setTarget(null);
        selected = null;
        unseenTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private double range() {
        var attribute = owner.getAttribute(Attributes.FOLLOW_RANGE);
        return attribute == null ? DEFAULT_RANGE : Math.max(0.0D, attribute.getValue());
    }

    private LivingEntity findPlayer(double range) {
        Level level = owner.level();
        double bestDistance = Double.MAX_VALUE;
        LivingEntity best = null;
        for (Player player : level.players()) {
            if (player.getAbilities().instabuild || !isSuitable(player)) {
                continue;
            }
            double distance = owner.distanceToSqr(player.getX(), owner.getEyeY(), player.getZ());
            if (!isWithinTargetRange(player, range, distance)) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    private LivingEntity findMob(double range) {
        AABB area = owner.getBoundingBox().inflate(range);
        List<Mob> candidates = owner.level().getEntitiesOfClass(Mob.class, area,
                candidate -> isSuitable(candidate)
                        && isWithinTargetRange(candidate, range, distanceFromSearchOrigin(candidate)));
        candidates.sort(Comparator.comparingDouble(this::distanceFromSearchOrigin));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private double distanceFromSearchOrigin(LivingEntity target) {
        return owner.distanceToSqr(target.getX(), owner.getEyeY(), target.getZ());
    }

    /** Mirrors vanilla TargetingConditions' visibility-aware search radius. */
    private boolean isWithinTargetRange(LivingEntity target, double range, double distance) {
        double visibility = target.getVisibilityPercent(owner);
        double effectiveRange = Math.max(range * visibility, 2.0D);
        return distance <= effectiveRange * effectiveRange;
    }

    private boolean isSuitable(LivingEntity target) {
        return isSuitable(target, checkSight);
    }

    private boolean isSuitable(LivingEntity target, boolean requireSight) {
        if (target == null || target == owner || !target.isAlive() || target.isSpectator()
                || owner.isAlliedTo(target) || !owner.isWithinRestriction(target.blockPosition())
                || !candidateFilter.test(target)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (!(target instanceof Player) && id != null && id.toString().contains("srparasites")) {
            return false;
        }
        return !requireSight || owner.hasLineOfSight(target);
    }
}
