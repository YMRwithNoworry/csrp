package alku.csrp.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class CarrierFlyingEntity extends CarrierEntity {
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");

    public CarrierFlyingEntity(EntityType<? extends CarrierFlyingEntity> type, Level level) {
        super(type, level, 30, 0, 4.0, 0, 300);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 36;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ARMOR, 2.5)
                .add(Attributes.ATTACK_DAMAGE, 15.0).add(Attributes.FLYING_SPEED, 0.25)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.KNOCKBACK_RESISTANCE, 0.15)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected boolean usesMeleeAttack() {
        return false;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new FlyingCombatGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!isRemoved()) {
            setNoGravity(true);
            if (!level().isClientSide && onGround()) {
                getMoveControl().setWantedPosition(getX(), getY() + 5.0, getZ(), 0.5);
            }
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected RawAnimation idleAnimation() {
        return IDLE;
    }

    private final class FlyingCombatGoal extends Goal {
        private boolean charging;
        private int chargeTicks;

        private FlyingCombatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && !isDetonating();
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() != null && getTarget().isAlive() && !isDetonating();
        }

        @Override
        public void stop() {
            charging = false;
            chargeTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }

            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = distanceToSqr(target);
            if (!charging && distance > 4.0 && random.nextInt(7) == 0) {
                charging = true;
                chargeTicks = 0;
            }

            if (charging) {
                Vec3 direction = target.getEyePosition().subtract(position());
                if (direction.lengthSqr() > 0.001) {
                    setDeltaMovement(direction.normalize().scale(0.65));
                }
                if (distance < 4.0 || ++chargeTicks > 20) {
                    if (distance < 4.0) {
                        doHurtTarget(target);
                    }
                    charging = false;
                }
            } else {
                getNavigation().moveTo(target.getX(), target.getY() + 1.0, target.getZ(), 1.0);
            }
        }
    }
}
