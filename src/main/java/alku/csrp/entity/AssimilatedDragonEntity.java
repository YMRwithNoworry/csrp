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
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation FLY = ParasiteAnimations.loop(this, "fly");
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
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(isFlying());
        updateBodyParts();
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
        tag.putFloat("head_health", headHealth);
        tag.putFloat("left_wing_health", leftWingHealth);
        tag.putFloat("right_wing_health", rightWingHealth);
        tag.putInt("ranged_cooldown", rangedCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        headHealth = tag.getFloat("head_health");
        leftWingHealth = tag.getFloat("left_wing_health");
        rightWingHealth = tag.getFloat("right_wing_health");
        rangedCooldown = tag.getInt("ranged_cooldown");
        entityData.set(HEAD_ATTACHED, headHealth > 0.0F);
        entityData.set(LEFT_WING_ATTACHED, leftWingHealth > 0.0F);
        entityData.set(RIGHT_WING_ATTACHED, rightWingHealth > 0.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (isFlying()) return state.setAndContinue(FLY);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
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

    private void setFlying(boolean flying) {
        entityData.set(FLYING, flying && canFly());
        setNoGravity(entityData.get(FLYING));
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
        Vec3 source = getEyePosition().add(getLookAngle().scale(1.8D));
        Vec3 direction = target.getEyePosition().subtract(source);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        direction = direction.normalize();
        Vec3 impact = source.add(direction.scale(Math.min(32.0D, Math.sqrt(distanceToSqr(target)))));
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().expandTowards(direction.scale(32.0D)).inflate(2.0D), this::isValidParasiteTarget)) {
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
