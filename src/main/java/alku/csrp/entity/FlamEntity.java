package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** Modern equivalent of SRP 1.10.7's independently registered EntityFlam. */
public final class FlamEntity extends PrimitiveParasiteEntity {
    public static final int ACTION_EXPLODE = 1;
    public static final int ACTION_ORB = 2;
    public static final int ACTION_TELEPORT = 3;

    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(
            FlamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FINISHING = SynchedEntityData.defineId(
            FlamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTIVATION_PROGRESS = SynchedEntityData.defineId(
            FlamEntity.class, EntityDataSerializers.INT);
    private static final int MAX_LIFETIME_TICKS = 400;
    private static final int MAX_STATIONARY_TICKS = 20;
    private static final int ACTION_ACTIVATION_PROGRESS = 20;
    private static final int BLOCK_BREAK_INTERVAL_TICKS = 20;
    private static final int BLOCK_BREAK_RANGE = 2;
    private static final int BLOCK_BREAK_HEIGHT = 2;
    private static final float BLOCK_BREAK_HARDNESS = 15.0F;
    private static final int MAX_ADAPTATION_HITS = 5;
    private static final int MAX_LEARNABLE_DAMAGE_SOURCES = 20;
    private static final byte BIOMASS_EVENT = 8;
    private static final byte FLAME_EVENT = 100;
    private static final Set<String> BLOCK_BREAK_BLACKLIST = Set.of(
            "csrp:biome_heart", "csrp:colony_heart", "csrp:parasite_rubble_dense",
            "csrp:parasite_canister_active", "srparasites:biomeheart", "srparasites:colonyheart",
            "srparasites:parasiterubbledense", "srparasites:parasitecanisteractive");

    private final RawAnimation idleAnimation = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation walkAnimation = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation attackAnimation = ParasiteAnimations.play(this, "attack");
    private UUID fatherId;
    private UUID targetId;
    private BlockPos targetPosition;
    private int actionType;
    private int activationProgress;
    private int stationaryTicks;
    private boolean actionConsumed;

    public FlamEntity(EntityType<? extends FlamEntity> type, Level level) {
        super(type, level);
        moveControl = new FlamMoveControl(this);
        setNoGravity(true);
        xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 85.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FLYING_SPEED, 0.32D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 80.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING, false);
        builder.define(FINISHING, false);
        builder.define(ACTIVATION_PROGRESS, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(4, new ChargeAttackGoal());
        goalSelector.addGoal(6, new RandomMoveGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    public void configure(PrimitiveParasiteEntity father, LivingEntity target, int actionType) {
        fatherId = father.getUUID();
        this.actionType = Mth.clamp(actionType, ACTION_EXPLODE, ACTION_TELEPORT);
        setTarget(target);
        var attackDamage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null && father.getAttributeValue(Attributes.ATTACK_DAMAGE) > 0.0D) {
            attackDamage.setBaseValue(father.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0D);
        }
    }

    public boolean isSummonedBy(PrimitiveParasiteEntity father) {
        return father != null && fatherId != null && fatherId.equals(father.getUUID());
    }

    public boolean reservesTeleportAction() {
        return actionType == ACTION_TELEPORT && !actionConsumed;
    }

    public boolean isCharging() {
        return entityData.get(CHARGING);
    }

    private void setCharging(boolean charging) {
        entityData.set(CHARGING, charging);
    }

    public boolean isFinishing() {
        return entityData.get(FINISHING);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target != null) {
            targetId = target.getUUID();
            targetPosition = target.blockPosition();
        }
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        fallDistance = 0.0F;
        if (level().isClientSide || isNoAi() || actionConsumed) {
            return;
        }

        refreshTargetPosition();
        if (isFinishing()) {
            setActivationProgress(activationProgress + 1);
        }
        if (getX() == xo && getZ() == zo) {
            stationaryTicks++;
        }
        if (stationaryTicks > MAX_STATIONARY_TICKS) {
            advanceFinishing();
            return;
        }

        PrimitiveParasiteEntity father = resolveFather();
        if (targetPosition == null || father == null) {
            setActivationProgress(activationProgress + 5000);
            advanceFinishing();
            return;
        }
        if (tickCount % BLOCK_BREAK_INTERVAL_TICKS == 0) {
            breakNearbyBlocks();
        }
        if (distanceToSqr(Vec3.atCenterOf(targetPosition)) < 4.0D || isFinishing()) {
            advanceFinishing();
            return;
        }
        if (tickCount > MAX_LIFETIME_TICKS) {
            setActivationProgress(activationProgress + 5000);
            advanceFinishing();
        }
    }

    private void refreshTargetPosition() {
        LivingEntity target = getTarget();
        if ((target == null || !target.isAlive()) && targetId != null && level() instanceof ServerLevel serverLevel) {
            Entity resolved = serverLevel.getEntity(targetId);
            if (resolved instanceof LivingEntity living && living.isAlive()) {
                setTarget(living);
                target = living;
            }
        }
        if (target != null && target.isAlive()) {
            targetPosition = target.blockPosition();
        }
    }

    private void advanceFinishing() {
        setActivationProgress(activationProgress + 1);
        if (!isFinishing()) {
            triggerAnim("action_controller", "attack");
        }
        entityData.set(FINISHING, true);
        setCharging(false);
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0D);
        setDeltaMovement(Vec3.ZERO);
        if (activationProgress >= ACTION_ACTIVATION_PROGRESS) {
            completeAction();
        }
    }

    private void setActivationProgress(int progress) {
        activationProgress = progress;
        entityData.set(ACTIVATION_PROGRESS, progress);
    }

    private void completeAction() {
        if (actionConsumed) {
            return;
        }
        actionConsumed = true;
        PrimitiveParasiteEntity father = resolveFather();
        switch (actionType) {
            case ACTION_EXPLODE -> performExplosion();
            case ACTION_ORB -> {
                if (!spawnScaryOrb(father, -3.0D)) {
                    performExplosion();
                }
            }
            case ACTION_TELEPORT -> completeTeleportAction(father);
        }
        discard();
    }

    private void completeTeleportAction(@Nullable PrimitiveParasiteEntity father) {
        if (father != null && targetPosition != null
                && distanceToSqr(Vec3.atCenterOf(targetPosition)) < 16.0D) {
            father.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            father.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (random.nextBoolean()) {
            performExplosion();
        } else if (!spawnScaryOrb(father, targetPosition == null ? -2.0D : -3.0D)) {
            performExplosion();
        }
    }

    private boolean spawnScaryOrb(@Nullable PrimitiveParasiteEntity father, double yOffset) {
        if (father == null) {
            return false;
        }
        ScaryOrbEntity orb = new ScaryOrbEntity(ModEntities.SCARY_ORB.get(), level(), father);
        orb.setAnchor(position().add(0.0D, yOffset, 0.0D));
        level().addFreshEntity(orb);
        playSound(ModSounds.ORB_START.get(), 1.0F, 1.0F);
        return true;
    }

    private void performExplosion() {
        level().broadcastEntityEvent(this, FLAME_EVENT);
        playSound(ModSounds.MOB_EXPLOSION.get(), 1.0F, 1.0F);
        hurtNearby(this, 4.0D, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), false);
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY() - 2.0D, getZ());
        cloud.setOwner(this);
        cloud.setRadius(getBbWidth() * 3.5F);
        cloud.setWaitTime(10);
        cloud.setDuration(300);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 2, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 300, 2, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 2, false, false));
        level().addFreshEntity(cloud);
    }

    private PrimitiveParasiteEntity resolveFather() {
        if (fatherId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity father = serverLevel.getEntity(fatherId);
        return father instanceof PrimitiveParasiteEntity parasite ? parasite : null;
    }

    private void breakNearbyBlocks() {
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !EventHooks.canEntityGrief(level(), this)) {
            return;
        }
        int baseX = Mth.floor(getX());
        int baseY = Mth.floor(getY() + 0.1D);
        int baseZ = Mth.floor(getZ());
        for (int offsetX = -BLOCK_BREAK_RANGE; offsetX <= BLOCK_BREAK_RANGE; offsetX++) {
            for (int offsetZ = -BLOCK_BREAK_RANGE; offsetZ <= BLOCK_BREAK_RANGE; offsetZ++) {
                for (int offsetY = -1; offsetY <= BLOCK_BREAK_HEIGHT + 1; offsetY++) {
                    BlockPos candidate = new BlockPos(baseX + offsetX, baseY + offsetY, baseZ + offsetZ);
                    BlockState state = level().getBlockState(candidate);
                    if (!isBreakable(state, candidate)
                            || !EventHooks.onEntityDestroyBlock(this, candidate, state)) {
                        continue;
                    }
                    ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this);
                }
            }
        }
    }

    private boolean isBreakable(BlockState state, BlockPos pos) {
        if (state.isAir()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level(), pos);
        if (hardness < 0.0F || hardness > BLOCK_BREAK_HARDNESS) {
            return false;
        }
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return !BLOCK_BREAK_BLACKLIST.contains(id) && state.canEntityDestroy(level(), pos, this);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == BIOMASS_EVENT) {
            for (int index = 0; index <= 10; index++) {
                level().addParticle(ParticleTypes.CRIMSON_SPORE,
                        getRandomX(1.2D), getRandomY(), getRandomZ(1.2D),
                        random.nextGaussian() * 0.03D, random.nextGaussian() * 0.03D,
                        random.nextGaussian() * 0.03D);
            }
        } else if (id == FLAME_EVENT) {
            for (int index = 0; index <= 1; index++) {
                level().addParticle(ParticleTypes.FLAME,
                        getRandomX(1.0D), getRandomY(), getRandomZ(1.0D),
                        random.nextGaussian() * 0.05D, random.nextGaussian() * 0.05D,
                        random.nextGaussian() * 0.05D);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(0.5F);
    }

    @Override
    protected int incomingDamageCapDivisor() {
        // SRP 1.10.7 inherits the shared preeminentCap (18); the Wiki lists 15 for Succor.
        return 18;
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return MAX_ADAPTATION_HITS;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return 0.20F;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return MAX_LEARNABLE_DAMAGE_SOURCES;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return 1.0F;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return 0.30F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (fatherId != null) {
            tag.putUUID("flam_father", fatherId);
        }
        if (targetId != null) {
            tag.putUUID("flam_target", targetId);
        }
        if (targetPosition != null) {
            tag.putLong("flam_target_pos", targetPosition.asLong());
        }
        tag.putInt("flam_action", actionType);
        tag.putInt("flam_activation", activationProgress);
        tag.putInt("flam_stationary", stationaryTicks);
        tag.putBoolean("flam_consumed", actionConsumed);
        tag.putBoolean("flam_charging", isCharging());
        tag.putBoolean("flam_finishing", isFinishing());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fatherId = tag.hasUUID("flam_father") ? tag.getUUID("flam_father") : null;
        targetId = tag.hasUUID("flam_target") ? tag.getUUID("flam_target") : null;
        targetPosition = tag.contains("flam_target_pos")
                ? BlockPos.of(tag.getLong("flam_target_pos")) : null;
        actionType = Mth.clamp(tag.getInt("flam_action"), 0, ACTION_TELEPORT);
        stationaryTicks = Math.max(0, tag.getInt("flam_stationary"));
        actionConsumed = tag.getBoolean("flam_consumed");
        setCharging(tag.getBoolean("flam_charging"));
        entityData.set(FINISHING, tag.getBoolean("flam_finishing"));
        setActivationProgress(Math.max(0, tag.getInt("flam_activation")));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (isFinishing() || !ParasiteAnimations.isMoving(this, state.isMoving())) {
                return state.setAndContinue(idleAnimation);
            }
            return state.setAndContinue(walkAnimation);
        }));
        controllers.add(new AnimationController<>(this, "action_controller", 0,
                state -> PlayState.STOP).triggerableAnim("attack", attackAnimation));
    }

    private final class ChargeAttackGoal extends Goal {
        private ChargeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return targetPosition != null && !getMoveControl().hasWanted() && random.nextInt(7) == 0
                    && !isFinishing() && distanceToSqr(Vec3.atCenterOf(targetPosition)) > 4.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return isCharging() && targetPosition != null && !isFinishing();
        }

        @Override
        public void start() {
            refreshTargetPosition();
            moveTowardTarget(0.8D);
            setCharging(true);
        }

        @Override
        public void stop() {
            setCharging(false);
        }

        @Override
        public void tick() {
            refreshTargetPosition();
            if (targetPosition != null && distanceToSqr(Vec3.atCenterOf(targetPosition)) < 9.0D) {
                moveTowardTarget(0.8D);
            }
        }

        private void moveTowardTarget(double speed) {
            if (targetPosition != null) {
                getMoveControl().setWantedPosition(targetPosition.getX() + 0.5D,
                        targetPosition.getY() + 2.0D, targetPosition.getZ() + 0.5D, speed);
            }
        }
    }

    private final class RandomMoveGoal extends Goal {
        private RandomMoveGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !getMoveControl().hasWanted() && random.nextInt(7) == 0 && !isFinishing();
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            refreshTargetPosition();
            BlockPos origin = targetPosition == null ? blockPosition() : targetPosition;
            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos candidate = targetPosition == null
                        ? origin.offset(random.nextInt(15) - 7, random.nextInt(11) - 5,
                                random.nextInt(15) - 7)
                        : origin.offset(random.nextInt(6) - 2, random.nextInt(7) - 2,
                                random.nextInt(6) - 2);
                if (!level().isEmptyBlock(candidate)) {
                    continue;
                }
                getMoveControl().setWantedPosition(candidate.getX() + 0.5D, candidate.getY() + 0.5D,
                        candidate.getZ() + 0.5D, 1.0D);
                if (getTarget() == null) {
                    getLookControl().setLookAt(candidate.getX() + 0.5D, candidate.getY() + 0.5D,
                            candidate.getZ() + 0.5D, 180.0F, 20.0F);
                }
                return;
            }
        }
    }

    private static final class FlamMoveControl extends MoveControl {
        private FlamMoveControl(FlamEntity flam) {
            super(flam);
        }

        @Override
        public void tick() {
            FlamEntity flam = (FlamEntity) mob;
            if (flam.isFinishing()) {
                operation = Operation.WAIT;
                flam.setDeltaMovement(Vec3.ZERO);
                return;
            }
            if (operation != Operation.MOVE_TO) {
                return;
            }
            double x = wantedX - flam.getX();
            double y = wantedY - flam.getY();
            double z = wantedZ - flam.getZ();
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance < flam.getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                flam.setDeltaMovement(flam.getDeltaMovement().scale(0.5D));
                return;
            }
            flam.setDeltaMovement(flam.getDeltaMovement().add(
                    x / distance * 0.05D * speedModifier,
                    y / distance * 0.05D * speedModifier,
                    z / distance * 0.05D * speedModifier));
            LivingEntity target = flam.getTarget();
            double lookX = target == null ? flam.getDeltaMovement().x : target.getX() - flam.getX();
            double lookZ = target == null ? flam.getDeltaMovement().z : target.getZ() - flam.getZ();
            flam.setYRot(-((float) Mth.atan2(lookX, lookZ)) * Mth.RAD_TO_DEG);
            flam.yBodyRot = flam.getYRot();
        }
    }
}
