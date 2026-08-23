package alku.csrp.entity;

import net.minecraftforge.event.ForgeEventFactory;

import net.minecraftforge.common.ForgeMod;
import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.Csrp;
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
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Shared port of the original Pure-tier combatants. They retain the legacy
 * fire weakness and adaptive resistance while their enum branches implement
 * the individual melee, flying, summoning, and ranged roles.
 */
public final class PureParasiteEntity extends PrimitiveParasiteEntity
        implements SummonCapacityOwner, ManualVariantProvider {
    private static final EntityDataAccessor<Boolean> WARDEN_CHARGING = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> WARDEN_STATUS = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> WARDEN_SKIN = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> VIGILANTE_STATUS = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> VIGILANTE_SKIN = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> VIGILANTE_LEFT_TENDRIL = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VIGILANTE_RIGHT_TENDRIL = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> GRUNT_SKIN = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> MONARCH_SKIN = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> MONARCH_COMBAT_STATUS = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> OMBOO_FLAGS = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> OMBOO_SKIN = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> OMBOO_COMBAT_STATUS = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> OVERSEER_SKIN = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> OVERSEER_SUMMONING = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BOOLEAN);
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
    private final WardenTendrilPart wardenLeftTendrilPart;
    private final WardenTendrilPart wardenRightTendrilPart;
    private final OverseerHeadPart overseerHeadPart;
    private final PartEntity<?>[] bodyParts;
    private final SummonCapacityTracker summonTracker = new SummonCapacityTracker();
    private int blockBreakCooldown;
    private int supportCooldown;
    private int attackAnimationTicks;
    private int scentCooldown = 800;
    private int seekerCreationPhase = -1;
    private boolean gruntSkillLeapActive;
    private int gruntSkillLeapTicks;
    private boolean gruntSkillLeapWasAirborne;
    private boolean monarchSkillLeapActive;
    private int monarchSkillLeapTicks;
    private boolean monarchSkillLeapWasAirborne;
    private boolean monarchWaterLeapActive;
    private boolean deathBurstFired;
    private boolean vigilanteMeleeMode = true;
    private int wardenLeapTicks;
    private double wardenLeapTargetX;
    private double wardenLeapTargetZ;

    public PureParasiteEntity(EntityType<? extends PureParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        if (kind == Kind.VIGILANTE) {
            leftTendrilPart = new VigilanteTendrilPart(this, true);
            rightTendrilPart = new VigilanteTendrilPart(this, false);
            wardenLeftTendrilPart = null;
            wardenRightTendrilPart = null;
            overseerHeadPart = null;
            bodyParts = new PartEntity<?>[]{leftTendrilPart, rightTendrilPart};
        } else if (kind == Kind.WARDEN) {
            leftTendrilPart = null;
            rightTendrilPart = null;
            wardenLeftTendrilPart = new WardenTendrilPart(this, true);
            wardenRightTendrilPart = new WardenTendrilPart(this, false);
            overseerHeadPart = null;
            bodyParts = new PartEntity<?>[]{wardenLeftTendrilPart, wardenRightTendrilPart};
        } else if (kind == Kind.OVERSEER) {
            leftTendrilPart = null;
            rightTendrilPart = null;
            wardenLeftTendrilPart = null;
            wardenRightTendrilPart = null;
            overseerHeadPart = new OverseerHeadPart(this);
            bodyParts = new PartEntity<?>[]{overseerHeadPart};
        } else {
            leftTendrilPart = null;
            rightTendrilPart = null;
            wardenLeftTendrilPart = null;
            wardenRightTendrilPart = null;
            overseerHeadPart = null;
            bodyParts = new PartEntity<?>[0];
        }
        xpReward = 75;
        if (kind == Kind.BOMBER_LIGHT) {
            moveControl = new OmbooMoveControl(this);
            setNoGravity(true);
        } else if (kind == Kind.OVERSEER) {
            moveControl = new OverseerMoveControl(this);
            setNoGravity(true);
        } else if (kind == Kind.SEEKER) {
            moveControl = new SeekerMoveControl(this);
            setNoGravity(true);
            noPhysics = true;
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
        if (kind == Kind.MONARCH || kind == Kind.VIGILANTE || kind == Kind.WARDEN) {
            attributes.add(ForgeMod.STEP_HEIGHT_ADDITION.get(), 1.0D);
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
                goalSelector.addGoal(0, new MonarchSkillLeapGoal());
                goalSelector.addGoal(0, new MonarchSwimmingDivingGoal());
                goalSelector.addGoal(6, new MonarchWebVolleyGoal());
                goalSelector.addGoal(2, new MonarchWaterLeapGoal());
                goalSelector.addGoal(3, new MonarchAreaMeleeGoal());
                goalSelector.addGoal(2, new MonarchEvasiveDashGoal(17, 2, 5, 3.5D, 15));
            }
            case OVERSEER -> {
                goalSelector.addGoal(1, new OverseerVolleyGoal());
                goalSelector.addGoal(2, new OverseerMeleeRushGoal());
                goalSelector.addGoal(3, new OverseerFlightLimitGoal());
                goalSelector.addGoal(5, new OverseerSummonGoal());
                goalSelector.addGoal(6, new OverseerRandomFlightGoal());
            }
            case SEEKER -> {
                goalSelector.addGoal(3, new FlightPursuitGoal(0.50D));
                goalSelector.addGoal(3, new OverseerFlightLimitGoal());
                goalSelector.addGoal(6, new SeekerRandomFlightGoal());
            }
            case VIGILANTE -> {
                goalSelector.addGoal(2, new VigilanteMeleeGoal());
                goalSelector.addGoal(4, new VigilanteRangedGoal());
                goalSelector.addGoal(6, new VigilanteRangeSwitchGoal());
            }
            case WARDEN -> {
                goalSelector.addGoal(0, new WardenLeapGoal());
                goalSelector.addGoal(0, new GruntSwimmingDivingGoal());
                goalSelector.addGoal(2, new WardenShockwaveGoal());
                goalSelector.addGoal(2, new WardenChargeGoal());
                goalSelector.addGoal(2, new GruntWaterLeapGoal());
                goalSelector.addGoal(2, new GruntEvasiveDashGoal(20, 2, 4, 3.0D, 15));
                goalSelector.addGoal(3, new WardenAreaMeleeGoal());
            }
        }
    }

    @Override
    protected boolean usesDefaultMovementGoals() {
        return !activeKind().flying;
    }

    @Override
    protected boolean usesDefaultFloatGoal() {
        return activeKind() != Kind.GRUNT && activeKind() != Kind.MONARCH;
    }

    @Override
    protected boolean usesDefaultTargetGoals() {
        return activeKind() != Kind.BOMBER_LIGHT;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(WARDEN_CHARGING, false);
        entityData.define(WARDEN_STATUS, 0);
        entityData.define(WARDEN_SKIN, (byte) 0);
        entityData.define(VIGILANTE_STATUS, 0);
        entityData.define(VIGILANTE_SKIN, (byte) 0);
        entityData.define(VIGILANTE_LEFT_TENDRIL, -1.0F);
        entityData.define(VIGILANTE_RIGHT_TENDRIL, -1.0F);
        entityData.define(GRUNT_SKIN, (byte) 0);
        entityData.define(MONARCH_SKIN, (byte) 0);
        entityData.define(MONARCH_COMBAT_STATUS, 0);
        entityData.define(OMBOO_FLAGS, (byte) 0);
        entityData.define(OMBOO_SKIN, (byte) 0);
        entityData.define(OMBOO_COMBAT_STATUS, 0);
        entityData.define(OVERSEER_SKIN, (byte) 0);
        entityData.define(OVERSEER_SUMMONING, false);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData, net.minecraft.nbt.CompoundTag spawnTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, spawnTag);
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
        if (!level.isClientSide() && activeKind() == Kind.MONARCH
                && (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase())) {
            setMonarchSkin(random.nextBoolean() ? 1 : 7);
            applyMonarchVariantAttributes();
            setHealth(getMaxHealth());
        }
        if (!level.isClientSide() && activeKind() == Kind.OVERSEER
                && (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase())) {
            setOverseerSkin(7);
        }
        if (!level.isClientSide() && activeKind() == Kind.VIGILANTE
                && (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase())) {
            setVigilanteSkin(7);
        }
        if (!level.isClientSide() && activeKind() == Kind.WARDEN
                && (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase())) {
            setWardenSkin(7);
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
        updateBodyParts();
        if (level().isClientSide) {
            return;
        }
        if (activeKind == Kind.VIGILANTE) {
            initializeVigilanteTendrils();
        }
        if (activeKind == Kind.OVERSEER && tickCount % 20 == 0
                && level() instanceof ServerLevel serverLevel) {
            summonTracker.prune(serverLevel);
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
        if (activeKind == Kind.MONARCH) {
            updateMonarchCombatStatus();
        }
        if (activeKind == Kind.BOMBER_LIGHT) {
            tickOmbooFlightEnvironment();
        } else if (activeKind == Kind.OVERSEER) {
            tickOverseerFlightEnvironment();
        } else if (activeKind == Kind.SEEKER && onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.5D);
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
        if (activeKind == Kind.MONARCH && tickCount % 61 == 20) {
            tryMonarchSummonSupport(target);
        } else if (activeKind != Kind.SEEKER && activeKind != Kind.OVERSEER
                && supportCooldown <= 0 && tickCount % 40 == 0) {
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
            case GRUNT, BOMBER_LIGHT, OVERSEER, SEEKER, WARDEN -> 0.95F;
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
            EffectStacking.apply(living, ModMobEffects.VIRAL.get(), 40, 0);
        }
        super.push(entity);
    }

    @Override
    public boolean applyScaryOrbEffect(LivingEntity target, int nearbyEntities) {
        boolean applied = super.applyScaryOrbEffect(target, nearbyEntities);
        if (applied && activeKind() == Kind.MONARCH) {
            ConfiguredOrbEffects.apply(this, target, nearbyEntities, MobsConfig.monarchOrbEffects());
        } else if (applied && activeKind() == Kind.VIGILANTE) {
            ConfiguredOrbEffects.apply(this, target, nearbyEntities, MobsConfig.vigilanteOrbEffects());
        } else if (applied && activeKind() == Kind.WARDEN) {
            ConfiguredOrbEffects.apply(this, target, nearbyEntities, MobsConfig.wardenOrbEffects());
        }
        return applied;
    }

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose pose) {
        return switch (activeKind()) {
            case GRUNT -> 1.73F;
            case BOMBER_LIGHT -> 2.4F;
            case MONARCH -> 3.5F;
            case OVERSEER -> 1.6F;
            case SEEKER -> 1.6F;
            case VIGILANTE -> 3.0F;
            case WARDEN -> 3.5F;
            default -> super.getEyeHeight(pose);
        };
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (activeKind() == Kind.BOMBER_LIGHT && entityData.get(OMBOO_COMBAT_STATUS) != 0) {
            return ModSounds.get("mob.silence");
        }
        if (activeKind() == Kind.MONARCH && entityData.get(MONARCH_COMBAT_STATUS) != 0) {
            return ModSounds.get("mob.silence");
        }
        if (activeKind() == Kind.WARDEN && getWardenStatus() != 0) {
            return ModSounds.get("mob.silence");
        }
        return super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if ((activeKind() == Kind.BOMBER_LIGHT || activeKind() == Kind.MONARCH)
                && random.nextBoolean() && getAdaptationHitStatus() > 0) {
            return ModSounds.get("mob.silence");
        }
        if ((activeKind() == Kind.SEEKER || activeKind() == Kind.VIGILANTE || activeKind() == Kind.WARDEN)
                && random.nextBoolean() && getAdaptationHitStatus() > 0) {
            return ModSounds.get("mob.silence");
        }
        return super.getHurtSound(source);
    }

    @Override
    protected float getSoundVolume() {
        return activeKind() == Kind.SEEKER ? 2.0F : super.getSoundVolume();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (activeKind() == Kind.GRUNT) {
            playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
            return;
        }
        if (activeKind() == Kind.MONARCH) {
            playSound(ModSounds.HEAVY_MULTIPLE_STEP.get(), 0.15F, 1.0F);
            return;
        }
        super.playStepSound(pos, state);
    }

    @Override
    protected float adjustBlockBreakHardness(float baseHardness) {
        if ((activeKind() == Kind.GRUNT && getGruntSkin() == 7)
                || (activeKind() == Kind.MONARCH && getMonarchSkin() == 7)
                || (activeKind() == Kind.WARDEN && getWardenSkin() == 7)) {
            return baseHardness * 2.0F;
        }
        return baseHardness;
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
            tag.putByte("VigilanteSkin", entityData.get(VIGILANTE_SKIN));
            tag.putFloat("VigilanteLeftTendril", entityData.get(VIGILANTE_LEFT_TENDRIL));
            tag.putFloat("VigilanteRightTendril", entityData.get(VIGILANTE_RIGHT_TENDRIL));
        }
        if (activeKind() == Kind.SEEKER) {
            tag.putInt("SeekerCreationPhase", seekerCreationPhase);
        }
        if (activeKind() == Kind.GRUNT) {
            tag.putByte("GruntSkin", entityData.get(GRUNT_SKIN));
        }
        if (activeKind() == Kind.MONARCH) {
            tag.putByte("MonarchSkin", entityData.get(MONARCH_SKIN));
        }
        if (activeKind() == Kind.BOMBER_LIGHT) {
            tag.putByte("OmbooSkin", entityData.get(OMBOO_SKIN));
        }
        if (activeKind() == Kind.OVERSEER) {
            tag.putByte("OverseerSkin", entityData.get(OVERSEER_SKIN));
            summonTracker.save(tag, "OverseerTrackedSummons");
        }
        if (activeKind() == Kind.WARDEN) {
            tag.putByte("WardenSkin", entityData.get(WARDEN_SKIN));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (activeKind() == Kind.VIGILANTE && tag.contains("VigilanteStatus")) {
            entityData.set(VIGILANTE_STATUS, tag.getInt("VigilanteStatus"));
            setVigilanteSkin(tag.contains("VigilanteSkin") ? tag.getByte("VigilanteSkin") : 0);
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
        if (activeKind() == Kind.MONARCH) {
            setMonarchSkin(tag.contains("MonarchSkin") ? tag.getByte("MonarchSkin") : 0);
            applyMonarchVariantAttributes();
        }
        if (activeKind() == Kind.BOMBER_LIGHT) {
            setOmbooSkin(tag.contains("OmbooSkin") ? tag.getByte("OmbooSkin") : 0);
        }
        if (activeKind() == Kind.OVERSEER) {
            setOverseerSkin(tag.contains("OverseerSkin") ? tag.getByte("OverseerSkin") : 0);
            summonTracker.load(tag, "OverseerTrackedSummons");
            entityData.set(OVERSEER_SUMMONING, false);
        }
        if (activeKind() == Kind.WARDEN) {
            setWardenSkin(tag.contains("WardenSkin") ? tag.getByte("WardenSkin") : 0);
            entityData.set(WARDEN_STATUS, 0);
            entityData.set(WARDEN_CHARGING, false);
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

    @Override
    public int getSummonCapacity() {
        return activeKind() == Kind.OVERSEER ? MobsConfig.overseerTotalActiveMobs() : 0;
    }

    @Override
    public int getUsedSummonCapacity() {
        return summonTracker.usedCapacity();
    }

    @Override
    public void reserveTrackedSummon(UUID entityId, int cost) {
        summonTracker.reserve(entityId, cost);
    }

    @Override
    public void replaceTrackedSummon(UUID previousId, UUID replacementId, int cost) {
        summonTracker.replace(previousId, replacementId, cost);
    }

    @Override
    public void releaseTrackedSummon(UUID entityId) {
        summonTracker.release(entityId);
    }

    public int getGruntSkin() {
        return activeKind() == Kind.GRUNT ? entityData.get(GRUNT_SKIN) : 0;
    }

    @Override
    public int getManualVariant() {
        return switch (activeKind()) {
            case GRUNT -> entityData.get(GRUNT_SKIN);
            case MONARCH -> entityData.get(MONARCH_SKIN);
            case BOMBER_LIGHT -> entityData.get(OMBOO_SKIN);
            case OVERSEER -> entityData.get(OVERSEER_SKIN);
            case VIGILANTE -> entityData.get(VIGILANTE_SKIN);
            case WARDEN -> entityData.get(WARDEN_SKIN);
            default -> 0;
        };
    }

    @Override
    public void setManualVariant(int variant) {
        byte skin = (byte) Mth.clamp(variant, 0, getMaxManualVariants() - 1);
        switch (activeKind()) {
            case GRUNT -> entityData.set(GRUNT_SKIN, skin);
            case MONARCH -> {
                entityData.set(MONARCH_SKIN, skin);
                applyMonarchVariantAttributes();
            }
            case BOMBER_LIGHT -> entityData.set(OMBOO_SKIN, skin);
            case OVERSEER -> entityData.set(OVERSEER_SKIN, skin);
            case VIGILANTE -> entityData.set(VIGILANTE_SKIN, skin);
            case WARDEN -> entityData.set(WARDEN_SKIN, skin);
            default -> {
            }
        }
    }

    private void setGruntSkin(int skin) {
        entityData.set(GRUNT_SKIN, (byte) (skin >= 5 && skin <= 7 ? skin : 0));
    }

    public int getMonarchSkin() {
        return activeKind() == Kind.MONARCH ? entityData.get(MONARCH_SKIN) : 0;
    }

    private void setMonarchSkin(int skin) {
        entityData.set(MONARCH_SKIN, (byte) (skin == 1 || skin == 7 ? skin : 0));
    }

    private void applyMonarchVariantAttributes() {
        if (activeKind() != Kind.MONARCH) {
            return;
        }
        double health = getMonarchSkin() == 1 ? Kind.MONARCH.maxHealth * 0.5D : Kind.MONARCH.maxHealth;
        double damage = getMonarchSkin() == 1 ? Kind.MONARCH.attackDamage * 1.5D : Kind.MONARCH.attackDamage;
        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        }
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        }
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    public void applyConfiguredAttributes() {
        Kind activeKind = activeKind();
        if (activeKind != Kind.OVERSEER && activeKind != Kind.SEEKER
                && activeKind != Kind.VIGILANTE && activeKind != Kind.WARDEN) {
            return;
        }
        double health = activeKind == Kind.OVERSEER || activeKind == Kind.SEEKER ? MobsConfig.overseerHealth()
                : activeKind == Kind.VIGILANTE ? MobsConfig.vigilanteHealth() : MobsConfig.wardenHealth();
        double armor = activeKind == Kind.OVERSEER || activeKind == Kind.SEEKER ? MobsConfig.overseerArmor()
                : activeKind == Kind.VIGILANTE ? MobsConfig.vigilanteArmor() : MobsConfig.wardenArmor();
        double damage = activeKind == Kind.OVERSEER || activeKind == Kind.SEEKER ? MobsConfig.overseerMeleeDamage()
                : activeKind == Kind.VIGILANTE ? MobsConfig.vigilanteMeleeDamage() : MobsConfig.wardenDamage();
        double knockbackResistance = activeKind == Kind.OVERSEER || activeKind == Kind.SEEKER
                ? MobsConfig.overseerKnockbackResistance()
                : activeKind == Kind.VIGILANTE ? MobsConfig.vigilanteKnockbackResistance()
                : MobsConfig.wardenKnockbackResistance();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        getAttribute(Attributes.ARMOR).setBaseValue(armor);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(knockbackResistance);
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

    public int getOverseerSkin() {
        return activeKind() == Kind.OVERSEER ? entityData.get(OVERSEER_SKIN) : 0;
    }

    private void setOverseerSkin(int skin) {
        entityData.set(OVERSEER_SKIN, (byte) (skin == 7 ? 7 : 0));
    }

    public boolean isOverseerSummoning() {
        return activeKind() == Kind.OVERSEER && entityData.get(OVERSEER_SUMMONING);
    }

    private void setOverseerSummoning(boolean summoning) {
        entityData.set(OVERSEER_SUMMONING, summoning);
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

    @Override
    public void remove(RemovalReason reason) {
        for (PartEntity<?> part : bodyParts) {
            if (!part.isRemoved()) {
                part.remove(reason);
            }
        }
        super.remove(reason);
    }

    public boolean isLeftVigilanteTendrilAttached() {
        return activeKind() != Kind.VIGILANTE || entityData.get(VIGILANTE_LEFT_TENDRIL) != 0.0F;
    }

    public boolean isRightVigilanteTendrilAttached() {
        return activeKind() != Kind.VIGILANTE || entityData.get(VIGILANTE_RIGHT_TENDRIL) != 0.0F;
    }

    private void initializeVigilanteTendrils() {
        float health = (float) (getMaxHealth() * Config.tendrilHealth());
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
            reduceAllResistances(Config.purePointDamageCap() / 2);
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

    private void updateBodyParts() {
        if (leftTendrilPart != null) {
            leftTendrilPart.updatePosition();
        }
        if (rightTendrilPart != null) {
            rightTendrilPart.updatePosition();
        }
        if (wardenLeftTendrilPart != null && !wardenLeftTendrilPart.isRemoved()) {
            wardenLeftTendrilPart.updatePosition();
        }
        if (wardenRightTendrilPart != null && !wardenRightTendrilPart.isRemoved()) {
            wardenRightTendrilPart.updatePosition();
        }
        if (overseerHeadPart != null) {
            overseerHeadPart.updatePosition();
        }
    }

    private boolean hurtWardenTendril(DamageSource source, float amount) {
        if (random.nextBoolean()) {
            EffectStacking.apply(this, ModMobEffects.BLEED.get(), 80, 0);
        }
        return hurt(source, amount * 3.0F);
    }

    public int getVigilanteStatus() {
        return entityData.get(VIGILANTE_STATUS);
    }

    public void setVigilanteStatus(int status) {
        entityData.set(VIGILANTE_STATUS, status);
    }

    public int getVigilanteSkin() {
        return activeKind() == Kind.VIGILANTE ? entityData.get(VIGILANTE_SKIN) : 0;
    }

    private void setVigilanteSkin(int skin) {
        entityData.set(VIGILANTE_SKIN, (byte) (skin == 7 ? 7 : 0));
    }

    public int getWardenStatus() {
        return activeKind() == Kind.WARDEN ? entityData.get(WARDEN_STATUS) : 0;
    }

    private void setWardenStatus(int status) {
        entityData.set(WARDEN_STATUS, activeKind() == Kind.WARDEN ? status : 0);
    }

    public int getWardenSkin() {
        return activeKind() == Kind.WARDEN ? entityData.get(WARDEN_SKIN) : 0;
    }

    private void setWardenSkin(int skin) {
        entityData.set(WARDEN_SKIN, (byte) (skin == 7 ? 7 : 0));
    }

    private PlayState movementAnimation(AnimationState<PureParasiteEntity> state) {
        if (isSpecialLeapAnimating()
                && (activeKind() == Kind.GRUNT || activeKind() == Kind.MONARCH || activeKind() == Kind.WARDEN)) {
            return state.setAndContinue(LEAP);
        }
        if (activeKind() == Kind.WARDEN && entityData.get(WARDEN_CHARGING)) {
            return state.setAndContinue(ParasiteAnimations.isMoving(this, state.isMoving()) ? WARDEN_CHARGE_WALK : WARDEN_CHARGE_IDLE);
        }
        if (activeKind() == Kind.WARDEN) {
            int status = getWardenStatus();
            boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
            if (ParasiteAnimations.isAttacking(this)) {
                return switch (status) {
                    case 1 -> state.setAndContinue(WARDEN_ATTACK_STATUS_1);
                    case 3 -> state.setAndContinue(WARDEN_ATTACK_STATUS_3);
                    case 10 -> state.setAndContinue(WARDEN_ATTACK_STATUS_10);
                    default -> state.setAndContinue(WARDEN_ATTACK);
                };
            }
            return switch (status) {
                case 1 -> state.setAndContinue(moving ? WARDEN_LIMB_STATUS_1 : WARDEN_AGE_STATUS_1_STILL);
                case 2 -> state.setAndContinue(moving ? WARDEN_LIMB_STATUS_2 : WARDEN_AGE_STILL);
                case 3 -> state.setAndContinue(moving ? WARDEN_LIMB_STATUS_3 : WARDEN_AGE_STATUS_3_STILL);
                case 10 -> state.setAndContinue(WARDEN_AGE_STATUS_10);
                default -> state.setAndContinue(moving ? WARDEN_LIMB_STATUS_2 : WARDEN_AGE_STILL);
            };
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
        boolean wardenAttack = activeKind() == Kind.WARDEN;
        if (gruntAttack || wardenAttack) {
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
        if (hit && !gruntAttack && !wardenAttack) {
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
                    EffectStacking.apply(target, ModMobEffects.VIRAL.get(), 40, 0);
                } else if (getGruntSkin() == 6) {
                    EffectStacking.apply(target, ModMobEffects.BLEED.get(), 40, 0);
                }
            }
            case MONARCH -> {
                target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.5D, 0.0D));
                target.hurtMarked = true;
            }
            case VIGILANTE -> target.knockback(1.0D, getX() - target.getX(), getZ() - target.getZ());
            case WARDEN -> maybeLaunchWardenTarget(target);
            default -> {
            }
        }
    }

    private void pushAway(LivingEntity target, double horizontal, double vertical) {
        Vec3 direction = target.position().subtract(position());
        double length = Math.max(0.001D, direction.horizontalDistance());
        target.push(direction.x / length * horizontal, vertical, direction.z / length * horizontal);
    }

    private void maybeLaunchWardenTarget(LivingEntity target) {
        if (random.nextFloat() >= 0.10F) {
            return;
        }
        Vec3 direction = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.horizontalDistanceSqr() < 1.0E-8D) {
            direction = new Vec3(random.nextDouble() - 0.5D, 0.0D, random.nextDouble() - 0.5D);
        }
        direction = direction.normalize();
        double vertical = target instanceof Player ? 0.525D : 1.05D;
        target.push(direction.x * 0.4D, vertical, direction.z * 0.4D);
        target.hurtMarked = true;
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
                    MobSpawnType.MOB_SUMMONED, null, null);
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

    private void updateMonarchCombatStatus() {
        LivingEntity target = getTarget();
        int status;
        if (monarchSkillLeapActive || monarchWaterLeapActive) {
            status = 10;
        } else if (target == null || !target.isAlive()) {
            status = 0;
        } else {
            status = distanceToSqr(target) > 64.0D ? 2 : 1;
        }
        entityData.set(MONARCH_COMBAT_STATUS, status);
    }

    private boolean monarchLeapBusy() {
        return monarchSkillLeapActive || monarchWaterLeapActive;
    }

    private void tryMonarchSummonSupport(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel) || hasLineOfSight(target)) {
            return;
        }
        double distance = distanceToSqr(target);
        boolean primaryBranch = (distance <= 64.0D || random.nextInt(3) != 0) && random.nextInt(10) != 0;
        MobEffectInstance slowness = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (primaryBranch && slowness != null && slowness.getAmplifier() == 2) {
            return;
        }

        boolean spawnSeizer = primaryBranch && random.nextInt(4) == 0;
        int range = spawnSeizer ? 3 : 5;
        int minimum = spawnSeizer ? 1 : 3;
        double offsetX = (random.nextInt(range) + minimum) * (random.nextBoolean() ? 1.0D : -1.0D);
        double offsetZ = (random.nextInt(range) + minimum) * (random.nextBoolean() ? 1.0D : -1.0D);
        double spawnX = target.getX() + offsetX;
        double spawnZ = target.getZ() + offsetZ;

        if (spawnSeizer) {
            long nearbySeizers = level().getEntitiesOfClass(DeterrentParasiteEntity.class,
                            target.getBoundingBox().inflate(16.0D), entity -> entity.getKind() == DeterrentParasiteEntity.Kind.SEIZER)
                    .size();
            if (nearbySeizers > 10) {
                return;
            }
            DeterrentParasiteEntity seizer = ModEntities.SEIZER.get().create(serverLevel);
            if (seizer == null) {
                return;
            }
            seizer.moveTo(spawnX, target.getY(), spawnZ, getYRot(), 0.0F);
            seizer.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(seizer.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null, null);
            seizer.setTarget(target);
            serverLevel.addFreshEntity(seizer);
            return;
        }

        DeterrentParasiteEntity dispatcher = ModEntities.DISPATCHERTEN.get().create(serverLevel);
        if (dispatcher != null) {
            dispatcher.moveTo(spawnX, target.getY(), spawnZ, getYRot(), 0.0F);
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
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH.get(), 220, 0, false, true));
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

    private void fireVigilanteProjectile(LivingEntity target) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(
                level(), ParasiteProjectileEntity.Mode.ANGED_BALL);
        if (projectile == null) {
            return;
        }
        Vec3 look = getViewVector(1.0F);
        Vec3 start = new Vec3(getX() + look.x, getY() + getEyeHeight() - 0.2D, getZ() + look.z);
        double accelerationY = target.getBoundingBox().minY + target.getBbHeight() / 4.0D
                - (1.0D + getY() + getBbHeight() / 2.0D);
        Vec3 acceleration = new Vec3(target.getX() - (getX() + look.x), accelerationY,
                target.getZ() - (getZ() + look.z));
        playSound(ModSounds.EMANA_SHOOTING.get(), 2.0F, 1.0F);
        projectile.configureLegacyFireball(this, ParasiteProjectileEntity.Mode.ANGED_BALL,
                start, acceleration, MobsConfig.vigilanteRangedDamage(), 0.0D, Integer.MAX_VALUE);
        level().addFreshEntity(projectile);
    }

    private void fireWebProjectile(LivingEntity target, int webKind) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(), ParasiteProjectileEntity.Mode.WEB);
        if (projectile == null) {
            return;
        }
        Vec3 look = getViewVector(1.0F);
        Vec3 start = new Vec3(getX() + look.x, getY() + getEyeHeight() - 0.2D, getZ() + look.z);
        double accelerationY = target.getBoundingBox().minY + target.getBbHeight() / 3.0D
                - (1.0D + getY() + getBbHeight() / 2.0D);
        Vec3 acceleration = new Vec3(target.getX() - (getX() + look.x), accelerationY,
                target.getZ() - (getZ() + look.z));
        projectile.configureLegacyFireball(this, ParasiteProjectileEntity.Mode.WEB, start,
                acceleration, 0.0F, 0.0D, 61);
        projectile.setWebKind(webKind);
        level().addFreshEntity(projectile);
        playSound(ModSounds.DORPA_RANGE.get(), 2.0F, 1.0F);
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

    private void tickOverseerFlightEnvironment() {
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
                    MobSpawnType.MOB_SUMMONED, null, null);
            buglin.setTarget(target);
            serverLevel.addFreshEntity(buglin);
        }
    }

    private void fireOverseerProjectile(LivingEntity target) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(),
                ParasiteProjectileEntity.Mode.ALAFHA_BALL);
        if (projectile == null) {
            return;
        }
        Vec3 look = getViewVector(1.0F);
        Vec3 start = new Vec3(getX() + look.x, getY() + getEyeHeight() - 0.2D, getZ() + look.z);
        double accelerationY = target.getBoundingBox().minY + target.getBbHeight() / 2.0D
                - (0.5D + getY() + getBbHeight() / 2.0D);
        Vec3 acceleration = new Vec3(target.getX() - (getX() + look.x), accelerationY,
                target.getZ() - (getZ() + look.z));
        projectile.configureLegacyFireball(this, ParasiteProjectileEntity.Mode.ALAFHA_BALL, start,
                acceleration, MobsConfig.overseerProjectileDamage(), 3.0D, 140);
        playSound(ModSounds.get("alafha.shooting"), 2.0F, 1.0F);
        level().addFreshEntity(projectile);
    }

    private boolean launchOverseerBiomass(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        List<BiomassEntity.SummonOption> options = resolveOverseerSummonOptions();
        java.util.Optional<BiomassEntity.SummonOption> selected = BiomassEntity.selectSummon(this, this, options);
        if (selected.isEmpty()) {
            return false;
        }
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(),
                ParasiteProjectileEntity.Mode.BIOMASS_BALL);
        if (projectile == null) {
            return false;
        }
        Vec3 look = getViewVector(1.0F);
        Vec3 start = new Vec3(getX() + look.x, getY() + getEyeHeight() - 0.2D, getZ() + look.z);
        double accelerationY = target.getBoundingBox().minY + target.getBbHeight() / 2.0D
                - (0.5D + getY() + getBbHeight() / 2.0D);
        Vec3 acceleration = new Vec3(target.getX() - (getX() + look.x), accelerationY,
                target.getZ() - (getZ() + look.z));
        BiomassEntity.SummonOption option = selected.get();
        projectile.configureBiomassBall(this, start, acceleration, option, 4, target);
        if (!serverLevel.addFreshEntity(projectile)) {
            return false;
        }
        reserveTrackedSummon(projectile.getUUID(), option.cost());
        serverLevel.sendParticles(ParticleTypes.WITCH, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                8, 0.45D, 0.45D, 0.45D, 0.04D);
        return true;
    }

    private List<BiomassEntity.SummonOption> resolveOverseerSummonOptions() {
        List<BiomassEntity.SummonOption> options = new ArrayList<>();
        for (String entry : MobsConfig.overseerSummonMobs()) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 3) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
            if (id == null) {
                continue;
            }
            if (id.getNamespace().equals("srparasites")) {
                id = new ResourceLocation(Csrp.MODID, id.getPath());
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (type == null) {
                continue;
            }
            try {
                double chance = Double.parseDouble(parts[1].trim());
                int cost = Integer.parseInt(parts[2].trim());
                options.add(new BiomassEntity.SummonOption(asMobType(type), chance, cost));
            } catch (NumberFormatException ignored) {
            }
        }
        return options;
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Mob> asMobType(EntityType<?> type) {
        return (EntityType<? extends Mob>) type;
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

    private final class MonarchSkillLeapGoal extends Goal {
        private int chargeTicks;

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return monarchSkillLeapActive || target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (monarchSkillLeapActive) {
                tickMonarchSkillLeap();
                return;
            }
            LivingEntity target = getTarget();
            if (target == null || monarchWaterLeapActive) {
                return;
            }
            double distance = distanceToSqr(target);
            if (hasLineOfSight(target) && distance >= 100.0D && distance < 10_000.0D) {
                chargeTicks++;
            }
            if (hasEffect(ModMobEffects.RAGE.get())) {
                chargeTicks++;
            }
            if (chargeTicks >= 40 && onGround() && !hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                chargeTicks = 0;
                startMonarchSkillLeap(target);
            }
        }
    }

    private final class MonarchSwimmingDivingGoal extends Goal {
        private MonarchSwimmingDivingGoal() {
            setFlags(EnumSet.of(Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (monarchLeapBusy() || !isInWaterOrBubble() && !isInLava()) {
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

    private final class MonarchWebVolleyGoal extends Goal {
        private int attackTimer;
        private int shots;

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
            attackTimer = 0;
            shots = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || monarchLeapBusy()) {
                attackTimer = 0;
                shots = 0;
                return;
            }
            if (distanceToSqr(target) < 4225.0D && hasLineOfSight(target)) {
                attackTimer += hasEffect(ModMobEffects.RAGE.get()) ? 2 : 1;
                if (attackTimer > 40) {
                    if (shots < 4) {
                        if (attackTimer % 15 == 0) {
                            getLookControl().setLookAt(target, 30.0F, 30.0F);
                            fireWebProjectile(target, 1);
                            shots++;
                        }
                    } else {
                        attackTimer = 0;
                        shots = 0;
                    }
                }
            } else if (attackTimer > 0) {
                attackTimer--;
            }
        }
    }

    private final class MonarchWaterLeapGoal extends Goal {
        private int attackTimer;
        private int attacking;
        private double targetX;
        private double targetZ;
        private float targetY;

        @Override
        public boolean canUse() {
            return attacking >= 1 || !monarchSkillLeapActive && (isInWaterOrBubble() || isInLava());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (!monarchSkillLeapActive && target != null && target.isAlive()) {
                attackTimer += hasEffect(ModMobEffects.RAGE.get()) ? 2 : 1;
                if (attackTimer >= 20 && attacking == 0) {
                    attacking = 1;
                    targetX = target.getX();
                    targetZ = target.getZ();
                    targetY = Math.max(0.0F, (float) ((target.getY() - getY()) * 0.07D));
                }
            } else if (attackTimer > 0) {
                attackTimer--;
            }

            if (attacking < 1) {
                return;
            }
            attacking++;
            if (attacking == 2 && onGround()) {
                getNavigation().stop();
                double x = targetX - getX();
                double z = targetZ - getZ();
                double horizontalLength = Math.sqrt(x * x + z * z);
                if (horizontalLength > 0.001D) {
                    Vec3 movement = getDeltaMovement();
                    setDeltaMovement(movement.x + x / horizontalLength * 1.5D * 0.9D + movement.x * 0.3D,
                            0.7D + targetY,
                            movement.z + z / horizontalLength * 1.5D * 0.9D + movement.z * 0.3D);
                    hurtMarked = true;
                    monarchWaterLeapActive = true;
                    startSpecialLeapAnimation(24);
                }
            }
            if (monarchWaterLeapActive) {
                startSpecialLeapAnimation(2);
            }
            if (attacking >= 3 && onGround()) {
                performMonarchLandingAttack(7.0D);
                attacking = 0;
                attackTimer = 0;
                monarchWaterLeapActive = false;
            } else if (attacking >= 80) {
                attacking = 0;
                attackTimer = 0;
                monarchWaterLeapActive = false;
            }
        }
    }

    private final class MonarchAreaMeleeGoal extends Goal {
        private int attackCooldown;

        private MonarchAreaMeleeGoal() {
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
            if (target == null || monarchLeapBusy()) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            attackCooldown--;
            MobEffectInstance pivot = getEffect(ModMobEffects.PIVOT.get());
            if (attackCooldown > 0 && pivot != null) {
                attackCooldown -= pivot.getAmplifier() * 2;
            }
            double distance = distanceToSqr(target);
            if (distance <= 4.0D && hasLineOfSight(target)) {
                getNavigation().stop();
                if (attackCooldown <= 0) {
                    attackCooldown = 10;
                    performAreaMelee(target);
                }
                return;
            }
            getNavigation().moveTo(target, distance > 64.0D ? 1.3D : 1.0D);
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    private final class MonarchEvasiveDashGoal extends Goal {
        private final int cooldownTicks;
        private final double minimumDistanceSqr;
        private final double dashStrength;
        private final double maximumDistanceSqr;
        private int cooldown;

        private MonarchEvasiveDashGoal(int cooldownTicks, int ignoredDurationTicks, int minimumDistance,
                                       double dashStrength, int maximumDistance) {
            this.cooldownTicks = cooldownTicks;
            this.minimumDistanceSqr = minimumDistance * minimumDistance;
            this.dashStrength = dashStrength;
            this.maximumDistanceSqr = maximumDistance * maximumDistance;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && onGround() && !monarchLeapBusy();
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

    private void startMonarchSkillLeap(LivingEntity target) {
        Vec3 offset = target.position().subtract(position());
        double horizontalLength = offset.horizontalDistance();
        if (horizontalLength <= 0.001D) {
            return;
        }
        getNavigation().stop();
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(movement.x + offset.x / horizontalLength * 3.5D * 0.9D + movement.x * 0.3D,
                0.5D,
                movement.z + offset.z / horizontalLength * 3.5D * 0.9D + movement.z * 0.3D);
        hurtMarked = true;
        monarchSkillLeapActive = true;
        monarchSkillLeapWasAirborne = false;
        monarchSkillLeapTicks = 2;
        startSpecialLeapAnimation(40);
    }

    private void tickMonarchSkillLeap() {
        monarchSkillLeapTicks++;
        startSpecialLeapAnimation(2);
        breakBlocksForMonarchSkill();
        if (monarchSkillLeapTicks % 5 == 0 && monarchSkillLeapTicks < 40) {
            spawnMonarchBuglin();
        }
        if (!onGround()) {
            monarchSkillLeapWasAirborne = true;
        }
        if ((monarchSkillLeapWasAirborne && onGround())
                || (!monarchSkillLeapWasAirborne && onGround() && monarchSkillLeapTicks >= 4)) {
            performMonarchLandingAttack(4.0D);
            monarchSkillLeapActive = false;
            monarchSkillLeapWasAirborne = false;
            monarchSkillLeapTicks = 0;
            playSound(ModSounds.get("mob.hitground"), 15.0F, 1.0F);
        } else if (monarchSkillLeapTicks >= 80) {
            monarchSkillLeapActive = false;
            monarchSkillLeapWasAirborne = false;
            monarchSkillLeapTicks = 0;
        }
    }

    private void performMonarchLandingAttack(double radius) {
        AABB area = new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D, getZ() + 1.0D)
                .inflate(radius, 2.0D, radius);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), area);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != PureParasiteEntity.this && entity.isAlive() && !(entity instanceof Parasite))) {
            target.knockback(2.5D, getX() - target.getX(), getZ() - target.getZ());
            if (super.doHurtTarget(target)) {
                applyMeleeEffects(target, Kind.MONARCH);
            }
        }
    }

    private void spawnMonarchBuglin() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BuglinEntity buglin = ModEntities.BUGLIN.get().create(serverLevel);
        if (buglin != null) {
            buglin.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            serverLevel.addFreshEntity(buglin);
        }
    }

    private void breakBlocksForMonarchSkill() {
        if (!(level() instanceof ServerLevel serverLevel)
                || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !ForgeEventFactory.getMobGriefingEvent(level(), this)) {
            return;
        }
        int baseX = Mth.floor(getX());
        int baseY = Mth.floor(getY() + 0.1D);
        int baseZ = Mth.floor(getZ());
        int verticalOffset = 0;
        int horizontalRange = 4;
        LivingEntity target = getTarget();
        if (target != null && distanceToSqr(target) < 9.0D) {
            if (target.getY() - getY() < -1.0D) {
                verticalOffset = -2;
            } else if (target.getY() - getY() > 2.0D) {
                verticalOffset = 1;
                horizontalRange = 0;
            }
        }
        for (int x = -horizontalRange; x <= horizontalRange; x++) {
            for (int z = -horizontalRange; z <= horizontalRange; z++) {
                for (int y = 1 + verticalOffset; y <= 20 + verticalOffset; y++) {
                    BlockPos pos = new BlockPos(baseX + x, baseY + y, baseZ + z);
                    BlockState state = serverLevel.getBlockState(pos);
                    float hardness = state.getDestroySpeed(serverLevel, pos);
                    if (state.isAir() || hardness < 0.0F || hardness > adjustBlockBreakHardness(5.0F)
                            || !state.canEntityDestroy(serverLevel, pos, this)
                            || !ForgeEventFactory.onEntityDestroyBlock(this, pos, state)) {
                        continue;
                    }
                    serverLevel.destroyBlock(pos, true, this);
                }
            }
        }
    }

    private static final class OverseerMoveControl extends MoveControl {
        private OverseerMoveControl(PureParasiteEntity mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            double deltaX = wantedX - mob.getX();
            double deltaY = wantedY - mob.getY();
            double deltaZ = wantedZ - mob.getZ();
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (distance < mob.getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5D));
                return;
            }
            mob.setDeltaMovement(mob.getDeltaMovement().add(
                    deltaX / distance * 0.05D * speedModifier,
                    deltaY / distance * 0.05D * speedModifier,
                    deltaZ / distance * 0.05D * speedModifier));
            LivingEntity target = mob.getTarget();
            double lookX = target == null ? mob.getDeltaMovement().x : target.getX() - mob.getX();
            double lookZ = target == null ? mob.getDeltaMovement().z : target.getZ() - mob.getZ();
            mob.setYRot(-((float) Mth.atan2(lookX, lookZ)) * Mth.RAD_TO_DEG);
            mob.yBodyRot = mob.getYRot();
        }
    }

    private static final class SeekerMoveControl extends MoveControl {
        private SeekerMoveControl(PureParasiteEntity mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            double deltaX = wantedX - mob.getX();
            double deltaY = wantedY - mob.getY();
            double deltaZ = wantedZ - mob.getZ();
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (distance < mob.getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5D));
                return;
            }
            mob.setDeltaMovement(mob.getDeltaMovement().add(
                    deltaX / distance * 0.05D * speedModifier,
                    deltaY / distance * 0.05D * speedModifier,
                    deltaZ / distance * 0.05D * speedModifier));
            LivingEntity target = mob.getTarget();
            double lookX = target == null ? mob.getDeltaMovement().x : target.getX() - mob.getX();
            double lookZ = target == null ? mob.getDeltaMovement().z : target.getZ() - mob.getZ();
            mob.setYRot(-((float) Mth.atan2(lookX, lookZ)) * Mth.RAD_TO_DEG);
            mob.yBodyRot = mob.getYRot();
        }
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
        private int attackTimer;
        private int shots;

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
            if (target == null || !target.isAlive() || isOverseerSummoning()) {
                attackTimer = 0;
                shots = 0;
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (distanceToSqr(target) < 4225.0D && hasLineOfSight(target)) {
                attackTimer++;
                if (hasEffect(ModMobEffects.RAGE.get())) {
                    attackTimer++;
                }
                if (attackTimer == 10) {
                    playSound(ModSounds.get("alafha.shootingpost"), 2.0F, 1.0F);
                }
                if (attackTimer > 20) {
                    if (shots < 4) {
                        if (attackTimer % 10 == 0) {
                            fireOverseerProjectile(target);
                            shots++;
                        }
                    } else {
                        attackTimer = 0;
                        shots = 0;
                    }
                }
            } else if (attackTimer > 0) {
                attackTimer--;
            }
        }

        @Override
        public void stop() {
            attackTimer = 0;
            shots = 0;
        }
    }

    private final class OverseerMeleeRushGoal extends Goal {
        private int chargeTicks;
        private int attackCooldown;

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
            if (target == null || !target.isAlive()) {
                setTarget(null);
                return;
            }
            if (isInWater()) {
                return;
            }
            chargeTicks++;
            if (chargeTicks < 80) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double attackDistance = distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            if (distanceToSqr(target) * 0.85D < 256.0D && hasLineOfSight(target)) {
                double deltaX = target.getX() - getX();
                double deltaZ = target.getZ() - getZ();
                double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                if (horizontal > 0.001D) {
                    Vec3 movement = getDeltaMovement();
                    double motionY = target.getY() >= getY() + 4.0D ? 0.52D : -0.2D;
                    setDeltaMovement(movement.x + deltaX / horizontal * 0.045D + movement.x * 0.045D,
                            motionY,
                            movement.z + deltaZ / horizontal * 0.045D + movement.z * 0.045D);
                }
                attackCooldown = Math.max(attackCooldown - 1, 0);
                if (attackDistance <= 20.25D && attackCooldown <= 0) {
                    attackCooldown = 20;
                    doHurtTarget(target);
                }
                if (chargeTicks > 140) {
                    chargeTicks = 0;
                }
            }
        }
    }

    private final class OverseerFlightLimitGoal extends Goal {
        @Override
        public boolean canUse() {
            return MobsConfig.overseerMaxY() != 256;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            int limit = MobsConfig.overseerMaxY();
            LivingEntity target = getTarget();
            boolean pushDown = target == null ? !hasBlockBelow(limit) : target.getY() + limit > getY();
            if (pushDown) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
            }
        }
    }

    private final class OverseerRandomFlightGoal extends Goal {
        private OverseerRandomFlightGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!getMoveControl().hasWanted()) {
                return true;
            }
            double deltaX = getMoveControl().getWantedX() - getX();
            double deltaY = getMoveControl().getWantedY() - getY();
            double deltaZ = getMoveControl().getWantedZ() - getZ();
            double distance = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
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
            double targetDistance = distanceToSqr(target);
            if (targetDistance > 400.0D) {
                center = target.blockPosition();
                mode = 2;
                speed += 0.11D;
            } else if (targetDistance < 100.0D) {
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

    private final class OverseerSummonGoal extends Goal {
        private int successfulLaunches;
        private int chargeTicks;
        private int castingTicks;

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return castingTicks >= 1 || target != null && target.onGround();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (castingTicks >= 1) {
                castingTicks++;
                setOverseerSummoning(true);
                getNavigation().stop();
                if (castingTicks % 20 == 0 && castingTicks >= 40
                        && getUsedSummonCapacity() < getSummonCapacity()
                        && successfulLaunches < MobsConfig.overseerSummonLimit()
                        && target != null && launchOverseerBiomass(target)) {
                    successfulLaunches++;
                }
                if (castingTicks >= 80 || successfulLaunches >= MobsConfig.overseerSummonLimit()) {
                    castingTicks = 0;
                    chargeTicks = 0;
                    successfulLaunches = 0;
                    setOverseerSummoning(false);
                }
                return;
            }
            setOverseerSummoning(false);
            if (target == null || !target.isAlive()) {
                chargeTicks = Math.max(0, chargeTicks - 1);
                return;
            }
            if (hasLineOfSight(target) && distanceToSqr(target) < 256.0D && target.onGround()) {
                chargeTicks++;
                if (chargeTicks >= MobsConfig.overseerSummonCooldownTicks()) {
                    castingTicks = 1;
                    setOverseerSummoning(true);
                }
            } else if (chargeTicks > 0) {
                chargeTicks--;
            }
        }

        @Override
        public void stop() {
            if (castingTicks == 0) {
                setOverseerSummoning(false);
            }
        }
    }

    private boolean updateVigilanteCombatMode() {
        LivingEntity target = getTarget();
        vigilanteMeleeMode = target != null && target.isAlive()
                && distanceToSqr(target) < 25.0D && hasLineOfSight(target);
        return vigilanteMeleeMode;
    }

    private final class VigilanteRangeSwitchGoal extends Goal {
        @Override
        public boolean canUse() {
            if (getTarget() == null) {
                vigilanteMeleeMode = true;
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            updateVigilanteCombatMode();
        }

        @Override
        public void stop() {
            vigilanteMeleeMode = true;
        }
    }

    private final class VigilanteMeleeGoal extends MeleeAttackGoal {
        private VigilanteMeleeGoal() {
            super(PureParasiteEntity.this, 1.5D, false);
        }

        @Override
        public boolean canUse() {
            return updateVigilanteCombatMode() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return updateVigilanteCombatMode() && super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            setVigilanteStatus(2);
        }

        @Override
        public void stop() {
            super.stop();
            if (getTarget() == null || !vigilanteMeleeMode) {
                setVigilanteStatus(0);
            }
        }
    }

    private final class VigilanteRangedGoal extends Goal {
        private int rangedAttackTime = -1;
        private int seeTime;

        private VigilanteRangedGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return onGround() && target != null && target.isAlive() && !updateVigilanteCombatMode();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || !getNavigation().isDone() && !vigilanteMeleeMode;
        }

        @Override
        public void start() {
            setVigilanteStatus(2);
        }

        @Override
        public void stop() {
            seeTime = 0;
            setVigilanteStatus(0);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }
            double distance = distanceToSqr(target);
            boolean visible = hasLineOfSight(target);
            if (visible) {
                seeTime++;
            } else {
                seeTime = 0;
            }
            double followRange = getAttributeValue(Attributes.FOLLOW_RANGE);
            double rangedDistance = followRange * 0.5D;
            double maximumRangedDistance = rangedDistance * rangedDistance;
            if (tickCount % 21 == 10 && distance > followRange * followRange) {
                setTarget(null);
                return;
            }
            if (distance <= maximumRangedDistance && seeTime >= 10) {
                getNavigation().stop();
            } else {
                getNavigation().moveTo(target, 1.5D);
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (hasEffect(ModMobEffects.RAGE.get())) {
                rangedAttackTime--;
            }
            if (--rangedAttackTime <= 0 && visible && distance <= maximumRangedDistance) {
                fireVigilanteProjectile(target);
                rangedAttackTime = 20;
            }
        }
    }

    private final class WardenAreaMeleeGoal extends Goal {
        private int attackCooldown;

        private WardenAreaMeleeGoal() {
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
            attackCooldown--;
            MobEffectInstance pivot = getEffect(ModMobEffects.PIVOT.get());
            if (attackCooldown > 0 && pivot != null) {
                attackCooldown -= pivot.getAmplifier() * 2;
            }
            double distance = distanceToSqr(target);
            if (distance <= 16.0D && hasLineOfSight(target)) {
                getNavigation().moveTo(target, 0.0D);
                setWardenStatus(1);
                if (attackCooldown <= 0) {
                    attackCooldown = 20;
                    performAreaMelee(target);
                }
                return;
            }
            setWardenStatus(2);
            getNavigation().moveTo(target, distance > 64.0D ? 1.3D : 1.0D);
        }

        @Override
        public void stop() {
            getNavigation().stop();
            if (getWardenStatus() <= 2) {
                setWardenStatus(getTarget() == null ? 0 : 2);
            }
        }
    }

    private final class WardenLeapGoal extends Goal {
        private int activationTicks;
        private boolean airborne;

        private WardenLeapGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || !onGround()
                    || hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || getWardenStatus() > 2) {
                return false;
            }
            double distance = distanceToSqr(target);
            if (distance < 100.0D || distance >= 10000.0D || !hasLineOfSight(target)) {
                return false;
            }
            activationTicks += hasEffect(ModMobEffects.RAGE.get()) ? 2 : 1;
            return activationTicks >= 80;
        }

        @Override
        public boolean canContinueToUse() {
            return wardenLeapTicks > 0 && wardenLeapTicks < 100;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            wardenLeapTargetX = target.getX();
            wardenLeapTargetZ = target.getZ();
            Vec3 movement = getDeltaMovement();
            double dx = wardenLeapTargetX - getX();
            double dz = wardenLeapTargetZ - getZ();
            double horizontal = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
            getNavigation().stop();
            setWardenStatus(10);
            setDeltaMovement(movement.x + dx / horizontal * 2.5D * 0.9D + movement.x * 0.3D,
                    1.2D,
                    movement.z + dz / horizontal * 2.5D * 0.9D + movement.z * 0.3D);
            hurtMarked = true;
            airborne = false;
            wardenLeapTicks = 1;
            startSpecialLeapAnimation(100);
        }

        @Override
        public void tick() {
            wardenLeapTicks++;
            startSpecialLeapAnimation(2);
            if (!onGround()) {
                airborne = true;
            }
            if (airborne && onGround()) {
                performWardenLandingAttack();
                playSound(ModSounds.get("mob.hitground"), 15.0F, 1.0F);
                wardenLeapTicks = 0;
                setWardenStatus(0);
            }
        }

        @Override
        public void stop() {
            activationTicks = 0;
            if (wardenLeapTicks >= 100) {
                wardenLeapTicks = 0;
                setWardenStatus(0);
            }
        }
    }

    private final class WardenChargeGoal extends Goal {
        private int activationTicks;
        private int chargeTicks;
        private double targetX;
        private double targetY;
        private double targetZ;
        private boolean finished;

        private WardenChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double distance = distanceToSqr(target);
            if (distance < 64.0D || distance >= 1024.0D || !hasLineOfSight(target)) {
                return false;
            }
            activationTicks += hasEffect(ModMobEffects.RAGE.get()) ? 2 : 1;
            return activationTicks >= 40;
        }

        @Override
        public boolean canContinueToUse() {
            return !finished && chargeTicks > 0 && chargeTicks < 200;
        }

        @Override
        public void start() {
            chargeTicks = 1;
            finished = false;
            getNavigation().stop();
            entityData.set(WARDEN_CHARGING, true);
            setWardenStatus(3);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                finished = true;
                return;
            }
            chargeTicks++;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (chargeTicks < 20) {
                if (chargeTicks == 2) {
                    playSound(ModSounds.get("ganro.hurt"), 4.0F,
                            (random.nextFloat() - random.nextFloat()) * 0.4F + 2.0F);
                }
                if (!onGround() || isInWater()
                        || target.getY() > getY() && target.onGround()) {
                    finished = true;
                    setWardenStatus(0);
                    return;
                }
                getNavigation().stop();
                setWardenStatus(3);
                double distance = Math.max(0.001D, distanceTo(target));
                targetX = getX() + 15.0D * (target.getX() - getX()) / distance;
                targetY = getY() + 15.0D * (target.getY() - getY()) / distance;
                targetZ = getZ() + 15.0D * (target.getZ() - getZ()) / distance;
                spawnWardenChargeParticles();
                return;
            }
            if (chargeTicks == 20) {
                getNavigation().moveTo(targetX, targetY, targetZ, 3.0D);
            }
            damageWardenChargeTargets();
            if (!onGround()) {
                Vec3 movement = getDeltaMovement();
                setDeltaMovement(movement.x * 0.7D, movement.y, movement.z * 0.7D);
            }
            if (chargeTicks >= 60 && getX() == xo && getZ() == zo) {
                finished = true;
                setWardenStatus(2);
            }
        }

        @Override
        public void stop() {
            activationTicks = 0;
            chargeTicks = 0;
            entityData.set(WARDEN_CHARGING, false);
            if (getWardenStatus() == 3) {
                setWardenStatus(0);
            }
        }
    }

    private final class WardenShockwaveGoal extends Goal {
        private int activationTicks;
        private int shockwaveTicks;

        private WardenShockwaveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            int maximumDistance = (int) (getAttributeValue(Attributes.FOLLOW_RANGE) * 0.7D);
            double distance = distanceToSqr(target);
            if (distance < 4.0D || distance >= maximumDistance * maximumDistance) {
                return false;
            }
            activationTicks += hasEffect(ModMobEffects.RAGE.get()) ? 2 : 1;
            return activationTicks >= 40;
        }

        @Override
        public boolean canContinueToUse() {
            return shockwaveTicks > 0 && shockwaveTicks <= 80 && getTarget() != null;
        }

        @Override
        public void start() {
            shockwaveTicks = 1;
            setWardenStatus(100);
            getNavigation().stop();
            playSound(ModSounds.get("ganro.hurt"), 4.0F,
                    (random.nextFloat() - random.nextFloat()) * 0.4F + 2.0F);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            shockwaveTicks++;
            getNavigation().stop();
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (shockwaveTicks <= 40) {
                spawnWardenChargeParticles();
            }
            if (shockwaveTicks == 40) {
                spawnWardenShockwave(target);
                triggerAttackAnimation();
                playSound(ModSounds.MOB_SWIPE.get(), 2.0F, 1.0F);
            }
        }

        @Override
        public void stop() {
            activationTicks = 0;
            shockwaveTicks = 0;
            setWardenStatus(0);
        }
    }

    private void performWardenLandingAttack() {
        AABB area = new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D, getZ() + 1.0D)
                .inflate(5.0D, 2.0D, 5.0D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), area);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                this::isValidParasiteTarget)) {
            target.knockback(2.5D, getX() - target.getX(), getZ() - target.getZ());
            hurtWardenSkillTarget(target);
        }
    }

    private void damageWardenChargeTargets() {
        AABB area = getBoundingBox().inflate(2.0D, 0.0D, 2.0D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), area);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                this::isValidParasiteTarget)) {
            target.knockback(0.5D, getX() - target.getX(), getZ() - target.getZ());
            hurtWardenSkillTarget(target);
        }
    }

    boolean hurtWardenSkillTarget(LivingEntity target) {
        if (!isValidParasiteTarget(target) || !super.doHurtTarget(target)) {
            return false;
        }
        applyMeleeEffects(target, Kind.WARDEN);
        return true;
    }

    private void spawnWardenChargeParticles() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    2, getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.03D);
        }
    }

    private void spawnWardenShockwave(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        WardenShockwaveEntity shockwave = ModEntities.WARDEN_SHOCKWAVE.get().create(serverLevel);
        if (shockwave == null) {
            return;
        }
        shockwave.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        shockwave.configure(this, target);
        if (serverLevel.noCollision(shockwave, shockwave.getBoundingBox())) {
            serverLevel.addFreshEntity(shockwave);
        }
    }

    private static final class WardenTendrilPart extends PartEntity<PureParasiteEntity> {
        private final boolean left;

        private WardenTendrilPart(PureParasiteEntity parent, boolean left) {
            super(parent);
            this.left = left;
        }

        private void updatePosition() {
            PureParasiteEntity parent = getParent();
            float yaw = parent.getYRot() * Mth.DEG_TO_RAD;
            float side = left ? 1.0F : -1.0F;
            setPos(parent.getX() + side * Mth.cos(yaw),
                    parent.getY() + 3.7D,
                    parent.getZ() + side * Mth.sin(yaw));
            setYRot(parent.getYRot());
        }

        @Override
        protected void defineSynchedData() {
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        public boolean isPickable() {
            return getParent().isAlive();
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            return getParent().hurtWardenTendril(source, amount);
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
            return Component.literal(left ? "warden_left_tendril" : "warden_right_tendril");
        }
    }

    private static final class OverseerHeadPart extends PartEntity<PureParasiteEntity> {
        private OverseerHeadPart(PureParasiteEntity parent) {
            super(parent);
        }

        private void updatePosition() {
            PureParasiteEntity parent = getParent();
            float rotation = parent.getYRot() * Mth.DEG_TO_RAD - parent.yBodyRot * 0.01F;
            float forward = 3.0F * Mth.cos((float) Math.PI / 18.0F);
            setPos(parent.getX() - Mth.sin(rotation) * forward,
                    parent.getY(),
                    parent.getZ() + Mth.cos(rotation) * forward);
            setYRot(parent.getYRot());
        }

        @Override
        protected void defineSynchedData() {
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        public boolean isPickable() {
            return getParent().isAlive();
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            return getParent().hurt(source, amount * 3.0F);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(1.2F, 1.2F);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public Component getName() {
            return Component.literal("overseer_head");
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
        protected void defineSynchedData() {
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
        OVERSEER(true, false, 80.0D, 20.0D, 22.0D, 0.27D, 0.40D, 32.0D, 5.0F, 2.0D),
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
