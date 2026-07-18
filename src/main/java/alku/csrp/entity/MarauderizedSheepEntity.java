package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Legacy Marauderized sheep: launches a lingering explosive nade at distant targets. */
public final class MarauderizedSheepEntity extends MarauderizedParasiteEntity {
    private static final int SHOT_COOLDOWN_TICKS = 100;

    public MarauderizedSheepEntity(EntityType<? extends MarauderizedSheepEntity> type, Level level) {
        super(type, level, 9);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMarauderizedAttributes(21.0D, 5.0D, 12.0D, 0.7D, 0.1725D, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new NadeGoal());
    }

    private void fireNade(LivingEntity target) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.35D));
        projectile.configure(this, ParasiteProjectileEntity.Mode.BOMB, start, target.getEyePosition(),
                0.55D, 3.0F, 3.0D, 60);
        level().addFreshEntity(projectile);
    }

    private final class NadeGoal extends Goal {
        private int cooldown;
        private boolean fired;

        private NadeGoal() {
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
                    && distanceToSqr(target) >= 9.0D && distanceToSqr(target) <= 256.0D;
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
                fireNade(target);
            }
            fired = true;
        }

        @Override
        public void stop() {
            cooldown = SHOT_COOLDOWN_TICKS;
        }
    }
}
