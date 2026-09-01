package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelAnimationState;
import alku.csrp.animation.CitadelPlayState;
import alku.csrp.animation.CitadelRawAnimation;

import java.util.EnumSet;

/**
 * Legacy Cruxa: a heavy crude parasite that sweeps groups, hurls nearby blocks, and grows stronger from kills.
 */
public final class CruxEntity extends CrudeParasiteEntity {
    private static final EntityDataAccessor<Byte> ANIMATION_STATUS =
            SynchedEntityData.defineId(CruxEntity.class, EntityDataSerializers.BYTE);
    private static final double BASE_ATTACK_DAMAGE = 20.0;
    private static final int DAMAGE_STACK_CAP = 10;
    private static final double DAMAGE_GAIN_PER_KILL = 0.12;
    private static final float THROW_BASE_DAMAGE = 10.0F;
    private static final double FALLING_BLOCK_DRAG = 0.98;
    private static final double FALLING_BLOCK_GRAVITY = 0.04;
    private static final String DAMAGE_STACKS_TAG = "crux_damage_stacks";
    private static final int STILL_ANIMATION_DELAY_TICKS = 25;
    private static final byte STATUS_IDLE = 0;
    private static final byte STATUS_APPROACHING = 1;
    private static final byte STATUS_SPRINTING = 2;
    private static final byte STATUS_THROWING = 3;
    private final CitadelRawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final CitadelRawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");
    private final CitadelRawAnimation APPROACH_AGE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final CitadelRawAnimation APPROACH_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final CitadelRawAnimation APPROACH_STILL = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final CitadelRawAnimation SPRINT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");

    private int attackCooldown;
    private int throwCooldown;
    private int damageStacks;
    private int stillAnimationTicks;
    private boolean throwingBlock;

