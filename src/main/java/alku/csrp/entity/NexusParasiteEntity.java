package alku.csrp.entity;

import alku.csrp.Csrp;
import alku.csrp.block.FogBlock;
import alku.csrp.block.InfestedBlock;
import alku.csrp.event.StatusEffectEvents;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModBlocks;
import alku.csrp.infection.BlockInfestation;
import alku.csrp.infection.InfestationSpreadLimiter;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModItems;
import alku.csrp.world.SrpCoreSystems;
import alku.csrp.world.SrpWorldData;
import alku.csrp.config.WorldConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.GameRules;
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
    private static final EntityDataAccessor<Boolean> ROOTER_GROUND_COVERED = SynchedEntityData.defineId(
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
    private static final int DISPATCHER_ATTACK_INTERVAL = 10;
    private static final int DISPATCHER_GLOBAL_DEFENDER_CAP = 7;
    private static final int STAGE_FOUR_UPGRADE_INFECTED_CAP = 30;
    private static final int DISPATCHER_FOG_MIN_Y_OFFSET = -2;
    private static final int DISPATCHER_FOG_MAX_Y_OFFSET = 4;

    private final Kind kind;
    private final SummonCapacityTracker summonCapacityTracker = new SummonCapacityTracker();
    private final List<String> storedParasiteIds = new ArrayList<>();
    private final List<UUID> dispatcherTentacleIds = new ArrayList<>();
    private final List<UUID> collectiveMemberIds = new ArrayList<>();
    private int growthTicks;
    private int growthDelayTicks;
    private int temporaryLifetimeTicks = -1;
    // Mirrors EntityAIVenkrolSummon's signed attack timer.
    private int summonCooldown;
    private int summonWaveCount;
    private int summonWaveLimit;
    private int collectiveExtraRemaining;
    private int collectiveLockTicks;
    private UUID collectiveLeader;
    private int dispatcherAttackCooldown;
    private int seizerCooldown;
    private int bombCooldown;
    private int supportCooldown;
    private int blockBreakCooldown;
    private int forcedEvolutionCooldown;
    private int rooterBlockCheckTicks;
    private int colonyPlacementProgress;
    private int dispatcherCrude;
    private int dispatcherInfected;
    private int dispatcherFlesh;
    private int dispatcherMangler;
    private int stageFourCount = 200;
    private int stageFourThunderCooldown = 300;
    private double buried;
    private boolean growthAttemptActive;
    private boolean collectiveAttack;
    private boolean canGrow = true;
    private boolean rooterGroundCovered;


    public NexusParasiteEntity(EntityType<? extends NexusParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.experience;
        growthDelayTicks = defaultGrowthDelay();
        if (kind.family == Family.DISPATCHER || kind.family == Family.ROOTER) {
            buried = 0.1D;
            setParasiteStatus(3);
            setFloorTimer((float) buried);
        } else {
            setFloorTimer(-0.1F);
        }
        if (kind.family == Family.BECKON) {
            setBODY(kind.maxBody - getBODY());
        }
        if (kind.family == Family.ROOTER) {
            supportCooldown = 10;
        }
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, kind.followRange);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BODY, 0.5F);
        builder.define(PARASITE_STATUS, 0);
        builder.define(FLOOR_TIMER, -1.0F);
        builder.define(ROOTER_GROUND_COVERED, false);
    }

    public float getBODY() {
        return entityData.get(BODY);
    }

    public void setBODY(float increment) {
        float newValue = getBODY() + increment;
        if (newValue > activeKind().maxBody) {
            newValue = activeKind().maxBody;
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

    public boolean getRooterGroundCovered() {
        return entityData.get(ROOTER_GROUND_COVERED);
    }

    private void setRooterGroundCovered(boolean value) {
        rooterGroundCovered = value;
        entityData.set(ROOTER_GROUND_COVERED, value);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (activeKind().isRooterBall()) {
            return;
        }
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidNexusTarget));
    }

    private boolean isValidNexusTarget(LivingEntity target) {
        return isValidParasiteTarget(target) && !(target instanceof Animal)
                && !(target instanceof WaterAnimal) && !(target instanceof Creeper);
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (buried > 0.0D) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        getNavigation().stop();
        anchorStationaryPosition();
        updateBurial();
        decrementCooldowns();

        Kind activeKind = activeKind();
        if (temporaryLifetimeTicks >= 0 && --temporaryLifetimeTicks <= 0) {
            hurt(damageSources().lava(), Float.MAX_VALUE);
        }
        if (isRemoved()) {
            return;
        }
        if (activeKind.family == Family.BECKON) {
            setBODY(getParasiteStatus() == 0 ? 0.04F : -0.04F);
        }

        if (activeKind.isRooterBall()) {
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            summonCapacityTracker.prune(serverLevel);
            if (activeKind.stage > 0 && activeKind.stage < 4 && tickGrowth(serverLevel, activeKind)) {
                return;
            }
            switch (activeKind.family) {
                case BECKON -> {
                    tickBeckonSummon(activeKind);
                    if (activeKind.stage == 4) {
                        tickBeckonStageFour(serverLevel);
                    } else if (tickCount % 201 == 0) {
                        BlockInfestation.infestAround(serverLevel, blockPosition(), activeKind.stage,
                                InfestationSpreadLimiter.Type.BECKON);
                    }
                }
                case DISPATCHER -> {
                    tickDispatcher(serverLevel, activeKind);
                }
                case ROOTER -> tickRooter(activeKind);
                case ROOTERBALL -> tickRooterBall();
            }
        }
    }

    private void anchorStationaryPosition() {
        Vec3 movement = getDeltaMovement();
        double vertical = onGround() ? Math.min(movement.y, 0.0D) : movement.y - 0.5D;
        setDeltaMovement(0.0D, vertical, 0.0D);
        setPos(xo, getY(), zo);
    }

    private void updateBurial() {
        if (!(buried > 0.0D)) {
            if (getParasiteStatus() == 3) {
                setParasiteStatus(0);
            }
            buried = -0.1D;
        } else {
            buried = Math.max(-0.1D, buried - 0.08D);
        }
        setFloorTimer((float) buried);
    }

    private boolean tickGrowth(ServerLevel serverLevel, Kind activeKind) {
        if (temporaryLifetimeTicks >= 0) {
            return false;
        }
        if (tickCount % 21 == 0) {
            canGrow = growthAllowedAtPhase(SrpWorldData.get(serverLevel).evolutionPhase(), activeKind.stage);
        }
        if (!canGrow) {
            return false;
        }
        growthAttemptActive = true;
        if (++growthTicks < growthDelayTicks || !canUpgradeToNextStage(serverLevel, activeKind)) {
            return false;
        }
        return evolve();
    }

    private boolean canUpgradeToNextStage(ServerLevel serverLevel, Kind activeKind) {
        if (activeKind.stage != 3) {
            return true;
        }

        SrpWorldData data = SrpWorldData.get(serverLevel);
        if (alku.csrp.Config.useEvolutionPhases()
                && data.evolutionPhase() < 11 && data.ubiquitousDevelopmentOverride() < 4) {
            return false;
        }
        if (activeKind.family == Family.DISPATCHER
                && data.nearestColonyInEffectRange(blockPosition()) != null) {
            return false;
        }
        // CSRP represents the original parasite biome with infested terrain rather than a
        // separate biome class; every Nexus family pauses its stage-IV upgrade there.
        return activeKind.family == Family.ROOTERBALL
                || !(serverLevel.getBlockState(blockPosition().below()).getBlock() instanceof InfestedBlock);
    }

    private boolean growthAllowedAtPhase(int phase, int stage) {
        if (!alku.csrp.Config.useEvolutionPhases()) {
            return true;
        }
        if (phase <= 0) {
            return false;
        }
        double penalty = switch (stage) {
            case 1 -> phase <= 2 ? 1.0D : phase == 3 ? 0.95D : 0.0D;
            case 2 -> phase <= 2 ? 1.0D : phase <= 4 ? 0.95D : 0.0D;
            case 3 -> phase <= 2 ? 1.0D : phase <= 5 ? 0.95D : 0.0D;
            default -> 0.0D;
        };
        return random.nextDouble() >= penalty;
    }


    private void tickBeckonSummon(Kind activeKind) {
        LivingEntity target = getTarget();
        if (collectiveLockTicks > 0) {
            setParasiteStatus(target != null && target.isAlive() ? 1 : 0);
            collectiveLockTicks--;
            summonCooldown--;
            if (collectiveLockTicks == 0) {
                collectiveLeader = null;
                summonCooldown = -activeKind.summonCooldownTicks();
            }
            return;
        }
        if (target == null || !target.isAlive()) {
            if (collectiveAttack || !collectiveMemberIds.isEmpty() || summonWaveCount > 0) {
                finishBeckonWave(activeKind);
            } else {
                summonWaveLimit = 0;
                collectiveExtraRemaining = 0;
            }
            setParasiteStatus(0);
            if (summonCooldown > 0) {
                summonCooldown--;
            }
            return;
        }
        setParasiteStatus(1);
        if (!hasLineOfSight(target)) {
            if (summonCooldown > 0) {
                summonCooldown--;
            }
            return;
        }
        summonCooldown++;
        if (summonCooldown <= 0 || summonCooldown % 20 != 0) {
            return;
        }

        if (summonCooldown == 20) {
            prepareCollectiveAttack(activeKind);
        }
        summonWaveLimit = activeKind.waveLimit;
        if (summonWaveCount < summonWaveLimit) {
            if (!canSpawnBeckonMob(activeKind) || !spawnBeckonMob(activeKind, target)) {
                return;
            }
            summonWaveCount++;
            if (collectiveAttack) {
                lockCollectiveBeckons();
            }
        } else if (collectiveAttack && collectiveExtraRemaining > 0) {
            if (!canSpawnBeckonMob(activeKind) || !spawnBeckonMob(activeKind, target)) {
                return;
            }
            collectiveExtraRemaining--;
        }
        if (summonWaveCount >= summonWaveLimit
                && (!collectiveAttack || collectiveExtraRemaining <= 0)) {
            finishBeckonWave(activeKind);
        }
    }

    private boolean canSpawnBeckonMob(Kind activeKind) {
        return summonCapacityTracker.usedCapacity() < activeKind.activeCap;
    }

    private boolean spawnBeckonMob(Kind activeKind, LivingEntity target) {
        if (activeKind.stage == 4) {
            return spawnStageFourBeckon(target);
        }
        boolean air = target.getY() >= getY() + 3.0D;
        return BiomassEntity.spawnFromNexus(this, this, activeKind.stage,
                beckonSummonOptions(activeKind.stage, air), target);
    }

    private boolean spawnStageFourBeckon(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        List<BiomassEntity.SummonOption> options = List.of(
                summonOption(ModEntities.BECKON_SI.get(), 0.3D, 3),
                summonOption(ModEntities.BECKON_SII.get(), 0.4D, 3),
                summonOption(ModEntities.BECKON_SIII.get(), 0.3D, 4));
        var selected = BiomassEntity.selectSummon(this, this, options);
        if (selected.isEmpty()) {
            return false;
        }
        BiomassEntity.SummonOption option = selected.get();
        for (int attempt = 0; attempt < 8; attempt++) {
            int offsetX = random.nextInt(12) * (random.nextBoolean() ? 1 : -1);
            int offsetZ = random.nextInt(12) * (random.nextBoolean() ? 1 : -1);
            BlockPos spawnPos = BlockPos.containing(target.getX() + offsetX, target.getY(),
                    target.getZ() + offsetZ);
            if (!serverLevel.isEmptyBlock(spawnPos) || serverLevel.isEmptyBlock(spawnPos.below())) {
                continue;
            }
            NexusParasiteEntity spawned = createBeckonStageFourChild(serverLevel, option.type());
            if (spawned == null) {
                return false;
            }
            spawned.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    getYRot(), getXRot());
            if (!serverLevel.noCollision(spawned)) {
                spawned.discard();
                continue;
            }
            spawned.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.MOB_SUMMONED, null);
            spawned.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16.0D);
            spawned.canGrow = false;
            spawned.temporaryLifetimeTicks = 300;
            spawned.setTarget(target);
            if (!serverLevel.addFreshEntity(spawned)) {
                spawned.discard();
                continue;
            }
            reserveTrackedSummon(spawned.getUUID(), option.cost());
            return true;
        }
        return false;
    }

    private NexusParasiteEntity createBeckonStageFourChild(ServerLevel serverLevel,
                                                            EntityType<? extends Mob> type) {
        if (type == ModEntities.BECKON_SI.get()) return ModEntities.BECKON_SI.get().create(serverLevel);
        if (type == ModEntities.BECKON_SII.get()) return ModEntities.BECKON_SII.get().create(serverLevel);
        if (type == ModEntities.BECKON_SIII.get()) return ModEntities.BECKON_SIII.get().create(serverLevel);
        return null;
    }

    private void prepareCollectiveAttack(Kind activeKind) {
        collectiveExtraRemaining = 0;
        collectiveAttack = false;
        collectiveMemberIds.clear();
        if (activeKind.collectiveMinimum <= 0 || activeKind.stage == 4) {
            return;
        }
        List<NexusParasiteEntity> group = connectedBeckons(activeKind.stage);
        if (group.size() < activeKind.collectiveMinimum) {
            return;
        }
        collectiveAttack = true;
        collectiveExtraRemaining = (int) Math.floor(group.size() * activeKind.collectiveExtra);
        for (NexusParasiteEntity member : group) {
            if (member != this) collectiveMemberIds.add(member.getUUID());
        }
    }

    private List<NexusParasiteEntity> connectedBeckons(int stage) {
        List<NexusParasiteEntity> group = new ArrayList<>();
        group.add(this);
        for (int index = 0; index < group.size(); index++) {
            NexusParasiteEntity origin = group.get(index);
            double range = origin.getAttributeValue(Attributes.FOLLOW_RANGE) + 5.0D;
            for (NexusParasiteEntity candidate : level().getEntitiesOfClass(NexusParasiteEntity.class,
                    origin.getBoundingBox().inflate(range), entity -> entity != origin && entity.isAlive()
                            && entity.activeKind().family == Family.BECKON
                            && entity.activeKind().stage == stage && entity.hasLineOfSight(origin))) {
                if (!group.contains(candidate)) group.add(candidate);
            }
        }
        return group;
    }

    private void lockCollectiveBeckons() {
        if (collectiveMemberIds.isEmpty()) {
            return;
        }
        for (UUID memberId : collectiveMemberIds) {
            Entity entity = level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(memberId) : null;
            if (entity instanceof NexusParasiteEntity beckon && beckon.isAlive()
                    && beckon.collectiveLeader == null) {
                beckon.collectiveLeader = getUUID();
                beckon.collectiveLockTicks = 200;
                beckon.summonCooldown = -1;
            }
        }
    }

    private List<BiomassEntity.SummonOption> beckonSummonOptions(int stage, boolean air) {
        if (stage == 1) {
            return air ? List.of() : List.of(
                    summonOption(ModEntities.RUPTER.get(), 1.0D, 1));
        }
        if (stage == 2) {
            if (air) {
                return List.of(summonOption(ModEntities.CARRIER_FLYING.get(), 0.3D, 3));
            }
            return List.of(
                    summonOption(ModEntities.SIM_HUMAN.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_COW.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_SHEEP.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_WOLF.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_BIGSPIDER.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_PIG.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_VILLAGER.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_HORSE.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_BEAR.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_ENDERMAN.get(), 0.2D, 3),
                    summonOption(ModEntities.RUPTER.get(), 0.1D, 1));
        }
        if (stage == 3) {
            if (air) {
                return List.of(
                        summonOption(ModEntities.OVERSEER.get(), 0.2D, 6),
                        summonOption(ModEntities.BOMBER_LIGHT.get(), 0.2D, 6),
                        summonOption(ModEntities.CARRIER_FLYING.get(), 0.5D, 3),
                        summonOption(ModEntities.PRI_YELLOWEYE.get(), 0.5D, 6));
            }
            return List.of(
                    summonOption(ModEntities.PRI_SUMMONER.get(), 0.2D, 6),
                    summonOption(ModEntities.PRI_MANDUCATER.get(), 0.4D, 6),
                    summonOption(ModEntities.PRI_BOLSTER.get(), 0.1D, 6),
                    summonOption(ModEntities.PRI_REEKER.get(), 0.4D, 6),
                    summonOption(ModEntities.PRI_LONGARMS.get(), 0.4D, 6),
                    summonOption(ModEntities.PRI_ARACHNIDA.get(), 0.4D, 6),
                    summonOption(ModEntities.PRI_VERMIN.get(), 0.3D, 6),
                    summonOption(ModEntities.PRI_TOZOON.get(), 0.3D, 3),
                    summonOption(ModEntities.SIM_HUMAN.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_COW.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_SHEEP.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_WOLF.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_BIGSPIDER.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_PIG.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_VILLAGER.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_HORSE.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_BEAR.get(), 0.2D, 3),
                    summonOption(ModEntities.SIM_ENDERMAN.get(), 0.2D, 3),
                    summonOption(ModEntities.CARRIER_HEAVY.get(), 0.2D, 6),
                    summonOption(ModEntities.RUPTER.get(), 0.1D, 1),
                    summonOption(ModEntities.VIGILANTE.get(), 0.2D, 6),
                    summonOption(ModEntities.WARDEN.get(), 0.2D, 6));
        }
        return List.of(
                summonOption(ModEntities.BECKON_SI.get(), 0.3D, 3),
                summonOption(ModEntities.BECKON_SII.get(), 0.4D, 3),
                summonOption(ModEntities.BECKON_SIII.get(), 0.3D, 4));
    }

    private static BiomassEntity.SummonOption summonOption(EntityType<? extends Mob> type,
                                                            double chance, int cost) {
        return new BiomassEntity.SummonOption(type, chance, cost);
    }

    private void finishBeckonWave(Kind activeKind) {
        summonWaveLimit = 0;
        summonWaveCount = 0;
        summonCooldown = -activeKind.summonCooldownTicks();
        collectiveExtraRemaining = 0;
        collectiveAttack = false;
        for (UUID memberId : collectiveMemberIds) {
            Entity entity = level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(memberId) : null;
            if (entity instanceof NexusParasiteEntity beckon && getUUID().equals(beckon.collectiveLeader)) {
                beckon.collectiveLeader = null;
                beckon.collectiveLockTicks = 0;
                beckon.summonCooldown = -beckon.activeKind().summonCooldownTicks();
            }
        }
        collectiveMemberIds.clear();
    }

    private void tickDispatcher(ServerLevel serverLevel, Kind activeKind) {
        LivingEntity target = getTarget();
        int storagePeriod = target == null ? 81 : 11;
        if (tickCount % storagePeriod == 0) {
            unhideNearbyHosts(serverLevel, activeKind);
            storeNearbyParasite();
            placeNestFog(activeKind.stage);
        }
        if (activeKind.stage == 4) {
            tryPlaceFirstColony();
        }
        if (target == null || !target.isAlive() || dispatcherAttackCooldown > 0) {
            return;
        }
        dispatcherAttackCooldown = DISPATCHER_ATTACK_INTERVAL;
        if (!storedParasiteIds.isEmpty() && random.nextInt(3) != 0) {
            int selected = random.nextInt(activeKind.stage * 2);
            if (selected < storedParasiteIds.size()) {
                for (int index = 0; index <= selected; index++) {
                    if (random.nextBoolean()) {
                        spawnDispatcherTentacle(serverLevel, target, activeKind.stage);
                    }
                }
            }
            return;
        }
        boolean sentry = activeKind.stage >= 3 && random.nextBoolean();
        if (sentry) {
            spawnDispatcherDefender(serverLevel, target, ModEntities.SENTRY.get(), 1, 6, 4);
        } else if (seizerCooldown <= 0 && noNearbySeizer()) {
            if (spawnDispatcherDefender(serverLevel, target, ModEntities.SEIZER.get(), 1, 3, 1)) {
                seizerCooldown = 40;
            }
        }
    }

    private boolean noNearbySeizer() {
        AABB range = getBoundingBox().inflate(7.0D, 5.0D, 7.0D);
        return level().getEntitiesOfClass(DeterrentParasiteEntity.class, range,
                entity -> entity.getKind() == DeterrentParasiteEntity.Kind.SEIZER && entity.isAlive()).isEmpty();
    }

    private boolean globalDefenderCapReached(ServerLevel serverLevel,
                                             EntityType<DeterrentParasiteEntity> type) {
        if (type != ModEntities.SEIZER.get() && type != ModEntities.SENTRY.get()) {
            return false;
        }
        DeterrentParasiteEntity.Kind defenderKind = type == ModEntities.SEIZER.get()
                ? DeterrentParasiteEntity.Kind.SEIZER : DeterrentParasiteEntity.Kind.SENTRY;
        int count = 0;
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity instanceof DeterrentParasiteEntity defender
                    && defender.getKind() == defenderKind && defender.isAlive()
                    && ++count >= DISPATCHER_GLOBAL_DEFENDER_CAP) {
                return true;
            }
        }
        return false;
    }

    private boolean spawnDispatcherDefender(ServerLevel serverLevel, LivingEntity target,
                                            EntityType<DeterrentParasiteEntity> type,
                                            int cost, int maxOffset, int minOffset) {
        if (!canReserveSummon(cost)) {
            return false;
        }
        if (globalDefenderCapReached(serverLevel, type)) {
            return false;
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            int distanceX = minOffset + random.nextInt(maxOffset);
            int distanceZ = minOffset + random.nextInt(maxOffset);
            if (random.nextBoolean()) distanceX = -distanceX;
            if (random.nextBoolean()) distanceZ = -distanceZ;
            BlockPos requested = BlockPos.containing(target.getX() + distanceX, target.getY(),
                    target.getZ() + distanceZ);
            BlockPos spawnPos = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, requested);
            if (!hasDispatcherSpawnGround(serverLevel, spawnPos, activeKind().stage)
                    || !serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(spawnPos).inflate(2.0D),
                    Entity::isAlive).isEmpty()) {
                continue;
            }
            DeterrentParasiteEntity defender = type.create(serverLevel);
            if (defender == null) {
                return false;
            }
            defender.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    getYRot(), getXRot());
            if (!serverLevel.noCollision(defender)) {
                continue;
            }
            defender.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.MOB_SUMMONED, null);
            defender.hideForDispatcher();
            defender.setDispatchTarget(target);
            defender.setTarget(target);
            if (!serverLevel.addFreshEntity(defender)) {
                continue;
            }
            reserveTrackedSummon(defender.getUUID(), cost);
            return true;
        }
        return false;
    }

    private boolean hasDispatcherSpawnGround(ServerLevel serverLevel, BlockPos spawnPos, int stage) {
        float totalHardness = 0.0F;
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos below = spawnPos.below(depth);
            float hardness = serverLevel.getBlockState(below).getDestroySpeed(serverLevel, below);
            if (hardness <= 0.0F) {
                return false;
            }
            totalHardness += hardness;
        }
        return totalHardness < switch (stage) {
            case 1 -> 10.0F;
            case 2 -> 15.0F;
            case 3 -> 20.0F;
            default -> 30.0F;
        };
    }

    private void unhideNearbyHosts(ServerLevel serverLevel, Kind activeKind) {
        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(range), Entity::isAlive);
        for (LivingEntity host : nearby) {
            if (host == this || host instanceof Parasite || !host.onGround() || host.isInWater()
                    || host instanceof Player) {
                continue;
            }
            if (nearby.size() < storedParasiteIds.size()) {
                spawnDispatcherTentacle(serverLevel, host, activeKind.stage);
            }
            if (host.hasEffect(ModMobEffects.COTH)) {
                host.addEffect(new MobEffectInstance(ModMobEffects.COTH, 600, 5,
                        false, false), this);
            }
        }
    }

    private void spawnDispatcherTentacle(ServerLevel serverLevel, LivingEntity target, int stage) {
        dispatcherTentacleIds.removeIf(id -> {
            Entity entity = serverLevel.getEntity(id);
            return entity == null || !entity.isAlive();
        });
        if (dispatcherTentacleIds.size() >= stage * 3 || storedParasiteIds.isEmpty()) {
            return;
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            int offsetX = 3 + random.nextInt(5);
            int offsetZ = 3 + random.nextInt(5);
            if (random.nextBoolean()) offsetX = -offsetX;
            if (random.nextBoolean()) offsetZ = -offsetZ;
            BlockPos requested = BlockPos.containing(target.getX() + offsetX, target.getY(), target.getZ() + offsetZ);
            BlockPos spawnPos = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, requested);
            DeterrentParasiteEntity tentacle = ModEntities.DISPATCHERTEN.get().create(serverLevel);
            if (tentacle == null) {
                return;
            }
            tentacle.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    getYRot(), getXRot());
            if (!serverLevel.noCollision(tentacle)) {
                continue;
            }
            tentacle.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.MOB_SUMMONED, null);
            String id = storedParasiteIds.remove(random.nextInt(storedParasiteIds.size()));
            ResourceLocation typeId = ResourceLocation.tryParse(id);
            if (typeId == null) {
                storedParasiteIds.add(id);
                continue;
            }
            tentacle.setDispatcherParent(this);
            tentacle.setDispatchEntity(typeId);
            tentacle.setDispatchTarget(target);
            tentacle.setTarget(target);
            if (serverLevel.addFreshEntity(tentacle)) {
                consumeDispatcherResourceFor(id);
                dispatcherTentacleIds.add(tentacle.getUUID());
                return;
            }
            storedParasiteIds.add(id);
        }
    }

    private void tickRooter(Kind activeKind) {
        if (supportCooldown <= 0) {
            applyRooterSupport(activeKind.stage);
            supportCooldown = activeKind.supportCooldownTicks;
        }
        if (activeKind.rooterBlockRange() <= 0 || ++rooterBlockCheckTicks < 140) {
            return;
        }
        rooterBlockCheckTicks = 0;
        boolean hasOpenGround = false;
        for (int x = -activeKind.rooterBlockRange(); x <= activeKind.rooterBlockRange(); x++) {
            for (int z = -activeKind.rooterBlockRange(); z <= activeKind.rooterBlockRange(); z++) {
                if (level().getBlockState(BlockPos.containing(getX() + x, getY() - 0.5D, getZ() + z)).isAir()) {
                    hasOpenGround = true;
                    break;
                }
            }
            if (hasOpenGround) {
                break;
            }
        }
        setRooterGroundCovered(!hasOpenGround);
    }

    private void tickRooterBall() {
        if (tickCount == 5) {
            removeEffect(ModMobEffects.PIVOT);
        }
    }

    private void tickBeckonStageFour(ServerLevel serverLevel) {
        if (stageFourCount > Integer.MIN_VALUE) stageFourCount--;
        if (stageFourThunderCooldown > 0) stageFourThunderCooldown--;
        if (serverLevel.isRaining() && serverLevel.isThundering()
                && serverLevel.canSeeSky(blockPosition().above())) {
            applyStormVortex();
        }
        if (random.nextDouble() >= 0.015D || stageFourThunderCooldown > 0) {
            return;
        }
        if (stageFourCount < 0 && upgradeNearbyParasites(serverLevel)) {
            stageFourThunderCooldown = 300;
            stageFourCount = 300;
            return;
        }
        int offsetX = random.nextInt(64) * (random.nextBoolean() ? 1 : -1);
        int offsetZ = random.nextInt(64) * (random.nextBoolean() ? 1 : -1);
        BlockPos requested = blockPosition().offset(offsetX, 0, offsetZ);
        BlockPos strikePos = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, requested);
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning != null) {
            lightning.moveTo(strikePos.getX() + 0.5D, strikePos.getY(), strikePos.getZ() + 0.5D);
            lightning.setVisualOnly(true);
            serverLevel.addFreshEntity(lightning);
        }
        stageFourThunderCooldown = 200;
    }

    private boolean upgradeNearbyParasites(ServerLevel serverLevel) {
        AABB area = getBoundingBox().inflate(48.0D, 34.0D, 48.0D);
        for (NexusParasiteEntity nexus : serverLevel.getEntitiesOfClass(NexusParasiteEntity.class, area,
                entity -> entity != this && entity.isAlive() && entity.activeKind().family == Family.BECKON
                        && entity.activeKind().stage < 4)) {
            nexus.growthTicks = 0;
            nexus.growthAttemptActive = false;
        }
        List<LivingEntity> infected = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                this::isInfectedTier);
        if (infected.size() > STAGE_FOUR_UPGRADE_INFECTED_CAP) {
            return true;
        }
        int upgraded = 0;
        for (LivingEntity parasite : infected) {
            if (forceEvolveParasite(serverLevel, parasite) && ++upgraded >= 3) {
                return true;
            }
        }
        for (LivingEntity parasite : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity instanceof PrimitiveParasiteEntity
                        && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().startsWith("pri_"))) {
            if (parasite instanceof PrimitiveParasiteEntity primitive) {
                primitive.addEvolutionKillCount(1_000.0D);
                spawnVisualThunder(serverLevel, parasite.position());
                if (++upgraded >= 3) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInfectedTier(LivingEntity candidate) {
        if (candidate == this || !candidate.isAlive()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType());
        String path = id == null ? "" : id.getPath();
        return path.startsWith("sim_") || path.startsWith("fer_") || isAssimilatedTier(candidate);
    }

    private boolean forceEvolveParasite(ServerLevel serverLevel, LivingEntity source) {
        if (!isInfectedTier(source)) {
            return false;
        }
        ResourceLocation typeId = ResourceLocation.tryParse(randomPrimitiveTypeId());
        EntityType<?> targetType = typeId == null ? null
                : BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
        Entity created = targetType == null ? null : targetType.create(serverLevel);
        if (!(created instanceof Mob replacement)) {
            return false;
        }
        replacement.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        replacement.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(source.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        replacement.setCustomName(source.getCustomName());
        replacement.setCustomNameVisible(source.isCustomNameVisible());
        if (source instanceof Mob sourceMob) {
            replacement.setTarget(sourceMob.getTarget());
        }
        replacement.setHealth(Math.max(1.0F,
                replacement.getMaxHealth() * source.getHealth() / Math.max(1.0F, source.getMaxHealth())));
        if (!serverLevel.addFreshEntity(replacement)) {
            replacement.discard();
            return false;
        }
        spawnVisualThunder(serverLevel, source.position());
        source.discard();
        return true;
    }

    private void spawnVisualThunder(ServerLevel serverLevel, Vec3 position) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning != null) {
            lightning.moveTo(position);
            lightning.setVisualOnly(true);
            serverLevel.addFreshEntity(lightning);
        }
    }

    private void applyStormVortex() {
        AABB area = new AABB(getX() - 120.0D, getY(), getZ() - 120.0D,
                getX() + 120.0D, getY() + 50.0D, getZ() + 120.0D);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                living -> living != this && living.isAlive() && !(living instanceof Parasite)
                        && living.getY() >= getY() && !living.isPassenger())) {
            if (target instanceof Player player) {
                if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.VENKROL_BOOTS)
                        || player.isSpectator() || player.getAbilities().invulnerable
                        && player.getAbilities().flying) {
                    continue;
                }
            }
            double dx = getX() - target.getX();
            double dz = getZ() - target.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 120.0D) {
                continue;
            }
            distance = Math.max(distance, 0.01D);
            double nx = dx / distance;
            double nz = dz / distance;
            double tier = distance >= 50.0D ? 0.05D : distance >= 25.0D ? 0.1D
                    : distance >= 15.0D ? 0.2D : distance >= 10.0D ? 0.35D
                    : distance >= 5.0D ? 0.55D : 1.0D;
            double pull = 0.08D * tier;
            double swirl = 0.07D * tier;
            double lift = distance <= 15.0D ? 0.25D * (1.0D - distance / 15.0D) : 0.0D;
            double height = target.getY() - getY();
            boolean fling = height > 16.0D && distance < 12.0D;
            if (height > 18.0D && distance > 18.0D) {
                continue;
            }
            double radialX = fling ? -nx : nx;
            double radialZ = fling ? -nz : nz;
            Vec3 movement = target.getDeltaMovement();
            if (fling) {
                double strength = 1.0D - Math.min(distance / 12.0D, 1.0D);
                pull *= 1.4D + 4.6D * strength;
                swirl *= 1.0D + 1.3D * strength;
                movement = movement.add(-nx * 1.1D * strength, -0.16D * strength, -nz * 1.1D * strength);
                movement = new Vec3(movement.x, Math.max(-1.6D, movement.y), movement.z);
            } else if (lift > 0.0D && height < 25.0D) {
                movement = movement.add(0.0D, lift, 0.0D);
                movement = new Vec3(movement.x, Math.min(1.2D, movement.y), movement.z);
                target.resetFallDistance();
            }
            double dotAway = movement.x * -nx + movement.z * -nz;
            pull *= dotAway > 0.0D ? 1.0D : 0.25D;
            target.setDeltaMovement(movement.add(radialX * pull - nz * swirl, 0.0D,
                    radialZ * pull + nx * swirl));
            target.hurtMarked = true;
        }
    }

    private void consumeDispatcherResourceFor(String id) {
        ResourceLocation type = ResourceLocation.tryParse(id);
        if (type == null) {
            return;
        }
        String path = type.getPath();
        if (path.startsWith("sim_")) {
            dispatcherInfected--;
        } else if (path.equals("movingflesh")) {
            dispatcherFlesh--;
        } else if (path.equals("incompleteform_small")) {
            dispatcherCrude--;
        } else if (path.equals("incompleteform_medium")) {
            dispatcherCrude -= 2;
        } else if (path.equals("mangler")) {
            dispatcherMangler--;
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && isDeadOrDying() && !activeKind().isRooterBall()) {
            dissipateNexusFog();
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
    public boolean hurt(DamageSource source, float amount) {
        Kind activeKind = activeKind();
        boolean damageRedirected = false;
        if (activeKind.family == Family.ROOTER && !level().isClientSide) {
            List<NexusParasiteEntity> cysts = rootmassCystsInRange(activeKind);
            if (!cysts.isEmpty()) {
                // Legacy Rooters split subsequent hits between nearby Rootmass Cysts.
                float sharedDamage = amount / cysts.size();
                for (NexusParasiteEntity cyst : cysts) {
                    cyst.hurt(source, sharedDamage);
                }
                damageRedirected = true;
            } else {
                spawnRootmassCysts(activeKind.rootmassCystSpawnLimit());
            }
        }

        float incomingDamage = damageRedirected ? 0.0F : amount;
        if (getParasiteStatus() == 0) {
            incomingDamage *= 0.4F;
        }
        boolean hurt = super.hurt(source, incomingDamage);
        if (!hurt || level().isClientSide || activeKind.isRooterBall()) {
            return hurt;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity && distanceToSqr(attacker) <= 36.0D && bombCooldown <= 0) {
            spawnBombVolley(activeKind.stage * 2, (float) activeKind.attackDamage);
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
        if (activeKind.family == Family.BECKON) {
            controllers.add(new CitadelAnimationController<>(this, "body_controller", 0, this::bodyAnimation));
            controllers.add(new CitadelAnimationController<>(this, "floor_controller", 0, this::floorAnimation));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("nexus_growth", growthTicks);
        tag.putInt("nexus_growth_delay", growthDelayTicks);
        tag.putInt("nexus_summon_cooldown", summonCooldown);
        tag.putInt("nexus_bomb_cooldown", bombCooldown);
        tag.putInt("nexus_support_cooldown", supportCooldown);
        tag.putInt("nexus_block_break_cooldown", blockBreakCooldown);
        tag.putInt("nexus_forced_evolution_cooldown", forcedEvolutionCooldown);
        tag.putBoolean("nexus_can_grow", canGrow);
        tag.putBoolean("nexus_growth_active", growthAttemptActive);
        tag.putInt("nexus_temporary_lifetime", temporaryLifetimeTicks);
        tag.putInt("nexus_summon_wave_count", summonWaveCount);
        tag.putInt("nexus_summon_wave_limit", summonWaveLimit);
        tag.putInt("nexus_collective_extra", collectiveExtraRemaining);
        tag.putBoolean("nexus_collective_attack", collectiveAttack);
        tag.putInt("nexus_collective_lock", collectiveLockTicks);
        if (collectiveLeader != null) tag.putUUID("nexus_collective_leader", collectiveLeader);
        tag.putInt("nexus_dispatcher_attack_cooldown", dispatcherAttackCooldown);
        tag.putInt("nexus_seizer_cooldown", seizerCooldown);
        tag.putInt("nexus_stage_four_count", stageFourCount);
        tag.putInt("nexus_stage_four_thunder_cooldown", stageFourThunderCooldown);
        tag.putInt("nexus_dispatcher_crude", dispatcherCrude);
        tag.putInt("nexus_dispatcher_infected", dispatcherInfected);
        tag.putInt("nexus_dispatcher_flesh", dispatcherFlesh);
        tag.putInt("nexus_dispatcher_mangler", dispatcherMangler);
        tag.putDouble("nexus_buried", buried);
        tag.putBoolean("nexus_rooter_ground_covered", getRooterGroundCovered());
        summonCapacityTracker.save(tag, "nexus_summon_capacity");
        tag.putFloat("nexus_body", getBODY());
        tag.putInt("nexus_parasite_status", getParasiteStatus());
        tag.putFloat("nexus_floor_timer", getFloorTimer());
        ListTag storedParasites = new ListTag();
        storedParasiteIds.forEach(id -> storedParasites.add(StringTag.valueOf(id)));
        tag.put("nexus_dispatcher_stored", storedParasites);
        ListTag tentacles = new ListTag();
        dispatcherTentacleIds.forEach(id -> tentacles.add(StringTag.valueOf(id.toString())));
        tag.put("nexus_dispatcher_tentacles", tentacles);
        ListTag collectiveMembers = new ListTag();
        collectiveMemberIds.forEach(id -> collectiveMembers.add(StringTag.valueOf(id.toString())));
        tag.put("nexus_collective_members", collectiveMembers);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        growthTicks = tag.getInt("nexus_growth");
        growthDelayTicks = tag.contains("nexus_growth_delay") ? tag.getInt("nexus_growth_delay")
                : defaultGrowthDelay();
        summonCooldown = tag.getInt("nexus_summon_cooldown");
        bombCooldown = tag.getInt("nexus_bomb_cooldown");
        supportCooldown = tag.getInt("nexus_support_cooldown");
        blockBreakCooldown = tag.getInt("nexus_block_break_cooldown");
        forcedEvolutionCooldown = tag.getInt("nexus_forced_evolution_cooldown");
        canGrow = !tag.contains("nexus_can_grow") || tag.getBoolean("nexus_can_grow");
        growthAttemptActive = tag.getBoolean("nexus_growth_active");
        temporaryLifetimeTicks = tag.contains("nexus_temporary_lifetime")
                ? tag.getInt("nexus_temporary_lifetime") : -1;
        summonWaveCount = tag.getInt("nexus_summon_wave_count");
        summonWaveLimit = tag.getInt("nexus_summon_wave_limit");
        collectiveExtraRemaining = tag.getInt("nexus_collective_extra");
        collectiveAttack = tag.getBoolean("nexus_collective_attack");
        collectiveLockTicks = tag.getInt("nexus_collective_lock");
        collectiveLeader = tag.hasUUID("nexus_collective_leader")
                ? tag.getUUID("nexus_collective_leader") : null;
        dispatcherAttackCooldown = tag.getInt("nexus_dispatcher_attack_cooldown");
        seizerCooldown = tag.getInt("nexus_seizer_cooldown");
        stageFourCount = tag.contains("nexus_stage_four_count") ? tag.getInt("nexus_stage_four_count") : 200;
        stageFourThunderCooldown = tag.contains("nexus_stage_four_thunder_cooldown")
                ? tag.getInt("nexus_stage_four_thunder_cooldown") : 300;
        dispatcherCrude = tag.getInt("nexus_dispatcher_crude");
        dispatcherInfected = tag.getInt("nexus_dispatcher_infected");
        dispatcherFlesh = tag.getInt("nexus_dispatcher_flesh");
        dispatcherMangler = tag.getInt("nexus_dispatcher_mangler");
        buried = tag.getDouble("nexus_buried");
        setRooterGroundCovered(tag.getBoolean("nexus_rooter_ground_covered"));
        summonCapacityTracker.load(tag, "nexus_summon_capacity");
        if (tag.contains("nexus_body")) {
            entityData.set(BODY, tag.getFloat("nexus_body"));
        }
        if (tag.contains("nexus_parasite_status")) {
            setParasiteStatus(tag.getInt("nexus_parasite_status"));
        }
        if (tag.contains("nexus_floor_timer")) {
            setFloorTimer(tag.getFloat("nexus_floor_timer"));
        }
        storedParasiteIds.clear();
        ListTag storedParasites = tag.getList("nexus_dispatcher_stored", Tag.TAG_STRING);
        for (int index = 0; index < storedParasites.size(); index++) {
            String id = storedParasites.getString(index);
            if (ResourceLocation.tryParse(id) != null) {
                storedParasiteIds.add(id);
            }
        }
        collectiveMemberIds.clear();
        ListTag collectiveMembers = tag.getList("nexus_collective_members", Tag.TAG_STRING);
        for (int index = 0; index < collectiveMembers.size(); index++) {
            try {
                collectiveMemberIds.add(UUID.fromString(collectiveMembers.getString(index)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public Kind getKind() {
        return activeKind();
    }

    @Override
    public int getSummonCapacity() {
        return activeKind().activeCap;
    }

    @Override
    public int getUsedSummonCapacity() {
        return summonCapacityTracker.usedCapacity();
    }

    @Override
    public void reserveTrackedSummon(UUID entityId, int cost) {
        summonCapacityTracker.reserve(entityId, cost);
    }

    @Override
    public void replaceTrackedSummon(UUID previousId, UUID replacementId, int cost) {
        summonCapacityTracker.replace(previousId, replacementId, cost);
    }

    @Override
    public void releaseTrackedSummon(UUID entityId) {
        summonCapacityTracker.release(entityId);
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
        if (dispatcherAttackCooldown > 0) dispatcherAttackCooldown--;
        if (seizerCooldown > 0) seizerCooldown--;
        if (bombCooldown > 0) bombCooldown--;
        if (supportCooldown > 0) supportCooldown--;
        if (blockBreakCooldown > 0) blockBreakCooldown--;
        if (forcedEvolutionCooldown > 0) forcedEvolutionCooldown--;
    }

    private boolean performFamilyAbility(Kind activeKind) {
        LivingEntity target = getTarget();
        return switch (activeKind.family) {
            case BECKON -> target != null && summonBeckonParasites(activeKind, target);
            case DISPATCHER -> target != null && summonDispatcherDefenses(activeKind, target);
            case ROOTER -> {
                if (activeKind.stage == 4 && target != null) {
                    spawnPodVolley(target);
                }
                yield true;
            }
            case ROOTERBALL -> false;
        };
    }

    /** 是否处于调度柱家族（供消失回收等外部逻辑判断）。 */
    public boolean isDispatcherFamily() {
        return activeKind().family == Family.DISPATCHER;
    }

    public boolean recallParasite(String entityTypeId) {
        if (!isDispatcherFamily()) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(entityTypeId);
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        int stage = activeKind().stage;
        if (!storedParasiteIds.add(entityTypeId)) {
            return false;
        }
        if (path.equals("movingflesh")) {
            dispatcherFlesh++;
        } else if (stage >= 2 && path.startsWith("sim_")) {
            dispatcherInfected++;
        } else if (stage >= 2 && path.startsWith("incompleteform_")) {
            dispatcherCrude += path.endsWith("medium") ? 2 : 1;
        } else if (stage >= 3 && path.equals("mangler")) {
            dispatcherMangler++;
        }
        return true;
    }

    private void storeNearbyParasite() {
        if (activeKind().family != Family.DISPATCHER) {
            return;
        }
        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        List<Mob> candidates = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(range),
                this::isStorableDispatcherParasite);
        for (Mob candidate : candidates) {
            if (!dispatcherCanStoreHere()) {
                return;
            }
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType());
            if (id == null || !Csrp.MODID.equals(id.getNamespace())) {
                continue;
            }
            storeDispatcherParasite(candidate, id);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, candidate.getX(),
                        candidate.getY() + candidate.getBbHeight() * 0.5D, candidate.getZ(),
                        12, 0.25D, 0.35D, 0.25D, 0.02D);
            }
        }
    }

    private boolean dispatcherCanStoreHere() {
        if (activeKind().family != Family.DISPATCHER) {
            return false;
        }
        float hardness = 0.0F;
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos position = blockPosition().below(depth);
            float value = level().getBlockState(position).getDestroySpeed(level(), position);
            if (value <= 0.0F) {
                return false;
            }
            hardness += value;
        }
        return hardness < 10.0F;
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
                    && candidate.getY() > serverLevel.getMinBuildHeight()) {
                serverLevel.setBlockAndUpdate(candidate,
                        ModBlocks.FOG.get().defaultBlockState());
                count--;
            }
        }
    }

    private void dissipateNexusFog() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos origin = blockPosition();
        for (BlockPos candidate : BlockPos.betweenClosed(
                origin.offset(-2, -2, -2), origin.offset(2, 2, 2))) {
            BlockState state = serverLevel.getBlockState(candidate);
            if (state.is(ModBlocks.FOG.get()) && state.getValue(FogBlock.AIR) != 2) {
                serverLevel.setBlock(candidate, state.setValue(FogBlock.AIR, 2), 3);
            }
        }
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

    private boolean summonBeckonParasites(Kind activeKind, LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel) || nearbyParasiteCount(18.0D) >= activeKind.activeCap) {
            return false;
        }
        if (activeKind.stage == 4) {
            int points = 12;
            int spawnedCount = 0;
            int availableSlots = Math.max(0, activeKind.activeCap - nearbyParasiteCount(18.0D));
            while (points >= 3 && spawnedCount < 4 && spawnedCount < availableSlots) {
                float roll = random.nextFloat();
                int stage = roll < 0.30F ? 1 : roll < 0.70F ? 2 : 3;
                int cost = stage == 3 ? 4 : 3;
                if (cost > points) {
                    stage = random.nextBoolean() ? 1 : 2;
                    cost = 3;
                }
                NexusParasiteEntity spawned = createNexus(serverLevel, Family.BECKON, stage);
                if (!spawnNexus(spawned, target, 5.0D + spawnedCount)) {
                    break;
                }
                points -= cost;
                spawnedCount++;
            }
            return spawnedCount > 0;
        }
        EntityType<? extends Mob> type = switch (activeKind.stage) {
            case 1 -> ModEntities.RUPTER.get();
            case 2 -> random.nextBoolean() ? ModEntities.RUPTER.get() : ModEntities.BUGLIN.get();
            default -> switch (random.nextInt(4)) {
                case 0 -> ModEntities.PRI_SUMMONER.get();
                case 1 -> ModEntities.PRI_LONGARMS.get();
                case 2 -> ModEntities.PRI_REEKER.get();
                default -> ModEntities.RUPTER.get();
            };
        };
        return spawnMob(type, target, 4.0D);
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
            ResourceLocation id = ResourceLocation.tryParse(storedParasiteIds.get(index));
            if (id == null || BuiltInRegistries.ENTITY_TYPE.getOptional(id).isEmpty()) {
                storedParasiteIds.remove(index);
                continue;
            }
            DeterrentParasiteEntity tentacle = ModEntities.DISPATCHERTEN.get().create(serverLevel);
            if (tentacle == null) {
                return false;
            }
            for (int attempt = 0; attempt < 6; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = 3.0D + random.nextDouble() * 2.0D;
                BlockPos requested = BlockPos.containing(target.getX() + Math.cos(angle) * distance,
                        target.getY(), target.getZ() + Math.sin(angle) * distance);
                BlockPos spawnPos = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, requested);
                tentacle.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        getYRot(), 0.0F);
                if (!serverLevel.noCollision(tentacle)) {
                    continue;
                }
                tentacle.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.MOB_SUMMONED, null);
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
            Vec3 start = position();
            Vec3 direction = new Vec3(random.nextFloat(), random.nextFloat(), random.nextFloat());
            double length = Math.max(direction.length(), 1.0E-6D);
            direction = direction.scale(1.0D / length);
            double velocity = 0.5D / (length / 4.0D + 0.1D);
            velocity *= random.nextFloat() * random.nextFloat() + 0.65F;
            Vec3 motion = new Vec3(direction.x * velocity, direction.y * velocity * activeKind().stage * 3.0D,
                    direction.z * velocity);
            ParasiteProjectileEntity bomb = ModEntities.PARASITE_PROJECTILE.get().create(level());
            if (bomb == null) {
                continue;
            }
            bomb.configure(this, ParasiteProjectileEntity.Mode.BOMB, start, start.add(motion), 1.0D,
                    damage, 1.5D, 60);
            bomb.setDeltaMovement(motion);
            bomb.setXRot(getXRot() + 20.0F);
            level().addFreshEntity(bomb);
        }
    }

    private void storeDispatcherParasite(Mob candidate, ResourceLocation id) {
        String path = id.getPath();
        int stage = activeKind().stage;
        if (path.equals("buglin")) {
            storedParasiteIds.add(Csrp.MODID + ":rupter");
            candidate.discard();
            return;
        }
        if (path.equals("movingflesh")) {
            candidate.discard();
            if (++dispatcherFlesh >= 2) {
                dispatcherFlesh -= 2;
                storedParasiteIds.add(randomPrimitiveTypeId());
            }
            return;
        }
        if (stage >= 2 && path.startsWith("sim_")) {
            storedParasiteIds.add(id.toString());
            candidate.discard();
            if (++dispatcherInfected >= 4) {
                dispatcherInfected -= 4;
                consumeStoredParasites("sim_", 4);
                storedParasiteIds.add(randomPrimitiveTypeId());
            }
            return;
        }
        if (stage >= 2 && path.startsWith("incompleteform_")) {
            storedParasiteIds.add(id.toString());
            candidate.discard();
            dispatcherCrude += path.endsWith("medium") ? 2 : 1;
            if (dispatcherCrude >= 6) {
                dispatcherCrude -= 6;
                consumeStoredParasites("incompleteform_", 6);
                storedParasiteIds.add(Csrp.MODID + ":heed");
            }
            return;
        }
        if (stage >= 3 && path.equals("rupter")) {
            storedParasiteIds.add(Csrp.MODID + ":mangler");
            candidate.discard();
            return;
        }
        if (stage >= 3 && path.equals("mangler")) {
            dispatcherMangler++;
        }
        storedParasiteIds.add(id.toString());
        candidate.discard();
    }

    private void consumeStoredParasites(String pathPrefix, int count) {
        for (int index = storedParasiteIds.size() - 1; index >= 0 && count > 0; index--) {
            ResourceLocation id = ResourceLocation.tryParse(storedParasiteIds.get(index));
            if (id != null && id.getPath().startsWith(pathPrefix)) {
                storedParasiteIds.remove(index);
                count--;
            }
        }
    }

    private String randomPrimitiveTypeId() {
        return switch (random.nextInt(7)) {
            case 0 -> Csrp.MODID + ":pri_yelloweye";
            case 1 -> Csrp.MODID + ":pri_summoner";
            case 2 -> Csrp.MODID + ":pri_bolster";
            case 3 -> Csrp.MODID + ":pri_longarms";
            case 4 -> Csrp.MODID + ":pri_arachnida";
            case 5 -> Csrp.MODID + ":pri_reeker";
            default -> Csrp.MODID + ":pri_manducater";
        };
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
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        projectile.configure(this, mode, start, target, speed, damage, radius, lifetime);
        level().addFreshEntity(projectile);
    }

    private List<NexusParasiteEntity> rootmassCystsInRange(Kind rooter) {
        int range = rooter.rootmassCystRange();
        return level().getEntitiesOfClass(NexusParasiteEntity.class,
                getBoundingBox().inflate(range + 1.0D, range, range + 1.0D),
                cyst -> cyst.isAlive() && cyst.activeKind().isRooterBall());
    }

    private void spawnRootmassCysts(int maximum) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int count = 1 + random.nextInt(Math.max(1, maximum));
        for (int index = 0; index < count; index++) {
            if (!canReserveSummon(1)) {
                return;
            }
            NexusParasiteEntity rooterBall = ModEntities.ROOTERBALL.get().create(serverLevel);
            if (rooterBall == null) {
                continue;
            }
            boolean spawned = false;
            for (int attempt = 0; attempt < 8; attempt++) {
                int offsetX = 2 + random.nextInt(4);
                int offsetZ = 2 + random.nextInt(4);
                if (random.nextBoolean()) offsetX = -offsetX;
                if (random.nextBoolean()) offsetZ = -offsetZ;
                BlockPos spawnPos = blockPosition().offset(offsetX, 0, offsetZ);
                if (!serverLevel.getBlockState(spawnPos).isAir()
                        || serverLevel.getBlockState(spawnPos.below()).isAir()
                        || !serverLevel.getEntitiesOfClass(LivingEntity.class,
                        new AABB(spawnPos).inflate(0.25D), Entity::isAlive).isEmpty()) {
                    continue;
                }
                rooterBall.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        getYRot(), 0.0F);
                if (!serverLevel.noCollision(rooterBall)) {
                    continue;
                }
                rooterBall.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.MOB_SUMMONED, null);
                if (serverLevel.addFreshEntity(rooterBall)) {
                    reserveTrackedSummon(rooterBall.getUUID(), 1);
                    spawned = true;
                    break;
                }
            }
            if (!spawned) {
                rooterBall.discard();
            }
        }
    }

    private void applyRooterSupport(int stage) {
        Kind rooter = activeKind();
        for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(rooter.rooterEffectRange),
                entity -> entity != this && entity instanceof Parasite)) {
            if (ally instanceof NexusParasiteEntity nexus
                    && (nexus.activeKind().family == Family.ROOTER || nexus.activeKind().isRooterBall())) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(ModMobEffects.PIVOT, 300, Math.max(0, stage - 1), false, false), this);
            ally.addEffect(new MobEffectInstance(ModMobEffects.PARATE, 300, Math.max(0, stage - 1), false, false), this);
            StatusEffectEvents.linkToRooter(ally, this);
        }
    }

    private void createStormVortex() {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(10.0D), this::isValidParasiteTarget)) {
            if (target.getItemBySlot(EquipmentSlot.FEET).is(ModItems.VENKROL_BOOTS)) {
                continue;
            }
            Vec3 pull = position().subtract(target.position());
            if (pull.lengthSqr() < 0.001D) {
                continue;
            }
            pull = pull.normalize().scale(0.32D);
            target.push(pull.x, 0.18D, pull.z);
        }
    }

    private void breakBlocksTowardsTarget(Kind activeKind) {
        LivingEntity target = getTarget();
        if (target == null || blockBreakCooldown > 0
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
        if (!(level() instanceof ServerLevel serverLevel) || activeKind.stage <= 0 || activeKind.stage >= 4) {
            return false;
        }
        NexusParasiteEntity next = createNexus(serverLevel, activeKind.family, activeKind.stage + 1);
        if (next == null) {
            return false;
        }
        next.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        next.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(next.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        next.setCustomName(getCustomName());
        next.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            next.setPersistenceRequired();
        }
        summonCapacityTracker.copyTo(next.summonCapacityTracker);
        serverLevel.addFreshEntity(next);
        discard();
        return true;
    }

    private boolean spawnNexus(NexusParasiteEntity spawned, LivingEntity target, double distance) {
        if (spawned == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        spawned.moveTo(target.getX() + Math.cos(angle) * distance, target.getY(),
                target.getZ() + Math.sin(angle) * distance, getYRot(), 0.0F);
        spawned.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawned.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        spawned.setTarget(target);
        serverLevel.addFreshEntity(spawned);
        return true;
    }

    private boolean spawnMob(EntityType<? extends Mob> type, LivingEntity target, double distance) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Mob spawned = type.create(serverLevel);
        if (spawned == null) {
            return false;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        spawned.moveTo(target.getX() + Math.cos(angle) * distance, target.getY(),
                target.getZ() + Math.sin(angle) * distance, getYRot(), 0.0F);
        spawned.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawned.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        spawned.setTarget(target);
        serverLevel.addFreshEntity(spawned);
        return true;
    }

    private int nearbyParasiteCount(double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                entity -> entity != this && entity instanceof Parasite).size();
    }

    private int defaultGrowthDelay() {
        return switch (activeKind().stage) {
            case 1 -> 240 + random.nextInt(61);
            case 2 -> 300 + random.nextInt(61);
            case 3 -> 600 + random.nextInt(601);
            default -> -1;
        };
    }

    private NexusParasiteEntity createNexus(ServerLevel level, Family family, int stage) {
        return switch (family) {
            case BECKON -> switch (stage) {
                case 1 -> ModEntities.BECKON_SI.get().create(level);
                case 2 -> ModEntities.BECKON_SII.get().create(level);
                case 3 -> ModEntities.BECKON_SIII.get().create(level);
                case 4 -> ModEntities.BECKON_SIV.get().create(level);
                default -> null;
            };
            case DISPATCHER -> switch (stage) {
                case 1 -> ModEntities.DISPATCHER_SI.get().create(level);
                case 2 -> ModEntities.DISPATCHER_SII.get().create(level);
                case 3 -> ModEntities.DISPATCHER_SIII.get().create(level);
                case 4 -> ModEntities.DISPATCHER_SIV.get().create(level);
                default -> null;
            };
            case ROOTER -> switch (stage) {
                case 1 -> ModEntities.ROOTER_SI.get().create(level);
                case 2 -> ModEntities.ROOTER_SII.get().create(level);
                case 3 -> ModEntities.ROOTER_SIII.get().create(level);
                case 4 -> ModEntities.ROOTER_SIV.get().create(level);
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
        BECKON_SI(Family.BECKON, 1, 25.0D, 4.0D, 2.5D, 2, 4, 10, 1.0F, 3.0D, 16,
                16.0D, 4, 4, 0.5D, 0.4F, 0, 0.0D, 0),
        BECKON_SII(Family.BECKON, 2, 60.0D, 8.0D, 6.0D, 4, 9, 12, 1.0F, 3.0D, 32,
                32.0D, 3, 4, 0.5D, 0.5F, 0, 0.0D, 0),
        BECKON_SIII(Family.BECKON, 3, 110.0D, 16.0D, 13.0D, 6, 12, 14, 1.0F, 3.0D, 64,
                48.0D, 2, 4, 0.35D, 0.6F, 0, 0.0D, 0),
        BECKON_SIV(Family.BECKON, 4, 220.0D, 25.0D, 20.0D, 8, 12, 8, 5.0F, 18.0D, 220,
                128.0D, 4, 4, 0.35D, 0.6F, 0, 0.0D, 0),
        DISPATCHER_SI(Family.DISPATCHER, 1, 33.0D, 7.0D, 3.0D, 2, 3, 0, 1.0F, 3.0D, 16,
                32.0D, 0, 0, 0.0D, 0.0F, 0, 0.0D, 0),
        DISPATCHER_SII(Family.DISPATCHER, 2, 70.0D, 14.0D, 7.0D, 4, 5, 0, 2.0F, 6.0D, 32,
                64.0D, 0, 0, 0.0D, 0.0F, 0, 0.0D, 0),
        DISPATCHER_SIII(Family.DISPATCHER, 3, 130.0D, 21.0D, 14.0D, 6, 7, 0, 3.0F, 9.0D, 64,
                96.0D, 0, 0, 0.0D, 0.0F, 0, 0.0D, 0),
        DISPATCHER_SIV(Family.DISPATCHER, 4, 250.0D, 28.0D, 22.0D, 8, 9, 0, 5.0F, 18.0D, 220,
                256.0D, 0, 0, 0.0D, 0.0F, 0, 0.0D, 0),
        ROOTER_SI(Family.ROOTER, 1, 40.0D, 7.0D, 2.5D, 2, 3, 0, 0.0F, 0.0D, 16,
                16.0D, 0, 0, 0.0D, 0.0F, 240, 20.0D, 0),
        ROOTER_SII(Family.ROOTER, 2, 80.0D, 14.0D, 6.0D, 4, 4, 0, 2.0F, 6.0D, 32,
                32.0D, 0, 0, 0.0D, 0.0F, 200, 30.0D, 2),
        ROOTER_SIII(Family.ROOTER, 3, 150.0D, 21.0D, 13.0D, 6, 5, 0, 3.0F, 9.0D, 64,
                48.0D, 0, 0, 0.0D, 0.0F, 160, 40.0D, 3),
        ROOTER_SIV(Family.ROOTER, 4, 300.0D, 28.0D, 20.0D, 8, 6, 0, 5.0F, 18.0D, 220,
                128.0D, 0, 0, 0.0D, 0.0F, 120, 56.0D, 5),
        ROOTERBALL(Family.ROOTERBALL, 0, 20.0D, 10.0D, 0.0D, 0, 0, 0, 0.0F, 0.0D, 0,
                0.0D, 0, 0, 0.0D, 0.0F, 0, 0.0D, 0);

        private final Family family;
        private final int stage;
        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final int bombCount;
        private final int activeCap;
        private final int summonCooldownSeconds;
        private final float maxBlockHardness;
        private final double blockRange;
        private final int experience;
        private final double followRange;
        private final int waveLimit;
        private final int collectiveMinimum;
        private final double collectiveExtra;
        private final float maxBody;
        private final int supportCooldownTicks;
        private final double rooterEffectRange;
        private final int configuredRooterBlockRange;

        Kind(Family family, int stage, double maxHealth, double armor, double attackDamage, int bombCount,
             int activeCap, int summonCooldown, float maxBlockHardness, double blockRange, int experience,
             double followRange, int waveLimit, int collectiveMinimum, double collectiveExtra, float maxBody,
             int supportCooldownTicks, double rooterEffectRange, int rooterBlockRange) {
            this.family = family;
            this.stage = stage;
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.bombCount = bombCount;
            this.activeCap = activeCap;
            this.summonCooldownSeconds = summonCooldown;
            this.maxBlockHardness = maxBlockHardness;
            this.blockRange = blockRange;
            this.experience = experience;
            this.followRange = followRange;
            this.waveLimit = waveLimit;
            this.collectiveMinimum = collectiveMinimum;
            this.collectiveExtra = collectiveExtra;
            this.maxBody = maxBody;
            this.supportCooldownTicks = supportCooldownTicks;
            this.rooterEffectRange = rooterEffectRange;
            this.configuredRooterBlockRange = rooterBlockRange;
        }

        private int summonCooldownTicks() {
            return summonCooldownSeconds * 20;
        }

        private boolean isRooterBall() {
            return family == Family.ROOTERBALL;
        }

        public int stage() {
            return stage;
        }

        private int rooterBlockRange() {
            return configuredRooterBlockRange;
        }

        private int rootmassCystRange() {
            return switch (this) {
                case ROOTER_SI -> 16;
                case ROOTER_SII -> 32;
                case ROOTER_SIII -> 48;
                case ROOTER_SIV -> 128;
                default -> 0;
            };
        }

        private int rootmassCystSpawnLimit() {
            return switch (this) {
                case ROOTER_SI -> 3;
                case ROOTER_SII -> 4;
                case ROOTER_SIII -> 5;
                case ROOTER_SIV -> 6;
                default -> 0;
            };
        }
    }
}
