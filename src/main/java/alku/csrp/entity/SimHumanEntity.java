package alku.csrp.entity;

import alku.csrp.config.RuntimeToggles;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import alku.csrp.animation.CitadelAnimatedEntity;
import alku.csrp.animation.CitadelAnimationCache;
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelPlayState;
import alku.csrp.animation.CitadelRawAnimation;
import alku.csrp.animation.CitadelAnimationUtil;

/**
 * Assimilated Human animation states mirror ModelInfHuman.
 */
public final class SimHumanEntity extends Monster implements CitadelAnimatedEntity, Parasite, MeltableAssimilated, AssimilationSpawnGate {

    // 动画状态常量
    public static final int STATE_NORMAL = 0;
    public static final int STATE_ATTACK = 1;
    public static final int STATE_PURSUIT = 2;

    private static final int COTH_AURA_INTERVAL_TICKS = 20;
    private static final double COTH_AURA_RADIUS = 3.0D;
    private static final int MELT_DURATION_TICKS = 127;
    private static final float BASE_HEIGHT = 1.95F;
    private static final float MELT_MIN_HEIGHT = 0.7F;
    private static final float BLEED_CHANCE = 0.2F;
    private static final int HOST_SKELETON_KILLS = 5;

    /** Original EntityInfHuman {@code getIDSpawn()} (register id 6 = sim_human). */
    @Override
    public String assimilationSpawnKey() {
        return "sim_human";
    }

