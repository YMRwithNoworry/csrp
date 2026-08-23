package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.Config;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.EnumSet;

public final class DredgeEntity extends CrudeParasiteEntity {
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_COMBAT = 1;
    private static final int STATUS_SPRINT = 2;
    private static final int STATUS_PULLING = 3;
    private static final int STILL_ANIMATION_DELAY_TICKS = 25;
    private static final int MAX_PULL_TICKS = 200;
    private static final double PULL_STRENGTH = 0.13D;
    private static final int MAX_LIQUID_LEAPS = 8;
    private static final int LIQUID_LEAP_INTERVAL_TICKS = 21;
    private static final double LIQUID_LEAP_HORIZONTAL_SPEED = 1.2D;
    private static final double LIQUID_LEAP_VERTICAL_SPEED = 0.3D;
    private static final int MELEE_ATTACK_INTERVAL_TICKS = 10;
    private static final float REGENERATION_AMOUNT = 4.0F;
    private static final int REGENERATION_INTERVAL_TICKS = 20;
    private static final double RECRUIT_RANGE = 16.0D;
    private static final EntityDataAccessor<Integer> TARGET_ENTITY = SynchedEntityData.defineId(
            DredgeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PULLING = SynchedEntityData.defineId(
            DredgeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            DredgeEntity.class, EntityDataSerializers.INT);
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation STILL_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_still_ani_1");
    private final RawAnimation COMBAT_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation COMBAT_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation COMBAT_STILL_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation SPRINT_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_2");
    private final RawAnimation SPRINT_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation SPRINT_STILL_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1");

    private LivingEntity targetedEntity;
    private int pulling;
    private boolean canPull = true;
    private int liquidLeap;
    private int regenUse = 1;
    private int stillAnimationTicks;

    public DredgeEntity(EntityType<? extends DredgeEntity> type, Level level) {
        super(type, level);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 9.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    public boolean supportsDamageAdaptation() {
        return true;
    }

    @Override
    protected boolean usesDefaultMovementGoals() {
        return false;
    }

    @Override
    protected boolean usesDefaultFloatGoal() {
        return false;
    }

    @Override
    protected boolean usesDefaultTargetGoals() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new DredgeSwimmingGoal());
        goalSelector.addGoal(3, new DredgeMeleeGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RecruitFollowersGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 0,
                false, false, this::isValidParasiteTarget));
        if (Config.mobAttackingEnabled()) {
            targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 0,
                    !Config.collectiveConsciousnessEnabled(), false, this::isValidDredgeMobTarget));
        }
    }

    private boolean isValidDredgeMobTarget(LivingEntity target) {
        if (!isValidParasiteTarget(target) || target instanceof WaterAnimal
                || target instanceof Animal || target instanceof Villager) {
            return false;
        }
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        String value = id.toString();
        String namespace = id.getNamespace();
        boolean listed = Config.mobAttackingBlacklist().stream()
                .anyMatch(entry -> entry.indexOf(':') >= 0 ? entry.equals(value) : entry.equals(namespace));
        return Config.mobAttackingBlacklistInverted() ? listed : !listed;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TARGET_ENTITY, 0);
        entityData.define(PULLING, false);
        entityData.define(PARASITE_STATUS, STATUS_IDLE);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit && entity instanceof LivingEntity target && !hasTargetedEntity() && canPull) {
            setTargetedEntity(target.getId());
            setParasiteStatus(STATUS_PULLING);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 3, false, false), this);
        }
        return hit;
    }

    @Override
    public void tick() {
        if (getX() == xo && getZ() == zo) {
            stillAnimationTicks++;
        } else {
            stillAnimationTicks = 0;
        }
        super.tick();
        if (level().isClientSide) {
            return;
        }

        if (!canPull && --pulling <= 0) {
            pulling = 0;
            canPull = true;
        }
        tickRegeneration();
        tickLiquidLeap();
        tickPulling();
    }

    private void tickRegeneration() {
        if (tickCount % REGENERATION_INTERVAL_TICKS != 10 || isOnFire()
                || getHealth() <= 0.0F || getHealth() >= getMaxHealth() || getParasiteKills() <= 1) {
            return;
        }
        heal(REGENERATION_AMOUNT);
        if (--regenUse <= 0) {
            consumeParasiteKill();
            regenUse = 1;
        }
    }

    private void tickLiquidLeap() {
        LivingEntity target = getTarget();
        boolean inLiquid = isInWaterOrBubble() || isInLava();
        if (tickCount % LIQUID_LEAP_INTERVAL_TICKS == 0 && inLiquid && target != null && target.isAlive()) {
            liquidLeap = Math.min(MAX_LIQUID_LEAPS, liquidLeap + 1);
        }
        if (liquidLeap < 1 || target == null || !target.isAlive()) {
            return;
        }
        liquidLeap--;
        getNavigation().stop();
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= 1.0E-6D) {
            return;
        }
        double speed = level().getBlockState(blockPosition()).isPathfindable(level(), blockPosition(), PathComputationType.LAND)
                ? LIQUID_LEAP_HORIZONTAL_SPEED : 1.0D;
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(movement.x + dx / distance * speed * 0.8D + movement.x * 0.2D,
                LIQUID_LEAP_VERTICAL_SPEED,
                movement.z + dz / distance * speed * 0.8D + movement.z * 0.2D);
        lookAt(target, 30.0F, 30.0F);
    }

    private void tickPulling() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setTarget(null);
            clearTargetedEntity();
        } else if (hasLineOfSight(target) && distanceToSqr(target) > 0.0D
                && canPull && getTargetedEntity() != null) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false), this);
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 1, false, false), this);
            lookAt(target, 30.0F, 30.0F);
            applyPrimitiveMinimumDamage(target, 0.02F);
            setParasiteStatus(STATUS_PULLING);
            pulling++;
            if (pulling > MAX_PULL_TICKS || distanceToSqr(target) > 9.0D) {
                clearTargetedEntity();
                canPull = false;
            }
        } else {
            clearTargetedEntity();
        }

        entityData.set(PULLING, hasTargetedEntity());
        LivingEntity pullTarget = getTargetedEntity();
        if (pullTarget != null) {
            applyPullMotion(pullTarget);
        } else {
            updateCombatStatus();
        }
    }

    private void applyPullMotion(LivingEntity target) {
        target.stopRiding();
        Vec3 direction = position().subtract(target.position());
        if (direction.lengthSqr() <= 0.001D) {
            return;
        }
        direction = direction.normalize().scale(PULL_STRENGTH);
        target.push(direction.x, direction.y, direction.z);
    }

    private void setTargetedEntity(int entityId) {
        if (canPull || entityId == 0) {
            entityData.set(TARGET_ENTITY, entityId);
            if (entityId == 0) {
                targetedEntity = null;
            }
        }
    }

    private void clearTargetedEntity() {
        setTargetedEntity(0);
    }

    private boolean hasTargetedEntity() {
        return canPull && entityData.get(TARGET_ENTITY) != 0;
    }

    private LivingEntity getTargetedEntity() {
        int entityId = entityData.get(TARGET_ENTITY);
        if (entityId == 0 || !canPull) {
            targetedEntity = null;
            return null;
        }
        if (!level().isClientSide) {
            return getTarget();
        }
        if (targetedEntity != null && targetedEntity.getId() == entityId) {
            return targetedEntity;
        }
        Entity entity = level().getEntity(entityId);
        targetedEntity = entity instanceof LivingEntity living && living.isAlive() ? living : null;
        return targetedEntity;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == TARGET_ENTITY) {
            targetedEntity = null;
        }
    }

    private int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    private void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    private boolean isStillAnimation() {
        return stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS;
    }

    private void updateCombatStatus() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setParasiteStatus(STATUS_IDLE);
            return;
        }
        setParasiteStatus(getDeltaMovement().horizontalDistanceSqr() > 0.0004D
                ? STATUS_SPRINT : STATUS_COMBAT);
    }

    @Override
    protected int incomingDamageCapDivisor() {
        return 6;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getParasiteStatus() == STATUS_IDLE ? ModSounds.DREDGE_LIVING.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return getAdaptationHitStatus() > 0 && random.nextBoolean() ? null : ModSounds.DREDGE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DREDGE_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
            return switch (getParasiteStatus()) {
                case STATUS_COMBAT -> state.setAndContinue(isStillAnimation()
                        ? COMBAT_STILL_IDLE : moving ? COMBAT_WALK : COMBAT_IDLE);
                case STATUS_SPRINT -> state.setAndContinue(isStillAnimation()
                        ? SPRINT_STILL_IDLE : moving ? SPRINT_WALK : SPRINT_IDLE);
                case STATUS_PULLING -> state.setAndContinue(IDLE);
                default -> state.setAndContinue(isStillAnimation() ? STILL_IDLE : moving ? WALK : IDLE);
            };
        }));
    }

    private final class DredgeSwimmingGoal extends Goal {
        private DredgeSwimmingGoal() {
            setFlags(EnumSet.of(Flag.JUMP));
            if (getNavigation() instanceof GroundPathNavigation navigation) {
                navigation.setCanFloat(true);
            }
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

    private final class DredgeMeleeGoal extends Goal {
        private int attackTick;

        private DredgeMeleeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            getNavigation().stop();
            attackTick = 0;
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
            double speed = level() instanceof ServerLevel serverLevel
                    && EvolutionSystem.generationProfile(serverLevel).sprinting() ? 1.3D : 1.0D;
            getNavigation().moveTo(target, speed);
            if (isWithinMeleeAttackRange(target) && attackTick <= 0 && getSensing().hasLineOfSight(target)) {
                attackTick = MELEE_ATTACK_INTERVAL_TICKS;
                doHurtTarget(target);
            }
        }
    }

    private final class RecruitFollowersGoal extends Goal {
        @Override
        public boolean canUse() {
            return tickCount % 20 == 0 && getTarget() == null
                    && ParasiteFollowGoal.getLeader(DredgeEntity.this) == null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            for (Mob follower : level().getEntitiesOfClass(Mob.class,
                    getBoundingBox().inflate(RECRUIT_RANGE, 2.0D, RECRUIT_RANGE), candidate ->
                            candidate != DredgeEntity.this && candidate instanceof Parasite
                                    && candidate.isAlive() && ParasiteFollowGoal.commandRank(candidate) < 31)) {
                if (!hasLineOfSight(follower)) {
                    continue;
                }
                Mob leader = ParasiteFollowGoal.getLeader(follower);
                if (leader == null || ParasiteFollowGoal.commandRank(leader) <= 10) {
                    ParasiteFollowGoal.setLeader(follower, DredgeEntity.this);
                    break;
                }
            }
        }
    }
}
