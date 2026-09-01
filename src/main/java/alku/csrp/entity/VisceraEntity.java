package alku.csrp.entity;

import alku.csrp.config.MobsConfig;
import alku.csrp.effect.EffectStacking;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelRawAnimation;

import java.util.EnumSet;

/** Original Primitive Viscera (EntityGim) behavior. */
public final class VisceraEntity extends PrimitiveParasiteEntity implements ManualVariantProvider {
    private static final EntityDataAccessor<Byte> CLIMBING = SynchedEntityData.defineId(
            VisceraEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            VisceraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(
            VisceraEntity.class, EntityDataSerializers.INT);

    private static final int STATUS_IDLE = 0;
    private static final int STATUS_WALK = 1;
    private static final int STATUS_RUN = 2;
    private static final int STATUS_LEAP = 10;
    private static final int SKIN_NORMAL = 0;
    private static final int SKIN_VIRULENT = 5;
    private static final int SKIN_BLEEDING = 6;
    private static final double MELEE_SPEED = 1.3D;
    private static final double MELEE_SPRINT_DISTANCE_SQR = 64.0D;
    private static final int MELEE_ATTACK_INTERVAL = 20;

    private final CitadelRawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final CitadelRawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");
    private final CitadelRawAnimation COMBAT_AGE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final CitadelRawAnimation COMBAT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final CitadelRawAnimation SPRINT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");

    public VisceraEntity(EntityType<? extends VisceraEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.ARMOR, 9.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected boolean usesDefaultFloatGoal() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new SwimmingDivingGoal());
        goalSelector.addGoal(2, new WaterLeapGoal());
        goalSelector.addGoal(2, new EvadeGoal());
        goalSelector.addGoal(3, new VisceraMeleeGoal());
        goalSelector.addGoal(6, new RecruitFollowersGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            setClimbing(horizontalCollision);
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                setParasiteStatus(STATUS_IDLE);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit && entity instanceof LivingEntity target) {
            if (getSkin() == SKIN_VIRULENT) {
                EffectStacking.apply(target, ModMobEffects.VIRAL, 40, 0);
            } else if (getSkin() == SKIN_BLEEDING) {
                EffectStacking.apply(target, ModMobEffects.BLEED, 40, 0);
            }
        }
        return hit;
    }

    @Override
    public void push(Entity entity) {
        super.push(entity);
        if (!level().isClientSide && getSkin() == SKIN_VIRULENT
                && entity instanceof LivingEntity target && !(target instanceof Parasite)) {
            EffectStacking.apply(target, ModMobEffects.VIRAL, 40, 0);
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return getAdaptationHitStatus() > 0 && random.nextBoolean() ? null : super.getHurtSound(source);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, (byte) 0);
        builder.define(PARASITE_STATUS, STATUS_IDLE);
        builder.define(SKIN, SKIN_NORMAL);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasite_status", getParasiteStatus());
        tag.putInt("viscera_skin", getSkin());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setParasiteStatus(tag.getInt("parasite_status"));
        setSkin(tag.getInt("viscera_skin"));
    }

    @Override
    public boolean onClimbable() {
        return (entityData.get(CLIMBING) & 1) != 0;
    }

    private void setClimbing(boolean climbing) {
        entityData.set(CLIMBING, climbing ? (byte) 1 : (byte) 0);
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    public int getSkin() {
        return entityData.get(SKIN);
    }

    @Override
    public int getManualVariant() {
        return entityData.get(SKIN);
    }

    @Override
    public void setManualVariant(int variant) {
        entityData.set(SKIN, Math.clamp(variant, 0, getMaxManualVariants() - 1));
    }

    public void setSkin(int skin) {
        entityData.set(SKIN, skin == SKIN_VIRULENT || skin == SKIN_BLEEDING ? skin : SKIN_NORMAL);
    }

    public void applyConfiguredAttributes() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(MobsConfig.visceraHealth());
        getAttribute(Attributes.ARMOR).setBaseValue(MobsConfig.visceraArmor());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(MobsConfig.visceraDamage());
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(MobsConfig.visceraKnockbackResistance());
    }

    @Override
    public boolean applyScaryOrbEffect(LivingEntity target, int nearbyEntities) {
        boolean applied = super.applyScaryOrbEffect(target, nearbyEntities);
        if (applied) {
            ConfiguredOrbEffects.apply(this, target, nearbyEntities, MobsConfig.visceraOrbEffects());
        }
        return applied;
    }

