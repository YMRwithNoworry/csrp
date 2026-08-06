package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * SimHuman (特殊人形感染体) - EntitySpeHuman
 * 具有四种动画状态的人形寄生体
 */
public final class SimHumanEntity extends Monster implements GeoEntity, Parasite {

    // 动画状态常量
    public static final int STATE_NORMAL = 0;      // 正常状态
    public static final int STATE_TRACKING = 1;    // 攻击准备/跟踪状态
    public static final int STATE_SNEAKING = 2;    // 潜行/慢速状态
    public static final int STATE_RIDING = 3;      // 骑乘/抓取状态

    private static final int COTH_AURA_INTERVAL_TICKS = 20;
    private static final double COTH_AURA_RADIUS = 3.0D;

    // 同步数据访问器
    private static final EntityDataAccessor<Integer> ANIMATION_STATE = SynchedEntityData.defineId(
            SimHumanEntity.class, EntityDataSerializers.INT);

    // 动画定义
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation TRACKING = ParasiteAnimations.loop(this, "tracking");
    private final RawAnimation SNEAKING = ParasiteAnimations.loop(this, "sneaking");
    private final RawAnimation RIDING = ParasiteAnimations.loop(this, "riding");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public SimHumanEntity(EntityType<? extends SimHumanEntity> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIMATION_STATE, STATE_NORMAL);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        // 更新动画状态
        updateAnimationState();

        // 定期感染附近生物
        if (tickCount % COTH_AURA_INTERVAL_TICKS == 0) {
            infectNearby();
        }
    }

    /**
     * 根据实体当前状态更新动画状态
     */
    private void updateAnimationState() {
        LivingEntity target = getTarget();

        if (target != null) {
            double distanceToTarget = distanceToSqr(target);

            // 骑乘状态 - 当实体正在骑乘其他生物时
            if (isPassenger()) {
                setAnimationState(STATE_RIDING);
            }
            // 近距离跟踪状态
            else if (distanceToTarget < 16.0D) {
                setAnimationState(STATE_TRACKING);
            }
            // 潜行状态 - 远距离接近目标
            else if (distanceToTarget < 64.0D && getDeltaMovement().horizontalDistanceSqr() < 0.01D) {
                setAnimationState(STATE_SNEAKING);
            }
            else {
                setAnimationState(STATE_NORMAL);
            }
        } else {
            setAnimationState(STATE_NORMAL);
        }
    }

    /**
     * 设置动画状态
     */
    public void setAnimationState(int state) {
        if (getAnimationState() != state) {
            entityData.set(ANIMATION_STATE, state);
        }
    }

    /**
     * 获取当前动画状态
     */
    public int getAnimationState() {
        return entityData.get(ANIMATION_STATE);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !level().isClientSide) {
            triggerAnim("attack_controller", "attack");

            // 尝试骑乘目标
            if (target instanceof LivingEntity living && living.isAlive() && !isPassenger()) {
                if (random.nextFloat() < 0.3F) {
                    startRiding(living, true);
                }
            }
        }
        return hit;
    }

    @Override
    public void rideTick() {
        super.rideTick();

        Entity vehicle = getVehicle();
        if (vehicle instanceof LivingEntity living) {
            // 持续伤害被骑乘的生物
            if (tickCount % 20 == 0) {
                living.hurt(damageSources().mobAttack(this), 2.0F);
            }

            // 应用感染效果
            if (tickCount % 40 == 0) {
                InfectionMechanics.applyCoth(living, this);
            }

            // 随机推动骑乘目标
            if (tickCount % 10 == 0 && random.nextFloat() < 0.3F) {
                double pushX = (random.nextDouble() - 0.5D) * 0.13D;
                double pushZ = (random.nextDouble() - 0.5D) * 0.13D;
                living.push(pushX, 0, pushZ);
            }
        }
    }

    /**
     * 感染附近的生物
     */
    private void infectNearby() {
        for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(COTH_AURA_RADIUS), this::isValidParasiteTarget)) {
            if (hasLineOfSight(nearby)) {
                InfectionMechanics.applyCoth(nearby, this);
            }
        }
    }

    /**
     * 判断是否为有效的寄生目标
     */
    private boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("animation_state", getAnimationState());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ANIMATION_STATE, tag.getInt("animation_state"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主要移动控制器 - 根据状态切换动画
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> {
                    int animState = getAnimationState();

                    // 根据动画状态选择对应的动画
                    switch (animState) {
                        case STATE_TRACKING:
                            return state.setAndContinue(
                                    getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? TRACKING : IDLE);
                        case STATE_SNEAKING:
                            return state.setAndContinue(
                                    getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? SNEAKING : IDLE);
                        case STATE_RIDING:
                            return state.setAndContinue(RIDING);
                        case STATE_NORMAL:
                        default:
                            return state.setAndContinue(
                                    getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? WALK : IDLE);
                    }
                }));

        // 攻击动画控制器
        controllers.add(new AnimationController<>(this, "attack_controller", 0,
                state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
