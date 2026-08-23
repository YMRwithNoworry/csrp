package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class OrbBoomEntity extends Entity {
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(
            OrbBoomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAIT_START = SynchedEntityData.defineId(
            OrbBoomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PROGRESS = SynchedEntityData.defineId(
            OrbBoomEntity.class, EntityDataSerializers.INT);
    private UUID ownerId;
    private int burstTicks;

    public OrbBoomEntity(EntityType<? extends OrbBoomEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(PrimitiveParasiteEntity owner, int fuse, int waitStart) {
        ownerId = owner == null ? null : owner.getUUID();
        entityData.set(FUSE, Math.max(1, fuse));
        entityData.set(WAIT_START, Math.max(0, waitStart));
        entityData.set(PROGRESS, 0);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FUSE, 7);
        entityData.define(WAIT_START, 40);
        entityData.define(PROGRESS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientParticles();
            return;
        }
        if (tickCount <= entityData.get(WAIT_START)) {
            return;
        }
        int progress = entityData.get(PROGRESS);
        int fuse = entityData.get(FUSE);
        if (progress < fuse) {
            entityData.set(PROGRESS, progress + 1);
            refreshDimensions();
            if (tickCount % 10 == 0) {
                pulse(false);
            }
            return;
        }
        if (burstTicks++ == 0) {
            pulse(true);
        }
        if (burstTicks == 2) {
            playSound(ModSounds.ORB_END.get(), 1.0F, 1.0F);
        }
        if (burstTicks > 5) {
            discard();
        }
    }

    private void pulse(boolean burst) {
        PrimitiveParasiteEntity owner = owner();
        double radius = Math.max(0.5D, getBbWidth() * 0.5D);
        if (owner != null && owner.isAlive()) {
            float damage = burst ? (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE)
                    : (float) Math.max(1.0D, owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5D);
            owner.hurtNearby(this, radius, damage, false);
            if (burst) {
                for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox(), owner::isValidParasiteTarget)) {
                    owner.applyPrimitiveMinimumDamage(target);
                }
            }
            return;
        }
        if (burst) {
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox(), LivingEntity::isAlive)) {
                target.hurt(damageSources().magic(), 10.0F);
            }
        }
    }

    private PrimitiveParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof PrimitiveParasiteEntity parasite ? parasite : null;
    }

    private void spawnClientParticles() {
        int count = entityData.get(PROGRESS) >= entityData.get(FUSE) ? 6 : 2;
        for (int index = 0; index < count; index++) {
            level().addParticle(index == 0
                            ? net.minecraft.core.particles.ParticleTypes.EXPLOSION
                            : net.minecraft.core.particles.ParticleTypes.END_ROD,
                    getRandomX(getBbWidth()), getRandomY(), getRandomZ(getBbWidth()),
                    0.0D, 0.01D, 0.0D);
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        int progress = entityData == null ? 0 : entityData.get(PROGRESS);
        return EntityDimensions.scalable(0.5F + progress, 0.5F + progress * 0.4F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        entityData.set(FUSE, Math.max(1, tag.getInt("fuse")));
        entityData.set(WAIT_START, Math.max(0, tag.getInt("wait_start")));
        entityData.set(PROGRESS, Math.max(0, tag.getInt("progress")));
        burstTicks = Math.max(0, tag.getInt("burst_ticks"));
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        tag.putInt("fuse", entityData.get(FUSE));
        tag.putInt("wait_start", entityData.get(WAIT_START));
        tag.putInt("progress", entityData.get(PROGRESS));
        tag.putInt("burst_ticks", burstTicks);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
