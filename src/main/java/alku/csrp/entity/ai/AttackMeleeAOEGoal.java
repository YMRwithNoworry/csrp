package alku.csrp.entity.ai;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Reusable melee goal that upgrades an attack to an area attack when crowded. */
public class AttackMeleeAOEGoal extends MeleeAttackGoal {
    private final PathfinderMob attacker;
    private final double aoeRange;
    private final int minTargets;
    private final Predicate<LivingEntity> targetFilter;
    private final Consumer<List<LivingEntity>> aoeAttack;

    public AttackMeleeAOEGoal(PathfinderMob attacker, double speed, boolean follow, double aoeRange,
                              int minTargets, Predicate<LivingEntity> targetFilter,
                              Consumer<List<LivingEntity>> aoeAttack) {
        super(attacker, speed, follow);
        this.attacker = attacker;
        this.aoeRange = aoeRange;
        this.minTargets = Math.max(1, minTargets);
        this.targetFilter = targetFilter;
        this.aoeAttack = aoeAttack;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distance) {
        if (!isTimeToAttack() || distance > getAttackReachSqr(target)
                || !attacker.getSensing().hasLineOfSight(target)) {
            return;
        }
        resetAttackCooldown();
        AABB area = target.getBoundingBox().inflate(aoeRange);
        List<LivingEntity> targets = attacker.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && targetFilter.test(entity));
        attacker.swing(InteractionHand.MAIN_HAND);
        if (targets.size() >= minTargets) {
            aoeAttack.accept(targets);
        } else {
            attacker.doHurtTarget(target);
        }
    }
}
