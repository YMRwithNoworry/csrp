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
 *
 * <p><b>动画系统说明（基于原模组EntityEsor）：</b>
 * <ul>
 *   <li><b>movement_controller</b>: 控制待机、行走、跳跃、游泳、潜行动画</li>
 *   <li><b>climbing_controller</b>: 控制攀爬墙壁时的动画状态</li>
 *   <li><b>attack_controller</b>: 控制近战挥击攻击动画（触发式）</li>
 *   <li><b>smash_controller</b>: 控制Smash技能的充能和攻击阶段动画</li>
 *   <li><b>stunned_controller</b>: 控制眩晕/受击状态的震动动画</li>
 *   <li><b>tendril_controller</b>: 控制触手部件的显示和动画状态</li>
 * </ul>
 *
 * <p><b>原模组动画状态对照：</b>
 * <ul>
 *   <li>状态0（正常移动）→ IDLE/WALK</li>
 *   <li>状态1（水中游泳）→ SWIM_IDLE/SWIM_WALK</li>
 *   <li>状态2（攀爬）→ CLIMB</li>
 *   <li>状态3（技能·重击）→ SMASH/SMASH_CHARGE</li>
 *   <li>状态4（跳跃攻击）→ LEAP</li>
 *   <li>状态10（潜行）→ SNEAK_IDLE/SNEAK_WALK</li>
 *   <li>状态25（眩晕/受击）→ STUNNED</li>
 * </ul>
 *
 * <p><b>原模组动画参数对照：</b>
 * <ul>
 *   <li>attackTimer / up → ATTACK_TICKS数据参数</li>
 *   <li>leftTendrilHealth / rightTendrilHealth → LEFT_TENDRIL_HEALTH / RIGHT_TENDRIL_HEALTH</li>
 *   <li>CLIMBING数据参数 → 攀爬状态同步</li>
 *   <li>border计数器 → SMASH_TICKS数据参数</li>
 * </ul>
 *
 * <p><b>客户端动画接口：</b>
 * <ul>
 *   <li>{@link #getAttackAnimationProgress(float)} - 攻击动画进度（0.0-1.0）</li>
 *   <li>{@link #getLeftTendrilHealthNormalized()} - 左触手健康值（0.0-1.0）</li>
 *   <li>{@link #getRightTendrilHealthNormalized()} - 右触手健康值（0.0-1.0）</li>
 *   <li>{@link #getSmashAnimationProgress(float)} - Smash技能进度（0.0-1.0）</li>
 *   <li>{@link #isSmashCharging()} - 是否处于Smash充能阶段</li>
 *   <li>{@link #isSmashAttacking()} - 是否处于Smash攻击阶段</li>
 * </ul>
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
    // 基础动画（对应原模组状态0 - 正常移动）
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");

    // 水中游泳动画（对应原模组状态1 - 水中游泳）
    private final RawAnimation SWIM_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation SWIM_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_1");

    // 攀爬动画（对应原模组状态2 - 攀爬）
    private final RawAnimation CLIMB = ParasiteAnimations.loop(this, "walk.get_parasite_status_2");

    // 攻击动画
    private final RawAnimation SWIPE = ParasiteAnimations.play(this, "attack");

    // 跳跃攻击动画（对应原模组状态4 - 跳跃攻击）
    private final RawAnimation LEAP = ParasiteAnimations.loop(this, "idle.get_parasite_status_4");

    // 潜行动画（对应原模组状态10 - 潜行）
    private final RawAnimation SNEAK_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_10");
    private final RawAnimation SNEAK_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_10");

    // 眩晕/受击动画（对应原模组状态25 - 眩晕/受击）
    private final RawAnimation STUNNED = ParasiteAnimations.loop(this, "idle.get_parasite_status_25");

    // Smash技能动画（对应原模组状态3 - 技能·重击和border计数器）
    // 充能阶段：border < 20，播放粒子效果，施加减速
    private final RawAnimation SMASH_CHARGE = ParasiteAnimations.loop(this, "idle.get_parasite_status_25");
    // 攻击阶段：border >= 20，范围攻击，击退敌人
    private final RawAnimation SMASH = ParasiteAnimations.loop(this, "idle.get_parasite_status_3");

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
        // 将攻击计时器转换为0-1的进度值
        // 原模组使用up标志控制上升和下降，这里简化为线性衰减
        return Mth.clamp((ticks + partialTick) / 10.0F, 0.0F, 1.0F);
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主要移动和待机动画控制器（对应原模组状态0/1/10）
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));

        // 攀爬动画控制器（对应原模组状态2）
        controllers.add(new AnimationController<>(this, "climbing_controller", 2, this::climbingAnimation));

        // 攻击动画控制器（支持触发式挥击动画，对应原模组attackTimer）
        controllers.add(new AnimationController<>(this, "attack_controller", 0, this::attackAnimation)
                .triggerableAnim("swipe", SWIPE));

        // Smash技能动画控制器（对应原模组状态3和border计数器）
        controllers.add(new AnimationController<>(this, "smash_controller", 0, this::smashAnimation));

        // 眩晕/受击动画控制器（对应原模组状态25）
        controllers.add(new AnimationController<>(this, "stunned_controller", 2, this::stunnedAnimation));

        // 触手状态动画控制器（根据触手生命值控制动画）
        controllers.add(new AnimationController<>(this, "tendril_controller", 2, this::tendrilAnimation));
    }

    /**
     * 主要移动动画：待机、行走、跳跃
     * 对应原模组的基础移动状态
     *
     * 原模组状态映射：
     * - 状态0（正常移动）：IDLE/WALK
     * - 状态1（水中游泳）：SWIM_IDLE/SWIM_WALK
     * - 状态4（闪避动画）：LEAP
     * - 状态10（潜地动画）：SNEAK_IDLE/SNEAK_WALK
     * - 状态25（受击震动）：STUNNED
     */
    private PlayState movementAnimation(AnimationState<MarauderEntity> state) {
        // 跳跃动画优先级最高（对应原模组状态4 - 闪避动画）
        if (isSpecialLeapAnimating()) {
            return state.setAndContinue(LEAP);
        }

        // Smash技能进行时不播放移动动画
        if (getSmashTicks() > 0) {
            return PlayState.STOP;
        }

        // 攀爬状态由climbing_controller处理
        if (isClimbing()) {
            return PlayState.STOP;
        }

        boolean isMoving = getDeltaMovement().horizontalDistanceSqr() >= 0.001;

        // 水中游泳动画（对应原模组状态1 - 水中游泳）
        if (isInWater()) {
            return state.setAndContinue(isMoving ? SWIM_WALK : SWIM_IDLE);
        }

        // 正常移动动画（对应原模组状态0 - 正常移动）
        return state.setAndContinue(isMoving ? WALK : IDLE);
    }

    /**
     * 攀爬动画控制器
     * 对应原模组状态2 - 攀爬
     *
     * 原模组特性：
     * - 腿部摆动与状态1相似
     * - 触手摆动加强
     * - 触手末端摆动频率调整为0.3
     */
    private PlayState climbingAnimation(AnimationState<MarauderEntity> state) {
        if (isClimbing() && horizontalCollision) {
            // 使用专门的攀爬动画（对应原模组状态2）
            return state.setAndContinue(CLIMB);
        }
        return PlayState.STOP;
    }

    /**
     * 攻击动画控制器
     * 对应原模组的attackTimer和up标志
     * 通过ATTACK_TICKS数据参数同步攻击状态
     *
     * 原模组攻击动画特性：
     * - 攻击计时器（attackTimer）：0-1.0，上升速率0.2/tick，下降速率0.1/tick
     * - 触手攻击动作：
     *   - 右侧触手关节逐级弯曲：
     *     * jointRA1: -0.8 * attackTimer
     *     * jointRA2: -1.0 * attackTimer
     *     * jointRA4/6/8/10: +0.3~0.5 * attackTimer
     *   - 左侧触手镜像动作
     * - 触发式动画SWIPE通过triggerAnim("swipe")激活
     */
    private PlayState attackAnimation(AnimationState<MarauderEntity> state) {
        // 攻击计时器大于0时保持攻击动画状态
        // 触发式动画SWIPE会在performSweepAttack中通过triggerAnim触发
        if (getAttackTicks() > 0) {
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    /**
     * Smash技能动画控制器
     * 对应原模组状态3 - 技能"Smash"动画和border计数器
     *
     * 原模组Smash动画特性：
     * - 充能阶段（border < 20）：
     *   - 主体前倾：mainbody.rotateAngleX = 0.5
     *   - 腿部支撑姿态：jointLL/RL.rotateAngleX = 0.5
     *   - 身体扭动：bodym.rotateAngleY = sin(ageInTicks * 0.3) * 0.643
     *   - 触手收缩准备：jointRA1.rotateAngleZ = 1.5 + 动态值
     *   - 触手前伸蓄力：jointRA2.rotateAngleX = -0.5, rotateAngleZ = 动态值
     *   - 粒子效果：火焰粒子（FLAME）2个/tick
     *   - 减速效果：slowness 100级
     * - 攻击阶段（border >= 20 至 100）：
     *   - 范围伤害：6格半径，全额攻击伤害
     *   - 强力击退：水平1.2倍，垂直0.8倍
     *   - 周期音效：每7 tick播放扫击音效
     *   - 粒子效果：暴击粒子（CRIT）6个/tick
     * - 结束阶段：
     *   - 盟友狂暴增益：24格范围，持续60秒，等级1
     *   - 冷却时间：200 ticks (10秒)
     */
    private PlayState smashAnimation(AnimationState<MarauderEntity> state) {
        int currentSmashTicks = getSmashTicks();

        if (currentSmashTicks <= 0) {
            return PlayState.STOP;
        }

        // 充能阶段（对应原模组border < 20）
        if (currentSmashTicks < SMASH_CHARGE_TICKS) {
            return state.setAndContinue(SMASH_CHARGE);
        }

        // 攻击阶段（对应原模组border >= 20）
        if (currentSmashTicks <= SMASH_DURATION_TICKS) {
            return state.setAndContinue(SMASH);
        }

        return PlayState.STOP;
    }

    /**
     * 眩晕/受击动画控制器
     * 对应原模组状态25 - 眩晕/受击
     *
     * 原模组特性：
     * - 高频震动效果（sin(ageInTicks * 2.6F/2.27F)）
     * - offsetY和offsetZ轻微震动
     * - 表示受创或眩晕状态
     */
    private PlayState stunnedAnimation(AnimationState<MarauderEntity> state) {
        // 眩晕状态可以通过多种条件触发
        // 例如：被击退、被冰冻、被虚弱等debuff效果
        // 这里检查是否有眩晕相关的效果
        if (hasEffect(MobEffects.WEAKNESS) || hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && getSmashTicks() <= 0) {
            return state.setAndContinue(STUNNED);
        }

        return PlayState.STOP;
    }

    /**
     * 触手状态动画控制器
     * 对应原模组的EntityBody部件和leftTendrilHealth/rightTendrilHealth
     *
     * 原模组触手系统特性：
     * - 左侧触须（leftTendril）：可独立受伤，血量 = 实体血量 * tendrilHealth配置
     * - 右侧触须（rightTendril）：可独立受伤
     * - 触须被破坏时：
     *   - 生成EntityTendril掉落物
     *   - 减少实体抗性：cutResistances(purePointDamCap / 2)
     *   - 触发客户端粒子效果（状态11=左触手破坏，状态22=右触手破坏）
     *
     * 触手动画细节（各状态下的触手表现）：
     * - 状态0（正常移动）：
     *   - 触手左右摆动，幅度0.3 * GD（获取增量），交替模式
     *   - 头发/触须正弦波摆动，频率0.13-0.15，幅度0.119-0.13
     * - 状态1（水中游泳）：
     *   - 触手摆动加强，后部触手保持0.3 * GD
     *   - 头发/触须加速，频率提升至0.16-0.18
     * - 状态2（攀爬）：
     *   - 触手末端摆动频率调整为0.3
     * - 状态3（Smash技能）：
     *   - 触手收缩准备姿态：jointRA1.rotateAngleZ = 1.5 + 动态值
     *   - 触手前伸攻击：jointRA2.rotateAngleX = -0.5
     * - 状态4（闪避）：
     *   - 触手防御姿态：jointRA1.rotateAngleX = -0.5 + 摆动
     *   - 触手横向展开：rotateAngleZ = 0.5 + 摆动
     * - 状态10（潜地）：
     *   - 触手前伸挖掘：jointLA1.rotateAngleX = -0.7 + 动态
     *   - 触手侧摆：rotateAngleZ = -0.5 + 动态
     *
     * 客户端接口：
     * - getLeftTendrilHealthNormalized() → 左触手健康值（0.0-1.0）
     * - getRightTendrilHealthNormalized() → 右触手健康值（0.0-1.0）
     * - 渲染器应根据这些值控制触手模型的显示与动画
     */
    private PlayState tendrilAnimation(AnimationState<MarauderEntity> state) {
        // 触手动画状态由渲染器通过触手健康值判断
        // 这里主要用于同步触手的整体状态
        boolean hasAnyTendril = isLeftTendrilAttached() || isRightTendrilAttached();

        if (hasAnyTendril) {
            // 如果有触手存在，保持触手动画播放
            // 具体的左右触手状态通过getLeftTendrilHealthNormalized()和
            // getRightTendrilHealthNormalized()方法在渲染器中处理
            return state.setAndContinue(IDLE);
        }

        return PlayState.STOP;
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
