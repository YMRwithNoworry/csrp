package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

import java.util.EnumSet;

/** Legacy hijacked blaze: aerial spineball volleys and nearby parasite illumination. */
public final class HiBlazeEntity extends HijackedParasiteEntity {
    private int rangedCooldown = 20;
    private int burstShots;
    private int burstDelay;

    public HiBlazeEntity(EntityType<? extends HiBlazeEntity> type, Level level) {
        super(type, level, 36);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return HijackedParasiteEntity.createAttributes(30.0D, 3.5D, 3.5D, 0.2D, 0.25D, 48.0D)
                .add(Attributes.FLYING_SPEED, 0.25D);
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
        goalSelector.addGoal(1, new SpineBurstGoal());
        goalSelector.addGoal(2, new BlazeFlightGoal());
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide) {
            return;
        }
        if (onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.5D);
        }
        if (rangedCooldown > 0) {
            rangedCooldown--;
        }
        if (tickCount % 10 == 0) {
            illuminateNearbyParasites();
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    private void illuminateNearbyParasites() {
        for (LivingEntity parasite : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(7.0D, 3.0D, 7.0D),
                entity -> entity != this && entity instanceof Parasite)) {
            parasite.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true), this);
        }
    }

    private void fireSpineball(LivingEntity target) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.35D));
        projectile.configure(this, ParasiteProjectileEntity.Mode.SPINE, start, target.getEyePosition(),
                0.85D, 5.0F, 0.75D, 50);
        level().addFreshEntity(projectile);
    }

    private final class SpineBurstGoal extends Goal {
        private SpineBurstGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && rangedCooldown <= 0 && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return burstShots > 0 && target != null && target.isAlive();
        }

        @Override
        public void start() {
            burstShots = 4;
            burstDelay = 0;
            rangedCooldown = 80;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || --burstDelay > 0) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            fireSpineball(target);
            burstShots--;
            burstDelay = 8;
        }
    }

    private final class BlazeFlightGoal extends Goal {
        private BlazeFlightGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            getMoveControl().setWantedPosition(target.getX(), target.getY() + 3.0D, target.getZ(), 1.0D);
        }
    }
}
