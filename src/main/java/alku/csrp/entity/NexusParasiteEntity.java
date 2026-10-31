package alku.csrp.entity;

import alku.csrp.config.MobsConfig;
import alku.csrp.config.RuntimeToggles;
import alku.csrp.config.WorldConfig;
import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.block.FogBlock;
import alku.csrp.event.StatusEffectEvents;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModBlocks;
import alku.csrp.infection.BlockInfestation;
import alku.csrp.infection.InfestationSpreadLimiter;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModSounds;
import alku.csrp.particle.SrpParticleEffects;
import alku.csrp.world.DislodgmentSystem;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.ParasiteBiomeDecorator;
import alku.csrp.world.SrpCoreSystems;
import alku.csrp.world.SrpWorldData;
import alku.csrp.world.gen.WorldGenParasiteNexusProtection1;
import alku.csrp.world.gen.WorldGenParasiteNexusProtection2;
import alku.csrp.world.gen.WorldGenParasiteNexusProtection3;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelAnimationState;
import alku.csrp.animation.CitadelPlayState;
import alku.csrp.animation.CitadelRawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Legacy Nexus families: stationary stage growth, reinforcement, and battlefield support. */
public final class NexusParasiteEntity extends PrimitiveParasiteEntity implements SummonCapacityOwner {
    private static final EntityDataAccessor<Float> BODY = SynchedEntityData.defineId(
            NexusParasiteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            NexusParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FLOOR_TIMER = SynchedEntityData.defineId(
            NexusParasiteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RTTS = SynchedEntityData.defineId(
            NexusParasiteEntity.class, EntityDataSerializers.BOOLEAN);

    private final CitadelRawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final CitadelRawAnimation BECKON_ATTACK_AGE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final CitadelRawAnimation BECKON_BODY = ParasiteAnimations.loop(this, "get_body");
    private final CitadelRawAnimation BECKON_ATTACK_BODY = ParasiteAnimations.loop(this,
            "get_body.get_parasite_status_1");
    private final CitadelRawAnimation BECKON_FLOOR_TIMER = ParasiteAnimations.loop(this, "get_floor_timer");
    private final CitadelRawAnimation BECKON_ATTACK_FLOOR_TIMER = ParasiteAnimations.loop(this,
            "get_floor_timer.get_parasite_status_1");
    private final CitadelRawAnimation BECKON_BURIED = ParasiteAnimations.play(this, "get_floor_timer");
    private static final byte BECKON_BURIED_EVENT = 50;
    private static final byte NEXUS_GROWTH_PARTICLES_EVENT = 51;
    private static final int TEMPORARY_BECKON_LIFETIME = 300;
    private static final int BECKON_PULSE_INTERVAL = 20;
    private static final int BECKON_LOCK_RELEASE_TICKS = 200;
    private static final int BECKON_SIV_INITIAL_RANGE = 12;
    private static final int BECKON_SIV_EXTRA_RANGE = 5;

    /** Original EntityAINexusGrow.spawnLeem 2% roll. */
    private static final double NEXUS_ROOTER_CHANCE = 0.02D;
    /** Original EntityAINexusGrow.tickss reset, shared by growth and Rooter checks. */
    private static final int NEXUS_ROOTER_INTERVAL = 20;
    private static final int NEXUS_GROWTH_RETRY_CHECKS = 20_000;
    private static final int LEGACY_GROWTH_UNIT = 20;
    private static final int DISPATCHER_INITIAL_CHECKS = 20;
    private static final int DISPATCHER_FOG_MIN_Y_OFFSET = -2;
    private static final int DISPATCHER_FOG_MAX_Y_OFFSET = 4;

    private final Kind kind;
    private final List<String> storedParasiteIds = new ArrayList<>();
    private int dispatcherCrude;
    private int dispatcherInfected;
    private int dispatcherMangler;
    private int dispatcherFlesh;
    private int dispatcherStorageChecks = DISPATCHER_INITIAL_CHECKS;
    private int dispatcherSelectorTick;
    private boolean dispatcherStorageRunning;
    private final SummonCapacityTracker summonTracker = new SummonCapacityTracker();
    private int growthTicks;
    private int growthDelayTicks;
    private int summonCooldown;
    private int beckonAttackTimer;
    private int beckonBatchCount;
    private double beckonExtraBudget;
    private boolean beckonGroupActive;
    private boolean beckonLocked;
    private boolean beckonCounted;
    private int bombCooldown;
    private int supportCooldown;
    private int rooterCycleTicks;
    private int rooterGroundChecks;
    private int blockBreakCooldown;
    private int forcedEvolutionCooldown;
    private int colonyPlacementProgress;
    private int temporaryLifetimeTicks = -1;
    private int rooterSummonCooldown = NEXUS_ROOTER_INTERVAL;
    private int growthSelectorTick;
    private boolean growthGoalRunning;
    private boolean growthPhaseAllowed = true;
    private boolean canGrow = true;
    private boolean dispatcherFogDissipationStarted;
    private boolean creationProgressCaptured;
    private int creationEvolutionPhase;
    private int creationDevelopmentLevel;
    /** Original EntityAINexusGrow.dimLock — highest stage allowed in this dimension, or -1. */
    private final int dimensionStageLock;

    public NexusParasiteEntity(EntityType<? extends NexusParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.experience;
        // Original EntityAINexusGrow constructor: maximumStageList locks a per-dimension stage cap.
        this.dimensionStageLock = Config.nexusStageLock(level);
        growthDelayTicks = MobsConfig.nexusGrowthDelayTicks(kind, random);
        if (kind.family == Family.BECKON && !Config.useEvolutionPhases() && !Config.ignoreCooldownAtSpawn()) {
            beckonAttackTimer = -MobsConfig.beckonCooldownSeconds(kind.stage) * 20;
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!creationProgressCaptured && level() instanceof ServerLevel serverLevel) {
            creationEvolutionPhase = SrpWorldData.get(serverLevel).evolutionPhase();
            creationDevelopmentLevel = EvolutionSystem.ubiquitousDevelopment(serverLevel.getServer());
            creationProgressCaptured = true;
        }
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, kind.defaultFollowRange);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BODY, 0.5F);
        builder.define(PARASITE_STATUS, 0);
        builder.define(FLOOR_TIMER, -1.0F);
        builder.define(RTTS, false);
    }

    public float getBODY() {
        return entityData.get(BODY);
    }

    public void setBODY(float increment) {
        float newValue = getBODY() + increment;
        if (newValue > 0.6F) {
            newValue = 0.6F;
        }
        if (newValue < 0.0F) {
            newValue = 0.0F;
        }
        entityData.set(BODY, newValue);
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    public float getFloorTimer() {
        return entityData.get(FLOOR_TIMER);
    }

    public void setFloorTimer(float value) {
        entityData.set(FLOOR_TIMER, value);
    }

    public boolean getRTTS() {
        return entityData.get(RTTS);
    }

    private void setRTTS(boolean value) {
        entityData.set(RTTS, value);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == NEXUS_GROWTH_PARTICLES_EVENT) {
            for (int i = 0; i < 11; i++) {
                double velocityX = random.nextGaussian() * 0.02D;
                double velocityY = random.nextGaussian() * 0.02D;
                double velocityZ = random.nextGaussian() * 0.02D;
                level().addParticle(ParticleTypes.EXPLOSION,
                        getX() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth(),
                        getY() + 0.5D + random.nextFloat() * getBbHeight(),
                        getZ() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth(),
                        velocityX, velocityY, velocityZ);
            }
            return;
        }
        Kind activeKind = activeKind();
        if (id == BECKON_BURIED_EVENT && activeKind.family == Family.BECKON
                && activeKind.stage < 4) {
            setFloorTimer(beckonBuriedDepth(activeKind.stage));
            triggerAnim("floor_controller", "beckon_buried");
            return;
        }
        super.handleEntityEvent(id);
    }

