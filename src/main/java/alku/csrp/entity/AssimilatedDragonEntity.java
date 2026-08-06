package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.entity.PartEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/** Assimilated Ender Dragon with removable head and wing durability driving flight and ranged combat. */
public final class AssimilatedDragonEntity extends Monster implements GeoEntity, Parasite {
    private static final float PART_HEALTH = 52.0F;
    private static final int RANGED_COOLDOWN = 40;

    // 动画定义 - 对应原模组的不同 parasiteStatus 状态
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");  // 状态 0: 空闲
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");  // 状态 0: 行走
    private final RawAnimation ATTACK_ANIM = RawAnimation.begin()
            .thenLoop("animation.sim_dragone.idle.get_parasite_status_1");  // 状态 1: 近战攻击
    private final RawAnimation SWIM = RawAnimation.begin()
            .thenLoop("animation.sim_dragone.walk.get_parasite_status_2");  // 状态 2: 游泳
    private final RawAnimation FLY = RawAnimation.begin()
            .thenLoop("animation.sim_dragone.idle.get_flying_state_1");     // 状态 3: 飞行
    private final RawAnimation BREATH_ATTACK = RawAnimation.begin()
            .thenLoop("animation.sim_dragone.idle.get_parasite_status_10"); // 状态 10: 火焰喷射
    private final RawAnimation MELEE_ATTACK = ParasiteAnimations.play(this, "attack");  // 触发式近战攻击
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            AssimilatedDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(
            AssimilatedDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HEAD_ATTACHED = SynchedEntityData.defineId(
            AssimilatedDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LEFT_WING_ATTACHED = SynchedEntityData.defineId(
            AssimilatedDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RIGHT_WING_ATTACHED = SynchedEntityData.defineId(
            AssimilatedDragonEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final DragonBodyPart headPart;
    private final DragonBodyPart leftWingPart;
    private final DragonBodyPart rightWingPart;
    private final PartEntity<?>[] bodyParts;
    private float headHealth = PART_HEALTH;
    private float leftWingHealth = PART_HEALTH;
    private float rightWingHealth = PART_HEALTH;
    private int rangedCooldown;
    private int attackStateTimer;  // 近战攻击状态计时器
    private int breathStateTimer;  // 火焰喷射状态计时器

    public AssimilatedDragonEntity(EntityType<? extends AssimilatedDragonEntity> type, Level level) {
        super(type, level);
        headPart = new DragonBodyPart(this, BodyPart.HEAD, "head", 2.2F, 2.0F);
        leftWingPart = new DragonBodyPart(this, BodyPart.LEFT_WING, "left_wing", 3.1F, 2.8F);
        rightWingPart = new DragonBodyPart(this, BodyPart.RIGHT_WING, "right_wing", 3.1F, 2.8F);
        bodyParts = new PartEntity<?>[]{headPart, leftWingPart, rightWingPart};
        moveControl = new FlyingMoveControl(this, 20, true);
        xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0D)
                .add(Attributes.ARMOR, 25.0D)
                .add(Attributes.ATTACK_DAMAGE, 30.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FLYING_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, 0);
        builder.define(FLYING, true);
        builder.define(HEAD_ATTACHED, true);
        builder.define(LEFT_WING_ATTACHED, true);
        builder.define(RIGHT_WING_ATTACHED, true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new DragonCombatGoal());
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(isFlying());
        updateBodyParts();
        updateParasiteStatus();
        if (level().isClientSide) {
            return;
        }
        if (rangedCooldown > 0) rangedCooldown--;
        if (!canFly() && isFlying()) {
            setFlying(false);
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (isFlying() && hasHead() && rangedCooldown <= 0 && hasLineOfSight(target)) {
            shootDragonBreath(target);
            rangedCooldown = RANGED_COOLDOWN;
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        LivingEntity livingTarget = entity instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(entity);
        if (hit) {
            // 设置近战攻击状态并触发攻击动画
            setParasiteStatus(1);
            attackStateTimer = 20; // 攻击动画持续约1秒 (20 ticks)
            triggerAnim("attack_controller", "attack");
        }
        if (hit && livingTarget != null) {
            ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
            InfectionMechanics.applyCoth(livingTarget, this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float applied = source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount;
        boolean hurt = super.hurt(source, applied);
        if (hurt && !level().isClientSide && random.nextInt(12) == 0 && !isFlying() && canFly()) {
            setFlying(true);
        }
        return hurt;
    }

    /** Applies weak-point damage from an external hitbox integration or a targeted gameplay hook. */
    public boolean hurtBodyPart(BodyPart part, DamageSource source, float amount) {
        if (!hurt(source, amount)) {
            return false;
        }
        switch (part) {
            case HEAD -> {
                headHealth -= amount;
                if (headHealth <= 0.0F && hasHead()) {
                    detachHead();
                }
            }
            case LEFT_WING -> {
                leftWingHealth -= amount;
                if (leftWingHealth <= 0.0F) {
                    entityData.set(LEFT_WING_ATTACHED, false);
                    setFlying(false);
                }
            }
            case RIGHT_WING -> {
                rightWingHealth -= amount;
                if (rightWingHealth <= 0.0F) {
                    entityData.set(RIGHT_WING_ATTACHED, false);
                    setFlying(false);
                }
            }
        }
        return true;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (bodyParts == null) {
            return;
        }
        for (int index = 0; index < bodyParts.length; index++) {
            bodyParts[index].setId(id + index + 1);
        }
    }

    @Override
    public PartEntity<?>[] getParts() {
        return bodyParts;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasite_status", getParasiteStatus());
        tag.putFloat("head_health", headHealth);
        tag.putFloat("left_wing_health", leftWingHealth);
        tag.putFloat("right_wing_health", rightWingHealth);
        tag.putInt("ranged_cooldown", rangedCooldown);
        tag.putInt("attack_state_timer", attackStateTimer);
        tag.putInt("breath_state_timer", breathStateTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setParasiteStatus(tag.getInt("parasite_status"));
        headHealth = tag.getFloat("head_health");
        leftWingHealth = tag.getFloat("left_wing_health");
        rightWingHealth = tag.getFloat("right_wing_health");
        rangedCooldown = tag.getInt("ranged_cooldown");
        attackStateTimer = tag.getInt("attack_state_timer");
        breathStateTimer = tag.getInt("breath_state_timer");
        entityData.set(HEAD_ATTACHED, headHealth > 0.0F);
        entityData.set(LEFT_WING_ATTACHED, leftWingHealth > 0.0F);
        entityData.set(RIGHT_WING_ATTACHED, rightWingHealth > 0.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主要移动动画控制器 - 根据 parasiteStatus 切换不同状态
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            int status = getParasiteStatus();

            // 状态 10: 火焰喷射技能状态
            if (status == 10) {
                return state.setAndContinue(BREATH_ATTACK);
            }

            // 状态 3: 飞行状态
            if (isFlying() || status == 3) {
                return state.setAndContinue(FLY);
            }

            // 状态 2: 游泳状态
            if (isInWater() || status == 2) {
                return state.setAndContinue(SWIM);
            }

            // 状态 1: 近战攻击状态 - 使用特殊的攻击动画循环
            if (status == 1) {
                return state.setAndContinue(ATTACK_ANIM);
            }

            // 状态 0: 空闲/行走
            boolean isMoving = getDeltaMovement().horizontalDistanceSqr() >= 0.001;
            return state.setAndContinue(isMoving ? WALK : IDLE);
        }));

        // 攻击动画控制器 - 可触发的近战攻击动画叠加
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP).triggerableAnim("attack", MELEE_ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    public boolean hasHead() {
        return entityData.get(HEAD_ATTACHED);
    }

    public boolean canFly() {
        return entityData.get(LEFT_WING_ATTACHED) && entityData.get(RIGHT_WING_ATTACHED);
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    private void setFlying(boolean flying) {
        entityData.set(FLYING, flying && canFly());
        setNoGravity(entityData.get(FLYING));
    }

    /**
     * 根据实体状态更新 parasiteStatus
     * 状态 0: 地面空闲/行走
     * 状态 1: 近战攻击
     * 状态 2: 游泳
     * 状态 3: 飞行
     * 状态 10: 火焰喷射技能
     */
    private void updateParasiteStatus() {
        // 更新攻击状态计时器
        if (attackStateTimer > 0) {
            attackStateTimer--;
            if (attackStateTimer == 0 && getParasiteStatus() == 1) {
                setParasiteStatus(0); // 攻击动画结束，回到普通状态
            }
        }

        // 更新火焰喷射状态计时器
        if (breathStateTimer > 0) {
            breathStateTimer--;
            if (breathStateTimer == 0 && getParasiteStatus() == 10) {
                setParasiteStatus(0); // 火焰喷射结束，回到普通状态
            }
        }

        // 如果处于特殊攻击状态，不自动切换
        int currentStatus = getParasiteStatus();
        if (currentStatus == 1 || currentStatus == 10) {
            return;
        }

        // 根据当前环境自动设置状态
        if (isFlying()) {
            setParasiteStatus(3);
        } else if (isInWater()) {
            setParasiteStatus(2);
        } else if (currentStatus != 0) {
            setParasiteStatus(0); // 回到默认地面状态
        }
    }

    private boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    private void detachHead() {
        entityData.set(HEAD_ATTACHED, false);
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AssimilatedDragonHeadEntity head = ModEntities.SIM_DRAGON_HEAD.get().create(serverLevel);
        if (head == null) {
            return;
        }
        Vec3 position = getEyePosition().add(getLookAngle().scale(1.4D));
        head.moveTo(position.x, position.y, position.z, getYRot(), getXRot());
        head.setTarget(getTarget());
        serverLevel.addFreshEntity(head);
    }

    private void updateBodyParts() {
        Vec3 look = getLookAngle();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();
        Vec3 headPosition = getEyePosition().add(look.scale(2.1D));
        headPart.setPos(headPosition.x, headPosition.y, headPosition.z);
        Vec3 leftWingPosition = position().add(side.scale(3.8D)).add(0.0D, 2.0D, 0.0D);
        Vec3 rightWingPosition = position().subtract(side.scale(3.8D)).add(0.0D, 2.0D, 0.0D);
        leftWingPart.setPos(leftWingPosition.x, leftWingPosition.y, leftWingPosition.z);
        rightWingPart.setPos(rightWingPosition.x, rightWingPosition.y, rightWingPosition.z);
    }

    private void shootDragonBreath(LivingEntity target) {
        // 设置火焰喷射状态
        setParasiteStatus(10);
        breathStateTimer = 40; // 火焰喷射动画持续约2秒 (40 ticks)

        Vec3 source = getEyePosition().add(getLookAngle().scale(1.8D));
        Vec3 direction = target.getEyePosition().subtract(source);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        direction = direction.normalize();
        Vec3 impact = source.add(direction.scale(Math.min(32.0D, Math.sqrt(distanceToSqr(target)))));
        AABB breathArea = getBoundingBox().expandTowards(direction.scale(32.0D)).inflate(2.0D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), breathArea);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                breathArea, this::isValidParasiteTarget)) {
            if (hasLineOfSight(victim)) {
                victim.hurt(damageSources().indirectMagic(this, this), 20.0F);
                victim.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 160, 0), this);
                break;
            }
        }
        AreaEffectCloud cloud = new AreaEffectCloud(level(), impact.x, impact.y, impact.z);
        cloud.setOwner(this);
        cloud.setRadius(3.0F);
        cloud.setDuration(100);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 300, 0, false, true));
        level().addFreshEntity(cloud);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, source.x, source.y, source.z,
                    12, 0.25D, 0.25D, 0.25D, 0.02D);
        }
    }

    public enum BodyPart {
        HEAD,
        LEFT_WING,
        RIGHT_WING
    }

    private static final class DragonBodyPart extends PartEntity<AssimilatedDragonEntity> {
        private final BodyPart part;
        private final String name;
        private final float width;
        private final float height;

        private DragonBodyPart(AssimilatedDragonEntity parent, BodyPart part, String name, float width, float height) {
            super(parent);
            this.part = part;
            this.name = name;
            this.width = width;
            this.height = height;
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        public boolean isPickable() {
            return switch (part) {
                case HEAD -> getParent().hasHead();
                case LEFT_WING -> getParent().entityData.get(LEFT_WING_ATTACHED);
                case RIGHT_WING -> getParent().entityData.get(RIGHT_WING_ATTACHED);
            };
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            return getParent().hurtBodyPart(part, source, amount);
        }

        @Override
        public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
            return net.minecraft.world.entity.EntityDimensions.scalable(width, height);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public Component getName() {
            return Component.literal(name);
        }
    }

    private final class DragonCombatGoal extends Goal {
        private int meleeCooldown;

        private DragonCombatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = distanceToSqr(target);
            if (canFly() && (distance > 144.0D || target.getY() > getY() + 3.0D)) {
                setFlying(true);
            } else if (distance < 36.0D) {
                setFlying(false);
            }
            if (isFlying()) {
                getMoveControl().setWantedPosition(target.getX(), target.getY() + 4.0D, target.getZ(), 0.8D);
            } else {
                getNavigation().moveTo(target, 1.35D);
            }
            if (meleeCooldown > 0) meleeCooldown--;
            if (distance < 20.25D && meleeCooldown <= 0) {
                doHurtTarget(target);
                meleeCooldown = 20;
            }
        }
    }
}
