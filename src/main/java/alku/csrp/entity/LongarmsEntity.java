package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class LongarmsEntity extends PrimitiveParasiteEntity {
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_COMBAT = 1;
    private static final int STATUS_SPRINT = 2;
    private static final int STATUS_SHOCKWAVE = 10;
    private static final int ATTACK_INTERVAL_TICKS = 10;
    private static final int ATTACK_ANIMATION_COOLDOWN_TICKS = 100;
    private static final int STILL_ANIMATION_DELAY_TICKS = 25;
    private static final int ATTACKS_BEFORE_REST = 2;
    private static final int ATTACK_REST_TICKS = 100;
    private static final int SHOCKWAVE_COOLDOWN_TICKS = 100;
    private static final double SHOCKWAVE_MIN_DISTANCE_SQR = 4.0D;
    private static final double SHOCKWAVE_MAX_VERTICAL_DISTANCE = 4.0D;
    private static final double SHOCKWAVE_MIN_VERTICAL_DISTANCE = -2.0D;
    private static final int BLOCK_BREAK_COOLDOWN_TICKS = 60;
    private static final float BLOCK_BREAK_HARDNESS = 1.0F;
    private static final float FIRE_DAMAGE_MULTIPLIER = 4.0F;
    private static final float RANDOM_BLOCK_CHANCE = 0.1F;
    private static final float MISSING_HEALTH_DAMAGE_FACTOR = 0.5F;
    private static final String ATTACKS_SINCE_REST_TAG = "MeleeAttacksSinceRest";
    private static final String REST_TICKS_TAG = "MeleeRestTicks";
    private static final EntityDataAccessor<Boolean> MELEE_RESTING = SynchedEntityData.defineId(
            LongarmsEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            LongarmsEntity.class, EntityDataSerializers.INT);

    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "get_attack_timer");
    private final RawAnimation STILL_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_still_ani_1");
    private final RawAnimation STILL_ATTACK = ParasiteAnimations.play(this,
            "get_attack_timer.get_still_ani_1");
    private final RawAnimation COMBAT_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation COMBAT_STILL_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation COMBAT_STILL_ATTACK = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation SPRINT_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation SPRINT_ATTACK = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_2");
    private int shockwaveCooldown = SHOCKWAVE_COOLDOWN_TICKS;
    private int blockBreakCooldown;
    private int meleeAttacksSinceRest;
    private int meleeRestTicks;
    private int attackAnimationCooldown;
    private int stillAnimationTicks;

    public LongarmsEntity(EntityType<? extends LongarmsEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 45.0).add(Attributes.ARMOR, 9.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public boolean onClimbable() {
        return horizontalCollision || super.onClimbable();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MELEE_RESTING, false);
        builder.define(PARASITE_STATUS, STATUS_IDLE);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new LongarmsRecoveryGoal());
        goalSelector.addGoal(1, new ShockwaveGoal());
        goalSelector.addGoal(2, new LongarmsMeleeGoal());
    }

    @Override
    public void tick() {
        if (getX() == xo && getZ() == zo) {
            stillAnimationTicks++;
        } else {
            stillAnimationTicks = 0;
        }
        boolean wasResting = !level().isClientSide && isRestingAfterMeleeAttacks();
        if (wasResting) {
            getNavigation().stop();
            setAggressive(false);
        }
        super.tick();
        if (wasResting) {
            getNavigation().stop();
            setAggressive(false);
            if (--meleeRestTicks <= 0) {
                meleeRestTicks = 0;
                entityData.set(MELEE_RESTING, false);
            }
        }
        if (!level().isClientSide && !wasResting && !isRestingAfterMeleeAttacks()
                && isInWaterOrBubble() && getTarget() != null && tickCount % 20 == 0) {
            setDeltaMovement(getDeltaMovement().add(0.0, 0.095, 0.0));
        }
        if (!level().isClientSide) {
            if (attackAnimationCooldown > 0) {
                attackAnimationCooldown--;
            }
            if (blockBreakCooldown > 0) {
                blockBreakCooldown--;
            }
            LivingEntity target = getTarget();
            if ((target == null || !target.isAlive()) && getParasiteStatus() != STATUS_SHOCKWAVE) {
                setParasiteStatus(STATUS_IDLE);
            }
            if (!wasResting && !isRestingAfterMeleeAttacks() && target != null && target.isAlive()) {
                breakSoftBlockTowards(target);
                if (shockwaveCooldown > 0 && isValidShockwaveTarget(target)) {
                    shockwaveCooldown--;
                }
            }
        }
    }

    private boolean specialMovesEnabled() {
        return level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).specialMoves();
    }

    private boolean isValidShockwaveTarget(LivingEntity target) {
        if (!specialMovesEnabled() || target == null || !target.isAlive() || !onGround()
                || !hasLineOfSight(target)) {
            return false;
        }
        double verticalDistance = target.getY() - getY();
        double maximumDistance = getAttributeValue(Attributes.FOLLOW_RANGE) * 0.7D;
        double distance = distanceToSqr(target);
        return verticalDistance >= SHOCKWAVE_MIN_VERTICAL_DISTANCE
                && verticalDistance <= SHOCKWAVE_MAX_VERTICAL_DISTANCE
                && distance >= SHOCKWAVE_MIN_DISTANCE_SQR
                && distance <= maximumDistance * maximumDistance;
    }

    private void breakSoftBlockTowards(LivingEntity target) {
        if (blockBreakCooldown > 0 || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 horizontal = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 0.01D) {
            return;
        }
        horizontal = horizontal.normalize();
        BlockPos origin = BlockPos.containing(getX() + horizontal.x,
                getY() + getBbHeight() * 0.5D, getZ() + horizontal.z);
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                    || hardness < 0.0F || hardness > BLOCK_BREAK_HARDNESS) {
                continue;
            }
            if (ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this)) {
                blockBreakCooldown = BLOCK_BREAK_COOLDOWN_TICKS;
            }
            return;
        }
    }

    private void performAoeAttack(Entity center) {
        if (isRestingAfterMeleeAttacks()) {
            return;
        }
        triggerAttackAnimation();
        playSound(ModSounds.get("mob.swipe"), 2.0F, 1.0F);
        if (center instanceof LivingEntity primaryTarget && isValidParasiteTarget(primaryTarget)
                && hasLineOfSight(primaryTarget)) {
            primaryTarget.invulnerableTime = 0;
            hitLongarmsTarget(primaryTarget, false);
        }
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(1.5D), this::isValidParasiteTarget)) {
            if (target == center) {
                continue;
            }
            if (hasLineOfSight(target)) {
                hitLongarmsTarget(target, false);
            }
        }
    }

    private float currentMeleeDamage() {
        float baseDamage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (getHealth() >= getMaxHealth()) {
            return baseDamage;
        }
        float healthRatio = Math.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F);
        return baseDamage + baseDamage * (1.0F - healthRatio * MISSING_HEALTH_DAMAGE_FACTOR);
    }

    boolean hitWithShockwave(LivingEntity target) {
        return hitLongarmsTarget(target, true);
    }

    private boolean hitLongarmsTarget(LivingEntity target, boolean shockwave) {
        if (!isValidParasiteTarget(target)) {
            return false;
        }
        if (shockwave) {
            target.invulnerableTime = 0;
        }
        if (!target.hurt(damageSources().mobAttack(this), currentMeleeDamage())) {
            return false;
        }
        target.knockback(0.4D, getX() - target.getX(), getZ() - target.getZ());
        if (random.nextFloat() < 0.1F) {
            double x = target.getX() - getX();
            double z = target.getZ() - getZ();
            double length = Math.max(0.0001D, Math.sqrt(x * x + z * z));
            target.push(x / length * 0.4D, target instanceof net.minecraft.world.entity.player.Player
                    ? 0.525D : 1.05D, z / length * 0.4D);
        }
        if (shockwave) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.64645D, 0.0D));
            target.hurtMarked = true;
        }
        return true;
    }

    private void spawnShockwave(LivingEntity target) {
        ShockwaveEntity shockwave = ModEntities.SHOCKWAVE.get().create(level());
        if (shockwave == null) {
            return;
        }
        shockwave.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        shockwave.configure(this, target);
        level().addFreshEntity(shockwave);
        triggerAttackAnimation();
        playSound(ModSounds.get("mob.swipe"), 2.0F, 1.0F);
    }

    private boolean isRestingAfterMeleeAttacks() {
        return entityData.get(MELEE_RESTING);
    }

    private int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    private void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    private boolean isStillAnimation() {
        return stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS;
    }

    private void triggerAttackAnimation() {
        String trigger = switch (getParasiteStatus()) {
            case STATUS_COMBAT -> "combat_still_attack";
            case STATUS_SPRINT -> "sprint_attack";
            default -> isStillAnimation() ? "still_attack" : "attack";
        };
        triggerAnim("attack_controller", trigger);
    }

    private void stopForMeleeRecovery() {
        getNavigation().stop();
        setJumping(false);
        setAggressive(false);
    }

    private void recordMeleeAttack() {
        meleeAttacksSinceRest++;
        if (meleeAttacksSinceRest < ATTACKS_BEFORE_REST) {
            return;
        }
        meleeAttacksSinceRest = 0;
        meleeRestTicks = ATTACK_REST_TICKS;
        entityData.set(MELEE_RESTING, true);
        stopForMeleeRecovery();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !isRestingAfterMeleeAttacks() && target instanceof LivingEntity living
                && hitLongarmsTarget(living, false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0.0F && tickCount > 5
                && !source.is(DamageTypeTags.BYPASSES_ARMOR)
                && random.nextFloat() < RANDOM_BLOCK_CHANCE) {
            playSound(SoundEvents.SHIELD_BLOCK, 0.7F, 1.15F + random.nextFloat() * 0.15F);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() * 0.6D, getZ(),
                        6, 0.08D, 0.08D, 0.08D, 0.01D);
            }
            if (source.getEntity() instanceof LivingEntity attacker) {
                setTarget(attacker);
            }
            return false;
        }
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE)
                ? amount * FIRE_DAMAGE_MULTIPLIER : amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(ATTACKS_SINCE_REST_TAG, meleeAttacksSinceRest);
        tag.putInt(REST_TICKS_TAG, meleeRestTicks);
        tag.putInt("ParasiteStatus", getParasiteStatus());
        tag.putInt("AttackAnimationCooldown", attackAnimationCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        meleeAttacksSinceRest = Math.clamp(tag.getInt(ATTACKS_SINCE_REST_TAG), 0, ATTACKS_BEFORE_REST - 1);
        meleeRestTicks = Math.max(0, tag.getInt(REST_TICKS_TAG));
        entityData.set(MELEE_RESTING, meleeRestTicks > 0);
        setParasiteStatus(tag.getInt("ParasiteStatus"));
        attackAnimationCooldown = Math.max(0, tag.getInt("AttackAnimationCooldown"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("still_attack", STILL_ATTACK)
                .triggerableAnim("combat_still_attack", COMBAT_STILL_ATTACK)
                .triggerableAnim("sprint_attack", SPRINT_ATTACK));
    }

    private PlayState movementAnimation(AnimationState<LongarmsEntity> state) {
        boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
        return switch (getParasiteStatus()) {
            case STATUS_COMBAT -> state.setAndContinue(moving ? COMBAT_WALK : COMBAT_STILL_IDLE);
            case STATUS_SPRINT -> state.setAndContinue(moving ? SPRINT_WALK : STILL_IDLE);
            case STATUS_SHOCKWAVE -> state.setAndContinue(STILL_IDLE);
            default -> state.setAndContinue(moving ? LIMB_SWING : STILL_IDLE);
        };
    }

    private final class LongarmsMeleeGoal extends Goal {
        private int cooldown;

        LongarmsMeleeGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return !isRestingAfterMeleeAttacks() && target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            setAggressive(true);
            setParasiteStatus(STATUS_SPRINT);
        }

        @Override public void tick() {
            var target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (isRestingAfterMeleeAttacks()) {
                stopForMeleeRecovery();
                return;
            }

            setAggressive(true);
            boolean sprinting = distanceToSqr(target) > 64.0D || attackAnimationCooldown == 0;
            setParasiteStatus(sprinting ? STATUS_SPRINT : STATUS_COMBAT);
            getNavigation().moveTo(target, sprinting ? 1.3D : 1.0D);
            if (cooldown > 0) cooldown--;
            if (distanceToSqr(target) <= 8.0D && cooldown == 0) {
                performAoeAttack(target);
                cooldown = ATTACK_INTERVAL_TICKS;
                attackAnimationCooldown = ATTACK_ANIMATION_COOLDOWN_TICKS;
                recordMeleeAttack();
            }
        }

        @Override
        public void stop() {
            if (getTarget() == null || !getTarget().isAlive()) {
                cooldown = 0;
                setParasiteStatus(STATUS_IDLE);
            }
        }
    }

    /** Mirrors the legacy base parasite wait goal by reserving all movement controls during recovery. */
    private final class LongarmsRecoveryGoal extends Goal {
        LongarmsRecoveryGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isRestingAfterMeleeAttacks();
        }

        @Override
        public boolean canContinueToUse() {
            return isRestingAfterMeleeAttacks();
        }

        @Override
        public void start() {
            stopForMeleeRecovery();
        }

        @Override
        public void tick() {
            stopForMeleeRecovery();
        }

        @Override
        public void stop() {
            setAggressive(false);
        }
    }

    private final class ShockwaveGoal extends Goal {
        private int charge;
        ShockwaveGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            return shockwaveCooldown <= 0 && !isRestingAfterMeleeAttacks()
                    && isValidShockwaveTarget(getTarget());
        }
        @Override public boolean canContinueToUse() {
            return charge < 80 && !isRestingAfterMeleeAttacks()
                    && isValidShockwaveTarget(getTarget());
        }
        @Override public void start() {
            charge = 0;
            setParasiteStatus(STATUS_SHOCKWAVE);
            getNavigation().stop();
            setAggressive(false);
            playSound(ModSounds.get("shyco.special"), 4.0F,
                    1.8F + random.nextFloat() * 0.4F);
        }
        @Override public void tick() {
            LivingEntity target = getTarget();
            if (target != null) getLookControl().setLookAt(target, 30.0F, 30.0F);
            charge++;
            if (charge <= 40 && charge % 4 == 0 && level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.2D, getZ(),
                        4, 0.45D, 0.1D, 0.45D, 0.02D);
            }
            if (charge == 60 && target != null) {
                spawnShockwave(target);
            }
        }
        @Override public void stop() {
            shockwaveCooldown = SHOCKWAVE_COOLDOWN_TICKS;
            charge = 0;
            setAggressive(false);
            setParasiteStatus(STATUS_IDLE);
        }
    }
}
