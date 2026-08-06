package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Legacy hijacked skeleton ranged spineball attacker. */
public final class HiSkeletonEntity extends HijackedParasiteEntity {
    private int rangedCooldown = 20;

    public HiSkeletonEntity(EntityType<? extends HiSkeletonEntity> type, Level level) {
        super(type, level, 30);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return HijackedParasiteEntity.createAttributes(27.0D, 8.0D, 17.0D, 0.9D, 0.205D, 48.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new SkeletonRangedGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && rangedCooldown > 0) {
            rangedCooldown--;
        }
    }

    private void fireSpineball(LivingEntity target) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.35D));
        projectile.configure(this, ParasiteProjectileEntity.Mode.SPINE, start, target.getEyePosition(),
                1.0D, 3.0F, 0.75D, 60);
        level().addFreshEntity(projectile);
    }

    private final class SkeletonRangedGoal extends Goal {
        private SkeletonRangedGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && rangedCooldown <= 0 && hasLineOfSight(target);
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            triggerAttackAnimation();
            fireSpineball(target);
            rangedCooldown = 40;
        }
    }
}
