package alku.csrp.entity;

import alku.csrp.world.EvolutionSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Legacy geneSprinting (applyGene): while the generation has unlocked sprinting, the parasite
 * covers long distances at {@value #SPRINT_MULTIPLIER}x its chase speed and falls back to the
 * normal speed once it is close enough to attack.
 *
 * <p>{@code MeleeAttackGoal.speedModifier} is private in 1.21.1, so this goal drives navigation
 * itself and is registered at the same priority as the mob's melee goal, *before* it: the goal
 * selector then picks this one while the target is far and the melee goal once it is not.
 */
public final class GeneSprintGoal extends Goal {
    private static final double SPRINT_DISTANCE_SQR = 16.0D;
    private static final double SPRINT_MULTIPLIER = 1.3D;

    private final PathfinderMob mob;
    private final double baseSpeed;

    public GeneSprintGoal(PathfinderMob mob, double baseSpeed) {
        this.mob = mob;
        this.baseSpeed = baseSpeed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && sprinting() && farFromTarget(target);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target != null) {
            mob.getNavigation().moveTo(target, baseSpeed * SPRINT_MULTIPLIER);
        }
    }

    /** Legacy geneSprinting gate: no sprinting until the generation unlocks it. */
    private boolean sprinting() {
        return mob.level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).sprinting();
    }

    private boolean farFromTarget(LivingEntity target) {
        return mob.distanceToSqr(target) > SPRINT_DISTANCE_SQR;
    }
}