    @Override
    public void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers) {
        controllers.add(new CitadelAnimationController<>(this, "movement_controller", 4, state -> {
            boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
            int status = getParasiteStatus();
            if (!moving) {
                return state.setAndContinue(status == STATUS_IDLE ? AGE_IN_TICKS : COMBAT_AGE);
            }
            if (status == STATUS_RUN || getDeltaMovement().horizontalDistanceSqr() > 0.02D) {
                return state.setAndContinue(SPRINT_LIMB);
            }
            return state.setAndContinue(status == STATUS_WALK ? COMBAT_LIMB : LIMB_SWING);
        }));
    }

    private final class SwimmingDivingGoal extends Goal {
        private SwimmingDivingGoal() {
            setFlags(EnumSet.of(Flag.JUMP));
            getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            if (!isInWaterOrBubble() && !isInLava()) {
                return false;
            }
            LivingEntity target = getTarget();
            if (target != null && (target.isInWaterOrBubble() || target.isInLava())
                    && distanceToSqr(getX(), target.getY(), getZ()) < 25.0D
                    && target.getY() - getY() < -1.0D) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.095D, 0.0D));
                return false;
            }
            return true;
        }

        @Override
        public void tick() {
            if (random.nextFloat() < 0.8F) {
                getJumpControl().jump();
            }
        }
    }

    private final class WaterLeapGoal extends Goal {
        private int attackTimer;
        private int attacking;
        private double targetX;
        private double targetY;
        private double targetZ;

        @Override
        public boolean canUse() {
            return isInWaterOrBubble() || isInLava() || attacking >= 1;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && getParasiteStatus() <= 2) {
                attackTimer++;
                if (attackTimer >= 20 && attacking == 0) {
                    attacking = 1;
                    targetX = target.getX();
                    targetZ = target.getZ();
                    targetY = Math.max(0.0D, (target.getY() - getY()) * 0.07D);
                }
            } else if (attackTimer > 0) {
                attackTimer--;
            }

            if (attacking < 1) {
                return;
            }
            attacking++;
            if (attacking == 2 && onGround()) {
                setParasiteStatus(STATUS_LEAP);
                getNavigation().stop();
                double dx = targetX - getX();
                double dz = targetZ - getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                Vec3 motion = getDeltaMovement();
                if (distance > 0.0D) {
                    setDeltaMovement(motion.x + dx / distance * 1.5D * 0.9D + motion.x * 0.3D,
                            0.7D + targetY,
                            motion.z + dz / distance * 1.5D * 0.9D + motion.z * 0.3D);
                }
            }
            if (attacking >= 3 && onGround()) {
                attacking = 0;
                attackTimer = 0;
                setParasiteStatus(STATUS_RUN);
            }
        }
    }

    private final class VisceraMeleeGoal extends Goal {
        private int attackTick;

        private VisceraMeleeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            attackTick = 0;
            updateMovementStatus();
        }

        @Override
        public void stop() {
            getNavigation().stop();
            setParasiteStatus(STATUS_IDLE);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (attackTick > 0) {
                attackTick--;
            }
            double distance = distanceToSqr(target);
            getNavigation().moveTo(target, distance > MELEE_SPRINT_DISTANCE_SQR ? MELEE_SPEED : 1.0D);
            updateMovementStatus();
            if (isWithinMeleeAttackRange(target) && attackTick <= 0 && getSensing().hasLineOfSight(target)) {
                attackTick = MELEE_ATTACK_INTERVAL;
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                doHurtTarget(target);
                setParasiteStatus(STATUS_WALK);
            }
        }

        private void updateMovementStatus() {
            LivingEntity target = getTarget();
            if (target == null) {
                setParasiteStatus(STATUS_IDLE);
            } else if (distanceToSqr(target) > MELEE_SPRINT_DISTANCE_SQR) {
                setParasiteStatus(STATUS_RUN);
            } else {
                setParasiteStatus(STATUS_WALK);
            }
        }
    }

    private final class EvadeGoal extends Goal {
        private int cooldown = 56;
        private int duration;
        private boolean evading;

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return evading || target != null && target.isAlive() && getParasiteStatus() > 0
                    && getParasiteStatus() < 3 && onGround()
                    && !hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                    && distanceToSqr(target) > 16.0D && distanceToSqr(target) < 225.0D
                    && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            cooldown = 0;
            duration = 0;
            evading = false;
            setXxa(0.0F);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (evading) {
                if (++duration >= 10) {
                    setXxa(0.0F);
                    duration = 0;
                    cooldown = 0;
                    evading = false;
                }
                return;
            }
            if (target == null || hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                return;
            }
            double distance = distanceToSqr(target);
            if (distance <= 16.0D || distance >= 225.0D || !hasLineOfSight(target)) {
                return;
            }
            if (++cooldown < 55) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            setXxa(random.nextBoolean() ? 1.0F : -1.0F);
            evading = true;
            Vec3 motion = getDeltaMovement();
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 0.0D) {
                setDeltaMovement(motion.x + dx / length * 1.77D * 0.8D + motion.x * 0.2D,
                        0.2D + getBbHeight() * 0.1D,
                        motion.z + dz / length * 1.77D * 0.8D + motion.z * 0.2D);
            }
            getNavigation().stop();
        }
    }

    private final class RecruitFollowersGoal extends Goal {
        @Override
        public boolean canUse() {
            return tickCount % 20 == 0 && getTarget() == null
                    && ParasiteFollowGoal.getLeader(VisceraEntity.this) == null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            for (Mob follower : level().getEntitiesOfClass(Mob.class,
                    getBoundingBox().inflate(16.0D, 2.0D, 16.0D), candidate ->
                            candidate != VisceraEntity.this && candidate instanceof Parasite
                                    && candidate.isAlive() && ParasiteFollowGoal.commandRank(candidate) < 41)) {
                if (hasLineOfSight(follower)) {
                    Mob leader = ParasiteFollowGoal.getLeader(follower);
                    if (leader == null || ParasiteFollowGoal.commandRank(leader) <= 30) {
                        ParasiteFollowGoal.setLeader(follower, VisceraEntity.this);
                        break;
                    }
                }
            }
        }
    }
}
