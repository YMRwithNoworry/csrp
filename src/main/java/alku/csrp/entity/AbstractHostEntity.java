package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.EnumSet;

abstract class AbstractHostEntity extends CrudeParasiteEntity {
    private static final EntityDataAccessor<Boolean> BURROWED =
            SynchedEntityData.defineId(AbstractHostEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BURROW_ANIMATION_TICKS =
            SynchedEntityData.defineId(AbstractHostEntity.class, EntityDataSerializers.INT);
    protected static final int BURROW_TRANSITION_TICKS = 10;

    private final double baseMovementSpeed;
    private final double attackRadius;
    private final double shockwaveRadius;
    private final int burrowDuration;
    private final int rangedInterval;
    private final int shockwaveChance;
    private int burrowCooldown;
    private int rangedCooldown = 20;

    protected AbstractHostEntity(EntityType<? extends AbstractHostEntity> type, Level level,
                                 double baseMovementSpeed, double attackRadius, double shockwaveRadius,
                                 int burrowDuration, int rangedInterval, int shockwaveChance) {
        super(type, level);
        this.baseMovementSpeed = baseMovementSpeed;
        this.attackRadius = attackRadius;
        this.shockwaveRadius = shockwaveRadius;
        this.burrowDuration = burrowDuration;
        this.rangedInterval = rangedInterval;
        this.shockwaveChance = shockwaveChance;
    }

    protected static AttributeSupplier.Builder createHostAttributes(double health, double armor, double damage,
                                                                    double movementSpeed, double followRange) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, movementSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, followRange);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROWED, false);
        builder.define(BURROW_ANIMATION_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new HostShockwaveGoal());
        goalSelector.addGoal(2, new HostRangedGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (burrowCooldown > 0) {
            burrowCooldown--;
        }
        if (rangedCooldown > 0) {
            rangedCooldown--;
        }
        if (entityData.get(BURROW_ANIMATION_TICKS) > 0) {
            entityData.set(BURROW_ANIMATION_TICKS, entityData.get(BURROW_ANIMATION_TICKS) - 1);
        }
        updateBurrowState();
        if (!isBurrowed()) {
            summonMinions();
        }
    }

    private void updateBurrowState() {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && distanceToSqr(target) <= 9.0) {
            setBurrowed(true);
            burrowCooldown = burrowDuration;
            getNavigation().stop();
        } else if (burrowCooldown <= 0) {
            setBurrowed(false);
        }

        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(isBurrowed() ? 0.0 : baseMovementSpeed);
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        boolean hit = false;
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(attackRadius));
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(attackRadius), this::isValidParasiteTarget)) {
            if (hasLineOfSight(target) && target.hurt(damageSources().mobAttack(this), damage)) {
                hit = true;
            }
        }
        if (hit) {
            triggerAttackAnimation();
        }
        return hit;
    }

    protected void performShockwave() {
        LivingEntity target = getTarget();
        WaveEntity wave = ModEntities.WAVE.get().create(level());
        if (target == null || wave == null) {
            return;
        }
        double angle = getYRot() * Mth.DEG_TO_RAD;
        double distance = 3.0D * Mth.cos(Mth.PI / 18.0F);
        wave.moveTo(getX() - Mth.sin((float) angle) * distance, getY(),
                getZ() + Mth.cos((float) angle) * distance, getYRot(), 0.0F);
        wave.configure(getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.3D,
                Config.primitiveMinimumDamage(), 1, 60, target);
        level().addFreshEntity(wave);
        triggerAttackAnimation();
        playSound(ModSounds.MOB_SWIPE.get(), 2.0F, 1.0F);
    }

    protected void spawnProjectile(ParasiteProjectileEntity.Mode mode, LivingEntity target, double speed,
                                   float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(), mode);
        if (projectile == null) {
            return;
        }
        Vec3 start = position().add(0.0, getBbHeight() * 0.65, 0.0);
        Vec3 destination = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        projectile.configure(this, mode, start, destination, speed, damage, radius, lifetime);
        level().addFreshEntity(projectile);
        triggerAttackAnimation();
        rangedCooldown = rangedInterval;
    }

    protected void spawnBomb(LivingEntity target, int fuse, float damage, int rangeRadius) {
        BombEntity bomb = ModEntities.BOMB.get().create(level());
        if (bomb == null) {
            return;
        }
        bomb.configure(this, fuse, 0.0F, damage, rangeRadius, 1, false);
        double targetY = target.getY() + target.getEyeHeight() - 1.1D;
        double x = target.getX() + target.getDeltaMovement().x - getX();
        double y = targetY - getY();
        double z = target.getZ() + target.getDeltaMovement().z - getZ();
        double horizontal = Math.sqrt(x * x + z * z);
        bomb.shoot(new Vec3(x, y + horizontal * 0.2D, z), 0.75F, 8.0F);
        level().addFreshEntity(bomb);
        triggerAttackAnimation();
        rangedCooldown = rangedInterval;
    }

    protected <T extends Mob> void spawnMinions(DeferredHolder<EntityType<?>, EntityType<T>> type,
                                                Class<T> entityClass, int cap) {
        if (!(level() instanceof ServerLevel serverLevel) || random.nextInt(getTarget() == null ? 400 : 150) != 0) {
            return;
        }
        if (serverLevel.getEntitiesOfClass(entityClass, getBoundingBox().inflate(16.0)).size() >= cap) {
            return;
        }
        T minion = type.get().create(serverLevel);
        if (minion == null) {
            return;
        }
        minion.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        minion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        minion.setTarget(getTarget());
        serverLevel.addFreshEntity(minion);
    }

    public boolean isBurrowed() {
        return entityData.get(BURROWED);
    }

    public void setBurrowed(boolean burrowed) {
        if (entityData.get(BURROWED) == burrowed) {
            return;
        }
        entityData.set(BURROWED, burrowed);
        entityData.set(BURROW_ANIMATION_TICKS, BURROW_TRANSITION_TICKS);
    }

    public int getBurrowAnimationTicks() {
        return entityData.get(BURROW_ANIMATION_TICKS);
    }

    protected abstract void triggerAttackAnimation();

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.HOST_LIVING.get();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return ModSounds.HOST_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HOST_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.RUPTER_STEP.get(), 0.15F, getVoicePitch());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("burrowed", isBurrowed());
        tag.putInt("burrow_cooldown", burrowCooldown);
        tag.putInt("ranged_cooldown", rangedCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBurrowed(tag.getBoolean("burrowed"));
        burrowCooldown = tag.getInt("burrow_cooldown");
        rangedCooldown = tag.getInt("ranged_cooldown");
    }

    protected abstract void performRangedAttack(LivingEntity target);

    protected abstract void summonMinions();

    private final class HostRangedGoal extends Goal {
        private HostRangedGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && isBurrowed() && rangedCooldown <= 0
                    && hasLineOfSight(target);
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                performRangedAttack(target);
            }
        }
    }

    private final class HostShockwaveGoal extends Goal {
        private int chargeTicks;

        private HostShockwaveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && onGround() && hasLineOfSight(target)
                    && distanceToSqr(target) < 256.0 && random.nextInt(shockwaveChance) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return chargeTicks <= 60 && target != null && target.isAlive();
        }

        @Override
        public void start() {
            chargeTicks = 0;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            chargeTicks++;
            getNavigation().stop();
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            if (chargeTicks == 20) {
                performShockwave();
            }
        }
    }
}
