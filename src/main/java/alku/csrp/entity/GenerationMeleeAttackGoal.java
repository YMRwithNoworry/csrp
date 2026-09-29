package alku.csrp.entity;

import alku.csrp.world.EvolutionSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Original {@code EntityAIAttackMeleeStatus}: while the generation "Sprinting" gene is unlocked and a
 * living target is being chased, the parasite moves at 1.3x its normal navigation speed (otherwise
 * the plain 1.0x speed). The generation system replaces every melee goal with this one.
 */
public class GenerationMeleeAttackGoal extends MeleeAttackGoal {
    /** Original {@code EntityAIAttackMeleeStatus} sprinting speed towards a target. */
    public static final double SPRINT_SPEED_MULTIPLIER = 1.3D;

    private final PathfinderMob parasite;
    private final double baseSpeed;

    public GenerationMeleeAttackGoal(PathfinderMob mob, double speedModifier,
                                     boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
        this.parasite = mob;
        this.baseSpeed = speedModifier;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(parasite.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = parasite.getTarget();
        if (target == null || !target.isAlive() || !EvolutionSystem.generationProfile(level).sprinting()) {
            return;
        }
        parasite.getNavigation().setSpeedModifier(baseSpeed * SPRINT_SPEED_MULTIPLIER);
    }
}
