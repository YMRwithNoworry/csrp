package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.config.MobsConfig;
import alku.csrp.effect.EffectStacking;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

/** Shared implementation for the legacy adapted parasite tier. */
public final class AdaptedVariantEntity extends BurrowingVariantEntity
        implements PullingBallOwner, SummonCapacityOwner {
    private static final byte VOMIT_EVENT = 100;
    private static final byte SUMMON_EVENT = 101;
    private static final int SUMMONER_COOLDOWN_TICKS = 160;
    private static final int SUMMONER_TOTAL_CAPACITY = 6;
    private static final int SUMMONER_LIMIT = 1;
    private static final int ARACHNIDA_SKILL_CHARGE_TICKS = 20;
    private static final int ARACHNIDA_SKILL_SHOTS = 6;
    private static final int ARACHNIDA_SKILL_SHOT_INTERVAL = 20;
    private static final int ARACHNIDA_MAX_PULL_TICKS = 400;
    private static final EntityDataAccessor<Integer> BOLSTER_VARIANT = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BOLSTER_ACTION = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BOLSTER_ACTION_TICKS = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BOLSTER_LEFT_TENDRIL = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BOLSTER_RIGHT_TENDRIL = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ARACHNIDA_STATUS = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ARACHNIDA_TARGET = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ARACHNIDA_SKIN = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> REEKER_CHARGING = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> REEKER_PULLING = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> REEKER_STILL_ANI = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SUMMONER_CASTING = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SUMMONER_STATUS = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MANDUCATER_STATUS = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MANDUCATER_STILL_ANI = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> YELLOWEYE_CHARGING = SynchedEntityData.defineId(
            AdaptedVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation RUN = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation LONGARMS_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation AGE_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation FLY = IDLE;
    private final RawAnimation DIG = ParasiteAnimations.loop(this, "get_dig_model.get_digging_1");
    private final RawAnimation DIG_BODY_02 = ParasiteAnimations.loop(this,
            "get_dig_model.get_body_number_0_2.get_digging_1");
    private final RawAnimation AGE_BODY_02 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_body_number_0_2");
    private final RawAnimation BOLSTER_ATTACK = ParasiteAnimations.play(this, "get_attack_timer");
    private final RawAnimation TOZOON_ATTACK = ParasiteAnimations.loop(this, "get_attack_timer");
    private final RawAnimation ARACHNIDA_ATTACK_PREP = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation ARACHNIDA_FAST_MOVE = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation ARACHNIDA_PULLING = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3");
    private final RawAnimation REEKER_CHARGE_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1");
    private final RawAnimation REEKER_CHARGE_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_3");
    private final RawAnimation REEKER_PULLING_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3");
    private final RawAnimation REEKER_PULLING_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_3");
    private final RawAnimation REEKER_ALERT = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation REEKER_ATTACK_PREP = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation SUMMONER_CAST = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");
    private final RawAnimation SUMMONER_ATTACK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation SUMMONER_VOMIT = ParasiteAnimations.loop(this,
            "idle.get_parasite_status_100");
    private final RawAnimation SUMMONER_SPECIAL = ParasiteAnimations.loop(this,
            "idle.get_parasite_status_25");
    private final RawAnimation MANDUCATER_ATTACK = ParasiteAnimations.loop(this,
            "walk.get_parasite_status_1");
    private final RawAnimation MANDUCATER_SUMMON = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation MANDUCATER_EVADE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation YELLOWEYE_CHARGE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation BOLSTER_STATUS_3 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3");
    private final RawAnimation BOLSTER_STATUS_15 = BOLSTER_STATUS_3;
    private final RawAnimation BOLSTER_STATUS_25 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_25");
    private final RawAnimation BOLSTER_ATTACK_STATUS_15 = ParasiteAnimations.loop(this,
            "get_attack_timer.get_parasite_status_15");
    private final RawAnimation BOLSTER_ATTACK_STATUS_25 = ParasiteAnimations.loop(this,
            "get_attack_timer.get_parasite_status_25");
    private final RawAnimation[] BODY_IDLE = {
            ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks"),
            ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks.get_body_number_1"),
            ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks.get_body_number_2"),
            ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks.get_body_number_3")
    };
    private final RawAnimation[] BODY_DIG = {
            DIG,
            ParasiteAnimations.loop(this, "get_dig_model.get_body_number_1.get_digging_1"),
            ParasiteAnimations.loop(this, "get_dig_model.get_body_number_2.get_digging_1"),
            ParasiteAnimations.loop(this, "get_dig_model.get_body_number_3.get_digging_1")
    };
    private final RawAnimation[] BODY_ATTACK = {
            ParasiteAnimations.loop(this, "get_attack_timer.get_body_number_neg_0_1"),
            ParasiteAnimations.loop(this, "get_attack_timer.get_body_number_1"),
            ParasiteAnimations.loop(this, "get_attack_timer.get_body_number_2"),
            ParasiteAnimations.loop(this, "get_attack_timer.get_body_number_3")
    };

    private final Kind kind;
    private final ArachnidaPart arachnidaAbdomen;
    private final ArachnidaPart arachnidaHead;
    private final PartEntity<?>[] arachnidaParts;
    private final SummonCapacityTracker summonTracker = new SummonCapacityTracker();
    private int abilityCooldown;
    private int summonerVomitTicks;
    private int supportCooldown;
    private int secondaryCooldown;
    private int blockBreakCooldown;
    private int rangedShots;
    private int cloakTicks;
    private boolean cloaked;
    private int residueCooldown;
    private int lastBolsterCombatTick;
    private boolean bolsterDeathHandled;
    private int manducaterVomitTicks;
    private int manducaterEvadeCooldown;
    private int reekerPullingCooldown;
    private int arachnidaPullingTicks;
    private int arachnidaAttackAnimationCooldown;
    private boolean arachnidaCanPull = true;

    public AdaptedVariantEntity(EntityType<? extends AdaptedVariantEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        if (kind == Kind.ARACHNIDA) {
            arachnidaAbdomen = new ArachnidaPart(this, "abdomen",
                    -1.6F, 1.5F, 1.7F, 1.9F, 2.0F, 0.75F);
            arachnidaHead = new ArachnidaPart(this, "head",
                    1.6F, 1.3F, 1.5F, 0.9F, 0.9F, 1.25F);
            arachnidaParts = new PartEntity<?>[]{arachnidaAbdomen, arachnidaHead};
        } else {
            arachnidaAbdomen = null;
            arachnidaHead = null;
            arachnidaParts = new PartEntity<?>[0];
        }
        xpReward = 55;
        if (kind == Kind.BURROWER || kind == Kind.TOZOON) {
            setPathfindingMalus(PathType.WATER, -1.0F);
        }
        if (isFlying(kind)) {
            moveControl = new FlyingMoveControl(this, 20, true);
            setNoGravity(true);
        } else if (kind == Kind.DEVOURER) {
            moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 0.2F, true);
        }
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return 10;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return 0.10F;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return 8;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return 0.80F;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return 0.50F;
    }

    @Override
    protected float damageAdaptationEffectiveness() {
        Kind activeKind = activeKind();
        return activeKind == Kind.MANDUCATER || activeKind == Kind.YELLOWEYE ? 0.95F : 1.0F;
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        double health;
        double armor;
        double damage;
        double speed;
        double knockbackResistance;
        double followRange;

        switch (kind) {
            case ARACHNIDA -> {
                health = MobsConfig.adaptedArachnidaHealth();
                armor = MobsConfig.adaptedArachnidaArmor();
                damage = MobsConfig.adaptedArachnidaDamage();
                speed = 0.33D;
                knockbackResistance = MobsConfig.adaptedArachnidaKnockbackResistance();
                followRange = MobsConfig.adaptedFollowRange();
            }
            case BOLSTER -> {
                health = 105.0D;
                armor = 19.0D;
                damage = 36.0D;
                speed = 0.17D;
                knockbackResistance = 0.90D;
                followRange = 32.0D;
            }
            case BURROWER -> {
                health = MobsConfig.adaptedTozoonHealth();
                armor = MobsConfig.adaptedTozoonArmor();
                damage = MobsConfig.adaptedTozoonDamage();
                speed = 0.32D;
                knockbackResistance = 1.0D;
                followRange = 32.0D;
            }
            case DEVOURER -> {
                health = 60.0D;
                armor = 13.0D;
                damage = 20.0D;
                speed = 0.32D;
                knockbackResistance = 0.40D;
                followRange = 40.0D;
            }
            case LONGARMS -> {
                health = 95.0D;
                armor = 16.0D;
                damage = 26.0D;
                speed = 0.31D;
                knockbackResistance = 0.70D;
                followRange = 48.0D;
            }
            case MANDUCATER -> {
                health = 45.0D;
                armor = 10.0D;
                damage = 24.0D;
                speed = 0.27D;
                knockbackResistance = 0.75D;
                followRange = 40.0D;
            }
            case REEKER -> {
                health = 90.0D;
                armor = 27.0D;
                damage = 32.0D;
                speed = 0.36D;
                knockbackResistance = 0.55D;
                followRange = 48.0D;
            }
            case SUMMONER -> {
                health = 100.0D;
                armor = 14.0D;
                damage = 30.0D;
                speed = 0.28D;
                knockbackResistance = 0.60D;
                followRange = 40.0D;
            }
            case TOZOON -> {
                health = MobsConfig.adaptedTozoonHealth();
                armor = MobsConfig.adaptedTozoonArmor();
                damage = MobsConfig.adaptedTozoonDamage();
                speed = 0.32D;
                knockbackResistance = 1.0D;
                followRange = 32.0D;
            }
            case VERMIN -> {
                health = 70.0D;
                armor = 24.0D;
                damage = 30.0D;
                speed = 0.25D;
                knockbackResistance = 0.35D;
                followRange = 40.0D;
            }
            case VISCERA -> {
                health = 95.0D;
                armor = 16.0D;
                damage = 27.0D;
                speed = 0.31D;
                knockbackResistance = 0.65D;
                followRange = 40.0D;
            }
            case YELLOWEYE -> {
                health = 55.0D;
                armor = 13.5D;
                damage = 17.0D;
                speed = 0.30D;
                knockbackResistance = 0.35D;
                followRange = 48.0D;
            }
            default -> throw new IllegalStateException("Unexpected adapted kind: " + kind);
        }

        AttributeSupplier.Builder attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, followRange);
        if (kind == Kind.ARACHNIDA || kind == Kind.BURROWER || kind == Kind.TOZOON) {
            attributes.add(Attributes.STEP_HEIGHT, 1.0D);
        }
        if (isFlying(kind)) {
            attributes.add(Attributes.FLYING_SPEED, 0.35D);
        }
        return attributes;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARACHNIDA_STATUS, 0);
        builder.define(ARACHNIDA_TARGET, 0);
        builder.define(ARACHNIDA_SKIN, 0);
        builder.define(BOLSTER_VARIANT, BolsterVariant.NORMAL.ordinal());
        builder.define(BOLSTER_ACTION, BolsterAction.NONE.ordinal());
        builder.define(BOLSTER_ACTION_TICKS, 0);
        builder.define(BOLSTER_LEFT_TENDRIL, -1.0F);
        builder.define(BOLSTER_RIGHT_TENDRIL, -1.0F);
        builder.define(REEKER_CHARGING, false);
        builder.define(REEKER_PULLING, 0);
        builder.define(REEKER_STILL_ANI, false);
        builder.define(SUMMONER_CASTING, false);
        builder.define(SUMMONER_STATUS, 0);
        builder.define(MANDUCATER_STATUS, 0);
        builder.define(MANDUCATER_STILL_ANI, false);
        builder.define(YELLOWEYE_CHARGING, false);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (!level.isClientSide() && activeKind() == Kind.ARACHNIDA
                && (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase())) {
            setArachnidaSkin(5 + random.nextInt(3));
        }
        if (!level.isClientSide() && activeKind() == Kind.BOLSTER) {
            if (random.nextFloat() < 0.33F) {
                setBolsterVariant(BolsterVariant.values()[1 + random.nextInt(3)]);
            }
            initializeBolsterTendrils();
            residueCooldown = 600 + random.nextInt(601);
        }
        return data;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (isDevourerType(getType())) {
            return new WaterBoundPathNavigation(this, level);
        }
        if (isArachnidaType(getType())) {
            return new WallClimberNavigation(this, level);
        }
        if (!isFlyingType(getType())) {
            return super.createNavigation(level);
        }
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        switch (activeKind()) {
            case ARACHNIDA -> {
                goalSelector.addGoal(2, new ArachnidaPullSkillGoal());
                goalSelector.addGoal(2, new ArachnidaWaterLeapGoal());
                goalSelector.addGoal(3, new ArachnidaMeleeGoal());
            }
            case BOLSTER -> {
                goalSelector.addGoal(1, new BolsterSupportGoal());
                goalSelector.addGoal(2, new BolsterOrbGoal());
                goalSelector.addGoal(3, new BarrageGoal());
                goalSelector.addGoal(4, new FastMeleeAttackGoal(this, 1.20D));
            }
            case BURROWER -> {
                goalSelector.addGoal(1, createBurrowMovementGoal());
                goalSelector.addGoal(2, new BurrowerMeleeGoal());
            }
            case TOZOON -> {
                goalSelector.addGoal(1, createBurrowMovementGoal());
                goalSelector.addGoal(2, new TozoonAoeAttackGoal());
            }
            case DEVOURER -> {
                goalSelector.addGoal(1, new TryFindWaterGoal(this));
                goalSelector.addGoal(2, new DevourerMeleeGoal());
                goalSelector.addGoal(6, new RandomSwimmingGoal(this, 1.0D, 20));
            }
            case LONGARMS -> {
                goalSelector.addGoal(1, new ShockwaveGoal());
                goalSelector.addGoal(2, new FastMeleeAttackGoal(this, 1.25D));
            }
            case MANDUCATER -> {
                goalSelector.addGoal(1, new CloakGoal());
                goalSelector.addGoal(2, new ManducaterEvadeGoal());
                goalSelector.addGoal(3, new ManducaterVomitGoal());
                goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.20D, false));
            }
            case REEKER -> {
                goalSelector.addGoal(1, new ChargeGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, false));
            }
            case SUMMONER -> {
                goalSelector.addGoal(1, new SummonGoal());
                goalSelector.addGoal(2, new VomitGoal());
                goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.05D, false));
            }
            case VERMIN -> goalSelector.addGoal(1, new VerminFlightGoal());
            case VISCERA -> {
                goalSelector.addGoal(1, new SideLeapGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, false));
            }
            case YELLOWEYE -> {
                goalSelector.addGoal(1, new YelloweyeRangedGoal());
                goalSelector.addGoal(2, new YelloweyeFlightGoal());
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        Kind activeKind = activeKind();
        if (activeKind == Kind.ARACHNIDA) {
            updateArachnidaParts();
        }
        if (isFlying(activeKind)) {
            setNoGravity(true);
        }
        if (level().isClientSide) {
            if (activeKind == Kind.SUMMONER && summonerVomitTicks > 0) {
                summonerVomitTicks--;
                spawnSummonerVomitParticles();
            }
            return;
        }
        if (abilityCooldown > 0) abilityCooldown--;
        if (supportCooldown > 0) supportCooldown--;
        if (secondaryCooldown > 0) secondaryCooldown--;
        if (blockBreakCooldown > 0) blockBreakCooldown--;
        if (reekerPullingCooldown > 0) reekerPullingCooldown--;
        if (arachnidaAttackAnimationCooldown > 0) arachnidaAttackAnimationCooldown--;
        if (entityData.get(BOLSTER_ACTION_TICKS) > 0) {
            entityData.set(BOLSTER_ACTION_TICKS, entityData.get(BOLSTER_ACTION_TICKS) - 1);
            if (entityData.get(BOLSTER_ACTION_TICKS) == 0) {
                entityData.set(BOLSTER_ACTION, BolsterAction.NONE.ordinal());
            }
        }
        updateCloak();

        if (activeKind == Kind.ARACHNIDA) {
            tickArachnidaTether();
        }

        LivingEntity target = getTarget();
        if (target != null && breaksSoftBlocks(activeKind)) {
            breakSoftBlockTowards(target);
        }
        if (activeKind == Kind.DEVOURER) {
            if (!isInWaterOrBubble() && tickCount % 40 == 0) {
                hurt(damageSources().drown(), 3.0F);
            }
        }
        if (activeKind == Kind.BOLSTER && level() instanceof ServerLevel serverLevel) {
            tickBolster(serverLevel);
        }
        if (activeKind == Kind.MANDUCATER) {
            tickManducater();
        }
        if (activeKind == Kind.SUMMONER) {
            tickSummoner();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == VOMIT_EVENT && activeKind() == Kind.SUMMONER) {
            summonerVomitTicks = 40;
        } else if (id == SUMMON_EVENT && activeKind() == Kind.SUMMONER) {
            spawnSummonParticles();
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void spawnSummonerVomitParticles() {
        Vec3 direction = getViewVector(1.0F);
        Vec3 start = getEyePosition().add(direction.scale(1.2D));
        for (int index = 0; index < 6; index++) {
            level().addParticle(ParticleTypes.WITCH, start.x, start.y - 0.2D, start.z,
                    direction.x * 0.2D + (random.nextDouble() - 0.5D) * 0.25D,
                    0.01D + random.nextDouble() * 0.1D,
                    direction.z * 0.2D + (random.nextDouble() - 0.5D) * 0.25D);
        }
    }

    private void spawnSummonParticles() {
        for (int index = 0; index < 11; index++) {
            level().addParticle(ModParticles.BIOMASS.get(),
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight(),
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    (random.nextDouble() - 0.5D) * 0.08D,
                    random.nextDouble() * 0.08D,
                    (random.nextDouble() - 0.5D) * 0.08D);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (activeKind() == Kind.MANDUCATER && cloaked) {
            endCloak();
            abilityCooldown = 140;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 4.0F;
        }
        if (activeKind() == Kind.BOLSTER && !level().isClientSide) {
            lastBolsterCombatTick = tickCount;
            damageBolsterTendril(source, amount);
        }
        return super.hurt(source, amount);
    }

    @Override
    protected int incomingDamageCapDivisor() {
        return activeKind() == Kind.BOLSTER && level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).damageCap() ? 9 : super.incomingDamageCapDivisor();
    }

    @Override
    protected int decreaseAirSupply(int airSupply) {
        return activeKind() == Kind.DEVOURER ? getMaxAirSupply() : super.decreaseAirSupply(airSupply);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        Kind activeKind = activeKind();
        if (activeKind == Kind.DEVOURER && !isInWaterOrBubble()) {
            return false;
        }
        if (activeKind == Kind.TOZOON) {
            return performTozoonAoeAttack(entity);
        }
        if (!(entity instanceof LivingEntity target)) {
            return super.doHurtTarget(entity);
        }

        if (activeKind == Kind.BOLSTER) {
            return performBolsterSweep(target);
        }

        boolean hit;
        if (activeKind == Kind.MANDUCATER && cloaked) {
            hit = target.hurt(damageSources().mobAttack(this), meleeDamage() * 4.0F);
            if (hit) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4), this);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), this);
            }
            endCloak();
            abilityCooldown = 140;
        } else if (activeKind == Kind.LONGARMS) {
            hit = target.hurt(damageSources().mobAttack(this), meleeDamage());
        } else {
            hit = super.doHurtTarget(entity);
        }
        if (!hit) {
            return false;
        }
        if (activeKind == Kind.ARACHNIDA) {
            arachnidaAttackAnimationCooldown = 100;
            if (getArachnidaSkin() == 5) {
                EffectStacking.apply(target, ModMobEffects.VIRAL, 100, 0);
            } else if (getArachnidaSkin() == 6) {
                EffectStacking.apply(target, ModMobEffects.BLEED, 100, 0);
            }
        }
        if (activeKind == Kind.BOLSTER || activeKind == Kind.MANDUCATER || activeKind == Kind.LONGARMS) {
            spawnAttackParticles(target);
        }
        if (activeKind == Kind.BOLSTER || activeKind == Kind.LONGARMS) {
            triggerAnim("bolster_attack_controller", "attack");
        }

        // 更新 Manducater 攻击状态
        if (activeKind == Kind.MANDUCATER) {
            setManducaterStatus(1);
            entityData.set(MANDUCATER_STILL_ANI, false);
        }

        // 更新 Summoner 攻击状态
        if (activeKind == Kind.SUMMONER) {
            setSummonerStatus(1);
        }

        switch (activeKind) {
            case BOLSTER -> hurtNearby(this, 2.75D, meleeDamage() * 0.75F, true);
            case LONGARMS -> hurtNearby(this, 3.25D, meleeDamage() * 0.80F, true);
            case REEKER -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), this);
            case VISCERA -> target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 100, 0), this);
            default -> {
            }
        }
        return true;
    }

    @Override
    protected void doPush(Entity entity) {
        super.doPush(entity);
        if (!level().isClientSide && activeKind() == Kind.ARACHNIDA && getArachnidaSkin() == 5
                && entity instanceof LivingEntity living && isValidParasiteTarget(living)) {
            EffectStacking.apply(living, ModMobEffects.VIRAL, 100, 0);
        }
        if (!level().isClientSide && activeKind() == Kind.BOLSTER
                && getBolsterVariant() == BolsterVariant.VIRULENT
                && entity instanceof LivingEntity living && isValidParasiteTarget(living)) {
            living.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 100, 0), this);
        }
    }

    @Override
    public boolean applyScaryOrbEffect(LivingEntity target, int nearbyEntities) {
        if (activeKind() == Kind.ARACHNIDA) {
            boolean applied = super.applyScaryOrbEffect(target, nearbyEntities);
            if (applied) {
                ConfiguredOrbEffects.apply(this, target, nearbyEntities, MobsConfig.adaptedArachnidaOrbEffects());
            }
            return applied;
        }
        if (activeKind() != Kind.BOLSTER) {
            return super.applyScaryOrbEffect(target, nearbyEntities);
        }
        if (target == this || target instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        if (target instanceof Parasite) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2, false, true), this);
            return true;
        }
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 2, false, true), this);
        target.addEffect(new MobEffectInstance(ModMobEffects.NEEDLER, 500, 2, false, true), this);
        if (target instanceof Player player) {
            player.causeFoodExhaustion(5.0F);
            Set<Item> cooledItems = new HashSet<>();
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && cooledItems.add(stack.getItem())) {
                    player.getCooldowns().addCooldown(stack.getItem(), 200);
                }
            }
            ItemStack offhand = player.getOffhandItem();
            if (!offhand.isEmpty() && cooledItems.add(offhand.getItem())) {
                player.getCooldowns().addCooldown(offhand.getItem(), 200);
            }
        }
        return true;
    }

    @Override
    public void die(DamageSource source) {
        if (activeKind() == Kind.ARACHNIDA && level() instanceof ServerLevel serverLevel
                && !SrpWorldData.get(serverLevel).colonies().isEmpty()) {
            Mob primitive = ModEntities.PRI_ARACHNIDA.get().create(serverLevel);
            if (primitive != null) {
                primitive.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                primitive.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                        MobSpawnType.CONVERSION, null);
                primitive.setCustomName(getCustomName());
                primitive.setCustomNameVisible(isCustomNameVisible());
                serverLevel.addFreshEntity(primitive);
            }
        }
        if (activeKind() == Kind.BOLSTER && !bolsterDeathHandled && level() instanceof ServerLevel serverLevel) {
            bolsterDeathHandled = true;
            if (random.nextFloat() < 0.20F) {
                createBolsterDeathBurst();
            }
            if (!SrpWorldData.get(serverLevel).colonies().isEmpty()) {
                Mob primitive = ModEntities.PRI_BOLSTER.get().create(serverLevel);
                if (primitive != null) {
                    primitive.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                    primitive.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                            MobSpawnType.CONVERSION, null);
                    primitive.setCustomName(getCustomName());
                    primitive.setCustomNameVisible(isCustomNameVisible());
                    serverLevel.addFreshEntity(primitive);
                }
            }
        }
        super.die(source);
    }

    private void createBolsterDeathBurst() {
        level().explode(this, getX(), getY() + getBbHeight() * 0.5D, getZ(), 2.5F,
                Level.ExplosionInteraction.NONE);
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(4.0F);
        cloud.setWaitTime(10);
        cloud.setDuration(200);
        cloud.setRadiusPerTick(-0.015F);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 1));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 1200, 1, false, false));
        level().addFreshEntity(cloud);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (activeKind() == Kind.ARACHNIDA) {
            tag.putInt("arachnida_skin", getArachnidaSkin());
        }
        if (activeKind() == Kind.BOLSTER) {
            tag.putInt("bolster_variant", entityData.get(BOLSTER_VARIANT));
            tag.putFloat("bolster_left_tendril", entityData.get(BOLSTER_LEFT_TENDRIL));
            tag.putFloat("bolster_right_tendril", entityData.get(BOLSTER_RIGHT_TENDRIL));
            tag.putInt("bolster_ability_cooldown", abilityCooldown);
            tag.putInt("bolster_support_cooldown", supportCooldown);
            tag.putInt("bolster_orb_cooldown", secondaryCooldown);
            tag.putInt("bolster_residue_cooldown", residueCooldown);
            tag.putInt("bolster_last_combat_tick", lastBolsterCombatTick);
        }
        if (activeKind() == Kind.MANDUCATER) {
            tag.putInt("manducater_status", entityData.get(MANDUCATER_STATUS));
            tag.putBoolean("manducater_still_ani", entityData.get(MANDUCATER_STILL_ANI));
            tag.putInt("manducater_vomit_ticks", manducaterVomitTicks);
            tag.putInt("manducater_evade_cooldown", manducaterEvadeCooldown);
            tag.putBoolean("manducater_cloaked", cloaked);
            tag.putInt("manducater_cloak_ticks", cloakTicks);
            tag.putInt("manducater_ability_cooldown", abilityCooldown);
            tag.putInt("manducater_secondary_cooldown", secondaryCooldown);
        }
        if (activeKind() == Kind.REEKER) {
            tag.putBoolean("reeker_charging", entityData.get(REEKER_CHARGING));
            tag.putInt("reeker_pulling", entityData.get(REEKER_PULLING));
            tag.putBoolean("reeker_still_ani", entityData.get(REEKER_STILL_ANI));
            tag.putInt("reeker_pulling_cooldown", reekerPullingCooldown);
            tag.putInt("reeker_ability_cooldown", abilityCooldown);
        }
        if (activeKind() == Kind.SUMMONER) {
            tag.putBoolean("summoner_casting", entityData.get(SUMMONER_CASTING));
            tag.putInt("summoner_status", entityData.get(SUMMONER_STATUS));
            tag.putInt("summoner_ability_cooldown", abilityCooldown);
            tag.putInt("summoner_secondary_cooldown", secondaryCooldown);
            summonTracker.save(tag, "summoner_tracked_summons");
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (activeKind() == Kind.ARACHNIDA) {
            setArachnidaSkin(tag.getInt("arachnida_skin"));
            setArachnidaStatus(0);
            entityData.set(ARACHNIDA_TARGET, 0);
            arachnidaPullingTicks = 0;
            arachnidaCanPull = true;
        }
        if (activeKind() == Kind.BOLSTER) {
            entityData.set(BOLSTER_VARIANT, tag.getInt("bolster_variant"));
            entityData.set(BOLSTER_LEFT_TENDRIL, tag.contains("bolster_left_tendril")
                    ? tag.getFloat("bolster_left_tendril") : -1.0F);
            entityData.set(BOLSTER_RIGHT_TENDRIL, tag.contains("bolster_right_tendril")
                    ? tag.getFloat("bolster_right_tendril") : -1.0F);
            abilityCooldown = tag.getInt("bolster_ability_cooldown");
            supportCooldown = tag.getInt("bolster_support_cooldown");
            secondaryCooldown = tag.getInt("bolster_orb_cooldown");
            residueCooldown = tag.contains("bolster_residue_cooldown")
                    ? tag.getInt("bolster_residue_cooldown") : 600 + random.nextInt(601);
            lastBolsterCombatTick = tag.getInt("bolster_last_combat_tick");
            setBolsterAction(BolsterAction.NONE, 0);
        }
        if (activeKind() == Kind.MANDUCATER) {
            entityData.set(MANDUCATER_STATUS, tag.getInt("manducater_status"));
            entityData.set(MANDUCATER_STILL_ANI, tag.getBoolean("manducater_still_ani"));
            manducaterVomitTicks = tag.getInt("manducater_vomit_ticks");
            manducaterEvadeCooldown = tag.getInt("manducater_evade_cooldown");
            cloaked = tag.getBoolean("manducater_cloaked");
            cloakTicks = tag.getInt("manducater_cloak_ticks");
            abilityCooldown = tag.getInt("manducater_ability_cooldown");
            secondaryCooldown = tag.getInt("manducater_secondary_cooldown");
            if (cloaked) {
                setInvisible(true);
            }
        }
        if (activeKind() == Kind.REEKER) {
            entityData.set(REEKER_CHARGING, tag.getBoolean("reeker_charging"));
            entityData.set(REEKER_PULLING, tag.getInt("reeker_pulling"));
            entityData.set(REEKER_STILL_ANI, tag.getBoolean("reeker_still_ani"));
            reekerPullingCooldown = tag.getInt("reeker_pulling_cooldown");
            abilityCooldown = tag.getInt("reeker_ability_cooldown");
        }
        if (activeKind() == Kind.SUMMONER) {
            entityData.set(SUMMONER_CASTING, tag.getBoolean("summoner_casting"));
            entityData.set(SUMMONER_STATUS, tag.getInt("summoner_status"));
            abilityCooldown = tag.getInt("summoner_ability_cooldown");
            secondaryCooldown = tag.getInt("summoner_secondary_cooldown");
            summonTracker.load(tag, "summoner_tracked_summons");
            entityData.set(SUMMONER_CASTING, false);
            setSummonerStatus(0);
        }
    }

    @Override
    public boolean onClimbable() {
        if (activeKind() == Kind.ARACHNIDA) {
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
        return activeKind() == Kind.LONGARMS && horizontalCollision || super.onClimbable();
    }

    @Override
    public boolean isMultipartEntity() {
        return arachnidaParts != null && arachnidaParts.length > 0;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (arachnidaParts == null) {
            return;
        }
        for (int index = 0; index < arachnidaParts.length; index++) {
            arachnidaParts[index].setId(id + index + 1);
        }
    }

    @Override
    public PartEntity<?>[] getParts() {
        return arachnidaParts == null ? new PartEntity<?>[0] : arachnidaParts;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (activeKind() == Kind.ARACHNIDA) {
            playSound(ModSounds.HEAVY_MULTIPLE_STEP.get(), getSoundVolume(), getVoicePitch());
            return;
        }
        super.playStepSound(pos, state);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return !isFlying(activeKind()) && super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        if (activeKind() == Kind.TOZOON) {
            controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                    .triggerableAnim("get_attack_timer", TOZOON_ATTACK));
        }
        controllers.add(new AnimationController<>(this, "bolster_overlay_controller", 0,
                this::bolsterOverlayAnimation));
        controllers.add(new AnimationController<>(this, "bolster_attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", BOLSTER_ATTACK));
    }

    private PlayState movementAnimation(AnimationState<AdaptedVariantEntity> state) {
        Kind kind = activeKind();
        if (kind == Kind.TOZOON && isBodyAttackAnimating()) {
            int body = Math.min(getBodyNumber(), BODY_ATTACK.length - 1);
            return state.setAndContinue(body == 0 ? TOZOON_ATTACK : BODY_ATTACK[body]);
        }
        if (getBodyNumber() > 0) {
            int body = Math.min(getBodyNumber(), BODY_IDLE.length - 1);
            if (kind == Kind.BURROWER) {
                return state.setAndContinue(isBurrowing() ? DIG_BODY_02 : AGE_BODY_02);
            }
            return state.setAndContinue(isBurrowing() ? BODY_DIG[body] : BODY_IDLE[body]);
        }
        if (kind == Kind.ARACHNIDA) {
            int status = getArachnidaStatus();
            if (status == 10 || status == 11) {
                return PlayState.STOP;
            }
            if (status == 3) {
                return state.setAndContinue(ARACHNIDA_PULLING);
            }
            boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
            if (status == 2) {
                return moving ? state.setAndContinue(ARACHNIDA_FAST_MOVE) : PlayState.STOP;
            }
            if (status == 1) {
                return state.setAndContinue(moving ? ARACHNIDA_ATTACK_PREP : AGE_STATUS_1);
            }
            if (!moving) {
                return state.setAndContinue(IDLE);
            }
            return state.setAndContinue(WALK);
        }
        if (kind == Kind.REEKER) {
            int pulling = entityData.get(REEKER_PULLING);
            boolean stillAni = entityData.get(REEKER_STILL_ANI);
            boolean charging = entityData.get(REEKER_CHARGING);

            // 拉拽技能动画（状态 3）
            if (pulling > 0) {
                boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
                if (stillAni) {
                    return state.setAndContinue(REEKER_PULLING_IDLE);
                }
                return state.setAndContinue(moving ? REEKER_PULLING_WALK : REEKER_PULLING_IDLE);
            }

            // 冲锋动画
            if (charging) {
                boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
                if (stillAni) {
                    return state.setAndContinue(REEKER_CHARGE_IDLE);
                }
                return state.setAndContinue(moving ? REEKER_CHARGE_WALK : REEKER_CHARGE_IDLE);
            }

            // 警戒状态（状态 1）- 当有目标但距离较远时
            LivingEntity target = getTarget();
            if (target != null && distanceToSqr(target) > 64.0D) {
                return state.setAndContinue(REEKER_ALERT);
            }

            // 攻击准备（状态 2）- 接近目标时快速移动
            if (target != null && getDeltaMovement().horizontalDistanceSqr() > 0.02D) {
                return state.setAndContinue(REEKER_ATTACK_PREP);
            }
        }
        if (kind == Kind.SUMMONER && entityData.get(SUMMONER_CASTING)) {
            return state.setAndContinue(SUMMONER_CAST);
        }
        if (kind == Kind.DEVOURER) {
            LivingEntity target = getTarget();
            return state.setAndContinue(target != null && target.isAlive() ? AGE_STATUS_1 : IDLE);
        }
        if (kind == Kind.LONGARMS) {
            LivingEntity target = getTarget();
            boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
            if (!moving) {
                return state.setAndContinue(target != null && target.isAlive()
                        ? LONGARMS_STATUS_1 : IDLE);
            }
            return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
        }
        if (kind == Kind.SUMMONER) {
            int status = getSummonerStatus();
            // 状态 100: 呕吐动画（呕吐粒子效果）
            if (status == 100) {
                return state.setAndContinue(SUMMONER_VOMIT);
            }
            // 状态 25: 特殊震动状态
            if (status == 25) {
                return state.setAndContinue(SUMMONER_SPECIAL);
            }
            // 状态 10: 召唤动画（已通过 SUMMONER_CASTING 处理）
            // 状态 1: 攻击状态（嘴部张开）
            if (status == 1 && ParasiteAnimations.isMoving(this, state.isMoving())) {
                return state.setAndContinue(SUMMONER_ATTACK);
            }
            // 状态 0 或 2: 默认移动
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                return state.setAndContinue(IDLE);
            }
            return state.setAndContinue(status == 2 || getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
        }
        if (kind == Kind.MANDUCATER) {
            int status = getManducaterStatus();
            if (status == 1) {
                return state.setAndContinue(MANDUCATER_ATTACK);
            } else if (status == 10) {
                return state.setAndContinue(MANDUCATER_SUMMON);
            } else if (status == 25) {
                return state.setAndContinue(MANDUCATER_EVADE);
            }
            // 默认状态 (status == 0 或 2)
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                return state.setAndContinue(IDLE);
            }
            return state.setAndContinue(status == 2 || getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
        }
        if (kind == Kind.BOLSTER) {
            return switch (getBolsterAction()) {
                case BARRAGE_WINDUP -> state.setAndContinue(BOLSTER_STATUS_25);
                case BARRAGE -> state.setAndContinue(BOLSTER_STATUS_3);
                case SUPPORT, VOMIT, ORB -> state.setAndContinue(BOLSTER_STATUS_15);
                default -> state.setAndContinue(ParasiteAnimations.isMoving(this, state.isMoving()) ? WALK : IDLE);
            };
        }
        if (supportsBurrowing() && isBurrowing()) {
            return state.setAndContinue(DIG);
        }
        if (kind == Kind.VERMIN) {
            return state.setAndContinue(IDLE);
        }
        if (isFlying(kind)) {
            // Yelloweye 冲锋状态动画（触手弯曲）
            if (kind == Kind.YELLOWEYE && entityData.get(YELLOWEYE_CHARGING)) {
                return state.setAndContinue(YELLOWEYE_CHARGE);
            }
            return state.setAndContinue(FLY);
        }
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(IDLE);
        }
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
    }

    private PlayState bolsterOverlayAnimation(AnimationState<AdaptedVariantEntity> state) {
        if (activeKind() != Kind.BOLSTER) {
            return PlayState.STOP;
        }
        return switch (getBolsterAction()) {
            case BARRAGE_WINDUP -> state.setAndContinue(BOLSTER_ATTACK_STATUS_25);
            case SUPPORT, VOMIT, ORB -> state.setAndContinue(BOLSTER_ATTACK_STATUS_15);
            default -> PlayState.STOP;
        };
    }

    private float meleeDamage() {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (activeKind() == Kind.LONGARMS) {
            damage *= 1.0F + (1.0F - getHealth() / getMaxHealth());
        }
        return damage;
    }

    private boolean performBolsterSweep(LivingEntity center) {
        if (level().isClientSide || getBolsterAction().blocksMelee()) {
            return false;
        }
        lastBolsterCombatTick = tickCount;
        setBolsterAction(BolsterAction.MELEE, 12);
        triggerAnim("bolster_attack_controller", "attack");
        playSound(ModSounds.get("mob.swipe"), 2.5F, 0.75F + random.nextFloat() * 0.2F);
        boolean hit = false;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(2.0D), this::isValidParasiteTarget)) {
            float healthBefore = target.getHealth();
            // 横扫动画和实际伤害在同一服务器 tick 结算，避免仅播放动画而未造成伤害。
            boolean targetHit = target.hurt(damageSources().mobAttack(this), meleeDamage());
            if (targetHit) {
                applyBolsterMinimumDamage(target, healthBefore);
                applyBolsterVariantAttack(target);
                if (random.nextFloat() < 0.50F) {
                    target.addEffect(new MobEffectInstance(ModMobEffects.COTH, 1200, 0), this);
                }
                double x = target.getX() - getX();
                double z = target.getZ() - getZ();
                double length = Math.max(0.001D, Math.sqrt(x * x + z * z));
                target.push(x / length * 0.35D, 0.35D, z / length * 0.35D);
            }
            hit |= targetHit;
        }
        return hit;
    }

    private void applyBolsterVariantAttack(LivingEntity target) {
        switch (getBolsterVariant()) {
            case BERSERKER -> target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 100, 0), this);
            case VIRULENT -> target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 100, 0), this);
            default -> {
            }
        }
    }

    /** 适应体近战攻击间隔，匹配原版寄生体的快速攻击节奏。 */
    private static final class FastMeleeAttackGoal extends MeleeAttackGoal {
        private FastMeleeAttackGoal(PathfinderMob mob, double speedModifier) {
            super(mob, speedModifier, false);
        }

        @Override
        protected int getTicksUntilNextAttack() {
            return 4;
        }
    }

    private void applyBolsterMinimumDamage(LivingEntity target, float healthBefore) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel).minimumDamage() || !target.isAlive()) {
            return;
        }
        float dealt = Math.max(0.0F, healthBefore - target.getHealth());
        if (dealt < 4.0F) {
            target.invulnerableTime = 0;
            target.hurt(damageSources().fellOutOfWorld(), 4.0F - dealt);
            target.invulnerableTime = 0;
        }
    }

    private void tickBolster(ServerLevel level) {
        initializeBolsterTendrils();
        if (getHealth() < getMaxHealth() && tickCount - lastBolsterCombatTick >= 80 && consumeParasiteKill()) {
            heal(getMaxHealth() * 0.001F);
        }
        if (isInWaterOrBubble()) {
            LivingEntity target = getTarget();
            if (target != null && target.isInWaterOrBubble()) {
                Vec3 direction = target.getEyePosition().subtract(getEyePosition());
                if (direction.lengthSqr() > 0.01D) {
                    direction = direction.normalize().scale(0.08D);
                    setDeltaMovement(getDeltaMovement().add(direction));
                }
            } else if (horizontalCollision && tickCount % 10 == 0) {
                setDeltaMovement(getDeltaMovement().add(0.0D, 0.18D, 0.0D));
            }
        }
        if (!bolsterSpecialMovesEnabled() || getBolsterAction() != BolsterAction.NONE) {
            return;
        }
        if (residueCooldown > 0) {
            residueCooldown--;
            return;
        }
        setBolsterAction(BolsterAction.VOMIT, 40);
        playSound(ModSounds.get("adapted.v"), 2.0F, 0.85F + random.nextFloat() * 0.2F);
        spreadBolsterResidue();
        residueCooldown = 600 + random.nextInt(601);
    }

    private boolean bolsterSpecialMovesEnabled() {
        return level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).specialMoves();
    }

    private boolean bolsterOrbEnabled() {
        return level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).ordinaryOrb();
    }

    private void spreadBolsterResidue() {
        BlockPos origin = blockPosition();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (random.nextFloat() < 0.45F) {
                    placeBolsterResidue(origin.offset(x, 3, z));
                }
            }
        }
    }

    private void placeBolsterResidue(BlockPos start) {
        for (int y = 0; y <= 6; y++) {
            BlockPos candidate = start.below(y);
            if (level().getBlockState(candidate).canBeReplaced()
                    && !level().getBlockState(candidate.below()).canBeReplaced()) {
                level().setBlock(candidate, ModBlocks.INFESTED_REMAINS.get().defaultBlockState(), 3);
                return;
            }
        }
    }

    private void initializeBolsterTendrils() {
        float health = getMaxHealth() * 0.25F;
        if (entityData.get(BOLSTER_LEFT_TENDRIL) < 0.0F) {
            entityData.set(BOLSTER_LEFT_TENDRIL, health);
        }
        if (entityData.get(BOLSTER_RIGHT_TENDRIL) < 0.0F) {
            entityData.set(BOLSTER_RIGHT_TENDRIL, health);
        }
    }

    private void damageBolsterTendril(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker == null || attacker == this || amount <= 0.0F) {
            return;
        }
        Vec3 toAttacker = attacker.position().subtract(position());
        Vec3 right = new Vec3(Math.cos(Math.toRadians(getYRot())), 0.0D,
                -Math.sin(Math.toRadians(getYRot())));
        EntityDataAccessor<Float> tendril = toAttacker.dot(right) >= 0.0D
                ? BOLSTER_RIGHT_TENDRIL : BOLSTER_LEFT_TENDRIL;
        float previous = entityData.get(tendril);
        if (previous <= 0.0F) {
            return;
        }
        float remaining = Math.max(0.0F, previous - amount);
        entityData.set(tendril, remaining);
        if (remaining == 0.0F) {
            reduceAllResistances(Integer.MAX_VALUE);
            addEffect(new MobEffectInstance(ModMobEffects.BLEED, 200, 1), this);
            addEffect(new MobEffectInstance(ModMobEffects.RAGE, 600, 1), this);
            playSound(ModSounds.get("mob.tendril"), 2.0F, 0.8F);
        }
    }

    public BolsterVariant getBolsterVariant() {
        int value = entityData.get(BOLSTER_VARIANT);
        return BolsterVariant.values()[Math.max(0, Math.min(BolsterVariant.values().length - 1, value))];
    }

    public boolean isAdaptedBolster() {
        return activeKind() == Kind.BOLSTER;
    }

    private void setBolsterVariant(BolsterVariant variant) {
        entityData.set(BOLSTER_VARIANT, variant.ordinal());
    }

    public boolean isLeftBolsterTendrilAttached() {
        return entityData.get(BOLSTER_LEFT_TENDRIL) != 0.0F;
    }

    public boolean isRightBolsterTendrilAttached() {
        return entityData.get(BOLSTER_RIGHT_TENDRIL) != 0.0F;
    }

    private BolsterAction getBolsterAction() {
        int value = entityData.get(BOLSTER_ACTION);
        return BolsterAction.values()[Math.max(0, Math.min(BolsterAction.values().length - 1, value))];
    }

    private void setBolsterAction(BolsterAction action, int ticks) {
        entityData.set(BOLSTER_ACTION, action.ordinal());
        entityData.set(BOLSTER_ACTION_TICKS, Math.max(0, ticks));
    }

    private void triggerAttackAnimation() {
        triggerAnim("bolster_attack_controller", "attack");
    }

    private void updateCloak() {
        if (!cloaked || --cloakTicks > 0) {
            return;
        }
        endCloak();
        abilityCooldown = 140;
    }

    private void endCloak() {
        cloaked = false;
        cloakTicks = 0;
        setInvisible(false);
    }

    private void updateArachnidaParts() {
        if (arachnidaAbdomen != null) {
            arachnidaAbdomen.updatePosition();
        }
        if (arachnidaHead != null) {
            arachnidaHead.updatePosition();
        }
    }

    private void tickArachnidaTether() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (target != null && !target.isAlive()) {
                setTarget(null);
            }
            setArachnidaTarget(0);
            if (getArachnidaStatus() == 3) {
                setArachnidaStatus(0);
            }
            return;
        }
        if (entityData.get(ARACHNIDA_TARGET) == 0) {
            if (!arachnidaCanPull) {
                arachnidaCanPull = true;
                arachnidaPullingTicks = 0;
            }
            return;
        }
        if (!arachnidaCanPull || !hasLineOfSight(target) || distanceToSqr(target) <= 0.0D) {
            setArachnidaTarget(0);
            setArachnidaStatus(Math.min(getArachnidaStatus(), 2));
            return;
        }

        setArachnidaStatus(3);
        getNavigation().stop();
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        target.stopRiding();
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 5, false, false), this);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 5, false, false), this);
        Vec3 pull = position().subtract(target.position());
        if (pull.lengthSqr() > 0.0D) {
            target.setDeltaMovement(target.getDeltaMovement().add(pull.normalize().scale(0.2D)));
            target.hurtMarked = true;
        }
        arachnidaPullingTicks++;
        if (arachnidaPullingTicks > ARACHNIDA_MAX_PULL_TICKS) {
            setArachnidaTarget(0);
            arachnidaCanPull = false;
            setArachnidaStatus(0);
        }
    }

    private void fireArachnidaPullProjectile(LivingEntity target) {
        PullingBallEntity projectile = ModEntities.PULLING_BALL.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 view = getViewVector(1.0F);
        Vec3 start = new Vec3(getX() + view.x, getY() + getEyeHeight() - 0.2D, getZ() + view.z);
        Vec3 direction = target.getEyePosition().subtract(start);
        if (direction.lengthSqr() <= 0.0D) {
            return;
        }
        projectile.moveTo(start.x, start.y, start.z, getYRot(), getXRot());
        projectile.setOwner(this);
        projectile.setDeltaMovement(direction.normalize().scale(0.1D));
        level().addFreshEntity(projectile);
    }

    public boolean isAdaptedArachnida() {
        return activeKind() == Kind.ARACHNIDA;
    }

    public int getArachnidaStatus() {
        return entityData.get(ARACHNIDA_STATUS);
    }

    public int getArachnidaSkin() {
        return entityData.get(ARACHNIDA_SKIN);
    }

    public void setArachnidaSkin(int skin) {
        entityData.set(ARACHNIDA_SKIN, skin >= 5 && skin <= 7 ? skin : 0);
    }

    @Nullable
    public LivingEntity getArachnidaTetherTarget() {
        Entity target = level().getEntity(entityData.get(ARACHNIDA_TARGET));
        return target instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void setArachnidaStatus(int status) {
        entityData.set(ARACHNIDA_STATUS, status);
    }

    private void setArachnidaTarget(int entityId) {
        if (arachnidaCanPull || entityId == 0) {
            arachnidaPullingTicks = 0;
            arachnidaCanPull = true;
            entityData.set(ARACHNIDA_TARGET, entityId);
        }
    }

    private void tickManducater() {
        if (manducaterVomitTicks > 0) {
            manducaterVomitTicks--;
            if (level().isClientSide && manducaterVomitTicks > 0) {
                // 客户端呕吐粒子效果
                for (int i = 0; i < 2; i++) {
                    double offsetX = (random.nextDouble() - 0.5D) * 0.5D;
                    double offsetY = getBbHeight() * 0.5D;
                    double offsetZ = (random.nextDouble() - 0.5D) * 0.5D;
                    level().addParticle(ParticleTypes.ITEM_SLIME,
                            getX() + offsetX, getY() + offsetY, getZ() + offsetZ,
                            (random.nextDouble() - 0.5D) * 0.1D,
                            random.nextDouble() * 0.1D,
                            (random.nextDouble() - 0.5D) * 0.1D);
                }
            }
        }
        if (manducaterEvadeCooldown > 0) {
            manducaterEvadeCooldown--;
        }
        // 自动恢复到默认状态
        if (!level().isClientSide && tickCount % 20 == 0) {
            int status = getManducaterStatus();
            if (status == 1 || status == 10 || status == 25) {
                setManducaterStatus(0);
            }
        }
    }

    private int getManducaterStatus() {
        return entityData.get(MANDUCATER_STATUS);
    }

    private void setManducaterStatus(int status) {
        entityData.set(MANDUCATER_STATUS, status);
    }

    public int getSummonerStatus() {
        return entityData.get(SUMMONER_STATUS);
    }

    private void setSummonerStatus(int status) {
        entityData.set(SUMMONER_STATUS, status);
    }

    private void tickSummoner() {
        if (tickCount % 20 == 0) {
            if (level() instanceof ServerLevel serverLevel) {
                summonTracker.prune(serverLevel);
            }
            int status = getSummonerStatus();
            if (status == 1 || status == 25 || status == 100) {
                setSummonerStatus(0);
            }
        }
    }

    private void breakSoftBlockTowards(LivingEntity target) {
        if (blockBreakCooldown > 0 || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.01D) {
            return;
        }
        horizontal = horizontal.normalize();
        double reach = activeKind() == Kind.BOLSTER ? 2.0D
                : activeKind() == Kind.MANDUCATER || activeKind() == Kind.VISCERA
                || activeKind() == Kind.YELLOWEYE ? 1.7D : 1.0D;
        BlockPos origin = BlockPos.containing(getX() + horizontal.x * reach,
                getY() + getBbHeight() * 0.5D, getZ() + horizontal.z * reach);
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                    || hardness < 0.0F || hardness > blockBreakHardness()) {
                continue;
            }
            if (ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this)) {
                blockBreakCooldown = 20;
            }
            return;
        }
    }

    private float blockBreakHardness() {
        if (activeKind() == Kind.BOLSTER) {
            return getBolsterVariant() == BolsterVariant.BREACHER ? 7.0F : 3.5F;
        }
        return 3.0F;
    }

    private static boolean breaksSoftBlocks(Kind kind) {
        return kind != Kind.BURROWER && kind != Kind.DEVOURER && kind != Kind.TOZOON && !isFlying(kind);
    }

    private void pullTargets(double radius, double strength) {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius), this::isValidParasiteTarget)) {
            Vec3 pull = position().subtract(target.position());
            if (pull.lengthSqr() > 0.001D) {
                pull = pull.normalize().scale(strength);
                target.push(pull.x, 0.08D, pull.z);
            }
        }
    }

    private void damageBolsterBarrage() {
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(8.0D));
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(8.0D), this::isValidParasiteTarget)) {
            if (!hasLineOfSight(target)) {
                continue;
            }
            float healthBefore = target.getHealth();
            target.invulnerableTime = 0;
            if (target.hurt(damageSources().mobAttack(this), meleeDamage() * 0.25F)) {
                applyBolsterMinimumDamage(target, healthBefore);
                applyBolsterVariantAttack(target);
                if (random.nextFloat() < 0.50F) {
                    target.addEffect(new MobEffectInstance(ModMobEffects.COTH, 1200, 0), this);
                }
            }
            target.invulnerableTime = 0;
        }
    }

    private void fireProjectile(LivingEntity target, ParasiteProjectileEntity.Mode mode, double speed,
                                float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.55D));
        projectile.configure(this, mode, start, target.getEyePosition(), speed, damage, radius, lifetime);
        level().addFreshEntity(projectile);
    }

    private void fireWebProjectile(LivingEntity target, int webKind) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.5D));
        projectile.configure(this, ParasiteProjectileEntity.Mode.WEB, start,
                target.getEyePosition(), 1.0D, 6.0F, 0.9D, 75, target);
        projectile.setWebKind(webKind);
        level().addFreshEntity(projectile);
    }

    private boolean summonBiomass() {
        return BiomassEntity.spawnFromVomit(this, this, 6, List.of(
                new BiomassEntity.SummonOption(ModEntities.RUPTER.get(), 0.1D, 1),
                new BiomassEntity.SummonOption(ModEntities.SIM_HUMAN.get(), 0.3D, 2),
                new BiomassEntity.SummonOption(ModEntities.SIM_COW.get(), 0.3D, 2),
                new BiomassEntity.SummonOption(ModEntities.SIM_WOLF.get(), 0.3D, 2)));
    }

    @Override
    public int getSummonCapacity() {
        return activeKind() == Kind.SUMMONER ? SUMMONER_TOTAL_CAPACITY : 0;
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

    private void spawnLice() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LiceEntity lice = ModEntities.LICE.get().create(serverLevel);
        if (lice == null) {
            return;
        }
        lice.moveTo(getX(), getY() - 0.5D, getZ(), getYRot(), 0.0F);
        lice.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(lice.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        lice.setTarget(getTarget());
        serverLevel.addFreshEntity(lice);
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        EntityType<?> type = getType();
        if (type == ModEntities.ADA_BOLSTER.get()) return Kind.BOLSTER;
        if (type == ModEntities.ADA_BURROWER.get()) return Kind.BURROWER;
        if (type == ModEntities.ADA_DEVOURER.get()) return Kind.DEVOURER;
        if (type == ModEntities.ADA_LONGARMS.get()) return Kind.LONGARMS;
        if (type == ModEntities.ADA_MANDUCATER.get()) return Kind.MANDUCATER;
        if (type == ModEntities.ADA_REEKER.get()) return Kind.REEKER;
        if (type == ModEntities.ADA_SUMMONER.get()) return Kind.SUMMONER;
        if (type == ModEntities.ADA_TOZOON.get()) return Kind.TOZOON;
        if (type == ModEntities.ADA_VERMIN.get()) return Kind.VERMIN;
        if (type == ModEntities.ADA_VISCERA.get()) return Kind.VISCERA;
        if (type == ModEntities.ADA_YELLOWEYE.get()) return Kind.YELLOWEYE;
        return Kind.ARACHNIDA;
    }

    @Override
    protected boolean supportsBurrowing() {
        Kind activeKind = activeKind();
        return activeKind == Kind.BURROWER || activeKind == Kind.TOZOON;
    }

    @Override
    protected int burrowSkillCooldownTicks() {
        return activeKind() == Kind.BURROWER ? 80 : 140;
    }

    @Override
    protected double bodyFollowDistance() {
        Kind kind = activeKind();
        return kind == Kind.BURROWER || kind == Kind.TOZOON ? 1.9D : super.bodyFollowDistance();
    }

    @Override
    protected int bodySegmentCount() {
        Kind kind = activeKind();
        return kind == Kind.BURROWER || kind == Kind.TOZOON ? 4 : 0;
    }

    @Override
    protected boolean shouldTriggerBodyPartEffect() {
        int body = getBodyNumber();
        return activeKind() == Kind.TOZOON ? body >= 1 && body <= 3 : super.shouldTriggerBodyPartEffect();
    }

    @Override
    protected void bodyPartEffect() {
        if (activeKind() != Kind.TOZOON || isBurrowing()) {
            return;
        }
        AABB area = new AABB(blockPosition()).inflate(5.0D);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                candidate -> !(candidate instanceof Parasite)
                        && (!(candidate instanceof Player player) || !player.getAbilities().instabuild))) {
            if (performTozoonAoeAttack(target)) {
                return;
            }
        }
    }

    @Override
    protected SoundEvent burrowSound() {
        return activeKind() == Kind.BURROWER
                ? ModSounds.ADAPTED_BURROWER_DIG.get()
                : ModSounds.ADAPTED_TOZOON_DIG.get();
    }

    private final class BurrowerMeleeGoal extends MeleeAttackGoal {
        private BurrowerMeleeGoal() {
            super(AdaptedVariantEntity.this, 1.30D, false);
        }

        @Override
        protected int getTicksUntilNextAttack() {
            return 10;
        }
    }

    private static boolean isFlying(Kind kind) {
        return kind == Kind.VERMIN || kind == Kind.YELLOWEYE;
    }

    private static boolean isFlyingType(EntityType<?> type) {
        return type == ModEntities.ADA_VERMIN.get() || type == ModEntities.ADA_YELLOWEYE.get();
    }

    private static boolean isDevourerType(EntityType<?> type) {
        return type == ModEntities.ADA_DEVOURER.get();
    }

    private static boolean isArachnidaType(EntityType<?> type) {
        return type == ModEntities.ADA_ARACHNIDA.get();
    }

    private boolean performTozoonAoeAttack(Entity target) {
        if (isBurrowing() || target == null) {
            return false;
        }
        startBodyAttackAnimation();
        if (getBodyNumber() == 0) {
            triggerAnim("attack_controller", "get_attack_timer");
        }
        playSound(ModSounds.MOB_SWIPE.get(), 2.0F, 1.0F);
        AABB area = new AABB(target.getX(), target.getY(), target.getZ(),
                target.getX() + 1.0D, target.getY() + 1.0D, target.getZ() + 1.0D).inflate(1.5D);
        boolean hit = false;
        for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate.isAlive() && !(candidate instanceof Parasite)
                        && hasLineOfSight(candidate))) {
            hit |= super.doHurtTarget(nearby);
        }
        return hit;
    }

    private final class TozoonAoeAttackGoal extends Goal {
        private static final int ATTACK_INTERVAL_TICKS = 10;
        private static final double ATTACK_DISTANCE_SQR = 16.0D;
        private int attackCooldown;

        private TozoonAoeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return activeKind() == Kind.TOZOON && !isBurrowing()
                    && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return activeKind() == Kind.TOZOON && !isBurrowing() && target != null && target.isAlive();
        }

        @Override
        public void start() {
            attackCooldown = 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            if (distance > ATTACK_DISTANCE_SQR || !hasLineOfSight(target)) {
                getNavigation().moveTo(target, 1.3D);
                return;
            }
            getNavigation().stop();
            if (attackCooldown <= 0) {
                attackCooldown = ATTACK_INTERVAL_TICKS;
                performTozoonAoeAttack(target);
            }
        }
    }

    private final class DevourerMeleeGoal extends MeleeAttackGoal {
        private DevourerMeleeGoal() {
            super(AdaptedVariantEntity.this, 1.30D, false);
        }

        @Override
        public boolean canUse() {
            return isInWaterOrBubble() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return isInWaterOrBubble() && super.canContinueToUse();
        }
    }

    private final class ArachnidaPullSkillGoal extends Goal {
        private int chargeTicks;
        private int castTicks;
        private int shots;
        private boolean casting;
        private boolean castSoundPlayed;

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && arachnidaCanPull
                    && entityData.get(ARACHNIDA_TARGET) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && arachnidaCanPull
                    && entityData.get(ARACHNIDA_TARGET) == 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (!casting) {
                int status = getArachnidaStatus();
                double distance = distanceToSqr(target);
                if ((status == 1 || status == 2) && distance < 900.0D && distance >= 25.0D
                        && hasLineOfSight(target)) {
                    chargeTicks++;
                }
                if (chargeTicks >= ARACHNIDA_SKILL_CHARGE_TICKS) {
                    if (!onGround()) {
                        chargeTicks = 0;
                        return;
                    }
                    casting = true;
                    castTicks = 0;
                    shots = 0;
                    castSoundPlayed = false;
                    setArachnidaStatus(11);
                    getNavigation().stop();
                }
                return;
            }

            if (!onGround()) {
                finishCast();
                return;
            }
            setArachnidaStatus(11);
            getNavigation().stop();
            castTicks++;
            if (shots == 2 && !castSoundPlayed) {
                castSoundPlayed = true;
                playSound(ModSounds.get("attack.ranrac"), 4.0F, random.nextFloat() * 0.4F + 1.0F);
            }
            if (castTicks % ARACHNIDA_SKILL_SHOT_INTERVAL == 0) {
                if (getTarget() == null || !hasLineOfSight(target)) {
                    finishCast();
                    return;
                }
                fireArachnidaPullProjectile(target);
                shots++;
                if (shots >= ARACHNIDA_SKILL_SHOTS) {
                    finishCast();
                }
            }
        }

        @Override
        public void stop() {
            if (casting) {
                finishCast();
            }
        }

        private void finishCast() {
            casting = false;
            chargeTicks = 0;
            castTicks = 0;
            shots = 0;
            castSoundPlayed = false;
            arachnidaPullingTicks = 60;
            if (entityData.get(ARACHNIDA_TARGET) == 0) {
                setArachnidaStatus(0);
            }
        }
    }

    private final class ArachnidaWaterLeapGoal extends Goal {
        private int chargeTicks;
        private int airborneTicks;
        private boolean leaping;
        private double targetX;
        private double targetZ;
        private float targetYOffset;

        private ArachnidaWaterLeapGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return leaping || isInWaterOrBubble();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (!leaping) {
                if (target != null && target.isAlive() && getArachnidaStatus() <= 2) {
                    chargeTicks++;
                    if (chargeTicks >= 20) {
                        leaping = true;
                        airborneTicks = 1;
                        targetX = target.getX();
                        targetZ = target.getZ();
                        targetYOffset = Math.max(0.0F, (float) (target.getY() - getY()) * 0.07F);
                    }
                } else if (chargeTicks > 0) {
                    chargeTicks--;
                }
            }

            if (!leaping) {
                return;
            }
            airborneTicks++;
            if (airborneTicks == 2 && onGround()) {
                setArachnidaStatus(10);
                getNavigation().stop();
                double deltaX = targetX - getX();
                double deltaZ = targetZ - getZ();
                double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                if (horizontal > 0.0D) {
                    Vec3 motion = getDeltaMovement();
                    setDeltaMovement(motion.x + deltaX / horizontal * 1.35D + motion.x * 0.3D,
                            0.7D + targetYOffset,
                            motion.z + deltaZ / horizontal * 1.35D + motion.z * 0.3D);
                    hasImpulse = true;
                }
            }
            if (airborneTicks >= 3 && onGround()) {
                leaping = false;
                chargeTicks = 0;
                airborneTicks = 0;
                setArachnidaStatus(2);
            }
        }
    }

    private final class ArachnidaMeleeGoal extends MeleeAttackGoal {
        private ArachnidaMeleeGoal() {
            super(AdaptedVariantEntity.this, 1.3D, false);
        }

        @Override
        public void tick() {
            int status = getArachnidaStatus();
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            if (status == 3 || status == 10 || status == 11) {
                getNavigation().stop();
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                return;
            }
            super.tick();
            double distance = distanceToSqr(target);
            boolean fast = distance > 64.0D || arachnidaAttackAnimationCooldown == 0;
            setArachnidaStatus(fast ? 2 : 1);
            getNavigation().moveTo(target, fast ? 1.3D : 1.0D);
        }

        @Override
        public void stop() {
            super.stop();
            if (entityData.get(ARACHNIDA_TARGET) == 0 && getArachnidaStatus() <= 2) {
                setArachnidaStatus(0);
            }
        }
    }

    private final class BolsterSupportGoal extends Goal {
        private BolsterSupportGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return bolsterSpecialMovesEnabled() && supportCooldown <= 0 && getTarget() != null
                    && getBolsterAction() == BolsterAction.NONE;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            setBolsterAction(BolsterAction.SUPPORT, 40);
            getNavigation().stop();
            playSound(ModSounds.get("attack.bano"), 3.0F, 1.0F + random.nextFloat() * 0.4F);
            for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(24.0D), entity -> entity instanceof Parasite && entity.isAlive())) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 3), AdaptedVariantEntity.this);
                ally.clearFire();
            }
            supportCooldown = 1200;
        }
    }

    private final class BolsterOrbGoal extends Goal {
        private int castTicks;

        private BolsterOrbGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return bolsterOrbEnabled() && secondaryCooldown <= 0 && target != null && target.isAlive()
                    && distanceToSqr(target) <= 576.0D && getBolsterAction() == BolsterAction.NONE;
        }

        @Override
        public boolean canContinueToUse() {
            return castTicks < 40 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            castTicks = 0;
            setBolsterAction(BolsterAction.ORB, 40);
            getNavigation().stop();
            playSound(ModSounds.ORB_START.get(), 2.5F, 0.8F);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (++castTicks == 25) {
                ScaryOrbEntity orb = new ScaryOrbEntity(ModEntities.SCARY_ORB.get(), level(),
                        AdaptedVariantEntity.this);
                orb.setTimings(20, 10);
                orb.setAnchor(position().add(0.0D, getBbHeight() + 1.0D, 0.0D));
                level().addFreshEntity(orb);
            }
        }

        @Override
        public void stop() {
            secondaryCooldown = 600;
        }
    }

    private final class BarrageGoal extends Goal {
        private int barrageTicks;

        private BarrageGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return bolsterSpecialMovesEnabled() && abilityCooldown <= 0 && onGround() && target != null
                    && distanceToSqr(target) <= 196.0D && getBolsterAction() == BolsterAction.NONE;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return barrageTicks < 101 && target != null && target.isAlive();
        }

        @Override
        public void start() {
            barrageTicks = 0;
            getNavigation().stop();
            setBolsterAction(BolsterAction.BARRAGE_WINDUP, 20);
        }

        @Override
        public void tick() {
            barrageTicks++;
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            if (barrageTicks <= 20) {
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.7D,
                            getZ(), 8, 0.8D, 1.1D, 0.8D, 0.03D);
                }
                if (barrageTicks == 2) {
                    playSound(ModSounds.get("attack.bano"), 4.0F, 1.0F + random.nextFloat() * 0.4F);
                }
                if (barrageTicks == 20) {
                    setBolsterAction(BolsterAction.BARRAGE, 81);
                    playSound(ModSounds.get("mob.swipe"), 5.0F, 1.0F);
                }
                return;
            }
            if (barrageTicks % 4 == 0) {
                damageBolsterBarrage();
                pullTargets(8.0D, 0.22D);
                if (barrageTicks % 8 == 0) {
                    playSound(ModSounds.get("mob.swipe"), 3.0F, 0.9F + random.nextFloat() * 0.2F);
                }
            }
        }

        @Override
        public void stop() {
            setBolsterAction(BolsterAction.NONE, 0);
            abilityCooldown = 1200;
        }
    }

    private final class ShockwaveGoal extends Goal {
        private int chargeTicks;

        private ShockwaveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && onGround() && target != null && distanceToSqr(target) <= 256.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return chargeTicks < 40 && getTarget() != null;
        }

        @Override
        public void start() {
            chargeTicks = 0;
            getNavigation().stop();
            triggerAttackAnimation();
        }

        @Override
        public void tick() {
            int currentTick = ++chargeTicks;
            if (level() instanceof ServerLevel serverLevel && currentTick <= 30 && currentTick % 3 == 0) {
                double radius = 0.8D + currentTick * 0.08D;
                serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.15D, getZ(),
                        10, radius, 0.08D, radius, 0.02D);
            }
            if (currentTick == 30) {
                hurtNearby(AdaptedVariantEntity.this, 10.0D, meleeDamage() * 1.20F, true);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2D, getZ(),
                            2, 0.4D, 0.1D, 0.4D, 0.0D);
                    serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.15D, getZ(),
                            24, 2.5D, 0.12D, 2.5D, 0.08D);
                }
                triggerAttackAnimation();
            }
        }

        @Override
        public void stop() {
            abilityCooldown = 180;
        }
    }

    private final class CloakGoal extends Goal {
        private CloakGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && !cloaked && target != null && target.isAlive()
                    && getHealth() >= getMaxHealth() * 0.40F && distanceToSqr(target) >= 16.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return cloaked && cloakTicks > 0;
        }

        @Override
        public void start() {
            cloaked = true;
            cloakTicks = 80;
            setInvisible(true);
        }
    }

    private final class ManducaterEvadeGoal extends Goal {
        private ManducaterEvadeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return manducaterEvadeCooldown <= 0 && target != null && target.isAlive()
                    && distanceToSqr(target) <= 36.0D && hasLineOfSight(target);
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
            // 设置闪避动画状态
            setManducaterStatus(25);
            entityData.set(MANDUCATER_STILL_ANI, false);

            // 向侧面闪避
            Vec3 direction = target.position().subtract(position());
            if (direction.lengthSqr() > 0.001D) {
                direction = direction.normalize();
                // 向垂直于目标方向的侧面移动
                double dodgeX = -direction.z * 0.5D;
                double dodgeZ = direction.x * 0.5D;
                if (random.nextBoolean()) {
                    dodgeX = -dodgeX;
                    dodgeZ = -dodgeZ;
                }
                setDeltaMovement(dodgeX, 0.25D, dodgeZ);
            }

            manducaterEvadeCooldown = 100;
            playSound(ModSounds.get("adapted.v"), 1.0F, 1.2F + random.nextFloat() * 0.2F);
        }
    }

    private final class ManducaterVomitGoal extends Goal {
        private ManducaterVomitGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return secondaryCooldown <= 0 && target != null && target.isAlive() && hasLineOfSight(target)
                    && distanceToSqr(target) >= 16.0D && distanceToSqr(target) <= 400.0D;
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
            getLookControl().setLookAt(target, 30.0F, 30.0F);

            // 发射呕吐物投射物
            fireProjectile(target, ParasiteProjectileEntity.Mode.VOMIT, 0.75D, 10.0F, 2.0D, 80);

            // 设置呕吐动画状态
            manducaterVomitTicks = 40;
            setManducaterStatus(1);

            triggerAttackAnimation();
            playSound(ModSounds.get("adapted.v"), 2.0F, 0.8F + random.nextFloat() * 0.2F);
            secondaryCooldown = 120;
        }
    }

    private final class ChargeGoal extends Goal {
        private int chargeTicks;

        private ChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && onGround() && target != null
                    && distanceToSqr(target) >= 25.0D && distanceToSqr(target) <= 484.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return chargeTicks < 24 && target != null && target.isAlive();
        }

        @Override
        public void start() {
            chargeTicks = 0;
            abilityCooldown = 160;
            entityData.set(REEKER_CHARGING, true);
        }

        @Override
        public void stop() {
            entityData.set(REEKER_CHARGING, false);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            Vec3 direction = target.position().subtract(position());
            if (direction.lengthSqr() > 0.001D) {
                direction = direction.normalize();
                setDeltaMovement(direction.x * 0.88D, getDeltaMovement().y, direction.z * 0.88D);
            }
            if (distanceToSqr(target) <= 9.0D) {
                doHurtTarget(target);
                hurtNearby(AdaptedVariantEntity.this, 3.5D, meleeDamage() * 1.35F, true);
                chargeTicks = 24;
                return;
            }
            chargeTicks++;
        }
    }

    private final class ReekerPullGoal extends Goal {
        private int pullingTicks;
        private LivingEntity pullTarget;

        private ReekerPullGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (reekerPullingCooldown > 0 || entityData.get(REEKER_PULLING) > 0) {
                return false;
            }
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double distSq = distanceToSqr(target);
            return distSq >= 36.0D && distSq <= 576.0D && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return pullingTicks < 400 && pullTarget != null && pullTarget.isAlive()
                    && hasLineOfSight(pullTarget);
        }

        @Override
        public void start() {
            pullingTicks = 0;
            pullTarget = getTarget();
            entityData.set(REEKER_PULLING, 400);
            entityData.set(REEKER_STILL_ANI, false);
            getNavigation().stop();
            playSound(ModSounds.get("adapted.v"), 1.5F, 0.7F + random.nextFloat() * 0.3F);
        }

        @Override
        public void stop() {
            entityData.set(REEKER_PULLING, 0);
            entityData.set(REEKER_STILL_ANI, false);
            reekerPullingCooldown = 200;
            pullTarget = null;
        }

        @Override
        public void tick() {
            if (pullTarget == null || !pullTarget.isAlive()) {
                pullingTicks = 400;
                return;
            }

            getLookControl().setLookAt(pullTarget, 30.0F, 30.0F);

            // 每10 tick拉拽一次
            if (pullingTicks % 10 == 0) {
                Vec3 pull = position().subtract(pullTarget.position());
                if (pull.lengthSqr() > 0.001D) {
                    pull = pull.normalize().scale(0.35D);
                    pullTarget.push(pull.x, 0.08D, pull.z);
                    pullTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2),
                            AdaptedVariantEntity.this);
                }
            }

            // 发射拉拽弹丸
            if (pullingTicks == 15) {
                entityData.set(REEKER_STILL_ANI, true);
                firePullProjectile(pullTarget);
            }

            pullingTicks++;
            int remaining = 400 - pullingTicks;
            entityData.set(REEKER_PULLING, Math.max(0, remaining));
        }
    }

    private void firePullProjectile(LivingEntity target) {
        PullingBallEntity projectile = ModEntities.PULLING_BALL.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.5D));
        Vec3 direction = target.getEyePosition().subtract(start).normalize().scale(0.8D);
        projectile.moveTo(start.x, start.y, start.z, getYRot(), getXRot());
        projectile.setOwner(this);
        projectile.setDeltaMovement(direction);
        level().addFreshEntity(projectile);
        playSound(ModSounds.get("attack.throw"), 1.5F, 1.0F + random.nextFloat() * 0.2F);
    }

    private void tickReeker() {
        int pulling = entityData.get(REEKER_PULLING);
        if (pulling > 0) {
            entityData.set(REEKER_PULLING, pulling - 1);
            if (pulling == 1) {
                entityData.set(REEKER_STILL_ANI, false);
            }
        }
    }

    private final class SummonGoal extends Goal {
        private int castTicks;
        private int successfulSummons;
        private int failedSummons;
        private static final int SPAWN_INTERVAL = 20;

        private SummonGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return abilityCooldown <= 0 && getTarget() != null && distanceToSqr(getTarget()) <= 400.0D
                    && !isInWaterOrBubble();
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() != null && getTarget().isAlive() && !isInWaterOrBubble()
                    && successfulSummons < SUMMONER_LIMIT && failedSummons <= 4;
        }

        @Override
        public void start() {
            castTicks = 0;
            successfulSummons = 0;
            failedSummons = 0;
            getNavigation().stop();
            entityData.set(SUMMONER_CASTING, true);
            setSummonerStatus(10);
            playSound(ModSounds.get("adapted.v"), 2.0F, 0.7F + random.nextFloat() * 0.3F);
        }

        @Override
        public void tick() {
            castTicks++;
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            if (castTicks % SPAWN_INTERVAL == 0) {
                if (getUsedSummonCapacity() < getSummonCapacity()) {
                    playSound(ModSounds.get("acanra.special"), 3.0F, 1.0F);
                }
                if (summonBiomass()) {
                    successfulSummons++;
                    level().broadcastEntityEvent(AdaptedVariantEntity.this, SUMMON_EVENT);
                } else {
                    failedSummons++;
                }
            }
        }

        @Override
        public void stop() {
            entityData.set(SUMMONER_CASTING, false);
            setSummonerStatus(0);
            abilityCooldown = SUMMONER_COOLDOWN_TICKS;
        }
    }

    private final class VomitGoal extends Goal {
        private int vomitTicks;

        private VomitGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return secondaryCooldown <= 0 && target != null && hasLineOfSight(target)
                    && distanceToSqr(target) >= 16.0D && distanceToSqr(target) <= 576.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return vomitTicks < 40;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            vomitTicks = 0;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            setSummonerStatus(100);
            playSound(ModSounds.get("adapted.v"), 2.0F, 0.8F + random.nextFloat() * 0.2F);
        }

        @Override
        public void tick() {
            vomitTicks++;
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            // The original attack creates its cloud directly in front of the Summoner.
            if (vomitTicks == 15 && target != null) {
                ParasiteCombatEffects.spawnVomitCloud(AdaptedVariantEntity.this,
                        6.5D, 5.0F, 100, 300, 40);
                level().broadcastEntityEvent(AdaptedVariantEntity.this, VOMIT_EVENT);
            }
        }

        @Override
        public void stop() {
            setSummonerStatus(0);
            secondaryCooldown = 100;
            vomitTicks = 0;
        }
    }

    private final class VerminFlightGoal extends Goal {
        private VerminFlightGoal() {
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
            getMoveControl().setWantedPosition(target.getX(), target.getY() + 4.0D, target.getZ(), 1.1D);
            if (abilityCooldown > 0 || distanceToSqr(target) > 576.0D) {
                return;
            }
            int liceCount = level().getEntitiesOfClass(LiceEntity.class, getBoundingBox().inflate(32.0D),
                    lice -> lice.getTarget() == target).size();
            if (liceCount >= 8) {
                fireProjectile(target, ParasiteProjectileEntity.Mode.BOMB, 0.75D, 12.0F, 2.5D, 80);
                abilityCooldown = 80;
            } else {
                spawnLice();
                spawnLice();
                abilityCooldown = 40;
            }
            triggerAttackAnimation();
        }
    }

    private final class SideLeapGoal extends Goal {
        private SideLeapGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && onGround() && target != null
                    && distanceToSqr(target) >= 9.0D && distanceToSqr(target) <= 144.0D;
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
            Vec3 direction = target.position().subtract(position());
            if (direction.lengthSqr() > 0.001D) {
                direction = direction.normalize();
                setDeltaMovement(-direction.z * 0.70D, 0.40D, direction.x * 0.70D);
            }
            abilityCooldown = 70;
        }
    }

    private final class YelloweyeRangedGoal extends Goal {
        private YelloweyeRangedGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && target != null && target.isAlive() && hasLineOfSight(target);
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
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            boolean acid = ++rangedShots % 5 == 0;
            if (acid) {
                fireProjectile(target, ParasiteProjectileEntity.Mode.ACID, 0.70D, 14.0F, 2.25D, 100);
                abilityCooldown = 90;
                playSound(ModSounds.get("emana.shooting"), 1.0F, 1.5F);
                playSound(ModSounds.get("attack.emana"), 2.0F, 1.0F);
            } else {
                fireProjectile(target, ParasiteProjectileEntity.Mode.SPINE, 1.15D, 7.0F, 0.85D, 70);
                fireProjectile(target, ParasiteProjectileEntity.Mode.SPINE, 1.05D, 7.0F, 0.85D, 70);
                abilityCooldown = 36;
                playSound(ModSounds.get("emana.shooting"), 1.0F, 1.0F);
            }
            playSound(ModSounds.get("aemana.shootingpost"), 2.0F, 1.0F);
            triggerAttackAnimation();
        }
    }

    private final class YelloweyeFlightGoal extends Goal {
        private YelloweyeFlightGoal() {
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
                entityData.set(YELLOWEYE_CHARGING, false);
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            getMoveControl().setWantedPosition(target.getX(), target.getY() + 2.5D, target.getZ(), 1.0D);

            // 当接近目标时设置冲锋状态（触手弯曲动画）
            double distSqr = distanceToSqr(target);
            if (distSqr <= 16.0D) {
                entityData.set(YELLOWEYE_CHARGING, true);
            } else {
                entityData.set(YELLOWEYE_CHARGING, false);
            }

            if (secondaryCooldown <= 0 && distSqr <= 4.0D) {
                doHurtTarget(target);
                secondaryCooldown = 20;
            }
        }

        @Override
        public void stop() {
            entityData.set(YELLOWEYE_CHARGING, false);
        }
    }

    // PullingBallOwner 接口实现
    @Override
    public boolean captureTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (activeKind() == Kind.ARACHNIDA) {
            if (!arachnidaCanPull || target != getTarget() || !hasLineOfSight(target)
                    || entityData.get(ARACHNIDA_TARGET) != 0) {
                return false;
            }
            setArachnidaStatus(3);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 5, false, false), this);
            setArachnidaTarget(target.getId());
            return true;
        }
        if (activeKind() != Kind.REEKER) {
            return false;
        }
        // 拉拽目标并施加效果
        Vec3 pull = position().subtract(target.position());
        if (pull.lengthSqr() > 0.001D) {
            pull = pull.normalize().scale(0.5D);
            target.push(pull.x, 0.15D, pull.z);
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3), this);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1), this);
        playSound(ModSounds.get("attack.throw"), 1.0F, 0.8F + random.nextFloat() * 0.4F);
        return true;
    }

    @Override
    public boolean isValidPullTarget(LivingEntity target) {
        if (activeKind() == Kind.ARACHNIDA) {
            return arachnidaCanPull && entityData.get(ARACHNIDA_TARGET) == 0
                    && target == getTarget() && isValidParasiteTarget(target) && hasLineOfSight(target);
        }
        return isValidParasiteTarget(target);
    }

    @Override
    public double pullProjectileCaptureRadius() {
        return activeKind() == Kind.ARACHNIDA ? 2.0D : PullingBallOwner.super.pullProjectileCaptureRadius();
    }

    @Override
    public double pullProjectileAccelerationMultiplier() {
        return activeKind() == Kind.ARACHNIDA ? 4.0D
                : PullingBallOwner.super.pullProjectileAccelerationMultiplier();
    }

    @Override
    public int pullProjectileMaxAge() {
        return activeKind() == Kind.ARACHNIDA ? 0 : PullingBallOwner.super.pullProjectileMaxAge();
    }

    private static final class ArachnidaPart extends PartEntity<AdaptedVariantEntity> {
        private final String name;
        private final float angle;
        private final float radius;
        private final float yOffset;
        private final float width;
        private final float height;
        private final float damageVulnerability;

        private ArachnidaPart(AdaptedVariantEntity parent, String name, float angle, float radius,
                              float yOffset, float width, float height, float damageVulnerability) {
            super(parent);
            this.name = name;
            this.angle = angle;
            this.radius = radius;
            this.yOffset = yOffset;
            this.width = width;
            this.height = height;
            this.damageVulnerability = damageVulnerability;
        }

        private void updatePosition() {
            AdaptedVariantEntity parent = getParent();
            float facing = parent.yBodyRot * Mth.DEG_TO_RAD + angle;
            setPos(parent.getX() + radius * Mth.cos(facing), parent.getY() + yOffset,
                    parent.getZ() + radius * Mth.sin(facing));
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
            return getParent().isAlive();
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            AdaptedVariantEntity parent = getParent();
            if (!parent.level().isClientSide && parent.random.nextBoolean()) {
                EffectStacking.apply(parent, ModMobEffects.BLEED, 80, 0);
            }
            return parent.hurt(source, amount * damageVulnerability);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(width, height);
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

    public enum BolsterVariant {
        NORMAL,
        BERSERKER,
        VIRULENT,
        BREACHER
    }

    private enum BolsterAction {
        NONE(false),
        MELEE(false),
        SUPPORT(true),
        BARRAGE_WINDUP(true),
        BARRAGE(true),
        VOMIT(true),
        ORB(true);

        private final boolean blocksMelee;

        BolsterAction(boolean blocksMelee) {
            this.blocksMelee = blocksMelee;
        }

        private boolean blocksMelee() {
            return blocksMelee;
        }
    }

    public enum Kind {
        ARACHNIDA,
        BOLSTER,
        BURROWER,
        DEVOURER,
        LONGARMS,
        MANDUCATER,
        REEKER,
        SUMMONER,
        TOZOON,
        VERMIN,
        VISCERA,
        YELLOWEYE
    }
}
