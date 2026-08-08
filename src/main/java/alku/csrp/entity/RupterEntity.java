package alku.csrp.entity;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.UUID;

public class RupterEntity extends Monster implements GeoEntity, Parasite {
    public static final int TUNNEL_KILL_COST = 5;
    private static final int OVERHEAT_WARMUP_DURATION = 100;
    private static final int BAT_LEAP_EVALUATION_TIMEOUT = 40;
    private static final String KILL_COUNT_NBT_KEY = "rupter_kill_count";
    private static final String VARIANT_NBT_KEY = "rupter_texture_variant";
    private static final String BEHAVIOR_VARIANT_NBT_KEY = "rupter_behavior_variant";
    private static final String OVERHEATED_NBT_KEY = "rupter_overheated";
    private static final String OVERHEAT_WARMUP_NBT_KEY = "rupter_overheat_warmup";
    private static final String FAILED_BAT_LEAPS_NBT_KEY = "rupter_failed_bat_leaps";
    private static final String FAILED_BAT_TARGET_NBT_KEY = "rupter_failed_bat_target";
    private static final float SPECIAL_VARIANT_CHANCE = 0.165F;
    private static final ResourceLocation OVERHEAT_ATTACK_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "rupter_overheat_attack");
    private static final ResourceLocation OVERHEAT_SPEED_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "rupter_overheat_speed");
    private static final ResourceLocation OVERHEAT_JUMP_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "rupter_overheat_jump");
    private static final EntityDataAccessor<Byte> CLIMBING =
            SynchedEntityData.defineId(RupterEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> TEXTURE_VARIANT =
            SynchedEntityData.defineId(RupterEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> BEHAVIOR_VARIANT =
            SynchedEntityData.defineId(RupterEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> OVERHEATED =
            SynchedEntityData.defineId(RupterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OVERHEAT_WARMUP_TICKS =
            SynchedEntityData.defineId(RupterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEAP_ATTACK_TICKS =
            SynchedEntityData.defineId(RupterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> COMBAT_STATUS =
            SynchedEntityData.defineId(RupterEntity.class, EntityDataSerializers.BOOLEAN);
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");
    private final RawAnimation COMBAT_AGE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation COMBAT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation SPRINT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int killCount;
    private int cloudCooldown;
    private int failedBatLeaps;
    private int pendingBatLeapTicks;
    private boolean variantsInitialized;
    private boolean pendingBatLeap;
    private boolean pendingBatLeapAirborne;
    @Nullable
    private UUID failedBatTarget;
    @Nullable
    private UUID pendingBatLeapTarget;

    public RupterEntity(EntityType<? extends RupterEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
        if (!level.isClientSide) {
            initializeVariants();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 5.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public static boolean checkRupterSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level,
                                                 MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        int phase = Config.evolutionPhase(level.getLevel());
        return phase >= 1 && phase <= 7
                && Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 6;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new OverheatStunGoal());
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new RupterLeapGoal(this, 0.7F));
        goalSelector.addGoal(3, new CothCloudGoal());
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.3, false));
        goalSelector.addGoal(5, new AvoidEntityGoal<>(this, LivingEntity.class, 8.0F, 1.0, 1.3,
                this::shouldAvoid));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(7, new ParasiteFollowGoal(this));
        goalSelector.addGoal(7, new RupterSpinGoal());
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false, this::canTargetByPhase));
    }

    private boolean shouldAvoid(LivingEntity entity) {
        return isLoneBeforeAggressivePhase()
                && !(entity instanceof Parasite)
                && (entity instanceof Player || entity instanceof Monster);
    }

    private boolean canTargetByPhase(LivingEntity entity) {
        return entity != this && entity.isAlive() && !(entity instanceof Parasite) && !isLoneBeforeAggressivePhase();
    }

    private boolean isLoneBeforeAggressivePhase() {
        // 原模组在阶段 4 前保持游击/感染行为，阶段 4 起才转为正常主动攻击。
        return Config.evolutionPhase(level()) < 4 && nearbyRupters() == 0;
    }

    private int nearbyRupters() {
        AABB searchArea = getBoundingBox().inflate(8.0);
        return level().getEntitiesOfClass(RupterEntity.class, searchArea, rupter -> rupter != this).size();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        setClimbing(horizontalCollision);
        tickOverheatWarmup();
        tickBatLeapAttempt();
        if (isOverheatCharging()) {
            navigation.stop();
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (cloudCooldown > 0) {
            cloudCooldown--;
        }
        int leapTicks = entityData.get(LEAP_ATTACK_TICKS);
        if (leapTicks > 0) {
            entityData.set(LEAP_ATTACK_TICKS, leapTicks - 1);
        }
        LivingEntity target = getTarget();
        entityData.set(COMBAT_STATUS, target != null && target.isAlive());
        performLiquidLeap();
        tryPlaceTunnel();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isOverheatCharging()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    private void tickOverheatWarmup() {
        int warmupTicks = getOverheatWarmupTicks();
        if (!isOverheated() || warmupTicks <= 0) {
            return;
        }

        warmupTicks--;
        entityData.set(OVERHEAT_WARMUP_TICKS, warmupTicks);
        if (warmupTicks == 0) {
            applyOverheatModifiers();
        }
    }

    private void beginBatLeapAttempt(Bat bat) {
        if (isOverheated()) {
            return;
        }
        pendingBatLeap = true;
        pendingBatLeapAirborne = false;
        pendingBatLeapTicks = 0;
        pendingBatLeapTarget = bat.getUUID();
    }

    private void tickBatLeapAttempt() {
        if (isOverheated()) {
            clearBatLeapTracking();
            return;
        }

        if (!pendingBatLeap) {
            LivingEntity target = getTarget();
            if (failedBatLeaps > 0 && (!(target instanceof Bat bat) || !bat.isAlive()
                    || !bat.getUUID().equals(failedBatTarget))) {
                clearBatLeapTracking();
            }
            return;
        }

        pendingBatLeapTicks++;
        if (!onGround()) {
            pendingBatLeapAirborne = true;
        }
        if ((pendingBatLeapAirborne && onGround()) || pendingBatLeapTicks >= BAT_LEAP_EVALUATION_TIMEOUT) {
            evaluateBatLeapAttempt();
        }
    }

    private void evaluateBatLeapAttempt() {
        UUID targetId = pendingBatLeapTarget;
        clearPendingBatLeap();
        if (!(level() instanceof ServerLevel serverLevel) || targetId == null) {
            clearBatLeapTracking();
            return;
        }

        Entity target = serverLevel.getEntity(targetId);
        if (!(target instanceof Bat bat) || !bat.isAlive()) {
            clearBatLeapTracking();
            return;
        }

        if (targetId.equals(failedBatTarget)) {
            failedBatLeaps++;
        } else {
            failedBatTarget = targetId;
            failedBatLeaps = 1;
        }
        if (failedBatLeaps >= 2) {
            activateOverheat();
        }
    }

    private void activateOverheat() {
        if (isOverheated()) {
            return;
        }
        entityData.set(OVERHEATED, true);
        entityData.set(OVERHEAT_WARMUP_TICKS, OVERHEAT_WARMUP_DURATION);
        navigation.stop();
        setDeltaMovement(Vec3.ZERO);
        clearBatLeapTracking();
    }

    private void clearPendingBatLeap() {
        pendingBatLeap = false;
        pendingBatLeapAirborne = false;
        pendingBatLeapTicks = 0;
        pendingBatLeapTarget = null;
    }

    private void clearBatLeapTracking() {
        clearPendingBatLeap();
        failedBatLeaps = 0;
        failedBatTarget = null;
    }

    private void applyOverheatModifiers() {
        addPermanentModifier(Attributes.ATTACK_DAMAGE, OVERHEAT_ATTACK_MODIFIER, 2.0D);
        addPermanentModifier(Attributes.MOVEMENT_SPEED, OVERHEAT_SPEED_MODIFIER, 0.5D);
        addPermanentModifier(Attributes.JUMP_STRENGTH, OVERHEAT_JUMP_MODIFIER, 0.5D);
    }

    private void addPermanentModifier(Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPermanentModifier(new AttributeModifier(
                    id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private void performLiquidLeap() {
        if (!isInWaterOrBubble() || navigation.isDone() || tickCount % 10 != 0) {
            return;
        }

        Vec3 movement = getDeltaMovement();
        setDeltaMovement(movement.x * 1.15, Math.max(movement.y, 0.18), movement.z * 1.15);
    }

    private void tryPlaceTunnel() {
        int phase = Config.evolutionPhase(level());
        if (phase < -1 || phase >= 3 || killCount < TUNNEL_KILL_COST || getTarget() != null
                || tickCount % 10 != 0 || random.nextInt(30) != 0) {
            return;
        }

        BlockPos pos = blockPosition();
        BlockState below = level().getBlockState(pos.below());
        if (level().getBlockState(pos).isAir() && below.isFaceSturdy(level(), pos.below(), Direction.UP)) {
            level().setBlockAndUpdate(pos, ModBlocks.TUNNEL.get().defaultBlockState());
            killCount -= TUNNEL_KILL_COST;
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), this);
            living.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0), this);
            if (getBehaviorVariant() == BehaviorVariant.BERSERKER) {
                living.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 60, 0), this);
            } else if (getBehaviorVariant() == BehaviorVariant.VIRULENT) {
                applyVirulentAttackEffect(living);
            }
        }
        return hit;
    }

    private void applyVirulentAttackEffect(LivingEntity target) {
        if (entityData.get(LEAP_ATTACK_TICKS) > 0) {
            target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 80, 0), this);
        } else {
            target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 40, 0), this);
        }
    }

    @Override
    public void push(Entity entity) {
        if (!level().isClientSide && getBehaviorVariant() == BehaviorVariant.VIRULENT
                && entity instanceof LivingEntity living && !(living instanceof Parasite)) {
            living.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 40, 0), this);
        }
        super.push(entity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        if (victim instanceof Bat) {
            clearBatLeapTracking();
        }
        killCount++;
        if (killCount >= 30) {
            tryEvolve(level);
        }
        return true;
    }

    private void tryEvolve(ServerLevel level) {
        ManglerEvolutionTarget.manglerType().ifPresent(type -> {
            Mob mangler = type.create(level);
            if (mangler == null) {
                return;
            }
            mangler.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            level.addFreshEntity(mangler);
            discard();
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, (byte) 0);
        builder.define(TEXTURE_VARIANT, (byte) TextureVariant.NORMAL.ordinal());
        builder.define(BEHAVIOR_VARIANT, (byte) BehaviorVariant.NORMAL.ordinal());
        builder.define(OVERHEATED, false);
        builder.define(OVERHEAT_WARMUP_TICKS, 0);
        builder.define(LEAP_ATTACK_TICKS, 0);
        builder.define(COMBAT_STATUS, false);
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    public boolean isClimbing() {
        return (entityData.get(CLIMBING) & 1) != 0;
    }

    public void setClimbing(boolean climbing) {
        byte value = entityData.get(CLIMBING);
        entityData.set(CLIMBING, climbing ? (byte) (value | 1) : (byte) (value & -2));
    }

    public TextureVariant getTextureVariant() {
        int index = entityData.get(TEXTURE_VARIANT);
        return index >= 0 && index < TextureVariant.values().length
                ? TextureVariant.values()[index]
                : TextureVariant.NORMAL;
    }

    private void setTextureVariant(TextureVariant variant) {
        entityData.set(TEXTURE_VARIANT, (byte) variant.ordinal());
    }

    public BehaviorVariant getBehaviorVariant() {
        int index = entityData.get(BEHAVIOR_VARIANT);
        return index >= 0 && index < BehaviorVariant.values().length
                ? BehaviorVariant.values()[index]
                : BehaviorVariant.NORMAL;
    }

    private void setBehaviorVariant(BehaviorVariant variant) {
        entityData.set(BEHAVIOR_VARIANT, (byte) variant.ordinal());
    }

    public boolean isOverheated() {
        return entityData.get(OVERHEATED);
    }

    public boolean isOverheatCharging() {
        return isOverheated() && getOverheatWarmupTicks() > 0;
    }

    public int getOverheatWarmupTicks() {
        return entityData.get(OVERHEAT_WARMUP_TICKS);
    }

    private void initializeVariants() {
        if (variantsInitialized) {
            return;
        }

        variantsInitialized = true;
        float specialRoll = random.nextFloat();
        if (specialRoll < SPECIAL_VARIANT_CHANCE) {
            setBehaviorVariant(BehaviorVariant.BERSERKER);
            setTextureVariant(TextureVariant.NORMAL);
        } else if (specialRoll < SPECIAL_VARIANT_CHANCE * 2.0F) {
            setBehaviorVariant(BehaviorVariant.VIRULENT);
            setTextureVariant(TextureVariant.NORMAL);
        } else {
            setBehaviorVariant(BehaviorVariant.NORMAL);
            rollTextureVariant();
        }
    }

    private void rollTextureVariant() {
        float roll = random.nextFloat();
        if (roll < 0.005F) {
            setTextureVariant(TextureVariant.GOLDEN);
        } else if (roll < 0.055F) {
            setTextureVariant(TextureVariant.WEIRD);
        } else if (roll < 0.305F) {
            setTextureVariant(TextureVariant.FLUFFY);
        } else if (roll < 0.705F) {
            setTextureVariant(TextureVariant.STRIPED);
        } else if (roll < 0.855F) {
            setTextureVariant(TextureVariant.CLASSIC);
        } else {
            setTextureVariant(TextureVariant.NORMAL);
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        initializeVariants();
        return data;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(KILL_COUNT_NBT_KEY, killCount);
        tag.putByte(VARIANT_NBT_KEY, (byte) getTextureVariant().ordinal());
        tag.putByte(BEHAVIOR_VARIANT_NBT_KEY, (byte) getBehaviorVariant().ordinal());
        tag.putBoolean(OVERHEATED_NBT_KEY, isOverheated());
        tag.putInt(OVERHEAT_WARMUP_NBT_KEY, getOverheatWarmupTicks());
        tag.putInt(FAILED_BAT_LEAPS_NBT_KEY, failedBatLeaps);
        if (failedBatTarget != null) {
            tag.putUUID(FAILED_BAT_TARGET_NBT_KEY, failedBatTarget);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        killCount = tag.getInt(KILL_COUNT_NBT_KEY);
        int variant = tag.getByte(VARIANT_NBT_KEY);
        if (variant >= 0 && variant < TextureVariant.values().length) {
            setTextureVariant(TextureVariant.values()[variant]);
        }
        int behaviorVariant = tag.getByte(BEHAVIOR_VARIANT_NBT_KEY);
        if (behaviorVariant >= 0 && behaviorVariant < BehaviorVariant.values().length) {
            setBehaviorVariant(BehaviorVariant.values()[behaviorVariant]);
        }
        boolean overheated = tag.getBoolean(OVERHEATED_NBT_KEY);
        int warmupTicks = overheated ? Math.max(0, tag.getInt(OVERHEAT_WARMUP_NBT_KEY)) : 0;
        entityData.set(OVERHEATED, overheated);
        entityData.set(OVERHEAT_WARMUP_TICKS, warmupTicks);
        failedBatTarget = tag.hasUUID(FAILED_BAT_TARGET_NBT_KEY)
                ? tag.getUUID(FAILED_BAT_TARGET_NBT_KEY)
                : null;
        failedBatLeaps = failedBatTarget == null
                ? 0
                : Math.min(1, Math.max(0, tag.getInt(FAILED_BAT_LEAPS_NBT_KEY)));
        clearPendingBatLeap();
        if (overheated && warmupTicks == 0) {
            applyOverheatModifiers();
        }
        variantsInitialized = true;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.RUPTER_LIVING.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RUPTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RUPTER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.RUPTER_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    private <T extends RupterEntity> PlayState movementAnimation(AnimationState<T> state) {
        if (entityData.get(LEAP_ATTACK_TICKS) > 0) {
            return state.setAndContinue(LEAP);
        }
        boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
        if (!moving) {
            return state.setAndContinue(entityData.get(COMBAT_STATUS) ? COMBAT_AGE : AGE_IN_TICKS);
        }
        if (getDeltaMovement().horizontalDistanceSqr() > 0.02D) {
            return state.setAndContinue(SPRINT_LIMB);
        }
        return state.setAndContinue(entityData.get(COMBAT_STATUS) ? COMBAT_LIMB : LIMB_SWING);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public enum TextureVariant {
        NORMAL(""),
        CLASSIC("_classic"),
        STRIPED("_striped"),
        FLUFFY("_fluffy"),
        WEIRD("_weird"),
        GOLDEN("_golden");

        private final String suffix;

        TextureVariant(String suffix) {
            this.suffix = suffix;
        }

        public String suffix() {
            return suffix;
        }
    }

    public enum BehaviorVariant {
        NORMAL(""),
        BERSERKER("_bleeding"),
        VIRULENT("_virus");

        private final String suffix;

        BehaviorVariant(String suffix) {
            this.suffix = suffix;
        }

        public String suffix() {
            return suffix;
        }
    }

    private final class OverheatStunGoal extends Goal {
        private OverheatStunGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isOverheatCharging();
        }

        @Override
        public boolean canContinueToUse() {
            return isOverheatCharging();
        }

        @Override
        public void start() {
            navigation.stop();
        }

        @Override
        public void tick() {
            navigation.stop();
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private static final class RupterLeapGoal extends LeapAtTargetGoal {
        private final RupterEntity rupter;

        private RupterLeapGoal(RupterEntity rupter, float verticalVelocity) {
            super(rupter, verticalVelocity);
            this.rupter = rupter;
        }

        @Override
        public void start() {
            super.start();
            rupter.entityData.set(LEAP_ATTACK_TICKS, 20);
            LivingEntity target = rupter.getTarget();
            if (target instanceof Bat bat) {
                rupter.beginBatLeapAttempt(bat);
            } else {
                rupter.clearBatLeapTracking();
            }
            if (rupter.isOverheated() && !rupter.isOverheatCharging()) {
                Vec3 movement = rupter.getDeltaMovement();
                rupter.setDeltaMovement(movement.x, movement.y * 1.5D, movement.z);
            }
        }
    }

    private final class RupterSpinGoal extends Goal {
        private static final int SPIN_CHANCE = 200;
        private int spinTicks;
        private int elapsedTicks;
        private int jumpAtTick;
        private float turnDirection;

        private RupterSpinGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && onGround() && navigation.isDone()
                    && random.nextInt(reducedTickDelay(SPIN_CHANCE)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return spinTicks > 0 && getTarget() == null;
        }

        @Override
        public void start() {
            navigation.stop();
            spinTicks = 40 + random.nextInt(41);
            elapsedTicks = 0;
            jumpAtTick = random.nextInt(3) == 0 ? 8 + random.nextInt(spinTicks - 15) : -1;
            turnDirection = random.nextBoolean() ? 24.0F : -24.0F;
        }

        @Override
        public void tick() {
            spinTicks--;
            elapsedTicks++;
            float yaw = getYRot() + turnDirection;
            setYRot(yaw);
            setYHeadRot(yaw);
            yBodyRot = yaw;
            if (elapsedTicks == jumpAtTick && onGround()) {
                getJumpControl().jump();
            }
        }
    }

    private final class CothCloudGoal extends Goal {
        @Nullable
        private Animal passiveTarget;

        private CothCloudGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cloudCooldown > 0 || !isLoneBeforeAggressivePhase() || random.nextInt(reducedTickDelay(20)) != 0) {
                return false;
            }

            passiveTarget = level().getEntitiesOfClass(Animal.class, getBoundingBox().inflate(10.0),
                            animal -> animal.isAlive() && !(animal instanceof Parasite))
                    .stream()
                    .min(Comparator.comparingDouble(RupterEntity.this::distanceToSqr))
                    .orElse(null);
            return passiveTarget != null;
        }

        @Override
        public void start() {
            if (passiveTarget == null) {
                return;
            }

            getLookControl().setLookAt(passiveTarget, 30.0F, 30.0F);
            AreaEffectCloud cloud = new AreaEffectCloud(
                    level(), passiveTarget.getX(), passiveTarget.getY(), passiveTarget.getZ());
            cloud.setOwner(RupterEntity.this);
            cloud.setRadius(2.5F);
            cloud.setDuration(200);
            cloud.setWaitTime(0);
            cloud.setRadiusPerTick(-0.01F);
            cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 600, 1));
            level().addFreshEntity(cloud);
            playSound(ModSounds.RUPTER_CLOUD.get(), 1.0F, 1.0F);
            cloudCooldown = 200;
        }
    }
}
