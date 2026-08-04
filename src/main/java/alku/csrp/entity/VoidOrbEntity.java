package alku.csrp.entity;

import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class VoidOrbEntity extends Entity {
    private static final int DEFAULT_START_TICKS = 40;
    private static final int DEFAULT_FUSE_TICKS = 7;
    private static final int ACTIVE_DURATION_TICKS = 150;
    private static final int SHRINK_DURATION_TICKS = 10;
    private static final int SHRINK_START_TICKS = ACTIVE_DURATION_TICKS - SHRINK_DURATION_TICKS;
    private static final double PULL_RADIUS = 42.0D;
    private static final double PULL_STRENGTH = 0.2D;
    private static final double CENTER_DISTANCE_SQR = 2.0D * 2.0D;
    private static final double DAMAGE_DISTANCE_SQR = 5.0D * 5.0D;
    private static final float INITIAL_WIDTH = 0.5F;
    private static final float INITIAL_HEIGHT = 0.5F;
    private static final float FUSE_WIDTH_GROWTH = 0.8F;
    private static final float FUSE_HEIGHT_GROWTH = 0.32F;

    private static final EntityDataAccessor<Integer> START_TICKS = SynchedEntityData.defineId(
            VoidOrbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUSE_TICKS = SynchedEntityData.defineId(
            VoidOrbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME_TICKS = SynchedEntityData.defineId(
            VoidOrbEntity.class, EntityDataSerializers.INT);

    private UUID ownerId;
    private boolean followOwner;
    private double ownerOffset;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private int fuseProgress;
    private int collapseTicks;

    public VoidOrbEntity(EntityType<? extends VoidOrbEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(DerivedParasiteEntity owner, int fuseTicks, int startTicks,
            boolean followOwner, double ownerOffset) {
        ownerId = owner.getUUID();
        this.followOwner = followOwner;
        this.ownerOffset = ownerOffset;
        entityData.set(FUSE_TICKS, Math.max(1, fuseTicks));
        entityData.set(START_TICKS, Math.max(0, startTicks));
        entityData.set(LIFETIME_TICKS, 0);
        fuseProgress = 0;
        collapseTicks = 0;
        anchorX = owner.getX();
        anchorY = owner.getY() + owner.getBbHeight() + ownerOffset;
        anchorZ = owner.getZ();
        setPos(anchorX, anchorY, anchorZ);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(START_TICKS, DEFAULT_START_TICKS);
        builder.define(FUSE_TICKS, DEFAULT_FUSE_TICKS);
        builder.define(LIFETIME_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        DerivedParasiteEntity owner = owner();
        if (!level().isClientSide) {
            if (followOwner) {
                if (owner == null || !owner.isAlive()) {
                    discard();
                    return;
                }
                anchorX = owner.getX();
                anchorY = owner.getY() + owner.getBbHeight() + ownerOffset;
                anchorZ = owner.getZ();
            }
            setPos(anchorX, anchorY, anchorZ);
        }

        if (level().isClientSide) {
            spawnPortalParticles();
            return;
        }
        int lifetimeTicks = getLifetimeTicks() + 1;
        entityData.set(LIFETIME_TICKS, lifetimeTicks);
        if (lifetimeTicks <= getStartTicks()) {
            return;
        }

        pullNearbyEntities(owner);
        int activeTicks = lifetimeTicks - getStartTicks();
        if (fuseProgress < getFuseTicks()) {
            fuseProgress++;
        }
        collapseTicks = Math.max(0, activeTicks - getFuseTicks());
        if (activeTicks > SHRINK_START_TICKS) {
            damageDuringCollapse(owner, activeTicks - SHRINK_START_TICKS);
            if (activeTicks == SHRINK_START_TICKS + 1) {
                playSound(ModSounds.ORB_END.get(), 1.0F, 1.0F);
            }
        }
        if (activeTicks >= ACTIVE_DURATION_TICKS) {
            discard();
        }
    }

    private void pullNearbyEntities(DerivedParasiteEntity owner) {
        if (owner != null) {
            DragonEggAssimilationEntity.assimilateDragonEggs(level(),
                    getBoundingBox().inflate(Math.sqrt(DAMAGE_DISTANCE_SQR)));
        }
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(PULL_RADIUS), this::canPull)) {
            if (!isUncovered(target)) {
                continue;
            }
            target.stopRiding();
            double distanceSqr = target.distanceToSqr(this);
            if (distanceSqr < CENTER_DISTANCE_SQR) {
                target.teleportTo(getX(), getY(), getZ());
                target.setDeltaMovement(Vec3.ZERO);
            } else {
                Vec3 direction = position().subtract(target.position());
                if (direction.lengthSqr() > 0.0D) {
                    direction = direction.normalize().scale(PULL_STRENGTH);
                    target.push(direction.x, direction.y, direction.z);
                }
            }
            if (distanceSqr < DAMAGE_DISTANCE_SQR && owner != null) {
                target.invulnerableTime = 0;
                owner.applyMinimumDamage(target, 14.0F / 10.0F);
                target.hurt(damageSources().fellOutOfWorld(), 10.0F);
                target.invulnerableTime = 0;
            }
        }
    }

    private void damageDuringCollapse(DerivedParasiteEntity owner, int shrinkTicks) {
        if (owner == null) {
            return;
        }
        int growthTicks = Math.max(0, getFuseTicks() - 1);
        float width = Math.max(0.1F,
                INITIAL_WIDTH + growthTicks * FUSE_WIDTH_GROWTH - shrinkTicks * FUSE_WIDTH_GROWTH);
        float height = Math.max(0.1F,
                INITIAL_HEIGHT + growthTicks * FUSE_HEIGHT_GROWTH - shrinkTicks * FUSE_HEIGHT_GROWTH);
        AABB damageBox = new AABB(
                getX() - width * 0.5D, getY() - height, getZ() - width * 0.5D,
                getX() + width * 0.5D, getY() + height, getZ() + width * 0.5D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), damageBox);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, damageBox,
                target -> canPull(target) && isUncovered(target))) {
            target.invulnerableTime = 0;
            owner.applyMinimumDamage(target, 70.0F);
            target.invulnerableTime = 0;
        }
    }

    private boolean canPull(LivingEntity target) {
        if (!target.isAlive() || target.getUUID().equals(ownerId) || !target.isPushable()) {
            return false;
        }
        return !(target instanceof Player player && player.getAbilities().invulnerable);
    }

    private boolean isUncovered(LivingEntity target) {
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        HitResult hit = level().clip(new ClipContext(position(), targetCenter,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void spawnPortalParticles() {
        if (getLifetimeTicks() <= getStartTicks()) {
            return;
        }
        for (int index = 0; index < 4; index++) {
            level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 3.0D,
                    getY() + random.nextDouble() * getBbHeight() - 0.25D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 3.0D,
                    (random.nextDouble() - 0.5D) * 2.0D,
                    -random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 2.0D);
        }
    }

    private DerivedParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof DerivedParasiteEntity derived ? derived : null;
    }

    public int getStartTicks() {
        return entityData.get(START_TICKS);
    }

    public int getFuseTicks() {
        return entityData.get(FUSE_TICKS);
    }

    public int getLifetimeTicks() {
        return entityData.get(LIFETIME_TICKS);
    }

    public float getRenderScale(float partialTick) {
        float age = getLifetimeTicks() + partialTick;
        if (age <= getStartTicks()) {
            return Mth.clamp(age / 10.0F, 0.25F, 1.0F);
        }
        float activeAge = age - getStartTicks();
        if (activeAge <= getFuseTicks()) {
            return 1.0F + activeAge / Math.max(1.0F, getFuseTicks()) * 2.0F;
        }
        if (activeAge <= SHRINK_START_TICKS) {
            return 3.0F;
        }
        float shrinkProgress = (activeAge - SHRINK_START_TICKS) / SHRINK_DURATION_TICKS;
        return Math.max(0.1F, 3.0F * (1.0F - shrinkProgress));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        followOwner = tag.getBoolean("follow_owner");
        ownerOffset = tag.getDouble("owner_offset");
        anchorX = tag.getDouble("anchor_x");
        anchorY = tag.getDouble("anchor_y");
        anchorZ = tag.getDouble("anchor_z");
        fuseProgress = tag.getInt("fuse_progress");
        collapseTicks = tag.getInt("collapse_ticks");
        entityData.set(START_TICKS, tag.contains("start_ticks")
                ? tag.getInt("start_ticks") : DEFAULT_START_TICKS);
        entityData.set(FUSE_TICKS, tag.contains("fuse_ticks")
                ? tag.getInt("fuse_ticks") : DEFAULT_FUSE_TICKS);
        entityData.set(LIFETIME_TICKS, tag.contains("lifetime_ticks")
                ? tag.getInt("lifetime_ticks")
                : getStartTicks() + fuseProgress + Math.max(0, collapseTicks - 1));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        tag.putBoolean("follow_owner", followOwner);
        tag.putDouble("owner_offset", ownerOffset);
        tag.putDouble("anchor_x", anchorX);
        tag.putDouble("anchor_y", anchorY);
        tag.putDouble("anchor_z", anchorZ);
        tag.putInt("fuse_progress", fuseProgress);
        tag.putInt("collapse_ticks", collapseTicks);
        tag.putInt("start_ticks", getStartTicks());
        tag.putInt("fuse_ticks", getFuseTicks());
        tag.putInt("lifetime_ticks", getLifetimeTicks());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65_536.0D;
    }
}
