package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.Config;
import alku.csrp.registry.ModSounds;
import alku.csrp.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
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
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.EnumSet;

/**
 * Ada Longleg - Adapted Arachnida variant (对应原模组的 EntityRanracAdapted)
 * 特性：蛛网拉拽技能、攀爬能力、多状态动画系统
 */
public class AdaLonglegEntity extends BurrowingVariantEntity implements PullingBallOwner {
    private static final EntityDataAccessor<Integer> ARACHNIDA_STATUS = SynchedEntityData.defineId(
            AdaLonglegEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CAN_PULL = SynchedEntityData.defineId(
            AdaLonglegEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PULLING_TICKS = SynchedEntityData.defineId(
            AdaLonglegEntity.class, EntityDataSerializers.INT);

    // 动画定义
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation AIMING = ParasiteAnimations.loop(this, "walk.get_parasite_status_1");
    private final RawAnimation ATTACK_PREP = ParasiteAnimations.loop(this, "walk.get_parasite_status_2");
    private final RawAnimation PULLING = ParasiteAnimations.loop(this, "idle.get_parasite_status_3");
    private final RawAnimation SKILL_CAST = ParasiteAnimations.loop(this, "idle.get_parasite_status_11");

    private int abilityCooldown;
    private int pullingDuration;
    private LivingEntity pullingTarget;

    public AdaLonglegEntity(EntityType<? extends AdaLonglegEntity> type, Level level) {
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

    public static boolean checkAdaLonglegSpawnRules(EntityType<? extends AdaLonglegEntity> type,
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ARACHNIDA_STATUS, 0);
        entityData.define(CAN_PULL, false);
        entityData.define(PULLING_TICKS, 0);
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
            if (pullingDuration > 0) pullingDuration--;

            updateArachnidaStatus();
            handlePullingEffect();
        }
    }

    /**
     * 更新 Arachnida 状态机
     */
    private void updateArachnidaStatus() {
        LivingEntity target = getTarget();

        // 状态 11: 技能施放
        if (entityData.get(ARACHNIDA_STATUS) == 11 && pullingDuration > 0) {
            return;
        }

        // 状态 3: 拉拽执行中
        if (entityData.get(PULLING_TICKS) > 0) {
            setArachnidaStatus(3);
            entityData.set(PULLING_TICKS, entityData.get(PULLING_TICKS) - 1);
            return;
        }

        // 状态 1: 警戒/瞄准（有目标且在拉拽范围内）
        if (target != null && canPullTarget(target)) {
            setArachnidaStatus(1);
            entityData.set(CAN_PULL, true);
            return;
        }

        // 状态 2: 攻击准备（接近目标快速移动）
        if (target != null && distanceToSqr(target) < 64.0D
                && getDeltaMovement().horizontalDistanceSqr() > 0.02D) {
            setArachnidaStatus(2);
            entityData.set(CAN_PULL, false);
            return;
        }

        // 状态 0: 空闲/默认
        setArachnidaStatus(0);
        entityData.set(CAN_PULL, false);
    }

    /**
     * 处理拉拽效果
     */
    private void handlePullingEffect() {
        if (pullingTarget == null || !pullingTarget.isAlive() || pullingDuration <= 0) {
            pullingTarget = null;
            pullingDuration = 0;
            return;
        }

        // 持续施加拉力
        Vec3 pullDirection = position().subtract(pullingTarget.position());
        if (pullDirection.lengthSqr() > 0.001D) {
            pullDirection = pullDirection.normalize().scale(0.65D);
            pullingTarget.push(pullDirection.x, 0.12D, pullDirection.z);
        }

        // 每秒施加负面效果
        if (pullingDuration % 20 == 0) {
            pullingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), this);
            pullingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1), this);
        }
    }

    /**
     * 检查目标是否可拉拽
     */
    private boolean canPullTarget(LivingEntity target) {
        double distanceSqr = distanceToSqr(target);
        return target != null
                && target.isAlive()
                && hasLineOfSight(target)
                && distanceSqr >= 9.0D
                && distanceSqr <= 400.0D;
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    private PlayState movementAnimation(AnimationState<AdaLonglegEntity> state) {
        int status = getArachnidaStatus();

        // 状态 11: 技能施放动画
        if (status == 11) {
            return state.setAndContinue(SKILL_CAST);
        }

        // 状态 3: 拉拽动画
        if (status == 3) {
            return state.setAndContinue(PULLING);
        }

        // 状态 2: 攻击准备（快速移动）
        if (status == 2) {
            return state.setAndContinue(ATTACK_PREP);
        }

        // 状态 1: 瞄准状态
        if (status == 1) {
            return state.setAndContinue(AIMING);
        }

        // 状态 0: 默认移动
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(IDLE);
        }

        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
    }

    /**
     * 蛛网拉拽技能 AI
     */
    private final class WebPullGoal extends Goal {
        private WebPullGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0
                    && target != null
                    && entityData.get(CAN_PULL)
                    && canPullTarget(target);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }

            // 设置技能施放状态
            setArachnidaStatus(11);
            pullingDuration = 60;

            // 朝向目标
            getLookControl().setLookAt(target, 30.0F, 30.0F);

            // 初始拉力和效果
            Vec3 pull = position().subtract(target.position());
            if (pull.lengthSqr() > 0.001D) {
                pull = pull.normalize().scale(0.65D);
                target.push(pull.x, 0.12D, pull.z);
            }

            // 施加初始负面效果
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), AdaLonglegEntity.this);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), AdaLonglegEntity.this);
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2), AdaLonglegEntity.this);

            // 设置拉拽目标和持续时间
            pullingTarget = target;
            entityData.set(PULLING_TICKS, 60);

            fireWebProjectile(target);

            // 设置冷却
            abilityCooldown = 70;
        }
    }

    private void fireWebProjectile(LivingEntity target) {
        PullingBallEntity projectile = ModEntities.PULLING_BALL.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.45D));
        Vec3 direction = target.getEyePosition().subtract(start);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        projectile.moveTo(start.x, start.y, start.z, getYRot(), getXRot());
        projectile.setOwner(this);
        projectile.setDeltaMovement(direction.normalize().scale(0.8D));
        level().addFreshEntity(projectile);
    }

    @Override
    public boolean captureTarget(LivingEntity target) {
        if (!isValidPullTarget(target)) {
            return false;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), this);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), this);
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2), this);
        pullingTarget = target;
        pullingDuration = 60;
        Vec3 pull = position().subtract(target.position());
        if (pull.lengthSqr() > 0.001D) {
            pull = pull.normalize().scale(0.65D);
            target.push(pull.x, 0.12D, pull.z);
        }
        return true;
    }

    @Override
    public boolean isValidPullTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
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
}
