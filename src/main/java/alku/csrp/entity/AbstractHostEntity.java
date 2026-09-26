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
    /** Legacy buriedC refill value applied while a target stays within 9 blocks (80 / 120). */
    private final int burrowRefill;
    private final int rangedInterval;
    private final int shockwaveChance;
    private int rangedCooldown = 20;

    /**
     * Legacy buriedC: a tick budget that keeps the host submerged. It is decremented once per tick
     * and refilled to {@link #burrowRefill} whenever a target closes in; a landing melee hit adds
     * a further 160 so a host that keeps connecting stays underground.
     */
    private int buriedC;
    private static final int BURROW_HIT_EXTENSION_TICKS = 160;

    // Legacy checkBurrowed() collision box growth: 0.25F <-> 3.5F at 0.05F per tick.
    private static final float MIN_BB_HEIGHT = 0.25F;
    private static final float FULL_BB_HEIGHT = 3.5F;
    private static final float BURROW_BB_STEP = 0.05F;
    private float burrowHitboxHeight = FULL_BB_HEIGHT;

    protected AbstractHostEntity(EntityType<? extends AbstractHostEntity> type, Level level,
                                 double baseMovementSpeed, double attackRadius, double shockwaveRadius,
                                 int burrowRefill, int rangedInterval, int shockwaveChance) {
        super(type, level);
        this.baseMovementSpeed = baseMovementSpeed;
        this.attackRadius = attackRadius;
        this.shockwaveRadius = shockwaveRadius;
        this.burrowRefill = burrowRefill;
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
        if (rangedCooldown > 0) {
            rangedCooldown--;
        }
        if (entityData.get(BURROW_ANIMATION_TICKS) > 0) {
            entityData.set(BURROW_ANIMATION_TICKS, entityData.get(BURROW_ANIMATION_TICKS) - 1);
        }
        updateBurrowState();
        updateBurrowHitbox();
        if (!isBurrowed()) {
            summonMinions();
        }
    }

    /**
     * Legacy checkBurrowed(): while submerged the hitbox grows by 0.05 per tick up to 3.5 and
     * shrinks back down to 0.25 once the host resurfaces, which is what gates attacks (height 1.0).
     */
    private void updateBurrowHitbox() {
        float height = getBbHeight();
        float full = fullBurrowHeight();
        float step = burrowHeightStep();
        float min = minBurrowHeight();
        float next;
        if (isBurrowed()) {
            next = Math.min(full, height + step);
        } else {
            next = Math.max(min, height - step);
        }
        if (Math.abs(next - height) < 1.0E-4F) {
            return;
        }
        burrowHitboxHeight = next;
        refreshDimensions();
    }

    /** Legacy EntityHost grows to 3.5F; EntityHostII to 7.5F. */
    protected float fullBurrowHeight() {
        return FULL_BB_HEIGHT;
    }

    /** Legacy EntityHost grows at 0.05F/tick; EntityHostII at 0.09F/tick. */
    protected float burrowHeightStep() {
        return BURROW_BB_STEP;
    }

    protected float minBurrowHeight() {
        return MIN_BB_HEIGHT;
    }

    @Override
    protected net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        net.minecraft.world.entity.EntityDimensions base = super.getDefaultDimensions(pose);
        return net.minecraft.world.entity.EntityDimensions.scalable(base.width(), burrowHitboxHeight);
    }

    private void updateBurrowState() {
        // Legacy func_70636_d: buriedC is drained first, then checkSpeed() re-submerges the host
        // whenever a target is within 9 blocks, refilling the budget to 80.
        if (buriedC > 0) {
            buriedC--;
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && distanceToSqr(target) <= 9.0) {
            setBurrowed(true);
            buriedC = burrowRefill;
            getNavigation().stop();
        } else if (buriedC <= 0) {
            setBurrowed(false);
        }

        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(isBurrowed() ? 0.0 : baseMovementSpeed);
        }
    }

    /**
     * Legacy {@code EntityHost.func_70652_k}: a melee swing only connects once the host has
     * finished emerging (burrowed and taller than 1.0). While still submerged the swing is
     * converted into a dive instead of a hit, refilling buriedC to 160.
     *
     * @return true when the hit landed
     */
    protected boolean performAoeMelee(Entity entityIn) {
        if (isBurrowed() && getBbHeight() >= 1.0F) {
            boolean hit = false;
            float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(attackRadius));
            for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(attackRadius), this::isValidParasiteTarget)) {
                if (victim != this && hasLineOfSight(victim) && hitMeleeTarget(victim, damage)) {
                    hit = true;
                    buriedC += BURROW_HIT_EXTENSION_TICKS;
                }
            }
            if (hit) {
                // Legacy attackEntityAsMobAOE plays the swipe cue three times per connecting sweep.
                playSound(ModSounds.MOB_SWIPE.get(), 1.0F, 1.0F);
                playSound(ModSounds.MOB_SWIPE.get(), 1.0F, 1.25F);
                playSound(ModSounds.MOB_SWIPE.get(), 2.0F, 1.0F);
            }
            return hit;
        }

        if (entityIn == null || distanceToSqr(entityIn) > 4.0) {
            return false;
        }
        // Not fully surfaced: dive instead of striking.
        setBurrowed(true);
        buriedC = BURROW_HIT_EXTENSION_TICKS;
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.0);
        }
        getNavigation().stop();
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        return performAoeMelee(entity);
    }

    /**
     * Legacy {@code attackEntityAsMob}: the tier minimum damage is applied through armor first,
     * then the ordinary melee damage goes through the regular damage pipeline.
     */
    private boolean hitMeleeTarget(LivingEntity victim, float damage) {
        applyPrimitiveMinimumDamage(victim);
        return victim.hurt(damageSources().mobAttack(this), damage);
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

    /** Legacy shockwave() branch border == 0: the host screams its hurt cue while winding up. */
    protected void playShockwaveWindupSound() {
        float pitch = (random.nextFloat() - random.nextFloat()) * 0.4F + 2.0F;
        playSound(ModSounds.HOST_HURT.get(), 4.0F, pitch);
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
                                                Class<T> entityClass, int targetingCap, int idleCap) {
        // Legacy spawnRupters(): 1/150 per tick while a target is held (cap 4 in a 16 block box),
        // otherwise 1/400 per tick (cap 3) and the summon is left without a target.
        LivingEntity target = getTarget();
        boolean hasTarget = target != null && target.isAlive();
        if (!(level() instanceof ServerLevel serverLevel) || random.nextInt(hasTarget ? 150 : 400) != 0) {
            return;
        }
        if (serverLevel.getEntitiesOfClass(entityClass, getBoundingBox().inflate(16.0)).size() >= (hasTarget ? targetingCap : idleCap)) {
            return;
        }
        T minion = type.get().create(serverLevel);
        if (minion == null) {
            return;
        }
        minion.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        minion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        if (hasTarget) {
            minion.setTarget(target);
        }
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
        tag.putInt("buried_c", buriedC);
        tag.putInt("ranged_cooldown", rangedCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBurrowed(tag.getBoolean("burrowed"));
        buriedC = tag.getInt("buried_c");
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
            // Legacy func_82196_d: bombs only fly while burrowed, fully emerged, and within
            // 13 blocks (distanceToSqr <= 169).
            return target != null && target.isAlive() && isBurrowed() && getBbHeight() >= 1.0F
                    && rangedCooldown <= 0 && distanceToSqr(target) <= 169.0
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
            playShockwaveWindupSound();
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
