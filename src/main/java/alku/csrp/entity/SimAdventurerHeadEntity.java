package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.EnumSet;

/** Walking head companion that reforms an Assimilated Adventurer with a Medium Incomplete Form. */
public final class SimAdventurerHeadEntity extends Monster implements GeoEntity, Parasite {
    private static final EntityDataAccessor<Integer> LEAP_TICKS = SynchedEntityData.defineId(
            SimAdventurerHeadEntity.class, EntityDataSerializers.INT);
    private static final double COTH_AURA_RADIUS = 3.0D;
    private static final float MINIMUM_DAMAGE = 0.5F;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this, "idle.get_parasite_status_10");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public SimAdventurerHeadEntity(EntityType<? extends SimAdventurerHeadEntity> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.5D)
                .add(Attributes.ATTACK_DAMAGE, 2.7D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MergeWithIncompleteFormGoal());
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, LivingEntity.class, 8.0F, 1.0D, 1.3D,
                this::shouldFleeInDaylight));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F) {
            @Override
            public void start() {
                super.start();
                entityData.set(LEAP_TICKS, 24);
            }
        });
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.3D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && entityData.get(LEAP_TICKS) > 0) {
            entityData.set(LEAP_TICKS, entityData.get(LEAP_TICKS) - 1);
        }
        if (level().isClientSide || tickCount % 20 != 0) {
            return;
        }
        for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(COTH_AURA_RADIUS), this::isValidParasiteTarget)) {
            if (hasLineOfSight(nearby)) {
                InfectionMechanics.applyCoth(nearby, this);
            }
        }

        if (isInWaterOrBubble() && getTarget() != null) {
            Vec3 direction = getTarget().position().subtract(position());
            if (direction.lengthSqr() > 0.001D) {
                direction = direction.normalize();
                setDeltaMovement(getDeltaMovement().add(direction.x * 0.08D, 0.14D, direction.z * 0.08D));
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof IncompleteFormMediumEntity medium) {
            return mergeWith(medium);
        }
        float healthBefore = target instanceof LivingEntity living
                ? ParasiteCombatEffects.healthWithAbsorption(living) : 0.0F;
        boolean hit = super.doHurtTarget(target);
        if (hit && !level().isClientSide) {
            triggerAnim("attack_controller", "attack");
            if (target instanceof LivingEntity living) {
                applyMinimumDamage(living, healthBefore);
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return super.causeFallDamage(distance, damageMultiplier * 0.3F, source);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.SIM_ADVENTURER_HEAD_LIVING.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SIM_ADVENTURER_HEAD_HURT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return ModSounds.SIM_ADVENTURER_HEAD_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(entityData.get(LEAP_TICKS) > 0
                        ? LEAP : ParasiteAnimations.isMoving(this, state.isMoving()) ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    private boolean shouldFleeInDaylight(LivingEntity target) {
        if (target == this || target instanceof Parasite || !level().isDay() || !level().canSeeSky(blockPosition())) {
            return false;
        }
        AABB nearby = getBoundingBox().inflate(16.0D);
        return level().getEntitiesOfClass(LivingEntity.class, nearby,
                entity -> entity != this && entity.isAlive() && entity instanceof Parasite).isEmpty();
    }

    private void copyIdentity(Mob target) {
        target.setCustomName(getCustomName());
        target.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            target.setPersistenceRequired();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LEAP_TICKS, 0);
    }

    private boolean mergeWith(IncompleteFormMediumEntity medium) {
        if (!isAlive() || !medium.isAlive() || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        SimAdventurerEntity adventurer = ModEntities.SIM_ADVENTURER.get().create(serverLevel);
        if (adventurer == null) {
            return false;
        }
        adventurer.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        adventurer.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        copyIdentity(adventurer);
        serverLevel.addFreshEntity(adventurer);
        medium.discard();
        discard();
        return true;
    }

    private void applyMinimumDamage(LivingEntity target, float healthBefore) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel).minimumDamage() || !target.isAlive()) {
            return;
        }
        float dealt = Math.max(0.0F, healthBefore - ParasiteCombatEffects.healthWithAbsorption(target));
        float remaining = MINIMUM_DAMAGE - dealt;
        if (remaining <= 0.0F) {
            return;
        }
        float absorption = target.getAbsorptionAmount();
        float absorbed = Math.min(absorption, remaining);
        target.setAbsorptionAmount(absorption - absorbed);
        remaining -= absorbed;
        if (remaining > 0.0F) {
            target.setHealth(Math.max(0.0F, target.getHealth() - remaining));
        }
        level().broadcastEntityEvent(target, (byte) 2);
    }

    private final class MergeWithIncompleteFormGoal extends Goal {
        private IncompleteFormMediumEntity mergeTarget;
        private int searchCooldown;

        private MergeWithIncompleteFormGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (searchCooldown-- > 0) {
                return false;
            }
            searchCooldown = 10;
            mergeTarget = level().getEntitiesOfClass(IncompleteFormMediumEntity.class,
                            getBoundingBox().inflate(getAttributeValue(Attributes.FOLLOW_RANGE)),
                            Entity::isAlive).stream()
                    .min(Comparator.comparingDouble(SimAdventurerHeadEntity.this::distanceToSqr))
                    .orElse(null);
            return mergeTarget != null;
        }

        @Override
        public boolean canContinueToUse() {
            return mergeTarget != null && mergeTarget.isAlive()
                    && distanceToSqr(mergeTarget) <= 576.0D;
        }

        @Override
        public void start() {
            getNavigation().moveTo(mergeTarget, 1.3D);
        }

        @Override
        public void tick() {
            if (mergeTarget == null) {
                return;
            }
            getLookControl().setLookAt(mergeTarget, 30.0F, 30.0F);
            getNavigation().moveTo(mergeTarget, 1.3D);
            double reach = getBbWidth() + mergeTarget.getBbWidth();
            if (distanceToSqr(mergeTarget) <= reach * reach) {
                mergeWith(mergeTarget);
            }
        }

        @Override
        public void stop() {
            mergeTarget = null;
            getNavigation().stop();
        }
    }
}
