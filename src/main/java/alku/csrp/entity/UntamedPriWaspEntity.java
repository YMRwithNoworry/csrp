package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * UntamedPriWasp - 未驯化的原始黄蜂
 * 原版 EntityRanrac (ID: 38) 的移植
 * 特性：
 * - 攀爬能力（可爬墙）
 * - 拉拽技能（发射拉拽弹）
 * - 多状态动画系统
 */
public class UntamedPriWaspEntity extends Monster implements GeoEntity, Parasite, PullingBallOwner {
    private static final String PARASITE_STATUS_NBT_KEY = "parasite_status";
    private static final String PULL_COOLDOWN_NBT_KEY = "pull_cooldown";
    private static final String PULL_COUNT_NBT_KEY = "pull_count";
    private static final String SKILL_BORDER_NBT_KEY = "skill_border";

    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(UntamedPriWaspEntity.class, EntityDataSerializers.INT);

    // 动画定义 - 根据原版 ModelRanrac 的状态系统
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK_PREPARE = ParasiteAnimations.loop(this, "idle.status_1_attack_prepare");
    private final RawAnimation CHASE = ParasiteAnimations.loop(this, "idle.status_2_chase");
    private final RawAnimation PULLING_SKILL = ParasiteAnimations.loop(this, "idle.status_3_pulling");
    private final RawAnimation SKILL_CAST = ParasiteAnimations.loop(this, "idle.status_11_skill_cast");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    // 技能系统
    private int pullCooldown;
    private int pullCount;
    private int skillBorder; // 技能发射计数器

