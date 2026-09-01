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
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelAnimationState;
import alku.csrp.animation.CitadelPlayState;
import alku.csrp.animation.CitadelRawAnimation;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Legacy EntityEsor port with damageable tendrils, wall climbing, support
 * tendrils, charged smash behavior, and the original model-function states.
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
    private static final int ATTACK_ANIMATION_TICKS = 18;
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
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            MarauderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STILL_ANI = SynchedEntityData.defineId(
            MarauderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HARDENED_VARIANT = SynchedEntityData.defineId(
            MarauderEntity.class, EntityDataSerializers.BOOLEAN);
    private final CitadelRawAnimation ageInTicksAnimation = animation("func_78087_a.age_in_ticks");
    private final CitadelRawAnimation limbSwingAnimation = animation("func_78087_a.limb_swing");
    private final CitadelRawAnimation attackTimerAnimation = animation("get_attack_timer");
    private final CitadelRawAnimation ageStillAnimation = animation("func_78087_a.age_in_ticks.get_still_ani_1");
    private final CitadelRawAnimation attackTimerStillAnimation = animation("get_attack_timer.get_still_ani_1");
    private final CitadelRawAnimation ageStatus1Animation = animation("func_78087_a.age_in_ticks.get_parasite_status_1");
    private final CitadelRawAnimation limbStatus1Animation = animation("func_78087_a.limb_swing.get_parasite_status_1");
    private final CitadelRawAnimation attackTimerStatus1Animation = animation("get_attack_timer.get_parasite_status_1");
    private final CitadelRawAnimation ageStatus1StillAnimation = animation(
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final CitadelRawAnimation attackTimerStatus1StillAnimation = animation(
            "get_attack_timer.get_parasite_status_1.get_still_ani_1");
    private final CitadelRawAnimation ageStatus2Animation = animation("func_78087_a.age_in_ticks.get_parasite_status_2");
    private final CitadelRawAnimation limbStatus2Animation = animation("func_78087_a.limb_swing.get_parasite_status_2");
    private final CitadelRawAnimation attackTimerStatus2Animation = animation("get_attack_timer.get_parasite_status_2");
    private final CitadelRawAnimation ageStatus2StillAnimation = animation(
            "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1");
    private final CitadelRawAnimation attackTimerStatus2StillAnimation = animation(
            "get_attack_timer.get_parasite_status_2.get_still_ani_1");
    private final CitadelRawAnimation ageStatus3Animation = animation("func_78087_a.age_in_ticks.get_parasite_status_3");
    private final CitadelRawAnimation ageStatus4Animation = animation("func_78087_a.age_in_ticks.get_parasite_status_4");
    private final CitadelRawAnimation attackTimerStatus4Animation = animation("get_attack_timer.get_parasite_status_4");
    private final CitadelRawAnimation ageStatus10Animation = animation("func_78087_a.age_in_ticks.get_parasite_status_10");
    private final CitadelRawAnimation attackTimerStatus10Animation = animation("get_attack_timer.get_parasite_status_10");
    private final CitadelRawAnimation ageStatus25Animation = animation("func_78087_a.age_in_ticks.get_parasite_status_25");
    private final CitadelRawAnimation attackTimerStatus25Animation = animation("get_attack_timer.get_parasite_status_25");

    @Nullable
    private UUID leftTendrilId;
    @Nullable
    private UUID rightTendrilId;
    private int smashTicks;
    private int smashCooldown = 80;
    private int stillTicks;
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
        builder.define(PARASITE_STATUS, 0);
        builder.define(STILL_ANI, false);
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
        updateCitadelAnimationState();
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

    private void updateCitadelAnimationState() {
        double dx = getX() - xo;
        double dz = getZ() - zo;
        if (dx * dx + dz * dz <= 1.0E-6D) {
            stillTicks++;
        } else {
            stillTicks = 0;
        }
        entityData.set(STILL_ANI, stillTicks > 25);

        int status;
        if (smashTicks > 0) {
            status = smashTicks < SMASH_CHARGE_TICKS ? 25 : 3;
        } else if (isSpecialLeapAnimating()) {
            status = 10;
        } else {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                status = 0;
            } else {
                double attackReach = getBbWidth() * 2.0D;
                double attackReachSqr = attackReach * attackReach + target.getBbWidth();
                status = distanceToSqr(target) > attackReachSqr ? 2 : 1;
            }
        }
        entityData.set(PARASITE_STATUS, status);
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
        entityData.set(ATTACK_TICKS, ATTACK_ANIMATION_TICKS);
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

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public boolean getStillAni() {
        return entityData.get(STILL_ANI);
    }

    public int getSmashTicks() {
        return entityData.get(SMASH_TICKS);
    }

    /**
     * 获取攻击动画进度（0.0-1.0）
     * 对应原模组的getAttackTimer()方法
     * 用于客户端渲染器平滑插值攻击动画
     */
    public float getAttackAnimationProgress(float partialTick) {
        int ticks = getAttackTicks();
        if (ticks <= 0) {
            return 0.0F;
        }
        float elapsed = ATTACK_ANIMATION_TICKS - ticks + partialTick;
        float progress = elapsed <= 6.0F ? elapsed * 0.2F : 1.2F - (elapsed - 6.0F) * 0.1F;
        return Mth.clamp(progress, 0.0F, 1.0F);
    }

    private CitadelRawAnimation animation(String action) {
        return ParasiteAnimations.loop(this, action);
    }

    /**
     * 获取左触手健康值（归一化到0.0-1.0）
     * 对应原模组的getLeft()方法
     * 用于客户端渲染器控制左触手的显示和动画
     */
    public float getLeftTendrilHealthNormalized() {
        float health = entityData.get(LEFT_TENDRIL_HEALTH);
        float maxHealth = maxTendrilHealth();
        return maxHealth > 0.0F ? Mth.clamp(health / maxHealth, 0.0F, 1.0F) : 0.0F;
    }

    /**
     * 获取右触手健康值（归一化到0.0-1.0）
     * 对应原模组的getRight()方法
     * 用于客户端渲染器控制右触手的显示和动画
     */
    public float getRightTendrilHealthNormalized() {
        float health = entityData.get(RIGHT_TENDRIL_HEALTH);
        float maxHealth = maxTendrilHealth();
        return maxHealth > 0.0F ? Mth.clamp(health / maxHealth, 0.0F, 1.0F) : 0.0F;
    }

    /**
     * 获取Smash技能动画进度（0.0-1.0）
     * 对应原模组的border计数器
     * 用于客户端渲染器和粒子效果的渐变控制
     */
    public float getSmashAnimationProgress(float partialTick) {
        int ticks = getSmashTicks();
        if (ticks <= 0) {
            return 0.0F;
        }
        return Mth.clamp((ticks + partialTick) / (float) SMASH_DURATION_TICKS, 0.0F, 1.0F);
    }

    /**
     * 判断是否处于Smash充能阶段
     * 对应原模组的border < 20判断
     */
    public boolean isSmashCharging() {
        int ticks = getSmashTicks();
        return ticks > 0 && ticks < SMASH_CHARGE_TICKS;
    }

    /**
     * 判断是否处于Smash攻击阶段
     * 对应原模组的border >= 20判断
     */
    public boolean isSmashAttacking() {
        int ticks = getSmashTicks();
        return ticks >= SMASH_CHARGE_TICKS && ticks <= SMASH_DURATION_TICKS;
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
    public void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers) {
        controllers.add(new CitadelAnimationController<>(this, "age_controller", 0, state ->
                state.setAndContinue(ageAnimation())));
        controllers.add(new CitadelAnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    private CitadelRawAnimation ageAnimation() {
        int status = getParasiteStatus();
        boolean still = getStillAni();
        if (getAttackTicks() > 0) {
            return switch (status) {
                case 1 -> still ? attackTimerStatus1StillAnimation : attackTimerStatus1Animation;
                case 2 -> still ? attackTimerStatus2StillAnimation : attackTimerStatus2Animation;
                case 4 -> attackTimerStatus4Animation;
                case 10 -> attackTimerStatus10Animation;
                case 25 -> attackTimerStatus25Animation;
                default -> still ? attackTimerStillAnimation : attackTimerAnimation;
            };
        }
        return switch (status) {
            case 1 -> still ? ageStatus1StillAnimation : ageStatus1Animation;
            case 2 -> still ? ageStatus2StillAnimation : ageStatus2Animation;
            case 3 -> ageStatus3Animation;
            case 4 -> ageStatus4Animation;
            case 10 -> ageStatus10Animation;
            case 25 -> ageStatus25Animation;
            default -> still ? ageStillAnimation : ageInTicksAnimation;
        };
    }

    /** Limb-swing functions only run while the entity actually changes position. */
    private CitadelPlayState movementAnimation(CitadelAnimationState<MarauderEntity> state) {
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return CitadelPlayState.STOP;
        }
        return switch (getParasiteStatus()) {
            case 1 -> state.setAndContinue(limbStatus1Animation);
            case 2 -> state.setAndContinue(limbStatus2Animation);
            case 0 -> state.setAndContinue(limbSwingAnimation);
            default -> CitadelPlayState.STOP;
        };
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
