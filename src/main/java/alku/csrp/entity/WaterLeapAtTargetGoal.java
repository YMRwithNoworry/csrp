package alku.csrp.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Legacy {@code EntityAIWaterLeapAtTargetStatus}.
 *
 * <p>While the parasite is in water (or lava, or already mid-leap) it aims at its target for
 * {@code cooldown} ticks, then launches itself at the recorded position and, if a damage range was
 * configured, hits everything around the landing spot. The generation must have enabled the
 * legacy {@code geneWaterleap} flag for the goal to run at all.
 *
 * <p>The original held its own status code (10 while leaping, 2 after landing); the port maps that
 * onto the shared special-leap animation state instead, so mobs keep their own status semantics.
 */
public final class WaterLeapAtTargetGoal extends Goal {
    private final PrimitiveParasiteEntity leaper;
    private final float leapMotionY;
    private final double jumpSpeed;
    private final int cooldown;
    private final double damageRange;
    private int attackTimer;
    private int attacking;
    private double targetX;
    private double targetY;
    private double targetZ;

    public WaterLeapAtTargetGoal(PrimitiveParasiteEntity leaper, float leapMotionY, double jumpSpeed,
                                 int cooldown, double damageRange) {
        this.leaper = leaper;
        this.leapMotionY = leapMotionY;
        this.jumpSpeed = jumpSpeed;
        this.cooldown = cooldown;
        this.damageRange = damageRange;
    }

    @Override
    public boolean canUse() {
        // Legacy geneWaterleap gate, then the original's wet-or-mid-leap condition.
        return leaper.waterLeapEnabled()
                && (leaper.isInWaterOrBubble() || leaper.isInLava() || attacking >= 1);
    }

    @Override
    public void tick() {
        LivingEntity target = leaper.getTarget();
        if (target != null && !target.isRemoved() && !leaper.isSpecialLeapAnimating()
                && target.isAlive()) {
            attackTimer++;
            if (attackTimer >= cooldown && attacking == 0) {
                attacking = 1;
                targetX = target.getX();
                targetZ = target.getZ();
                targetY = Math.max(0.0D, (target.getY() - leaper.getY()) * 0.07D);
            }
        } else if (attackTimer > 0) {
            attackTimer--;
        }
        if (attacking < 1) {
            return;
        }
        attacking++;
        if (attacking == 2 && leaper.onGround()) {
            leaper.startSpecialLeapAnimation(leapAnimationTicks());
            leaper.getNavigation().stop();
            double dx = targetX - leaper.getX();
            double dz = targetZ - leaper.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 0.001D) {
                length = 0.001D;
            }
            Vec3 motion = leaper.getDeltaMovement();
            leaper.setDeltaMovement(
                    motion.x + (dx / length * jumpSpeed * 0.9D + motion.x * 0.3D),
                    leapMotionY + targetY,
                    motion.z + (dz / length * jumpSpeed * 0.9D + motion.z * 0.3D));
            leaper.hasImpulse = true;
        }
        if (attacking >= 3 && leaper.onGround()) {
            if (damageRange > 0.0D) {
                for (LivingEntity victim : leaper.level().getEntitiesOfClass(LivingEntity.class,
                        leaper.getBoundingBox().inflate(damageRange, 2.0D, damageRange))) {
                    if (victim == leaper || victim instanceof Parasite) {
                        continue;
                    }
                    double dx = leaper.getX() - victim.getX();
                    double dz = leaper.getZ() - victim.getZ();
                    double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
                    victim.push(dx / length * 2.5D, 0.4D, dz / length * 2.5D);
                    leaper.doHurtTarget(victim);
                }
            }
            attacking = 0;
            attackTimer = 0;
        }
    }

    /** The original ran a ten tick leap animation for status 10. */
    private int leapAnimationTicks() {
        return 10;
    }
}
