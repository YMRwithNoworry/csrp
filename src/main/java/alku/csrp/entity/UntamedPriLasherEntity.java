package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
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

/**
 * UntamedPriLasher (原Nogla) - 原始寄生体：鞭笞者
 * 特性：快速冲刺攻击，多状态动画系统
 */
public class UntamedPriLasherEntity extends PrimitiveParasiteEntity {
    private static final String PARASITE_STATUS_NBT_KEY = "parasite_status";
    private static final String DASH_COOLDOWN_NBT_KEY = "dash_cooldown";
    private static final String DASH_CHARGE_NBT_KEY = "dash_charge";
    private static final String DASH_DURATION_NBT_KEY = "dash_duration";
    private static final String DASH_TARGET_X_NBT_KEY = "dash_target_x";
    private static final String DASH_TARGET_Y_NBT_KEY = "dash_target_y";
    private static final String DASH_TARGET_Z_NBT_KEY = "dash_target_z";

    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(UntamedPriLasherEntity.class, EntityDataSerializers.INT);

    // 动画定义
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation CHARGING = ParasiteAnimations.loop(this, "idle.status_1");
    private final RawAnimation DASH_PREPARE = ParasiteAnimations.loop(this, "idle.status_2");
    private final RawAnimation DASHING = ParasiteAnimations.loop(this, "idle.status_3");

    // 冲刺技能参数
    private static final int DASH_CHARGE_TIME = 20; // 1秒蓄力
    private static final int DASH_DURATION_MAX = 60; // 最多3秒冲刺
    private static final double DASH_SPEED = 2.5;
    private static final float DASH_DAMAGE = 8.0F;
    private static final int DASH_COOLDOWN_MIN = 100; // 5秒
    private static final int DASH_COOLDOWN_MAX = 200; // 10秒

    private int dashCooldown;
    private int dashChargeTicks;
    private int dashDurationTicks;
    private Vec3 dashTarget;

    public UntamedPriLasherEntity(EntityType<? extends PrimitiveParasiteEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public static boolean checkSpawnRules(EntityType<? extends Monster> type,
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
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            updateParasiteStatus();
            updateDashCooldown();
            handleDashSkill();
        }
    }

    private void updateParasiteStatus() {
        LivingEntity target = getTarget();

        // Status 3: 冲刺中
        if (dashDurationTicks > 0) {
            setParasiteStatus(3);
            return;
        }

        // Status 2: 冲刺准备
        if (dashChargeTicks > 0) {
            setParasiteStatus(2);
            return;
        }

        // Status 1: 蓄力状态
        if (target != null && canStartDash(target)) {
            setParasiteStatus(1);
            return;
        }

        // Status 0: 默认状态
        setParasiteStatus(0);
    }

    private boolean canStartDash(LivingEntity target) {
        if (dashCooldown > 0 || target == null) {
            return false;
        }

        double distanceSq = distanceToSqr(target);
        return distanceSq > 16.0 && distanceSq < 196.0 // 4-14格距离
                && hasLineOfSight(target)
                && random.nextInt(100) < 15; // 15% 触发概率
    }

    private void updateDashCooldown() {
        if (dashCooldown > 0) {
            dashCooldown--;
        }
    }

    private void handleDashSkill() {
        LivingEntity target = getTarget();

        // 蓄力阶段
        if (getParasiteStatus() == 1) {
            if (dashChargeTicks == 0) {
                dashChargeTicks = 1;
                // 播放受伤音效表示蓄力开始
                playSound(ParasiteSoundProfiles.hurt(this), getSoundVolume(), getVoicePitch());
            } else {
                dashChargeTicks++;
                if (dashChargeTicks >= DASH_CHARGE_TIME) {
                    startDash(target);
                }
            }
            return;
        }

        // 冲刺准备阶段
        if (getParasiteStatus() == 2 && dashTarget != null) {
            dashDurationTicks++;
            if (dashDurationTicks >= 10) { // 0.5秒准备时间后开始冲刺
                setParasiteStatus(3);
            }
            return;
        }

        // 冲刺执行阶段
        if (getParasiteStatus() == 3 && dashTarget != null) {
            executeDash();
            dashDurationTicks++;

            // 检查结束条件
            if (dashDurationTicks >= DASH_DURATION_MAX
                    || (target != null && distanceToSqr(dashTarget) < 4.0)
                    || onGround() && getDeltaMovement().horizontalDistanceSqr() < 0.01) {
                endDash();
            }
        }
    }

