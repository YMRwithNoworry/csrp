package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Shared implementation for the legacy preeminent parasites. This tier
 * uses stronger adaptation and delegates its battlefield support to Flams.
 */
public final class PreeminentParasiteEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Boolean> CARRIER_VARIANT =
            SynchedEntityData.defineId(PreeminentParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAUNTER_VARIANT =
            SynchedEntityData.defineId(PreeminentParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_ADAPTATION_HITS = 5;
    private static final int MAX_LEARNABLE_DAMAGE_SOURCES = 20;
    private static final int MAX_SUMMONED_FLAMS = 3;
    private static final int FLAM_SUMMON_TIMER_MAX = 80;
    private static final int FLAM_SUMMON_PHASE = 40;
    private static final int FLAM_SUMMON_SUCCESS_REWIND = 100;
    private static final byte BIOMASS_EVENT = 8;
    private static final int STEALTH_CHECK_INTERVAL = 20;
    private static final int STEALTH_CHECK_OFFSET = 10;
    private static final int STEALTH_CHECKS_REQUIRED = 3;
    private static final int STEALTH_EFFECT_TICKS = 25;
    private static final double STEALTH_HEALTH_THRESHOLD = 0.40D;
    private static final int MINIMUM_FLIGHT_HEIGHT = 10;
    private static final int MAXIMUM_FLIGHT_HEIGHT = 30;
    private static final int CARRIER_BUFF_INITIAL_DELAY_TICKS = 60;
    private static final int CARRIER_BUFF_COOLDOWN_TICKS = 600;
    private static final double CARRIER_BUFF_RANGE = 60.0D;
    private static final int CARRIER_RECRUIT_RANGE = 16;
    private static final int CARRIER_MELEE_INTERVAL_TICKS = 10;
    private static final int CARRIER_WATER_LEAP_COOLDOWN_TICKS = 20;
    private static final int HAUNTER_BLOCK_BREAK_INTERVAL_TICKS = 60;
    private static final double HAUNTER_BLOCK_BREAK_MAX_DISTANCE_SQR = 64.0D * 64.0D;
    private static final float HAUNTER_BLOCK_BREAK_HARDNESS = 15.0F;
    private static final int HAUNTER_BLOCK_BREAK_RANGE = 5;
    private static final Set<String> HAUNTER_BLOCK_BREAK_BLACKLIST = Set.of(
            "csrp:biome_heart", "csrp:colony_heart", "csrp:parasite_rubble_dense",
            "csrp:parasite_canister_active", "srparasites:biomeheart", "srparasites:colonyheart",
            "srparasites:parasiterubbledense", "srparasites:parasitecanisteractive");
    private static final float ADAPTATION_PER_HIT = 0.20F;
    private static final float ADAPTATION_LEARN_CHANCE = 1.0F;
    private static final float FIRE_SUPPRESSION_CHANCE = 0.30F;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation FLY = ParasiteAnimations.loop(this, "fly");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    private final Kind kind;
    private final CarrierHeadPart carrierHeadPart;
    private final HaunterBodyPart haunterHeadPart;
    private final HaunterBodyPart haunterMiddlePart;
    private final PartEntity<?>[] parts;
    private int blockBreakCooldown;
    private int haunterBlockBreakCooldown;
    private int supportCooldown;
    private int stealthChecks;
    private int attackAnimationTicks;
    private int wraithProjectileCount;
    private boolean stealthActive;
    private boolean charging;
    private boolean haunterMeleeMode;

    public PreeminentParasiteEntity(EntityType<? extends PreeminentParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        carrierHeadPart = kind == Kind.CARRIER_COLONY ? new CarrierHeadPart(this) : null;
        haunterHeadPart = kind == Kind.HAUNTER ? new HaunterBodyPart(this, "haunter_head", 2.0F, 7.3F) : null;
        haunterMiddlePart = kind == Kind.HAUNTER ? new HaunterBodyPart(this, "haunter_middle", 1.9F, 3.5F) : null;
        parts = carrierHeadPart != null ? new PartEntity<?>[]{carrierHeadPart}
                : haunterHeadPart != null ? new PartEntity<?>[]{haunterHeadPart, haunterMiddlePart}
                : new PartEntity<?>[0];
        xpReward = 75;
        if (kind.flying) {
            moveControl = kind == Kind.BOGLE || kind == Kind.WRAITH
                    ? new PreeminentFlyingMoveControl(this)
                    : new FlyingMoveControl(this, 18, true);
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
        if (kind == Kind.CARRIER_COLONY || kind == Kind.HAUNTER) {
            attributes.add(Attributes.STEP_HEIGHT, 1.0D);
        }
        return attributes;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CARRIER_VARIANT, false);
        builder.define(HAUNTER_VARIANT, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        switch (activeKind()) {
            case BOGLE -> {
                goalSelector.addGoal(4, new PreeminentChargeAttackGoal());
                goalSelector.addGoal(5, new LegacyProjectileAttackGoal(60, 30, 3));
                goalSelector.addGoal(6, new PreeminentRandomFlightGoal());
            }
            case CARRIER_COLONY -> {
                goalSelector.addGoal(0, new CarrierSwimmingGoal());
                goalSelector.addGoal(2, new CarrierWaterLeapGoal());
                goalSelector.addGoal(3, new CarrierMeleeGoal());
                goalSelector.addGoal(3, new CarrierBuffGoal());
                goalSelector.addGoal(6, new CarrierRecruitGoal());
            }
            case HAUNTER -> {
                goalSelector.addGoal(0, new HaunterSwimmingDivingGoal());
                goalSelector.addGoal(2, new HaunterMeleeAoeGoal());
                goalSelector.addGoal(2, new HaunterEvadeDashGoal());
                goalSelector.addGoal(3, new HaunterTargetMaintenanceGoal());
                goalSelector.addGoal(4, new HaunterRangedPositionGoal());
                goalSelector.addGoal(6, new HaunterMeleeRangeSwitchGoal());
                goalSelector.addGoal(6, new HaunterHomingBurstGoal());
                goalSelector.addGoal(9, new HaunterBlockBreakGoal());
            }
            case BOMBER_HEAVY -> {
                goalSelector.addGoal(1, new HeavyBomberBombGoal());
                goalSelector.addGoal(2, new FlightPursuitGoal(0.85D));
            }
            case WRAITH -> {
                goalSelector.addGoal(4, new PreeminentChargeAttackGoal());
                goalSelector.addGoal(5, new LegacyProjectileAttackGoal(20, 10, 4));
                goalSelector.addGoal(6, new PreeminentRandomFlightGoal());
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        Kind activeKind = activeKind();
        if (carrierHeadPart != null) {
            updateCarrierHeadPart();
        }
        if (haunterHeadPart != null) {
            updateHaunterBodyParts();
        }
        if (activeKind.flying) {
            setNoGravity(true);
        }
        if (level().isClientSide) {
            return;
        }
        if (blockBreakCooldown > 0) {
            blockBreakCooldown--;
        }
        supportCooldown++;
        if (supportCooldown > FLAM_SUMMON_TIMER_MAX) {
            supportCooldown = 0;
        }
        if (attackAnimationTicks > 0) {
            attackAnimationTicks--;
        }
        if (activeKind == Kind.CARRIER_COLONY && tickCount == 25) {
            applyCarrierInitialLinks();
        }
        if (activeKind.flying && !isStealthKind() && onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.50D);
        }
        LivingEntity target = getTarget();
        if (activeKind.flying) {
            applyFlightLimits(target);
        }
        if (isStealthKind()) {
            if (Math.floorMod(tickCount, STEALTH_CHECK_INTERVAL) == STEALTH_CHECK_OFFSET) {
                if (target != null && (!level().isEmptyBlock(blockPosition().below())
                        || !level().isEmptyBlock(blockPosition().below(2)))) {
                    Vec3 movement = getDeltaMovement();
                    setDeltaMovement(movement.x, 0.5D, movement.z);
                }
                updateStealth();
            }
        }

        if (target == null || !target.isAlive()) {
            return;
        }
        if (activeKind != Kind.HAUNTER) {
            breakBlocksTowardsTarget(target, activeKind);
        }
        if (supportCooldown == FLAM_SUMMON_PHASE && trySummonFlam(target)) {
            supportCooldown -= FLAM_SUMMON_SUCCESS_REWIND;
        }
        if ((activeKind == Kind.BOGLE || activeKind == Kind.WRAITH)
                && Math.floorMod(tickCount, STEALTH_CHECK_INTERVAL) == STEALTH_CHECK_OFFSET) {
            applyFlyingAura();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && isStealthKind()) {
            revealStealth();
        }
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    protected int incomingDamageCapDivisor() {
        return 18;
    }

    @Override
    public boolean applyScaryOrbEffect(LivingEntity target, int nearbyEntities) {
        boolean applied = super.applyScaryOrbEffect(target, nearbyEntities);
        if (!applied || (activeKind() != Kind.CARRIER_COLONY && activeKind() != Kind.HAUNTER)
                || target instanceof Parasite) {
            return applied;
        }
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 4, false, false), this);
        target.addEffect(new MobEffectInstance(ModMobEffects.NEEDLER, 2400, 4, false, false), this);
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 300, 4, false, false), this);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 4, false, false), this);
        return true;
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
            case BOGLE, WRAITH, BOMBER_HEAVY -> 0.95F;
            default -> 1.0F;
        };
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hurt = super.doHurtTarget(entity);
        if (hurt) {
            attackAnimationTicks = 8;
            triggerAnim("attack_controller", "attack");
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return activeKind() == Kind.HAUNTER || super.fireImmune();
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (activeKind() == Kind.HAUNTER && effect.is(MobEffects.POISON)) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public int getMaxFallDistance() {
        return activeKind() == Kind.HAUNTER ? 3 : super.getMaxFallDistance();
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase()) {
            if (activeKind() == Kind.CARRIER_COLONY) {
                setCarrierVariant(true);
            } else if (activeKind() == Kind.HAUNTER) {
                setHaunterVariant(true);
                setHealth(getMaxHealth());
            }
        }
        return data;
    }

    public boolean isCarrierVariant() {
        return activeKind() == Kind.CARRIER_COLONY && entityData.get(CARRIER_VARIANT);
    }

    private void setCarrierVariant(boolean variant) {
        entityData.set(CARRIER_VARIANT, variant);
        if (!variant) {
            return;
        }
        var armor = getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(Kind.CARRIER_COLONY.armor * 1.5D);
        }
        var movementSpeed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(0.1694D);
        }
    }

    public boolean isHaunterVariant() {
        return activeKind() == Kind.HAUNTER && entityData.get(HAUNTER_VARIANT);
    }

    private void setHaunterVariant(boolean variant) {
        entityData.set(HAUNTER_VARIANT, variant);
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(variant ? Kind.HAUNTER.maxHealth * 0.5D : Kind.HAUNTER.maxHealth);
        }
        var attackDamage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(variant ? Kind.HAUNTER.attackDamage * 1.5D : Kind.HAUNTER.attackDamage);
        }
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDefaultDimensions(pose);
        if (activeKind() == Kind.CARRIER_COLONY) {
            return dimensions.withEyeHeight(1.5F);
        }
        return activeKind() == Kind.HAUNTER ? dimensions.withEyeHeight(4.7F) : dimensions;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (activeKind() == Kind.HAUNTER) {
            return null;
        }
        return activeKind() == Kind.CARRIER_COLONY ? ModSounds.CARRIER_COLONY_LIVING.get()
                : super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (activeKind() == Kind.HAUNTER) {
            return null;
        }
        return activeKind() == Kind.CARRIER_COLONY ? ModSounds.CARRIER_COLONY_HURT.get()
                : super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        if (activeKind() == Kind.HAUNTER) {
            return null;
        }
        return activeKind() == Kind.CARRIER_COLONY ? ModSounds.CARRIER_COLONY_DEATH.get()
                : super.getDeathSound();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (activeKind() == Kind.CARRIER_COLONY) {
            playSound(ModSounds.HEAVY_MULTIPLE_STEP.get(), 0.15F, 1.0F);
        } else {
            super.playStepSound(pos, state);
        }
    }

    @Override
    protected float getSoundVolume() {
        return activeKind() == Kind.CARRIER_COLONY || activeKind() == Kind.HAUNTER
                ? 5.0F : super.getSoundVolume();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("preeminent_carrier_variant", isCarrierVariant());
        tag.putBoolean("preeminent_haunter_variant", isHaunterVariant());
        tag.putInt("preeminent_support_cooldown", supportCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (activeKind() == Kind.CARRIER_COLONY) {
            setCarrierVariant(tag.getBoolean("preeminent_carrier_variant"));
        } else if (activeKind() == Kind.HAUNTER) {
            setHaunterVariant(tag.getBoolean("preeminent_haunter_variant"));
        }
        supportCooldown = Mth.clamp(tag.getInt("preeminent_support_cooldown"),
                FLAM_SUMMON_PHASE - FLAM_SUMMON_SUCCESS_REWIND, FLAM_SUMMON_TIMER_MAX);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    public Kind getKind() {
        return activeKind();
    }

    @Override
    public boolean isMultipartEntity() {
        return parts.length > 0;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int index = 0; index < parts.length; index++) {
            parts[index].setId(id + index + 1);
        }
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public void remove(RemovalReason reason) {
        for (PartEntity<?> part : parts) {
            if (!part.isRemoved()) {
                part.remove(reason);
            }
        }
        super.remove(reason);
    }

    private void updateCarrierHeadPart() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float forward = 3.1F * Mth.cos((float) Math.PI / 18.0F);
        carrierHeadPart.setPos(getX() + Mth.sin(yaw) * forward, getY() + 1.6D,
                getZ() - Mth.cos(yaw) * forward);
        carrierHeadPart.setYRot(getYRot());
    }

    private void updateHaunterBodyParts() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float forward = 2.5F * Mth.cos((float) Math.PI / 18.0F);
        haunterHeadPart.setPos(getX() + Mth.sin(yaw) * forward, getY(),
                getZ() - Mth.cos(yaw) * forward);
        haunterHeadPart.setYRot(getYRot());
        haunterMiddlePart.setPos(getX(), getY() + 3.7D, getZ());
        haunterMiddlePart.setYRot(getYRot());
    }

    private boolean hurtCarrierHead(DamageSource source, float amount) {
        if (random.nextBoolean()) {
            addEffect(new MobEffectInstance(ModMobEffects.BLEED, 80, 0), this);
        }
        return hurt(source, amount * 3.0F);
    }

    private void applyCarrierInitialLinks() {
        for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(32.0D), entity -> entity instanceof Parasite && entity.isAlive())) {
            ally.addEffect(new MobEffectInstance(ModMobEffects.LINK, 6666, 0, false, false), this);
            ally.addEffect(new MobEffectInstance(ModMobEffects.FOSTER, 6666, 0, false, false), this);
        }
    }

    private PlayState movementAnimation(AnimationState<PreeminentParasiteEntity> state) {
        if (activeKind().flying) {
            return state.setAndContinue(FLY);
        }
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.0001 ? WALK : IDLE);
    }

    private void triggerAttackAnimation() {
        attackAnimationTicks = 10;
        triggerAnim("attack_controller", "attack");
    }

    private boolean isStealthKind() {
        return activeKind() == Kind.BOGLE || activeKind() == Kind.WRAITH;
    }

    private void revealStealth() {
        stealthActive = false;
        stealthChecks = 0;
    }

    private void updateStealth() {
        double healthRatio = getHealth() / getMaxHealth();
        if (stealthActive) {
            addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, STEALTH_EFFECT_TICKS, 1,
                    false, false), this);
            if (healthRatio < STEALTH_HEALTH_THRESHOLD) {
                stealthActive = false;
            }
        } else if (healthRatio >= STEALTH_HEALTH_THRESHOLD) {
            stealthChecks++;
            if (stealthChecks >= STEALTH_CHECKS_REQUIRED) {
                stealthActive = true;
                stealthChecks = 0;
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5D,
                            getZ(), 12, getBbWidth() * 0.4D, getBbHeight() * 0.3D,
                            getBbWidth() * 0.4D, 0.02D);
                }
            }
        }
    }

    private void applyFlyingAura() {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.0D),
                this::isValidParasiteTarget)) {
            Vec3 movement = target.getDeltaMovement();
            Vec3 away = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 0.0001D) {
                away = away.normalize().scale(2.5D);
                double vertical = target.onGround() ? 0.4D : movement.y;
                target.setDeltaMovement(movement.x * 0.5D + away.x, vertical,
                        movement.z * 0.5D + away.z);
                target.hurtMarked = true;
            }
            doHurtTarget(target);
        }
    }

    private void applyFlightLimits(LivingEntity target) {
        double verticalAdjustment = 0.0D;
        if (hasGroundWithin(MINIMUM_FLIGHT_HEIGHT)) {
            verticalAdjustment += 0.04D;
        }
        if (target != null) {
            if (target.getY() + MAXIMUM_FLIGHT_HEIGHT > getY()) {
                verticalAdjustment -= 0.04D;
            }
        } else if (!hasGroundWithin(MAXIMUM_FLIGHT_HEIGHT)) {
            verticalAdjustment -= 0.04D;
        }
        if (verticalAdjustment != 0.0D) {
            setDeltaMovement(getDeltaMovement().add(0.0D, verticalAdjustment, 0.0D));
        }
    }

    private boolean hasGroundWithin(int distance) {
        BlockPos cursor = blockPosition().below();
        for (int offset = 1; offset <= distance && cursor.getY() >= level().getMinBuildHeight(); offset++) {
            if (!level().getBlockState(cursor).isAir()) {
                return true;
            }
            cursor = cursor.below();
        }
        return false;
    }

    private void breakBlocksTowardsTarget(LivingEntity target, Kind activeKind) {
        if (blockBreakCooldown > 0 || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() <= 0.001D) {
            return;
        }
        horizontal = horizontal.normalize();
        BlockPos origin = BlockPos.containing(getX() + horizontal.x * activeKind.blockRange,
                getY() + getBbHeight() * 0.5D, getZ() + horizontal.z * activeKind.blockRange);
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || hardness < 0.0F || hardness > 15.0F) {
                continue;
            }
            if (ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this)) {
                blockBreakCooldown = 20;
            }
            return;
        }
    }

    private boolean trySummonFlam(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        int existingFlams = 0;
        boolean teleportActionReserved = false;
        for (Entity entity : serverLevel.getAllEntities()) {
            if (!(entity instanceof FlamEntity flam) || !flam.isAlive() || !flam.isSummonedBy(this)) {
                continue;
            }
            existingFlams++;
            teleportActionReserved |= flam.reservesTeleportAction();
        }
        if (existingFlams >= MAX_SUMMONED_FLAMS) {
            return false;
        }
        FlamEntity flam = ModEntities.SUCCOR.get().create(serverLevel);
        if (flam == null) {
            return false;
        }
        float heading = getYRot() * Mth.DEG_TO_RAD - yBodyRot * 0.01F;
        float spawnDistance = 4.0F * Mth.cos((float) Math.PI / 18.0F);
        Vec3 spawn = position().add(-Mth.sin(heading) * spawnDistance, getEyeHeight(),
                Mth.cos(heading) * spawnDistance);
        flam.moveTo(spawn.x, spawn.y, spawn.z, getYRot(), 0.0F);
        int actionType = random.nextInt(3) + 1;
        if (actionType == FlamEntity.ACTION_TELEPORT && (distanceToSqr(target) < 100.0D
                || !target.onGround() || teleportActionReserved)) {
            actionType = random.nextInt(2) + 1;
        }
        flam.configure(this, target, actionType);
        if (hasEffect(MobEffects.INVISIBILITY)) {
            flam.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false), this);
        }
        if (!serverLevel.addFreshEntity(flam)) {
            return false;
        }
        for (int index = 0; index < 4; index++) {
            serverLevel.broadcastEntityEvent(flam, BIOMASS_EVENT);
        }
        triggerAttackAnimation();
        return true;
    }

    private void fireProjectile(LivingEntity target, ParasiteProjectileEntity.Mode mode, double speed,
                                float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.65D));
        projectile.configure(this, mode, start, target.getEyePosition(), speed, damage, radius, lifetime, target);
        level().addFreshEntity(projectile);
        triggerAttackAnimation();
    }

    private void fireLegacyProjectile(LivingEntity target, ParasiteProjectileEntity.Mode mode) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 look = getViewVector(1.0F);
        Vec3 start = new Vec3(getX() + look.x, getY() + getEyeHeight() - 0.2D, getZ() + look.z);
        Vec3 accelerationDirection = new Vec3(
                target.getX() - (getX() + look.x),
                target.getBoundingBox().minY + target.getBbHeight() * 0.5D
                        - (0.5D + getY() + getBbHeight() * 0.5D),
                target.getZ() - (getZ() + look.z));
        double radius = mode == ParasiteProjectileEntity.Mode.LENCIA_BALL ? 10.0D
                : mode == ParasiteProjectileEntity.Mode.ELVIA_NADE ? 1.45D : 0.3D;
        projectile.configureAccelerating(this, mode, start, accelerationDirection,
                (float) getAttributeValue(Attributes.ATTACK_DAMAGE), radius);
        level().addFreshEntity(projectile);
        triggerAttackAnimation();
    }

    private void spawnHeavyPayload(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Mob payload = switch (random.nextInt(4)) {
            case 0 -> ModEntities.OVERSEER.get().create(serverLevel);
            case 1 -> ModEntities.VIGILANTE.get().create(serverLevel);
            case 2 -> ModEntities.MARAUDER.get().create(serverLevel);
            default -> ModEntities.MONARCH.get().create(serverLevel);
        };
        if (payload == null) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        payload.moveTo(target.getX() + Math.cos(angle) * 2.5D, target.getY(),
                target.getZ() + Math.sin(angle) * 2.5D, getYRot(), 0.0F);
        payload.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(payload.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        payload.setTarget(target);
        serverLevel.addFreshEntity(payload);
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        EntityType<?> type = getType();
        if (type == ModEntities.CARRIER_COLONY.get()) return Kind.CARRIER_COLONY;
        if (type == ModEntities.HAUNTER.get()) return Kind.HAUNTER;
        if (type == ModEntities.BOMBER_HEAVY.get()) return Kind.BOMBER_HEAVY;
        if (type == ModEntities.WRAITH.get()) return Kind.WRAITH;
        return Kind.BOGLE;
    }

    private static final class PreeminentFlyingMoveControl extends MoveControl {
        private PreeminentFlyingMoveControl(PreeminentParasiteEntity mob) {
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
            getMoveControl().setWantedPosition(target.getX(), target.getY() + 3.5D, target.getZ(), speed);
            if (contactCooldown > 0) {
                contactCooldown--;
            } else if (distanceToSqr(target) <= 9.0D) {
                doHurtTarget(target);
                contactCooldown = 20;
            }
        }
    }

    private final class LegacyProjectileAttackGoal extends Goal {
        private final int warmup;
        private final int shotInterval;
        private final int shotsPerCycle;
        private int attackTimer;
        private int shotsFired;
        private int airborneTargetShots;

        private LegacyProjectileAttackGoal(int warmup, int shotInterval, int shotsPerCycle) {
            this.warmup = warmup;
            this.shotInterval = shotInterval;
            this.shotsPerCycle = shotsPerCycle;
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
            if (target == null || !target.isAlive()) {
                attackTimer = 0;
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (distanceToSqr(target) >= 4225.0D || !hasLineOfSight(target)) {
                if (attackTimer > 0) {
                    attackTimer--;
                }
                return;
            }
            attackTimer += hasEffect(ModMobEffects.RAGE) ? 2 : 1;
            if (attackTimer == warmup - 10) {
                revealStealth();
                if (activeKind() == Kind.WRAITH) {
                    wraithProjectileCount++;
                }
            }
            if (attackTimer <= warmup) {
                return;
            }
            if (shotsFired >= shotsPerCycle) {
                attackTimer = 0;
                shotsFired = 0;
                return;
            }
            if (Math.floorMod(attackTimer, shotInterval) != 0) {
                return;
            }
            if (target.onGround()) {
                airborneTargetShots = 0;
            } else {
                airborneTargetShots++;
            }
            if (airborneTargetShots <= 5) {
                ParasiteProjectileEntity.Mode mode;
                if (activeKind() == Kind.BOGLE) {
                    mode = ParasiteProjectileEntity.Mode.LENCIA_BALL;
                } else if (wraithProjectileCount >= 1) {
                    wraithProjectileCount = 0;
                    mode = ParasiteProjectileEntity.Mode.ELVIA_NADE;
                } else {
                    mode = ParasiteProjectileEntity.Mode.ELVIA_BALL;
                }
                fireLegacyProjectile(target, mode);
            }
            shotsFired++;
        }

        @Override
        public void stop() {
            attackTimer = 0;
            shotsFired = 0;
        }
    }

    private final class PreeminentChargeAttackGoal extends Goal {
        private PreeminentChargeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && random.nextInt(5) == 0
                    && distanceToSqr(target) > 4.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return getMoveControl().hasWanted() && charging && target != null && target.isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            Vec3 eye = target.getEyePosition();
            getMoveControl().setWantedPosition(eye.x, target.getY() + 20.0D, eye.z, 0.7D);
            charging = true;
        }

        @Override
        public void stop() {
            charging = false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }
            if (getBoundingBox().intersects(target.getBoundingBox())) {
                doHurtTarget(target);
                charging = false;
                return;
            }
            Vec3 eye = target.getEyePosition();
            double distance = distanceToSqr(target);
            if (distance < 9.0D) {
                getMoveControl().setWantedPosition(eye.x,
                        hasLineOfSight(target) ? eye.y + 20.0D : eye.y, eye.z,
                        hasLineOfSight(target) ? 0.7D : 1.1D);
            } else {
                getMoveControl().setWantedPosition(eye.x, target.getY() + 20.0D, eye.z, 1.1D);
            }
        }
    }

    private final class PreeminentRandomFlightGoal extends Goal {
        private PreeminentRandomFlightGoal() {
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
        public void tick() {
            BlockPos origin = blockPosition();
            int mode = 1;
            double speed = 0.6D;
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
                    speed += 0.15D;
                }
            }
            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos candidate;
                if (mode == 2) {
                    candidate = origin.offset(random.nextInt(6) - 2, random.nextInt(7) - 2,
                            random.nextInt(6) - 2);
                } else if (mode == 3) {
                    candidate = origin.offset(random.nextInt(4) + 3, random.nextInt(5) + 4,
                            random.nextInt(4) + 3);
                } else {
                    candidate = origin.offset(random.nextInt(15) - 7, random.nextInt(11) - 5,
                            random.nextInt(15) - 7);
                }
                if (level().isEmptyBlock(candidate)) {
                    getMoveControl().setWantedPosition(candidate.getX() + 0.5D, candidate.getY() + 1.0D,
                            candidate.getZ() + 0.5D, speed);
                    if (target == null) {
                        getLookControl().setLookAt(candidate.getX() + 0.5D, candidate.getY() + 1.0D,
                                candidate.getZ() + 0.5D, 180.0F, 20.0F);
                    }
                    return;
                }
            }
        }
    }

    private final class CarrierSwimmingGoal extends Goal {
        private CarrierSwimmingGoal() {
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
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.15D, 0.0D));
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

    private final class CarrierWaterLeapGoal extends Goal {
        private int attackTimer;
        private int attacking;
        private double targetX;
        private double targetZ;
        private float targetHeight;

        @Override
        public boolean canUse() {
            return isInWaterOrBubble() || isInLava() || attacking >= 1;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                attackTimer++;
                if (attackTimer >= CARRIER_WATER_LEAP_COOLDOWN_TICKS && attacking == 0) {
                    attacking = 1;
                    targetX = target.getX();
                    targetZ = target.getZ();
                    targetHeight = Math.max(0.0F, (float) (target.getY() - getY()) * 0.07F);
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
                double horizontalDistance = Math.sqrt(x * x + z * z);
                if (horizontalDistance > 0.001D) {
                    Vec3 movement = getDeltaMovement();
                    setDeltaMovement(movement.x + x / horizontalDistance * 1.35D + movement.x * 0.3D,
                            0.7D + targetHeight,
                            movement.z + z / horizontalDistance * 1.35D + movement.z * 0.3D);
                }
            }
            if (attacking >= 3 && onGround()) {
                attacking = 0;
                attackTimer = 0;
            }
        }
    }

    private final class CarrierMeleeGoal extends Goal {
        private int attackCooldown;

        private CarrierMeleeGoal() {
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
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            getNavigation().moveTo(target, distance > 64.0D || attackCooldown == 0 ? 1.3D : 1.0D);
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            double reach = Mth.square(getBbWidth() * 2.0F) + target.getBbWidth();
            if (distance <= reach && attackCooldown <= 0 && hasLineOfSight(target)) {
                attackCooldown = CARRIER_MELEE_INTERVAL_TICKS;
                doHurtTarget(target);
            }
        }
    }

    private final class CarrierBuffGoal extends Goal {
        private int buffTimer;

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
            buffTimer++;
            if (buffTimer < CARRIER_BUFF_INITIAL_DELAY_TICKS) {
                return;
            }
            for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(CARRIER_BUFF_RANGE), entity -> entity != PreeminentParasiteEntity.this
                            && entity instanceof Parasite && entity.isAlive())) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 3, false, false),
                        PreeminentParasiteEntity.this);
                ally.addEffect(new MobEffectInstance(ModMobEffects.FOSTER, 1200, 2, false, false),
                        PreeminentParasiteEntity.this);
                ally.addEffect(new MobEffectInstance(ModMobEffects.LINK, 200, 1, false, false),
                        PreeminentParasiteEntity.this);
            }
            buffTimer -= CARRIER_BUFF_COOLDOWN_TICKS;
        }
    }

    private final class CarrierRecruitGoal extends Goal {
        @Override
        public boolean canUse() {
            return tickCount % 20 == 0 && ParasiteFollowGoal.getLeader(PreeminentParasiteEntity.this) == null
                    && getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            AABB searchArea = new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D,
                    getZ() + 1.0D).inflate(CARRIER_RECRUIT_RANGE, 2.0D, CARRIER_RECRUIT_RANGE);
            for (Mob recruit : level().getEntitiesOfClass(Mob.class, searchArea,
                    entity -> entity != PreeminentParasiteEntity.this && entity instanceof Parasite
                            && entity.isAlive() && ParasiteFollowGoal.commandRank(entity) < 41
                            && hasLineOfSight(entity))) {
                Mob currentLeader = ParasiteFollowGoal.getLeader(recruit);
                if (currentLeader == null || ParasiteFollowGoal.commandRank(currentLeader) <= 30) {
                    ParasiteFollowGoal.setLeader(recruit, PreeminentParasiteEntity.this);
                    return;
                }
            }
        }
    }

    private static final class CarrierHeadPart extends PartEntity<PreeminentParasiteEntity> {
        private CarrierHeadPart(PreeminentParasiteEntity parent) {
            super(parent);
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
            return getParent().hurtCarrierHead(source, amount);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(3.8F, 3.8F);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public Component getName() {
            return Component.literal("carrier_colony_head");
        }
    }

    private static final class HaunterBodyPart extends PartEntity<PreeminentParasiteEntity> {
        private final String name;
        private final float width;
        private final float height;

        private HaunterBodyPart(PreeminentParasiteEntity parent, String name, float width, float height) {
            super(parent);
            this.name = name;
            this.width = width;
            this.height = height;
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
            return getParent().hurt(source, amount);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(width, height).withEyeHeight(0.2F);
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

    private boolean performHaunterAoeAttack(LivingEntity center) {
        AABB targetArea = new AABB(center.getX(), center.getY(), center.getZ(), center.getX() + 1.0D,
                center.getY() + 1.0D, center.getZ() + 1.0D).inflate(5.0D, 2.0D, 5.0D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), targetArea);
        List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class, targetArea);
        if (nearby.size() > 4) {
            AABB damageArea = new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D,
                    getZ() + 1.0D).inflate(5.0D, 3.0D, 5.0D);
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, damageArea,
                    this::isHaunterHostile)) {
                HaunterDamageEntity damage = ModEntities.HAUNTER_DAMAGE.get().create(level());
                if (damage != null) {
                    damage.configure(this, target.position());
                    level().addFreshEntity(damage);
                }
            }
            return true;
        }
        boolean attacked = false;
        for (LivingEntity target : nearby) {
            if (isHaunterHostile(target)) {
                attacked |= doHurtTarget(target);
            }
        }
        return !nearby.isEmpty() || attacked;
    }

    private boolean isHaunterHostile(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    private void fireHaunterHomingProjectile(LivingEntity target) {
        HaunterHomingProjectileEntity projectile = ModEntities.HAUNTER_HOMING.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 look = getViewVector(1.0F);
        Vec3 start = new Vec3(getX() + look.x, getY() + getEyeHeight() - 0.2D, getZ() + look.z);
        projectile.configure(this, target, start);
        playSound(ModSounds.DORPA_RANGE.get(), 2.0F, 1.0F);
        level().addFreshEntity(projectile);
        triggerAttackAnimation();
    }

    private void breakHaunterBlocks(LivingEntity target) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !EventHooks.canEntityGrief(level(), this)) {
            return;
        }
        int baseY = Mth.floor(getY() + 0.1D);
        int verticalOffset = 0;
        int horizontalRange = HAUNTER_BLOCK_BREAK_RANGE;
        if (target != null && target.distanceToSqr(getX(), target.getY(), getZ()) < 9.0D) {
            double heightDifference = target.getY() - getY();
            if (heightDifference < -1.0D) {
                verticalOffset -= 2;
                if (!onGround()) {
                    verticalOffset--;
                }
            } else if (heightDifference > 2.0D) {
                verticalOffset++;
                horizontalRange = 0;
            }
        }
        for (int offsetX = -horizontalRange; offsetX <= horizontalRange; offsetX++) {
            for (int offsetZ = -horizontalRange; offsetZ <= horizontalRange; offsetZ++) {
                for (int offsetY = 1 + verticalOffset; offsetY <= 8 + verticalOffset; offsetY++) {
                    BlockPos candidate = new BlockPos(Mth.floor(getX() + offsetX), baseY + offsetY,
                            Mth.floor(getZ() + offsetZ));
                    BlockState state = level().getBlockState(candidate);
                    if (!isHaunterBreakable(state, candidate)
                            || !EventHooks.onEntityDestroyBlock(this, candidate, state)) {
                        continue;
                    }
                    ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this);
                }
            }
        }
    }

    private boolean isHaunterBreakable(BlockState state, BlockPos pos) {
        if (state.isAir() || state.hasBlockEntity()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level(), pos);
        if (hardness < 0.0F || hardness > HAUNTER_BLOCK_BREAK_HARDNESS) {
            return false;
        }
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return !HAUNTER_BLOCK_BREAK_BLACKLIST.contains(id) && state.canEntityDestroy(level(), pos, this);
    }

    private final class HaunterSwimmingDivingGoal extends Goal {
        private HaunterSwimmingDivingGoal() {
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
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.15D, 0.0D));
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

    private final class HaunterMeleeRangeSwitchGoal extends Goal {
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
            haunterMeleeMode = target != null && distanceToSqr(target) < 100.0D && hasLineOfSight(target);
        }

        @Override
        public void stop() {
            haunterMeleeMode = false;
        }
    }

    private final class HaunterMeleeAoeGoal extends Goal {
        private int attackCooldown;

        private HaunterMeleeAoeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return haunterMeleeMode && target != null && target.isAlive();
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
            if (distanceToSqr(target) <= 25.0D && hasLineOfSight(target)) {
                getNavigation().stop();
                if (attackCooldown == 0) {
                    performHaunterAoeAttack(target);
                    attackCooldown = 10;
                }
            } else {
                getNavigation().moveTo(target, 1.0D);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    private final class HaunterTargetMaintenanceGoal extends Goal {
        private int lostTargetTicks;

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
            if (target == null) {
                lostTargetTicks = 0;
                return;
            }
            if (target instanceof Parasite || !target.isAlive()) {
                setTarget(null);
                haunterMeleeMode = false;
                lostTargetTicks = 0;
                return;
            }
            if (!hasLineOfSight(target) && (random.nextInt(5) == 0
                    || distanceToSqr(target) >= getAttributeValue(Attributes.FOLLOW_RANGE)
                    * getAttributeValue(Attributes.FOLLOW_RANGE))) {
                lostTargetTicks++;
            } else {
                lostTargetTicks = 0;
            }
            if (lostTargetTicks >= 6) {
                setTarget(null);
                haunterMeleeMode = false;
                lostTargetTicks = 0;
            }
        }
    }

    private final class HaunterRangedPositionGoal extends Goal {
        private int seeTime;

        private HaunterRangedPositionGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return !haunterMeleeMode && onGround() && target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return !haunterMeleeMode && target != null && target.isAlive()
                    && (onGround() || !getNavigation().isDone());
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            double distance = distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            if (distance > getAttributeValue(Attributes.FOLLOW_RANGE) * getAttributeValue(Attributes.FOLLOW_RANGE)) {
                setTarget(null);
                return;
            }
            boolean canSeeTarget = hasLineOfSight(target);
            seeTime = canSeeTarget ? seeTime + 1 : 0;
            if (distance <= 1600.0D && seeTime >= 10) {
                getNavigation().stop();
            } else {
                getNavigation().moveTo(target, 1.0D);
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        @Override
        public void stop() {
            seeTime = 0;
        }
    }

    private final class HaunterHomingBurstGoal extends Goal {
        private int attackTimer;
        private int shotsFired;

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
                attackTimer = 0;
                return;
            }
            if (distanceToSqr(target) >= 4225.0D || !hasLineOfSight(target)) {
                if (attackTimer > 0) {
                    attackTimer--;
                }
                return;
            }
            attackTimer++;
            if (attackTimer <= 60) {
                return;
            }
            if (shotsFired < 3) {
                if (attackTimer % 10 == 0) {
                    fireHaunterHomingProjectile(target);
                    shotsFired++;
                }
            } else {
                attackTimer = 0;
                shotsFired = 0;
            }
        }

        @Override
        public void stop() {
            attackTimer = 0;
            shotsFired = 0;
        }
    }

    private final class HaunterEvadeDashGoal extends Goal {
        private int cooldown = 41;

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
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            double distance = distanceToSqr(target);
            if (distance > 16.0D && distance < 10_000.0D && hasLineOfSight(target) && cooldown < 40) {
                cooldown++;
            }
            if (cooldown < 40) {
                return;
            }
            double x = target.getX() - getX();
            double z = target.getZ() - getZ();
            double horizontalLength = Math.sqrt(x * x + z * z);
            if (horizontalLength > 0.001D) {
                Vec3 movement = getDeltaMovement();
                double bonusX = random.nextBoolean() ? 5.0D : 0.0D;
                double bonusZ = bonusX == 0.0D ? 5.0D : 0.0D;
                setDeltaMovement(movement.x + x / horizontalLength * 4.0D + movement.x * 0.2D + bonusX,
                        movement.y, movement.z + z / horizontalLength * 4.0D + movement.z * 0.2D + bonusZ);
                hurtMarked = true;
            }
            getNavigation().stop();
            cooldown = 0;
        }
    }

    private final class HaunterBlockBreakGoal extends Goal {
        @Override
        public boolean canUse() {
            return isInWall() || getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (haunterBlockBreakCooldown > 0) {
                haunterBlockBreakCooldown--;
                return;
            }
            LivingEntity target = getTarget();
            if (!isInWall() && (target == null || distanceToSqr(target) > HAUNTER_BLOCK_BREAK_MAX_DISTANCE_SQR)) {
                return;
            }
            breakHaunterBlocks(target);
            haunterBlockBreakCooldown = HAUNTER_BLOCK_BREAK_INTERVAL_TICKS;
        }
    }

    private final class HeavyBomberBombGoal extends Goal {
        private int cooldown;

        private HeavyBomberBombGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.onGround() && hasLineOfSight(target) && distanceToSqr(target) <= 2304.0D;
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
            fireProjectile(target, ParasiteProjectileEntity.Mode.BOMB, 0.62D, 55.0F, 5.0D, 120);
            spawnHeavyPayload(target);
            cooldown = 160;
        }
    }

    public enum Kind {
        BOGLE(true, 310.0D, 15.5D, 70.0D, 0.28D, 2.0D, 80.0D, 5.0D),
        CARRIER_COLONY(false, 390.0D, 15.5D, 45.0D, 0.242D, 2.0D, 80.0D, 5.0D),
        HAUNTER(false, 360.0D, 15.5D, 110.0D, 0.283D, 2.0D, 80.0D, 5.0D),
        BOMBER_HEAVY(true, 420.0D, 15.5D, 33.0D, 0.25D, 0.15D, 80.0D, 5.0D),
        WRAITH(true, 310.0D, 15.5D, 70.0D, 0.28D, 2.0D, 80.0D, 5.0D);

        private final boolean flying;
        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final double movementSpeed;
        private final double knockbackResistance;
        private final double followRange;
        private final double blockRange;

        Kind(boolean flying, double maxHealth, double armor, double attackDamage, double movementSpeed,
             double knockbackResistance, double followRange, double blockRange) {
            this.flying = flying;
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.knockbackResistance = knockbackResistance;
            this.followRange = followRange;
            this.blockRange = blockRange;
        }
    }
}
