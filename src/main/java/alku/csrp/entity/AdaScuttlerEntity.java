package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

/**
 * Ada Scuttler - Adapted Arachnida (对应原模组的 EntityRanracAdapted)
 * 特性：蛛网拉拽技能、攀爬能力、多状态动画系统
 *
 * 动画状态说明：
 * - Status 0: 正常行走/待机
 * - Status 1: 攻击准备（减速，下颚张开）
 * - Status 2: 快速移动
 * - Status 3: 拉拽技能准备
 * - Status 11: 拉拽技能释放
 */
public class AdaScuttlerEntity extends BurrowingVariantEntity implements PullingBallOwner {
    private static final EntityDataAccessor<Integer> ARACHNIDA_STATUS = SynchedEntityData.defineId(
            AdaScuttlerEntity.class, EntityDataSerializers.INT);

    // 动画定义
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation ATTACK_PREP = ParasiteAnimations.loop(this, "walk.get_parasite_status_1");
    private final RawAnimation FAST_MOVE = ParasiteAnimations.loop(this, "walk.get_parasite_status_2");
    private final RawAnimation PULLING_PREP = ParasiteAnimations.loop(this, "idle.get_parasite_status_3");
    private final RawAnimation SKILL_CAST = ParasiteAnimations.loop(this, "idle.get_parasite_status_11");

    private int abilityCooldown;
    private int pullingTicks;
    private LivingEntity pullingTarget;

    public AdaScuttlerEntity(EntityType<? extends AdaScuttlerEntity> type, Level level) {
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

    public static boolean checkAdaScuttlerSpawnRules(EntityType<? extends AdaScuttlerEntity> type,
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
        builder.define(ARACHNIDA_STATUS, 0);
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
            updateArachnidaStatus();
        }
    }

    /**
     * 更新 Arachnida 状态机
     */
    private void updateArachnidaStatus() {
        int currentStatus = getArachnidaStatus();

        // 处理拉拽技能计时
        if (currentStatus == 3) {
            pullingTicks++;
            if (pullingTicks >= 100) { // 5秒后进入技能释放状态
                setArachnidaStatus(11);
                executePullingSkill();
                pullingTicks = 0;
            }
            return;
        } else {
            pullingTicks = 0;
        }

        // 技能释放后恢复到默认状态
        if (currentStatus == 11 && tickCount % 20 == 0) {
            setArachnidaStatus(0);
            abilityCooldown = 200; // 10秒冷却
            return;
        }

        // 更新状态逻辑
        if (currentStatus != 3 && currentStatus != 11) {
            LivingEntity target = getTarget();
            if (target != null) {
                double distSqr = distanceToSqr(target);

                // 检查是否可以使用拉拽技能
                if (abilityCooldown <= 0 && distSqr >= 49.0D && distSqr <= 400.0D && hasLineOfSight(target)) {
                    setArachnidaStatus(3);
                    pullingTarget = target;
                } else if (distSqr < 16.0D) {
                    // 状态 2: 快速移动（近距离）
                    setArachnidaStatus(2);
                } else if (distSqr < 64.0D) {
                    // 状态 1: 攻击准备（中距离，减速）
                    setArachnidaStatus(1);
                } else {
                    // 状态 0: 默认
                    setArachnidaStatus(0);
                }
            } else {
                setArachnidaStatus(0);
            }
        }
    }

    /**
     * 执行拉拽技能
     */
    private void executePullingSkill() {
        if (pullingTarget != null && pullingTarget.isAlive()) {
            // 发射拉拽弹丸
            PullingBallEntity pullingBall = ModEntities.PULLING_BALL.get().create(level());
            if (pullingBall != null) {
                Vec3 eyePos = getEyePosition();
                Vec3 targetPos = pullingTarget.getEyePosition();
                Vec3 direction = targetPos.subtract(eyePos).normalize().scale(1.0D);
                pullingBall.setPos(eyePos.x, eyePos.y, eyePos.z);
                pullingBall.setOwner(this);
                pullingBall.setDeltaMovement(direction);
                level().addFreshEntity(pullingBall);
                playSound(ModSounds.get("mob.shoot"), 1.0F, 1.0F);
            }
        }
        pullingTarget = null;
    }

    public int getArachnidaStatus() {
        return entityData.get(ARACHNIDA_STATUS);
    }

    public void setArachnidaStatus(int status) {
        entityData.set(ARACHNIDA_STATUS, status);
    }

    @Override
    public boolean onClimbable() {
        return horizontalCollision || super.onClimbable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("arachnida_status", getArachnidaStatus());
        tag.putInt("arachnida_pulling_ticks", pullingTicks);
        tag.putInt("arachnida_ability_cooldown", abilityCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setArachnidaStatus(tag.getInt("arachnida_status"));
        pullingTicks = tag.getInt("arachnida_pulling_ticks");
        abilityCooldown = tag.getInt("arachnida_ability_cooldown");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    private PlayState movementAnimation(AnimationState<AdaScuttlerEntity> state) {
        int status = getArachnidaStatus();

        // 状态 11: 技能释放动画
        if (status == 11) {
            return state.setAndContinue(SKILL_CAST);
        }

        // 状态 3: 拉拽准备动画
        if (status == 3) {
            return state.setAndContinue(PULLING_PREP);
        }

        // 状态 2: 快速移动
        if (status == 2 && state.isMoving()) {
            return state.setAndContinue(FAST_MOVE);
        }

        // 状态 1: 攻击准备（减速）
        if (status == 1 && state.isMoving()) {
            return state.setAndContinue(ATTACK_PREP);
        }

        // 状态 0: 默认移动
        if (!state.isMoving()) {
            return state.setAndContinue(IDLE);
        }

        return state.setAndContinue(WALK);
    }

    /**
     * 蛛网拉拽技能 AI
     */
    private final class WebPullGoal extends Goal {
        private WebPullGoal() {
            setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0
                    && target != null
                    && canPullTarget(target);
        }

        @Override
        public boolean canContinueToUse() {
            return getArachnidaStatus() == 3 && pullingTicks < 100;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            setArachnidaStatus(3);
            pullingTarget = target;
            pullingTicks = 0;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            if (getArachnidaStatus() == 3 || getArachnidaStatus() == 11) {
                setArachnidaStatus(0);
            }
        }

        /**
         * 检查目标是否可拉拽
         */
        private boolean canPullTarget(LivingEntity target) {
            double distanceSqr = distanceToSqr(target);
            return target.isAlive()
                    && hasLineOfSight(target)
                    && distanceSqr >= 49.0D
                    && distanceSqr <= 400.0D;
        }
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
    protected SoundEvent burrowSound() {
        return ModSounds.ADAPTED_BURROWER_DIG.get();
    }

    @Override
    protected int burrowSkillCooldownTicks() {
        return 200;
    }

    @Override
    public boolean isValidPullTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distanceSqr = distanceToSqr(target);
        return hasLineOfSight(target)
                && distanceSqr >= 49.0D
                && distanceSqr <= 400.0D;
    }

    @Override
    public boolean captureTarget(LivingEntity target) {
        if (level().isClientSide || target == null || !target.isAlive()) {
            return false;
        }

        // 施加拉拽效果
        Vec3 pullDirection = position().subtract(target.position());
        if (pullDirection.lengthSqr() > 0.001D) {
            pullDirection = pullDirection.normalize().scale(0.65D);
            target.push(pullDirection.x, 0.12D, pullDirection.z);
        }

        // 施加负面效果
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), this);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), this);
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2), this);

        return true;
    }
}
