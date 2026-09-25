package alku.csrp.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Legacy {@code EntityAIAttackSwell}: a creeper-style proximity goal that drives the SELFE fuse.
 *
 * <pre>
 * shouldExecute: getSelfeState() &gt; 0 || (target != null &amp;&amp; distanceSqr &lt; distance)
 * updateTask:    no target | distanceSqr &gt; 49.0 | no line of sight  -&gt; setSelfeState(-1)
 *                otherwise                                          -&gt; setSelfeState(1)
 * </pre>
 *
 * <p>The original's horse overrides {@code setSelfeState} so the state only advances below half
 * health; {@code requireHalfHealth} reproduces that gate here so the goal itself is faithful for
 * mobs that do not override the setter.
 */
public class AttackSwellGoal extends Goal {
    /** Legacy {@code func_75246_d}: beyond this squared distance (7 blocks) the swell is cancelled. */
    private static final double CANCEL_DISTANCE_SQR = 49.0D;

    private final Mob mob;
    private final double distanceSqr;
    private final boolean requireHalfHealth;
    private LivingEntity target;

    public AttackSwellGoal(Mob mob, double distance, boolean requireHalfHealth) {
        this.mob = mob;
        this.distanceSqr = distance * distance;
        this.requireHalfHealth = requireHalfHealth;
        setFlags(java.util.EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (ParasiteFuseState.getStateOf(mob) > 0) {
            return true;
        }
        LivingEntity current = mob.getTarget();
        return current != null && mob.distanceToSqr(current) < distanceSqr;
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        target = mob.getTarget();
    }

    @Override
    public void stop() {
        target = null;
    }

    @Override
    public void tick() {
        if (target == null || mob.distanceToSqr(target) > CANCEL_DISTANCE_SQR || !mob.getSensing().hasLineOfSight(target)) {
            ParasiteFuseState.setStateOf(mob, -1);
            return;
        }
        if (requireHalfHealth && mob.getHealth() > mob.getMaxHealth() * 0.5F) {
            return;
        }
        ParasiteFuseState.setStateOf(mob, 1);
    }
}
