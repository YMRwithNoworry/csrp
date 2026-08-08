package alku.csrp.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/** Shared staged digging movement used by the original and adapted Zaa and Wymo forms. */
public abstract class BurrowingVariantEntity extends PrimitiveParasiteEntity {
    private static final byte BURROW_NONE = 0;
    private static final byte BURROW_DIVING = 1;
    private static final byte BURROW_UNDERGROUND = 2;
    private static final byte BURROW_EMERGING = 3;
    private static final int DIVE_TICKS = 30;
    private static final int UNDERGROUND_TICKS = 20;
    private static final int EMERGE_TICKS = 30;
    private static final int BODY_ATTACK_ANIMATION_TICKS = 15;
    private static final EntityDataAccessor<Byte> BURROW_PHASE = SynchedEntityData.defineId(
            BurrowingVariantEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> BURROW_DEPTH = SynchedEntityData.defineId(
            BurrowingVariantEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> BODY_NUMBER = SynchedEntityData.defineId(
            BurrowingVariantEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> BODY_TAIL = SynchedEntityData.defineId(
            BurrowingVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BODY_ATTACK_TICKS = SynchedEntityData.defineId(
            BurrowingVariantEntity.class, EntityDataSerializers.INT);

    private int burrowTicks;
    private int burrowSkillTicks;
    private boolean movedUnderground;
    private float previousBurrowDepth;
    private UUID bodyPredecessor;
    private boolean bodyChainInitialized;

    protected BurrowingVariantEntity(EntityType<? extends BurrowingVariantEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROW_PHASE, BURROW_NONE);
        builder.define(BURROW_DEPTH, 0.0F);
        builder.define(BODY_NUMBER, (byte) 0);
        builder.define(BODY_TAIL, false);
        builder.define(BODY_ATTACK_TICKS, 0);
    }

    @Override
    public void tick() {
        previousBurrowDepth = entityData.get(BURROW_DEPTH);
        super.tick();
        if (!level().isClientSide) {
            if (entityData.get(BODY_ATTACK_TICKS) > 0) {
                entityData.set(BODY_ATTACK_TICKS, entityData.get(BODY_ATTACK_TICKS) - 1);
            }
            updateBodyChain();
            if (!isRemoved() && getBodyNumber() == 1 && tickCount % 21 == 10 && !isBurrowing()) {
                bodyPartEffect();
            }
        }
        if (level().isClientSide || !supportsBurrowing() || getBodyNumber() > 0) {
            return;
        }
        if (!isBurrowing()) {
            burrowSkillTicks = Math.min(burrowSkillCooldownTicks(), burrowSkillTicks + 1);
            return;
        }
        updateBurrowMovement();
    }

    protected abstract boolean supportsBurrowing();

    protected abstract int burrowSkillCooldownTicks();

    protected abstract SoundEvent burrowSound();

    protected int bodySegmentCount() {
        return 0;
    }

    public final int getBodyNumber() {
        return Byte.toUnsignedInt(entityData.get(BODY_NUMBER));
    }

    public final boolean isBodyTail() {
        return entityData.get(BODY_TAIL);
    }

    public final boolean isBodyAttackAnimating() {
        return entityData.get(BODY_ATTACK_TICKS) > 0;
    }

    private void updateBodyChain() {
        if (!(level() instanceof ServerLevel serverLevel) || !supportsBurrowing()) {
            return;
        }
        if (getBodyNumber() == 0) {
            if (!bodyChainInitialized) {
                spawnBodyChain(serverLevel);
                bodyChainInitialized = true;
            }
            return;
        }

        setNoAi(true);
        setTarget(null);
        getNavigation().stop();
        Entity predecessor = bodyPredecessor == null ? null : serverLevel.getEntity(bodyPredecessor);
        if (!(predecessor instanceof BurrowingVariantEntity previous) || !predecessor.isAlive()) {
            discard();
            return;
        }
        entityData.set(BURROW_PHASE, previous.entityData.get(BURROW_PHASE));
        entityData.set(BURROW_DEPTH, previous.entityData.get(BURROW_DEPTH));
        Vec3 direction = previous.getDeltaMovement();
        if (direction.horizontalDistanceSqr() < 0.001D && previous.bodyPredecessor != null) {
            Entity beforePrevious = serverLevel.getEntity(previous.bodyPredecessor);
            if (beforePrevious != null) {
                direction = previous.position().subtract(beforePrevious.position());
            }
        }
        if (direction.horizontalDistanceSqr() < 0.001D) {
            float yaw = previous.getYRot() * Mth.DEG_TO_RAD;
            direction = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        } else {
            direction = direction.normalize();
        }
        double spacing = bodyFollowDistance();
        Vec3 destination = previous.position().subtract(direction.scale(spacing));
        if (distanceToSqr(previous) > 9.0D) {
            setPos(previous.getX(), previous.getY(), previous.getZ());
        } else {
            setPos(Mth.lerp(0.45D, getX(), destination.x),
                    Mth.lerp(0.45D, getY(), destination.y),
                    Mth.lerp(0.45D, getZ(), destination.z));
        }
        setYRot(previous.getYRot());
        setYBodyRot(previous.yBodyRot);
        setYHeadRot(previous.getYHeadRot());
        setDeltaMovement(Vec3.ZERO);
    }

    private void spawnBodyChain(ServerLevel serverLevel) {
        int count = bodySegmentCount();
        BurrowingVariantEntity previous = this;
        for (int index = 1; index <= count; index++) {
            Entity created = getType().create(serverLevel);
            if (!(created instanceof BurrowingVariantEntity segment)) {
                return;
            }
            segment.entityData.set(BODY_NUMBER, (byte) index);
            segment.entityData.set(BODY_TAIL, index == count);
            segment.bodyPredecessor = previous.getUUID();
            segment.bodyChainInitialized = true;
            segment.setPersistenceRequired();
            segment.moveTo(previous.getX(), previous.getY(), previous.getZ(), previous.getYRot(), 0.0F);
            serverLevel.addFreshEntity(segment);
            previous = segment;
        }
    }

    protected final Goal createBurrowMovementGoal() {
        return new BurrowMovementGoal();
    }

    protected double bodyFollowDistance() {
        return Math.max(0.55D, getBbWidth() * 0.65D);
    }

    protected void bodyPartEffect() {
    }

    protected final void startBodyAttackAnimation() {
        entityData.set(BODY_ATTACK_TICKS, BODY_ATTACK_ANIMATION_TICKS);
    }

    private boolean canStartBurrowing() {
        LivingEntity target = getTarget();
        if (!supportsBurrowing() || isBurrowing() || burrowSkillTicks < burrowSkillCooldownTicks()
                || !onGround() || target == null || !target.isAlive()) {
            return false;
        }
        double distance = distanceToSqr(target);
        return distance > 196.0D || !hasLineOfSight(target) && distance > 49.0D;
    }

    private void beginBurrowing() {
        burrowTicks = 0;
        movedUnderground = false;
        entityData.set(BURROW_PHASE, BURROW_DIVING);
        entityData.set(BURROW_DEPTH, 0.0F);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        playSound(burrowSound(), 2.0F, getVoicePitch());
    }

    private void updateBurrowMovement() {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        burrowTicks++;

        byte phase = entityData.get(BURROW_PHASE);
        if (phase == BURROW_DIVING) {
            entityData.set(BURROW_DEPTH, Math.min(1.0F, burrowTicks / (float) DIVE_TICKS));
            spawnBurrowParticles();
            if (burrowTicks >= DIVE_TICKS) {
                burrowTicks = 0;
                entityData.set(BURROW_DEPTH, 1.0F);
                entityData.set(BURROW_PHASE, BURROW_UNDERGROUND);
            }
            return;
        }
        if (phase == BURROW_UNDERGROUND) {
            entityData.set(BURROW_DEPTH, 1.0F);
            if (!movedUnderground) {
                movedUnderground = moveNearTargetUnderground();
            }
            if (burrowTicks >= UNDERGROUND_TICKS) {
                burrowTicks = 0;
                entityData.set(BURROW_PHASE, BURROW_EMERGING);
            }
            return;
        }

        entityData.set(BURROW_DEPTH, Math.max(0.0F, 1.0F - burrowTicks / (float) EMERGE_TICKS));
        spawnBurrowParticles();
        if (burrowTicks >= EMERGE_TICKS) {
            LivingEntity target = getTarget();
            burrowTicks = 0;
            burrowSkillTicks = 0;
            movedUnderground = false;
            entityData.set(BURROW_DEPTH, 0.0F);
            entityData.set(BURROW_PHASE, BURROW_NONE);
            if (target != null && target.isAlive() && distanceToSqr(target) <= 9.0D) {
                doHurtTarget(target);
            }
        }
    }

    private boolean moveNearTargetUnderground() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        BlockPos destination = findBurrowDestination(target);
        if (destination == null) {
            return false;
        }
        teleportTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D);
        return true;
    }

    private BlockPos findBurrowDestination(LivingEntity target) {
        BlockPos targetPos = target.blockPosition();
        for (int attempt = 0; attempt < 5; attempt++) {
            int xOffset = 1 + random.nextInt(4);
            int zOffset = 1 + random.nextInt(4);
            int x = targetPos.getX() + (random.nextBoolean() ? xOffset : -xOffset);
            int z = targetPos.getZ() + (random.nextBoolean() ? zOffset : -zOffset);
            for (int yOffset = 5; yOffset >= -5; yOffset--) {
                BlockPos candidate = new BlockPos(x, targetPos.getY() + yOffset, z);
                if (isValidBurrowDestination(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isValidBurrowDestination(BlockPos position) {
        if (!hasBurrowableGround(position) || !level().getFluidState(position).isEmpty()) {
            return false;
        }
        Vec3 offset = new Vec3(position.getX() + 0.5D - getX(), position.getY() - getY(),
                position.getZ() + 0.5D - getZ());
        return level().noCollision(this, getBoundingBox().move(offset));
    }

    private boolean hasBurrowableGround(BlockPos position) {
        float totalHardness = 0.0F;
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos below = position.below(depth);
            BlockState state = level().getBlockState(below);
            float hardness = state.getDestroySpeed(level(), below);
            if (state.isAir() || !state.isSolidRender(level(), below) || hardness == 0.0F) {
                return false;
            }
            // The legacy check reads three layers. Permit an unbreakable third layer so
            // the standard shallow superflat preset can still support horizontal digging.
            if (hardness < 0.0F) {
                if (depth < 3) {
                    return false;
                }
                continue;
            }
            totalHardness += hardness;
        }
        return totalHardness < 10.0F;
    }

    private void spawnBurrowParticles() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 2 != 0) {
            return;
        }
        BlockPos groundPos = blockPosition().below();
        BlockState ground = level().getBlockState(groundPos);
        if (!ground.isAir()) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    getX(), getY() + 0.05D, getZ(), 5,
                    getBbWidth() * 0.55D, 0.08D, getBbWidth() * 0.55D, 0.04D);
        }
    }

    public final boolean isBurrowing() {
        return entityData.get(BURROW_PHASE) != BURROW_NONE;
    }

    public final boolean isFullyBurrowed() {
        return entityData.get(BURROW_PHASE) == BURROW_UNDERGROUND;
    }

    public final float getBurrowDepth(float partialTick) {
        float depth = entityData.get(BURROW_DEPTH);
        return previousBurrowDepth + (depth - previousBurrowDepth) * partialTick;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isFullyBurrowed()) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && getBodyNumber() > 0 && bodyPredecessor != null
                && level() instanceof ServerLevel serverLevel) {
            Entity predecessor = serverLevel.getEntity(bodyPredecessor);
            if (predecessor instanceof BurrowingVariantEntity previous && previous.isAlive()) {
                previous.hurt(source, amount * 0.5F);
            }
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = !isBurrowing() && super.doHurtTarget(entity);
        if (hit && !level().isClientSide) {
            startBodyAttackAnimation();
        }
        return hit;
    }

