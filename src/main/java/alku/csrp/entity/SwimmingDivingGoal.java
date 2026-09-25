package alku.csrp.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Legacy {@code EntityAISwimmingDiving}.
 *
 * <p>While the parasite is in water (or lava) it either dives after a target that is below it in the
 * same liquid, or keeps swimming with the legacy 80% stroke. The original registered it at priority
 * zero with a {@code yMotion} of 0.08 for every infected/feral class.
 */
public final class SwimmingDivingGoal extends Goal {
    /** Legacy squared range gate for the dive (25.0). */
    private static final double DIVE_RANGE_SQR = 25.0D;
    /** Legacy required height difference: the target must be more than one block lower. */
    private static final double DIVE_HEIGHT_DIFFERENCE = 1.0D;
    /** Legacy stroke chance in {@code updateTask}. */
    private static final float STROKE_CHANCE = 0.8F;

    private final Mob mob;
    private final double diveMotion;

    public SwimmingDivingGoal(Mob mob, double diveMotion) {
        this.mob = mob;
        this.diveMotion = diveMotion;
        setFlags(EnumSet.of(Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!mob.isInWaterOrBubble() && !mob.isInLava()) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target != null && inLiquid(target)
                && target.distanceToSqr(mob.getX(), target.getY(), mob.getZ()) < DIVE_RANGE_SQR
                && target.getY() - mob.getY() < -DIVE_HEIGHT_DIFFERENCE) {
            // Legacy dive: sink toward the submerged target and let the task yield this tick.
            mob.setDeltaMovement(mob.getDeltaMovement().subtract(0.0D, diveMotion, 0.0D));
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return mob.isInWaterOrBubble() || mob.isInLava();
    }

    @Override
    public void tick() {
        // Legacy stroke: the parent jumps on 80% of its ticks while swimming.
        if (mob.getRandom().nextFloat() < STROKE_CHANCE) {
            mob.getJumpControl().jump();
        }
    }

    private static boolean inLiquid(LivingEntity entity) {
        return entity.isInWaterOrBubble() || entity.isInLava();
    }
}
