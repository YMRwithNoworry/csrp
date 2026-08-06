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
 * AssimilatedPig (EntityInfPig) - 被感染的猪
 * 具有正常行走、攻击、潜行和融化状态的四足寄生体
 */
public final class AssimilatedPigEntity extends Monster implements GeoEntity, Parasite {
    private static final String PARASITE_STATUS_NBT_KEY = "parasite_status";
    private static final String MELT_HEIGHT_NBT_KEY = "melt_height";
    private static final String MELT_TIMER_NBT_KEY = "melt_timer";

    // 状态定义 - 根据原模组 EntityInfPig
    private static final int STATUS_IDLE = 0;      // 正常行走
    private static final int STATUS_ATTACK = 1;    // 攻击状态
    private static final int STATUS_SNEAK = 2;     // 潜行/爬行
    private static final int STATUS_MELT = 6;      // 融化状态

    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(AssimilatedPigEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MELT_HEIGHT =
            SynchedEntityData.defineId(AssimilatedPigEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MELT_TIMER =
            SynchedEntityData.defineId(AssimilatedPigEntity.class, EntityDataSerializers.INT);

    // 动画定义 - 根据原模组的状态系统
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation ATTACK_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_1");
    private final RawAnimation SNEAK_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_2");
    private final RawAnimation SNEAK_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_2");
    private final RawAnimation MELT = ParasiteAnimations.loop(this, "idle.get_parasite_status_6");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public AssimilatedPigEntity(EntityType<? extends AssimilatedPigEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0)
                .add(Attributes.ARMOR, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public static boolean checkAssimilatedPigSpawnRules(EntityType<? extends Monster> type,
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
        builder.define(MELT_HEIGHT, 1.0F);
        builder.define(MELT_TIMER, 0);
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
        if (getParasiteStatus() == STATUS_MELT) {
            int timer = entityData.get(MELT_TIMER);
            entityData.set(MELT_TIMER, timer + 1);

            // 逐渐降低高度
            float currentHeight = entityData.get(MELT_HEIGHT);
            if (currentHeight > 0.7F) {
                entityData.set(MELT_HEIGHT, Math.max(0.7F, currentHeight - 0.01F));
            }

            // 融化超过25 tick后死亡（转换为Lesh实体的逻辑可在此添加）
            if (timer >= 25 || currentHeight <= 0.7F) {
                // TODO: 生成 Lesh 实体
                this.discard();
            }
        }
    }

    private void updateStatusMachine() {
        int currentStatus = getParasiteStatus();
        LivingEntity target = getTarget();
        float healthPercent = getHealth() / getMaxHealth();

        // 状态 6: 融化 - 低血量时进入
        if (healthPercent <= 0.15F && currentStatus != STATUS_MELT) {
            setParasiteStatus(STATUS_MELT);
            return;
        }

        // 融化状态不可逆
        if (currentStatus == STATUS_MELT) {
            return;
        }

        // 状态 2: 潜行 - 受伤时有概率进入潜行状态
        if (currentStatus == STATUS_SNEAK) {
            if (target == null || healthPercent > 0.5F) {
                setParasiteStatus(STATUS_IDLE);
            }
            return;
        }

        // 状态 1: 攻击 - 有目标且准备攻击
        if (currentStatus == STATUS_ATTACK) {
            if (target == null) {
                setParasiteStatus(STATUS_IDLE);
            }
            return;
        }

        // 状态 0: 闲置 - 默认状态
        if (target != null) {
            setParasiteStatus(STATUS_ATTACK);
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

                // 攻击状态下有概率应用虚弱效果
                if (getParasiteStatus() == STATUS_ATTACK && random.nextFloat() < 0.1F) {
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), this);
                }
            }
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (hurt && !level().isClientSide) {
            float healthPercent = getHealth() / getMaxHealth();

            // 受伤时有概率进入潜行状态
            if (healthPercent < 0.5F && getParasiteStatus() == STATUS_IDLE && random.nextFloat() < 0.3F) {
                setParasiteStatus(STATUS_SNEAK);
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

    public float getMeltHeight() {
        return entityData.get(MELT_HEIGHT);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(PARASITE_STATUS_NBT_KEY, getParasiteStatus());
        tag.putFloat(MELT_HEIGHT_NBT_KEY, getMeltHeight());
        tag.putInt(MELT_TIMER_NBT_KEY, entityData.get(MELT_TIMER));
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
        if (tag.contains(MELT_TIMER_NBT_KEY)) {
            entityData.set(MELT_TIMER, tag.getInt(MELT_TIMER_NBT_KEY));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    private <T extends AssimilatedPigEntity> PlayState movementAnimation(AnimationState<T> state) {
        int status = getParasiteStatus();
        boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());

        // 状态 6: 融化动画
        if (status == STATUS_MELT) {
            return state.setAndContinue(MELT);
        }

        // 状态 2: 潜行动画
        if (status == STATUS_SNEAK) {
            return state.setAndContinue(moving ? SNEAK_WALK : SNEAK_IDLE);
        }

        // 状态 1: 攻击动画
        if (status == STATUS_ATTACK) {
            return state.setAndContinue(moving ? ATTACK_WALK : ATTACK_IDLE);
        }

        // 状态 0: 闲置/行走动画
        return state.setAndContinue(moving ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