    @Override
    protected boolean canBreakBlocks() {
        return getBodyNumber() == 0;
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        if (getBodyNumber() > 0) {
            BurrowingVariantEntity head = findBodyHead(level);
            if (head != this) {
                return head.killedEntity(level, victim);
            }
        }
        return super.killedEntity(level, victim);
    }

    private BurrowingVariantEntity findBodyHead(ServerLevel level) {
        BurrowingVariantEntity current = this;
        for (int index = 0; index < 32 && current.bodyPredecessor != null; index++) {
            Entity predecessor = level.getEntity(current.bodyPredecessor);
            if (!(predecessor instanceof BurrowingVariantEntity previous)) {
                break;
            }
            current = previous;
        }
        return current;
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (getBodyNumber() == 0) {
            super.setTarget(target);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return !supportsBurrowing() && super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    public void travel(Vec3 travelVector) {
        super.travel(isBurrowing() ? Vec3.ZERO : travelVector);
    }

    @Override
    public boolean isPushable() {
        return getBodyNumber() == 0 && !isBurrowing() && super.isPushable();
    }

    @Override
    public boolean canCollideWith(Entity other) {
        if (other instanceof BurrowingVariantEntity segment && segment.getType() == getType()) {
            return false;
        }
        return super.canCollideWith(other);
    }

    @Override
    public void push(Entity entity) {
        if (getBodyNumber() == 0 && !isBurrowing()
                && !(entity instanceof BurrowingVariantEntity segment && segment.getType() == getType())) {
            super.push(entity);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("burrow_phase", entityData.get(BURROW_PHASE));
        tag.putFloat("burrow_depth", entityData.get(BURROW_DEPTH));
        tag.putInt("burrow_ticks", burrowTicks);
        tag.putInt("burrow_skill_ticks", burrowSkillTicks);
        tag.putBoolean("burrow_moved", movedUnderground);
        tag.putByte("body_number", entityData.get(BODY_NUMBER));
        tag.putBoolean("body_tail", entityData.get(BODY_TAIL));
        if (bodyPredecessor != null) {
            tag.putUUID("body_predecessor", bodyPredecessor);
        }
        tag.putBoolean("body_chain_initialized", bodyChainInitialized);
        tag.putInt("body_attack_ticks", entityData.get(BODY_ATTACK_TICKS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        byte phase = tag.contains("burrow_phase")
                ? tag.getByte("burrow_phase") : tag.getByte("tozoon_burrow_phase");
        if (phase < BURROW_NONE || phase > BURROW_EMERGING) {
            phase = BURROW_NONE;
        }
        float depth = tag.contains("burrow_depth")
                ? tag.getFloat("burrow_depth") : tag.getFloat("tozoon_burrow_depth");
        entityData.set(BURROW_PHASE, phase);
        entityData.set(BURROW_DEPTH, Math.max(0.0F, Math.min(1.0F, depth)));
        burrowTicks = Math.max(0, tag.contains("burrow_ticks")
                ? tag.getInt("burrow_ticks") : tag.getInt("tozoon_burrow_ticks"));
        burrowSkillTicks = Math.max(0, tag.getInt("burrow_skill_ticks"));
        movedUnderground = tag.getBoolean("burrow_moved");
        previousBurrowDepth = entityData.get(BURROW_DEPTH);
        entityData.set(BODY_NUMBER, tag.getByte("body_number"));
        entityData.set(BODY_TAIL, tag.getBoolean("body_tail"));
        bodyPredecessor = tag.hasUUID("body_predecessor") ? tag.getUUID("body_predecessor") : null;
        bodyChainInitialized = tag.getBoolean("body_chain_initialized") || getBodyNumber() > 0;
        entityData.set(BODY_ATTACK_TICKS, Math.max(0, tag.getInt("body_attack_ticks")));
    }

    private final class BurrowMovementGoal extends Goal {
        private BurrowMovementGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return canStartBurrowing();
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                beginBurrowing();
            }
        }
    }
}
