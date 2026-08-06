package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
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
 * PriManducater (EntityHull) - 原始吞噬者
 * 四足寄生体，具有冲刺、拉拽和隐身能力
 */
public class PriManducaterEntity extends PrimitiveParasiteEntity implements GeoEntity {
    private static final String PARASITE_STATUS_NBT_KEY = "parasite_status";
    private static final String ATTACK_COOLDOWN_NBT_KEY = "attack_cooldown";
    private static final String PULLING_NBT_KEY = "pulling";
    private static final String TARGETED_ENTITY_NBT_KEY = "targeted_entity";
    private static final String STEALTH_TIMER_NBT_KEY = "stealth_timer";

    private static final double SPRINT_DISTANCE_SQ = 64.0; // 8.0^2
    private static final int PULL_MAX_TICKS = 200;
    private static final double PULL_MAX_DISTANCE_SQ = 81.0; // 9.0^2

    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(PriManducaterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_COOLDOWN =
            SynchedEntityData.defineId(PriManducaterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PULLING_COUNTER =
            SynchedEntityData.defineId(PriManducaterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STILL_ANI =
            SynchedEntityData.defineId(PriManducaterEntity.class, EntityDataSerializers.BOOLEAN);

    // 动画定义
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK_PREPARE = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation SPRINT = ParasiteAnimations.loop(this, "walk.get_parasite_status_2");
    private final RawAnimation PULL = ParasiteAnimations.loop(this, "walk.get_parasite_status_3");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private LivingEntity targetedEntity;
    private int stealthTimer;

    public PriManducaterEntity(EntityType<? extends PriManducaterEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.65)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    public static boolean checkPriManducaterSpawnRules(EntityType<? extends Monster> type,
                                                        ServerLevelAccessor level,
                                                        MobSpawnType spawnType,
                                                        BlockPos pos,
                                                        RandomSource random) {
        int phase = Config.evolutionPhase(level.getLevel());
        return phase >= 1 && phase <= 7
                && Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, 0);
        builder.define(ATTACK_COOLDOWN, 0);
        builder.define(PULLING_COUNTER, 0);
        builder.define(STILL_ANI, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SprintGoal());
        goalSelector.addGoal(2, new PullGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false, this::canTargetEntity));
    }

    private boolean canTargetEntity(LivingEntity entity) {
        return entity != this && entity.isAlive() && !(entity instanceof Parasite);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        // 攻击冷却
        int cooldown = entityData.get(ATTACK_COOLDOWN);
        if (cooldown > 0) {
            entityData.set(ATTACK_COOLDOWN, cooldown - 1);
        }

        // 更新状态机
        updateStatusMachine();

        // 拉拽逻辑
        updatePulling();

        // 隐身逻辑
        updateStealth();
    }

    private void updateStatusMachine() {
        int currentStatus = entityData.get(PARASITE_STATUS);
        LivingEntity target = getTarget();
        int cooldown = entityData.get(ATTACK_COOLDOWN);
        int pulling = entityData.get(PULLING_COUNTER);

        // Status 3: Pull - 拉拽状态
        if (currentStatus == 3) {
            if (pulling > PULL_MAX_TICKS || targetedEntity == null || !targetedEntity.isAlive()
                    || distanceToSqr(targetedEntity) > PULL_MAX_DISTANCE_SQ) {
                setParasiteStatus(0);
                entityData.set(PULLING_COUNTER, 0);
                targetedEntity = null;
            }
            return;
        }

        // Status 2: Sprint - 冲刺状态
        if (currentStatus == 2) {
            if (target == null || distanceToSqr(target) <= SPRINT_DISTANCE_SQ || cooldown > 0) {
                setParasiteStatus(1); // 转到准备状态
            }
            return;
        }

        // Status 1: Attack Prepare - 攻击准备
        if (currentStatus == 1) {
            if (target == null) {
                setParasiteStatus(0);
            }
            return;
        }

        // Status 0: Idle/Walk - 空闲/行走
        if (target != null && distanceToSqr(target) > SPRINT_DISTANCE_SQ
                && cooldown == 0 && canSprint()) {
            setParasiteStatus(2); // 转到冲刺状态
        }
    }

    private void updatePulling() {
        int pulling = entityData.get(PULLING_COUNTER);
        if (pulling > 0) {
            entityData.set(PULLING_COUNTER, pulling + 1);

            if (targetedEntity != null && targetedEntity.isAlive()) {
                // 拉拽目标向自己
                Vec3 direction = position().subtract(targetedEntity.position());
                if (direction.lengthSqr() > 0.01) {
                    direction = direction.normalize().scale(0.15);
                    targetedEntity.setDeltaMovement(
                        targetedEntity.getDeltaMovement().add(direction.x, 0.05, direction.z)
                    );
                }

                // 持续施加效果
                if (tickCount % 20 == 0) {
                    targetedEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), this);
                    targetedEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), this);
                }
            }
        }
    }

    private void updateStealth() {
        double healthPercent = getHealth() / getMaxHealth();
        // 配置值待定，暂时使用 0.75 和 200 ticks
        if (healthPercent >= 0.75 && getTarget() == null) {
            stealthTimer++;
            if (stealthTimer > 200) {
                // 进入隐身状态 - 通过渲染层处理透明度
            }
        } else {
            stealthTimer = 0;
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);

        if (hit && entity instanceof LivingEntity living) {
            triggerAnim("attack_controller", "attack");

            // 应用效果
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
            if (random.nextFloat() < 0.20F) {
                living.addEffect(new MobEffectInstance(ModMobEffects.COTH, 300, 0), this);
            }

            // 触发拉拽
            if (canPull(living)) {
                targetedEntity = living;
                entityData.set(PULLING_COUNTER, 1);
                setParasiteStatus(3);
            }

            // 设置攻击冷却
            entityData.set(ATTACK_COOLDOWN, 100);

            // 取消隐身
            stealthTimer = 0;
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 受伤取消隐身
        stealthTimer = 0;
        return super.hurt(source, amount);
    }

    @Override
    public boolean canSprint() {
        // 基因模块检查 - 暂时默认允许
        return true;
    }

    private boolean canPull(LivingEntity entity) {
        return entity != null && entity.isAlive() && !(entity instanceof Parasite);
    }

    private void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public boolean getStillAni() {
        return entityData.get(STILL_ANI);
    }

    public int getStealthTimer() {
        return stealthTimer;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(PARASITE_STATUS_NBT_KEY, getParasiteStatus());
        tag.putInt(ATTACK_COOLDOWN_NBT_KEY, entityData.get(ATTACK_COOLDOWN));
        tag.putInt(PULLING_NBT_KEY, entityData.get(PULLING_COUNTER));
        tag.putInt(STEALTH_TIMER_NBT_KEY, stealthTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(PARASITE_STATUS_NBT_KEY)) {
            setParasiteStatus(tag.getInt(PARASITE_STATUS_NBT_KEY));
        }
        if (tag.contains(ATTACK_COOLDOWN_NBT_KEY)) {
            entityData.set(ATTACK_COOLDOWN, tag.getInt(ATTACK_COOLDOWN_NBT_KEY));
        }
        if (tag.contains(PULLING_NBT_KEY)) {
            entityData.set(PULLING_COUNTER, tag.getInt(PULLING_NBT_KEY));
        }
        if (tag.contains(STEALTH_TIMER_NBT_KEY)) {
            stealthTimer = tag.getInt(STEALTH_TIMER_NBT_KEY);
        }
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_PREPARE));
    }

    private <T extends PriManducaterEntity> PlayState movementAnimation(AnimationState<T> state) {
        // 如果被定身，停止动画
        if (getStillAni()) {
            return PlayState.STOP;
        }

        int status = getParasiteStatus();

        // Status 3: Pull - 拉拽动画
        if (status == 3) {
            return state.setAndContinue(PULL);
        }

        // Status 2: Sprint - 冲刺动画
        if (status == 2) {
            return state.setAndContinue(SPRINT);
        }

        // Status 1: Attack Prepare - 攻击准备动画
        if (status == 1) {
            return state.setAndContinue(ATTACK_PREPARE);
        }

        // Status 0: Idle/Walk - 空闲或行走
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(IDLE);
        }

        return state.setAndContinue(WALK);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    /**
     * 冲刺目标 AI
     */
    private final class SprintGoal extends Goal {
        private SprintGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return getParasiteStatus() == 2 && target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && getParasiteStatus() == 2;
        }

        @Override
        public void start() {
            // 提高移动速度
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                getNavigation().moveTo(target, 1.3);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /**
     * 拉拽目标 AI
     */
    private final class PullGoal extends Goal {
        private PullGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getParasiteStatus() == 3 && targetedEntity != null && targetedEntity.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (targetedEntity != null) {
                getLookControl().setLookAt(targetedEntity, 30.0F, 30.0F);
                // 保持位置，不移动
                getNavigation().stop();
            }
        }
    }
}
