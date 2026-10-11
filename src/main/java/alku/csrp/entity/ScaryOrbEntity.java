package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public final class ScaryOrbEntity extends Entity {
    private static final EntityDataAccessor<Boolean> BOLSTER_ORB = SynchedEntityData.defineId(
            ScaryOrbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> BURST_SCALE = SynchedEntityData.defineId(
            ScaryOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VISUAL_WIDTH = SynchedEntityData.defineId(
            ScaryOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VISUAL_HEIGHT = SynchedEntityData.defineId(
            ScaryOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final int DEFAULT_START_TICKS = 40;
    private static final int DEFAULT_FUSE_TICKS = 7;
    private static final int BURST_TICKS = 35;
    private static final int DISCARD_TICKS = 45;
    private static final float INITIAL_WIDTH = 0.5F;
    private static final float INITIAL_HEIGHT = 0.5F;
    private static final float FUSE_WIDTH_GROWTH = 0.8F;
    private static final float FUSE_HEIGHT_GROWTH = 0.482F;
    private static final float BURST_WIDTH_SHRINK = 0.8F;
    private static final float BURST_HEIGHT_SHRINK = 0.32F;
    private UUID ownerId;
    private UUID targetId;
    private int activeTicks;
    private int travelTicks;
    private int startTicks = DEFAULT_START_TICKS;
    private int fuseTicks = DEFAULT_FUSE_TICKS;
    private boolean launched;
    private boolean anchored;
    private double anchorX;
    private double anchorY;
    private double anchorZ;

    public ScaryOrbEntity(EntityType<? extends ScaryOrbEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public ScaryOrbEntity(EntityType<? extends ScaryOrbEntity> type, Level level, PrimitiveParasiteEntity owner) {
        this(type, level);
        ownerId = owner.getUUID();
        entityData.set(BOLSTER_ORB, owner instanceof AdaptedVariantEntity adapted && adapted.isAdaptedBolster());
    }

    public boolean isBolsterOrb() {
        return entityData.get(BOLSTER_ORB);
    }

    public void setAnchor(Vec3 anchor) {
        launched = false;
        anchored = true;
        activeTicks = 0;
        setVisualDimensions(INITIAL_WIDTH, INITIAL_HEIGHT);
        anchorX = anchor.x;
        anchorY = anchor.y;
        anchorZ = anchor.z;
        setDeltaMovement(Vec3.ZERO);
        setPos(anchor);
    }

    public void setTimings(int startTicks, int fuseTicks) {
        this.startTicks = Math.max(0, startTicks);
        this.fuseTicks = Math.max(1, fuseTicks);
    }

    /** Starts the orb as a projectile and anchors it only after reaching its target. */
    public void launch(Vec3 start, Vec3 target, LivingEntity targetEntity) {
        launched = true;
        anchored = false;
        activeTicks = 0;
        travelTicks = 0;
        setVisualDimensions(INITIAL_WIDTH, INITIAL_HEIGHT);
        targetId = targetEntity == null ? null : targetEntity.getUUID();
        anchorX = target.x;
        anchorY = target.y;
        anchorZ = target.z;
        setPos(start);
        refreshDimensions();
        Vec3 direction = target.subtract(start);
        setDeltaMovement(direction.lengthSqr() > 0.001D ? direction.normalize().scale(0.55D) : Vec3.ZERO);
    }

    @Override public void tick() {
        super.tick();
        PrimitiveParasiteEntity owner = owner();
        if (launched && !anchored) {
            LivingEntity target = target();
            if (target != null && target.isAlive()) {
                anchorX = target.getX();
                anchorY = target.getY() + target.getBbHeight() * 0.5D;
                anchorZ = target.getZ();
            }
            Vec3 start = position();
            Vec3 movement = getDeltaMovement();
            setPos(start.add(movement));
            travelTicks++;
            boolean reachedTarget = distanceToSqr(new Vec3(anchorX, anchorY, anchorZ)) <= 2.25D;
            boolean blockHit = level().clip(new net.minecraft.world.level.ClipContext(start, position(),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, this)).getType()
                    != net.minecraft.world.phys.HitResult.Type.MISS;
            if (reachedTarget || blockHit || travelTicks >= 50) {
                anchored = true;
                setDeltaMovement(Vec3.ZERO);
                setPos(anchorX, anchorY, anchorZ);
            }
        } else if (anchored) {
            setPos(anchorX, anchorY, anchorZ);
        } else if (owner != null && owner.isAlive()) {
            setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.5, owner.getZ());
        }
        if (level().isClientSide) {
            return;
        }
        if (!anchored) return;
        if (activeTicks < startTicks) {
            activeTicks++;
            return;
        }
        int activeAge = activeTicks - startTicks;
        activeTicks++;
        if (activeAge > 0 && activeAge < fuseTicks) {
            setVisualDimensions(getVisualWidth() + FUSE_WIDTH_GROWTH,
                    getVisualHeight() + FUSE_HEIGHT_GROWTH);
        } else if (activeAge > fuseTicks + BURST_TICKS) {
            setVisualDimensions(Math.max(INITIAL_WIDTH,
                            getVisualWidth() - BURST_WIDTH_SHRINK),
                    Math.max(INITIAL_HEIGHT,
                            getVisualHeight() - BURST_HEIGHT_SHRINK));
        }
        if (activeAge % 10 == 0 && owner != null) {
            applyOrbEffects(owner, activeAge % 20 == 0);
        }
        if (activeAge == fuseTicks + BURST_TICKS && owner != null) {
            burst(owner);
        }
        if (activeAge < fuseTicks + DISCARD_TICKS
                && owner != null && owner.isAlive()) {
            return;
        }
        discard();
    }

    private void applyOrbEffects(PrimitiveParasiteEntity owner, boolean applyMinimumDamage) {
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(2.5D));
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(2.5D), LivingEntity::isAlive);
        int nearbyEntities = targets.size();
        for (LivingEntity target : targets) {
            owner.applyScaryOrbEffect(target, nearbyEntities);
            if (applyMinimumDamage) {
                owner.applyScaryOrbMinimumDamage(target, 0.5F);
            }
        }
    }

    private void burst(PrimitiveParasiteEntity owner) {
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(3.0D));
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(3.0D), LivingEntity::isAlive)) {
            owner.applyScaryOrbMinimumDamage(target, 5.0F);
        }
    }

    public float burstScale() {
        return entityData.get(BURST_SCALE);
    }

    public float getVisualWidth() {
        return entityData.get(VISUAL_WIDTH);
    }

    public float getVisualHeight() {
        return entityData.get(VISUAL_HEIGHT);
    }

    /** Matches the original renderer's size curve, including its warm-up and burst bounce. */
    public float getRenderScale(float partialTick) {
        float age = tickCount + partialTick;
        float base = Math.max(getVisualWidth() * 2.0F, getVisualHeight() * 1.9F) * 0.8F;
        float progress = Mth.clamp(age / 35.0F, 0.0F, 1.0F);
        progress = progress * progress * (3.0F - 2.0F * progress);
        float normalScale = base * (0.35F + 0.95F * progress);
        float bounce = 0.28F * (float) Math.exp(-5.0F * progress)
                * Mth.sin(10.0F * progress * (float) Math.PI);
        return Math.max(0.001F, normalScale * (1.0F + bounce));
    }

    private void setVisualDimensions(float width, float height) {
        float clampedWidth = Math.max(0.1F, width);
        float clampedHeight = Math.max(0.1F, height);
        entityData.set(VISUAL_WIDTH, clampedWidth);
        entityData.set(VISUAL_HEIGHT, clampedHeight);
        entityData.set(BURST_SCALE, clampedWidth / INITIAL_WIDTH);
        refreshDimensions();
    }

    private PrimitiveParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof PrimitiveParasiteEntity parasite ? parasite : null;
    }

    private LivingEntity target() {
        if (targetId == null || !(level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(targetId);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override protected void defineSynchedData() {
        entityData.define(BOLSTER_ORB, false);
        entityData.define(BURST_SCALE, 1.0F);
        entityData.define(VISUAL_WIDTH, INITIAL_WIDTH);
        entityData.define(VISUAL_HEIGHT, INITIAL_HEIGHT);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) ownerId = tag.getUUID("owner");
        if (tag.hasUUID("target")) targetId = tag.getUUID("target");
        activeTicks = tag.getInt("active_ticks");
        travelTicks = tag.getInt("travel_ticks");
        startTicks = tag.contains("start_ticks") ? tag.getInt("start_ticks") : DEFAULT_START_TICKS;
        fuseTicks = tag.contains("fuse_ticks") ? tag.getInt("fuse_ticks") : DEFAULT_FUSE_TICKS;
        launched = tag.getBoolean("launched");
        anchored = tag.getBoolean("anchored");
        anchorX = tag.getDouble("anchor_x");
        anchorY = tag.getDouble("anchor_y");
        anchorZ = tag.getDouble("anchor_z");
        entityData.set(BOLSTER_ORB, tag.getBoolean("bolster_orb"));
        setVisualDimensions(tag.contains("visual_width") ? tag.getFloat("visual_width") : INITIAL_WIDTH,
                tag.contains("visual_height") ? tag.getFloat("visual_height") : INITIAL_HEIGHT);
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("owner", ownerId);
        if (targetId != null) tag.putUUID("target", targetId);
        tag.putInt("active_ticks", activeTicks);
        tag.putInt("travel_ticks", travelTicks);
        tag.putInt("start_ticks", startTicks);
        tag.putInt("fuse_ticks", fuseTicks);
        tag.putBoolean("launched", launched);
        tag.putBoolean("anchored", anchored);
        tag.putDouble("anchor_x", anchorX);
        tag.putDouble("anchor_y", anchorY);
        tag.putDouble("anchor_z", anchorZ);
        tag.putBoolean("bolster_orb", entityData.get(BOLSTER_ORB));
        tag.putFloat("visual_width", getVisualWidth());
        tag.putFloat("visual_height", getVisualHeight());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(getVisualWidth(), getVisualHeight());
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
