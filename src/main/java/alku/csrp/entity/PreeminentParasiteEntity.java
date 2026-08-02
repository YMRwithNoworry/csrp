package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Shared implementation for the six legacy preeminent parasites. This tier
 * uses stronger adaptation and delegates its battlefield support to Succors.
 */
public final class PreeminentParasiteEntity extends PrimitiveParasiteEntity {
    private static final int MAX_ADAPTATION_HITS = 5;
    private static final int MAX_LEARNABLE_DAMAGE_SOURCES = 20;
    private static final int MAX_SUMMONED_SUCCORS = 3;
    private static final int SUCCOR_SUMMON_TIMER_MAX = 80;
    private static final int SUCCOR_SUMMON_PHASE = 40;
    private static final int SUCCOR_TRAVEL_TIMEOUT = 400;
    private static final int SUCCOR_STATIONARY_TIMEOUT = 20;
    private static final int SUCCOR_ACTIVATION_TICKS = 10;
    private static final int STEALTH_CHECK_INTERVAL = 20;
    private static final int STEALTH_CHECK_OFFSET = 10;
    private static final int STEALTH_CHECKS_REQUIRED = 3;
    private static final int STEALTH_EFFECT_TICKS = 25;
    private static final double STEALTH_HEALTH_THRESHOLD = 0.40D;
    private static final int MINIMUM_FLIGHT_HEIGHT = 10;
    private static final int MAXIMUM_FLIGHT_HEIGHT = 30;
    private static final float ADAPTATION_PER_HIT = 0.20F;
    private static final float ADAPTATION_LEARN_CHANCE = 1.0F;
    private static final float FIRE_SUPPRESSION_CHANCE = 0.30F;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation FLY = ParasiteAnimations.loop(this, "fly");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    private final Kind kind;
    private UUID summonerId;
    private int blockBreakCooldown;
    private int supportCooldown;
    private int stealthChecks;
    private int attackAnimationTicks;
    private int wraithProjectileCount;
    private int succorActionType;
    private int succorTravelTicks;
    private int succorStationaryTicks;
    private int succorActivationTicks;
    private BlockPos succorTargetPos;
    private boolean succorActionConsumed;
    private boolean stealthActive;
    private boolean charging;

