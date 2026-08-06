package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Legacy EntityEsor port. The Marauder keeps two independently damageable
 * tendril hitboxes, climbs walls, summons stationary support tendrils, and
 * periodically performs the old charged smash sequence.
 */
public final class MarauderEntity extends PrimitiveParasiteEntity {
    @Override
    protected int maxDamageAdaptationHits() {
        return 8;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return 0.125F;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return 12;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return 0.95F;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return 0.30F;
    }
    private static final float TENDRIL_HEALTH_FRACTION = 0.50F;
    private static final int SWEEP_COOLDOWN_TICKS = 20;
    private static final int SMASH_CHARGE_TICKS = 20;
    private static final int SMASH_DURATION_TICKS = 100;
    private static final int SMASH_COOLDOWN_TICKS = 200;
    private static final int SUPPORT_SUMMON_INTERVAL_TICKS = 60;
    private static final int MAX_SUPPORT_TENDRILS = 3;
    private static final EntityDataAccessor<Byte> CLIMBING = SynchedEntityData.defineId(
            MarauderEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> LEFT_TENDRIL_HEALTH = SynchedEntityData.defineId(
            MarauderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RIGHT_TENDRIL_HEALTH = SynchedEntityData.defineId(
            MarauderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ATTACK_TICKS = SynchedEntityData.defineId(
            MarauderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SMASH_TICKS = SynchedEntityData.defineId(
            MarauderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HARDENED_VARIANT = SynchedEntityData.defineId(
            MarauderEntity.class, EntityDataSerializers.BOOLEAN);
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation SMASH_CHARGE =
            ParasiteAnimations.loop(this, "idle.get_parasite_status_25");
    private final RawAnimation SMASH =
            ParasiteAnimations.loop(this, "idle.get_parasite_status_3");
    private final RawAnimation SWIPE = ParasiteAnimations.play(this, "attack");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this, "idle.get_parasite_status_10");

    @Nullable
    private UUID leftTendrilId;
    @Nullable
    private UUID rightTendrilId;
    private int smashTicks;
    private int smashCooldown = 80;
    private boolean variantInitialized;

    public MarauderEntity(EntityType<? extends MarauderEntity> type, Level level) {
        super(type, level);
        xpReward = 120;
        float tendrilHealth = maxTendrilHealth();
        entityData.set(LEFT_TENDRIL_HEALTH, tendrilHealth);
        entityData.set(RIGHT_TENDRIL_HEALTH, tendrilHealth);
        if (!level.isClientSide) {
            initializeVariant();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.255D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, createAnimatedLeapGoal(1.2F, 40));
        goalSelector.addGoal(1, new MarauderSmashGoal());
        goalSelector.addGoal(2, new MarauderMeleeGoal());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, (byte) 0);
        builder.define(LEFT_TENDRIL_HEALTH, 0.0F);
        builder.define(RIGHT_TENDRIL_HEALTH, 0.0F);
        builder.define(ATTACK_TICKS, 0);
        builder.define(SMASH_TICKS, 0);
        builder.define(HARDENED_VARIANT, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnSmashParticles();
            return;
        }

        setClimbing(horizontalCollision);
        if (entityData.get(ATTACK_TICKS) > 0) {
            entityData.set(ATTACK_TICKS, entityData.get(ATTACK_TICKS) - 1);
        }
        if (smashCooldown > 0) {
            smashCooldown--;
        }
        ensureAttachedTendrils();
        if (tickCount % SUPPORT_SUMMON_INTERVAL_TICKS == 20) {
            trySummonSupportTendril();
        }
    }

    private void initializeVariant() {
        if (variantInitialized) {
            return;
        }
        variantInitialized = true;
        entityData.set(HARDENED_VARIANT, random.nextFloat() < 0.05F);
    }

    private float maxTendrilHealth() {
        return (float) getAttributeValue(Attributes.MAX_HEALTH) * TENDRIL_HEALTH_FRACTION;
    }

    private void ensureAttachedTendrils() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (isLeftTendrilAttached() && resolveTendril(leftTendrilId) == null) {
            leftTendrilId = createAttachedTendril(serverLevel, TendrilSide.LEFT).getUUID();
        }
        if (isRightTendrilAttached() && resolveTendril(rightTendrilId) == null) {
            rightTendrilId = createAttachedTendril(serverLevel, TendrilSide.RIGHT).getUUID();
        }
    }

    private MarauderTendrilEntity createAttachedTendril(ServerLevel level, TendrilSide side) {
        MarauderTendrilEntity tendril = ModEntities.MARAUDER_TENDRIL.get().create(level);
        if (tendril == null) {
            throw new IllegalStateException("Marauder tendril entity could not be created");
        }
        tendril.attach(this, side);
        level.addFreshEntity(tendril);
        return tendril;
    }

    @Nullable
    private MarauderTendrilEntity resolveTendril(@Nullable UUID id) {
        if (id == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(id);
        return entity instanceof MarauderTendrilEntity tendril && tendril.isAttachedTo(this) ? tendril : null;
    }

    public boolean isClimbing() {
        return (entityData.get(CLIMBING) & 1) != 0;
    }

    private void setClimbing(boolean climbing) {
        byte state = entityData.get(CLIMBING);
        entityData.set(CLIMBING, climbing ? (byte) (state | 1) : (byte) (state & -2));
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hurt = super.doHurtTarget(entity);
        if (hurt) {
            entity.push(0.0D, 0.5D, 0.0D);
        }
        return hurt;
    }

    private boolean performSweepAttack(LivingEntity center) {
        if (level().isClientSide) {
            return false;
        }
        entityData.set(ATTACK_TICKS, 10);
        triggerAnim("attack_controller", "swipe");
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.75F + random.nextFloat() * 0.25F);
        boolean hit = false;
        AABB area = center.getBoundingBox().inflate(2.0D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), area);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area, this::isValidParasiteTarget)) {
            hit |= doHurtTarget(target);
        }
        return hit;
    }

    private void performSmashStrike() {
        AABB area = getBoundingBox().inflate(6.0D, 3.0D, 6.0D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), area);
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area, this::isValidParasiteTarget)) {
            target.hurt(damageSources().mobAttack(this), damage);
            Vec3 direction = target.position().subtract(position());
            double length = Math.max(0.001D, direction.horizontalDistance());
            target.push(direction.x / length * 1.2D, 0.8D, direction.z / length * 1.2D);
        }
    }

    private void finishSmash(boolean enrageAllies) {
        if (enrageAllies) {
            AABB area = getBoundingBox().inflate(24.0D, 5.0D, 24.0D);
            for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != this && entity instanceof Parasite)) {
                ally.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 1, false, false), this);
            }
            smashCooldown = SMASH_COOLDOWN_TICKS;
        }
        smashTicks = 0;
        entityData.set(SMASH_TICKS, 0);
    }

    private void trySummonSupportTendril() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || countSupportTendrils() >= MAX_SUPPORT_TENDRILS) {
            return;
        }

        boolean obscured = !hasLineOfSight(target);
        boolean spawnTeleport = obscured && distanceToSqr(target) > 64.0D && random.nextInt(3) == 0;
        if (!spawnTeleport && random.nextInt(10) != 0) {
            return;
        }

        BlockPos position = findSupportPosition(target, spawnTeleport ? 5 : 3, spawnTeleport ? 3 : 1);
        if (position == null || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        MarauderTendrilEntity tendril = ModEntities.MARAUDER_TENDRIL.get().create(serverLevel);
        if (tendril == null) {
            return;
        }
        tendril.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, getYRot(), 0.0F);
        tendril.startSupport(this, target, spawnTeleport
                ? MarauderTendrilEntity.Mode.TELEPORT
                : MarauderTendrilEntity.Mode.SNARE);
        serverLevel.addFreshEntity(tendril);
    }

    private int countSupportTendrils() {
        return level().getEntitiesOfClass(MarauderTendrilEntity.class, getBoundingBox().inflate(32.0D),
                        tendril -> tendril.isSupportFor(this))
                .size();
    }

    @Nullable
    private BlockPos findSupportPosition(LivingEntity target, int range, int minimumDistance) {
        for (int attempt = 0; attempt < 5; attempt++) {
            int x = target.getBlockX() + random.nextInt(range * 2 + 1) - range;
            int z = target.getBlockZ() + random.nextInt(range * 2 + 1) - range;
            if (Math.abs(x - target.getBlockX()) < minimumDistance && Math.abs(z - target.getBlockZ()) < minimumDistance) {
                continue;
            }
            BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(x, target.getBlockY() + 5, z);
            for (int scan = 0; scan < 12; scan++) {
                BlockState at = level().getBlockState(position);
                BlockState below = level().getBlockState(position.below());
                if (at.isAir() && level().getBlockState(position.above()).isAir() && below.isSolid()) {
                    return position.immutable();
                }
                position.move(0, -1, 0);
            }
        }
        return null;
    }

    boolean hurtTendril(MarauderTendrilEntity tendril, DamageSource source, float amount) {
        if (level().isClientSide || !hurt(source, amount)) {
            return false;
        }
        TendrilSide side = tendril.getAttachedSide();
        float remaining = getTendrilHealth(side) - amount;
        setTendrilHealth(side, remaining);
        if (remaining <= 0.0F) {
            breakTendril(tendril, side);
        }
        return true;
    }

    private void breakTendril(MarauderTendrilEntity tendril, TendrilSide side) {
        setTendrilHealth(side, 0.0F);
        if (side == TendrilSide.LEFT) {
            leftTendrilId = null;
        } else {
            rightTendrilId = null;
        }
        tendril.detach();
        playSound(ModSounds.MARAUDER_HURT.get(), 2.0F, 1.25F);
    }

    private float getTendrilHealth(TendrilSide side) {
        return entityData.get(side == TendrilSide.LEFT ? LEFT_TENDRIL_HEALTH : RIGHT_TENDRIL_HEALTH);
    }

    private void setTendrilHealth(TendrilSide side, float health) {
        entityData.set(side == TendrilSide.LEFT ? LEFT_TENDRIL_HEALTH : RIGHT_TENDRIL_HEALTH,
                Math.max(0.0F, health));
    }

    public boolean isLeftTendrilAttached() {
        return entityData.get(LEFT_TENDRIL_HEALTH) > 0.0F;
    }

    public boolean isRightTendrilAttached() {
        return entityData.get(RIGHT_TENDRIL_HEALTH) > 0.0F;
    }

    public boolean isTendrilAttached(TendrilSide side) {
        return side == TendrilSide.LEFT ? isLeftTendrilAttached() : isRightTendrilAttached();
    }

    public boolean isHardenedVariant() {
        return entityData.get(HARDENED_VARIANT);
    }

    public int getAttackTicks() {
        return entityData.get(ATTACK_TICKS);
    }

    public int getSmashTicks() {
        return entityData.get(SMASH_TICKS);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.MARAUDER_LIVING.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MARAUDER_HURT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return ModSounds.MARAUDER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 0.15F, 1.0F);
    }

    private void spawnSmashParticles() {
        int currentSmashTicks = entityData.get(SMASH_TICKS);
        if (currentSmashTicks <= 0) {
            return;
        }
        int particles = currentSmashTicks < SMASH_CHARGE_TICKS ? 2 : 6;
        for (int index = 0; index < particles; index++) {
            level().addParticle(currentSmashTicks < SMASH_CHARGE_TICKS ? ParticleTypes.FLAME : ParticleTypes.CRIT,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.5D,
                    getY() + random.nextDouble() * getBbHeight(),
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.5D,
                    0.0D, 0.03D, 0.0D);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("marauder_left_tendril", entityData.get(LEFT_TENDRIL_HEALTH));
        tag.putFloat("marauder_right_tendril", entityData.get(RIGHT_TENDRIL_HEALTH));
        tag.putInt("marauder_smash_cooldown", smashCooldown);
        tag.putBoolean("marauder_hardened", isHardenedVariant());
        if (leftTendrilId != null) {
            tag.putUUID("marauder_left_tendril_id", leftTendrilId);
        }
        if (rightTendrilId != null) {
            tag.putUUID("marauder_right_tendril_id", rightTendrilId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(LEFT_TENDRIL_HEALTH, tag.contains("marauder_left_tendril")
                ? tag.getFloat("marauder_left_tendril") : maxTendrilHealth());
        entityData.set(RIGHT_TENDRIL_HEALTH, tag.contains("marauder_right_tendril")
                ? tag.getFloat("marauder_right_tendril") : maxTendrilHealth());
        smashCooldown = tag.getInt("marauder_smash_cooldown");
        entityData.set(HARDENED_VARIANT, tag.getBoolean("marauder_hardened"));
        leftTendrilId = tag.hasUUID("marauder_left_tendril_id") ? tag.getUUID("marauder_left_tendril_id") : null;
        rightTendrilId = tag.hasUUID("marauder_right_tendril_id") ? tag.getUUID("marauder_right_tendril_id") : null;
        variantInitialized = true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("swipe", SWIPE));
    }

    private PlayState movementAnimation(AnimationState<MarauderEntity> state) {
        if (isSpecialLeapAnimating()) {
            return state.setAndContinue(LEAP);
        }
        if (getSmashTicks() > 0) {
            return state.setAndContinue(getSmashTicks() < SMASH_CHARGE_TICKS ? SMASH_CHARGE : SMASH);
        }
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.0001 ? WALK : IDLE);
    }

    public enum TendrilSide {
        LEFT(1),
        RIGHT(-1);

        private final int offsetSign;

        TendrilSide(int offsetSign) {
            this.offsetSign = offsetSign;
        }

        public int offsetSign() {
            return offsetSign;
        }
    }

    private final class MarauderMeleeGoal extends Goal {
        private int cooldown;

        private MarauderMeleeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && getSmashTicks() <= 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            getNavigation().moveTo(target, 1.3D);
            if (cooldown > 0) {
                cooldown--;
            }
            if (cooldown <= 0 && distanceToSqr(target) <= 16.0D) {
                performSweepAttack(target);
                cooldown = SWEEP_COOLDOWN_TICKS;
            }
        }
    }

    private final class MarauderSmashGoal extends Goal {
        private MarauderSmashGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return smashCooldown <= 0 && target != null && target.isAlive() && onGround()
                    && random.nextInt(reducedTickDelay(100)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return smashTicks > 0 && smashTicks <= SMASH_DURATION_TICKS && target != null && target.isAlive();
        }

        @Override
        public void start() {
            smashTicks = 1;
            entityData.set(SMASH_TICKS, smashTicks);
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (!onGround()) {
                finishSmash(false);
                return;
            }
            getNavigation().stop();
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            smashTicks++;
            entityData.set(SMASH_TICKS, smashTicks);
            if (smashTicks <= SMASH_CHARGE_TICKS) {
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 110, 100, false, false));
                return;
            }

            performSmashStrike();
            if (smashTicks % 7 == 0) {
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 5.0F, 0.65F + random.nextFloat() * 0.2F);
            }
            if (smashTicks > SMASH_DURATION_TICKS) {
                finishSmash(true);
            }
        }

        @Override
        public void stop() {
            if (smashTicks > 0) {
                finishSmash(false);
            }
        }
    }
}
