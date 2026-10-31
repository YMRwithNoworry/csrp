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
import alku.csrp.world.EvolutionSystem;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
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
    private static final int TEMPORARY_BECKON_LIFETIME = 300;
    private static final int BECKON_PULSE_INTERVAL = 20;
    private static final int BECKON_LOCK_RELEASE_TICKS = 200;
    private static final int BECKON_SIV_INITIAL_RANGE = 12;
    private static final int BECKON_SIV_EXTRA_RANGE = 5;

    /** Original SRPConfig.nexusLeemCap. */
    private static final int NEXUS_ROOTER_CAP = 5;
    /** Original SRPConfig.nexusLeemDis. */
    private static final double NEXUS_ROOTER_DISTANCE = 32.0D;
    /** Original EntityAINexusGrow.spawnLeem 2% roll. */
    private static final double NEXUS_ROOTER_CHANCE = 0.02D;
    /** Original EntityAINexusGrow.tickss reset of 20. */
    private static final int NEXUS_ROOTER_INTERVAL = 20;
    private static final int DISPATCHER_FOG_MIN_Y_OFFSET = -2;
    private static final int DISPATCHER_FOG_MAX_Y_OFFSET = 4;

    private final Kind kind;
    private final List<String> storedParasiteIds = new ArrayList<>();
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
    private boolean canGrow = true;
    private boolean dispatcherFogDissipationStarted;
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
        if (canGrow && activeKind.supportsOrdinaryGrowth() && growthDelayTicks > 0
                && level() instanceof ServerLevel serverLevel) {
            int phase = SrpWorldData.get(serverLevel).evolutionPhase();
            int minimumPhase = activeKind.stage + 2;
            if (phase >= minimumPhase && (phase > minimumPhase || tickCount % 4 == 0)
                    && ++growthTicks >= growthDelayTicks && evolve()) {
                return;
            }
        }
        // Original EntityAINexusGrow.spawnLeem ran from shouldExecute every 20 ticks.
        if ((activeKind.family == Family.BECKON || activeKind.family == Family.DISPATCHER)
                && (activeKind.stage == 2 || activeKind.stage == 3)
                && level() instanceof ServerLevel rooterLevel && --rooterSummonCooldown <= 0) {
            rooterSummonCooldown = NEXUS_ROOTER_INTERVAL;
            trySpawnRooterSi(rooterLevel);
        }

        if (activeKind.family == Family.DISPATCHER && tickCount % 40 == 0) {
            storeNearbyParasite();
            placeNestFog(activeKind.stage);
        }
        if (activeKind.family == Family.DISPATCHER && activeKind.stage == 4) {
            tryPlaceFirstColony();
        }
        if (activeKind.family == Family.BECKON) {
            tickBeckonSummoning(activeKind);
        } else if (summonCooldown <= 0 && performFamilyAbility(activeKind)) {
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
        boolean hurt = super.hurtServer(level, source,
                source.is(DamageTypeTags.IS_FIRE) ? incomingDamage * 4.0F : incomingDamage);
        if (!hurt || level.isClientSide() || activeKind.isRooterBall()) {
            return hurt;
        }
        if (bombCooldown <= 0) {
            spawnBombVolley(activeKind.bombCount, (float) activeKind.attackDamage);
            bombCooldown = 100;
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
        tag.putInt("nexus_bomb_cooldown", bombCooldown);
        tag.putInt("nexus_support_cooldown", supportCooldown);
        tag.putInt("nexus_rooter_cycle", rooterCycleTicks);
        tag.putInt("nexus_rooter_ground_checks", rooterGroundChecks);
        tag.putBoolean("nexus_rooter_rtts", getRTTS());
        tag.putInt("nexus_block_break_cooldown", blockBreakCooldown);
        tag.putInt("nexus_forced_evolution_cooldown", forcedEvolutionCooldown);
        tag.putInt("nexus_temporary_lifetime", temporaryLifetimeTicks);
        tag.putBoolean("nexus_can_grow", canGrow);
        tag.putFloat("nexus_body", getBODY());
        tag.putInt("nexus_parasite_status", getParasiteStatus());
        tag.putFloat("nexus_floor_timer", getFloorTimer());
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
        summonCooldown = tag.getIntOr("nexus_summon_cooldown", 0);
        bombCooldown = tag.getIntOr("nexus_bomb_cooldown", 0);
        supportCooldown = tag.getIntOr("nexus_support_cooldown", 0);
        rooterCycleTicks = Mth.clamp(tag.getIntOr("nexus_rooter_cycle", 0), 0, 20);
        rooterGroundChecks = Mth.clamp(tag.getIntOr("nexus_rooter_ground_checks", 0), 0, 6);
        setRTTS(tag.getBooleanOr("nexus_rooter_rtts", false));
        blockBreakCooldown = tag.getIntOr("nexus_block_break_cooldown", 0);
        forcedEvolutionCooldown = tag.getIntOr("nexus_forced_evolution_cooldown", 0);
        temporaryLifetimeTicks = tag.getInt("nexus_temporary_lifetime").orElse(-1);
        canGrow = tag.getBooleanOr("nexus_can_grow", true);
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

    /** 原版 storeBefDes：寄生体超距消失时回收入库，调度柱之后可再部署。 */
    public boolean recallParasite(String entityTypeId) {
        if (!isDispatcherFamily() || storedParasiteIds.size() >= 12) {
            return false;
        }
        return storedParasiteIds.add(entityTypeId);
    }

    private void storeNearbyParasite() {
        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        List<Mob> candidates = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(range),
                this::isStorableDispatcherParasite);
        if (candidates.isEmpty()) {
            return;
        }
        Mob candidate = candidates.get(random.nextInt(candidates.size()));
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType());
        if (id == null || !Csrp.MODID.equals(id.getNamespace())) {
            return;
        }
        storedParasiteIds.add(id.toString());
        candidate.discard();
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, candidate.getX(),
                    candidate.getY() + candidate.getBbHeight() * 0.5D, candidate.getZ(),
                    12, 0.25D, 0.35D, 0.25D, 0.02D);
        }
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
                && candidate.getTarget() == null
                && candidate.onGround()
                && candidate.tickCount > 100
                && !candidate.isPassenger()
                && !candidate.isVehicle()
                && !(candidate instanceof NexusParasiteEntity)
                && !(candidate instanceof PreeminentParasiteEntity)
                && !(candidate instanceof AncientParasiteEntity)
                && !(candidate instanceof AbominationEntity)
                && !(candidate instanceof ArchitectEntity)
                && !(candidate instanceof DeterrentParasiteEntity)
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
                    Mob spawned = option.type().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
                    if (spawned == null) {
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
        boolean spawned = spawnMob(type, target, 5.0D);
        if (spawned && activeKind.stage == 4 && random.nextFloat() < 0.30F) {
            spawnPodVolley(target);
        }
        return spawned;
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
                tentacle.setDispatchEntity(id);
                tentacle.setTarget(target);
                serverLevel.addFreshEntity(tentacle);
                storedParasiteIds.remove(index);
                return true;
            }
            return false;
        }
        return false;
    }

    private void spawnBombVolley(int count, float damage) {
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2.0D * index / count + random.nextDouble() * 0.45D;
            Vec3 start = position().add(0.0D, getBbHeight() * 0.65D, 0.0D);
            Vec3 target = start.add(Math.cos(angle) * (4.0D + random.nextDouble() * 3.0D),
                    random.nextDouble() * 1.5D, Math.sin(angle) * (4.0D + random.nextDouble() * 3.0D));
            fireProjectile(ParasiteProjectileEntity.Mode.BOMB, start, target, 0.72D, damage, 1.25D, 90);
        }
    }

    private void spawnPodVolley(LivingEntity target) {
        for (int index = 0; index < 5; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            Vec3 landing = target.position().add(Math.cos(angle) * (2.0D + random.nextDouble() * 3.0D),
                    0.0D, Math.sin(angle) * (2.0D + random.nextDouble() * 3.0D));
            Vec3 start = landing.add(0.0D, 8.0D + random.nextDouble() * 3.0D, 0.0D);
            fireProjectile(ParasiteProjectileEntity.Mode.BOMB, start, landing, 0.85D, 50.0F, 2.5D, 100);
        }
    }

    private void fireProjectile(ParasiteProjectileEntity.Mode mode, Vec3 start, Vec3 target,
                                double speed, float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get()
                .create(level(), EntitySpawnReason.MOB_SUMMONED);
        if (projectile == null) {
            return;
        }
        projectile.configure(this, mode, start, target, speed, damage, radius, lifetime);
        level().addFreshEntity(projectile);
    }

    private AABB rooterArea(double horizontalRange, double verticalRange) {
        return new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D, getZ() + 1.0D)
                .inflate(horizontalRange, verticalRange, horizontalRange);
    }

    private List<NexusParasiteEntity> rootmassCystsInRange(Kind rooter) {
        int range = MobsConfig.rooterCystRange(rooter.stage);
        return level().getEntitiesOfClass(NexusParasiteEntity.class,
                rooterArea(range + 1.0D, range),
                cyst -> cyst.isAlive() && cyst.activeKind().isRooterBall());
    }

    private void spawnRootmassCysts(int maximum) {
        if (!(level() instanceof ServerLevel serverLevel) || maximum <= 0) {
            return;
        }
        summonTracker.prune(serverLevel);
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
                if (serverLevel.addFreshEntity(cyst)) {
                    reserveTrackedSummon(cyst.getUUID(), 1);
                }
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

    /**
     * Port of {@code EntityAINexusGrow.spawnLeem()} (out109 {@code entity/ai/EntityAINexusGrow.java:69-98}).
     *
     * <p>While a Dispatcher or Beckon sits at stage 2 or 3 it has a 2% chance every 20 ticks to call a
     * {@code rooter_si}. The original refuses when more than {@code SRPConfig.nexusLeemCap} Rooters
     * already exist or when the nearest one is closer than {@code SRPConfig.nexusLeemDis}. Those two
     * options live in the config package in 1.10.9, so the port keeps their defaults (5 / 32) here.
     */
    private void trySpawnRooterSi(ServerLevel serverLevel) {
        if (random.nextDouble() >= NEXUS_ROOTER_CHANCE) {
            return;
        }
        // Original EntityAINexusGrow.spawnLeem gate: the nest only calls rooters once the dimension has
        // reached the configured nest phase or development level.
        SrpWorldData data = SrpWorldData.get(serverLevel);
        if (data.evolutionPhase() < Config.evolutionNests()
                && data.ubiquitousDevelopmentOverride() < Config.deveNestsUse()) {
            return;
        }
        int rooterCount = 0;
        double nearestDistanceSqr = Double.MAX_VALUE;
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity instanceof NexusParasiteEntity other && other != this
                    && other.activeKind().family == Family.ROOTER) {
                rooterCount++;
                nearestDistanceSqr = Math.min(nearestDistanceSqr, distanceToSqr(other));
            }
        }
        if (rooterCount > NEXUS_ROOTER_CAP || nearestDistanceSqr < NEXUS_ROOTER_DISTANCE * NEXUS_ROOTER_DISTANCE) {
            return;
        }
        NexusParasiteEntity rooter = createNexus(serverLevel, Family.ROOTER, 1);
        if (rooter == null) {
            return;
        }
        rooter.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
        rooter.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                EntitySpawnReason.MOB_SUMMONED, null);
        rooter.setCustomName(getCustomName());
        rooter.setCustomNameVisible(isCustomNameVisible());
        if (serverLevel.addFreshEntity(rooter)) {
            playSound(ModSounds.get("leem.si"), 4.0F, 1.0F);
        }
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
        // Original EntityAINexusGrow.checkPhase dimLock: `dimLock >= currentStage + 1` stops growth.
        if (dimensionStageLock >= activeKind.stage + 1) {
            return false;
        }
        NexusParasiteEntity next = createNexus(serverLevel, activeKind.family, activeKind.stage + 1);
        if (next == null) {
            return false;
        }
        next.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
        next.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(next.blockPosition()),
                EntitySpawnReason.MOB_SUMMONED, null);
        next.setCustomName(getCustomName());
        next.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            next.setPersistenceRequired();
        }
        serverLevel.addFreshEntity(next);
        // Original EntityAINexusGrow.upgradeV/upgradeD/upgradeL rolled for a nexus protection
        // structure right after each stage upgrade (SRPConfig.nexusStructures, default true):
        // Beckon 0.5, Dispatcher 0.3, Rooter 0.3. The original only reached this from inside its
        // `if (SRPConfigSystems.rsSounds)` block; the port keeps the structure roll unconditional
        // because sound configuration no longer gates world generation.
        generateProtectionStructure(serverLevel, activeKind);
        discard();
        return true;
    }

    /**
     * Port of {@code EntityPDispatcher.generateStructure()} (NexusProtection1 at the block below),
     * {@code EntityPBeckon.generateStructure()} (NexusProtection2 at the block) and
     * {@code EntityPRooter.generateStructure()} (NexusProtection3 at the block), all built with
     * {@code stage = 1}.
     */
    private void generateProtectionStructure(ServerLevel serverLevel, Kind activeKind) {
        double chance = switch (activeKind.family) {
            case BECKON -> 0.5D;
            case DISPATCHER -> 0.3D;
            case ROOTER -> 0.3D;
            case ROOTERBALL -> 0.0D;
        };
        if (chance <= 0.0D || random.nextDouble() >= chance) {
            return;
        }
        BlockPos origin = activeKind.family == Family.DISPATCHER ? blockPosition().below() : blockPosition();
        switch (activeKind.family) {
            case DISPATCHER -> new WorldGenParasiteNexusProtection1(1).generate(serverLevel, random, origin);
            case BECKON -> new WorldGenParasiteNexusProtection2(1).generate(serverLevel, random, origin);
            case ROOTER -> new WorldGenParasiteNexusProtection3(1).generate(serverLevel, random, origin);
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
        BECKON_SI(Family.BECKON, 1, 25.0D, 4.0D, 2.5D, 4, 4, 50, 1.0F, 3.0D, 16, 16.0D),
        BECKON_SII(Family.BECKON, 2, 60.0D, 8.0D, 6.0D, 5, 6, 45, 1.0F, 3.0D, 32, 32.0D),
        BECKON_SIII(Family.BECKON, 3, 110.0D, 16.0D, 13.0D, 6, 8, 40, 1.0F, 3.0D, 64, 48.0D),
        BECKON_SIV(Family.BECKON, 4, 220.0D, 25.0D, 20.0D, 8, 12, 40, 5.0F, 18.0D, 220, 128.0D),
        DISPATCHER_SI(Family.DISPATCHER, 1, 33.0D, 7.0D, 3.0D, 4, 3, 60, 1.0F, 3.0D, 16, 32.0D),
        DISPATCHER_SII(Family.DISPATCHER, 2, 70.0D, 14.0D, 7.0D, 5, 5, 55, 2.0F, 6.0D, 32, 64.0D),
        DISPATCHER_SIII(Family.DISPATCHER, 3, 130.0D, 21.0D, 14.0D, 6, 7, 50, 3.0F, 9.0D, 64, 96.0D),
        DISPATCHER_SIV(Family.DISPATCHER, 4, 250.0D, 28.0D, 22.0D, 8, 9, 45, 5.0F, 18.0D, 220, 256.0D),
        ROOTER_SI(Family.ROOTER, 1, 25.0D, 4.0D, 2.5D, 4, 3, 60, 0.0F, 0.0D, 16, 16.0D),
        ROOTER_SII(Family.ROOTER, 2, 60.0D, 8.0D, 6.0D, 5, 5, 55, 2.0F, 6.0D, 32, 32.0D),
        ROOTER_SIII(Family.ROOTER, 3, 110.0D, 16.0D, 13.0D, 6, 7, 50, 3.0F, 9.0D, 64, 48.0D),
        ROOTER_SIV(Family.ROOTER, 4, 220.0D, 25.0D, 20.0D, 8, 9, 45, 5.0F, 18.0D, 220, 128.0D),
        ROOTERBALL(Family.ROOTERBALL, 0, 20.0D, 10.0D, 0.0D, 0, 0, 0, 0.0F, 0.0D, 0, 16.0D);

        private final Family family;
        private final int stage;
        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final int bombCount;
        private final int activeCap;
        private final int summonCooldown;
        private final float maxBlockHardness;
        private final double blockRange;
        private final int experience;
        private final double defaultFollowRange;

        Kind(Family family, int stage, double maxHealth, double armor, double attackDamage, int bombCount,
             int activeCap, int summonCooldown, float maxBlockHardness, double blockRange, int experience,
             double defaultFollowRange) {
            this.family = family;
            this.stage = stage;
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.bombCount = bombCount;
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
