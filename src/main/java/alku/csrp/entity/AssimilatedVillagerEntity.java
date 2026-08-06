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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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
 * AssimilatedVillager (EntityInfVillager) - 被感染的村民
 * 具有正常、攻击、游泳和融化状态的人形寄生体
 */
public final class AssimilatedVillagerEntity extends Monster implements GeoEntity, Parasite {
    private static final String PARASITE_STATUS_NBT_KEY = "parasite_status";
    private static final String MELT_HEIGHT_NBT_KEY = "melt_height";
    private static final String MELT_SCALE_NBT_KEY = "melt_scale";
    private static final String MELT_TIMER_NBT_KEY = "melt_timer";

    // 状态定义
    private static final int STATUS_NORMAL = 0;   // 正常状态
    private static final int STATUS_ATTACK = 1;   // 攻击状态
    private static final int STATUS_SWIMMING = 2; // 游泳状态

    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(AssimilatedVillagerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MELT_HEIGHT =
            SynchedEntityData.defineId(AssimilatedVillagerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MELT_SCALE =
            SynchedEntityData.defineId(AssimilatedVillagerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_MELTING =
            SynchedEntityData.defineId(AssimilatedVillagerEntity.class, EntityDataSerializers.BOOLEAN);

    // 动画定义 - 根据原模组的状态系统
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation ATTACK_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_1");
    private final RawAnimation SWIM_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_2");
    private final RawAnimation SWIM_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_2");
    private final RawAnimation MELTING = ParasiteAnimations.loop(this, "idle.is_melting_1");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    private int meltTimer = 0;

    public AssimilatedVillagerEntity(EntityType<? extends AssimilatedVillagerEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public static boolean checkAssimilatedVillagerSpawnRules(EntityType<? extends Monster> type,
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
        builder.define(PARASITE_STATUS, STATUS_NORMAL);
        builder.define(MELT_HEIGHT, 1.0F);
        builder.define(MELT_SCALE, 1.0F);
        builder.define(IS_MELTING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
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

        // 融化状态处理
        if (isMelting()) {
            handleMelting();
        }
    }

    private void updateStatusMachine() {
        int currentStatus = getParasiteStatus();

        // 融化状态不可逆
        if (isMelting()) {
            return;
        }

        // 检查是否应该进入融化状态
        float healthPercent = getHealth() / getMaxHealth();
        if (healthPercent <= 0.15F && !isMelting()) {
            setMelting(true);
            return;
        }

        // 游泳状态检测
        if (isInWater() && !onGround()) {
            if (currentStatus != STATUS_SWIMMING) {
                setParasiteStatus(STATUS_SWIMMING);
            }
            return;
        }

        // 攻击状态检测
        LivingEntity target = getTarget();
        if (target != null && distanceToSqr(target) < 9.0) { // 3格内
            if (currentStatus != STATUS_ATTACK) {
                setParasiteStatus(STATUS_ATTACK);
            }
            return;
        }

        // 返回正常状态
        if (currentStatus != STATUS_NORMAL) {
            setParasiteStatus(STATUS_NORMAL);
        }
    }

    private void handleMelting() {
        meltTimer++;

        // 高度变化速率：-0.01F/tick
        float currentHeight = entityData.get(MELT_HEIGHT);
        if (currentHeight > 0.7F) {
            entityData.set(MELT_HEIGHT, Math.max(0.7F, currentHeight - 0.01F));
        }

        // 模型缩放速率：-0.005F/tick
        float currentScale = entityData.get(MELT_SCALE);
        if (currentScale > 0.5F) {
            entityData.set(MELT_SCALE, Math.max(0.5F, currentScale - 0.005F));
        }

        // 每20 tick播放音效和生成粒子
        if (meltTimer % 20 == 0) {
            playSound(ParasiteSoundProfiles.ambient(this), 1.0F, 0.8F);

            // TODO: 粒子效果 GCLOUD (127, 106, 0) 和 (127, 0, 0)
            // 需要在客户端实现粒子生成
        }

        // 完成融化后转换为其他实体 (EntityLesh)
        // TODO: 实现转换逻辑
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);

        if (hit) {
            triggerAnim("attack_controller", "attack");

            if (entity instanceof LivingEntity living) {
                // 应用感染
                InfectionMechanics.applyCoth(living, this);

                // 攻击状态下的额外效果
                if (getParasiteStatus() == STATUS_ATTACK) {
                    if (random.nextFloat() < 0.2F) {
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), this);
                    }
                    if (random.nextFloat() < 0.15F) {
                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), this);
                    }
                }
            }
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (hurt && !level().isClientSide) {
            // 受伤时检查是否应该进入融化状态
            float healthPercent = getHealth() / getMaxHealth();
            if (healthPercent <= 0.15F && !isMelting()) {
                setMelting(true);
            }
        }

        return hurt;
    }

    private void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public boolean isMelting() {
        return entityData.get(IS_MELTING);
    }

    private void setMelting(boolean melting) {
        entityData.set(IS_MELTING, melting);
    }

    public float getMeltHeight() {
        return entityData.get(MELT_HEIGHT);
    }

    public float getMeltScale() {
        return entityData.get(MELT_SCALE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(PARASITE_STATUS_NBT_KEY, getParasiteStatus());
        tag.putFloat(MELT_HEIGHT_NBT_KEY, getMeltHeight());
        tag.putFloat(MELT_SCALE_NBT_KEY, getMeltScale());
        tag.putBoolean("is_melting", isMelting());
        tag.putInt(MELT_TIMER_NBT_KEY, meltTimer);
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
        if (tag.contains(MELT_SCALE_NBT_KEY)) {
            entityData.set(MELT_SCALE, tag.getFloat(MELT_SCALE_NBT_KEY));
        }
        if (tag.contains("is_melting")) {
            setMelting(tag.getBoolean("is_melting"));
        }
        if (tag.contains(MELT_TIMER_NBT_KEY)) {
            meltTimer = tag.getInt(MELT_TIMER_NBT_KEY);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    private <T extends AssimilatedVillagerEntity> PlayState movementAnimation(AnimationState<T> state) {
        // 融化状态优先级最高
        if (isMelting()) {
            return state.setAndContinue(MELTING);
        }

        int status = getParasiteStatus();
        boolean moving = getDeltaMovement().horizontalDistanceSqr() >= 0.001;

        // 状态 2: 游泳动画
        if (status == STATUS_SWIMMING) {
            return state.setAndContinue(moving ? SWIM_WALK : SWIM_IDLE);
        }

        // 状态 1: 攻击动画
        if (status == STATUS_ATTACK) {
            return state.setAndContinue(moving ? ATTACK_WALK : ATTACK_IDLE);
        }

        // 状态 0: 正常闲置/行走动画
        return state.setAndContinue(moving ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
