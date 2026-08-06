package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public class PriReekerEntity extends PrimitiveParasiteEntity {
    // 动画状态常量
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_ATTACK_PREP = 1;
    private static final int STATUS_CHARGE_RECOVERY = 2;
    private static final int STATUS_CHARGING = 3;

    // 冲锋技能相关常量
    private static final int CHARGE_WINDUP_TICKS = 20;
    private static final int CHARGE_MAX_TICKS = 60;
    private static final float CHARGE_SPEED = 2.5F;
    private static final float CHARGE_DAMAGE = 8.0F;
    private static final double CHARGE_COLLISION_RANGE = 2.0;

    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(PriReekerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE_TICKS =
            SynchedEntityData.defineId(PriReekerEntity.class, EntityDataSerializers.INT);

    // 动画定义
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK_PREP = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation CHARGE_RECOVERY = ParasiteAnimations.loop(this, "idle.get_parasite_status_2");
    private final RawAnimation CHARGING = ParasiteAnimations.loop(this, "idle.get_parasite_status_3");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    private boolean skillCharge = true;
    private Vec3 chargeTarget;
    private int chargeStuckTicks;

    public PriReekerEntity(EntityType<? extends PriReekerEntity> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, STATUS_NORMAL);
        builder.define(CHARGE_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ChargeAttackGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(4, new ParasiteFollowGoal(this));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            int chargeTicks = entityData.get(CHARGE_TICKS);
            if (chargeTicks > 0) {
                entityData.set(CHARGE_TICKS, chargeTicks - 1);
                handleCharging();
            }

            // 粒子效果
            if (chargeTicks > 0 && chargeTicks >= CHARGE_MAX_TICKS - CHARGE_WINDUP_TICKS) {
                spawnChargeParticles();
            }
        }
    }

    private void handleCharging() {
        int chargeTicks = entityData.get(CHARGE_TICKS);
        int attacking = CHARGE_MAX_TICKS - chargeTicks;

        // 蓄力阶段
        if (attacking < CHARGE_WINDUP_TICKS) {
            setParasiteStatus(STATUS_ATTACK_PREP);
            navigation.stop();
            setDeltaMovement(Vec3.ZERO);

            if (attacking % 5 == 0) {
                playSound(ModSounds.RUPTER_LIVING.get(), 0.5F, 2.0F + random.nextFloat() * 0.8F);
            }
            return;
        }

        // 冲锋启动
        if (attacking == CHARGE_WINDUP_TICKS) {
            setParasiteStatus(STATUS_CHARGING);
            LivingEntity target = getTarget();
            if (target != null && canChargeTo(target)) {
                chargeTarget = target.position();
                Vec3 direction = chargeTarget.subtract(position()).normalize();
                setDeltaMovement(direction.scale(CHARGE_SPEED));
            } else {
                endCharge();
                return;
            }
        }

        // 冲锋中
        if (attacking >= CHARGE_WINDUP_TICKS && attacking < CHARGE_MAX_TICKS) {
            setParasiteStatus(STATUS_CHARGING);

            // 碰撞检测
            AABB collisionBox = getBoundingBox().inflate(CHARGE_COLLISION_RANGE);
            for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, collisionBox,
                    this::isValidParasiteTarget)) {
                if (entity.hurt(damageSources().mobAttack(this), CHARGE_DAMAGE)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), this);
                    entity.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0), this);
                    Vec3 knockback = entity.position().subtract(position()).normalize().scale(1.5);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 0.5, knockback.z));
                }
            }

            // 检测卡住
            if (getDeltaMovement().horizontalDistanceSqr() < 0.01) {
                chargeStuckTicks++;
                if (chargeStuckTicks > 10) {
                    endCharge();
                }
            } else {
                chargeStuckTicks = 0;
            }

            // 空中减速
            if (!onGround()) {
                setDeltaMovement(getDeltaMovement().scale(0.7));
            }
        }

        // 超时或完成
        if (attacking >= CHARGE_MAX_TICKS) {
            endCharge();
        }
    }

    private boolean canChargeTo(LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }

        double heightDiff = target.getY() - getY();
        if (Math.abs(heightDiff) > 3.0) {
            return false;
        }

        return onGround() || isInWater();
    }

    private void endCharge() {
        entityData.set(CHARGE_TICKS, 0);
        setParasiteStatus(STATUS_CHARGE_RECOVERY);
        skillCharge = true;
        chargeTarget = null;
        chargeStuckTicks = 0;
    }

    private void spawnChargeParticles() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    getX(), getY() + 0.5, getZ(),
                    3, 0.3, 0.3, 0.3, 0.02);
        }
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            triggerAnim("attack_controller", "attack");
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), this);
                living.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0), this);
            }
        }
        return hurt;
    }

    @Override
    public void travel(Vec3 travelVector) {
        int chargeTicks = entityData.get(CHARGE_TICKS);
        int attacking = CHARGE_MAX_TICKS - chargeTicks;

        // 蓄力时冻结
        if (chargeTicks > 0 && attacking < CHARGE_WINDUP_TICKS) {
            super.travel(Vec3.ZERO);
            return;
        }

        super.travel(travelVector);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ParasiteStatus", getParasiteStatus());
        tag.putInt("ChargeTicks", entityData.get(CHARGE_TICKS));
        tag.putBoolean("SkillCharge", skillCharge);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setParasiteStatus(tag.getInt("ParasiteStatus"));
        entityData.set(CHARGE_TICKS, tag.getInt("ChargeTicks"));
        skillCharge = tag.getBoolean("SkillCharge");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.RUPTER_LIVING.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RUPTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RUPTER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.RUPTER_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    private <T extends PriReekerEntity> PlayState movementAnimation(AnimationState<T> state) {
        int status = getParasiteStatus();

        // 冲锋恢复动画
        if (status == STATUS_CHARGE_RECOVERY) {
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                setParasiteStatus(STATUS_NORMAL);
                return state.setAndContinue(IDLE);
            }
            return state.setAndContinue(CHARGE_RECOVERY);
        }

        // 冲锋动画
        if (status == STATUS_CHARGING) {
            return state.setAndContinue(CHARGING);
        }

        // 攻击准备动画
        if (status == STATUS_ATTACK_PREP) {
            return state.setAndContinue(ATTACK_PREP);
        }

        // 常规移动动画
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(IDLE);
        }

        return state.setAndContinue(WALK);
    }

    private final class ChargeAttackGoal extends Goal {
        private static final int CHARGE_COOLDOWN = 100;
        private int cooldown;

        private ChargeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!skillCharge || cooldown > 0) {
                return false;
            }

            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }

            double distanceSq = distanceToSqr(target);
            return distanceSq > 16.0 && distanceSq < 256.0 && canChargeTo(target);
        }

        @Override
        public boolean canContinueToUse() {
            return entityData.get(CHARGE_TICKS) > 0;
        }

        @Override
        public void start() {
            entityData.set(CHARGE_TICKS, CHARGE_MAX_TICKS);
            setParasiteStatus(STATUS_ATTACK_PREP);
            skillCharge = false;
            cooldown = CHARGE_COOLDOWN;
        }

        @Override
        public void stop() {
            if (entityData.get(CHARGE_TICKS) == 0) {
                setParasiteStatus(STATUS_NORMAL);
            }
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
        }
    }
}
