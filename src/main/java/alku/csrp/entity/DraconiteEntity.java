package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
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

public final class DraconiteEntity extends DerivedParasiteEntity {
    private static final int FIRE_BREATH_DURATION_TICKS = 60;
    private static final int FIRE_BREATH_COOLDOWN_TICKS = 180;
    private static final int FIRE_BREATH_SHOT_INTERVAL_TICKS = 10;
    private static final int METEOR_TELEGRAPH_TICKS = 40;
    private static final int METEOR_SALVO_TICKS = 15;
    private static final int METEOR_COOLDOWN_TICKS = 220;
    private static final int LIGHT_BARRAGE_COUNT = 20;

    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(DraconiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FIRE_BREATH_TICKS =
            SynchedEntityData.defineId(DraconiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> FIRE_BREATH_TARGET =
            SynchedEntityData.defineId(DraconiteEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> METEOR_TICKS =
            SynchedEntityData.defineId(DraconiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> METEOR_TARGET =
            SynchedEntityData.defineId(DraconiteEntity.class, EntityDataSerializers.BLOCK_POS);
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation FLY = RawAnimation.begin()
            .thenLoop("animation.draconite.idle.get_flying_state_1");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation CLONE_IDLE = ParasiteAnimations.loop(this, "idle.get_clone_c_1");
    private final RawAnimation CLONE_WALK = ParasiteAnimations.loop(this, "walk.get_clone_c_1");
    private final RawAnimation SHAKING_IDLE = ParasiteAnimations.loop(this, "idle.shaking_c_1");
    private final RawAnimation SHAKING_WALK = ParasiteAnimations.loop(this, "walk.shaking_c_1");
    private final RawAnimation CLONE_SHAKING_IDLE = ParasiteAnimations.loop(
            this, "idle.get_clone_c_1.shaking_c_1");
    private final RawAnimation CLONE_SHAKING_WALK = ParasiteAnimations.loop(
            this, "walk.get_clone_c_1.shaking_c_1");
    private final RawAnimation FIRE_BREATH = ParasiteAnimations.loop(this,
            "idle.get_parasite_status_3");
    private final RawAnimation FIRE_BREATH_SHAKING = ParasiteAnimations.loop(this,
            "idle.get_parasite_status_3.shaking_c_1");
    private final RawAnimation CLONE_FIRE_BREATH = ParasiteAnimations.loop(this,
            "idle.get_clone_c_1.get_parasite_status_3");
    private final RawAnimation CLONE_FIRE_BREATH_SHAKING = ParasiteAnimations.loop(this,
            "idle.get_clone_c_1.get_parasite_status_3.shaking_c_1");

    private int salivaCooldown = 40;
    private int meteorCooldown = 80;
    private int lightCooldown = 80;
    private int fireBreathCooldown = 100;
    private int fireBreathTicks;
    private int meteorRainTicks;
    private BlockPos fireBreathTarget = BlockPos.ZERO;
    private BlockPos meteorTarget = BlockPos.ZERO;

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
        builder.define(FIRE_BREATH_TICKS, 0);
        builder.define(FIRE_BREATH_TARGET, BlockPos.ZERO);
        builder.define(METEOR_TICKS, 0);
        builder.define(METEOR_TARGET, BlockPos.ZERO);
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
            spawnFireBreathParticles();
            spawnMeteorWarningParticles();
            return;
        }

        tickCooldowns();
        if (tickMeteorRain() || tickFireBreath()) {
            return;
        }

        switchFlightMode();
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (meteorCooldown <= 0 && distanceToSqr(target) > 25.0D) {
            beginMeteorRain(target);
            return;
        }
        if (fireBreathCooldown <= 0 && !isFlying() && hasLineOfSight(target)
                && distanceToSqr(target) >= 16.0D && distanceToSqr(target) <= 900.0D) {
            beginFireBreath(target);
            return;
        }
        if (salivaCooldown <= 0 && hasLineOfSight(target)) {
            shootSalivaBall(target);
            salivaCooldown = 80;
            return;
        }
        if (lightCooldown <= 0 && hasLineOfSight(target)) {
            performLightBarrage(target);
            lightCooldown = 180;
        }
    }

    private void tickCooldowns() {
        if (salivaCooldown > 0) salivaCooldown--;
        if (meteorCooldown > 0) meteorCooldown--;
        if (lightCooldown > 0) lightCooldown--;
        if (fireBreathCooldown > 0) fireBreathCooldown--;
    }

    private void switchFlightMode() {
        LivingEntity target = getTarget();
        if (target == null) {
            if (tickCount % 200 == 0) setFlying(false);
            return;
        }
        double distance = distanceToSqr(target);
        if (distance > 144.0D || target.getY() > getY() + 3.0D) {
            setFlying(true);
        } else if (distance < 36.0D && onGround()) {
            setFlying(false);
        }
    }

    private void shootSalivaBall(LivingEntity target) {
        triggerAnim("attack_controller", "attack");
        fireProjectile(ParasiteProjectileEntity.Mode.BOMB, projectileMuzzle(), target.getEyePosition(),
                1.20D, 48.0F, 3.5D, 80, null);
        playSound(ModSounds.DRACONITE_FIRE_SHOOT.get(), 2.0F, 1.0F);
    }

    private void beginFireBreath(LivingEntity target) {
        triggerAnim("attack_controller", "attack");
        fireBreathTicks = FIRE_BREATH_DURATION_TICKS;
        fireBreathTarget = targetBlock(target);
        entityData.set(FIRE_BREATH_TICKS, fireBreathTicks);
        entityData.set(FIRE_BREATH_TARGET, fireBreathTarget);
        getNavigation().stop();
        playSound(ModSounds.DRACONITE_FIRE_SHOOT.get(), 2.0F, 0.85F);
    }

    private boolean tickFireBreath() {
        if (fireBreathTicks <= 0) {
            return false;
        }
        Vec3 destination = Vec3.atCenterOf(fireBreathTarget);
        getLookControl().setLookAt(destination.x, destination.y, destination.z, 30.0F, 30.0F);
        getNavigation().stop();
        if (fireBreathTicks % FIRE_BREATH_SHOT_INTERVAL_TICKS == 0) {
            fireProjectile(ParasiteProjectileEntity.Mode.BOMB, projectileMuzzle(), destination,
                    1.10D, 42.0F, 3.25D, 80, null);
        }
        fireBreathTicks--;
        entityData.set(FIRE_BREATH_TICKS, fireBreathTicks);
        if (fireBreathTicks == 0) {
            fireBreathTarget = BlockPos.ZERO;
            entityData.set(FIRE_BREATH_TARGET, BlockPos.ZERO);
            fireBreathCooldown = FIRE_BREATH_COOLDOWN_TICKS;
        }
        return true;
    }

    private void beginMeteorRain(LivingEntity target) {
        triggerAnim("attack_controller", "attack");
        meteorRainTicks = METEOR_TELEGRAPH_TICKS;
        meteorTarget = targetBlock(target);
        entityData.set(METEOR_TICKS, meteorRainTicks);
        entityData.set(METEOR_TARGET, meteorTarget);
        meteorCooldown = METEOR_COOLDOWN_TICKS;
        playSound(ModSounds.DRACONITE_FIRE_SHOOT.get(), 2.0F, 0.8F);
    }

    private boolean tickMeteorRain() {
        if (meteorRainTicks <= 0) {
            return false;
        }
        if (meteorRainTicks <= METEOR_SALVO_TICKS) {
            spawnMeteor();
        }
        meteorRainTicks--;
        entityData.set(METEOR_TICKS, meteorRainTicks);
        if (meteorRainTicks == 0) {
            meteorTarget = BlockPos.ZERO;
            entityData.set(METEOR_TARGET, BlockPos.ZERO);
        }
        return true;
    }

    private void spawnMeteor() {
        Vec3 destination = Vec3.atCenterOf(meteorTarget).add(random.nextGaussian() * 3.5D, 0.5D,
                random.nextGaussian() * 3.5D);
        Vec3 start = destination.add(random.nextGaussian() * 2.0D, 20.0D + random.nextDouble() * 4.0D,
                random.nextGaussian() * 2.0D);
        fireProjectile(ParasiteProjectileEntity.Mode.METEOR, start, destination, 1.20D,
                42.0F, 4.0D, 60, null);
    }

    private void performLightBarrage(LivingEntity target) {
        triggerAnim("attack_controller", "attack");
        Vec3 start = projectileMuzzle();
        for (int i = 0; i < LIGHT_BARRAGE_COUNT; i++) {
            Vec3 destination = target.getEyePosition().add(random.nextGaussian() * 1.5D,
                    random.nextGaussian() * 0.75D, random.nextGaussian() * 1.5D);
            fireProjectile(ParasiteProjectileEntity.Mode.LIGHT, start, destination, 1.35D,
                    24.0F, 1.5D, 95, target);
        }
        playSound(ModSounds.DRACONITE_FIRE_SHOOT.get(), 2.0F, 1.2F);
    }

    private void fireProjectile(ParasiteProjectileEntity.Mode mode, Vec3 start, Vec3 destination,
                                double speed, float damage, double radius, int lifetime,
                                LivingEntity homingTarget) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        projectile.configure(this, mode, start, destination, speed, damage, radius, lifetime, homingTarget);
        level().addFreshEntity(projectile);
    }

    private Vec3 projectileMuzzle() {
        return getEyePosition().add(getLookAngle().scale(Math.max(1.6D, getBbWidth() * 0.7D)));
    }

    private static BlockPos targetBlock(LivingEntity target) {
        return BlockPos.containing(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
    }

    private void spawnFireBreathParticles() {
        if (entityData.get(FIRE_BREATH_TICKS) <= 0) {
            return;
        }
        Vec3 source = projectileMuzzle();
        Vec3 destination = Vec3.atCenterOf(entityData.get(FIRE_BREATH_TARGET));
        Vec3 direction = destination.subtract(source);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        direction = direction.normalize();
        for (int index = 0; index < 8; index++) {
            Vec3 point = source.add(direction.scale(index * 0.65D));
            level().addParticle(ParticleTypes.FLAME, point.x, point.y, point.z,
                    direction.x * 0.08D, direction.y * 0.08D, direction.z * 0.08D);
            if (index % 2 == 0) {
                level().addParticle(ParticleTypes.SMOKE, point.x, point.y, point.z,
                        direction.x * 0.02D, 0.04D, direction.z * 0.02D);
            }
        }
    }

    private void spawnMeteorWarningParticles() {
        if (entityData.get(METEOR_TICKS) <= METEOR_SALVO_TICKS) {
            return;
        }
        Vec3 source = projectileMuzzle();
        for (int index = 0; index < 10; index++) {
            level().addParticle(ParticleTypes.FLAME, source.x, source.y, source.z,
                    (random.nextDouble() - 0.5D) * 0.08D, 0.45D + random.nextDouble() * 0.20D,
                    (random.nextDouble() - 0.5D) * 0.08D);
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hurt = super.doHurtTarget(entity);
        if (hurt) {
            triggerAnim("attack_controller", "attack");
        }
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
        tag.putInt("saliva_cooldown", salivaCooldown);
        tag.putInt("meteor_cooldown", meteorCooldown);
        tag.putInt("light_cooldown", lightCooldown);
        tag.putInt("fire_breath_cooldown", fireBreathCooldown);
        tag.putInt("fire_breath_ticks", fireBreathTicks);
        tag.putInt("meteor_rain_ticks", meteorRainTicks);
        tag.putLong("fire_breath_target", fireBreathTarget.asLong());
        tag.putLong("meteor_target", meteorTarget.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFlying(tag.getBoolean("flying"));
        salivaCooldown = tag.contains("saliva_cooldown") ? tag.getInt("saliva_cooldown")
                : tag.getInt("toxic_cloud_cooldown");
        meteorCooldown = tag.getInt("meteor_cooldown");
        lightCooldown = tag.getInt("light_cooldown");
        fireBreathCooldown = tag.getInt("fire_breath_cooldown");
        fireBreathTicks = tag.getInt("fire_breath_ticks");
        meteorRainTicks = tag.getInt("meteor_rain_ticks");
        fireBreathTarget = tag.contains("fire_breath_target")
                ? BlockPos.of(tag.getLong("fire_breath_target")) : BlockPos.ZERO;
        meteorTarget = tag.contains("meteor_target") ? BlockPos.of(tag.getLong("meteor_target")) : BlockPos.ZERO;
        entityData.set(FIRE_BREATH_TICKS, fireBreathTicks);
        entityData.set(FIRE_BREATH_TARGET, fireBreathTarget);
        entityData.set(METEOR_TICKS, meteorRainTicks);
        entityData.set(METEOR_TARGET, meteorTarget);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (entityData.get(FIRE_BREATH_TICKS) > 0) {
                if (isShadowClone()) {
                    return state.setAndContinue(isShadowHitFlashing()
                            ? CLONE_FIRE_BREATH_SHAKING : CLONE_FIRE_BREATH);
                }
                return state.setAndContinue(isShadowHitFlashing() ? FIRE_BREATH_SHAKING : FIRE_BREATH);
            }
            if (isShadowClone()) {
                if (isShadowHitFlashing()) {
                    return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? CLONE_SHAKING_WALK : CLONE_SHAKING_IDLE);
                }
                return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? CLONE_WALK : CLONE_IDLE);
            }
            if (isShadowHitFlashing()) {
                return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? SHAKING_WALK : SHAKING_IDLE);
            }
            if (isFlying()) return state.setAndContinue(FLY);
            return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP).triggerableAnim("attack", ATTACK));
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
                getMoveControl().setWantedPosition(target.getX(), target.getY() + 4.0D, target.getZ(), 1.0D);
            } else if (fireBreathTicks <= 0 && meteorRainTicks <= 0) {
                getNavigation().moveTo(target, 1.1D);
            }
            if (attackCooldown > 0) attackCooldown--;
            if (distanceToSqr(target) < 16.0D && attackCooldown <= 0) {
                doHurtTarget(target);
                attackCooldown = 20;
            }
        }
    }
}
