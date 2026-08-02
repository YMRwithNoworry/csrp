package alku.csrp.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class LongarmsEntity extends PrimitiveParasiteEntity {
    private static final int ATTACK_INTERVAL_TICKS = 10;
    private static final int ATTACKS_BEFORE_REST = 2;
    private static final int ATTACK_REST_TICKS = 100;
    private static final String ATTACKS_SINCE_REST_TAG = "MeleeAttacksSinceRest";
    private static final String REST_TICKS_TAG = "MeleeRestTicks";
    private static final EntityDataAccessor<Boolean> MELEE_RESTING = SynchedEntityData.defineId(
            LongarmsEntity.class, EntityDataSerializers.BOOLEAN);

    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private int shockwaveCooldown = 100;
    private int meleeAttacksSinceRest;
    private int meleeRestTicks;

    public LongarmsEntity(EntityType<? extends LongarmsEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 45.0).add(Attributes.ARMOR, 9.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MELEE_RESTING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new LongarmsRestGoal());
        goalSelector.addGoal(1, new ShockwaveGoal());
        goalSelector.addGoal(2, new LongarmsMeleeGoal());
    }

    @Override
    public void tick() {
        if (!level().isClientSide && meleeRestTicks > 0) {
            meleeRestTicks--;
            if (meleeRestTicks == 0) {
                entityData.set(MELEE_RESTING, false);
            }
        }
        super.tick();
        if (!level().isClientSide && isInWaterOrBubble() && getTarget() != null && tickCount % 20 == 0) {
            setDeltaMovement(getDeltaMovement().add(0.0, 0.095, 0.0));
        }
        if (shockwaveCooldown > 0) shockwaveCooldown--;
    }

    private void performAoeAttack(Entity center) {
        if (isRestingAfterMeleeAttacks()) {
            return;
        }
        triggerAnim("attack_controller", "attack");
        hurtNearby(center, 1.5, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), random.nextFloat() < 0.1F);
    }

    private boolean isRestingAfterMeleeAttacks() {
        return entityData.get(MELEE_RESTING);
    }

    private void recordMeleeAttack() {
        meleeAttacksSinceRest++;
        if (meleeAttacksSinceRest < ATTACKS_BEFORE_REST) {
            return;
        }
        meleeAttacksSinceRest = 0;
        meleeRestTicks = ATTACK_REST_TICKS;
        entityData.set(MELEE_RESTING, true);
        getNavigation().stop();
        setAggressive(false);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !isRestingAfterMeleeAttacks() && super.doHurtTarget(target);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(ATTACKS_SINCE_REST_TAG, meleeAttacksSinceRest);
        tag.putInt(REST_TICKS_TAG, meleeRestTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        meleeAttacksSinceRest = Math.clamp(tag.getInt(ATTACKS_SINCE_REST_TAG), 0, ATTACKS_BEFORE_REST - 1);
        meleeRestTicks = Math.max(0, tag.getInt(REST_TICKS_TAG));
        entityData.set(MELEE_RESTING, meleeRestTicks > 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    private PlayState movementAnimation(AnimationState<LongarmsEntity> state) {
        if (isRestingAfterMeleeAttacks()) return state.setAndContinue(IDLE);
        if (!state.isMoving()) return state.setAndContinue(IDLE);
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02 ? RUN : WALK);
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
        }

        @Override public void tick() {
            var target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (isRestingAfterMeleeAttacks()) {
                getNavigation().stop();
                return;
            }

            getNavigation().moveTo(target, 1.3D);
            if (cooldown > 0) cooldown--;
            if (distanceToSqr(target) <= 8.0D && cooldown == 0) {
                performAoeAttack(target);
                recordMeleeAttack();
                if (!isRestingAfterMeleeAttacks()) {
                    cooldown = ATTACK_INTERVAL_TICKS;
                }
            }
        }

        @Override
        public void stop() {
            if (getTarget() == null || !getTarget().isAlive()) {
                cooldown = 0;
            }
        }
    }

    private final class LongarmsRestGoal extends Goal {
        LongarmsRestGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
            getNavigation().stop();
            setAggressive(false);
        }

        @Override
        public void tick() {
            getNavigation().stop();
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }

        @Override
        public void stop() {
            LivingEntity target = getTarget();
            setAggressive(target != null && target.isAlive());
        }
    }

    private final class ShockwaveGoal extends Goal {
        private int charge;
        ShockwaveGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            return shockwaveCooldown == 0 && !isRestingAfterMeleeAttacks() && getTarget() != null && onGround();
        }
        @Override public boolean canContinueToUse() {
            return charge < 80 && !isRestingAfterMeleeAttacks() && getTarget() != null;
        }
        @Override public void start() { charge = 0; getNavigation().stop(); }
        @Override public void tick() { if (++charge == 60) hurtNearby(LongarmsEntity.this, 12.0, 4.5F, true); }
        @Override public void stop() { shockwaveCooldown = 100; charge = 0; }
    }
}