    private static float beckonBuriedDepth(int stage) {
        return switch (stage) {
            case 1 -> 2.6F;
            case 2 -> 4.4F;
            case 3 -> 5.8F;
            default -> 0.0F;
        };
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (activeKind().isRooterBall()) {
            return;
        }
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, (target, level) -> isValidParasiteTarget(target)));
        if (activeKind() == Kind.DISPATCHER_SIV) {
            goalSelector.addGoal(4, new DispatcherPodGoal());
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        Kind activeKind = activeKind();
        if (activeKind.family == Family.DISPATCHER) {
            tickDispatcherStorageGoal();
        }
        if (activeKind.supportsOrdinaryGrowth() && growthDelayTicks > 0) {
            tickGrowthGoal(serverLevel, activeKind);
        }
    }

    private void tickDispatcherStorageGoal() {
        boolean scanGoals = dispatcherSelectorTick == 0;
        dispatcherSelectorTick = (dispatcherSelectorTick + 1) % 3;
        if (dispatcherStorageRunning) {
            dispatcherStorageRunning = shouldRunDispatcherStorageGoal();
        } else if (scanGoals) {
            dispatcherStorageRunning = shouldRunDispatcherStorageGoal();
        }
        if (dispatcherStorageRunning) {
            storeNearbyParasite();
        }
    }

    private boolean shouldRunDispatcherStorageGoal() {
        if (--dispatcherStorageChecks < 0) {
            dispatcherStorageChecks = getTarget() == null ? 80 : 10;
            return true;
        }
        return false;
    }

    private void tickGrowthGoal(ServerLevel level, Kind activeKind) {
        // 1.12's EntityAITasks evaluates inactive goals every third AI tick and running goals every tick.
        boolean scanGoals = growthSelectorTick == 0;
        growthSelectorTick = (growthSelectorTick + 1) % 3;
        if (growthGoalRunning) {
            growthGoalRunning = shouldRunGrowthGoal(level, activeKind);
        } else if (scanGoals) {
            growthGoalRunning = shouldRunGrowthGoal(level, activeKind);
        }
        if (!growthGoalRunning) {
            return;
        }
        if (activeKind.stage == 3 && ParasiteBiomeDecorator.parasiteBiomeIndex(level.getBiome(blockPosition())) != 0) {
            rooterSummonCooldown = NEXUS_GROWTH_RETRY_CHECKS;
            return;
        }
        // Existing 26.3 saves store these quantities scaled by 20; retain that format.
        int nextGrowth = growthTicks + LEGACY_GROWTH_UNIT;
        if (canGrow) {
            growthTicks = nextGrowth;
        }
        if (nextGrowth > growthDelayTicks) {
            if (RuntimeToggles.mobEvolution() && activeKind.stage == 3 && !stageFourUnlocked(level, activeKind)) {
                rooterSummonCooldown = NEXUS_GROWTH_RETRY_CHECKS;
                return;
            }
            evolve();
        }
    }

    private boolean shouldRunGrowthGoal(ServerLevel level, Kind activeKind) {
        if (--rooterSummonCooldown < 0) {
            rooterSummonCooldown = NEXUS_ROOTER_INTERVAL;
            checkGrowthPhase(level, activeKind, new Random());
            trySpawnRooterSi(level);
            return growthPhaseAllowed;
        }
        return false;
    }

    private void checkGrowthPhase(ServerLevel level, Kind activeKind, Random roll) {
        if (dimensionStageLock >= activeKind.stage + 1) {
            growthPhaseAllowed = false;
        } else if (Config.useEvolutionPhases()) {
            int phase = SrpWorldData.get(level).evolutionPhase();
            if (phase == -1 || phase == 0) {
                growthPhaseAllowed = false;
            } else if (phase >= 1 && phase <= 10) {
                growthPhaseAllowed = !(roll.nextDouble() < Config.nexusGrowthPenalty(activeKind.stage, phase));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            float buried = getFloorTimer();
            if (buried > 0.0F) {
                float remaining = buried - 0.08F;
                setFloorTimer(remaining > 0.0F ? remaining : -0.1F);
            }
            return;
        }
        getNavigation().stop();
        decrementCooldowns();

        // Update body animation for BECKON family
        Kind activeKind = activeKind();
        if (activeKind.family == Family.BECKON) {
            if (getParasiteStatus() == 0) {
                setBODY(0.04F);  // Expand body
            } else {
                setBODY(-0.04F); // Contract body
            }
        }

        if (activeKind.family == Family.ROOTER || activeKind.isRooterBall()) {
            tickRooterCycle(activeKind);
        }
        if (activeKind.isRooterBall()) {
            return;
        }
        if (activeKind.family == Family.BECKON && temporaryLifetimeTicks > 0
                && --temporaryLifetimeTicks <= 0) {
            discard();
            return;
        }
        if (activeKind.family == Family.DISPATCHER && tickCount % 40 == 0) {
            placeNestFog(activeKind.stage);
        }
        if (activeKind.family == Family.DISPATCHER && activeKind.stage == 4) {
            tryPlaceFirstColony();
        }
        if (activeKind.family == Family.BECKON) {
            if (!isNoAi()) {
                tickBeckonSummoning(activeKind);
            }
        } else if (!isNoAi() && summonCooldown <= 0 && performFamilyAbility(activeKind)) {
            setParasiteStatus(1);
            summonCooldown = activeKind.summonCooldown;
        } else if (summonCooldown > 0 && getParasiteStatus() == 1) {
            // Reset status after summoning
            setParasiteStatus(0);
        }
        if (activeKind.family == Family.BECKON && activeKind.stage == 4
                && forcedEvolutionCooldown <= 0 && forceEvolveNearbyParasite()) {
            forcedEvolutionCooldown = 100;
        }
        // Original EntityVenkrolSIV.tick ran VenkrolTornadoLogic every tick while the Venkrol stood
        // under open sky during a thunderstorm (SRPConfigWorld.venkrolTornadoEnabled, default true).
        if (activeKind.family == Family.BECKON && activeKind.stage == 4
                && level().isThundering() && level().isRaining()
                && level().canSeeSky(blockPosition().above())) {
            createStormVortex();
        }
        if (activeKind.family == Family.BECKON && tickCount % Math.max(20, 100 - activeKind.stage * 15) == 0
                && level() instanceof ServerLevel serverLevel) {
            BlockInfestation.infestAround(serverLevel, blockPosition().below(),
                    Math.max(0, Math.min(3, activeKind.stage - 1)), InfestationSpreadLimiter.Type.BECKON);
        }
        // 调度柱随阶段周期性感染脚下方块（原版 EntityAIBlockInfest ~200t 间隔）
        if (activeKind.family == Family.DISPATCHER && tickCount % 200 == 0
                && level() instanceof ServerLevel dispatcherLevel) {
            BlockInfestation.infestAround(dispatcherLevel, blockPosition().below(),
                    Math.max(1, Math.min(3, activeKind.stage)), InfestationSpreadLimiter.Type.BECKON);
        }
        if (tickCount % 10 == 0) {
            breakBlocksTowardsTarget(activeKind);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide() && isDeadOrDying() && !dispatcherFogDissipationStarted
                && activeKind() == Kind.DISPATCHER_SIV) {
            dispatcherFogDissipationStarted = true;
            dissipateDispatcherFog();
        }
    }

    private void tryPlaceFirstColony() {
        if (tickCount < 1_200 || random.nextInt(10) != 0 || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!SrpWorldData.get(serverLevel).colonies().isEmpty()) {
            colonyPlacementProgress = -1_000;
            return;
        }
        if (++colonyPlacementProgress < 200) {
            return;
        }
        colonyPlacementProgress = -100;
        for (int attempt = 0; attempt < 3; attempt++) {
            int offsetX = (5 + random.nextInt(7)) * (random.nextBoolean() ? 1 : -1);
            int offsetZ = (5 + random.nextInt(7)) * (random.nextBoolean() ? 1 : -1);
            if (SrpCoreSystems.placeColony(serverLevel, blockPosition().offset(offsetX, 0, offsetZ))) {
                colonyPlacementProgress = -1_000;
                return;
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Kind activeKind = activeKind();
        boolean damageRedirected = false;
        if (activeKind.family == Family.ROOTER && !level.isClientSide()) {
            List<NexusParasiteEntity> cysts = rootmassCystsInRange(activeKind);
            if (!cysts.isEmpty()) {
                // Legacy Rooters split subsequent hits between nearby Rootmass Cysts.
                float sharedDamage = amount / cysts.size();
                for (NexusParasiteEntity cyst : cysts) {
                    cyst.hurtServer(level, source, sharedDamage);
                }
                damageRedirected = true;
            } else {
                spawnRootmassCysts(MobsConfig.rooterCystLimit(activeKind.stage));
            }
        }

        float incomingDamage = damageRedirected ? 0.0F : amount;
        if (!activeKind.isRooterBall() && getParasiteStatus() == 0) {
            incomingDamage *= 0.4F;
        }
        boolean hurt = super.hurtServer(level, source,
                source.is(DamageTypeTags.IS_FIRE) ? incomingDamage * 4.0F : incomingDamage);
        if (!hurt || level.isClientSide() || activeKind.isRooterBall()) {
            return hurt;
        }
        if (bombCooldown <= 0 && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && source.getEntity() instanceof LivingEntity attacker && attacker.distanceToSqr(this) <= 36.0D) {
            spawnBombVolley(level, activeKind);
            bombCooldown = 80;
        }
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return switch (activeKind().stage) {
            case 1 -> 10;
            case 2 -> 8;
            case 3 -> 6;
            case 4 -> 4;
            default -> 0;
        };
    }

    @Override
    protected float damageAdaptationPerHit() {
        return switch (activeKind().stage) {
            case 1 -> 0.07F;
            case 2 -> 0.125F;
            case 3 -> 0.17F;
            case 4 -> 0.25F;
            default -> 0.0F;
        };
    }

    @Override
    protected int maxLearnableDamageSources() {
        return switch (activeKind().stage) {
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 15;
            case 4 -> 23;
            default -> 0;
        };
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return switch (activeKind().stage) {
            case 1 -> 0.70F;
            case 2 -> 0.80F;
            case 3, 4 -> 0.90F;
            default -> 0.0F;
        };
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return switch (activeKind().stage) {
            case 1 -> 0.70F;
            case 2 -> 0.50F;
            case 3 -> 0.30F;
            case 4 -> 0.10F;
            default -> 0.0F;
        };
    }

    @Override
    public void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers) {
        Kind activeKind = activeKind();
        if (activeKind.isRooterBall()) {
            return;
        }
        controllers.add(new CitadelAnimationController<>(this, "age_controller", 0, this::ageAnimation));
        if (activeKind.family == Family.BECKON && activeKind.stage < 4) {
            controllers.add(new CitadelAnimationController<>(this, "body_controller", 0, this::bodyAnimation));
            controllers.add(new CitadelAnimationController<>(this, "floor_controller", 0, this::floorAnimation)
                    .triggerableAnim("beckon_buried", BECKON_BURIED));
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("nexus_growth", growthTicks);
        tag.putInt("nexus_growth_delay", growthDelayTicks);
        tag.putInt("nexus_summon_cooldown", summonCooldown);
        tag.putInt("nexus_support_cooldown", supportCooldown);
        tag.putInt("nexus_rooter_cycle", rooterCycleTicks);
        tag.putInt("nexus_rooter_ground_checks", rooterGroundChecks);
        tag.putBoolean("nexus_rooter_rtts", getRTTS());
        tag.putInt("nexus_block_break_cooldown", blockBreakCooldown);
        tag.putInt("nexus_forced_evolution_cooldown", forcedEvolutionCooldown);
        tag.putInt("nexus_temporary_lifetime", temporaryLifetimeTicks);
        tag.putBoolean("nexus_can_grow", canGrow);
        tag.putBoolean("nexus_creation_captured", creationProgressCaptured);
        tag.putInt("nexus_creation_phase", creationEvolutionPhase);
        tag.putInt("nexus_creation_development", creationDevelopmentLevel);
        tag.putFloat("nexus_body", getBODY());
        tag.putInt("nexus_parasite_status", getParasiteStatus());
        tag.putFloat("nexus_floor_timer", getFloorTimer());
        tag.putInt("nexus_dispatcher_crude", dispatcherCrude);
        tag.putInt("nexus_dispatcher_infected", dispatcherInfected);
        tag.putInt("nexus_dispatcher_flesh", dispatcherFlesh);
        ValueOutput.TypedOutputList<String> storedParasites = tag.list("nexus_dispatcher_stored", Codec.STRING);

        for (String id : storedParasiteIds) {
            storedParasites.add(id);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        growthTicks = tag.getIntOr("nexus_growth", 0);
        growthDelayTicks = tag.getInt("nexus_growth_delay").orElseGet(this::defaultGrowthDelay);
        rooterSummonCooldown = NEXUS_ROOTER_INTERVAL;
        growthSelectorTick = 0;
        growthGoalRunning = false;
        growthPhaseAllowed = true;
        dispatcherStorageChecks = DISPATCHER_INITIAL_CHECKS;
        dispatcherSelectorTick = 0;
        dispatcherStorageRunning = false;
        summonCooldown = tag.getIntOr("nexus_summon_cooldown", 0);
        supportCooldown = tag.getIntOr("nexus_support_cooldown", 0);
        rooterCycleTicks = Mth.clamp(tag.getIntOr("nexus_rooter_cycle", 0), 0, 20);
        rooterGroundChecks = Mth.clamp(tag.getIntOr("nexus_rooter_ground_checks", 0), 0, 6);
        setRTTS(tag.getBooleanOr("nexus_rooter_rtts", false));
        blockBreakCooldown = tag.getIntOr("nexus_block_break_cooldown", 0);
        forcedEvolutionCooldown = tag.getIntOr("nexus_forced_evolution_cooldown", 0);
        temporaryLifetimeTicks = tag.getInt("nexus_temporary_lifetime").orElse(-1);
        canGrow = tag.getBooleanOr("nexus_can_grow", true);
        creationProgressCaptured = tag.getBooleanOr("nexus_creation_captured", false);
        creationEvolutionPhase = tag.getIntOr("nexus_creation_phase", 0);
        creationDevelopmentLevel = tag.getIntOr("nexus_creation_development", 0);
        if (tag.read("nexus_body", Codec.FLOAT).isPresent()) {
            entityData.set(BODY, tag.getFloatOr("nexus_body", 0.0F));
        }
        if (tag.getInt("nexus_parasite_status").isPresent()) {
            setParasiteStatus(tag.getIntOr("nexus_parasite_status", 0));
        }
        if (tag.read("nexus_floor_timer", Codec.FLOAT).isPresent()) {
            setFloorTimer(tag.getFloatOr("nexus_floor_timer", 0.0F));
        }
        storedParasiteIds.clear();
        for (String id : tag.listOrEmpty("nexus_dispatcher_stored", Codec.STRING)) {
            if (Identifier.tryParse(id) != null) {
                storedParasiteIds.add(id);
            }
        }
        dispatcherCrude = tag.getInt("nexus_dispatcher_crude").orElseGet(() -> storedParasiteIds.stream()
                .mapToInt(id -> id.contains("incompleteform_small") ? 1
                        : id.contains("incompleteform_medium") ? 2 : 0).sum());
        dispatcherInfected = tag.getInt("nexus_dispatcher_infected").orElseGet(() ->
                (int) storedParasiteIds.stream().filter(id -> id.contains("sim_") || id.contains("mar_")).count());
        dispatcherFlesh = tag.getIntOr("nexus_dispatcher_flesh", 0);
        // EntityPDispatcher did not persist its mangler counter.
        dispatcherMangler = 0;
    }

    public Kind getKind() {
        return activeKind();
    }

    @Override
    public int getSummonCapacity() {
        Kind activeKind = activeKind();
        return switch (activeKind.family) {
            case BECKON -> MobsConfig.beckonTotalCapacity(activeKind.stage);
            case ROOTER -> MobsConfig.rooterCystLimit(activeKind.stage);
            default -> 0;
        };
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

    private CitadelPlayState ageAnimation(CitadelAnimationState<NexusParasiteEntity> state) {
        return state.setAndContinue(activeKind().family == Family.BECKON
                && activeKind().stage < 4 && getParasiteStatus() == 1
                ? BECKON_ATTACK_AGE : AGE_IN_TICKS);
    }

    private CitadelPlayState bodyAnimation(CitadelAnimationState<NexusParasiteEntity> state) {
        return state.setAndContinue(getParasiteStatus() == 1 ? BECKON_ATTACK_BODY : BECKON_BODY);
    }

    private CitadelPlayState floorAnimation(CitadelAnimationState<NexusParasiteEntity> state) {
        return state.setAndContinue(getParasiteStatus() == 1
                ? BECKON_ATTACK_FLOOR_TIMER : BECKON_FLOOR_TIMER);
    }

    private void decrementCooldowns() {
        if (summonCooldown > 0) summonCooldown--;
        if (bombCooldown > 0) bombCooldown--;
        if (blockBreakCooldown > 0) blockBreakCooldown--;
        if (forcedEvolutionCooldown > 0) forcedEvolutionCooldown--;
    }

    private boolean performFamilyAbility(Kind activeKind) {
        LivingEntity target = getTarget();
        return switch (activeKind.family) {
            case BECKON -> target != null && summonBeckonParasites(activeKind, target);
            case DISPATCHER -> target != null && summonDispatcherDefenses(activeKind, target);
            case ROOTER, ROOTERBALL -> false;
        };
    }

    /** 是否处于调度柱家族（供消失回收等外部逻辑判断）。 */
    public boolean isDispatcherFamily() {
        return activeKind().family == Family.DISPATCHER;
    }

    /** Return a previously deployed parasite to the stored list. */
    public boolean recallParasite(String entityTypeId) {
        return isDispatcherFamily() && entityTypeId != null && storedParasiteIds.add(entityTypeId);
    }

    private void storeNearbyParasite() {
        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB area = new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D, getZ() + 1.0D)
                .inflate(range);
        for (Mob candidate : level().getEntitiesOfClass(Mob.class, area, this::isStorableDispatcherParasite)) {
            storeDispatcherParasite(candidate);
        }
    }

    boolean storeDispatcherParasite(Mob candidate) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType());
        if (!candidate.isAlive() || id == null || !Csrp.MODID.equals(id.getNamespace())
                || id.getPath().equals("worker") || !dispatcherStorageGroundValid()) {
            return false;
        }
        String path = id.getPath();
        int stage = activeKind().stage;
        if (path.equals("buglin")) {
            storedParasiteIds.add("csrp:rupter");
        } else if (path.equals("movingflesh")) {
            if (++dispatcherFlesh >= 2) {
                dispatcherFlesh -= 2;
                addDispatcherPrimitive();
            }
        } else if (stage >= 2 && (path.startsWith("sim_") || path.startsWith("mar_"))) {
            dispatcherInfected++;
            storedParasiteIds.add(id.toString());
            if (dispatcherInfected >= 4) {
                dispatcherInfected -= 4;
                if (removeStoredMatches("sim_", 4)) {
                    addDispatcherPrimitive();
                }
            }
        } else if (stage >= 2 && (path.equals("incompleteform_small") || path.equals("incompleteform_medium"))) {
            dispatcherCrude += path.equals("incompleteform_small") ? 1 : 2;
            storedParasiteIds.add(id.toString());
            if (dispatcherCrude >= 6) {
                dispatcherCrude -= 6;
                if (removeStoredMatches("incompleteform", 6)) {
                    storedParasiteIds.add("csrp:heed");
                }
            }
        } else if (stage >= 3 && path.equals("rupter")) {
            storedParasiteIds.add("csrp:mangler");
        } else {
            if (stage >= 3 && path.equals("mangler")) {
                dispatcherMangler++;
            }
            storedParasiteIds.add(id.toString());
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, candidate.getX(),
                    candidate.getY() + candidate.getBbHeight() * 0.5D, candidate.getZ(),
                    11, 0.25D, 0.35D, 0.25D, 0.0D);
        }
        candidate.discard();
        return true;
    }

    private boolean dispatcherStorageGroundValid() {
        float hardness = 0.0F;
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos pos = blockPosition().below(depth);
            float value = level().getBlockState(pos).getDestroySpeed(level(), pos);
            if (value <= 0.0F) {
                return false;
            }
            hardness += value;
        }
        return hardness < 10.0F;
    }

    private boolean removeStoredMatches(String needle, int count) {
        for (int removed = 0; removed < count; removed++) {
            int index = -1;
            for (int i = 0; i < storedParasiteIds.size(); i++) {
                if (storedParasiteIds.get(i).contains(needle)) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                return false;
            }
            storedParasiteIds.remove(index);
        }
        return true;
    }

    private void addDispatcherPrimitive() {
        String[] primitives = {"pri_yelloweye", "pri_summoner", "pri_bolster", "pri_longarms",
                "pri_arachnida", "pri_reeker", "pri_manducater"};
        int selected = random.nextInt(primitives.length);
        for (int attempt = 0; attempt < 10; attempt++) {
            if (MobsConfig.dispatcherPrimitiveEnabled(selected)) {
                storedParasiteIds.add("csrp:" + primitives[selected]);
                return;
            }
            selected = (selected + 1) % primitives.length;
        }
    }

    private String takeStoredParasite(int index) {
        String id = storedParasiteIds.remove(index);
        if (id.contains("sim_")) {
            dispatcherInfected--;
        } else if (id.contains("incompleteform_small")) {
            dispatcherCrude--;
        } else if (id.contains("incompleteform_medium")) {
            dispatcherCrude -= 2;
        } else if (id.contains("mangler")) {
            dispatcherMangler--;
        }
        return id;
    }

    private void placeNestFog(int stage) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int count = Math.min(3, 1 + stage);
        int radius = dispatcherFogRadius(stage);
        for (int attempt = 0; attempt < 8 && count > 0; attempt++) {
            BlockPos candidate = blockPosition().offset(
                    random.nextInt(radius * 2 + 1) - radius,
                    random.nextInt(DISPATCHER_FOG_MAX_Y_OFFSET - DISPATCHER_FOG_MIN_Y_OFFSET + 1)
                            + DISPATCHER_FOG_MIN_Y_OFFSET,
                    random.nextInt(radius * 2 + 1) - radius);
            if (serverLevel.getBlockState(candidate).canBeReplaced()
                    && candidate.getY() > serverLevel.getMinY()) {
                serverLevel.setBlockAndUpdate(candidate,
                        ModBlocks.FOG.get().defaultBlockState());
                count--;
            }
        }
    }

    private void dissipateDispatcherFog() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos origin = blockPosition();
        int radius = dispatcherFogRadius(activeKind().stage);
        List<NexusParasiteEntity> otherDispatchers = serverLevel.getEntitiesOfClass(
                NexusParasiteEntity.class,
                getBoundingBox().inflate(radius * 2.0D + 1.0D,
                        DISPATCHER_FOG_MAX_Y_OFFSET - DISPATCHER_FOG_MIN_Y_OFFSET + 1.0D,
                        radius * 2.0D + 1.0D),
                entity -> entity != this && entity.isAlive()
                        && entity.activeKind().family == Family.DISPATCHER);
        for (BlockPos candidate : BlockPos.betweenClosed(
                origin.offset(-radius, DISPATCHER_FOG_MIN_Y_OFFSET, -radius),
                origin.offset(radius, DISPATCHER_FOG_MAX_Y_OFFSET, radius))) {
            BlockState state = serverLevel.getBlockState(candidate);
            if (state.is(ModBlocks.FOG.get()) && state.getValue(FogBlock.AIR) != 2
                    && otherDispatchers.stream().noneMatch(dispatcher ->
                            isInsideDispatcherFogVolume(dispatcher, candidate))) {
                serverLevel.setBlock(candidate, state.setValue(FogBlock.AIR, 2), 3);
            }
        }
    }

    private static boolean isInsideDispatcherFogVolume(NexusParasiteEntity dispatcher, BlockPos pos) {
        BlockPos origin = dispatcher.blockPosition();
        int radius = dispatcherFogRadius(dispatcher.activeKind().stage);
        int yOffset = pos.getY() - origin.getY();
        return Math.abs(pos.getX() - origin.getX()) <= radius
                && Math.abs(pos.getZ() - origin.getZ()) <= radius
                && yOffset >= DISPATCHER_FOG_MIN_Y_OFFSET
                && yOffset <= DISPATCHER_FOG_MAX_Y_OFFSET;
    }

    private static int dispatcherFogRadius(int stage) {
        return 5 + stage;
    }

    private boolean isStorableDispatcherParasite(Mob candidate) {
        return candidate != this
                && candidate instanceof Parasite
                && candidate.isAlive()
                && candidate.getTarget() == null
                && candidate.onGround()
                && candidate.tickCount > 100
                && DispatcherStorageCooldown.canStore(candidate)
                && !(candidate instanceof NexusParasiteEntity)
                && !(candidate instanceof PreeminentParasiteEntity)
                && !(candidate instanceof ArchitectEntity)
                && !(candidate instanceof FlamEntity)
                && !(candidate instanceof DerivedParasiteEntity)
                && !(candidate instanceof DeterrentParasiteEntity)
                && !(candidate instanceof BurrowingVariantEntity burrowing && burrowing.bodySegmentCount() > 0)
                && !(candidate instanceof MarauderTendrilEntity);
    }

    private void tickBeckonSummoning(Kind activeKind) {
        LivingEntity target = getTarget();
        if (target == null) {
            beckonBatchCount = 0;
            if (getParasiteStatus() != 0) {
                setParasiteStatus(0);
            }
        }
        if (beckonLocked) {
            if (--beckonAttackTimer <= -BECKON_LOCK_RELEASE_TICKS) {
                beckonLocked = false;
                beckonAttackTimer = -beckonCooldownTicks(activeKind);
            }
            return;
        }
        if (target == null) {
            return;
        }
        if (getParasiteStatus() != 1) {
            setParasiteStatus(1);
        }
        if (!target.isAlive() || !hasLineOfSight(target)) {
            if (beckonAttackTimer > 0) {
                beckonAttackTimer--;
            }
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.broadcastEntityEvent(this, (byte) 12);
        beckonAttackTimer++;
        if (beckonAttackTimer <= 0 || beckonAttackTimer % BECKON_PULSE_INTERVAL != 0) {
            return;
        }
        if (beckonAttackTimer == BECKON_PULSE_INTERVAL) {
            prepareBeckonGroup(activeKind);
        }
        summonTracker.prune(serverLevel);
        int maximumBatch = MobsConfig.beckonBatchLimit(activeKind.stage);
        if (beckonBatchCount < maximumBatch
                && getUsedSummonCapacity() < getSummonCapacity()
                && summonBeckonPulse(activeKind, target, false)) {
            beckonBatchCount++;
            if (activeKind.stage < 4 && canGrow) {
                growthTicks += LEGACY_GROWTH_UNIT;
            }
            if (beckonGroupActive) {
                lockNearbyBeckons(activeKind);
            }
        }
        if (beckonBatchCount >= maximumBatch) {
            if (beckonGroupActive && beckonExtraBudget > 0.0D
                    && beckonAttackTimer > maximumBatch * BECKON_PULSE_INTERVAL) {
                if (summonBeckonPulse(activeKind, target, true)) {
                    beckonExtraBudget -= 1.0D;
                }
            } else if (!beckonGroupActive || beckonExtraBudget <= 0.0D) {
                finishBeckonBatch(activeKind);
            }
        }
    }

    private boolean summonBeckonPulse(Kind activeKind, LivingEntity target, boolean extra) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (activeKind.stage == 4) {
            int range = extra ? BECKON_SIV_EXTRA_RANGE : BECKON_SIV_INITIAL_RANGE;
            LivingEntity center = extra ? this : target;
            return summonStageFour(serverLevel, center, target, range);
        }
        boolean airborne = getY() + 3.0D <= target.getY();
        if (activeKind.stage == 1 && airborne) {
            return false;
        }
        return BiomassEntity.spawnFromBeckon(this, target, activeKind.stage,
                MobsConfig.beckonSummonMobs(activeKind.stage, airborne));
    }

    private boolean summonStageFour(ServerLevel serverLevel, LivingEntity center, LivingEntity target, int range) {
        List<BiomassEntity.SummonOption> options = MobsConfig.beckonSummonMobs(4, false);
        if (options.isEmpty()) {
            return false;
        }

        java.util.Random selection = new java.util.Random();
        int xOffset = selection.nextInt(range);
        int zOffset = selection.nextInt(range);
        if (selection.nextBoolean()) {
            xOffset = -xOffset;
        }
        if (selection.nextBoolean()) {
            zOffset = -zOffset;
        }
        int index = selection.nextInt(options.size());
        int wraps = 0;
        while (true) {
            if (index >= options.size()) {
                index = 0;
                wraps++;
            }
            if (wraps == 2) {
                return false;
            }

            BiomassEntity.SummonOption option = options.get(index);
            if (option == null) {
                index++;
                continue;
            }
            if (selection.nextDouble() <= option.chance()) {
                if (!canReserveSummon(option.cost())) {
                    index++;
                    continue;
                }
                BlockPos position = BlockPos.containing(center.getX() + xOffset,
                        center.getY(), center.getZ() + zOffset);
                if (serverLevel.isEmptyBlock(position) && !serverLevel.isEmptyBlock(position.below())) {
                    Entity created = option.type().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
                    if (!(created instanceof Mob spawned)) {
                        return false;
                    }
                    spawned.snapTo(position.getX(), position.getY(), position.getZ(), getYRot(), getXRot());
                    spawned.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(position),
                            EntitySpawnReason.MOB_SUMMONED, null);
                    var followRange = spawned.getAttribute(Attributes.FOLLOW_RANGE);
                    if (followRange != null) {
                        followRange.setBaseValue(16.0D);
                    }
                    boolean temporaryBeckon = false;
                    if (activeKind().stage == 4 && spawned instanceof NexusParasiteEntity nexus
                            && nexus.activeKind().family == Family.BECKON) {
                        nexus.makeTemporaryBeckon();
                        temporaryBeckon = true;
                    }
                    if (spawned.getType() == ModEntities.BECKON_SI.get()
                            && serverLevel.getBlockState(position.below()).is(ModBlocks.INFESTED_STAIN.get())) {
                        return false;
                    }
                    if (!serverLevel.addFreshEntity(spawned)) {
                        return false;
                    }
                    if (temporaryBeckon) {
                        serverLevel.broadcastEntityEvent(spawned, BECKON_BURIED_EVENT);
                    }
                    reserveTrackedSummon(spawned.getUUID(), option.cost());
                    spawned.setTarget(target);
                    return true;
                }
                xOffset = selection.nextInt(range);
                zOffset = selection.nextInt(range);
                if (selection.nextBoolean()) {
                    xOffset = -xOffset;
                }
                if (selection.nextBoolean()) {
                    zOffset = -zOffset;
                }
            }
            index++;
        }
    }


    private void prepareBeckonGroup(Kind activeKind) {
        int groupSize = countNearbyBeckons(activeKind);
        beckonGroupActive = groupSize >= MobsConfig.beckonCapacityMinimum(activeKind.stage);
        beckonExtraBudget = beckonGroupActive
                ? Math.floor(groupSize * MobsConfig.beckonCapacityExtra(activeKind.stage)) : 0.0D;
        beckonBatchCount = 0;
    }

    private void finishBeckonBatch(Kind activeKind) {
        beckonExtraBudget = 0.0D;
        beckonGroupActive = false;
        beckonBatchCount = 0;
        beckonAttackTimer = -beckonCooldownTicks(activeKind);
        unlockNearbyBeckons(activeKind);
    }

    private int beckonCooldownTicks(Kind activeKind) {
        return MobsConfig.beckonCooldownSeconds(activeKind.stage) * 20;
    }

    private int countNearbyBeckons(Kind activeKind) {
        if (activeKind.stage == 4) {
            return 1;
        }
        int count = countConnectedBeckons(activeKind.stage, getUUID());
        clearBeckonCounted(activeKind.stage, getUUID());
        return count;
    }

    private int countConnectedBeckons(int stage, UUID sourceId) {
        int count = 1;
        for (NexusParasiteEntity other : nearbyBeckons(stage)) {
            if (!other.getUUID().equals(sourceId) && !other.beckonCounted && other.hasLineOfSight(this)) {
                other.beckonCounted = true;
                count += other.countConnectedBeckons(stage, sourceId);
            }
        }
        return count;
    }

    private void clearBeckonCounted(int stage, UUID sourceId) {
        for (NexusParasiteEntity other : nearbyBeckons(stage)) {
            if (!other.getUUID().equals(sourceId) && other.beckonCounted && other.hasLineOfSight(this)) {
                other.beckonCounted = false;
                other.clearBeckonCounted(stage, sourceId);
            }
        }
    }

    private List<NexusParasiteEntity> nearbyBeckons(int stage) {
        AABB area = new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D, getZ() + 1.0D)
                .inflate(getAttributeValue(Attributes.FOLLOW_RANGE) + 5.0D);
        return level().getEntitiesOfClass(NexusParasiteEntity.class, area,
                other -> other != this && other.isAlive()
                        && other.activeKind().family == Family.BECKON
                        && other.activeKind().stage == stage);
    }

    private void lockNearbyBeckons(Kind activeKind) {
        lockNearbyBeckons(activeKind.stage, getUUID());
    }

    private void lockNearbyBeckons(int stage, UUID sourceId) {
        if (stage == 4) {
            return;
        }
        for (NexusParasiteEntity other : nearbyBeckons(stage)) {
            if (!other.getUUID().equals(sourceId) && !other.beckonLocked && other.hasLineOfSight(this)) {
                other.beckonLocked = true;
                other.beckonAttackTimer = -1;
                other.lockNearbyBeckons(stage, sourceId);
            }
        }
    }

    private void unlockNearbyBeckons(Kind activeKind) {
        unlockNearbyBeckons(activeKind.stage, getUUID(), beckonCooldownTicks(activeKind));
    }

    private void unlockNearbyBeckons(int stage, UUID sourceId, int cooldownTicks) {
        if (stage == 4) {
            return;
        }
        for (NexusParasiteEntity other : nearbyBeckons(stage)) {
            if (!other.getUUID().equals(sourceId) && other.beckonLocked && other.hasLineOfSight(this)) {
                other.beckonLocked = false;
                other.beckonAttackTimer = -cooldownTicks;
                other.unlockNearbyBeckons(stage, sourceId, cooldownTicks);
            }
        }
    }

    private boolean summonBeckonParasites(Kind activeKind, LivingEntity target) {
        return summonBeckonPulse(activeKind, target, false);
    }

    boolean hasBeckonWorldCapacity(ServerLevel serverLevel) {
        int parasiteCount = 0;
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity instanceof Parasite) {
                parasiteCount++;
            }
        }
        return parasiteCount < WorldConfig.beckonSummonLimit(serverLevel);
    }

    void spawnBeckonCapacityBomb(ServerLevel serverLevel, int stage) {
        // Original signed nextInt() threshold is intentionally preserved (~50%, not nextInt(100) 40%).
        if (serverLevel.getRandom().nextInt() < 40) {
            return;
        }
        BombEntity bomb = ModEntities.BOMB.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
        if (bomb == null) {
            return;
        }

        float baseAttackDamage = (float) getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
        bomb.configure(this, 60, 0.0F, baseAttackDamage, 2, 1, false);
        double stageOffset = stage == 3 ? 0.5D : 0.0D;
        bomb.snapTo(getX(), getY() + getEyeHeight() + stageOffset, getZ(), getYRot(), getXRot() + 20.0F);

        double sampleX = (float) getX() + serverLevel.getRandom().nextFloat();
        double sampleY = (float) getY() + getEyeHeight() + serverLevel.getRandom().nextFloat();
        double sampleZ = (float) getZ() + serverLevel.getRandom().nextFloat();
        double dx = sampleX - getX();
        double dy = sampleY - getY();
        double dz = sampleZ - getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= distance;
        dy /= distance;
        dz /= distance;
        double strength = 0.5D / (distance / 4.0D + 0.1D);
        strength *= serverLevel.getRandom().nextFloat() * serverLevel.getRandom().nextFloat() + 1.7F;
        double horizontalScale = stage == 3 ? 5.0D : 3.0D;
        bomb.setBeckonMotion(dx * strength * horizontalScale, dy * strength * 2.0D,
                strength * horizontalScale, 0.4D, 0.5D);
        if (!serverLevel.addFreshEntity(bomb)) {
            return;
        }
        serverLevel.broadcastEntityEvent(bomb, (byte) 0);
        SrpParticleEffects.spawnType10(serverLevel, getX(), getY(), getZ());
    }

    private boolean forceEvolveNearbyParasite() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        List<LivingEntity> candidates = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(16.0D), this::canForceEvolve);
        if (candidates.isEmpty()) {
            return false;
        }

        LivingEntity source = candidates.get(random.nextInt(candidates.size()));
        EntityType<?> targetType = forcedEvolutionType(source);
        Entity created = targetType == null ? null
                : targetType.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
        if (!(created instanceof Mob replacement)) {
            return false;
        }

        replacement.snapTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        replacement.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(replacement.blockPosition()),
                EntitySpawnReason.MOB_SUMMONED, null);
        replacement.setCustomName(source.getCustomName());
        replacement.setCustomNameVisible(source.isCustomNameVisible());
        if (source instanceof Mob sourceMob) {
            replacement.setTarget(sourceMob.getTarget());
        }
        replacement.setHealth(Math.max(1.0F,
                replacement.getMaxHealth() * source.getHealth() / Math.max(1.0F, source.getMaxHealth())));
        serverLevel.addFreshEntity(replacement);

        LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
        if (lightning != null) {
            lightning.snapTo(source.position());
            lightning.setVisualOnly(true);
            serverLevel.addFreshEntity(lightning);
        }
        source.discard();
        return true;
    }

    private boolean canForceEvolve(LivingEntity candidate) {
        if (candidate == this || !candidate.isAlive()) {
            return false;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType());
        if (id.getNamespace().equals(Csrp.MODID) && id.getPath().startsWith("pri_")) {
            Identifier adaptedId = Identifier.fromNamespaceAndPath(Csrp.MODID,
                    "ada_" + id.getPath().substring("pri_".length()));
            return BuiltInRegistries.ENTITY_TYPE.containsKey(adaptedId);
        }
        return isAssimilatedTier(candidate);
    }

    private EntityType<?> forcedEvolutionType(LivingEntity candidate) {
        if (!canForceEvolve(candidate)) {
            return null;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType());
        String path = id.getPath();
        if (id.getNamespace().equals(Csrp.MODID) && path.startsWith("pri_")) {
            Identifier adaptedId = Identifier.fromNamespaceAndPath(Csrp.MODID,
                    "ada_" + path.substring("pri_".length()));
            return BuiltInRegistries.ENTITY_TYPE.containsKey(adaptedId)
                    ? BuiltInRegistries.ENTITY_TYPE.getValue(adaptedId) : null;
        }
        if (!isAssimilatedTier(candidate)) {
            return null;
        }
        return switch (random.nextInt(12)) {
            case 0 -> ModEntities.PRI_LONGARMS.get();
            case 1 -> ModEntities.PRI_SUMMONER.get();
            case 2 -> ModEntities.PRI_VERMIN.get();
            case 3 -> ModEntities.PRI_VISCERA.get();
            case 4 -> ModEntities.PRI_ARACHNIDA.get();
            case 5 -> ModEntities.PRI_BOLSTER.get();
            case 6 -> ModEntities.PRI_BURROWER.get();
            case 7 -> ModEntities.PRI_DEVOURER.get();
            case 8 -> ModEntities.PRI_MANDUCATER.get();
            case 9 -> ModEntities.PRI_REEKER.get();
            case 10 -> ModEntities.PRI_TOZOON.get();
            default -> ModEntities.PRI_YELLOWEYE.get();
        };
    }

    private static boolean isAssimilatedTier(LivingEntity candidate) {
        return candidate instanceof AssimilatedParasiteEntity
                || candidate instanceof AssimilatedVariantEntity
                || candidate instanceof AssimilatedEndermanEntity
                || candidate instanceof AssimilatedHeadEntity
                || candidate instanceof AssimilatedDragonEntity
                || candidate instanceof AssimilatedDragonHeadEntity
                || candidate instanceof SimAdventurerEntity
                || candidate instanceof SimAdventurerHeadEntity
                || candidate instanceof FeralParasiteEntity;
    }

    private boolean summonDispatcherDefenses(Kind activeKind, LivingEntity target) {
        if (nearbyParasiteCount(20.0D) >= activeKind.activeCap) {
            return false;
        }
        if (deployStoredParasite(target)) {
            return true;
        }
        EntityType<? extends Mob> type = random.nextFloat() < (activeKind.stage == 1 ? 0.75F : 0.50F)
                ? ModEntities.SEIZER.get() : ModEntities.SENTRY.get();
        return spawnMob(type, target, 5.0D);
    }

    private boolean deployStoredParasite(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        while (!storedParasiteIds.isEmpty()) {
            int index = random.nextInt(storedParasiteIds.size());
            Identifier id = Identifier.tryParse(storedParasiteIds.get(index));
            if (id == null || BuiltInRegistries.ENTITY_TYPE.getOptional(id).isEmpty()) {
                storedParasiteIds.remove(index);
                continue;
            }
            DeterrentParasiteEntity tentacle = ModEntities.DISPATCHERTEN.get()
                    .create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (tentacle == null) {
                return false;
            }
            for (int attempt = 0; attempt < 6; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = 3.0D + random.nextDouble() * 2.0D;
                BlockPos requested = BlockPos.containing(target.getX() + Math.cos(angle) * distance,
                        target.getY(), target.getZ() + Math.sin(angle) * distance);
                BlockPos spawnPos = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, requested);
                tentacle.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        getYRot(), 0.0F);
                if (!serverLevel.noCollision(tentacle)) {
                    continue;
                }
                tentacle.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                        EntitySpawnReason.MOB_SUMMONED, null);
                takeStoredParasite(index);
                tentacle.setDispatchEntity(id);
                tentacle.setDispatcherStage(activeKind().stage);
                tentacle.setTarget(target);
                if (isOnFire()) {
                    tentacle.igniteForSeconds(8.0F);
                }
                serverLevel.addFreshEntity(tentacle);
                tentacle.setDispatcherOwner(this);
                return true;
            }
            return false;
        }
        return false;
    }

    private void spawnBombVolley(ServerLevel serverLevel, Kind activeKind) {
        int stage = activeKind.stage;
        float damage = activeKind.family == Family.ROOTER ? 2.5F : (float) activeKind.attackDamage;
        float eyeHeight = switch (activeKind.family) {
            case ROOTER -> 1.4F;
            case DISPATCHER -> switch (stage) {
                case 1 -> 2.3F;
                case 2 -> 3.4F;
                case 3 -> 4.6F;
                default -> 2.5F;
            };
            case BECKON -> switch (stage) {
                case 1 -> 1.4F;
                case 2 -> 2.7F;
                case 3 -> 4.9F;
                default -> 8.1F;
            };
            case ROOTERBALL -> 1.8F;
        };
        for (int index = 0; index < stage * 2; index++) {
            double dx = (float) getX() + serverLevel.getRandom().nextFloat() - getX();
            double dy = (float) getY() + serverLevel.getRandom().nextFloat() - getY();
            double dz = (float) getZ() + serverLevel.getRandom().nextFloat() - getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance == 0.0D) {
                distance = 1.0D;
            }
            double strength = 0.5D / (distance / 4.0D + 0.1D);
            strength *= serverLevel.getRandom().nextFloat() * serverLevel.getRandom().nextFloat() + 0.65F;
            BombEntity bomb = ModEntities.BOMB.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (bomb == null) {
                continue;
            }
            bomb.configure(this, 60, 0.0F, damage, stage, 1, false);
            bomb.snapTo(getX(), getY() + eyeHeight - 0.1F, getZ(), getYRot(), getXRot() + 20.0F);
            bomb.setBeckonMotion(dx / distance * strength, dy / distance * strength * stage * 3.0D,
                    dz / distance * strength, 0.35D, stage / 10.0F * 2.0F);
            if (serverLevel.addFreshEntity(bomb)) {
                serverLevel.broadcastEntityEvent(bomb, (byte) 0);
            }
        }
    }

    /** EntityAIAncientSummon on the stage-IV Dispatcher, independent of its defense summoning. */
    private final class DispatcherPodGoal extends Goal {
        private int attackTimer;
        private int attackingTicks;
        private int spawnedPods;
        private double targetX;
        private double targetY;
        private double targetZ;

        @Override
        public boolean canUse() {
            return getTarget() != null || attackingTicks >= 1;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (attackingTicks >= 1) {
                attackingTicks++;
                if (attackingTicks == 2) {
                    playSound(ModSounds.get("ancient.pod"), 5.0F, 1.0F);
                }
                if (attackingTicks >= 40 && attackingTicks % 20 == 0 && spawnDispatcherPod()) {
                    spawnedPods++;
                }
                if (spawnedPods >= 4) {
                    attackingTicks = 0;
                    attackTimer = 0;
                    spawnedPods = 0;
                }
                return;
            }

            LivingEntity target = getTarget();
            if (target == null || target.isRemoved()) {
                attackTimer = 0;
                return;
            }
            if (hasLineOfSight(target) && distanceToSqr(target) < 2500.0D) {
                if (target.onGround() || random.nextBoolean()) {
                    attackTimer++;
                }
                if (attackTimer >= 120) {
                    attackingTicks = 1;
                    targetX = target.getX();
                    targetY = getY() + 25.0D;
                    targetZ = target.getZ();
                }
            } else {
                attackTimer = 0;
            }
        }

        @Override
        public void stop() {
            attackTimer = 0;
            spawnedPods = 0;
        }

        private boolean spawnDispatcherPod() {
            if (!(level() instanceof ServerLevel serverLevel)) {
                return false;
            }
            AncientPodEntity pod = ModEntities.ANC_POD.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (pod == null) {
                return false;
            }
            int xOffset = random.nextInt(10);
            int zOffset = random.nextInt(10);
            if (random.nextBoolean()) xOffset = -xOffset;
            if (random.nextBoolean()) zOffset = -zOffset;
            pod.snapTo(targetX + xOffset, targetY, targetZ + zOffset, getYRot(), getXRot());
            pod.setOwner((byte) 0);
            serverLevel.addFreshEntity(pod);
            return true;
        }
    }

    private AABB rooterArea(double horizontalRange, double verticalRange) {
        return new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D, getZ() + 1.0D)
                .inflate(horizontalRange, verticalRange, horizontalRange);
    }

    private List<NexusParasiteEntity> rootmassCystsInRange(Kind rooter) {
        int range = MobsConfig.rooterCystRange(rooter.stage);
        return level().getEntitiesOfClass(NexusParasiteEntity.class,
                rooterArea(range + 1.0D, range),
                cyst -> cyst.activeKind().isRooterBall());
    }

    private void spawnRootmassCysts(int maximum) {
        if (!(level() instanceof ServerLevel serverLevel) || maximum <= 0) {
            return;
        }
        // EntityPRooter never calls checkID; occupied slots reset when the Rooter reloads.
        int count = 1 + random.nextInt(maximum);
        for (int index = 0; index < count && getUsedSummonCapacity() < getSummonCapacity(); index++) {
            for (int attempt = 0; attempt < 2; attempt++) {
                int offsetX = 2 + random.nextInt(4);
                int offsetZ = 2 + random.nextInt(4);
                if (random.nextBoolean()) offsetX = -offsetX;
                if (random.nextBoolean()) offsetZ = -offsetZ;
                double x = getX() + offsetX;
                double z = getZ() + offsetZ;
                BlockPos position = BlockPos.containing(x, getY(), z);
                if (!serverLevel.getBlockState(position).isAir()
                        || serverLevel.getBlockState(position.below()).isAir()) {
                    continue;
                }
                NexusParasiteEntity cyst = ModEntities.ROOTERBALL.get()
                        .create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
                if (cyst == null) {
                    break;
                }
                cyst.snapTo(x, getY(), z, getYRot(), getXRot());
                if (!serverLevel.noCollision(cyst)
                        || !serverLevel.getEntities(cyst, cyst.getBoundingBox().inflate(0.25D)).isEmpty()) {
                    cyst.discard();
                    continue;
                }
                cyst.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(position),
                        EntitySpawnReason.MOB_SUMMONED, null);
                reserveTrackedSummon(cyst.getUUID(), 1);
                serverLevel.addFreshEntity(cyst);
                break;
            }
        }
    }

    private void tickRooterCycle(Kind activeKind) {
        if (++rooterCycleTicks > 20) {
            rooterCycleTicks = 0;
        }
        if (rooterCycleTicks == 5) {
            if (activeKind.isRooterBall()) {
                removeEffect(ModMobEffects.PIVOT);
                return;
            }
            supportCooldown--;
            if (++rooterGroundChecks >= 7) {
                rooterGroundChecks = 0;
                checkRooterGround(activeKind.stage);
            }
        }
        if (!activeKind.isRooterBall() && rooterCycleTicks == 10 && supportCooldown <= 0) {
            supportCooldown = MobsConfig.rooterSupportCooldown(activeKind.stage);
            applyRooterSupport(activeKind.stage);
        }
    }

    private void checkRooterGround(int stage) {
        int range = switch (stage) {
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 5;
            default -> 0;
        };
        if (range == 0) {
            return;
        }
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                BlockPos pos = BlockPos.containing(getX() + x, getY() - 0.5D, getZ() + z);
                if (level().getBlockState(pos).isAir()) {
                    setRTTS(false);
                    return;
                }
            }
        }
        setRTTS(true);
    }

    private void applyRooterSupport(int stage) {
        int range = MobsConfig.rooterEffectRange(stage);
        for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class,
                rooterArea(range, range),
                entity -> entity != this && entity.isAlive() && entity instanceof Parasite)) {
            if (ally instanceof NexusParasiteEntity nexus
                    && (nexus.activeKind().family == Family.ROOTER || nexus.activeKind().isRooterBall())) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(ModMobEffects.PIVOT, 300, stage - 1, false, false), this);
            ally.addEffect(new MobEffectInstance(ModMobEffects.PARATE, 300, stage - 1, false, false), this);
            StatusEffectEvents.linkToRooter(ally, this);
            EvolutionSystem.applyRooterGenes(ally);
        }
    }

    /**
     * Port of {@code entity/logic/VenkrolTornadoLogic.tickTornadoEffects} (out109
     * {@code entity/logic/VenkrolTornadoLogic.java:15-171}), driven from a stage-4 Beckon.
     *
     * <p>Every non-parasite living entity inside a 120-block radius / 50-block tall box that is not
     * below the Venkrol is pulled in, swirled and, in the inner lift zone, thrown upwards. Constants,
     * tier boundaries and clamps are copied from the original.
     */
    private void createStormVortex() {
        final double maxRadius = 120.0D;
        AABB area = new AABB(getX() - maxRadius, getY(), getZ() - maxRadius,
                getX() + maxRadius, getY() + 50.0D, getZ() + maxRadius);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == this || !target.isAlive() || isSrpParasite(target)
                    || target.getY() < getY()) {
                continue;
            }
            applyTornadoForces(target, maxRadius);
        }
    }

    /** Original {@code isSRPParasite}: anything registered under the mod namespace is immune. */
    private static boolean isSrpParasite(LivingEntity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return Csrp.MODID.equals(id.getNamespace());
    }

    private void applyTornadoForces(LivingEntity target, double maxRadius) {
        if (target instanceof Player player) {
            if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.VENKROL_BOOTS)
                    || player.isSpectator()
                    || player.isCreative() && player.getAbilities().flying) {
                return;
            }
        }
        if (target.isPassenger() || target.isRemoved()) {
            return;
        }
        double dx = getX() - target.getX();
        double dz = getZ() - target.getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq < 1.0E-4D) {
            distSq = 1.0E-4D;
        }
        double horizDist = Math.sqrt(distSq);
        if (horizDist > maxRadius) {
            return;
        }
        double normX = dx / horizDist;
        double normZ = dz / horizDist;

        double pullTierFactor;
        if (horizDist >= 50.0D) {
            pullTierFactor = 0.05D;
        } else if (horizDist >= 25.0D) {
            pullTierFactor = 0.1D;
        } else if (horizDist >= 15.0D) {
            pullTierFactor = 0.2D;
        } else if (horizDist >= 10.0D) {
            pullTierFactor = 0.35D;
        } else if (horizDist >= 5.0D) {
            pullTierFactor = 0.55D;
        } else {
            pullTierFactor = 1.0D;
        }

        double pullStrength = 0.08D * pullTierFactor;
        double swirlStrength = 0.07D * pullTierFactor;
        double swirlX = -normZ;
        double swirlZ = normX;

        double innerLiftRadius = 15.0D;
        double liftAccel = 0.0D;
        if (horizDist <= innerLiftRadius) {
            double liftFactor = Mth.clamp(1.0D - horizDist / innerLiftRadius, 0.0D, 1.0D);
            liftAccel = 0.25D * liftFactor;
        }

        double heightAboveVenkrol = target.getY() - getY();
        boolean inFlingZone = heightAboveVenkrol > 16.0D && horizDist < 12.0D;
        if (heightAboveVenkrol > 18.0D && horizDist > 18.0D) {
            return;
        }

        double radialDirX = normX;
        double radialDirZ = normZ;
        double motionX = target.getDeltaMovement().x;
        double motionY = target.getDeltaMovement().y;
        double motionZ = target.getDeltaMovement().z;

        if (inFlingZone) {
            radialDirX = -normX;
            radialDirZ = -normZ;
            double flingFactor = 1.0D - Math.min(horizDist / 12.0D, 1.0D);
            pullStrength *= 1.4D + (6.0D - 1.4D) * flingFactor;
            swirlStrength *= 1.0D + (2.3D - 1.0D) * flingFactor;
            double outwardBurst = 1.1D * flingFactor;
            motionX += radialDirX * outwardBurst;
            motionZ += radialDirZ * outwardBurst;
            motionY = Math.max(-1.6D, motionY - 0.16D * flingFactor);
        } else if (liftAccel > 0.0D && heightAboveVenkrol < 25.0D) {
            motionY = Math.min(1.2D, motionY + liftAccel);
            target.resetFallDistance();
        }

        double awayX = -normX;
        double awayZ = -normZ;
        double dotAway = motionX * awayX + motionZ * awayZ;
        pullStrength *= dotAway > 0.0D ? 1.0D : 0.25D;

        target.setDeltaMovement(motionX + radialDirX * pullStrength + swirlX * swirlStrength,
                motionY,
                motionZ + radialDirZ * pullStrength + swirlZ * swirlStrength);
        target.syncVelocity = true;
    }

    /** Port of EntityAINexusGrow.spawnLeem and its ParasiteSummon.SummonM call. */
    private void trySpawnRooterSi(ServerLevel serverLevel) {
        trySpawnRooterSi(serverLevel, serverLevel.getRandom(), new Random());
    }

    private boolean trySpawnRooterSi(ServerLevel serverLevel, RandomSource roll, Random summonRandom) {
        if (Config.evolutionNests() < creationEvolutionPhase
                && Config.deveNestsUse() < creationDevelopmentLevel) {
            return false;
        }
        if (roll.nextDouble() >= NEXUS_ROOTER_CHANCE) {
            return false;
        }
        Kind activeKind = activeKind();
        if ((activeKind.stage != 2 && activeKind.stage != 3)
                || (activeKind.family != Family.BECKON && activeKind.family != Family.DISPATCHER)) {
            return false;
        }
        int rooterCount = 0;
        double minimumDistance = Config.nexusRooterDistance();
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity instanceof NexusParasiteEntity other && other.activeKind().family == Family.ROOTER
                    && (++rooterCount > Config.nexusRooterCap()
                    || distanceToSqr(other) < minimumDistance * minimumDistance)) {
                return false;
            }
        }
        if (!summonRooterSi(serverLevel, summonRandom)) {
            return false;
        }
        if (Config.reinforcementSounds()
                && (!Config.disloGrowlNoise()
                || DislodgmentSystem.activeValue(SrpWorldData.get(serverLevel), 15) == 0)) {
            playSound(ModSounds.get("leem.si"), 4.0F, 1.0F);
        }
        return true;
    }

    private boolean summonRooterSi(ServerLevel serverLevel, Random summonRandom) {
        // SummonM initializes both offsets and a list index before entering its two passes.
        summonRandom.nextInt(9);
        summonRandom.nextInt(9);
        summonRandom.nextInt(1);
        for (int attempt = 0; attempt < 2; attempt++) {
            summonRandom.nextDouble(); // The single configured entry has chance 1.0.
            int offsetX = 12 + summonRandom.nextInt(9);
            int offsetZ = 12 + summonRandom.nextInt(9);
            if (summonRandom.nextBoolean()) offsetX = -offsetX;
            if (summonRandom.nextBoolean()) offsetZ = -offsetZ;
            double x = getX() + offsetX;
            double y = getY();
            double z = getZ() + offsetZ;
            BlockPos position = BlockPos.containing(x, y, z);
            if (!serverLevel.getBlockState(position).is(Blocks.AIR)
                    || serverLevel.getBlockState(BlockPos.containing(x, y - 1.0D, z)).is(Blocks.AIR)) {
                continue;
            }
            NexusParasiteEntity rooter = createNexus(serverLevel, Family.ROOTER, 1);
            if (rooter == null) {
                return false;
            }
            rooter.snapTo(x, y, z, getYRot(), getXRot());
            if (!serverLevel.noCollision(rooter)
                    || !serverLevel.getEntities(rooter, rooter.getBoundingBox().inflate(0.25D), Entity::isAlive).isEmpty()) {
                rooter.discard();
                continue;
            }
            rooter.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(position),
                    EntitySpawnReason.MOB_SUMMONED, null);
            if (getSummonCapacity() - getUsedSummonCapacity() < 0
                    || Config.reinforcementSky() && !hasDirectSky(serverLevel,
                            BlockPos.containing(x, y + rooter.getEyeHeight(), z))) {
                rooter.discard();
                continue;
            }
            serverLevel.addFreshEntity(rooter);
            return true;
        }
        return false;
    }

    private static boolean hasDirectSky(ServerLevel level, BlockPos position) {
        int lowestSky = level.getChunkAt(position).getSkyLightSources().getLowestSourceY(
                position.getX() & 15, position.getZ() & 15);
        return position.getY() >= lowestSky;
    }

    private void breakBlocksTowardsTarget(Kind activeKind) {
        LivingEntity target = getTarget();
        if (target == null || blockBreakCooldown > 0
                || !(level() instanceof ServerLevel serverLevel)
                || !serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
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
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || hardness < 0.0F || hardness > activeKind.maxBlockHardness) {
                continue;
            }
            if (ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this)) {
                blockBreakCooldown = activeKind.stage >= 4 ? 20 : 60;
            }
            return;
        }
    }

    private boolean evolve() {
        Kind activeKind = activeKind();
        // Original EntityAINexusGrow required ParasiteEventEntity.canSpawnNext before every stage upgrade.
        if (!activeKind.supportsOrdinaryGrowth() || !RuntimeToggles.mobEvolution()
                || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!stageFourUnlocked(serverLevel, activeKind)) {
            return false;
        }
        NexusParasiteEntity next = createNexus(serverLevel, activeKind.family, activeKind.stage + 1);
        if (next == null) {
            return false;
        }
        boolean burning = isOnFire();
        discard();
        next.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
        next.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(next.blockPosition()),
                EntitySpawnReason.MOB_SUMMONED, null);
        if (getCustomName() != null) {
            next.setCustomName(getCustomName());
            next.setCustomNameVisible(isCustomNameVisible());
        }
        if (isPersistenceRequired()) {
            next.setPersistenceRequired();
        }
        serverLevel.addFreshEntity(next);
        copyDamageAdaptationsTo(next);
        serverLevel.broadcastEntityEvent(next, NEXUS_GROWTH_PARTICLES_EVENT);
        if (burning) {
            next.setHealth(next.getMaxHealth() * 0.5F);
            next.igniteForSeconds(8.0F);
        }
        if (Config.reinforcementSounds()
                && (!Config.disloWalkNoise() || DislodgmentSystem.activeValue(SrpWorldData.get(serverLevel), 16) < 1)) {
            String familySound = switch (activeKind.family) {
                case BECKON -> "venkrol.";
                case DISPATCHER -> "dod.";
                case ROOTER -> "leem.";
                case ROOTERBALL -> throw new IllegalStateException("Rootmass Cysts do not grow");
            };
            String stageSound = switch (activeKind.stage) {
                case 1 -> "sii";
                case 2 -> "siii";
                case 3 -> "siv";
                default -> throw new IllegalStateException("Invalid Nexus growth stage");
            };
            playSound(ModSounds.get(familySound + stageSound), activeKind.stage == 2 ? 9.0F : 10.0F, 1.0F);
            if (shouldGenerateProtectionStructure(activeKind, serverLevel.getRandom())) {
                generateProtectionStructure(serverLevel, activeKind);
            }
        }
        return true;
    }

    private boolean stageFourUnlocked(ServerLevel level, Kind activeKind) {
        if (activeKind.stage != 3) {
            return true;
        }
        if (ParasiteBiomeDecorator.parasiteBiomeIndex(level.getBiome(blockPosition())) != 0) {
            return false;
        }
        if (Config.useEvolutionPhases()) {
            int phase = SrpWorldData.get(level).evolutionPhase();
            int development = EvolutionSystem.ubiquitousDevelopment(level.getServer());
            int phaseRequired = switch (activeKind.family) {
                case BECKON -> Config.evolutionNodeUnlock();
                case DISPATCHER -> Config.evolutionColonyUnlock();
                case ROOTER -> Config.evolutionHives();
                case ROOTERBALL -> throw new IllegalStateException("Rootmass Cysts do not grow");
            };
            int developmentRequired = switch (activeKind.family) {
                case BECKON -> Config.deveNodesUse();
                case DISPATCHER -> Config.deveColoniesUse();
                case ROOTER -> Config.deveHivesUse();
                case ROOTERBALL -> throw new IllegalStateException("Rootmass Cysts do not grow");
            };
            if (phase < phaseRequired && development < developmentRequired) {
                return false;
            }
        }
        SrpWorldData data = SrpWorldData.get(level);
        return switch (activeKind.family) {
            case BECKON -> !WorldConfig.nodesActivated() || !WorldConfig.biomeRegistered()
                    || data.nearestHeartAge(blockPosition()) < 1;
            case DISPATCHER -> data.nearestColonyInEffectRange(blockPosition()) == null;
            case ROOTER -> true;
            case ROOTERBALL -> false;
        };
    }

    private boolean shouldGenerateProtectionStructure(Kind activeKind, RandomSource worldRandom) {
        if (activeKind.stage != 1 || !Config.nexusStructures()) {
            return false;
        }
        double chance = switch (activeKind.family) {
            case BECKON -> 0.5D;
            case DISPATCHER -> 0.3D;
            case ROOTER -> 0.3D;
            case ROOTERBALL -> 0.0D;
        };
        return worldRandom.nextDouble() < chance;
    }

    /** Family-specific protection structures, all built with stage 1. */
    private void generateProtectionStructure(ServerLevel serverLevel, Kind activeKind) {
        BlockPos origin = activeKind.family == Family.DISPATCHER ? blockPosition().below() : blockPosition();
        switch (activeKind.family) {
            case DISPATCHER -> new WorldGenParasiteNexusProtection1(1).generate(serverLevel, serverLevel.getRandom(), origin);
            case BECKON -> new WorldGenParasiteNexusProtection2(1).generate(serverLevel, serverLevel.getRandom(), origin);
            case ROOTER -> new WorldGenParasiteNexusProtection3(1).generate(serverLevel, serverLevel.getRandom(), origin);
            default -> {
            }
        }
    }

    private boolean spawnNexus(NexusParasiteEntity spawned, LivingEntity target, double distance) {
        if (spawned == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        boolean temporaryBeckon = activeKind().family == Family.BECKON
                && spawned.activeKind().family == Family.BECKON;
        if (temporaryBeckon) {
            spawned.makeTemporaryBeckon();
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        spawned.snapTo(target.getX() + Math.cos(angle) * distance, target.getY(),
                target.getZ() + Math.sin(angle) * distance, getYRot(), 0.0F);
        spawned.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawned.blockPosition()),
                EntitySpawnReason.MOB_SUMMONED, null);
        spawned.setTarget(target);
        if (!serverLevel.addFreshEntity(spawned)) {
            return false;
        }
        if (temporaryBeckon) {
            serverLevel.broadcastEntityEvent(spawned, BECKON_BURIED_EVENT);
        }
        return true;
    }

    private void makeTemporaryBeckon() {
        temporaryLifetimeTicks = TEMPORARY_BECKON_LIFETIME;
        canGrow = false;
    }

    private boolean spawnMob(EntityType<? extends Mob> type, LivingEntity target, double distance) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Mob spawned = type.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
        if (spawned == null) {
            return false;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        spawned.snapTo(target.getX() + Math.cos(angle) * distance, target.getY(),
                target.getZ() + Math.sin(angle) * distance, getYRot(), 0.0F);
        spawned.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawned.blockPosition()),
                EntitySpawnReason.MOB_SUMMONED, null);
        spawned.setTarget(target);
        serverLevel.addFreshEntity(spawned);
        return true;
    }

    private int nearbyParasiteCount(double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                entity -> entity != this && entity instanceof Parasite).size();
    }

    private int defaultGrowthDelay() {
        return MobsConfig.nexusGrowthDelayTicks(activeKind(), random);
    }

    public int getGrowthDelayTicks() {
        return growthDelayTicks;
    }

    private NexusParasiteEntity createNexus(ServerLevel level, Family family, int stage) {
        return switch (family) {
            case BECKON -> switch (stage) {
                case 1 -> ModEntities.BECKON_SI.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                case 2 -> ModEntities.BECKON_SII.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                case 3 -> ModEntities.BECKON_SIII.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                case 4 -> ModEntities.BECKON_SIV.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                default -> null;
            };
            case DISPATCHER -> switch (stage) {
                case 1 -> ModEntities.DISPATCHER_SI.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                case 2 -> ModEntities.DISPATCHER_SII.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                case 3 -> ModEntities.DISPATCHER_SIII.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                case 4 -> ModEntities.DISPATCHER_SIV.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                default -> null;
            };
            case ROOTER -> switch (stage) {
                case 1 -> ModEntities.ROOTER_SI.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                case 2 -> ModEntities.ROOTER_SII.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                case 3 -> ModEntities.ROOTER_SIII.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                case 4 -> ModEntities.ROOTER_SIV.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                default -> null;
            };
            case ROOTERBALL -> null;
        };
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        EntityType<?> type = getType();
        if (type == ModEntities.BECKON_SII.get()) return Kind.BECKON_SII;
        if (type == ModEntities.BECKON_SIII.get()) return Kind.BECKON_SIII;
        if (type == ModEntities.BECKON_SIV.get()) return Kind.BECKON_SIV;
        if (type == ModEntities.DISPATCHER_SI.get()) return Kind.DISPATCHER_SI;
        if (type == ModEntities.DISPATCHER_SII.get()) return Kind.DISPATCHER_SII;
        if (type == ModEntities.DISPATCHER_SIII.get()) return Kind.DISPATCHER_SIII;
        if (type == ModEntities.DISPATCHER_SIV.get()) return Kind.DISPATCHER_SIV;
        if (type == ModEntities.ROOTER_SI.get()) return Kind.ROOTER_SI;
        if (type == ModEntities.ROOTER_SII.get()) return Kind.ROOTER_SII;
        if (type == ModEntities.ROOTER_SIII.get()) return Kind.ROOTER_SIII;
        if (type == ModEntities.ROOTER_SIV.get()) return Kind.ROOTER_SIV;
        if (type == ModEntities.ROOTERBALL.get()) return Kind.ROOTERBALL;
        return Kind.BECKON_SI;
    }

    private enum Family {
        BECKON,
        DISPATCHER,
        ROOTER,
        ROOTERBALL
    }

    public enum Kind {
        // Summoning pillars cast every 2–3 seconds in the original game.
        // Use a four-times faster cadence while retaining stage progression.
        BECKON_SI(Family.BECKON, 1, 25.0D, 4.0D, 2.5D, 4, 50, 1.0F, 3.0D, 16, 16.0D),
        BECKON_SII(Family.BECKON, 2, 60.0D, 8.0D, 36.0D, 6, 45, 1.0F, 3.0D, 32, 32.0D),
        BECKON_SIII(Family.BECKON, 3, 110.0D, 16.0D, 13.0D, 8, 40, 1.0F, 3.0D, 64, 48.0D),
        BECKON_SIV(Family.BECKON, 4, 220.0D, 25.0D, 20.0D, 12, 40, 5.0F, 18.0D, 220, 128.0D),
        DISPATCHER_SI(Family.DISPATCHER, 1, 33.0D, 7.0D, 3.0D, 3, 60, 1.0F, 3.0D, 16, 32.0D),
        DISPATCHER_SII(Family.DISPATCHER, 2, 70.0D, 14.0D, 7.0D, 5, 55, 2.0F, 6.0D, 32, 64.0D),
        DISPATCHER_SIII(Family.DISPATCHER, 3, 130.0D, 21.0D, 14.0D, 7, 50, 3.0F, 9.0D, 64, 96.0D),
        DISPATCHER_SIV(Family.DISPATCHER, 4, 250.0D, 28.0D, 22.0D, 9, 45, 5.0F, 18.0D, 220, 256.0D),
        ROOTER_SI(Family.ROOTER, 1, 25.0D, 4.0D, 2.5D, 3, 60, 0.0F, 0.0D, 16, 16.0D),
        ROOTER_SII(Family.ROOTER, 2, 60.0D, 8.0D, 36.0D, 5, 55, 2.0F, 6.0D, 32, 32.0D),
        ROOTER_SIII(Family.ROOTER, 3, 110.0D, 16.0D, 13.0D, 7, 50, 3.0F, 9.0D, 64, 48.0D),
        ROOTER_SIV(Family.ROOTER, 4, 220.0D, 25.0D, 20.0D, 9, 45, 5.0F, 18.0D, 220, 128.0D),
        ROOTERBALL(Family.ROOTERBALL, 0, 20.0D, 10.0D, 0.0D, 0, 0, 0.0F, 0.0D, 0, 16.0D);

        private final Family family;
        private final int stage;
        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final int activeCap;
        private final int summonCooldown;
        private final float maxBlockHardness;
        private final double blockRange;
        private final int experience;
        private final double defaultFollowRange;

        Kind(Family family, int stage, double maxHealth, double armor, double attackDamage,
             int activeCap, int summonCooldown, float maxBlockHardness, double blockRange, int experience,
             double defaultFollowRange) {
            this.family = family;
            this.stage = stage;
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.activeCap = activeCap;
            this.summonCooldown = summonCooldown;
            this.maxBlockHardness = maxBlockHardness;
            this.blockRange = blockRange;
            this.experience = experience;
            this.defaultFollowRange = defaultFollowRange;
        }

        private boolean supportsOrdinaryGrowth() {
            return stage > 0 && stage < 4 && family != Family.ROOTERBALL;
        }

        private boolean isRooterBall() {
            return family == Family.ROOTERBALL;
        }

        public int stage() {
            return stage;
        }
    }
}
