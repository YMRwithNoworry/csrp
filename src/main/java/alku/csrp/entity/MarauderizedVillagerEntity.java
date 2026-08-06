package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Legacy Marauderized villager: rapid spineball fire at targets outside melee range. */
public final class MarauderizedVillagerEntity extends MarauderizedParasiteEntity {
    private static final int SHOT_COOLDOWN_TICKS = 40;

    public MarauderizedVillagerEntity(EntityType<? extends MarauderizedVillagerEntity> type, Level level) {
        super(type, level, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMarauderizedAttributes(27.0D, 8.0D, 17.0D, 0.9D, 0.225D, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new SpineballGoal());
    }

    private void fireSpineball(LivingEntity target) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.35D));
        projectile.configure(this, ParasiteProjectileEntity.Mode.SPINE, start, target.getEyePosition(),
                0.85D, 3.0F, 0.75D, 50);
        level().addFreshEntity(projectile);
        triggerAnim("attack_controller", "attack");
    }

    private final class SpineballGoal extends Goal {
        private int cooldown;
        private boolean fired;

        private SpineballGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && hasLineOfSight(target)
                    && distanceToSqr(target) >= 4.0D && distanceToSqr(target) <= 256.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return !fired;
        }

        @Override
        public void start() {
            fired = false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                fireSpineball(target);
            }
            fired = true;
        }

        @Override
        public void stop() {
            cooldown = SHOT_COOLDOWN_TICKS;
        }
    }
}
