package alku.csrp.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Three-dimensional pursuit goal for flying parasites with a melee dive attack. */
public class FlightAttackGoal extends Goal {
    private final Mob mob;
    private final double speed;
    private final double heightOffset;
    private final double attackRange;
    private final int attackInterval;
    private int attackCooldown;

    public FlightAttackGoal(Mob mob, double speed, double heightOffset, double attackRange,
                            int attackInterval) {
        this.mob = mob;
        this.speed = speed;
        this.heightOffset = heightOffset;
        this.attackRange = attackRange;
        this.attackInterval = Math.max(1, attackInterval);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        mob.setNoGravity(true);
        attackCooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        Vec3 destination = target.position().add(0.0D, heightOffset, 0.0D);
        mob.getMoveControl().setWantedPosition(destination.x, destination.y, destination.z, speed);
        if (attackCooldown > 0) attackCooldown--;
        if (mob.distanceToSqr(target) <= attackRange * attackRange && attackCooldown == 0) {
            mob.doHurtTarget(target);
            attackCooldown = attackInterval;
        }
    }

    @Override
    public void stop() {
        mob.setNoGravity(false);
    }
}