    public UntamedPriWaspEntity(EntityType<? extends UntamedPriWaspEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    public static boolean checkUntamedPriWaspSpawnRules(EntityType<? extends Monster> type,
                                                         ServerLevelAccessor level,
                                                         MobSpawnType spawnType,
                                                         BlockPos pos,
                                                         RandomSource random) {
        int phase = Config.evolutionPhase(level.getLevel());
        return phase >= 2 && phase <= 7
                && Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 3;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, level()));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false, this::canTargetEntity));
    }

    private boolean canTargetEntity(LivingEntity entity) {
        return entity != this && entity.isAlive() && !(entity instanceof Parasite);
    }

    @Override
    public void tick() {
        super.tick();

        // 攀爬能力
        if (!level().isClientSide) {
            setClimbing(horizontalCollision);
        }

        if (!level().isClientSide) {
            updateParasiteStatus();
            updatePullCooldown();
            handleSkillExecution();
        }
    }

    private void updateParasiteStatus() {
        LivingEntity target = getTarget();

        // Status 11: 技能施放状态（发射后的恢复期）
        if (getParasiteStatus() == 11) {
            // 在 handleSkillExecution 中处理状态切换
            return;
        }

        // Status 3: 拉拽技能准备状态
        if (canUsePullSkill(target)) {
            setParasiteStatus(3);
            getNavigation().stop(); // 停止导航
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F); // 朝向目标
            }
            return;
        }

        // Status 2: 快速追击状态
        if (target != null && distanceToSqr(target) > 4.0 && distanceToSqr(target) < 49.0 && hasLineOfSight(target)) {
            setParasiteStatus(2);
            return;
        }

        // Status 1: 攻击准备状态
        if (target != null && distanceToSqr(target) <= 4.0 && hasLineOfSight(target)) {
            setParasiteStatus(1);
            return;
        }

        // Status 0: 默认移动状态
        setParasiteStatus(0);
    }

    private boolean canUsePullSkill(LivingEntity target) {
        return target != null
                && hasLineOfSight(target)
                && distanceToSqr(target) >= 16.0 // 至少 4 格距离
                && distanceToSqr(target) <= 144.0 // 最多 12 格距离
                && pullCooldown <= 0
                && random.nextInt(100) < 8; // 8% 触发概率
    }

    private void updatePullCooldown() {
        if (pullCooldown > 0) {
            pullCooldown--;
        }
    }

    private void handleSkillExecution() {
        int status = getParasiteStatus();

        // 技能施放后的恢复期
        if (status == 11) {
            skillBorder++;
            if (skillBorder > 40) { // 2秒恢复期
                setParasiteStatus(0);
                skillBorder = 0;
            }
            return;
        }

        // 拉拽技能执行
        if (status != 3) {
            pullCount = 0;
            skillBorder = 0;
            return;
        }

        if (tickCount % 20 == 0) { // 每秒发射一次
            LivingEntity target = getTarget();
            if (target != null && hasLineOfSight(target)) {
                pullCount++;
                executePullSkill(target);
                skillBorder++;

                if (skillBorder > 5) { // 发射 5 次后进入恢复期
                    setParasiteStatus(11);
                    pullCooldown = random.nextInt(200) + 100; // 100-300 tick 冷却 (5-15秒)
                    pullCount = 0;
                }
            } else {
                setParasiteStatus(0);
                pullCount = 0;
                skillBorder = 0;
            }
        }
    }

    private void executePullSkill(LivingEntity target) {
        if (level() instanceof ServerLevel serverLevel) {
            // 创建并发射拉拽弹丸
            PullingBallEntity pullingBall = new PullingBallEntity(
                ModEntities.PULLING_BALL.get(), level());
            pullingBall.setOwner(this);

            // 计算发射位置（从实体中心偏上）
            double offsetY = getBbHeight() * 0.7;
            pullingBall.setPos(getX(), getY() + offsetY, getZ());

            // 计算发射方向
            double dx = target.getX() - getX();
            double dy = target.getY(0.5) - (getY() + offsetY);
            double dz = target.getZ() - getZ();

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double speed = 1.2;

            pullingBall.setDeltaMovement(
                dx / distance * speed,
                dy / distance * speed,
                dz / distance * speed
            );

            level().addFreshEntity(pullingBall);

            // 播放发射音效
            playSound(ModSounds.RUPTER_STEP.get(), 1.0F, 0.8F + random.nextFloat() * 0.4F);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit) {
            triggerAnim("attack_controller", "attack");
        }
        return hit;
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    public boolean isClimbing() {
        return (entityData.get(DATA_SHARED_FLAGS_ID) & 1) != 0;
    }

    public void setClimbing(boolean climbing) {
        byte flags = entityData.get(DATA_SHARED_FLAGS_ID);
        if (climbing) {
            flags = (byte)(flags | 1);
        } else {
            flags = (byte)(flags & -2);
        }
        entityData.set(DATA_SHARED_FLAGS_ID, flags);
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(PARASITE_STATUS_NBT_KEY, getParasiteStatus());
        tag.putInt(PULL_COOLDOWN_NBT_KEY, pullCooldown);
        tag.putInt(PULL_COUNT_NBT_KEY, pullCount);
        tag.putInt(SKILL_BORDER_NBT_KEY, skillBorder);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(PARASITE_STATUS_NBT_KEY)) {
            setParasiteStatus(tag.getInt(PARASITE_STATUS_NBT_KEY));
        }
        if (tag.contains(PULL_COOLDOWN_NBT_KEY)) {
            pullCooldown = tag.getInt(PULL_COOLDOWN_NBT_KEY);
        }
        if (tag.contains(PULL_COUNT_NBT_KEY)) {
            pullCount = tag.getInt(PULL_COUNT_NBT_KEY);
        }
        if (tag.contains(SKILL_BORDER_NBT_KEY)) {
            skillBorder = tag.getInt(SKILL_BORDER_NBT_KEY);
        }
    }

    @Nullable
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.RUPTER_STEP.get(), 0.15F, 1.2F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ParasiteAnimations.play(this, "attack")));
    }

    private <T extends UntamedPriWaspEntity> PlayState movementAnimation(AnimationState<T> state) {
        int status = getParasiteStatus();

        // Status 11: 技能施放恢复期
        if (status == 11) {
            return state.setAndContinue(SKILL_CAST);
        }

        // Status 3: 拉拽技能状态
        if (status == 3) {
            return state.setAndContinue(PULLING_SKILL);
        }

        // Status 2: 快速追击状态
        if (status == 2) {
            return state.setAndContinue(CHASE);
        }

        // Status 1: 攻击准备状态
        if (status == 1) {
            return state.setAndContinue(ATTACK_PREPARE);
        }

        // Status 0: 默认移动状态
        if (!state.isMoving()) {
            return state.setAndContinue(IDLE);
        }
        return state.setAndContinue(WALK);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    // PullingBallOwner 接口实现
    @Override
    public boolean captureTarget(LivingEntity target) {
        // 捕获目标时不做特殊处理，仅返回成功
        return target != null && target.isAlive() && isValidPullTarget(target);
    }

    @Override
    public boolean isValidPullTarget(LivingEntity target) {
        // 验证目标是否有效
        return target != null
                && target.isAlive()
                && !(target instanceof Parasite)
                && target != this;
    }
}
