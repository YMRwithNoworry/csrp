package alku.csrp.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Legacy {@code EntityParasiteBase.EntityAIJumping}: once every ten ticks, a grounded parasite
 * whose target sits more than one block above its eyes and within two blocks horizontally stops
 * pathing and hops up at it.
 *
 * <p>The original did its work inside {@code shouldExecute} and always returned false, so this goal
 * mirrors that: it is a periodic trigger rather than a running task.
 */
public final class JumpAtHigherTargetGoal extends Goal {
    /** Legacy guard: the check runs on every tenth tick. */
    private static final int CHECK_INTERVAL_TICKS = 10;
    /** Legacy 4.0 squared-distance gate and the one block height difference. */
    private static final double VERTICAL_RANGE_SQR = 4.0D;
    private static final double REQUIRED_HEIGHT_DIFFERENCE = 1.0D;
    private static final double HORIZONTAL_STRENGTH = 0.5D;

    private final Mob mob;
    private int ticks;

    public JumpAtHigherTargetGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (++ticks < CHECK_INTERVAL_TICKS) {
            return false;
        }
        ticks = 0;
        LivingEntity target = mob.getTarget();
        if (target == null || !mob.onGround()) {
            return false;
        }
        // The legacy check measured against the target's own height and the mob's eye position.
        if (target.distanceToSqr(mob.getX(), target.getY(), mob.getZ()) >= VERTICAL_RANGE_SQR
                || target.getY() - (mob.getY() + mob.getEyeHeight()) <= REQUIRED_HEIGHT_DIFFERENCE) {
            return false;
        }
        mob.getNavigation().stop();
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001D) {
            length = 0.001D;
        }
        Vec3 motion = mob.getDeltaMovement();
        mob.setDeltaMovement(
                motion.x + (dx / length * HORIZONTAL_STRENGTH * 0.8D + motion.x * 0.2D),
                0.2D + mob.getBbHeight() * 0.15D,
                motion.z + (dz / length * HORIZONTAL_STRENGTH * 0.8D + motion.z * 0.2D));
        mob.hasImpulse = true;
        return false;
    }
}
