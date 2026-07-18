package alku.csrp.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Legacy Marauderized human: pounces onto a victim and disables it while riding. */
public final class MarauderizedHumanEntity extends MarauderizedParasiteEntity {
    private static final double MOUNT_DISTANCE_SQR = 6.25D;
    private static final int MOUNT_COOLDOWN_TICKS = 40;

    private int mountCooldown;
    private int pushDirectionTicks;
    private double pushX;
    private double pushZ;

    public MarauderizedHumanEntity(EntityType<? extends MarauderizedHumanEntity> type, Level level) {
        super(type, level, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMarauderizedAttributes(24.0D, 7.0D, 15.0D, 0.3D, 0.27D, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new PounceMountGoal());
    }

    @Override
    protected double meleeSpeed() {
        return 1.5D;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (mountCooldown > 0) {
            mountCooldown--;
        }

        Entity vehicle = getVehicle();
        if (vehicle instanceof LivingEntity victim && victim.isAlive() && isValidParasiteTarget(victim)) {
            controlMountedVictim(victim);
        } else if (vehicle != null) {
            stopRiding();
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return distance >= 60.0F && super.causeFallDamage(distance, damageMultiplier, source);
    }

    private void controlMountedVictim(LivingEntity victim) {
        if (--pushDirectionTicks <= 0) {
            Vec3 randomDirection = new Vec3(random.nextDouble() - 0.5D, 0.0D, random.nextDouble() - 0.5D);
            if (randomDirection.lengthSqr() < 0.001D) {
                randomDirection = new Vec3(1.0D, 0.0D, 0.0D);
            }
            randomDirection = randomDirection.normalize();
            pushX = randomDirection.x;
            pushZ = randomDirection.z;
            pushDirectionTicks = 20;
        }

        victim.push(pushX * 0.13D, 0.0D, pushZ * 0.13D);
        victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false), this);
        victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 0, false, false), this);
        victim.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 0, false, false), this);
        if (tickCount % 20 == 0) {
            doHurtTarget(victim);
        }
    }

    private boolean mountTarget(LivingEntity target) {
        if (target.isVehicle() || distanceToSqr(target) >= MOUNT_DISTANCE_SQR || !startRiding(target, true)) {
            return false;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false), this);
        mountCooldown = MOUNT_COOLDOWN_TICKS;
        return true;
    }

    private final class PounceMountGoal extends Goal {
        private int pounceCooldown;

        private PounceMountGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return mountCooldown <= 0 && !isPassenger() && target != null && target.isAlive()
                    && isValidParasiteTarget(target) && !target.isVehicle() && hasLineOfSight(target)
                    && distanceToSqr(target) <= 36.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return !isPassenger() && target != null && target.isAlive() && isValidParasiteTarget(target)
                    && !target.isVehicle() && distanceToSqr(target) <= 49.0D;
        }

        @Override
        public void start() {
            pounceCooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (mountTarget(target)) {
                getNavigation().stop();
                return;
            }

            getNavigation().moveTo(target, 1.5D);
            if (pounceCooldown > 0) {
                pounceCooldown--;
                return;
            }
            if (!onGround()) {
                return;
            }

            Vec3 direction = target.position().subtract(position());
            if (direction.lengthSqr() < 0.001D) {
                return;
            }
            direction = direction.normalize();
            setDeltaMovement(getDeltaMovement().multiply(0.25D, 0.0D, 0.25D)
                    .add(direction.x * 0.62D, 0.42D, direction.z * 0.62D));
            hasImpulse = true;
            pounceCooldown = 20;
        }
    }
}
