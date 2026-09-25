package alku.csrp.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Legacy {@code EntityParasiteBase.handleWater}: every liquid hit while a target is in sight adds
 * one charge (capped at four), and each charge is spent dashing at the target once the generation
 * has unlocked {@code geneWaterleap}.
 *
 * <p>The original accumulated on a periodic tick ({@code srpTicks == 1}) and spent on every tick,
 * which is what the two call shapes below reproduce.
 */
public final class LiquidLeap {
    /** Legacy cap. */
    private static final int MAX_CHARGES = 4;

    private int liquidLeap;

    /** Legacy {@code handleWater(true)}: one liquid hit adds a charge. */
    public void accumulate(Mob mob) {
        if ((mob.isInWaterOrBubble() || mob.isInLava()) && mob.getTarget() != null) {
            liquidLeap = Math.min(MAX_CHARGES, liquidLeap + 1);
        }
    }

    /** Legacy {@code handleWater(false)}: spend a charge on a dash while the gene allows it. */
    public void spend(Mob mob, boolean geneWaterLeap) {
        if (liquidLeap < 1) {
            return;
        }
        if (!geneWaterLeap) {
            liquidLeap--;
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        // The original used weaker values while still submerged and stronger ones at the surface.
        boolean submerged = mob.isInWaterOrBubble() || mob.isInLava();
        double vertical = submerged ? 0.1D : 0.3D;
        double strength = submerged ? 0.5D : 1.0D;
        liquidLeap--;
        mob.getNavigation().stop();
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001D) {
            length = 0.001D;
        }
        Vec3 motion = mob.getDeltaMovement();
        mob.setDeltaMovement(
                motion.x + (dx / length * strength * 0.8D + motion.x * 0.2D),
                vertical,
                motion.z + (dz / length * strength * 0.8D + motion.z * 0.2D));
        mob.hasImpulse = true;
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    public int charges() {
        return liquidLeap;
    }
}
