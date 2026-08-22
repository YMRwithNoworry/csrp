package alku.csrp.entity;

import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.EnumSet;
import java.util.List;

public final class CarrierFlyingEntity extends CarrierEntity {
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");

    public CarrierFlyingEntity(EntityType<? extends CarrierFlyingEntity> type, Level level) {
        super(type, level, 30, 4.0, 1, 300, 400);
        moveControl = new CarrierFlyingMoveControl();
        setNoGravity(true);
        xpReward = 16;
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
        goalSelector.addGoal(6, new FlyingRandomMoveGoal());
    }

    @Override
    protected boolean usesDefaultMovementGoals() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!isRemoved()) {
            setNoGravity(true);
            if (!level().isClientSide && onGround()) {
                getMoveControl().setWantedPosition(getX(), getY() + 5.0, getZ(), 0.5);
            }
            if (!level().isClientSide && getY() > MobsConfig.carrierFlyingMaxY()) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.08D, 0.0D));
            }
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected RawAnimation ageAnimation() {
        return AGE_IN_TICKS;
    }

    @Override
    protected boolean griefingEnabled() {
        return MobsConfig.carrierFlyingGriefing();
    }

    @Override
    protected List<? extends String> spawnTable() {
        return MobsConfig.carrierFlyingMobTable();
    }

    @Override
    protected int variantViralAmplifier() {
        return 1;
    }

    @Override
    protected double variantViralRadius() {
        return 4.0D;
    }

    @Override
    protected int variantCloudDuration() {
        return 300;
    }

    @Override
    protected int fuseIncrement() {
        return 1;
    }

    @Override
    protected boolean startsFuseAtLowHealth() {
        return false;
    }

    @Override
    protected SoundEvent explosionSound() {
        return ModSounds.get("buthol.boom");
    }

    @Override
    protected float explosionVolume() {
        return 1.0F;
    }

    private final class FlyingCombatGoal extends Goal {
        private boolean charging;

        private FlyingCombatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && !isDetonating() && !getMoveControl().hasWanted()
                    && random.nextInt(7) == 0 && distanceToSqr(target) > 4.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && charging && getMoveControl().hasWanted()
                    && !isDetonating();
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                getMoveControl().setWantedPosition(target.getX(), target.getEyeY(), target.getZ(), 1.0D);
                charging = true;
            }
        }

        @Override
        public void stop() {
            charging = false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }

            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = distanceToSqr(target);
            if (getBoundingBox().intersects(target.getBoundingBox())) {
                doHurtTarget(target);
                charging = false;
            } else if (distance < 9.0D) {
                getMoveControl().setWantedPosition(target.getX(),
                        Math.min(target.getEyeY(), MobsConfig.carrierFlyingMaxY()), target.getZ(), 1.0D);
            }
        }
    }

    private final class FlyingRandomMoveGoal extends Goal {
        private FlyingRandomMoveGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !isDetonating() && !getMoveControl().hasWanted() && random.nextInt(7) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos origin = blockPosition();
            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos destination = origin.offset(random.nextInt(15) - 7,
                        random.nextInt(11) - 5, random.nextInt(15) - 7);
                if (destination.getY() <= MobsConfig.carrierFlyingMaxY()
                        && level().getBlockState(destination).isAir()) {
                    getMoveControl().setWantedPosition(destination.getX() + 0.5D,
                            destination.getY() + 0.5D, destination.getZ() + 0.5D, 0.25D);
                    if (getTarget() == null) {
                        getLookControl().setLookAt(destination.getX() + 0.5D,
                                destination.getY() + 0.5D, destination.getZ() + 0.5D);
                    }
                    return;
                }
            }
        }
    }

    private final class CarrierFlyingMoveControl extends MoveControl {
        private CarrierFlyingMoveControl() {
            super(CarrierFlyingEntity.this);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            double dx = wantedX - getX();
            double dy = wantedY - getY();
            double dz = wantedZ - getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                setDeltaMovement(getDeltaMovement().scale(0.5D));
                return;
            }
            setDeltaMovement(getDeltaMovement().add(
                    dx / distance * 0.05D * speedModifier,
                    dy / distance * 0.05D * speedModifier,
                    dz / distance * 0.05D * speedModifier));
            LivingEntity target = getTarget();
            double lookX = target == null ? getDeltaMovement().x : target.getX() - getX();
            double lookZ = target == null ? getDeltaMovement().z : target.getZ() - getZ();
            setYRot(-((float) Math.atan2(lookX, lookZ)) * net.minecraft.util.Mth.RAD_TO_DEG);
            yBodyRot = getYRot();
        }
    }
}
