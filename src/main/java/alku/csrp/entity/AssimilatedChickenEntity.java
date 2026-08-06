package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.infection.InfectionMechanics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * AssimilatedChicken (EntityInfChicken) - 被感染的鸡
 * 小型快速寄生体，具有闲置、行走、奔跑和攻击动画
 */
public final class AssimilatedChickenEntity extends Monster implements GeoEntity, Parasite {
    private static final String PARASITE_STATUS_NBT_KEY = "parasite_status";
    private static final String MELT_HEIGHT_NBT_KEY = "melt_height";

    // 状态定义
    private static final int STATUS_IDLE = 0;      // 闲置/行走
    private static final int STATUS_MELT = 6;      // 融化状态

    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(AssimilatedChickenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MELT_HEIGHT =
            SynchedEntityData.defineId(AssimilatedChickenEntity.class, EntityDataSerializers.FLOAT);

    // 动画定义 - 基于 GeckoLib 4.x 标准
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation MELT = ParasiteAnimations.loop(this, "idle.get_parasite_status_6");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public AssimilatedChickenEntity(EntityType<? extends AssimilatedChickenEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public static boolean checkAssimilatedChickenSpawnRules(EntityType<? extends Monster> type,
                                                             ServerLevelAccessor level,
                                                             MobSpawnType spawnType,
                                                             BlockPos pos,
                                                             RandomSource random) {
        int phase = Config.evolutionPhase(level.getLevel());
        return phase >= 1 && phase <= 5
                && Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, STATUS_IDLE);
        builder.define(MELT_HEIGHT, 0.7F);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, false));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(3, new ParasiteFollowGoal(this));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false, this::canTargetEntity));
    }

    private boolean canTargetEntity(LivingEntity entity) {
        return entity != this && entity.isAlive() && !(entity instanceof Parasite);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ParasiteSoundProfiles.ambient(this);
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

        if (level().isClientSide) {
            return;
        }

        // 更新状态机
        updateStatusMachine();

        // 融化状态下逐渐降低高度
        if (getParasiteStatus() == STATUS_MELT) {
            float currentHeight = entityData.get(MELT_HEIGHT);
            if (currentHeight > 0.4F) {
                entityData.set(MELT_HEIGHT, Math.max(0.4F, currentHeight - 0.01F));
            }
        }
    }

    private void updateStatusMachine() {
        int currentStatus = getParasiteStatus();
        float healthPercent = getHealth() / getMaxHealth();

        // 状态 6: 融化 - 低血量时进入
        if (healthPercent <= 0.2F && currentStatus != STATUS_MELT) {
            setParasiteStatus(STATUS_MELT);
            return;
        }

        // 融化状态不可逆
        if (currentStatus == STATUS_MELT) {
            return;
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);

        if (hit) {
            triggerAnim("attack_controller", "attack");

            if (entity instanceof LivingEntity living) {
                // 应用感染
                InfectionMechanics.applyCoth(living, this);
            }
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    private void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public float getMeltHeight() {
        return entityData.get(MELT_HEIGHT);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(PARASITE_STATUS_NBT_KEY, getParasiteStatus());
        tag.putFloat(MELT_HEIGHT_NBT_KEY, getMeltHeight());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(PARASITE_STATUS_NBT_KEY)) {
            setParasiteStatus(tag.getInt(PARASITE_STATUS_NBT_KEY));
        }
        if (tag.contains(MELT_HEIGHT_NBT_KEY)) {
            entityData.set(MELT_HEIGHT, tag.getFloat(MELT_HEIGHT_NBT_KEY));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    private <T extends AssimilatedChickenEntity> PlayState movementAnimation(AnimationState<T> state) {
        int status = getParasiteStatus();
        boolean moving = getDeltaMovement().horizontalDistanceSqr() >= 0.001;

        // 状态 6: 融化动画
        if (status == STATUS_MELT) {
            return state.setAndContinue(MELT);
        }

        // 根据移动速度选择动画
        if (moving) {
            // 快速移动时播放奔跑动画
            double speedSqr = getDeltaMovement().horizontalDistanceSqr();
            if (speedSqr >= 0.01) {
                return state.setAndContinue(RUN);
            }
            // 慢速移动时播放行走动画
            return state.setAndContinue(WALK);
        }

        // 静止时播放闲置动画
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
