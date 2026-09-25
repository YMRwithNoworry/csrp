package alku.csrp.entity;

import alku.csrp.world.EvolutionSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Legacy geneAttackSpeed + geneSprinting in one goal.
 *
 * <p>The original derived its attack cadence from {@code getAttackSpeed() = (int)(attackSpeedT *
 * geneAttackSpeed)} and its chase speed from the sprinting gene. Both live behind private state in
 * the vanilla {@code MeleeAttackGoal} ({@code speedModifier} and its attack timer are private in
 * 1.21.1), so families that need the genes drive their melee loop here instead: the mob closes the
 * distance (faster while the sprint gene is on and the target is far) and attacks on a cadence
 * scaled by the generation's attack speed multiplier.
 */
public final class GeneMeleeGoal extends Goal {
    /** Legacy attackSpeedT: the port's base interval for these families. */
    private static final int BASE_ATTACK_INTERVAL_TICKS = 20;
    private static final double SPRINT_DISTANCE_SQR = 16.0D;
    private static final double SPRINT_MULTIPLIER = 1.3D;

    private final Mob mob;
    /** Legacy per-mob attackSpeedT; defaults to the family base of 20 ticks. */
    private final int attackIntervalTicks;
    private final double baseSpeed;
    private final boolean requireLineOfSight;
    private int attackCooldown;

    public GeneMeleeGoal(Mob mob, double baseSpeed, boolean requireLineOfSight) {
        this(mob, baseSpeed, requireLineOfSight, BASE_ATTACK_INTERVAL_TICKS);
    }

    /** @param attackIntervalTicks legacy attackSpeedT (heads use 15). */
    public GeneMeleeGoal(Mob mob, double baseSpeed, boolean requireLineOfSight, int attackIntervalTicks) {
        this.attackIntervalTicks = Math.max(1, attackIntervalTicks);
        this.mob = mob;
        this.baseSpeed = baseSpeed;
        this.requireLineOfSight = requireLineOfSight;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive()
                && (!requireLineOfSight || mob.getSensing().hasLineOfSight(target));
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        attackCooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (mob.isWithinMeleeAttackRange(target) && attackCooldown <= 0) {
            mob.getNavigation().stop();
            if (mob.doHurtTarget(target)) {
                attackCooldown = attackInterval();
            }
            return;
        }
        double speed = baseSpeed * sprintMultiplier(target);
        mob.getNavigation().moveTo(target, speed);
    }

    /** Legacy getAttackSpeed(): the generation shrinks the interval as it advances. */
    private int attackInterval() {
        float multiplier = mob.level() instanceof ServerLevel serverLevel
                ? EvolutionSystem.generationProfile(serverLevel).attackSpeedMultiplier() : 1.0F;
        if (multiplier <= 0.0F) {
            return attackIntervalTicks;
        }
        return Math.max(1, Math.round(attackIntervalTicks * multiplier));
    }

    /** Legacy geneSprinting: close long distances faster once the generation unlocks it. */
    private double sprintMultiplier(LivingEntity target) {
        boolean sprinting = mob.level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).sprinting();
        return sprinting && mob.distanceToSqr(target) > SPRINT_DISTANCE_SQR ? SPRINT_MULTIPLIER : 1.0D;
    }
}
