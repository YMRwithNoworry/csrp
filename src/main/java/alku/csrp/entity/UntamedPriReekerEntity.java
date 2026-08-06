package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * UntamedPriReeker - 未驯化的原始尖啸者
 * 原版 EntityRanrac (PriReeker) 的完整移植
 *
 * 特性：
 * - 冲锋攻击技能（蓄力20 ticks，冲锋40 ticks）
 * - 多阶段动画系统（正常、攻击准备、冲锋恢复、冲锋中）
 * - 碰撞检测与击退效果
 * - 攀爬能力
 */
public class UntamedPriReekerEntity extends Monster implements GeoEntity, Parasite {
    // 动画状态常量
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_ATTACK_PREP = 1;
    private static final int STATUS_CHARGE_RECOVERY = 2;
    private static final int STATUS_CHARGING = 3;

    // 冲锋技能相关常量
    private static final int CHARGE_COOLDOWN = 100;
    private static final int CHARGE_WINDUP_TICKS = 20;
    private static final int CHARGE_MAX_TICKS = 60;
    private static final float CHARGE_SPEED = 2.5F;
    private static final float CHARGE_DAMAGE = 8.0F;
    private static final double CHARGE_COLLISION_RANGE = 2.0;
    private static final double CHARGE_MIN_DISTANCE = 4.0;
    private static final double CHARGE_MAX_DISTANCE = 16.0;
    private static final double CHARGE_HEIGHT_DIFF = 3.0;

    // 数据同步器
    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(UntamedPriReekerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE_TICKS =
            SynchedEntityData.defineId(UntamedPriReekerEntity.class, EntityDataSerializers.INT);

    // 动画定义 - 按照原版 ModelRanrac 的动画映射
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK_PREP = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation CHARGE_RECOVERY = ParasiteAnimations.loop(this, "idle.get_parasite_status_2");
    private final RawAnimation CHARGING = ParasiteAnimations.loop(this, "idle.get_parasite_status_3");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    // 冲锋技能状态
    private boolean skillCharge = true;
    private Vec3 chargeTarget;
    private int chargeStuckTicks;

    public UntamedPriReekerEntity(EntityType<? extends UntamedPriReekerEntity> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, STATUS_NORMAL);
        builder.define(CHARGE_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ChargeAttackGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, entity -> entity instanceof LivingEntity && !(entity instanceof Parasite)));
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            int chargeTicks = entityData.get(CHARGE_TICKS);
            if (chargeTicks > 0) {
                entityData.set(CHARGE_TICKS, chargeTicks - 1);
                handleCharging();
            }

