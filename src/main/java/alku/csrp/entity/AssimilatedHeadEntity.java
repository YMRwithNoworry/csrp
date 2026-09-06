package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.EnumSet;

/** Shared walking-head behavior: infect targets and rebuild a body with a medium incomplete form. */
public final class AssimilatedHeadEntity extends Monster implements GeoEntity, Parasite {
    private static final EntityDataAccessor<Integer> LEAP_TICKS = SynchedEntityData.defineId(
            AssimilatedHeadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            AssimilatedHeadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SCREAMING = SynchedEntityData.defineId(
            AssimilatedHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private final RawAnimation FUNC_78087_A_AGE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final RawAnimation FUNC_78087_A_MOVEMENT = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");
    private final RawAnimation FUNC_78087_A_AGE_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation FUNC_78087_A_MOVEMENT_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation FUNC_78087_A_AGE_STATUS_10 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");
    private final RawAnimation FUNC_78087_A_AGE_SCREAMING = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.is_screaming_1");
    private final RawAnimation FUNC_78087_A_MOVEMENT_SCREAMING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.is_screaming_1");
    private final RawAnimation FUNC_78087_A_AGE_STATUS_1_SCREAMING = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1.is_screaming_1");
    private final RawAnimation FUNC_78087_A_MOVEMENT_STATUS_1_SCREAMING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1.is_screaming_1");
    private final RawAnimation FUNC_78087_A_AGE_STATUS_10_SCREAMING = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10.is_screaming_1");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Kind kind;
    private int cloudCooldown;

    public AssimilatedHeadEntity(EntityType<? extends AssimilatedHeadEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.experience;
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.FOLLOW_RANGE, kind.followRange);
    }

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
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LivingEntity.class, 8.0F, 1.0D, 1.3D,
                this::shouldAvoid));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F) {
            @Override
            public void start() {
                super.start();
                entityData.set(LEAP_TICKS, 24);
                setParasiteStatus(10);
            }
        });
        goalSelector.addGoal(3, new HeadCothCloudGoal());
        goalSelector.addGoal(4, new HeadMeleeGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        setAggressive(target != null);
        if (kind == Kind.ENDERMAN) {
            entityData.set(SCREAMING, target != null);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (kind == Kind.ENDERMAN) {
                spawnPortalParticles();
            }
            return;
        }
        if (cloudCooldown > 0) {
            cloudCooldown--;
        }
        if (shouldRetreatForPackSize() && getTarget() != null) {
            setTarget(null);
        }
        int leapTicks = entityData.get(LEAP_TICKS);
        if (leapTicks > 0) {
            int remainingTicks = leapTicks - 1;
            entityData.set(LEAP_TICKS, remainingTicks);
            if (getParasiteStatus() == 10
                    && (remainingTicks == 0 || remainingTicks <= 22 && onGround())) {
                entityData.set(LEAP_TICKS, 0);
                setParasiteStatus(0);
            }
        }
        if (kind == Kind.ENDERMAN && tickCount % 20 == 0 && getTarget() != null
                && distanceToSqr(getTarget()) > 4.0D && random.nextInt(3) == 0) {
            teleportAwayFromTarget(getTarget());
        }
        if (tickCount % 20 == 0) {
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(3.0D), this::isValidParasiteTarget)) {
                InfectionMechanics.applyCoth(target, this);
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LEAP_TICKS, 0);
        entityData.define(PARASITE_STATUS, 0);
        entityData.define(SCREAMING, false);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof IncompleteFormMediumEntity && level() instanceof ServerLevel serverLevel) {
            Mob body = switch (kind) {
                case COW -> ModEntities.SIM_COW.get().create(serverLevel);
                case ENDERMAN -> ModEntities.SIM_ENDERMAN.get().create(serverLevel);
                case HORSE -> ModEntities.SIM_HORSE.get().create(serverLevel);
                case HUMAN -> ModEntities.SIM_HUMAN.get().create(serverLevel);
                case PIG -> ModEntities.SIM_PIG.get().create(serverLevel);
                case SHEEP -> ModEntities.SIM_SHEEP.get().create(serverLevel);
                case VILLAGER -> ModEntities.SIM_VILLAGER.get().create(serverLevel);
                case WOLF -> ModEntities.SIM_WOLF.get().create(serverLevel);
            };
            if (body != null) {
                body.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                body.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                        MobSpawnType.MOB_SUMMONED, null, null);
                body.setCustomName(getCustomName());
                body.setCustomNameVisible(isCustomNameVisible());
                if (isPersistenceRequired()) {
                    body.setPersistenceRequired();
                }
                serverLevel.addFreshEntity(body);
            }
            target.discard();
            discard();
            return true;
        }
        LivingEntity livingTarget = target instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(target);
        if (hit && livingTarget != null) {
            ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
            InfectionMechanics.applyCoth(livingTarget, this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (kind == Kind.ENDERMAN && !level().isClientSide && source.getDirectEntity() != null
                && source.getDirectEntity() != source.getEntity()) {
            for (int attempt = 0; attempt < 16; attempt++) {
                if (teleportAwayFromTarget(getTarget())) {
                    return true;
                }
            }
            return false;
        }
        boolean hurt = super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
        if (hurt && kind == Kind.ENDERMAN && !level().isClientSide && random.nextBoolean()) {
            teleportAwayFromTarget(getTarget());
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return super.causeFallDamage(distance, damageMultiplier * 0.3F, source);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 4, state -> {
            RawAnimation animation = switch (getParasiteStatus()) {
                case 1, 2 -> usesStatusOneAgeAnimation()
                        ? isScreaming() ? FUNC_78087_A_AGE_STATUS_1_SCREAMING : FUNC_78087_A_AGE_STATUS_1
                        : FUNC_78087_A_AGE;
                case 10 -> isScreaming() ? FUNC_78087_A_AGE_STATUS_10_SCREAMING : FUNC_78087_A_AGE_STATUS_10;
                default -> isScreaming() ? FUNC_78087_A_AGE_SCREAMING : FUNC_78087_A_AGE;
            };
            return state.setAndContinue(animation);
        }));
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (getParasiteStatus() == 10
                    || !ParasiteAnimations.isMoving(this, state.isMoving())) {
                return PlayState.STOP;
            }
            RawAnimation animation = getParasiteStatus() == 1 || getParasiteStatus() == 2
                    ? isScreaming() ? FUNC_78087_A_MOVEMENT_STATUS_1_SCREAMING
                    : FUNC_78087_A_MOVEMENT_STATUS_1
                    : isScreaming() ? FUNC_78087_A_MOVEMENT_SCREAMING : FUNC_78087_A_MOVEMENT;
            return state.setAndContinue(animation);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public Kind getKind() {
        return kind;
    }

    private boolean usesStatusOneAgeAnimation() {
        return kind != Kind.HORSE;
    }

    private boolean isScreaming() {
        return kind == Kind.ENDERMAN && entityData.get(SCREAMING);
    }

    @Override
    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    private void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    private boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite)
                && !shouldRetreatForPackSize();
    }

    private boolean shouldAvoid(LivingEntity target) {
        return shouldRetreatForPackSize()
                && target != this
                && !(target instanceof Parasite)
                && (target instanceof Player || target instanceof Monster);
    }

    private boolean shouldRetreatForPackSize() {
        return nearbyParasites() <= 2;
    }

    private int nearbyParasites() {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(8.0D),
                parasite -> parasite != this && parasite.isAlive() && parasite instanceof Parasite).size();
    }

    private final class HeadCothCloudGoal extends Goal {
        private LivingEntity passiveTarget;

        private HeadCothCloudGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cloudCooldown > 0 || !shouldRetreatForPackSize() || getTarget() != null
                    || !level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(8.0D),
                    AssimilatedHeadEntity.this::shouldAvoid).isEmpty()
                    || random.nextInt(reducedTickDelay(20)) != 0) {
                return false;
            }
            AABB scanArea = getBoundingBox().inflate(12.0D, 3.0D, 12.0D);
            passiveTarget = level().getEntitiesOfClass(LivingEntity.class, scanArea,
                            entity -> entity != AssimilatedHeadEntity.this && entity.isAlive()
                                    && (entity instanceof Animal || entity instanceof WaterAnimal
                                    || entity instanceof Villager)
                                    && !entity.hasEffect(ModMobEffects.COTH.get())
                                    && hasLineOfSight(entity)
                                    && distanceToSqr(entity) < 81.0D
                                    && navigation.createPath(entity, 1) != null)
                    .stream()
                    .min(Comparator.comparingDouble(AssimilatedHeadEntity.this::distanceToSqr))
                    .orElse(null);
            return passiveTarget != null;
        }

        @Override
        public void start() {
            if (passiveTarget == null) {
                return;
            }
            getLookControl().setLookAt(passiveTarget, 30.0F, 30.0F);
            ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
            cloud.setOwner(AssimilatedHeadEntity.this);
            cloud.setRadius((float) getBbWidth() * 4.0F);
            cloud.setRadiusOnUse(-0.5F);
            cloud.setDuration(1200);
            cloud.setWaitTime(10);
            cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
            cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH.get(), 3600, 1, false, false, true));
            // Legacy COTH II cloud effect: new MobEffectInstance(ModMobEffects.COTH, 3600, 1, false, false, true).
            level().addFreshEntity(cloud);
            playSound(ModSounds.RUPTER_CLOUD.get(), 1.2F, 1.1F);
            cloudCooldown = 20;
        }
    }

    private boolean teleportAwayFromTarget(LivingEntity target) {
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 destination = position().add((random.nextDouble() - 0.5D) * 32.0D,
                    random.nextInt(16) - 8, (random.nextDouble() - 0.5D) * 32.0D);
            if (target != null && target.distanceToSqr(destination) < 4.0D) {
                continue;
            }
            net.minecraft.core.BlockPos blockPos = net.minecraft.core.BlockPos.containing(destination);
            while (blockPos.getY() > level().getMinBuildHeight() && !level().getBlockState(blockPos).blocksMotion()) {
                blockPos = blockPos.below();
            }
            if (!level().getBlockState(blockPos).blocksMotion()) {
                continue;
            }
            Vec3 safeDestination = new Vec3(destination.x, blockPos.getY() + 1.0D, destination.z);
            if (!level().noCollision(this, getBoundingBox().move(safeDestination.subtract(position())))) {
                continue;
            }
            teleportTo(safeDestination.x, safeDestination.y, safeDestination.z);
            resetFallDistance();
            return true;
        }
        return false;
    }

    private void spawnPortalParticles() {
        for (int index = 0; index < 2; index++) {
            level().addParticle(ParticleTypes.PORTAL,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight() - 0.25D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    (random.nextDouble() - 0.5D) * 2.0D, -random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 2.0D);
        }
    }

    private final class HeadMeleeGoal extends MeleeAttackGoal {
        private HeadMeleeGoal() {
            super(AssimilatedHeadEntity.this, 1.3D, false);
        }

        @Override
        public void start() {
            super.start();
            updateMeleeStatus();
        }

        @Override
        public void tick() {
            super.tick();
            updateMeleeStatus();
        }

        @Override
        public void stop() {
            super.stop();
            if (getParasiteStatus() != 10) {
                setParasiteStatus(0);
            }
        }

        private void updateMeleeStatus() {
            if (getParasiteStatus() != 10) {
                setParasiteStatus(1);
            }
        }
    }

    public enum Kind {
        COW("sim_cowhead", 5.4D, 2.1D, 0.30D, 16.0D),
        ENDERMAN("sim_endermanhead", 16.5D, 3.3D, 0.30D, 32.0D),
        HORSE("sim_horsehead", 7.2D, 2.25D, 0.30D, 16.0D),
        HUMAN("sim_humanhead", 4.5D, 2.7D, 0.30D, 16.0D),
        PIG("sim_pighead", 2.7D, 1.05D, 0.30D, 16.0D),
        SHEEP("sim_sheephead", 3.9D, 1.8D, 0.30D, 16.0D),
        VILLAGER("sim_villagerhead", 4.8D, 3.0D, 0.30D, 16.0D),
        WOLF("sim_wolfhead", 3.0D, 3.15D, 0.34D, 16.0D);

        private final String id;
        private final double maxHealth;
        private final double attackDamage;
        private final double movementSpeed;
        private final double followRange;
        private final int experience;

        Kind(String id, double maxHealth, double attackDamage, double movementSpeed, double followRange) {
            this.id = id;
            this.maxHealth = maxHealth;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.followRange = followRange;
            this.experience = 4;
        }
    }
}
