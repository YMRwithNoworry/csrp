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
        if (!isSuitable(target) || owner.distanceToSqr(target) > range() * range()) {
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
            double horizontalDistance = owner.distanceToSqr(player.getX(), owner.getY(), player.getZ());
            double adjustedRange = player.isShiftKeyDown() ? range : range;
            if (player.isInvisible()) {
                adjustedRange *= 1.0D;
            }
            if (horizontalDistance >= adjustedRange * adjustedRange
                    || Math.abs(player.getY() - owner.getEyeY()) >= range) {
                continue;
            }
            if (horizontalDistance < bestDistance) {
                bestDistance = horizontalDistance;
                best = player;
            }
        }
        return best;
    }

    private LivingEntity findMob(double range) {
        AABB area = owner.getBoundingBox().inflate(range);
        List<Mob> candidates = owner.level().getEntitiesOfClass(Mob.class, area,
                candidate -> isSuitable(candidate));
        candidates.sort(Comparator.comparingDouble(owner::distanceToSqr));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean isSuitable(LivingEntity target) {
        if (target == null || target == owner || !target.isAlive() || target.isSpectator()
                || owner.isAlliedTo(target) || !owner.isWithinRestriction(target.blockPosition())
                || !candidateFilter.test(target)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (!(target instanceof Player) && id != null && id.toString().contains("srparasites")) {
            return false;
        }
        return !checkSight || owner.hasLineOfSight(target);
    }
}
