package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.config.MobsConfig;
import alku.csrp.effect.EffectStacking;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

/**
 * Shared port of the original Pure-tier combatants. They retain the legacy
 * fire weakness and adaptive resistance while their enum branches implement
 * the individual melee, flying, summoning, and ranged roles.
 */
public final class PureParasiteEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Boolean> WARDEN_CHARGING = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> VIGILANTE_STATUS = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VIGILANTE_LEFT_TENDRIL = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VIGILANTE_RIGHT_TENDRIL = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> GRUNT_SKIN = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> OMBOO_FLAGS = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> OMBOO_SKIN = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> OMBOO_COMBAT_STATUS = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.INT);
    private static final int MAX_ADAPTATION_HITS = 8;
    private static final int MAX_LEARNABLE_DAMAGE_SOURCES = 12;
    private static final float ADAPTATION_PER_HIT = 0.125F;
    private static final float ADAPTATION_LEARN_CHANCE = 0.95F;
    private static final float FIRE_SUPPRESSION_CHANCE = 0.30F;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation RUN = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation FLY = IDLE;
    private final RawAnimation WARDEN_ATTACK = ParasiteAnimations.play(this, "get_attack_timer");
    private final RawAnimation WARDEN_AGE_STILL = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_still_ani_1");
    private final RawAnimation WARDEN_ATTACK_STILL = ParasiteAnimations.play(this,
            "get_attack_timer.get_still_ani_1");
    private final RawAnimation WARDEN_AGE_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation WARDEN_LIMB_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation WARDEN_ATTACK_STATUS_1 = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_1");
    private final RawAnimation WARDEN_AGE_STATUS_1_STILL = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation WARDEN_ATTACK_STATUS_1_STILL = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation WARDEN_LIMB_STATUS_2 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation WARDEN_AGE_STATUS_3 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3");
    private final RawAnimation WARDEN_LIMB_STATUS_3 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_3");
    private final RawAnimation WARDEN_ATTACK_STATUS_3 = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_3");
    private final RawAnimation WARDEN_AGE_STATUS_3_STILL = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1");
    private final RawAnimation WARDEN_ATTACK_STATUS_3_STILL = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_3.get_still_ani_1");
    private final RawAnimation WARDEN_AGE_STATUS_10 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");
    private final RawAnimation WARDEN_ATTACK_STATUS_10 = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_10");
    private final RawAnimation WARDEN_CHARGE_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3");
    private final RawAnimation WARDEN_CHARGE_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_3");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");
    private final RawAnimation VIGILANTE_ATTACK_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation VIGILANTE_ATTACK_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation VIGILANTE_ATTACK2_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation VIGILANTE_ATTACK2_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation VIGILANTE_UNDERGROUND = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_25");

    private final Kind kind;
    private final VigilanteTendrilPart leftTendrilPart;
    private final VigilanteTendrilPart rightTendrilPart;
    private final PartEntity<?>[] bodyParts;
    private int blockBreakCooldown;
    private int supportCooldown;
    private int attackAnimationTicks;
    private int scentCooldown = 800;
    private int seekerCreationPhase = -1;
    private boolean gruntSkillLeapActive;
    private int gruntSkillLeapTicks;
    private boolean gruntSkillLeapWasAirborne;
    private boolean deathBurstFired;

    public PureParasiteEntity(EntityType<? extends PureParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        if (kind == Kind.VIGILANTE) {
            leftTendrilPart = new VigilanteTendrilPart(this, true);
            rightTendrilPart = new VigilanteTendrilPart(this, false);
            bodyParts = new PartEntity<?>[]{leftTendrilPart, rightTendrilPart};
        } else {
            leftTendrilPart = null;
            rightTendrilPart = null;
            bodyParts = new PartEntity<?>[0];
        }
        xpReward = 75;
        if (kind == Kind.BOMBER_LIGHT) {
            moveControl = new OmbooMoveControl(this);
            setNoGravity(true);
        } else if (kind.flying) {
            moveControl = new FlyingMoveControl(this, 16, true);
            setNoGravity(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        AttributeSupplier.Builder attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind.knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, kind.followRange);
        if (kind.flying) {
            attributes.add(Attributes.FLYING_SPEED, kind.movementSpeed);
        }
        if (kind == Kind.MONARCH || kind == Kind.WARDEN) {
            attributes.add(Attributes.STEP_HEIGHT, 1.0D);
        }
        return attributes;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (isClimberType(getType())) {
            return new WallClimberNavigation(this, level);
        }
        return super.createNavigation(level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        switch (activeKind()) {
            case GRUNT -> {
                goalSelector.addGoal(0, new GruntSwimmingDivingGoal());
                goalSelector.addGoal(0, new GruntSkillLeapGoal());
                goalSelector.addGoal(2, new GruntWaterLeapGoal());
                goalSelector.addGoal(2, new GruntEvasiveDashGoal(20, 2, 4, 1.5D, 15));
                goalSelector.addGoal(3, new GruntAreaMeleeGoal());
            }
            case BOMBER_LIGHT -> {
                targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
                goalSelector.addGoal(3, new OmbooFlightTargetGoal());
                goalSelector.addGoal(4, new OmbooChargeAttackGoal());
                goalSelector.addGoal(4, new OmbooFlightLimitsGoal());
                goalSelector.addGoal(5, new LightBomberBombGoal());
                goalSelector.addGoal(6, new OmbooRandomFlightGoal());
            }
            case MONARCH -> {
                goalSelector.addGoal(1, new MonarchWebGoal());
                goalSelector.addGoal(2, new MonarchLeapGoal());
                goalSelector.addGoal(3, new MonarchChargeGoal());
                goalSelector.addGoal(4, new EvasiveDashGoal(100, 0.75D));
                goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.20D, false));
            }
            case OVERSEER -> {
                goalSelector.addGoal(1, new OverseerVolleyGoal());
                goalSelector.addGoal(2, new OverseerSummonGoal());
                goalSelector.addGoal(3, new FlightPursuitGoal(0.95D));
            }
            case SEEKER -> {
                goalSelector.addGoal(3, new FlightPursuitGoal(0.50D));
                goalSelector.addGoal(6, new SeekerRandomFlightGoal());
            }
            case VIGILANTE -> {
                goalSelector.addGoal(1, new VigilanteRangedGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.90D, false));
            }
            case WARDEN -> {
                goalSelector.addGoal(1, new WardenShockwaveGoal());
                goalSelector.addGoal(2, new WardenChargeGoal());
                goalSelector.addGoal(3, createAnimatedLeapGoal(0.75F, 30));
                goalSelector.addGoal(4, new EvasiveDashGoal(100, 0.70D));
                goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.10D, false));
            }
        }
    }

    @Override
    protected boolean usesDefaultMovementGoals() {
        return !activeKind().flying;
    }

    @Override
    protected boolean usesDefaultFloatGoal() {
        return activeKind() != Kind.GRUNT;
    }

    @Override
    protected boolean usesDefaultTargetGoals() {
        return activeKind() != Kind.BOMBER_LIGHT;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WARDEN_CHARGING, false);
        builder.define(VIGILANTE_STATUS, 0);
        builder.define(VIGILANTE_LEFT_TENDRIL, -1.0F);
        builder.define(VIGILANTE_RIGHT_TENDRIL, -1.0F);
        builder.define(GRUNT_SKIN, (byte) 0);
        builder.define(OMBOO_FLAGS, (byte) 0);
        builder.define(OMBOO_SKIN, (byte) 0);
        builder.define(OMBOO_COMBAT_STATUS, 0);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (!level.isClientSide() && activeKind() == Kind.GRUNT
                && (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase())) {
            entityData.set(GRUNT_SKIN, (byte) (5 + random.nextInt(3)));
        }
        if (!level.isClientSide() && activeKind() == Kind.BOMBER_LIGHT
                && (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase())) {
            setOmbooSkin(7);
        }
        return data;
    }

    @Override
    public void tick() {
        super.tick();
        Kind activeKind = activeKind();
        if (activeKind.flying) {
            setNoGravity(true);
        }
        updateVigilanteParts();
        if (level().isClientSide) {
            return;
        }
        if (activeKind == Kind.VIGILANTE) {
            initializeVigilanteTendrils();
        }
        if (blockBreakCooldown > 0) {
            blockBreakCooldown--;
        }
        if (supportCooldown > 0) {
            supportCooldown--;
        }
        if (attackAnimationTicks > 0) {
            attackAnimationTicks--;
        }
        tickGruntSkillLeap();
        if (activeKind == Kind.BOMBER_LIGHT) {
            tickOmbooFlightEnvironment();
        } else if (activeKind.flying && onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 4.0D, getZ(), 0.55D);
        }
        if (activeKind == Kind.SEEKER) {
            tickSeekerScent();
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        breakBlocksTowardsTarget(target, activeKind);
        if (activeKind != Kind.SEEKER && supportCooldown <= 0 && tickCount % 40 == 0) {
            trySummonSupport(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 4.0F;
        }
        boolean hurt = super.hurt(source, amount);
        return hurt;
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return MAX_ADAPTATION_HITS;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return ADAPTATION_PER_HIT;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return MAX_LEARNABLE_DAMAGE_SOURCES;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return ADAPTATION_LEARN_CHANCE;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return FIRE_SUPPRESSION_CHANCE;
    }

    @Override
    protected float damageAdaptationEffectiveness() {
        return switch (activeKind()) {
            case GRUNT, BOMBER_LIGHT, OVERSEER, WARDEN -> 0.95F;
            default -> 1.0F;
        };
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (!(entity instanceof LivingEntity target)) {
            return super.doHurtTarget(entity);
        }
        return switch (activeKind()) {
            case GRUNT, MONARCH, WARDEN -> performAreaMelee(target);
            default -> {
                boolean hurt = super.doHurtTarget(target);
                if (hurt) {
                    attackAnimationTicks = 8;
                    triggerAttackAnimation();
                    applyMeleeEffects(target, activeKind());
                }
                yield hurt;
            }
        };
    }

    @Override
    public void push(Entity entity) {
        if (!level().isClientSide && activeKind() == Kind.GRUNT && getGruntSkin() == 5
                && entity instanceof LivingEntity living && living != this && !(living instanceof Parasite)) {
            EffectStacking.apply(living, ModMobEffects.VIRAL, 40, 0);
        }
        super.push(entity);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDefaultDimensions(pose);
        return switch (activeKind()) {
            case GRUNT -> dimensions.withEyeHeight(1.73F);
            case BOMBER_LIGHT -> dimensions.withEyeHeight(2.4F);
            default -> dimensions;
        };
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (activeKind() == Kind.BOMBER_LIGHT && entityData.get(OMBOO_COMBAT_STATUS) != 0) {
            return ModSounds.get("mob.silence");
        }
        return super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (activeKind() == Kind.BOMBER_LIGHT && random.nextBoolean() && getAdaptationHitStatus() > 0) {
            return ModSounds.get("mob.silence");
        }
        return super.getHurtSound(source);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (activeKind() == Kind.GRUNT) {
            playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
            return;
        }
        super.playStepSound(pos, state);
    }

    @Override
    protected float adjustBlockBreakHardness(float baseHardness) {
        return activeKind() == Kind.GRUNT && getGruntSkin() == 7 ? baseHardness * 2.0F : baseHardness;
    }

    @Override
    public boolean onClimbable() {
        if (activeKind() == Kind.WARDEN) {
            LivingEntity target = getTarget();
            if (target != null) {
                if (!hasLineOfSight(target) && distanceToSqr(target) < 100.0D) {
                    return super.onClimbable();
                }
                if (hasLineOfSight(target) && target.getY() + 1.0D < getY()) {
                    return super.onClimbable();
                }
            }
            return horizontalCollision || super.onClimbable();
        }
        return activeKind().climbs && horizontalCollision || super.onClimbable();
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !deathBurstFired && random.nextFloat() < 0.25F) {
            deathBurstFired = true;
            triggerPureDeathBurst();
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (activeKind() == Kind.VIGILANTE) {
            tag.putInt("VigilanteStatus", entityData.get(VIGILANTE_STATUS));
            tag.putFloat("VigilanteLeftTendril", entityData.get(VIGILANTE_LEFT_TENDRIL));
            tag.putFloat("VigilanteRightTendril", entityData.get(VIGILANTE_RIGHT_TENDRIL));
        }
        if (activeKind() == Kind.SEEKER) {
            tag.putInt("SeekerCreationPhase", seekerCreationPhase);
        }
        if (activeKind() == Kind.GRUNT) {
            tag.putByte("GruntSkin", entityData.get(GRUNT_SKIN));
        }
        if (activeKind() == Kind.BOMBER_LIGHT) {
            tag.putByte("OmbooSkin", entityData.get(OMBOO_SKIN));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (activeKind() == Kind.VIGILANTE && tag.contains("VigilanteStatus")) {
            entityData.set(VIGILANTE_STATUS, tag.getInt("VigilanteStatus"));
            entityData.set(VIGILANTE_LEFT_TENDRIL, tag.contains("VigilanteLeftTendril")
                    ? tag.getFloat("VigilanteLeftTendril") : -1.0F);
            entityData.set(VIGILANTE_RIGHT_TENDRIL, tag.contains("VigilanteRightTendril")
                    ? tag.getFloat("VigilanteRightTendril") : -1.0F);
        }
        if (activeKind() == Kind.SEEKER) {
            seekerCreationPhase = tag.contains("SeekerCreationPhase")
                    ? tag.getInt("SeekerCreationPhase") : -1;
        }
        if (activeKind() == Kind.GRUNT) {
            setGruntSkin(tag.contains("GruntSkin") ? tag.getByte("GruntSkin") : 0);
        }
        if (activeKind() == Kind.BOMBER_LIGHT) {
            setOmbooSkin(tag.contains("OmbooSkin") ? tag.getByte("OmbooSkin") : 0);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        if (activeKind() == Kind.WARDEN) {
            controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                    .triggerableAnim("get_attack_timer", WARDEN_ATTACK));
        }
    }

    public Kind getKind() {
        return activeKind();
    }

    public int getGruntSkin() {
        return activeKind() == Kind.GRUNT ? entityData.get(GRUNT_SKIN) : 0;
    }

    private void setGruntSkin(int skin) {
        entityData.set(GRUNT_SKIN, (byte) (skin >= 5 && skin <= 7 ? skin : 0));
    }

    public int getOmbooSkin() {
        return activeKind() == Kind.BOMBER_LIGHT ? entityData.get(OMBOO_SKIN) : 0;
    }

    private void setOmbooSkin(int skin) {
        entityData.set(OMBOO_SKIN, (byte) (skin == 7 ? 7 : 0));
    }

    public boolean isOmbooCharging() {
        return activeKind() == Kind.BOMBER_LIGHT && (entityData.get(OMBOO_FLAGS) & 1) != 0;
    }

    private void setOmbooCharging(boolean charging) {
        byte flags = entityData.get(OMBOO_FLAGS);
        entityData.set(OMBOO_FLAGS, charging ? (byte) (flags | 1) : (byte) (flags & ~1));
    }

    @Override
    public boolean isMultipartEntity() {
        return bodyParts.length > 0;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int index = 0; index < bodyParts.length; index++) {
            bodyParts[index].setId(id + index + 1);
        }
    }

    @Override
    public PartEntity<?>[] getParts() {
        return bodyParts;
    }

    public boolean isLeftVigilanteTendrilAttached() {
        return activeKind() != Kind.VIGILANTE || entityData.get(VIGILANTE_LEFT_TENDRIL) != 0.0F;
    }

    public boolean isRightVigilanteTendrilAttached() {
        return activeKind() != Kind.VIGILANTE || entityData.get(VIGILANTE_RIGHT_TENDRIL) != 0.0F;
    }

    private void initializeVigilanteTendrils() {
        float health = getMaxHealth() * 0.4F;
        if (entityData.get(VIGILANTE_LEFT_TENDRIL) < 0.0F) {
            entityData.set(VIGILANTE_LEFT_TENDRIL, health);
        }
        if (entityData.get(VIGILANTE_RIGHT_TENDRIL) < 0.0F) {
            entityData.set(VIGILANTE_RIGHT_TENDRIL, health);
        }
    }

    private boolean hurtVigilanteTendril(boolean left, DamageSource source, float amount) {
        if (!hurt(source, amount)) {
            return false;
        }
        EntityDataAccessor<Float> data = left ? VIGILANTE_LEFT_TENDRIL : VIGILANTE_RIGHT_TENDRIL;
        float previous = entityData.get(data);
        if (previous <= 0.0F) {
            return false;
        }
        float remaining = Math.max(0.0F, previous - amount);
        entityData.set(data, remaining);
        if (remaining == 0.0F) {
            spawnVigilanteTendril(left);
            reduceAllResistances(Math.max(1, maxDamageAdaptationHits() / 2));
            playSound(ModSounds.get("mob.tendril"), 2.0F, 0.8F);
        }
        return true;
    }

    private void spawnVigilanteTendril(boolean left) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        TendrilEntity tendril = ModEntities.TENDRIL.get().create(serverLevel);
        if (tendril == null) {
            return;
        }
        double side = left ? 1.0D : -1.0D;
        double yaw = Math.toRadians(getYRot());
        tendril.setSkin(TendrilEntity.ANGED);
        tendril.moveTo(getX() + side * Math.cos(yaw) * 1.1D,
                getY() + 2.3D,
                getZ() + side * Math.sin(yaw) * 1.1D,
                getYRot(), 0.0F);
        serverLevel.addFreshEntity(tendril);
    }

    private void updateVigilanteParts() {
        if (leftTendrilPart != null) {
            leftTendrilPart.updatePosition();
        }
        if (rightTendrilPart != null) {
            rightTendrilPart.updatePosition();
        }
    }

    public int getVigilanteStatus() {
        return entityData.get(VIGILANTE_STATUS);
    }

    public void setVigilanteStatus(int status) {
        entityData.set(VIGILANTE_STATUS, status);
    }

    private PlayState movementAnimation(AnimationState<PureParasiteEntity> state) {
        if (isSpecialLeapAnimating()
                && (activeKind() == Kind.GRUNT || activeKind() == Kind.MONARCH || activeKind() == Kind.WARDEN)) {
            return state.setAndContinue(LEAP);
        }
        if (activeKind() == Kind.WARDEN && entityData.get(WARDEN_CHARGING)) {
            return state.setAndContinue(ParasiteAnimations.isMoving(this, state.isMoving()) ? WARDEN_CHARGE_WALK : WARDEN_CHARGE_IDLE);
        }
        if (activeKind() == Kind.VIGILANTE) {
            int status = entityData.get(VIGILANTE_STATUS);
            boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
            return switch (status) {
                case 1 -> state.setAndContinue(moving ? VIGILANTE_ATTACK_WALK : VIGILANTE_ATTACK_IDLE);
                case 2 -> state.setAndContinue(moving ? VIGILANTE_ATTACK2_WALK : VIGILANTE_ATTACK2_IDLE);
                case 25 -> state.setAndContinue(VIGILANTE_UNDERGROUND);
                default -> state.setAndContinue(moving ? (getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK) : IDLE);
            };
        }
        if (activeKind() == Kind.BOMBER_LIGHT || activeKind() == Kind.OVERSEER
                || activeKind() == Kind.SEEKER) {
            return state.setAndContinue(FLY);
        }
        if (activeKind() == Kind.GRUNT) {
            if (ParasiteAnimations.isAttacking(this)) {
                return state.setAndContinue(VIGILANTE_ATTACK_WALK);
            }
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                return state.setAndContinue(IDLE);
            }
            return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
        }
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(IDLE);
        }
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
    }

    private boolean performAreaMelee(LivingEntity center) {
        double radius = activeKind() == Kind.WARDEN ? 2.6D : 2.0D;
        boolean gruntAttack = activeKind() == Kind.GRUNT;
        if (gruntAttack) {
            playSound(ModSounds.MOB_SWIPE.get(), 2.0F, 1.0F);
            triggerAttackAnimation();
        }
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), center.getBoundingBox().inflate(radius));
        boolean hit = false;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(radius), this::isValidParasiteTarget)) {
            if (!hasLineOfSight(target) || !super.doHurtTarget(target)) {
                continue;
            }
            hit = true;
            applyMeleeEffects(target, activeKind());
        }
        if (hit && !gruntAttack) {
            attackAnimationTicks = 10;
            triggerAttackAnimation();
        }
        return hit;
    }

    private void triggerAttackAnimation() {
        attackAnimationTicks = 10;
        swing(InteractionHand.MAIN_HAND);
        if (activeKind() == Kind.WARDEN) {
            triggerAnim("attack_controller", "get_attack_timer");
        }
    }

    private void applyMeleeEffects(LivingEntity target, Kind activeKind) {
        if (random.nextFloat() < 0.40F) {
            InfectionMechanics.applyCothEffect(target, this, 180, 0, false, false);
        }
        switch (activeKind) {
            case GRUNT -> {
                if (getGruntSkin() == 5) {
                    EffectStacking.apply(target, ModMobEffects.VIRAL, 40, 0);
                } else if (getGruntSkin() == 6) {
                    EffectStacking.apply(target, ModMobEffects.BLEED, 40, 0);
                }
            }
            case MONARCH -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, false), this);
            case VIGILANTE -> pushAway(target, 0.45D, 0.20D);
            case WARDEN -> pushAway(target, 0.70D, 0.55D);
            default -> {
            }
        }
    }

    private void pushAway(LivingEntity target, double horizontal, double vertical) {
        Vec3 direction = target.position().subtract(position());
        double length = Math.max(0.001D, direction.horizontalDistance());
        target.push(direction.x / length * horizontal, vertical, direction.z / length * horizontal);
    }

    private void breakBlocksTowardsTarget(LivingEntity target, Kind activeKind) {
        if (activeKind == Kind.GRUNT || activeKind == Kind.BOMBER_LIGHT
                || activeKind.blockHardness <= 0.0F || blockBreakCooldown > 0
                || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.001D) {
            return;
        }
        horizontal = horizontal.normalize();
        BlockPos origin = BlockPos.containing(getX() + horizontal.x * activeKind.blockRange,
                getY() + getBbHeight() * 0.5D, getZ() + horizontal.z * activeKind.blockRange);
        float maximumHardness = adjustBlockBreakHardness(activeKind.blockHardness);
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || hardness < 0.0F || hardness > maximumHardness) {
                continue;
            }
            if (ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this)) {
                blockBreakCooldown = 20;
            }
            return;
        }
    }

    private void trySummonSupport(LivingEntity target) {
        supportCooldown = 160;
        if (!(level() instanceof ServerLevel serverLevel) || random.nextInt(4) != 0) {
            return;
        }
        int seizers = level().getEntitiesOfClass(DeterrentParasiteEntity.class, getBoundingBox().inflate(32.0D),
                        entity -> entity.getKind() == DeterrentParasiteEntity.Kind.SEIZER)
                .size();
        if (seizers < 3 && random.nextBoolean()) {
            DeterrentParasiteEntity seizer = ModEntities.SEIZER.get().create(serverLevel);
            if (seizer == null) {
                return;
            }
            double angle = random.nextDouble() * Math.PI * 2.0D;
            seizer.moveTo(target.getX() + Math.cos(angle) * 3.0D, target.getY(),
                    target.getZ() + Math.sin(angle) * 3.0D, getYRot(), 0.0F);
            seizer.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(seizer.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            seizer.setTarget(target);
            serverLevel.addFreshEntity(seizer);
            return;
        }
        if (!hasLineOfSight(target) && distanceToSqr(target) > 64.0D) {
            DeterrentParasiteEntity dispatcher = ModEntities.DISPATCHERTEN.get().create(serverLevel);
            if (dispatcher == null) {
                return;
            }
            dispatcher.moveTo(target.getX(), target.getY(), target.getZ(), getYRot(), 0.0F);
            dispatcher.setDispatchTarget(this);
            dispatcher.setLifetimeTicks(0);
            serverLevel.addFreshEntity(dispatcher);
        }
    }

    private void tickSeekerScent() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (seekerCreationPhase < 0) {
            seekerCreationPhase = Config.evolutionPhase(serverLevel);
        }
        if (--scentCooldown >= 0 || tickCount % 21 != 10 || !Config.scentEnabled()
                || seekerCreationPhase < Config.scentDevelopmentLevel()
                || serverLevel.getEntities(ModEntities.SCENT.get(), scent -> true).size()
                        > Config.scentCap()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        ParasiticScentEntity scent = ModEntities.SCENT.get().create(serverLevel);
        if (scent == null) {
            return;
        }
        scent.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        scent.setTargetToKill(target, false);
        scent.setDieAfterKilling(true);
        scent.setCanFollow(true);
        serverLevel.addFreshEntity(scent);
        scentCooldown = 800;
    }

    private final class SeekerRandomFlightGoal extends Goal {
        private SeekerRandomFlightGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!getMoveControl().hasWanted()) {
                return true;
            }
            double x = getMoveControl().getWantedX() - getX();
            double y = getMoveControl().getWantedY() - getY();
            double z = getMoveControl().getWantedZ() - getZ();
            double distance = x * x + y * y + z * z;
            return distance < 1.0D || distance > 3600.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                getMoveControl().setWantedPosition(
                        getX() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F,
                        getY() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F,
                        getZ() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F, 0.5D);
                return;
            }

            BlockPos center = blockPosition();
            int mode = 1;
            double speed = 0.11D;
            double distance = distanceToSqr(target);
            if (distance > 400.0D) {
                center = target.blockPosition();
                mode = 2;
                speed += 0.11D;
            } else if (distance < 100.0D) {
                center = target.blockPosition();
                mode = 3;
                speed += 0.11D;
            }

            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos destination = switch (mode) {
                    case 2 -> center.offset(random.nextInt(6) - 2,
                            random.nextInt(7) - 2, random.nextInt(6) - 2);
                    case 3 -> center.offset(random.nextInt(4) + 3,
                            random.nextInt(5) + 4, random.nextInt(4) + 3);
                    default -> center.offset(random.nextInt(15) - 7,
                            random.nextInt(9) - 5, random.nextInt(15) - 7);
                };
                if (level().isEmptyBlock(destination)) {
                    getMoveControl().setWantedPosition(destination.getX() + 0.5D,
                            destination.getY() + 0.5D, destination.getZ() + 0.5D, speed);
                    return;
                }
            }
        }
    }

    private void triggerPureDeathBurst() {
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(2.0D));
        Level.ExplosionInteraction interaction = level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY() + getBbHeight() * 0.5D, getZ(), 2.0F, interaction);
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(3.0F);
        cloud.setDuration(80);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 220, 0, false, true));
        level().addFreshEntity(cloud);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    12, 0.75D, 0.75D, 0.75D, 0.02D);
        }
    }

    private void fireProjectile(LivingEntity target, ParasiteProjectileEntity.Mode mode, double speed,
                                float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(), mode);
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.55D));
        projectile.configure(this, mode, start, target.getEyePosition(), speed, damage, radius, lifetime, target);
        level().addFreshEntity(projectile);
    }

    private void fireWebProjectile(LivingEntity target, int webKind) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(), ParasiteProjectileEntity.Mode.WEB);
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.55D));
        projectile.configure(this, ParasiteProjectileEntity.Mode.WEB, start,
                target.getEyePosition(), 0.95D, 8.0F, 1.0D, 80, target);
        projectile.setWebKind(webKind);
        level().addFreshEntity(projectile);
    }

    private void fireBomb(LivingEntity target) {
        BombEntity bomb = ModEntities.BOMB.get().create(level());
        if (bomb == null) {
            return;
        }
        bomb.configure(this, 80, 1.0F, MobsConfig.ombooBombDamage(), 4, 0,
                MobsConfig.ombooGriefing());
        bomb.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        level().addFreshEntity(bomb);
    }

    private void tickOmbooFlightEnvironment() {
        if (tickCount % 21 != 10) {
            return;
        }
        if (onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.5D);
        }
        LivingEntity target = getTarget();
        if (target != null && (!level().getBlockState(blockPosition().below()).isAir()
                || !level().getBlockState(blockPosition().below(2)).isAir())) {
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(movement.x, 0.5D, movement.z);
        }
    }

    private boolean hasBlockBelow(int distance) {
        BlockPos.MutableBlockPos cursor = blockPosition().below().mutable();
        for (int offset = 1; offset <= distance && cursor.getY() >= level().getMinBuildHeight(); offset++) {
            if (!level().getBlockState(cursor).isAir()) {
                return true;
            }
            cursor.move(0, -1, 0);
        }
        return false;
    }

    private void spawnBuglins(LivingEntity target, int count) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int index = 0; index < count; index++) {
            BuglinEntity buglin = ModEntities.BUGLIN.get().create(serverLevel);
            if (buglin == null) {
                continue;
            }
            double angle = Math.PI * 2.0D * index / Math.max(1, count);
            buglin.moveTo(getX() + Math.cos(angle) * 1.5D, getY() + 0.2D,
                    getZ() + Math.sin(angle) * 1.5D, getYRot(), 0.0F);
            buglin.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(buglin.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            buglin.setTarget(target);
            serverLevel.addFreshEntity(buglin);
        }
    }

    private void spawnOverseerMinion(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Mob minion = random.nextFloat() < 0.66F
                ? ModEntities.GRUNT.get().create(serverLevel)
                : ModEntities.RUPTER.get().create(serverLevel);
        if (minion == null) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        minion.moveTo(target.getX() + Math.cos(angle) * 2.0D, target.getY(),
                target.getZ() + Math.sin(angle) * 2.0D, getYRot(), 0.0F);
        minion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(minion.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        minion.setTarget(target);
        minion.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 1, false, false), this);
        serverLevel.addFreshEntity(minion);
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        EntityType<?> type = getType();
        if (type == ModEntities.BOMBER_LIGHT.get()) return Kind.BOMBER_LIGHT;
        if (type == ModEntities.MONARCH.get()) return Kind.MONARCH;
        if (type == ModEntities.OVERSEER.get()) return Kind.OVERSEER;
        if (type == ModEntities.SEEKER.get()) return Kind.SEEKER;
        if (type == ModEntities.VIGILANTE.get()) return Kind.VIGILANTE;
        if (type == ModEntities.WARDEN.get()) return Kind.WARDEN;
        return Kind.GRUNT;
    }

    private void startGruntSkillLeap(LivingEntity target) {
        Vec3 offset = target.position().subtract(position());
        double horizontalLength = offset.horizontalDistance();
        if (horizontalLength <= 0.001D) {
            return;
        }
        getNavigation().stop();
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(movement.x + offset.x / horizontalLength * 3.5D * 0.9D + movement.x * 0.3D,
                1.1D,
                movement.z + offset.z / horizontalLength * 3.5D * 0.9D + movement.z * 0.3D);
        hurtMarked = true;
        gruntSkillLeapActive = true;
        gruntSkillLeapWasAirborne = false;
        gruntSkillLeapTicks = 0;
        startSpecialLeapAnimation(40);
    }

    private void tickGruntSkillLeap() {
        if (!gruntSkillLeapActive || activeKind() != Kind.GRUNT) {
            return;
        }
        gruntSkillLeapTicks++;
        if (!onGround()) {
            gruntSkillLeapWasAirborne = true;
        }
        if ((gruntSkillLeapWasAirborne && onGround()) || gruntSkillLeapTicks >= 80) {
            gruntSkillLeapActive = false;
            gruntSkillLeapTicks = 0;
        }
    }

    private static boolean isClimberType(EntityType<?> type) {
        return type == ModEntities.GRUNT.get() || type == ModEntities.MONARCH.get()
                || type == ModEntities.WARDEN.get();
    }

    private final class EvasiveDashGoal extends Goal {
        private final int interval;
        private final double speed;
        private int cooldown;

        private EvasiveDashGoal(int interval, double speed) {
            this.interval = interval;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && distanceToSqr(target) >= 9.0D
                    && distanceToSqr(target) <= 196.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            Vec3 toTarget = target.position().subtract(position());
            Vec3 strafe = new Vec3(-toTarget.z, 0.0D, toTarget.x);
            if (strafe.lengthSqr() > 0.001D) {
                strafe = strafe.normalize().scale(random.nextBoolean() ? speed : -speed);
                setDeltaMovement(strafe.x, 0.25D, strafe.z);
            }
            cooldown = interval;
        }
    }

    private final class GruntEvasiveDashGoal extends Goal {
        private final int cooldownTicks;
        private final double minimumDistanceSqr;
        private final double dashStrength;
        private final double maximumDistanceSqr;
        private int cooldown;

        private GruntEvasiveDashGoal(int cooldownTicks, int ignoredDurationTicks, int minimumDistance,
                                     double dashStrength, int maximumDistance) {
            this.cooldownTicks = cooldownTicks;
            this.minimumDistanceSqr = minimumDistance * minimumDistance;
            this.dashStrength = dashStrength;
            this.maximumDistanceSqr = maximumDistance * maximumDistance;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && onGround();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            cooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            double distance = distanceToSqr(target);
            if (distance > minimumDistanceSqr && distance < maximumDistanceSqr && hasLineOfSight(target)
                    && cooldown < cooldownTicks) {
                cooldown++;
            }
            if (cooldown < cooldownTicks) {
                return;
            }
            Vec3 towardTarget = target.position().subtract(position());
            double horizontalLength = towardTarget.horizontalDistance();
            if (horizontalLength <= 0.001D) {
                return;
            }
            double bonusX = random.nextBoolean() ? dashStrength : 0.0D;
            double bonusZ = bonusX == 0.0D ? dashStrength : 0.0D;
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(movement.x + towardTarget.x / horizontalLength * dashStrength * 0.8D
                            + movement.x * 0.2D + bonusX,
                    movement.y,
                    movement.z + towardTarget.z / horizontalLength * dashStrength * 0.8D
                            + movement.z * 0.2D + bonusZ);
            hurtMarked = true;
            getNavigation().stop();
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        41, getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.08D);
            }
            cooldown = 0;
        }
    }

    private final class GruntSwimmingDivingGoal extends Goal {
        private GruntSwimmingDivingGoal() {
            setFlags(EnumSet.of(Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!isInWaterOrBubble() && !isInLava()) {
                return false;
            }
            LivingEntity target = getTarget();
            if (target != null && (target.isInWaterOrBubble() || target.isInLava())
                    && target.distanceToSqr(getX(), target.getY(), getZ()) < 25.0D
                    && target.getY() - getY() < -1.0D) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.12D, 0.0D));
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (random.nextFloat() < 0.8F) {
                getJumpControl().jump();
            }
        }
    }

    private final class GruntWaterLeapGoal extends Goal {
        private int cooldown;

        private GruntWaterLeapGoal() {
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || (!isInWaterOrBubble() && !isInLava())) {
                return false;
            }
            if (cooldown < 20) {
                cooldown++;
                return false;
            }
            return onGround();
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            Vec3 offset = target.position().subtract(position());
            double horizontalLength = offset.horizontalDistance();
            if (horizontalLength > 0.001D) {
                double heightBonus = Math.max(0.0D, (target.getY() - getY()) * 0.07D);
                Vec3 movement = getDeltaMovement();
                setDeltaMovement(movement.x + offset.x / horizontalLength * 1.5D * 0.9D + movement.x * 0.3D,
                        0.7D + heightBonus,
                        movement.z + offset.z / horizontalLength * 1.5D * 0.9D + movement.z * 0.3D);
                hurtMarked = true;
                startSpecialLeapAnimation(24);
            }
            cooldown = 0;
        }
    }

    private final class GruntSkillLeapGoal extends Goal {
        private int chargeTicks;

        private GruntSkillLeapGoal() {
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return !gruntSkillLeapActive && target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return !gruntSkillLeapActive && target != null && target.isAlive();
        }

        @Override
        public void start() {
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            double distance = distanceToSqr(target);
            if (hasLineOfSight(target) && distance >= 100.0D && distance < 10_000.0D) {
                chargeTicks++;
            }
            if (chargeTicks >= 40 && onGround() && !hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                chargeTicks = 0;
                startGruntSkillLeap(target);
            }
        }
    }

    private final class GruntAreaMeleeGoal extends Goal {
        private int attackCooldown;

        private GruntAreaMeleeGoal() {
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
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            if (distanceToSqr(target) <= 9.0D && hasLineOfSight(target)) {
                getNavigation().stop();
                if (attackCooldown == 0) {
                    performAreaMelee(target);
                    attackCooldown = 20;
                }
            } else {
                getNavigation().moveTo(target, 1.5D);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    private final class MonarchWebGoal extends Goal {
        private int cooldown;

        private MonarchWebGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && hasLineOfSight(target) && distanceToSqr(target) >= 16.0D
                    && distanceToSqr(target) <= 400.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                fireWebProjectile(target, 1);
                triggerAttackAnimation();
                cooldown = 70;
            }
        }
    }

    private final class MonarchLeapGoal extends Goal {
        private int cooldown;

        private MonarchLeapGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && distanceToSqr(target) >= 25.0D
                    && distanceToSqr(target) <= 196.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            leapTowards(target, 0.75D, 0.62D);
            startSpecialLeapAnimation(30);
            spawnBuglins(target, 5);
            triggerAttackAnimation();
            cooldown = 220;
        }
    }

    private final class MonarchChargeGoal extends Goal {
        private int cooldown;

        private MonarchChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && distanceToSqr(target) >= 9.0D
                    && distanceToSqr(target) <= 100.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            leapTowards(target, 1.05D, 0.18D);
            spawnBuglins(target, 3);
            triggerAttackAnimation();
            cooldown = 180;
        }
    }

    private void leapTowards(LivingEntity target, double horizontalSpeed, double verticalSpeed) {
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() <= 0.001D) {
            return;
        }
        horizontal = horizontal.normalize().scale(horizontalSpeed);
        setDeltaMovement(horizontal.x, verticalSpeed, horizontal.z);
    }

    private static final class OmbooMoveControl extends MoveControl {
        private OmbooMoveControl(PureParasiteEntity mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            double x = wantedX - mob.getX();
            double y = wantedY - mob.getY();
            double z = wantedZ - mob.getZ();
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance < mob.getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5D));
                return;
            }
            mob.setDeltaMovement(mob.getDeltaMovement().add(
                    x / distance * 0.05D * speedModifier,
                    y / distance * 0.05D * speedModifier,
                    z / distance * 0.05D * speedModifier));
            LivingEntity target = mob.getTarget();
            double lookX = target == null ? mob.getDeltaMovement().x : target.getX() - mob.getX();
            double lookZ = target == null ? mob.getDeltaMovement().z : target.getZ() - mob.getZ();
            mob.setYRot(-((float) Mth.atan2(lookX, lookZ)) * Mth.RAD_TO_DEG);
            mob.yBodyRot = mob.getYRot();
        }
    }

    private final class OmbooFlightTargetGoal extends Goal {
        private int lostTargetTicks;

        @Override
        public boolean canUse() {
            int cycleTick = tickCount % 21;
            return cycleTick > 0 && cycleTick <= 10;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                validateTarget(target);
                return;
            }
            lostTargetTicks = 0;
            entityData.set(OMBOO_COMBAT_STATUS, 0);
            double followRange = getAttributeValue(Attributes.FOLLOW_RANGE);
            AABB searchArea = new AABB(blockPosition()).inflate(followRange);
            for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, searchArea)) {
                if (candidate instanceof Player player) {
                    if (isValidParasiteTarget(player) && !player.getAbilities().instabuild
                            && !player.isSpectator() && canAttack(player)) {
                        setTarget(player);
                        return;
                    }
                } else if (Config.mobAttackingEnabled() && candidate instanceof Mob
                        && !(candidate instanceof Animal)
                        && !(candidate instanceof Creeper) && !(candidate instanceof WaterAnimal)
                        && isAllowedByMobAttackingList(candidate)
                        && isValidParasiteTarget(candidate) && distanceToSqr(candidate) < 1024.0D
                        && hasLineOfSight(candidate) && canAttack(candidate)) {
                    setTarget(candidate);
                    entityData.set(OMBOO_COMBAT_STATUS, 1);
                    return;
                }
            }
        }

        private boolean isAllowedByMobAttackingList(LivingEntity candidate) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType()).toString();
            boolean listed = Config.mobAttackingBlacklist().stream().anyMatch(id::contains);
            return Config.mobAttackingBlacklistInverted() ? listed : !listed;
        }

        private void validateTarget(LivingEntity target) {
            if (!isValidParasiteTarget(target)
                    || target instanceof Player player
                    && (player.getAbilities().instabuild || player.isSpectator())) {
                clearTarget();
                return;
            }
            double followRange = getAttributeValue(Attributes.FOLLOW_RANGE);
            if (!hasLineOfSight(target) || distanceToSqr(target) >= followRange * followRange) {
                lostTargetTicks++;
            } else {
                lostTargetTicks = 0;
            }
            if (lostTargetTicks >= 6) {
                clearTarget();
            }
        }

        private void clearTarget() {
            setTarget(null);
            setOmbooCharging(false);
            entityData.set(OMBOO_COMBAT_STATUS, 0);
            lostTargetTicks = 0;
            getMoveControl().setWantedPosition(getX(), getY(), getZ(), 1.0D);
        }
    }

    private final class OmbooChargeAttackGoal extends Goal {
        private OmbooChargeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && random.nextInt(7) == 0
                    && distanceToSqr(target) > 3.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return getMoveControl().hasWanted() && isOmbooCharging()
                    && target != null && target.isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            Vec3 eye = target.getEyePosition();
            getMoveControl().setWantedPosition(eye.x, eye.y + 10.0D, eye.z, 1.0D);
            setOmbooCharging(true);
        }

        @Override
        public void stop() {
            setOmbooCharging(false);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }
            if (getBoundingBox().intersects(target.getBoundingBox())) {
                doHurtTarget(target);
                setOmbooCharging(false);
            } else if (distanceToSqr(target) < 9.0D) {
                Vec3 eye = target.getEyePosition();
                getMoveControl().setWantedPosition(eye.x, eye.y + 10.0D, eye.z, 1.0D);
            }
        }
    }

    private final class OmbooFlightLimitsGoal extends Goal {
        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            double verticalAdjustment = 0.0D;
            int configuredLimit = MobsConfig.ombooMaxY();
            if (configuredLimit != 256 && shouldPushOmbooDown(configuredLimit, target)) {
                verticalAdjustment -= 0.04D;
            }
            if (shouldPushOmbooDown(20, target)) {
                verticalAdjustment -= 0.04D;
            }
            if (hasBlockBelow(7)) {
                verticalAdjustment += 0.04D;
            }
            if (verticalAdjustment != 0.0D) {
                setDeltaMovement(getDeltaMovement().add(0.0D, verticalAdjustment, 0.0D));
            }
        }

        private boolean shouldPushOmbooDown(int limit, @Nullable LivingEntity target) {
            return target == null ? !hasBlockBelow(limit) : target.getY() + limit > getY();
        }
    }

    private final class OmbooRandomFlightGoal extends Goal {
        private OmbooRandomFlightGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !getMoveControl().hasWanted() && random.nextInt(7) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos origin = blockPosition();
            int mode = 1;
            double speed = 0.2D;
            LivingEntity target = getTarget();
            if (target != null) {
                double distance = distanceToSqr(target);
                if (distance > 100.0D) {
                    origin = target.blockPosition();
                    mode = 2;
                    speed += 0.1D;
                } else if (distance < 36.0D) {
                    origin = target.blockPosition();
                    mode = 3;
                    speed += 0.1D;
                }
            }
            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos destination = switch (mode) {
                    case 2 -> origin.offset(random.nextInt(6) - 2,
                            random.nextInt(7) - 2, random.nextInt(6) - 2);
                    case 3 -> origin.offset(random.nextInt(4) + 3,
                            random.nextInt(5) + 4, random.nextInt(4) + 3);
                    default -> origin.offset(random.nextInt(15) - 7,
                            random.nextInt(11) - 5, random.nextInt(15) - 7);
                };
                if (!level().getBlockState(destination).isAir()) {
                    continue;
                }
                getMoveControl().setWantedPosition(destination.getX() + 0.5D,
                        destination.getY() + 1.0D, destination.getZ() + 0.5D, speed);
                if (target == null) {
                    getLookControl().setLookAt(destination.getX() + 0.5D,
                            destination.getY() + 1.0D, destination.getZ() + 0.5D,
                            180.0F, 20.0F);
                }
                return;
            }
        }
    }

    private final class FlightPursuitGoal extends Goal {
        private final double speed;
        private int contactCooldown;

        private FlightPursuitGoal(double speed) {
            this.speed = speed;
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
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double heightOffset = activeKind() == Kind.OVERSEER ? 2.5D : 4.0D;
            getMoveControl().setWantedPosition(target.getX(), target.getY() + heightOffset, target.getZ(), speed);
            if (contactCooldown > 0) {
                contactCooldown--;
            } else if (distanceToSqr(target) <= 9.0D) {
                doHurtTarget(target);
                contactCooldown = 20;
            }
        }
    }

    private final class LightBomberBombGoal extends Goal {
        private int checkTicks;

        @Override
        public boolean canUse() {
            checkTicks++;
            if (checkTicks < 15) {
                return false;
            }
            checkTicks = 0;
            LivingEntity target = getTarget();
            if (target == null) {
                return false;
            }
            if (!target.onGround()) {
                checkTicks = 7;
                return false;
            }
            double x = target.getX() - getX();
            double z = target.getZ() - getZ();
            return x * x + z * z < 25.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                fireBomb(target);
            }
        }
    }

    private final class OverseerVolleyGoal extends Goal {
        private int cooldown;
        private int warmup;
        private int shots;
        private int shotDelay;

        private OverseerVolleyGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && hasLineOfSight(target) && distanceToSqr(target) <= 1024.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && shots < 6;
        }

        @Override
        public void start() {
            warmup = 10;
            shots = 0;
            shotDelay = 0;
            getNavigation().stop();
            triggerAttackAnimation();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (warmup > 0) {
                warmup--;
                return;
            }
            if (shotDelay > 0) {
                shotDelay--;
                return;
            }
            fireProjectile(target, ParasiteProjectileEntity.Mode.NEEDLE, 0.90D, 30.0F, 1.6D, 70);
            shots++;
            shotDelay = 4;
        }

        @Override
        public void stop() {
            cooldown = 160;
        }
    }

    private final class OverseerSummonGoal extends Goal {
        private int cooldown;
        private int chargeTicks;

        private OverseerSummonGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.onGround() && distanceToSqr(target) <= 1024.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return chargeTicks < 60 && getTarget() != null;
        }

        @Override
        public void start() {
            chargeTicks = 0;
            getNavigation().stop();
            triggerAttackAnimation();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            chargeTicks++;
            if (chargeTicks % 20 == 0) {
                spawnOverseerMinion(target);
                level().addParticle(ParticleTypes.WITCH, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        0.0D, 0.05D, 0.0D);
            }
        }

        @Override
        public void stop() {
            cooldown = 200;
        }
    }

    private final class VigilanteRangedGoal extends Goal {
        private int cooldown;
        private int shots;
        private int shotDelay;
        private int warmupTicks;

        private VigilanteRangedGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && hasLineOfSight(target) && distanceToSqr(target) >= 36.0D
                    && distanceToSqr(target) <= 1024.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && shots < 3;
        }

        @Override
        public void start() {
            shots = 0;
            shotDelay = 0;
            warmupTicks = 0;
            getNavigation().stop();
            triggerAttackAnimation();
            entityData.set(VIGILANTE_STATUS, 1);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (warmupTicks < 10) {
                warmupTicks++;
                return;
            }
            if (shotDelay > 0) {
                shotDelay--;
                return;
            }
            fireProjectile(target, ParasiteProjectileEntity.Mode.ACID, 0.80D, 27.0F, 2.25D, 90);
            shots++;
            shotDelay = 8;
            if (shots >= 3) {
                entityData.set(VIGILANTE_STATUS, 2);
            }
        }

        @Override
        public void stop() {
            cooldown = 80;
            entityData.set(VIGILANTE_STATUS, 0);
        }
    }

    private final class WardenChargeGoal extends Goal {
        private int cooldown;
        private int chargeTicks;
        private int dashTicks;

        private WardenChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && distanceToSqr(target) >= 25.0D
                    && distanceToSqr(target) <= 225.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && dashTicks < 18;
        }

        @Override
        public void start() {
            chargeTicks = 0;
            dashTicks = 0;
            getNavigation().stop();
            entityData.set(WARDEN_CHARGING, true);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (chargeTicks < 20) {
                chargeTicks++;
                getNavigation().stop();
                level().addParticle(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        0.0D, 0.03D, 0.0D);
                return;
            }
            Vec3 direction = target.position().subtract(position());
            Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
            if (horizontal.lengthSqr() > 0.001D) {
                horizontal = horizontal.normalize().scale(1.10D);
                setDeltaMovement(horizontal.x, 0.12D, horizontal.z);
            }
            dashTicks++;
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(1.5D));
            for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(1.5D), PureParasiteEntity.this::isValidParasiteTarget)) {
                if (victim.hurt(damageSources().mobAttack(PureParasiteEntity.this),
                        (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25F)) {
                    pushAway(victim, 1.10D, 0.85D);
                }
            }
        }

        @Override
        public void stop() {
            cooldown = 220;
            entityData.set(WARDEN_CHARGING, false);
        }
    }

    private final class WardenShockwaveGoal extends Goal {
        private int cooldown;
        private int chargeTicks;

        private WardenShockwaveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && hasLineOfSight(target) && distanceToSqr(target) >= 36.0D
                    && distanceToSqr(target) <= 400.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return chargeTicks < 40 && getTarget() != null;
        }

        @Override
        public void start() {
            chargeTicks = 0;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            getNavigation().stop();
            chargeTicks++;
            if (chargeTicks < 20) {
                level().addParticle(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        0.0D, 0.04D, 0.0D);
        } else if (chargeTicks == 20) {
                fireShockwave(target);
                triggerAttackAnimation();
            }
        }

        @Override
        public void stop() {
            cooldown = 240;
        }
    }

    private void fireShockwave(LivingEntity target) {
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() <= 0.001D) {
            return;
        }
        horizontal = horizontal.normalize();
        AABB shockwave = getBoundingBox().expandTowards(horizontal.scale(14.0D)).inflate(1.35D, 1.5D, 1.35D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), shockwave);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, shockwave,
                this::isValidParasiteTarget)) {
            if (victim.hurt(damageSources().mobAttack(this),
                    (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.80F)) {
                pushAway(victim, 0.80D, 1.15D);
            }
        }
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        for (int step = 1; step <= 12; step++) {
            BlockPos position = BlockPos.containing(getX() + horizontal.x * step, getY(), getZ() + horizontal.z * step);
            BlockState state = level().getBlockState(position);
            float hardness = state.getDestroySpeed(level(), position);
            if (!state.isAir() && !state.hasBlockEntity() && hardness >= 0.0F && hardness <= 5.0F) {
                ParasiteBlockInventory.collect((ServerLevel) level(), position, this);
            }
        }
    }

    private static final class VigilanteTendrilPart extends PartEntity<PureParasiteEntity> {
        private final boolean left;

        private VigilanteTendrilPart(PureParasiteEntity parent, boolean left) {
            super(parent);
            this.left = left;
        }

        private void updatePosition() {
            PureParasiteEntity parent = getParent();
            float yaw = parent.getYRot() * (float) Math.PI / 180.0F;
            float side = left ? 1.0F : -1.0F;
            setPos(parent.getX() + side * (float) Math.cos(yaw) * 1.1F,
                    parent.getY() + 2.3D,
                    parent.getZ() + side * (float) Math.sin(yaw) * 1.1F);
            setYRot(parent.getYRot());
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
            PureParasiteEntity parent = getParent();
            return parent.isAlive() && (left ? parent.isLeftVigilanteTendrilAttached()
                    : parent.isRightVigilanteTendrilAttached());
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            return getParent().hurtVigilanteTendril(left, source, amount);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(0.7F, 0.9F);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public Component getName() {
            return Component.literal(left ? "left_tendril" : "right_tendril");
        }
    }

    public enum Kind {
        GRUNT(false, true, 20.0D, 7.0D, 13.0D, 0.274172325D, 0.40D, 32.0D, 3.0F, 1.0D),
        BOMBER_LIGHT(true, false, 75.0D, 20.0D, 25.0D, 0.27D, 0.15D, 32.0D, 5.0F, 2.0D),
        MONARCH(false, true, 75.0D, 10.0D, 25.0D, 0.2775D, 1.0D, 32.0D, 5.0F, 4.0D),
        OVERSEER(true, false, 80.0D, 20.0D, 45.0D, 0.27D, 0.40D, 32.0D, 5.0F, 2.0D),
        SEEKER(true, false, 80.0D, 20.0D, 22.0D, 0.27D, 0.40D, 32.0D, 5.0F, 2.0D),
        VIGILANTE(false, false, 70.0D, 25.0D, 23.0D, 0.20D, 1.0D, 32.0D, 5.0F, 2.0D),
        WARDEN(false, true, 80.0D, 15.0D, 25.0D, 0.27D, 1.0D, 32.0D, 5.0F, 2.0D);

        private final boolean flying;
        private final boolean climbs;
        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final double movementSpeed;
        private final double knockbackResistance;
        private final double followRange;
        private final float blockHardness;
        private final double blockRange;

        Kind(boolean flying, boolean climbs, double maxHealth, double armor, double attackDamage,
             double movementSpeed, double knockbackResistance, double followRange,
             float blockHardness, double blockRange) {
            this.flying = flying;
            this.climbs = climbs;
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.knockbackResistance = knockbackResistance;
            this.followRange = followRange;
            this.blockHardness = blockHardness;
            this.blockRange = blockRange;
        }
    }
}
