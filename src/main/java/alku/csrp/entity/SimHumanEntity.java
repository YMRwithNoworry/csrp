package alku.csrp.entity;

import alku.csrp.config.MobsConfig;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.event.ParasiteCombatRules;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.level.Level;
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
public final class SimHumanEntity extends Monster implements CitadelAnimatedEntity, Parasite, MeltableAssimilated, SelfeFuseOwner {

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

    // 同步数据访问器
    private static final EntityDataAccessor<Integer> ANIMATION_STATE = SynchedEntityData.defineId(
            SimHumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MELTING = SynchedEntityData.defineId(
            SimHumanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MELT_TICKS = SynchedEntityData.defineId(
            SimHumanEntity.class, EntityDataSerializers.INT);

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

    public SimHumanEntity(EntityType<? extends SimHumanEntity> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Legacy SRPConfigMobs.infhuman* per-mob multipliers (default 1.0F in the original).
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D * MobsConfig.infhumanHealthMultiplier())
                .add(Attributes.ARMOR, 6.0D * MobsConfig.infhumanArmorMultiplier())
                .add(Attributes.ATTACK_DAMAGE, 12.0D * MobsConfig.infhumanDamageMultiplier())
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE,
                        Math.min(1.0D, 0.2D * MobsConfig.infhumanKnockbackMultiplier()))
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** Legacy SELFE self-destruct fuse, shared with every other parasite family. */
    private final ParasiteFuseState selfeFuse = new ParasiteFuseState();

    @Override
    public boolean willExplodeOnDeath() {
        return selfeFuse.willExplodeOnDeath(this);
    }

    @Override
    public void startDyingFuse() {
        selfeFuse.start(this);
    }

    @Override
    public boolean isDyingFuseActive() {
        return selfeFuse.isActive(this);
    }

    @Override
    public float getSelfeFlashIntensity(float partialTick) {
        return selfeFuse.flashIntensity(this, partialTick);
    }

    /** Legacy onDeathUpdate: hold the corpse while the fuse burns, then burst. */
    @Override
    protected void tickDeath() {
        if (!selfeFuse.isActive(this)) {
            super.tickDeath();
            return;
        }
        if (deathTime < ParasiteFuseState.DEATH_ANIMATION_TICKS) {
            deathTime++;
        }
        if (level().isClientSide) {
            return;
        }
        if (selfeFuse.advance(this)) {
            if (level() instanceof ServerLevel serverLevel) {
                ParasiteCombatRules.selfExplode(serverLevel, this);
            }
            selfeFuse.clear(this);
            super.tickDeath();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ParasiteFuseState.SELFE, -1);
        builder.define(ANIMATION_STATE, STATE_NORMAL);
        builder.define(MELTING, false);
        builder.define(MELT_TICKS, 0);
    }

    /** Legacy geneSpecialmove (applyGene): gates the leap granted by the generation. */
    private boolean generationAllowsSpecialMoves() {
        return level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).specialMoves();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Legacy EntityInfHuman:117: tasks.addTask(0, EntityAISwimmingDiving(this, 0.08)).
        goalSelector.addGoal(0, new SwimmingDivingGoal(this, 0.08D));
        // Legacy geneSpecialmove: the generation decides whether special moves exist at all.
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.4F) {
            @Override
            public boolean canUse() {
                return generationAllowsSpecialMoves() && super.canUse();
            }
        });
        // Legacy EntityInfHuman:119: tasks.addTask(2, EntityAIWaterLeapAtTargetStatus(this, 0.7F, 1.5, 3, 20, 0)).
        goalSelector.addGoal(2, new WaterLeapAtTargetGoal(this, () -> level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).waterLeap(), 0.7F, 1.5D, 20, 0.0D));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        // Legacy EntityInfHuman:122 registers the recruit task at priority 5 (range 16).
        goalSelector.addGoal(5, new RecruitFollowersGoal(this, 16));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
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

        if (level().isClientSide) {
            return;
        }
        if (isMelting()) {
            AssimilatedMeltSystem.freeze(this);
            tickMelting();
            return;
        }

        // 更新动画状态
        updateCitadelAnimationState();

        // 定期感染附近生物
        if (tickCount % COTH_AURA_INTERVAL_TICKS == 0) {
            infectNearby();
            if (AssimilatedMeltSystem.tryStartGroup(this, parasiteKills)) {
                parasiteKills = 0;
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
    public boolean doHurtTarget(Entity target) {
        LivingEntity livingTarget = target instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(target);
        if (hit && !level().isClientSide) {
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
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            selfeFuse.willExplodeOnDeath(this);
        }
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        if (victim instanceof AbstractSkeleton && ++skeletonKills >= HOST_SKELETON_KILLS) {
            transformToHost(level);
            return super.killedEntity(level, victim);
        }
        parasiteKills++;
        if (AssimilatedMeltSystem.tryStartGroup(this, parasiteKills)) {
            parasiteKills = 0;
        } else if (parasiteKills > AssimilatedParasiteEntity.FERAL_KILL_THRESHOLD) {
            transformToFeral(level);
        }
        return super.killedEntity(level, victim);
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("animation_state", getAnimationState());
        tag.putInt("parasite_kills", parasiteKills);
        tag.putInt("skeleton_kills", skeletonKills);
        tag.putBoolean("melting", isMelting());
        tag.putInt("melt_ticks", entityData.get(MELT_TICKS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setAnimationState(tag.getInt("animation_state"));
        parasiteKills = tag.getInt("parasite_kills");
        skeletonKills = tag.getInt("skeleton_kills");
        entityData.set(MELTING, tag.getBoolean("melting"));
        entityData.set(MELT_TICKS, tag.getInt("melt_ticks"));
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
        FeralParasiteEntity feral = ModEntities.FER_HUMAN.get().create(level);
        if (feral == null) {
            return;
        }
        feral.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
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
        HostEntity host = ModEntities.HOST.get().create(level);
        if (host == null) {
            return;
        }
        host.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        host.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
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