            // 蓄力阶段粒子效果
            if (chargeTicks > 0 && chargeTicks >= CHARGE_MAX_TICKS - CHARGE_WINDUP_TICKS) {
                spawnChargeParticles();
            }
        }
    }

    /**
     * 处理冲锋逻辑 - 三阶段：蓄力、冲锋、恢复
     */
    private void handleCharging() {
        int chargeTicks = entityData.get(CHARGE_TICKS);
        int attacking = CHARGE_MAX_TICKS - chargeTicks;

        // 阶段1：蓄力阶段 (0-19 ticks)
        if (attacking < CHARGE_WINDUP_TICKS) {
            setParasiteStatus(STATUS_ATTACK_PREP);
            navigation.stop();
            setDeltaMovement(Vec3.ZERO);

            // 每5 ticks播放声音
            if (attacking % 5 == 0) {
                playSound(ModSounds.RUPTER_LIVING.get(), 0.5F, 2.0F + random.nextFloat() * 0.8F);
            }
            return;
        }

        // 阶段2启动：计算冲锋方向 (20 tick)
        if (attacking == CHARGE_WINDUP_TICKS) {
            setParasiteStatus(STATUS_CHARGING);
            LivingEntity target = getTarget();
            if (target != null && canChargeTo(target)) {
                chargeTarget = target.position();
                Vec3 direction = chargeTarget.subtract(position()).normalize();
                setDeltaMovement(direction.scale(CHARGE_SPEED));
            } else {
                endCharge();
                return;
            }
        }

        // 阶段2：冲锋中 (20-59 ticks)
        if (attacking >= CHARGE_WINDUP_TICKS && attacking < CHARGE_MAX_TICKS) {
            setParasiteStatus(STATUS_CHARGING);

            // 碰撞检测 - 范围内的所有敌对生物
            AABB collisionBox = getBoundingBox().inflate(CHARGE_COLLISION_RANGE);
            for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, collisionBox,
                    e -> e instanceof LivingEntity && !(e instanceof Parasite) && e.isAlive())) {
                if (entity.hurt(damageSources().mobAttack(this), CHARGE_DAMAGE)) {
                    // 应用效果：缓慢 II (60 ticks) + COTH (3600 ticks)
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), this);
                    entity.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0), this);

                    // 击退效果：水平1.5 + 垂直0.5
                    Vec3 knockback = entity.position().subtract(position()).normalize().scale(1.5);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 0.5, knockback.z));
                }
            }

            // 卡住检测
            if (getDeltaMovement().horizontalDistanceSqr() < 0.01) {
                chargeStuckTicks++;
                if (chargeStuckTicks > 10) {
                    endCharge();
                }
            } else {
                chargeStuckTicks = 0;
            }

            // 空中减速
            if (!onGround()) {
                setDeltaMovement(getDeltaMovement().scale(0.7));
            }
        }

        // 超时结束
        if (attacking >= CHARGE_MAX_TICKS) {
            endCharge();
        }
    }

    /**
     * 检查是否可以向目标冲锋
     */
    private boolean canChargeTo(LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }

        // 高度差限制
        double heightDiff = target.getY() - getY();
        if (Math.abs(heightDiff) > CHARGE_HEIGHT_DIFF) {
            return false;
        }

        // 必须在地面或水中
        return onGround() || isInWater();
    }

    /**
     * 结束冲锋，进入恢复阶段
     */
    private void endCharge() {
        entityData.set(CHARGE_TICKS, 0);
        setParasiteStatus(STATUS_CHARGE_RECOVERY);
        skillCharge = true;
        chargeTarget = null;
        chargeStuckTicks = 0;
    }

    /**
     * 生成蓄力粒子效果
     */
    private void spawnChargeParticles() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    getX(), getY() + 0.5, getZ(),
                    3, 0.3, 0.3, 0.3, 0.02);
        }
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            // 触发攻击动画
            triggerAnim("attack_controller", "attack");

            if (target instanceof LivingEntity living) {
                // 普通攻击效果：缓慢 I (40 ticks) + COTH (3600 ticks)
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), this);
                living.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0), this);
            }
        }
        return hurt;
    }

    @Override
    public void travel(Vec3 travelVector) {
        int chargeTicks = entityData.get(CHARGE_TICKS);
        int attacking = CHARGE_MAX_TICKS - chargeTicks;

        // 蓄力时强制停止移动
        if (chargeTicks > 0 && attacking < CHARGE_WINDUP_TICKS) {
            super.travel(Vec3.ZERO);
            return;
        }

        super.travel(travelVector);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ParasiteStatus", getParasiteStatus());
        tag.putInt("ChargeTicks", entityData.get(CHARGE_TICKS));
        tag.putBoolean("SkillCharge", skillCharge);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setParasiteStatus(tag.getInt("ParasiteStatus"));
        entityData.set(CHARGE_TICKS, tag.getInt("ChargeTicks"));
        skillCharge = tag.getBoolean("SkillCharge");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.RUPTER_LIVING.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RUPTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RUPTER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.RUPTER_STEP.get(), 0.15F, 1.0F);
    }

    // ==================== GeckoLib 动画系统 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Movement Controller - 转换时间4 ticks
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));

        // Attack Controller - 转换时间0 ticks（立即响应）
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    /**
     * 移动动画控制器
     * 按照原版 ModelRanrac 的动画优先级：
     * 1. 冲锋恢复 (Status 2) - 若停止移动则切换到 NORMAL
     * 2. 冲锋中 (Status 3)
     * 3. 攻击准备 (Status 1)
     * 4. 常规移动 - 根据移动速度切换 IDLE/WALK
     */
    private <T extends UntamedPriReekerEntity> PlayState movementAnimation(AnimationState<T> state) {
        int status = getParasiteStatus();

        // 冲锋恢复动画 - 检测是否停止移动
        if (status == STATUS_CHARGE_RECOVERY) {
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                setParasiteStatus(STATUS_NORMAL);
                return state.setAndContinue(IDLE);
            }
            return state.setAndContinue(CHARGE_RECOVERY);
        }

        // 冲锋动画
        if (status == STATUS_CHARGING) {
            return state.setAndContinue(CHARGING);
        }

        // 攻击准备动画
        if (status == STATUS_ATTACK_PREP) {
            return state.setAndContinue(ATTACK_PREP);
        }

        // 常规移动动画
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(IDLE);
        }

        return state.setAndContinue(WALK);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    // ==================== 冲锋攻击 AI Goal ====================

    private final class ChargeAttackGoal extends Goal {
        private int cooldown;

        private ChargeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!skillCharge || cooldown > 0) {
                return false;
            }

            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }

            // 触发条件：距离4-16方块，高度差<3方块，在地面或水中
            double distanceSq = distanceToSqr(target);
            double minDistSq = CHARGE_MIN_DISTANCE * CHARGE_MIN_DISTANCE;
            double maxDistSq = CHARGE_MAX_DISTANCE * CHARGE_MAX_DISTANCE;

            return distanceSq > minDistSq && distanceSq < maxDistSq && canChargeTo(target);
        }

        @Override
        public boolean canContinueToUse() {
            return entityData.get(CHARGE_TICKS) > 0;
        }

        @Override
        public void start() {
            entityData.set(CHARGE_TICKS, CHARGE_MAX_TICKS);
            setParasiteStatus(STATUS_ATTACK_PREP);
            skillCharge = false;
            cooldown = CHARGE_COOLDOWN;
        }

        @Override
        public void stop() {
            if (entityData.get(CHARGE_TICKS) == 0) {
                setParasiteStatus(STATUS_NORMAL);
            }
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
        }
    }
}