    public PreeminentParasiteEntity(EntityType<? extends PreeminentParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
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
        return attributes;
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
                goalSelector.addGoal(1, new ColonySupportGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, false));
            }
            case HAUNTER -> {
                goalSelector.addGoal(1, new HaunterHomingBurstGoal());
                goalSelector.addGoal(2, new EvasiveDashGoal(100, 1.0D));
                goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.05D, false));
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
            case SUCCOR -> goalSelector.addGoal(1, new SuccorActionGoal());
        }
    }

    @Override
    public void tick() {
        super.tick();
        Kind activeKind = activeKind();
        if (activeKind.flying) {
            setNoGravity(true);
        }
        if (level().isClientSide) {
            return;
        }
        if (blockBreakCooldown > 0) {
            blockBreakCooldown--;
        }
        if (activeKind != Kind.SUCCOR) {
            supportCooldown++;
            if (supportCooldown > SUCCOR_SUMMON_TIMER_MAX) {
                supportCooldown = 0;
            }
        }
        if (attackAnimationTicks > 0) {
            attackAnimationTicks--;
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
        breakBlocksTowardsTarget(target, activeKind);
        if (activeKind != Kind.SUCCOR && supportCooldown == SUCCOR_SUMMON_PHASE) {
            trySummonSuccor(target);
        }
        if ((activeKind == Kind.BOGLE || activeKind == Kind.WRAITH)
                && Math.floorMod(tickCount, STEALTH_CHECK_INTERVAL) == STEALTH_CHECK_OFFSET) {
            applyFlyingAura();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 4.0F;
        }
        if (!level().isClientSide && isStealthKind()) {
            revealStealth();
        }
        return super.hurt(source, amount);
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
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("preeminent_support_cooldown", supportCooldown);
        tag.putBoolean("preeminent_succor_action", succorActionConsumed);
        tag.putInt("preeminent_succor_action_type", succorActionType);
        tag.putInt("preeminent_succor_travel", succorTravelTicks);
        tag.putInt("preeminent_succor_stationary", succorStationaryTicks);
        tag.putInt("preeminent_succor_activation", succorActivationTicks);
        if (succorTargetPos != null) {
            tag.putLong("preeminent_succor_target", succorTargetPos.asLong());
        }
        if (summonerId != null) {
            tag.putUUID("preeminent_summoner", summonerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        supportCooldown = Math.floorMod(tag.getInt("preeminent_support_cooldown"),
                SUCCOR_SUMMON_TIMER_MAX + 1);
        succorActionConsumed = tag.getBoolean("preeminent_succor_action");
        succorActionType = tag.getInt("preeminent_succor_action_type");
        succorTravelTicks = tag.getInt("preeminent_succor_travel");
        succorStationaryTicks = tag.getInt("preeminent_succor_stationary");
        succorActivationTicks = tag.getInt("preeminent_succor_activation");
        succorTargetPos = tag.contains("preeminent_succor_target")
                ? BlockPos.of(tag.getLong("preeminent_succor_target")) : null;
        summonerId = tag.hasUUID("preeminent_summoner") ? tag.getUUID("preeminent_summoner") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    public Kind getKind() {
        return activeKind();
    }

    public void setSummoner(PreeminentParasiteEntity summoner) {
        summonerId = summoner == null ? null : summoner.getUUID();
    }

    public boolean isSummonedBy(PreeminentParasiteEntity summoner) {
        return summoner != null && summonerId != null && summonerId.equals(summoner.getUUID());
    }

    private PlayState movementAnimation(AnimationState<PreeminentParasiteEntity> state) {
        if (activeKind().flying) {
            return state.setAndContinue(FLY);
        }
        if (attackAnimationTicks > 0) {
            return state.setAndContinue(ATTACK);
        }
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
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
            if (level().destroyBlock(candidate, true, this)) {
                blockBreakCooldown = 20;
            }
            return;
        }
    }

    private void trySummonSuccor(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int existingSuccors = 0;
        boolean teleportActionReserved = false;
        for (Entity entity : serverLevel.getAllEntities()) {
            if (!(entity instanceof PreeminentParasiteEntity succor) || succor.getKind() != Kind.SUCCOR
                    || !succor.isAlive() || !succor.isSummonedBy(this)) {
                continue;
            }
            existingSuccors++;
            teleportActionReserved |= succor.succorActionType == 3;
        }
        if (existingSuccors >= MAX_SUMMONED_SUCCORS) {
            return;
        }
        PreeminentParasiteEntity succor = ModEntities.SUCCOR.get().create(serverLevel);
        if (succor == null) {
            return;
        }
        Vec3 horizontalLook = getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D);
        if (horizontalLook.lengthSqr() > 0.001D) {
            horizontalLook = horizontalLook.normalize();
        }
        Vec3 spawn = position().subtract(horizontalLook.scale(4.0D)).add(0.0D, getEyeHeight(), 0.0D);
        succor.moveTo(spawn.x, spawn.y, spawn.z, getYRot(), 0.0F);
        succor.setSummoner(this);
        succor.succorTargetPos = target.blockPosition();
        int actionType = random.nextInt(3) + 1;
        if (actionType == 3 && (distanceToSqr(target) < 100.0D || !target.onGround()
                || teleportActionReserved)) {
            actionType = random.nextInt(2) + 1;
        }
        succor.succorActionType = actionType;
        succor.setTarget(target);
        var attackDamage = succor.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0D);
        }
        if (isInvisible()) {
            succor.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false), this);
        }
        serverLevel.addFreshEntity(succor);
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

    private PreeminentParasiteEntity resolveSummoner() {
        if (summonerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(summonerId);
        return entity instanceof PreeminentParasiteEntity preeminent && preeminent.isAlive() ? preeminent : null;
    }

    private void completeSuccorAction() {
        if (succorActionConsumed || level().isClientSide) {
            return;
        }
        succorActionConsumed = true;
        PreeminentParasiteEntity summoner = resolveSummoner();
        int action = succorActionType;
        if (action == 3) {
            boolean reachedTarget = succorTargetPos != null
                    && distanceToSqr(Vec3.atCenterOf(succorTargetPos)) < 16.0D;
            if (reachedTarget && summoner != null) {
                summoner.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                summoner.setDeltaMovement(Vec3.ZERO);
                discard();
                return;
            }
            action = random.nextInt(2) + 1;
        }
        if (action == 2 && summoner != null) {
            ScaryOrbEntity orb = new ScaryOrbEntity(ModEntities.SCARY_ORB.get(), level(), summoner);
            orb.setAnchor(position().add(0.0D, -3.0D, 0.0D));
            level().addFreshEntity(orb);
            discard();
            return;
        }
        hurtNearby(this, 4.0D, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), false);
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(4.2F);
        cloud.setDuration(300);
        cloud.setWaitTime(10);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 2, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 300, 2, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 2, false, false));
        level().addFreshEntity(cloud);
        discard();
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
        if (type == ModEntities.SUCCOR.get()) return Kind.SUCCOR;
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

    private final class ColonySupportGoal extends Goal {
        private int cooldown;

        private ColonySupportGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            return getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16.0D),
                    entity -> entity instanceof Parasite && entity.isAlive())) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2, false, false),
                        PreeminentParasiteEntity.this);
                ally.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 1, false, false),
                        PreeminentParasiteEntity.this);
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, false),
                        PreeminentParasiteEntity.this);
            }
            cooldown = 200;
        }
    }

    private final class HaunterHomingBurstGoal extends Goal {
        private int cooldown;
        private int shots;
        private int delay;

        private HaunterHomingBurstGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && hasLineOfSight(target) && distanceToSqr(target) >= 25.0D
                    && distanceToSqr(target) <= 1600.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && shots < 3;
        }

        @Override
        public void start() {
            shots = 0;
            delay = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (delay > 0) {
                delay--;
                return;
            }
            fireProjectile(target, ParasiteProjectileEntity.Mode.LIGHT, 1.30D, 45.0F, 1.25D, 90);
            shots++;
            delay = 6;
        }

        @Override
        public void stop() {
            cooldown = 90;
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
                    && distanceToSqr(target) <= 625.0D;
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
                setDeltaMovement(strafe.x, 0.35D, strafe.z);
            }
            cooldown = interval;
        }
    }

    private final class SuccorActionGoal extends Goal {
        private SuccorActionGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !succorActionConsumed && succorTargetPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (succorActionConsumed || succorTargetPos == null) {
                return;
            }
            if (succorActivationTicks > 0) {
                setDeltaMovement(Vec3.ZERO);
                succorActivationTicks++;
                if (succorActivationTicks >= SUCCOR_ACTIVATION_TICKS) {
                    completeSuccorAction();
                }
                return;
            }
            succorTravelTicks++;
            if (getX() == xo && getZ() == zo) {
                succorStationaryTicks++;
            }
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                getMoveControl().setWantedPosition(target.getX(), target.getY() + 0.5D, target.getZ(), 1.20D);
            } else {
                Vec3 targetCenter = Vec3.atCenterOf(succorTargetPos);
                getMoveControl().setWantedPosition(targetCenter.x, targetCenter.y, targetCenter.z, 1.20D);
            }
            if (distanceToSqr(Vec3.atCenterOf(succorTargetPos)) < 4.0D
                    || succorStationaryTicks > SUCCOR_STATIONARY_TIMEOUT
                    || succorTravelTicks > SUCCOR_TRAVEL_TIMEOUT || resolveSummoner() == null) {
                succorActivationTicks = 1;
                getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0D);
                setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    public enum Kind {
        BOGLE(true, 310.0D, 15.5D, 70.0D, 0.28D, 2.0D, 80.0D, 5.0D),
        CARRIER_COLONY(false, 390.0D, 15.5D, 45.0D, 0.242D, 2.0D, 80.0D, 5.0D),
        HAUNTER(false, 360.0D, 15.5D, 110.0D, 0.283D, 2.0D, 80.0D, 5.0D),
        BOMBER_HEAVY(true, 420.0D, 15.5D, 33.0D, 0.25D, 0.15D, 80.0D, 5.0D),
        WRAITH(true, 310.0D, 15.5D, 70.0D, 0.28D, 2.0D, 80.0D, 5.0D),
        SUCCOR(true, 85.0D, 2.0D, 1.0D, 0.32D, 1.0D, 80.0D, 2.0D);

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
