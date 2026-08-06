package alku.csrp.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Legacy hijacked golem's wind-up charge and close-range control. */
public final class HiGolemEntity extends HijackedParasiteEntity {
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(
            HiGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            HiGolemEntity.class, EntityDataSerializers.INT);

    // 状态 0 - 普通移动状态
    private final RawAnimation idleAnimation = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation walkAnimation = ParasiteAnimations.loop(this, "walk");

    // 状态 1 - 攻击准备状态
    private final RawAnimation idleStatus1Animation = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation walkStatus1Animation = ParasiteAnimations.loop(this, "walk.get_parasite_status_1");

    // 状态 2 - 蓄力状态
    private final RawAnimation idleStatus2Animation = ParasiteAnimations.loop(this, "idle.get_parasite_status_2");
    private final RawAnimation walkStatus2Animation = ParasiteAnimations.loop(this, "walk.get_parasite_status_2");

    // 状态 3 - 冲锋状态
    private final RawAnimation chargeIdleAnimation = ParasiteAnimations.loop(this, "idle.get_parasite_status_3");
    private final RawAnimation chargeWalkAnimation = ParasiteAnimations.loop(this, "walk.get_parasite_status_3");

    // 攻击动画
    private final RawAnimation attackAnimation = ParasiteAnimations.play(this, "attack");

    private int chargeCooldown;

    public HiGolemEntity(EntityType<? extends HiGolemEntity> type, Level level) {
        super(type, level, 60);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return HijackedParasiteEntity.createAttributes(150.0D, 16.0D, 40.0D, 0.8D, 0.2725D, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING, false);
        builder.define(PARASITE_STATUS, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new GolemChargeGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.5D, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && chargeCooldown > 0) {
            chargeCooldown--;
        }

        // 更新状态机
        updateParasiteStatus();
    }

    private void updateParasiteStatus() {
        if (isCharging()) {
            setParasiteStatus(3); // 冲锋状态
        } else if (getTarget() != null) {
            // 根据目标距离和攻击状态决定状态
            double distSqr = distanceToSqr(getTarget());
            if (distSqr < 9.0D && isAggressive()) {
                setParasiteStatus(1); // 攻击准备状态
            } else if (distSqr >= 9.0D && distSqr < 576.0D) {
                setParasiteStatus(2); // 蓄力状态
            } else {
                setParasiteStatus(0); // 普通移动状态
            }
        } else {
            setParasiteStatus(0); // 普通移动状态
        }
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    public boolean isCharging() {
        return entityData.get(CHARGING);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", attackAnimation));
    }

    private PlayState movementAnimation(AnimationState<HiGolemEntity> state) {
        boolean isMoving = getDeltaMovement().horizontalDistanceSqr() >= 0.001;
        int status = getParasiteStatus();

        switch (status) {
            case 1: // 攻击准备状态
                return state.setAndContinue(isMoving ? walkStatus1Animation : idleStatus1Animation);
            case 2: // 蓄力状态
                return state.setAndContinue(isMoving ? walkStatus2Animation : idleStatus2Animation);
            case 3: // 冲锋状态
                return state.setAndContinue(isMoving ? chargeWalkAnimation : chargeIdleAnimation);
            default: // 状态 0 - 普通移动状态
                return state.setAndContinue(isMoving ? walkAnimation : idleAnimation);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("charge_cooldown", chargeCooldown);
        tag.putInt("parasite_status", getParasiteStatus());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        chargeCooldown = tag.getInt("charge_cooldown");
        setParasiteStatus(tag.getInt("parasite_status"));
    }

    private final class GolemChargeGoal extends Goal {
        private final Set<UUID> struckTargets = new HashSet<>();
        private int windupTicks;
        private int chargeTicks;

        private GolemChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return chargeCooldown <= 0 && target != null && target.isAlive() && onGround()
                    && hasLineOfSight(target) && distanceToSqr(target) >= 9.0D
                    && distanceToSqr(target) < 576.0D && random.nextInt(100) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && chargeTicks < 40;
        }

        @Override
        public void start() {
            windupTicks = 20;
            chargeTicks = 0;
            struckTargets.clear();
            chargeCooldown = 120;
            entityData.set(CHARGING, true);
            triggerAttackAnimation();
            getNavigation().stop();
        }

        @Override
        public void stop() {
            entityData.set(CHARGING, false);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (windupTicks-- > 0) {
                getNavigation().stop();
                return;
            }

            Vec3 direction = target.position().subtract(position());
            if (direction.lengthSqr() < 0.001D) {
                return;
            }
            direction = direction.normalize();
            getMoveControl().setWantedPosition(getX() + direction.x * 12.0D, getY(),
                    getZ() + direction.z * 12.0D, 1.8D);
            setDeltaMovement(getDeltaMovement().multiply(0.25D, 0.75D, 0.25D)
                    .add(direction.x * 0.38D, 0.0D, direction.z * 0.38D));
            strikeTargets(direction);
            chargeTicks++;
        }

        private void strikeTargets(Vec3 direction) {
            float damage = (float) Math.max(1.0D, getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5D);
            DragonEggAssimilationEntity.assimilateDragonEggs(level(),
                    getBoundingBox().inflate(1.5D, 0.75D, 1.5D));
            for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(1.5D, 0.75D, 1.5D), HiGolemEntity.this::isValidParasiteTarget)) {
                if (!struckTargets.add(victim.getUUID())) {
                    continue;
                }
                victim.hurt(damageSources().mobAttack(HiGolemEntity.this), damage);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), HiGolemEntity.this);
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1), HiGolemEntity.this);
                victim.push(direction.x * 0.85D, 0.35D, direction.z * 0.85D);
            }
        }
    }
}