    // 同步数据访问器
    private static final EntityDataAccessor<Integer> ANIMATION_STATE = SynchedEntityData.defineId(
            SimHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MELTING = SynchedEntityData.defineId(
            SimHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MELT_TICKS = SynchedEntityData.defineId(
            SimHumanEntity.class, EntityDataSerializers.INT);
    /** Original EntityInfHuman skin id 111 — the "Sound Eater" variant. */
    private static final EntityDataAccessor<Boolean> SOUND_EATER = SynchedEntityData.defineId(
            SimHumanEntity.class, EntityDataSerializers.BOOLEAN);

    /** out109 EntityInfHuman:627 — 1% of natural variants become the Sound Eater. */
    private static final float SOUND_EATER_CHANCE = 0.01F;
    private static final int SOUND_EATER_SKIN = 111;
    private static final double SOUND_EATER_FOLLOW_RANGE = 12.0D;
    private static final double SOUND_EATER_MOVEMENT_SPEED = 0.32D;
    /** out109 EntityInfHuman:148 — the hearing box is inflated 16 x 4 x 16. */
    private static final double HEARING_RANGE_HORIZONTAL = 16.0D;
    private static final double HEARING_RANGE_VERTICAL = 4.0D;
    /** Sprinting players are three times as loud (out109 EntityInfHuman:164-166). */
    private static final double SPRINT_LOUDNESS_MULTIPLIER = 3.0D;
    private static final int MOVEMENT_SOUND_MEMORY_TICKS = 60;
    /** out109 SoundEaterBlockSoundHandler: block break radius 16 / life 100, place radius 12 / life 80. */
    static final double BLOCK_BREAK_SOUND_RADIUS = 16.0D;
    static final int BLOCK_BREAK_SOUND_LIFE_TICKS = 100;
    static final double BLOCK_PLACE_SOUND_RADIUS = 12.0D;
    static final int BLOCK_PLACE_SOUND_LIFE_TICKS = 80;

    private static final int STILL_ANIMATION_DELAY_TICKS = 25;
    private final CitadelRawAnimation AGE = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final CitadelRawAnimation LIMB = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final CitadelRawAnimation AGE_STILL = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_still_ani_1");
    private final CitadelRawAnimation AGE_STATUS_1 = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final CitadelRawAnimation LIMB_STATUS_1 = ParasiteAnimations.loop(
            this, "func_78087_a.limb_swing.get_parasite_status_1");
    private final CitadelRawAnimation AGE_STATUS_1_STILL = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final CitadelRawAnimation AGE_STATUS_2 = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_2");
    private final CitadelRawAnimation LIMB_STATUS_2 = ParasiteAnimations.loop(
            this, "func_78087_a.limb_swing.get_parasite_status_2");

    private final CitadelAnimationCache animationCache = CitadelAnimationUtil.createInstanceCache(this);
    private int stillAnimationTicks;
    private int parasiteKills;
    private int skeletonKills;
    private BlockPos lastHeardSoundPos;
    private int soundMemoryTicks;

    public SimHumanEntity(EntityType<? extends SimHumanEntity> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIMATION_STATE, STATE_NORMAL);
        builder.define(MELTING, false);
        builder.define(MELT_TICKS, 0);
        builder.define(SOUND_EATER, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, (target, level) -> this.isValidParasiteTarget(target)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getAnimationState() == STATE_NORMAL
                ? ParasiteSoundProfiles.ambient(this) : ModSounds.get("mob.silence");
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
    public void tick() {
        if (isMelting()) {
            AssimilatedMeltSystem.freeze(this);
        }
        super.tick();

        if (ParasiteAnimations.isMoving(this, true)) {
            stillAnimationTicks = 0;
        } else {
            stillAnimationTicks++;
        }

        if (level().isClientSide()) {
            return;
        }
        if (isMelting()) {
            AssimilatedMeltSystem.freeze(this);
            tickMelting();
            return;
        }

        // 更新动画状态
        updateCitadelAnimationState();

        // Sound Eater (skin 111) hunts by noise instead of sight.
        if (isSoundEater()) {
            tickSoundEater();
        }

        // 定期感染附近生物
        if (tickCount % COTH_AURA_INTERVAL_TICKS == 0) {
            infectNearby();
            if (AssimilatedMeltSystem.tryStartGroup(this, parasiteKills)) {
                parasiteKills = 0;
            }
        }
    }

    /** Original {@code EntityInfHuman.getSkin() == 111}. */
    public boolean isSoundEater() {
        return entityData.get(SOUND_EATER);
    }

    public void setSoundEater(boolean soundEater) {
        entityData.set(SOUND_EATER, soundEater);
        if (soundEater) {
            var followRange = getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null) {
                followRange.setBaseValue(SOUND_EATER_FOLLOW_RANGE);
            }
            var movementSpeed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                movementSpeed.setBaseValue(SOUND_EATER_MOVEMENT_SPEED);
            }
        }
    }

    /** Original {@code EntityInfHuman.func_180482_a} rolled skin 111 with a 1% chance. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnGroupData);
        if (random.nextFloat() < SOUND_EATER_CHANCE) {
            setSoundEater(true);
        }
        return data;
    }

    /**
     * Original {@code EntityInfHuman:151-188}: tick the sound memory, otherwise listen for the
     * loudest nearby player (walk distance this tick, tripled while sprinting) and chase that noise.
     */
    private void tickSoundEater() {
        tickSoundMemory();
        if (getHeardSoundPos() == null) {
            Player loudest = null;
            double loudestLoudness = 0.0D;
            for (Player player : level().getEntitiesOfClass(Player.class,
                    getBoundingBox().inflate(HEARING_RANGE_HORIZONTAL, HEARING_RANGE_VERTICAL,
                            HEARING_RANGE_HORIZONTAL),
                    candidate -> !candidate.isSpectator() && !candidate.isCreative() && candidate.isAlive())) {
                // 26.3 no longer exposes the old walkDist/walkDistO pair server side; the horizontal
                // movement of this tick is the same quantity the original compared against 0.01.
                double walkedThisTick = player.getDeltaMovement().horizontalDistance();
                boolean moving = walkedThisTick > 0.01D || player.isSprinting();
                if (!moving) {
                    continue;
                }
                double loudness = walkedThisTick;
                if (player.isSprinting()) {
                    loudness *= SPRINT_LOUDNESS_MULTIPLIER;
                }
                if (loudness > loudestLoudness) {
                    loudestLoudness = loudness;
                    loudest = player;
                }
            }
            if (loudest != null) {
                notifyHeardSound(loudest.blockPosition(), MOVEMENT_SOUND_MEMORY_TICKS);
                setTarget(loudest);
            }
        }
        if (getHeardSoundPos() == null && getLastHurtByMob() == null && getTarget() != null) {
            setTarget(null);
        }
    }

    /** Original {@code EntityInfHuman.notifyHeardSound}. */
    public void notifyHeardSound(BlockPos pos, int lifeTicks) {
        if (!isSoundEater()) {
            return;
        }
        lastHeardSoundPos = pos;
        soundMemoryTicks = lifeTicks;
    }

    /** Original {@code EntityInfHuman.getHeardSoundPos}. */
    public BlockPos getHeardSoundPos() {
        return lastHeardSoundPos;
    }

    /** Original {@code EntityInfHuman.tickSoundMemory}. */
    public void tickSoundMemory() {
        if (soundMemoryTicks > 0 && --soundMemoryTicks <= 0) {
            lastHeardSoundPos = null;
        }
    }

    /** Original {@code EntityInfHuman.clearHeardSound}. */
    public void clearHeardSound() {
        lastHeardSoundPos = null;
        soundMemoryTicks = 0;
    }

    /**
     * Original {@code SoundEaterSoundHelper.broadcastSound}: every Sound Eater inside {@code radius}
     * of {@code pos} remembers the noise for {@code lifeTicks}.
     */
    public static void broadcastSound(ServerLevel level, BlockPos pos, double radius, int lifeTicks) {
        for (SimHumanEntity human : level.getEntitiesOfClass(SimHumanEntity.class,
                new AABB(pos).inflate(radius))) {
            if (human.isSoundEater()) {
                human.notifyHeardSound(pos, lifeTicks);
            }
        }
    }

    /**
     * 根据实体当前状态更新动画状态
     */
    private void updateCitadelAnimationState() {
        LivingEntity target = getTarget();

        if (target == null || !target.isAlive()) {
            setAnimationState(STATE_NORMAL);
            return;
        }
        double reach = getBbWidth() * 2.0D;
        setAnimationState(distanceToSqr(target) > reach * reach + target.getBbWidth()
                ? STATE_PURSUIT : STATE_ATTACK);
    }

    /**
     * 设置动画状态
     */
    public void setAnimationState(int state) {
        int clampedState = Math.clamp(state, STATE_NORMAL, STATE_PURSUIT);
        if (getAnimationState() != clampedState) {
            entityData.set(ANIMATION_STATE, clampedState);
        }
    }

    /**
     * 获取当前动画状态
     */
    public int getAnimationState() {
        return entityData.get(ANIMATION_STATE);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        LivingEntity livingTarget = target instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && !level().isClientSide()) {
            if (livingTarget != null) {
                ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
                InfectionMechanics.applyCoth(livingTarget, this);
                if (random.nextFloat() < BLEED_CHANCE) {
                    livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            ModMobEffects.BLEED, 100, 0), this);
                }
            }
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim, DamageSource source) {
        if (victim instanceof AbstractSkeleton && ++skeletonKills >= HOST_SKELETON_KILLS) {
            transformToHost(level);
            return super.killedEntity(level, victim, source);
        }
        parasiteKills++;
        if (AssimilatedMeltSystem.tryStartGroup(this, parasiteKills)) {
            parasiteKills = 0;
        // Original EntityPInfected gated the sim -> feral upgrade behind ParasiteEventEntity.canSpawnNext.
        } else if (parasiteKills > AssimilatedParasiteEntity.FERAL_KILL_THRESHOLD && RuntimeToggles.mobEvolution()) {
            transformToFeral(level);
        }
        return super.killedEntity(level, victim, source);
    }

    /**
     * 感染附近的生物
     */
    private void infectNearby() {
        for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(COTH_AURA_RADIUS), this::isValidParasiteTarget)) {
            if (hasLineOfSight(nearby)) {
                InfectionMechanics.applyCoth(nearby, this);
            }
        }
    }

    /**
     * 判断是否为有效的寄生目标
     */
    private boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("animation_state", getAnimationState());
        output.putInt("parasite_kills", parasiteKills);
        output.putInt("skeleton_kills", skeletonKills);
        output.putBoolean("melting", isMelting());
        output.putInt("melt_ticks", entityData.get(MELT_TICKS));
        output.putBoolean("sound_eater", isSoundEater());
        output.putInt("sound_memory_ticks", soundMemoryTicks);
        if (lastHeardSoundPos != null) {
            output.putLong("heard_sound_pos", lastHeardSoundPos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setAnimationState(input.getIntOr("animation_state", 0));
        parasiteKills = input.getIntOr("parasite_kills", 0);
        skeletonKills = input.getIntOr("skeleton_kills", 0);
        entityData.set(MELTING, input.getBooleanOr("melting", false));
        entityData.set(MELT_TICKS, input.getIntOr("melt_ticks", 0));
        setSoundEater(input.getBooleanOr("sound_eater", false));
        soundMemoryTicks = input.getIntOr("sound_memory_ticks", 0);
        lastHeardSoundPos = input.getLong("heard_sound_pos").isPresent()
                ? BlockPos.of(input.getLongOr("heard_sound_pos", 0L)) : null;
    }

    @Override
    public boolean canMelt() {
        return !isMelting();
    }

    @Override
    public boolean isMelting() {
        return entityData.get(MELTING);
    }

    @Override
    public void melt() {
        if (!canMelt()) {
            return;
        }
        entityData.set(MELTING, true);
        entityData.set(MELT_TICKS, 0);
        AssimilatedMeltSystem.freeze(this);
        refreshDimensions();
    }

    @Override
    public float getMeltRenderScale(float partialTick) {
        if (!isMelting()) {
            return 1.0F;
        }
        return Math.max(0.01F, 1.0F - (entityData.get(MELT_TICKS) + partialTick) * 0.005F);
    }

    public float getMeltHeight() {
        return isMelting()
                ? Math.max(MELT_MIN_HEIGHT, BASE_HEIGHT - entityData.get(MELT_TICKS) * 0.01F)
                : BASE_HEIGHT;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDefaultDimensions(pose);
        return isMelting() ? dimensions.scale(1.0F, getMeltHeight() / BASE_HEIGHT) : dimensions;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == MELTING || accessor == MELT_TICKS) {
            refreshDimensions();
        }
    }

    @Override
    public void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers) {
        controllers.add(new CitadelAnimationController<>(this, "age_controller", 0,
                state -> state.setAndContinue(ageAnimation())));
        controllers.add(new CitadelAnimationController<>(this, "movement_controller", 4, state -> {
            if (isMelting() || !ParasiteAnimations.isMoving(this, state.isMoving())) {
                return CitadelPlayState.STOP;
            }
            return state.setAndContinue(switch (getAnimationState()) {
                case STATE_ATTACK -> LIMB_STATUS_1;
                case STATE_PURSUIT -> LIMB_STATUS_2;
                default -> LIMB;
            });
        }));
    }

    private CitadelRawAnimation ageAnimation() {
        boolean still = stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS;
        if (isMelting()) {
            return AGE_STILL;
        }
        return switch (getAnimationState()) {
            case STATE_ATTACK -> still ? AGE_STATUS_1_STILL : AGE_STATUS_1;
            case STATE_PURSUIT -> AGE_STATUS_2;
            default -> still ? AGE_STILL : AGE;
        };
    }

    @Override
    public CitadelAnimationCache getCitadelAnimationCache() {
        return animationCache;
    }

    private void tickMelting() {
        int ticks = entityData.get(MELT_TICKS) + 1;
        entityData.set(MELT_TICKS, ticks);
        if (ticks % 20 == 0) {
            playSound(ModSounds.SIM_ADVENTURER_MELT.get(), 1.0F, 1.0F);
        }
        if (level() instanceof ServerLevel serverLevel) {
            AssimilatedMeltSystem.sendMeltParticles(serverLevel, this);
        }
        if (getMeltHeight() > MELT_MIN_HEIGHT && ticks < MELT_DURATION_TICKS) {
            return;
        }
        AssimilatedMeltSystem.spawnMovingFlesh(this, 1);
    }

    private void transformToFeral(ServerLevel level) {
        FeralParasiteEntity feral = ModEntities.FER_HUMAN.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (feral == null) {
            return;
        }
        feral.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
        feral.setTarget(getTarget());
        feral.setCustomName(getCustomName());
        feral.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            feral.setPersistenceRequired();
        }
        if (level.addFreshEntity(feral)) {
            discard();
        }
    }

    private void transformToHost(ServerLevel level) {
        HostEntity host = ModEntities.HOST.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (host == null) {
            return;
        }
        host.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
        host.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()),
                EntitySpawnReason.MOB_SUMMONED, null);
        host.setCustomName(getCustomName());
        host.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            host.setPersistenceRequired();
        }
        if (level.addFreshEntity(host)) {
            AssimilatedMeltSystem.sendMeltParticles(level, host);
            discard();
        }
    }
}
