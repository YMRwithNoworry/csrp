package alku.csrp.entity.ai;

import alku.csrp.infection.BlockInfestation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Periodically converts one nearby block using the shared infestation rules. */
public class BlockInfestGoal extends Goal {
    private final Mob mob;
    private final int radius;
    private final int stage;
    private final int interval;
    private int cooldown;

    public BlockInfestGoal(Mob mob, int radius, int stage, int interval) {
        this.mob = mob;
        this.radius = Math.max(1, radius);
        this.stage = Math.max(0, Math.min(3, stage));
        this.interval = Math.max(1, interval);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        return mob.level() instanceof ServerLevel && cooldown <= 0 && mob.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        cooldown = interval;
        ServerLevel level = (ServerLevel) mob.level();
        BlockPos origin = mob.blockPosition();
        for (int attempt = 0; attempt < radius * 6; attempt++) {
            BlockPos candidate = origin.offset(mob.getRandom().nextInt(radius * 2 + 1) - radius,
                    mob.getRandom().nextInt(radius * 2 + 1) - radius,
                    mob.getRandom().nextInt(radius * 2 + 1) - radius);
            if (BlockInfestation.convert(level, candidate, stage)) return;
        }
        for (Direction direction : Direction.values()) {
            if (BlockInfestation.convert(level, origin.relative(direction), stage)) return;
        }
    }

}
