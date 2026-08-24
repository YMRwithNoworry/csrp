package alku.csrp.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Periodically dashes sideways around a target to avoid predictable melee paths. */
public class EvadeDashGoal extends Goal {
    private final Mob mob;
    private final double dashSpeed;
    private final double minDistance;
    private final double maxDistance;
    private final int cooldownTicks;
    private int cooldown;

    public EvadeDashGoal(Mob mob, double dashSpeed, double minDistance, double maxDistance, int cooldownTicks) {
        this.mob = mob;
        this.dashSpeed = dashSpeed;
        this.minDistance = minDistance;
        this.maxDistance = Math.max(minDistance, maxDistance);
        this.cooldownTicks = Math.max(1, cooldownTicks);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && cooldown <= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        cooldown = cooldownTicks;
        if (target == null) return;
        double distance = mob.distanceTo(target);
        if (distance < minDistance || distance > maxDistance) return;
        Vec3 away = mob.position().subtract(target.position()).normalize();
        Vec3 side = new Vec3(-away.z, 0.0D, away.x)
                .scale(mob.getRandom().nextBoolean() ? 1.0D : -1.0D);
        mob.setDeltaMovement(side.normalize().scale(dashSpeed).add(0.0D, 0.08D, 0.0D));
    }

}
