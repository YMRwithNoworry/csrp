package alku.csrp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Hitbox and detached-tendril implementation used by Marauder. Attached
 * instances are invisible collision targets; detached/support modes render the
 * legacy Esor tendril and retain their own short-lived combat behavior.
 */
public final class MarauderTendrilEntity extends Monster implements GeoEntity, Parasite {
    private static final int TELEPORT_LIFETIME_TICKS = 90;
    private static final int TELEPORT_TRIGGER_REMAINING_TICKS = 30;
    private static final int SNARE_LIFETIME_TICKS = 180;
    private static final int DETACHED_LIFETIME_TICKS = 1200;
    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(
            MarauderTendrilEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            MarauderTendrilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(
            MarauderTendrilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> ATTACHED_SIDE = SynchedEntityData.defineId(
            MarauderTendrilEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> REMAINING_TICKS = SynchedEntityData.defineId(
            MarauderTendrilEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;

    public MarauderTendrilEntity(EntityType<? extends MarauderTendrilEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, target -> target != this && target.isAlive() && !(target instanceof Parasite)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE, (byte) Mode.DETACHED.ordinal());
        builder.define(OWNER_ID, 0);
        builder.define(TARGET_ID, 0);
        builder.define(ATTACHED_SIDE, (byte) MarauderEntity.TendrilSide.LEFT.ordinal());
        builder.define(REMAINING_TICKS, DETACHED_LIFETIME_TICKS);
    }

    public void attach(MarauderEntity owner, MarauderEntity.TendrilSide side) {
        ownerUuid = owner.getUUID();
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(ATTACHED_SIDE, (byte) side.ordinal());
        setMode(Mode.ATTACHED);
        setNoAi(true);
        setNoGravity(true);
        updateAttachedPosition(owner);
    }

    public void detach() {
        MarauderEntity owner = getOwnerMarauder();
        if (owner != null) {
            setTarget(owner.getTarget());
        }
        ownerUuid = null;
        entityData.set(OWNER_ID, 0);
        entityData.set(TARGET_ID, 0);
        targetUuid = null;
        setMode(Mode.DETACHED);
        entityData.set(REMAINING_TICKS, DETACHED_LIFETIME_TICKS);
        setNoAi(false);
        setNoGravity(false);
        setDeltaMovement((random.nextDouble() - 0.5D) * 0.25D, 0.3D,
                (random.nextDouble() - 0.5D) * 0.25D);
    }

    public void startSupport(MarauderEntity owner, LivingEntity target, Mode mode) {
        if (mode != Mode.TELEPORT && mode != Mode.SNARE) {
            throw new IllegalArgumentException("Support tendril mode must be TELEPORT or SNARE");
        }
        ownerUuid = owner.getUUID();
        targetUuid = target.getUUID();
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(TARGET_ID, target.getId());
        setMode(mode);
        entityData.set(REMAINING_TICKS, mode == Mode.TELEPORT ? TELEPORT_LIFETIME_TICKS : SNARE_LIFETIME_TICKS);
        setNoAi(true);
        setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        Mode mode = getMode();
        if (mode == Mode.ATTACHED) {
            MarauderEntity owner = getOwnerMarauder();
            if (owner != null) {
                updateAttachedPosition(owner);
            }
            if (!level().isClientSide && (owner == null || !owner.isAlive() || !owner.isTendrilAttached(getAttachedSide()))) {
                discard();
            }
            return;
        }
        if (level().isClientSide) {
            return;
        }

        int remaining = entityData.get(REMAINING_TICKS) - 1;
        entityData.set(REMAINING_TICKS, remaining);
        if (remaining <= 0) {
            discard();
            return;
        }

        if (mode == Mode.TELEPORT) {
            tickTeleportSupport(remaining);
        } else if (mode == Mode.SNARE) {
            tickSnareSupport();
        }
    }

    private void tickTeleportSupport(int remaining) {
        MarauderEntity owner = getOwnerMarauder();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (remaining == TELEPORT_TRIGGER_REMAINING_TICKS) {
            owner.teleportTo(getX(), getY(), getZ());
            spawnSupportParticles(ParticleTypes.PORTAL, 24, 0.75D);
            discard();
        }
    }

    private void tickSnareSupport() {
        LivingEntity target = getSupportTarget();
        MarauderEntity owner = getOwnerMarauder();
        if ((target == null || !target.isAlive()) && owner != null) {
            target = owner.getTarget();
            if (target != null) {
                targetUuid = target.getUUID();
                entityData.set(TARGET_ID, target.getId());
            }
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        double distance = distanceToSqr(target);
        if (distance > 25.0D) {
            return;
        }
        target.stopRiding();
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false), this);
        Vec3 direction = position().subtract(target.position());
        if (direction.lengthSqr() > 0.001D) {
            Vec3 pull = direction.normalize().scale(0.10D);
            target.push(pull.x, pull.y, pull.z);
        }
        if (tickCount % 10 == 0) {
            spawnSupportParticles(ParticleTypes.CRIT, 4, 0.25D);
        }
    }

    private void updateAttachedPosition(MarauderEntity owner) {
        float radians = owner.getYRot() * Mth.DEG_TO_RAD;
        MarauderEntity.TendrilSide side = getAttachedSide();
        double x = owner.getX() + side.offsetSign() * Mth.cos(radians) * 0.9D;
        double z = owner.getZ() + side.offsetSign() * Mth.sin(radians) * 0.9D;
        moveTo(x, owner.getY() + 1.5D, z, owner.getYRot(), 0.0F);
        setDeltaMovement(Vec3.ZERO);
    }

    private void spawnSupportParticles(net.minecraft.core.particles.ParticleOptions particle, int count, double spread) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, getX(), getY() + 0.9D, getZ(), count,
                    spread, spread, spread, 0.02D);
        }
    }

    @Nullable
    private MarauderEntity getOwnerMarauder() {
        int syncedId = entityData.get(OWNER_ID);
        Entity synced = syncedId == 0 ? null : level().getEntity(syncedId);
        if (synced instanceof MarauderEntity owner) {
            return owner;
        }
        if (ownerUuid != null && level() instanceof ServerLevel serverLevel) {
            Entity saved = serverLevel.getEntity(ownerUuid);
            return saved instanceof MarauderEntity owner ? owner : null;
        }
        return null;
    }

    @Nullable
    private LivingEntity getSupportTarget() {
        int syncedId = entityData.get(TARGET_ID);
        Entity synced = syncedId == 0 ? null : level().getEntity(syncedId);
        if (synced instanceof LivingEntity target) {
            return target;
        }
        if (targetUuid != null && level() instanceof ServerLevel serverLevel) {
            Entity saved = serverLevel.getEntity(targetUuid);
            return saved instanceof LivingEntity target ? target : null;
        }
        return null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isAttached()) {
            MarauderEntity owner = getOwnerMarauder();
            return owner != null && owner.hurtTendril(this, source, amount);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return !isAttached();
    }

    public Mode getMode() {
        int ordinal = entityData.get(MODE);
        return ordinal >= 0 && ordinal < Mode.values().length ? Mode.values()[ordinal] : Mode.DETACHED;
    }

    private void setMode(Mode mode) {
        entityData.set(MODE, (byte) mode.ordinal());
    }

    public boolean isAttached() {
        return getMode() == Mode.ATTACHED;
    }

    public boolean isAttachedTo(MarauderEntity owner) {
        MarauderEntity currentOwner = getOwnerMarauder();
        return isAttached() && currentOwner != null && currentOwner.getUUID().equals(owner.getUUID());
    }

    public boolean isSupportFor(MarauderEntity owner) {
        Mode mode = getMode();
        MarauderEntity currentOwner = getOwnerMarauder();
        return (mode == Mode.TELEPORT || mode == Mode.SNARE)
                && currentOwner != null && currentOwner.getUUID().equals(owner.getUUID());
    }

    public MarauderEntity.TendrilSide getAttachedSide() {
        int ordinal = entityData.get(ATTACHED_SIDE);
        return ordinal >= 0 && ordinal < MarauderEntity.TendrilSide.values().length
                ? MarauderEntity.TendrilSide.values()[ordinal] : MarauderEntity.TendrilSide.LEFT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("marauder_tendril_mode", (byte) getMode().ordinal());
        tag.putByte("marauder_tendril_side", (byte) getAttachedSide().ordinal());
        tag.putInt("marauder_tendril_remaining", entityData.get(REMAINING_TICKS));
        if (ownerUuid != null) {
            tag.putUUID("marauder_tendril_owner", ownerUuid);
        }
        if (targetUuid != null) {
            tag.putUUID("marauder_tendril_target", targetUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int mode = tag.getByte("marauder_tendril_mode");
        setMode(mode >= 0 && mode < Mode.values().length ? Mode.values()[mode] : Mode.DETACHED);
        entityData.set(ATTACHED_SIDE, tag.getByte("marauder_tendril_side"));
        entityData.set(REMAINING_TICKS, tag.contains("marauder_tendril_remaining")
                ? tag.getInt("marauder_tendril_remaining") : DETACHED_LIFETIME_TICKS);
        ownerUuid = tag.hasUUID("marauder_tendril_owner") ? tag.getUUID("marauder_tendril_owner") : null;
        targetUuid = tag.hasUUID("marauder_tendril_target") ? tag.getUUID("marauder_tendril_target") : null;
        setNoAi(getMode() != Mode.DETACHED);
        setNoGravity(getMode() != Mode.DETACHED);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public enum Mode {
        ATTACHED,
        DETACHED,
        TELEPORT,
        SNARE
    }
}
