package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

/** Legacy hijacked skeleton ranged spineball attacker. */
public final class HiSkeletonEntity extends HijackedParasiteEntity {
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            HiSkeletonEntity.class, EntityDataSerializers.INT);
    private final RawAnimation ageInTicksAnimation = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation limbSwingAnimation = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation rangedLimbAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private int rangedCooldown = 20;

    public HiSkeletonEntity(EntityType<? extends HiSkeletonEntity> type, Level level) {
        super(type, level, 30);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return HijackedParasiteEntity.createAttributes(27.0D, 8.0D, 17.0D, 0.9D, 0.205D, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, 0);
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
        if (!level().isClientSide) {
            entityData.set(PARASITE_STATUS, getTarget() != null && getTarget().isAlive() ? 2 : 0);
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 0,
                state -> state.setAndContinue(ageInTicksAnimation)));
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                return PlayState.STOP;
            }
            return state.setAndContinue(entityData.get(PARASITE_STATUS) == 2
                    ? rangedLimbAnimation : limbSwingAnimation);
        }));
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
            fireSpineball(target);
            rangedCooldown = 40;
        }
    }
}
