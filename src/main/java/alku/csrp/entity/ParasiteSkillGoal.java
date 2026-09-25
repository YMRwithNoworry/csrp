package alku.csrp.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Legacy {@code EntityAISkill} dispatch contract.
 *
 * <p>The original task took {@code (para, cooldown, miniDistance, [maxDistance], needVisual,
 * attackID[, ignoreStatus])}, gated itself on {@code getGeneMod(5)} (the {@code geneSpecialmove}
 * flag) plus a parasite-status condition, waited until the target sat inside the squared distance
 * window, and then called {@code parentEntity.doSpecialSkill(attackID)} every tick until
 * {@code getFinished(attackID)} reported the skill was done.
 *
 * <p>The port keeps the same shape, with the {@code attackID} behaviour supplied as a
 * {@link ParasiteSkill} instead of a per-mob {@code doSpecialSkill} switch. A skill's own gate
 * ({@code ignoreStatus}, the attackID 13/31 exception) is expressed by overriding
 * {@link #geneAllows()} or {@link #canUse()}.
 */
public final class ParasiteSkillGoal extends Goal {
    /** The legacy {@code doSpecialSkill(attackID)} / {@code getFinished(attackID)} pair. */
    public interface ParasiteSkill {
        /** Legacy doSpecialSkill: runs on every tick while the skill is active. */
        void tick();

        /** Legacy getFinished: true once the skill has played out. */
        boolean isFinished();
    }

    private final Mob mob;
    private final int attackId;
    private final ParasiteSkill skill;
    private final int cooldownTicks;
    private final double minDistanceSqr;
    private final double maxDistanceSqr;
    private final boolean needVisual;
    private final boolean ignoreStatus;

    private int attackTimer;
    private int attacking;

    public ParasiteSkillGoal(Mob mob, int attackId, ParasiteSkill skill, int cooldownTicks,
                             int minDistance, boolean needVisual) {
        this(mob, attackId, skill, cooldownTicks, minDistance, 0, needVisual, false);
    }

    /** @param maxDistance legacy maxDistance; 0 means "no upper bound" */
    public ParasiteSkillGoal(Mob mob, int attackId, ParasiteSkill skill, int cooldownTicks,
                             int minDistance, int maxDistance, boolean needVisual) {
        this(mob, attackId, skill, cooldownTicks, minDistance, maxDistance, needVisual, false);
    }

    public ParasiteSkillGoal(Mob mob, int attackId, ParasiteSkill skill, int cooldownTicks,
                             int minDistance, int maxDistance, boolean needVisual, boolean ignoreStatus) {
        this.mob = mob;
        this.attackId = attackId;
        this.skill = skill;
        this.cooldownTicks = cooldownTicks;
        // The original squared the distances up front.
        this.minDistanceSqr = (double) minDistance * minDistance;
        this.maxDistanceSqr = (double) maxDistance * maxDistance;
        this.needVisual = needVisual;
        this.ignoreStatus = ignoreStatus;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!ignoreStatus && !geneAllows()) {
            return false;
        }
        if (attacking >= 1) {
            return true;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = mob.distanceToSqr(target);
        if (distance < minDistanceSqr || (maxDistanceSqr > 0.0D && distance >= maxDistanceSqr)) {
            return false;
        }
        return !needVisual || mob.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return attacking >= 1 || canUse();
    }

    @Override
    public void stop() {
        attacking = 0;
        attackTimer = 0;
    }

    @Override
    public void tick() {
        if (attacking >= 1) {
            skill.tick();
            if (skill.isFinished()) {
                attacking = 0;
                attackTimer = 0;
            }
            return;
        }
        // Legacy warm-up: the task waits out the cooldown before the skill fires.
        if (attackTimer < cooldownTicks) {
            attackTimer++;
            return;
        }
        attacking = 1;
    }

    /**
     * Legacy {@code getGeneMod(5)}: the generation must have unlocked {@code geneSpecialmove}.
     * Mobs with unconditional skills (legacy {@code ignoreStatus}) override {@link #canUse}.
     */
    protected boolean geneAllows() {
        return mob instanceof PrimitiveParasiteEntity parasite && parasite.specialMovesEnabled();
    }

    public int attackId() {
        return attackId;
    }
}
