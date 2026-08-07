package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
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
 * Assimilated Human animation states mirror ModelInfHuman.
 */
public final class SimHumanEntity extends Monster implements GeoEntity, Parasite {

    // 动画状态常量
    public static final int STATE_NORMAL = 0;
    public static final int STATE_ATTACK = 1;
    public static final int STATE_PURSUIT = 2;

    private static final int COTH_AURA_INTERVAL_TICKS = 20;
    private static final double COTH_AURA_RADIUS = 3.0D;

    // 同步数据访问器
    private static final EntityDataAccessor<Integer> ANIMATION_STATE = SynchedEntityData.defineId(
            SimHumanEntity.class, EntityDataSerializers.INT);

    private static final int STILL_ANIMATION_DELAY_TICKS = 25;
    private final RawAnimation AGE = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation AGE_STILL = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_still_ani_1");
    private final RawAnimation AGE_STATUS_1 = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation LIMB_STATUS_1 = ParasiteAnimations.loop(
            this, "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation AGE_STATUS_1_STILL = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation AGE_STATUS_2 = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_2");
    private final RawAnimation LIMB_STATUS_2 = ParasiteAnimations.loop(
            this, "func_78087_a.limb_swing.get_parasite_status_2");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int stillAnimationTicks;

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
    protected SoundEvent getAmbientSound() {
        return getAnimationState() == STATE_NORMAL
                ? ParasiteSoundProfiles.ambient(this) : ModSounds.get("mob.silence");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ParasiteSoundProfiles.hurt(this);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ParasiteSoundProfiles.death(this);
    }

    @Override
    public void tick() {
        super.tick();

        if (ParasiteAnimations.isMoving(this, true)) {
            stillAnimationTicks = 0;
        } else {
            stillAnimationTicks++;
        }

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

        if (target == null || !target.isAlive()) {
            setAnimationState(STATE_NORMAL);
            return;
        }
        double reach = getBbWidth() * 2.0D;
        setAnimationState(isPassenger()
                || distanceToSqr(target) > reach * reach + target.getBbWidth()
                ? STATE_PURSUIT : STATE_ATTACK);
    }

    /**
     * 设置动画状态
     */
    public void setAnimationState(int state) {
        int clampedState = Math.clamp(state, STATE_NORMAL, STATE_PURSUIT);
        if (getAnimationState() != clampedState) {
            entityData.set(ANIMATION_STATE, clampedState);
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
        setAnimationState(tag.getInt("animation_state"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 0,
                state -> state.setAndContinue(ageAnimation())));
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                return PlayState.STOP;
            }
            return state.setAndContinue(switch (getAnimationState()) {
                case STATE_ATTACK -> LIMB_STATUS_1;
                case STATE_PURSUIT -> LIMB_STATUS_2;
                default -> LIMB;
            });
        }));
    }

    private RawAnimation ageAnimation() {
        boolean still = stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS;
        return switch (getAnimationState()) {
            case STATE_ATTACK -> still ? AGE_STATUS_1_STILL : AGE_STATUS_1;
            case STATE_PURSUIT -> AGE_STATUS_2;
            default -> still ? AGE_STILL : AGE;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
