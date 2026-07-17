package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
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
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class DraconiteEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(DraconiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");

    private int toxicCloudCooldown = 40;
    private int meteorCooldown = 80;
    private int lightCooldown = 20;

    public DraconiteEntity(EntityType<? extends DraconiteEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 525.0)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 210.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FLYING_SPEED, 0.27)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new DraconiteCombatGoal());
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(isFlying());
        if (level().isClientSide) {
            return;
        }
        if (toxicCloudCooldown > 0) toxicCloudCooldown--;
        if (meteorCooldown > 0) meteorCooldown--;
        if (lightCooldown > 0) lightCooldown--;

        switchFlightMode();
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (toxicCloudCooldown <= 0 && distanceToSqr(target) < 400.0) {
            spawnToxicCloud(target);
            toxicCloudCooldown = 120;
        }
        if (meteorCooldown <= 0 && distanceToSqr(target) > 25.0) {
            performMeteorBarrage(target);
            meteorCooldown = 160;
        }
        if (lightCooldown <= 0 && hasLineOfSight(target)) {
            performLightBarrage(target);
            lightCooldown = 100;
        }
    }

    private void switchFlightMode() {
        LivingEntity target = getTarget();
        if (target == null) {
            if (tickCount % 200 == 0) setFlying(false);
            return;
        }
        double distance = distanceToSqr(target);
        if (distance > 144.0 || target.getY() > getY() + 3.0) {
            setFlying(true);
        } else if (distance < 36.0 && onGround()) {
            setFlying(false);
        }
    }

    private void spawnToxicCloud(LivingEntity target) {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), target.getX(), target.getY(), target.getZ());
        cloud.setOwner(this);
        cloud.setRadius(4.0F);
        cloud.setDuration(160);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-0.02F);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 120, 1));
        level().addFreshEntity(cloud);
    }

    private void performMeteorBarrage(LivingEntity target) {
        for (int i = 0; i < 3; i++) {
            Vec3 destination = target.position().add(random.nextGaussian() * 2.5, 0.5, random.nextGaussian() * 2.5);
            Vec3 start = destination.add(random.nextGaussian() * 2.0, 12.0 + random.nextDouble() * 6.0,
                    random.nextGaussian() * 2.0);
            fireProjectile(ParasiteProjectileEntity.Mode.METEOR, start, destination, 0.75,
                    42.0F, 4.0, 50);
        }
        playSound(ModSounds.DRACONITE_FIRE_SHOOT.get(), 2.0F, 0.8F);
    }

    private void performLightBarrage(LivingEntity target) {
        Vec3 start = getEyePosition();
        for (int i = 0; i < 5; i++) {
            Vec3 destination = target.getEyePosition().add(random.nextGaussian(), random.nextGaussian() * 0.5,
                    random.nextGaussian());
            fireProjectile(ParasiteProjectileEntity.Mode.LIGHT, start, destination, 1.25,
                    24.0F, 1.5, 60);
        }
        playSound(ModSounds.DRACONITE_FIRE_SHOOT.get(), 2.0F, 1.2F);
    }

    private void fireProjectile(ParasiteProjectileEntity.Mode mode, Vec3 start, Vec3 destination,
                                double speed, float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        projectile.configure(this, mode, start, destination, speed, damage, radius, lifetime);
        level().addFreshEntity(projectile);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hurt = super.doHurtTarget(entity);
        if (hurt && entity instanceof LivingEntity target) {
            target.igniteForSeconds(5.0F);
        }
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    private void setFlying(boolean flying) {
        entityData.set(FLYING, flying);
        setNoGravity(flying);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.DRACONITE_LIVING.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DRACONITE_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("flying", isFlying());
        tag.putInt("toxic_cloud_cooldown", toxicCloudCooldown);
        tag.putInt("meteor_cooldown", meteorCooldown);
        tag.putInt("light_cooldown", lightCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFlying(tag.getBoolean("flying"));
        toxicCloudCooldown = tag.getInt("toxic_cloud_cooldown");
        meteorCooldown = tag.getInt("meteor_cooldown");
        lightCooldown = tag.getInt("light_cooldown");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (isFlying()) return state.setAndContinue(FLY);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
    }

    private final class DraconiteCombatGoal extends Goal {
        private int attackCooldown;

        private DraconiteCombatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (isFlying()) {
                getMoveControl().setWantedPosition(target.getX(), target.getY() + 4.0, target.getZ(), 1.0);
            } else {
                getNavigation().moveTo(target, 1.1);
            }
            if (attackCooldown > 0) attackCooldown--;
            if (distanceToSqr(target) < 16.0 && attackCooldown <= 0) {
                doHurtTarget(target);
                attackCooldown = 20;
            }
        }
    }
}
