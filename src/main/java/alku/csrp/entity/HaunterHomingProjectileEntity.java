package alku.csrp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The Pheon's original block-piercing homing orb. It deliberately does not
 * share the generic projectile impact code because its damage and movement
 * rules are unique to this preeminent form.
 */
public final class HaunterHomingProjectileEntity extends Entity {
    private static final float DAMAGE = 15.0F;
    private static final double HOMING_ACCELERATION = 0.075D;
    private static final int MAX_LIFETIME_TICKS = 200;
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(
            HaunterHomingProjectileEntity.class, EntityDataSerializers.INT);

    private UUID ownerId;
    private UUID targetId;

    public HaunterHomingProjectileEntity(EntityType<? extends HaunterHomingProjectileEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(PreeminentParasiteEntity owner, LivingEntity target, Vec3 start) {
        ownerId = owner.getUUID();
        targetId = target.getUUID();
        entityData.set(TARGET_ID, target.getId());
        setPos(start);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        LivingEntity target = target();
        if (!level().isClientSide && targetId != null && target == null) {
            discard();
            return;
        }
        PreeminentParasiteEntity owner = owner();
        if (!level().isClientSide && ownerId != null && owner == null) {
            discard();
            return;
        }

        Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        if (target != null && target.isAlive()) {
            Vec3 destination = new Vec3(target.getX(), target.getY() - target.getBbHeight() * 1.5D,
                    target.getZ());
            Vec3 direction = destination.subtract(position());
            if (direction.length() < getBoundingBox().getSize()) {
                movement = movement.scale(0.5D);
            } else {
                movement = movement.add(direction.normalize().scale(HOMING_ACCELERATION));
            }
            setYRot((float) (-Math.toDegrees(Math.atan2(target.getX() - getX(), target.getZ() - getZ()))));
        } else if (movement.horizontalDistanceSqr() > 0.0001D) {
            setYRot((float) (-Math.toDegrees(Math.atan2(movement.x, movement.z))));
        }
        setDeltaMovement(movement);

        if (level().isClientSide) {
            return;
        }

        AABB impactArea = new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D,
                getZ() + 1.0D).inflate(2.0D);
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, impactArea,
                this::isValidTarget)) {
            DamageSource source = damageSources().indirectMagic(this, owner == null ? this : owner);
            if (candidate.hurt(source, DAMAGE)) {
                discard();
                return;
            }
        }
        if (tickCount > MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    private boolean isValidTarget(LivingEntity target) {
        return target.isAlive() && !(target instanceof Parasite);
    }

    private PreeminentParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof PreeminentParasiteEntity preeminent ? preeminent : null;
    }

    private LivingEntity target() {
        if (targetId != null && level() instanceof ServerLevel serverLevel) {
            Entity savedTarget = serverLevel.getEntity(targetId);
            if (savedTarget instanceof LivingEntity living) {
                entityData.set(TARGET_ID, living.getId());
                return living;
            }
            return null;
        }
        Entity syncedTarget = level().getEntity(entityData.get(TARGET_ID));
        if (syncedTarget instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16_384.0D;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            playSound(SoundEvents.SHULKER_BULLET_HURT, 1.0F, 1.0F);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 15,
                        0.2D, 0.2D, 0.2D, 0.0D);
            }
            discard();
        }
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_ID, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        if (tag.hasUUID("target")) {
            targetId = tag.getUUID("target");
        }
        entityData.set(TARGET_ID, tag.getInt("target_id"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        if (targetId != null) {
            tag.putUUID("target", targetId);
        }
        tag.putInt("target_id", entityData.get(TARGET_ID));
    }
}