    private void startDash(LivingEntity target) {
        if (target == null) {
            dashChargeTicks = 0;
            return;
        }

        dashTarget = target.position();
        dashChargeTicks = 0;
        dashDurationTicks = 0;
        setParasiteStatus(2);
    }

    private void executeDash() {
        if (dashTarget == null) {
            endDash();
            return;
        }

        Vec3 direction = dashTarget.subtract(position()).normalize();
        Vec3 dashVelocity = direction.scale(DASH_SPEED);

        // 设置冲刺速度
        setDeltaMovement(dashVelocity.x, getDeltaMovement().y, dashVelocity.z);

        // 检测并攻击路径上的实体
        if (tickCount % 5 == 0) {
            for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(1.0), this::isValidParasiteTarget)) {
                if (entity.hurt(damageSources().mobAttack(this), DASH_DAMAGE)) {
                    // 击退效果
                    double kx = entity.getX() - getX();
                    double kz = entity.getZ() - getZ();
                    double length = Math.max(0.001, Math.sqrt(kx * kx + kz * kz));
                    entity.push(kx / length * 0.6, 0.4, kz / length * 0.6);
                }
            }
        }

        // 破坏路径上的方块（可选，保持与原版一致）
        if (tickCount % 10 == 0 && random.nextFloat() < 0.3F) {
            BlockPos blockPos = blockPosition();
            BlockState state = level().getBlockState(blockPos);
            if (state.getDestroySpeed(level(), blockPos) >= 0
                    && state.getDestroySpeed(level(), blockPos) <= 0.6F) {
                level().destroyBlock(blockPos, true, this);
            }
        }
    }

    private void endDash() {
        dashTarget = null;
        dashDurationTicks = 0;
        dashChargeTicks = 0;
        dashCooldown = random.nextInt(DASH_COOLDOWN_MAX - DASH_COOLDOWN_MIN) + DASH_COOLDOWN_MIN;
        setParasiteStatus(0);
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
        tag.putInt(DASH_COOLDOWN_NBT_KEY, dashCooldown);
        tag.putInt(DASH_CHARGE_NBT_KEY, dashChargeTicks);
        tag.putInt(DASH_DURATION_NBT_KEY, dashDurationTicks);
        if (dashTarget != null) {
            tag.putDouble(DASH_TARGET_X_NBT_KEY, dashTarget.x);
            tag.putDouble(DASH_TARGET_Y_NBT_KEY, dashTarget.y);
            tag.putDouble(DASH_TARGET_Z_NBT_KEY, dashTarget.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(PARASITE_STATUS_NBT_KEY)) {
            setParasiteStatus(tag.getInt(PARASITE_STATUS_NBT_KEY));
        }
        if (tag.contains(DASH_COOLDOWN_NBT_KEY)) {
            dashCooldown = tag.getInt(DASH_COOLDOWN_NBT_KEY);
        }
        if (tag.contains(DASH_CHARGE_NBT_KEY)) {
            dashChargeTicks = tag.getInt(DASH_CHARGE_NBT_KEY);
        }
        if (tag.contains(DASH_DURATION_NBT_KEY)) {
            dashDurationTicks = tag.getInt(DASH_DURATION_NBT_KEY);
        }
        if (tag.contains(DASH_TARGET_X_NBT_KEY)) {
            dashTarget = new Vec3(
                    tag.getDouble(DASH_TARGET_X_NBT_KEY),
                    tag.getDouble(DASH_TARGET_Y_NBT_KEY),
                    tag.getDouble(DASH_TARGET_Z_NBT_KEY)
            );
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
        playSound(ModSounds.RUPTER_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    private <T extends UntamedPriLasherEntity> PlayState movementAnimation(AnimationState<T> state) {
        int status = getParasiteStatus();

        // Status 3: 冲刺中
        if (status == 3) {
            return state.setAndContinue(DASHING);
        }

        // Status 2: 冲刺准备
        if (status == 2) {
            return state.setAndContinue(DASH_PREPARE);
        }

        // Status 1: 蓄力状态
        if (status == 1) {
            return state.setAndContinue(CHARGING);
        }

        // Status 0: 默认移动状态
        if (ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(WALK);
        }
        return state.setAndContinue(IDLE);
    }
}
