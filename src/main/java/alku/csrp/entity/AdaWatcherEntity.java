package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelAnimationState;
import alku.csrp.animation.CitadelPlayState;
import alku.csrp.animation.CitadelRawAnimation;

import java.util.EnumSet;

/**
 * Ada Watcher - Adapted tier arachnid variant
 * 基于 EntityRanracAdapted 的动画系统实现
 * 特性：蛛网拉拽技能、攀爬能力、多状态动画系统
 */
public class AdaWatcherEntity extends BurrowingVariantEntity implements PullingBallOwner {
    // 状态数据同步器
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            AdaWatcherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_COOLDOWN_ANI = SynchedEntityData.defineId(
            AdaWatcherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STILL_ANI = SynchedEntityData.defineId(
            AdaWatcherEntity.class, EntityDataSerializers.BOOLEAN);

    // 动画定义 - 对应原模组的动画状态
    private final CitadelRawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final CitadelRawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final CitadelRawAnimation RUN = ParasiteAnimations.loop(this, "run");
    // Status 1: 攻击准备状态（下颚张开，频率加快）
    private final CitadelRawAnimation ATTACK_PREP = ParasiteAnimations.loop(this, "walk.get_parasite_status_1");
    // Status 2: 攻击执行状态（触须前伸，下颚张开）
    private final CitadelRawAnimation ATTACK_EXEC = ParasiteAnimations.loop(this, "walk.get_parasite_status_2");
    // Status 3: 拉拽目标状态（触须完全伸展摆动）
    private final CitadelRawAnimation PULLING = ParasiteAnimations.loop(this, "idle.get_parasite_status_3");
    // Status 11: 技能释放状态
    private final CitadelRawAnimation SKILL_CAST = ParasiteAnimations.loop(this, "idle.get_parasite_status_11");

    // AI 变量
    private int abilityCooldown;
    private int pullingTicks;
    private LivingEntity pullingTargetEntity;

    public AdaWatcherEntity(EntityType<? extends AdaWatcherEntity> type, Level level) {
        super(type, level);
        xpReward = 55;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ATTACK_DAMAGE, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.50D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    public static boolean checkAdaWatcherSpawnRules(EntityType<? extends AdaWatcherEntity> type,
                                                     ServerLevelAccessor level,
                                                     MobSpawnType spawnType,
                                                     BlockPos pos,
                                                     RandomSource random) {
        int phase = Config.evolutionPhase(level.getLevel());
        return phase >= 4 && checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return 10;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return 0.10F;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return 8;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return 0.80F;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return 0.50F;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, 0);
        builder.define(ATTACK_COOLDOWN_ANI, 0);
        builder.define(STILL_ANI, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WebPullGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.20D, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (abilityCooldown > 0) abilityCooldown--;
            if (entityData.get(ATTACK_COOLDOWN_ANI) > 0) {
                entityData.set(ATTACK_COOLDOWN_ANI, entityData.get(ATTACK_COOLDOWN_ANI) - 1);
            }
            updateParasiteStatus();
            handlePullingEffect();
        }
    }

    /**
     * 更新寄生体状态机（基于原模组 EntityRanracAdapted 的逻辑）
     */
    private void updateParasiteStatus() {
        int currentStatus = getParasiteStatus();
        LivingEntity target = getTarget();

        // Status 11: 技能释放状态（拉拽技能执行完毕后短暂保持）
        if (currentStatus == 11) {
            if (pullingTicks <= 0) {
                setParasiteStatus(0);
            }
            return;
        }

        // Status 3: 拉拽目标状态（持续 400 tick 或目标丢失）
        if (currentStatus == 3) {
            pullingTicks++;
            if (pullingTicks >= 400 || pullingTargetEntity == null || !pullingTargetEntity.isAlive()) {
                setParasiteStatus(0);
                pullingTicks = 0;
                pullingTargetEntity = null;
            }
            return;
        }

        // 没有目标时重置为 Status 0
        if (target == null) {
            setParasiteStatus(0);
            return;
        }

        double distSqr = distanceToSqr(target);

        // Status 2: 攻击执行状态（近距离快速移动）
        if (distSqr < 16.0D && getDeltaMovement().horizontalDistanceSqr() > 0.01D) {
            setParasiteStatus(2);
            return;
        }

        // Status 1: 攻击准备状态（中距离减速接近）
        if (distSqr >= 16.0D && distSqr < 64.0D) {
            setParasiteStatus(1);
            return;
        }

        // Status 0: 默认空闲/行走状态
        setParasiteStatus(0);
    }

    /**
     * 处理拉拽效果（对应原模组的 pulling 机制）
     */
    private void handlePullingEffect() {
        if (getParasiteStatus() != 3 || pullingTargetEntity == null || !pullingTargetEntity.isAlive()) {
            return;
        }

        // 每 5 tick 施加一次拉力
        if (pullingTicks % 5 == 0) {
            Vec3 pullDirection = position().subtract(pullingTargetEntity.position());
            if (pullDirection.lengthSqr() > 0.001D) {
                pullDirection = pullDirection.normalize().scale(0.35D);
                pullingTargetEntity.push(pullDirection.x, 0.08D, pullDirection.z);
            }

            // 施加负面效果
            pullingTargetEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2), this);
            pullingTargetEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1), this);
        }
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    public int getAttackCooldownAni() {
        return entityData.get(ATTACK_COOLDOWN_ANI);
    }

    public void setAttackCooldownAni(int ticks) {
        entityData.set(ATTACK_COOLDOWN_ANI, ticks);
    }

    public boolean getStillAni() {
        return entityData.get(STILL_ANI);
    }

    public void setStillAni(boolean still) {
        entityData.set(STILL_ANI, still);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasite_status", getParasiteStatus());
        tag.putInt("attack_cooldown_ani", getAttackCooldownAni());
        tag.putBoolean("still_ani", getStillAni());
        tag.putInt("pulling_ticks", pullingTicks);
        tag.putInt("ability_cooldown", abilityCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setParasiteStatus(tag.getInt("parasite_status"));
        setAttackCooldownAni(tag.getInt("attack_cooldown_ani"));
        setStillAni(tag.getBoolean("still_ani"));
        pullingTicks = tag.getInt("pulling_ticks");
        abilityCooldown = tag.getInt("ability_cooldown");
    }

    @Override
    public boolean onClimbable() {
        // 蜘蛛类寄生体可以攀爬墙壁
        return horizontalCollision || super.onClimbable();
    }

    @Override
    protected boolean supportsBurrowing() {
        return false;
    }

    @Override
    protected int bodySegmentCount() {
        return 0;
    }

    @Override
    protected int burrowSkillCooldownTicks() {
        return 80;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent burrowSound() {
        return ModSounds.get("adapted.dig");
    }

    // PullingBallOwner 接口实现
    @Override
    public boolean captureTarget(LivingEntity target) {
        if (!isValidPullTarget(target)) {
            return false;
        }
        // 设置拉拽状态
        setParasiteStatus(3);
        pullingTicks = 0;
        pullingTargetEntity = target;
        setStillAni(false);
        return true;
    }

    @Override
    public boolean isValidPullTarget(LivingEntity target) {
        return isValidParasiteTarget(target) && target.isAlive();
    }

    @Override
    public void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers) {
        controllers.add(new CitadelAnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    /**
     * 动画控制器（基于 ModelRanracAdapted 的 setRotationAngles 逻辑）
     */
    private CitadelPlayState movementAnimation(CitadelAnimationState<AdaWatcherEntity> state) {
        int status = getParasiteStatus();

        // Status 11: 技能释放动画
        if (status == 11) {
            return state.setAndContinue(SKILL_CAST);
        }

        // Status 3: 拉拽目标动画（触须完全伸展摆动，下颚大幅张开）
        if (status == 3) {
            return state.setAndContinue(PULLING);
        }

        // Status 2: 攻击执行动画（触须前伸，腿部加速）
        if (status == 2) {
            if (ParasiteAnimations.isMoving(this, state.isMoving())) {
                return state.setAndContinue(ATTACK_EXEC);
            }
            return state.setAndContinue(IDLE);
        }

        // Status 1: 攻击准备动画（下颚张开，腿部减速）
        if (status == 1) {
            if (ParasiteAnimations.isMoving(this, state.isMoving())) {
                return state.setAndContinue(ATTACK_PREP);
            }
            return state.setAndContinue(IDLE);
        }

        // Status 0: 默认空闲/行走动画
        if (!ParasiteAnimations.isMoving(this, state.isMoving()) || getStillAni()) {
            return state.setAndContinue(IDLE);
        }

        // 根据速度选择行走或奔跑动画
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
    }

    /**
     * 蛛网拉拽技能 AI Goal
     * 对应原模组的拉拽技能机制
     */
    private final class WebPullGoal extends Goal {
        private WebPullGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (abilityCooldown > 0 || target == null || !target.isAlive()) {
                return false;
            }
            double distSqr = distanceToSqr(target);
            // 拉拽技能触发范围：7-20 格
            return distSqr >= 49.0D && distSqr <= 400.0D && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return getParasiteStatus() == 3 && pullingTicks < 400
                    && pullingTargetEntity != null && pullingTargetEntity.isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }

            // 设置拉拽状态
            setParasiteStatus(3);
            pullingTicks = 0;
            pullingTargetEntity = target;
            setStillAni(false);

            // 停止移动并朝向目标
            getNavigation().stop();
            getLookControl().setLookAt(target, 30.0F, 30.0F);

            // 播放音效
            playSound(ModSounds.get("mob.shoot"), 1.5F, 0.8F + random.nextFloat() * 0.4F);

            // 发射拉拽弹丸（如果有对应实体）
            firePullingProjectile(target);

            // 设置冷却时间（10秒）
            abilityCooldown = 200;
        }

        @Override
        public void tick() {
            if (pullingTargetEntity != null) {
                getLookControl().setLookAt(pullingTargetEntity, 30.0F, 30.0F);
            }

            // 达到 100 tick（5秒）后进入技能释放状态
            if (pullingTicks >= 100 && getParasiteStatus() == 3) {
                setParasiteStatus(11);
                executePullingSkill();
            }
        }

        @Override
        public void stop() {
            if (getParasiteStatus() == 3 || getParasiteStatus() == 11) {
                setParasiteStatus(0);
            }
            pullingTicks = 0;
            pullingTargetEntity = null;
            setStillAni(false);
        }
    }

    /**
     * 发射拉拽弹丸
     */
    private void firePullingProjectile(LivingEntity target) {
        // 尝试创建拉拽弹丸实体
        PullingBallEntity projectile = ModEntities.PULLING_BALL.get().create(level());
        if (projectile == null) {
            return;
        }

        Vec3 eyePos = getEyePosition();
        Vec3 direction = target.getEyePosition().subtract(eyePos).normalize();

        projectile.moveTo(eyePos.x, eyePos.y, eyePos.z, getYRot(), getXRot());
        projectile.setOwner(this);
        // 设置弹丸运动方向和速度
        projectile.setDeltaMovement(direction.scale(1.0D));

        level().addFreshEntity(projectile);
    }

    /**
     * 执行拉拽技能效果
     */
    private void executePullingSkill() {
        if (pullingTargetEntity == null || !pullingTargetEntity.isAlive()) {
            return;
        }

        // 施加强力拉拽
        Vec3 pullDirection = position().subtract(pullingTargetEntity.position());
        if (pullDirection.lengthSqr() > 0.001D) {
            pullDirection = pullDirection.normalize().scale(1.2D);
            pullingTargetEntity.push(pullDirection.x, 0.35D, pullDirection.z);
        }

        // 施加持续负面效果
        pullingTargetEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3), this);
        pullingTargetEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2), this);
        pullingTargetEntity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2), this);

        // 播放技能释放音效
        playSound(ModSounds.get("mob.swipe"), 2.0F, 0.7F + random.nextFloat() * 0.3F);
    }
}