    public CruxEntity(EntityType<? extends CruxEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 70.0).add(Attributes.ARMOR, 9.0)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIMATION_STATUS, STATUS_IDLE);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new BlockThrowGoal());
        goalSelector.addGoal(2, new CruxMeleeGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (ParasiteAnimations.isMoving(this, true)) {
            stillAnimationTicks = 0;
        } else {
            stillAnimationTicks++;
        }
        if (!level().isClientSide) {
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            if (throwCooldown > 0) {
                throwCooldown--;
            }
            if (!throwingBlock) {
                setAnimationStatus(getTarget() == null ? STATUS_IDLE
                        : isSprinting() ? STATUS_SPRINTING : STATUS_APPROACHING);
            }
        }
    }

    @Override
    protected void onParasiteKill(net.minecraft.server.level.ServerLevel level, LivingEntity victim, int kills) {
        if (damageStacks >= DAMAGE_STACK_CAP) {
            return;
        }

        damageStacks++;
        AttributeInstance attackDamage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(BASE_ATTACK_DAMAGE * (1.0 + DAMAGE_GAIN_PER_KILL * damageStacks));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(DAMAGE_STACKS_TAG, damageStacks);
        tag.putInt("crux_throw_cooldown", throwCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(DAMAGE_STACKS_TAG, Tag.TAG_INT)) {
            damageStacks = Math.min(DAMAGE_STACK_CAP, Math.max(0, tag.getInt(DAMAGE_STACKS_TAG)));
            AttributeInstance attackDamage = getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                attackDamage.setBaseValue(BASE_ATTACK_DAMAGE * (1.0 + DAMAGE_GAIN_PER_KILL * damageStacks));
            }
        }
        if (tag.contains("crux_throw_cooldown", Tag.TAG_INT)) {
            throwCooldown = Math.max(0, tag.getInt("crux_throw_cooldown"));
        }
    }

    @Override
    public void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers) {
        controllers.add(new CitadelAnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        CitadelAnimationController<CruxEntity> actions = new CitadelAnimationController<>(this, "action_controller", 0,
                state -> CitadelPlayState.STOP);
        registerAction(actions, "get_attack_timer_m");
        registerAction(actions, "get_attack_timer_m.get_parasite_status_1");
        registerAction(actions, "get_attack_timer_m.get_parasite_status_1.get_still_ani_1");
        registerAction(actions, "get_attack_timer_r");
        registerAction(actions, "get_attack_timer_r.get_parasite_status_1");
        registerAction(actions, "get_attack_timer_r.get_parasite_status_1.get_still_ani_1");
        controllers.add(actions);
    }

    private CitadelPlayState movementAnimation(CitadelAnimationState<CruxEntity> state) {
        boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
        return switch (animationStatus()) {
            case STATUS_APPROACHING -> state.setAndContinue(isStillAnimation()
                    ? APPROACH_STILL : moving ? APPROACH_LIMB : APPROACH_AGE);
            case STATUS_SPRINTING -> state.setAndContinue(moving ? SPRINT_LIMB : AGE_IN_TICKS);
            case STATUS_THROWING -> state.setAndContinue(AGE_IN_TICKS);
            default -> state.setAndContinue(moving ? LIMB_SWING : AGE_IN_TICKS);
        };
    }

    private void registerAction(CitadelAnimationController<CruxEntity> controller, String functionName) {
        controller.triggerableAnim(functionName, ParasiteAnimations.play(this, functionName));
    }

    private void triggerOriginalAction(String timerFunction) {
        byte status = animationStatus();
        String functionName = timerFunction;
        if (status == STATUS_APPROACHING) {
            functionName += ".get_parasite_status_1";
        }
        if (status == STATUS_APPROACHING && isStillAnimation()) {
            functionName += ".get_still_ani_1";
        }
        triggerAnim("action_controller", functionName);
    }

    private byte animationStatus() {
        return entityData.get(ANIMATION_STATUS);
    }

    private void setAnimationStatus(byte status) {
        entityData.set(ANIMATION_STATUS, status);
    }

    private boolean isStillAnimation() {
        return stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS;
    }

    private boolean performAoeAttack(LivingEntity target) {
        triggerOriginalAction("get_attack_timer_m");
        AABB attackArea = target.getBoundingBox().inflate(1.0);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), attackArea);
        boolean hit = false;
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, attackArea, this::isValidParasiteTarget)) {
            hit |= victim.hurt(damageSources().mobAttack(this), damage);
        }
        attackCooldown = 20;
        return hit;
    }

    private boolean throwBlockAt(LivingEntity target, BlockPos source) {
        if (level().isClientSide || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }

        BlockState state = level().getBlockState(source);
        float hardness = state.getDestroySpeed(level(), source);
        if (!isThrowableBlock(state, hardness)) {
            return false;
        }

        FallingBlockEntity block = FallingBlockEntity.fall(level(), source, state);
        Vec3 launchPosition = launchPosition(target);
        block.setPos(launchPosition.x, launchPosition.y, launchPosition.z);
        block.disableDrop();
        block.setDeltaMovement(throwVelocity(launchPosition, target));

        float damage = THROW_BASE_DAMAGE + Math.max(0.0F, hardness);
        CruxThrownBlockDamageEntity damageProxy = ModEntities.CRUX_BLOCK_DAMAGE.get().create(level());
        if (damageProxy != null) {
            damageProxy.configure(this, block, damage);
            level().addFreshEntity(damageProxy);
        }
        return true;
    }

    private Vec3 launchPosition(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(position());
        Vec3 horizontalDirection = new Vec3(toTarget.x, 0.0, toTarget.z).normalize();
        return getEyePosition().add(horizontalDirection.scale(1.25)).subtract(0.0, 0.7, 0.0);
    }

    private Vec3 throwVelocity(Vec3 launchPosition, LivingEntity target) {
        Vec3 initialTargetPosition = target.getEyePosition();
        Vec3 initialDelta = initialTargetPosition.subtract(launchPosition);
        double initialHorizontalDistance = Math.max(0.001,
                Math.sqrt(initialDelta.x * initialDelta.x + initialDelta.z * initialDelta.z));
        double flightTicks = Mth.clamp(initialHorizontalDistance / 0.9, 12.0, 32.0);
        Vec3 targetPosition = initialTargetPosition.add(target.getDeltaMovement().scale(flightTicks));
        Vec3 toTarget = targetPosition.subtract(launchPosition);
        double horizontalDistance = Math.max(0.001, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z));
        double dragDistance = (1.0 - Math.pow(FALLING_BLOCK_DRAG, flightTicks)) / (1.0 - FALLING_BLOCK_DRAG);
        double horizontalSpeed = horizontalDistance / dragDistance;
        double gravityDistance = FALLING_BLOCK_GRAVITY / (1.0 - FALLING_BLOCK_DRAG)
                * (flightTicks - FALLING_BLOCK_DRAG * dragDistance);
        double verticalSpeed = (toTarget.y + gravityDistance) / dragDistance;
        return new Vec3(toTarget.x / horizontalDistance * horizontalSpeed, verticalSpeed,
                toTarget.z / horizontalDistance * horizontalSpeed);
    }

    private BlockPos findThrowableBlock() {
        BlockPos origin = blockPosition();
        for (int attempt = 0; attempt < 8; attempt++) {
            int offsetX = random.nextInt(5) - 2;
            int offsetZ = random.nextInt(5) - 2;
            if (offsetX == 0 && offsetZ == 0) {
                continue;
            }
            for (int offsetY = 2; offsetY >= -4; offsetY--) {
                BlockPos candidate = origin.offset(offsetX, offsetY, offsetZ);
                BlockState state = level().getBlockState(candidate);
                float hardness = state.getDestroySpeed(level(), candidate);
                if (isThrowableBlock(state, hardness)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isThrowableBlock(BlockState state, float hardness) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && hardness > 0.0F && hardness <= 50.0F;
    }

    private final class CruxMeleeGoal extends Goal {
        private CruxMeleeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            setSprinting(true);
            setAnimationStatus(STATUS_SPRINTING);
        }

        @Override
        public void stop() {
            setSprinting(false);
            if (!throwingBlock) {
                setAnimationStatus(getTarget() == null ? STATUS_IDLE : STATUS_APPROACHING);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            getNavigation().moveTo(target, 1.3);
            double reach = getBbWidth() + target.getBbWidth() + 1.0;
            if (attackCooldown == 0 && distanceToSqr(target) <= reach * reach) {
                performAoeAttack(target);
            }
        }
    }

    private final class BlockThrowGoal extends Goal {
        private int windupTicks;
        private BlockPos throwSource;

        private BlockThrowGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            throwSource = null;
            LivingEntity target = getTarget();
            if (target == null || throwCooldown > 0
                    || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                return false;
            }
            double verticalOffset = target.getY() - getY();
            double distance = distanceToSqr(target);
            if (distance < 144.0 || distance > 1024.0 || verticalOffset < 0.0 || verticalOffset > 3.0) {
                return false;
            }
            throwSource = findThrowableBlock();
            return throwSource != null;
        }

        @Override
        public boolean canContinueToUse() {
            return windupTicks < 20 && getTarget() != null && throwSource != null;
        }

        @Override
        public void start() {
            windupTicks = 0;
            throwingBlock = true;
            setAnimationStatus(STATUS_THROWING);
            getNavigation().stop();
            triggerOriginalAction("get_attack_timer_r");
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (++windupTicks == 12) {
                throwBlockAt(target, throwSource);
            }
        }

        @Override
        public void stop() {
            throwCooldown = 100;
            throwSource = null;
            throwingBlock = false;
            setAnimationStatus(getTarget() == null ? STATUS_IDLE
                    : isSprinting() ? STATUS_SPRINTING : STATUS_APPROACHING);
        }
    }
}
