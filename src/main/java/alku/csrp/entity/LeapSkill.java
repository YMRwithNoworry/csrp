package alku.csrp.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Legacy {@code EntityParasiteBase.skillLeap()} driven through {@code attackID = 14} — the hijacked
 * head family's leap. The original runs it from {@code EntityAISkill(this, 40, 100, 3, true, 14)}
 * and configures it with {@code setskillLeapValues(0.7F, 2.5, 0)}.
 *
 * <p>Behaviour (EntityParasiteBase:2427): remember the target's X/Z on the first tick; on the next
 * tick, while grounded, stop navigation, set parasite status 10 and add the leap velocity towards
 * the remembered point — vertical {@code leapMotionY} plus {@code jumpSpeed * 0.9} horizontally,
 * keeping 30% of the existing horizontal motion. A non-zero {@code jumpRadius} would add landing
 * damage; the heads pass 0, so there is none.
 */
public final class LeapSkill implements ParasiteSkillGoal.ParasiteSkill {
    private final Mob mob;
    private final double leapMotionY;
    private final double jumpSpeed;
    private final int jumpRadius;

    private int phase;
    private double targetX;
    private double targetZ;

    public LeapSkill(Mob mob, float leapMotionY, double jumpSpeed, int jumpRadius) {
        this.mob = mob;
        this.leapMotionY = leapMotionY;
        this.jumpSpeed = jumpSpeed;
        this.jumpRadius = jumpRadius;
    }

    @Override
    public void tick() {
        if (phase == 0) {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                targetX = target.getX();
                targetZ = target.getZ();
                phase = 1;
            }
            return;
        }
        phase++;
        if (phase == 2 && mob.onGround()) {
            mob.getNavigation().stop();
            double dx = targetX - mob.getX();
            double dz = targetZ - mob.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.0D) {
                mob.setDeltaMovement(mob.getDeltaMovement().x
                                + dx / distance * jumpSpeed * 0.9D + mob.getDeltaMovement().x * 0.3D,
                        leapMotionY,
                        mob.getDeltaMovement().z
                                + dz / distance * jumpSpeed * 0.9D + mob.getDeltaMovement().z * 0.3D);
            } else {
                mob.setDeltaMovement(mob.getDeltaMovement().x, leapMotionY, mob.getDeltaMovement().z);
            }
        }
        // jumpRadius is 0 for the heads, so the original's landing-damage branch never runs.
    }

    @Override
    public boolean isFinished() {
        return phase > 2 && mob.onGround();
    }

    /** Unused by the heads (their {@code jumpR} is 0) but kept so the value is not lost. */
    public int jumpRadius() {
        return jumpRadius;
    }
}
