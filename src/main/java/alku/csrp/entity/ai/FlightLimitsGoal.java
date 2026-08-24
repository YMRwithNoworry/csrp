package alku.csrp.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Keeps a flying mob within a configurable distance above the nearest solid floor. */
public class FlightLimitsGoal extends Goal {
    private final Mob mob;
    private final int minHeight;
    private final int maxHeight;

    public FlightLimitsGoal(Mob mob, int minHeight, int maxHeight) {
        this.mob = mob;
        this.minHeight = Math.max(1, minHeight);
        this.maxHeight = Math.max(this.minHeight, maxHeight);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return mob.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        BlockPos floor = findFloor(mob.blockPosition());
        int height = floor == null ? maxHeight : mob.blockPosition().getY() - floor.getY();
        double correction = height < minHeight ? 0.08D : height > maxHeight ? -0.08D : 0.0D;
        if (correction != 0.0D) {
            mob.setDeltaMovement(mob.getDeltaMovement().add(0.0D, correction, 0.0D));
        }
    }

    private BlockPos findFloor(BlockPos start) {
        for (int offset = 0; offset <= 32; offset++) {
            BlockPos candidate = start.below(offset);
            if (!mob.level().getBlockState(candidate).getCollisionShape(mob.level(), candidate).isEmpty()) {
                return candidate;
            }
        }
        return null;
    }
}
