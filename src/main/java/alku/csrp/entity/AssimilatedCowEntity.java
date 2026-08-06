package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModMobEffects;
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
import net.minecraft.world.entity.Mob;
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

import java.util.EnumSet;

/**
 * AssimilatedCow (EntityInfCow) - 被感染的牛
 * 具有闲置、警戒、恢复、冲锋和融化状态的四足寄生体
 */
public final class AssimilatedCowEntity extends Monster implements GeoEntity, Parasite {
    private static final String PARASITE_STATUS_NBT_KEY = "parasite_status";
    private static final String CHARGE_COOLDOWN_NBT_KEY = "charge_cooldown";
    private static final String MELT_HEIGHT_NBT_KEY = "melt_height";

    // 状态定义
    private static final int STATUS_IDLE = 0;      // 闲置/行走
    private static final int STATUS_ALERT = 1;     // 警戒状态
    private static final int STATUS_RECOVERY = 2;  // 恢复状态
    private static final int STATUS_CHARGE = 3;    // 冲锋攻击
    private static final int STATUS_MELT = 6;      // 融化状态

    private static final double CHARGE_DISTANCE_SQ = 100.0; // 10.0^2

    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(AssimilatedCowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE_COOLDOWN =
            SynchedEntityData.defineId(AssimilatedCowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MELT_HEIGHT =
            SynchedEntityData.defineId(AssimilatedCowEntity.class, EntityDataSerializers.FLOAT);

    // 动画定义 - 根据原模组的状态系统
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ALERT_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation ALERT_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_1");
    private final RawAnimation RECOVERY_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_2");
    private final RawAnimation RECOVERY_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_2");
    private final RawAnimation CHARGE_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_3");
    private final RawAnimation CHARGE_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_3");
    private final RawAnimation MELT = ParasiteAnimations.loop(this, "idle.get_parasite_status_6");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public AssimilatedCowEntity(EntityType<? extends AssimilatedCowEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public static boolean checkAssimilatedCowSpawnRules(EntityType<? extends Monster> type,
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
        return 3;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, STATUS_IDLE);
        builder.define(CHARGE_COOLDOWN, 0);
        builder.define(MELT_HEIGHT, 1.4F);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ChargeGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(4, new ParasiteFollowGoal(this));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
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

        // 冷却计时器递减
        int cooldown = entityData.get(CHARGE_COOLDOWN);
        if (cooldown > 0) {
            entityData.set(CHARGE_COOLDOWN, cooldown - 1);
        }

        // 更新状态机
        updateStatusMachine();

        // 融化状态下逐渐降低高度
        if (getParasiteStatus() == STATUS_MELT) {
            float currentHeight = entityData.get(MELT_HEIGHT);
            if (currentHeight > 0.7F) {
                entityData.set(MELT_HEIGHT, Math.max(0.7F, currentHeight - 0.01F));
            }
        }
    }

    private void updateStatusMachine() {
        int currentStatus = getParasiteStatus();
        LivingEntity target = getTarget();
        int cooldown = entityData.get(CHARGE_COOLDOWN);
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

        // 状态 3: 冲锋 - 冲刺攻击
        if (currentStatus == STATUS_CHARGE) {
            if (target == null || !target.isAlive() || distanceToSqr(target) < 4.0) {
                setParasiteStatus(STATUS_RECOVERY);
                entityData.set(CHARGE_COOLDOWN, 200); // 冷却10秒
            }
            return;
        }

        // 状态 2: 恢复 - 冲锋后的恢复期
        if (currentStatus == STATUS_RECOVERY) {
            if (cooldown <= 0) {
                setParasiteStatus(target != null ? STATUS_ALERT : STATUS_IDLE);
            }
            return;
        }

        // 状态 1: 警戒 - 有目标时的准备状态
        if (currentStatus == STATUS_ALERT) {
            if (target == null) {
                setParasiteStatus(STATUS_IDLE);
            } else if (distanceToSqr(target) > CHARGE_DISTANCE_SQ && cooldown == 0 && canCharge()) {
                setParasiteStatus(STATUS_CHARGE);
            }
            return;
        }

        // 状态 0: 闲置 - 默认状态
        if (target != null) {
            setParasiteStatus(STATUS_ALERT);
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);

        if (hit) {
            triggerAnim("attack_controller", "attack");

            if (entity instanceof LivingEntity living) {
                // 应用感染和效果
                InfectionMechanics.applyCoth(living, this);

                if (random.nextFloat() < 0.15F) {
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), this);
                }

                // 恐惧效果 - 冲锋状态下造成高伤害时应用恐惧
                if (getParasiteStatus() == STATUS_CHARGE) {
                    float healthBefore = ParasiteCombatEffects.healthWithAbsorption(living);
                    ParasiteCombatEffects.applyFearFromDamage(living, healthBefore, this);
                }
            }
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (hurt && !level().isClientSide) {
            // 受伤时有概率进入恢复状态
            if (getParasiteStatus() == STATUS_CHARGE && random.nextFloat() < 0.3F) {
                setParasiteStatus(STATUS_RECOVERY);
                entityData.set(CHARGE_COOLDOWN, 100);
            }
        }

        return hurt;
    }

    private boolean canCharge() {
        // 基本检查 - 可扩展为基因模块检查
        return true;
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
        tag.putInt(CHARGE_COOLDOWN_NBT_KEY, entityData.get(CHARGE_COOLDOWN));
        tag.putFloat(MELT_HEIGHT_NBT_KEY, getMeltHeight());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(PARASITE_STATUS_NBT_KEY)) {
            setParasiteStatus(tag.getInt(PARASITE_STATUS_NBT_KEY));
        }
        if (tag.contains(CHARGE_COOLDOWN_NBT_KEY)) {
            entityData.set(CHARGE_COOLDOWN, tag.getInt(CHARGE_COOLDOWN_NBT_KEY));
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

    private <T extends AssimilatedCowEntity> PlayState movementAnimation(AnimationState<T> state) {
        int status = getParasiteStatus();
        boolean moving = getDeltaMovement().horizontalDistanceSqr() >= 0.001;

        // 状态 6: 融化动画
        if (status == STATUS_MELT) {
            return state.setAndContinue(MELT);
        }

        // 状态 3: 冲锋动画
        if (status == STATUS_CHARGE) {
            return state.setAndContinue(moving ? CHARGE_WALK : CHARGE_IDLE);
        }

        // 状态 2: 恢复动画
        if (status == STATUS_RECOVERY) {
            return state.setAndContinue(moving ? RECOVERY_WALK : RECOVERY_IDLE);
        }

        // 状态 1: 警戒动画
        if (status == STATUS_ALERT) {
            return state.setAndContinue(moving ? ALERT_WALK : ALERT_IDLE);
        }

        // 状态 0: 闲置/行走动画
        return state.setAndContinue(moving ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    /**
     * 冲锋攻击 AI
     */
    private final class ChargeGoal extends Goal {
        private ChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return getParasiteStatus() == STATUS_CHARGE && target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && getParasiteStatus() == STATUS_CHARGE;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                // 冲锋时提高速度
                getNavigation().moveTo(target, 1.5);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}
