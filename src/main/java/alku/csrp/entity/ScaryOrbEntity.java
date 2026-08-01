package alku.csrp.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ScaryOrbEntity extends Entity {
    private static final int START_TICKS = 40;
    private static final int BURST_TICKS = 35;
    private static final int DISCARD_TICKS = 45;
    private UUID ownerId;
    private UUID targetId;
    private int activeTicks;
    private int travelTicks;
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
    }

    public void setAnchor(Vec3 anchor) {
        launched = false;
        anchored = true;
        anchorX = anchor.x;
        anchorY = anchor.y;
        anchorZ = anchor.z;
        setDeltaMovement(Vec3.ZERO);
        setPos(anchor);
    }

    /** Starts the orb as a projectile and anchors it only after reaching its target. */
    public void launch(Vec3 start, Vec3 target, LivingEntity targetEntity) {
        launched = true;
        anchored = false;
        activeTicks = 0;
        travelTicks = 0;
        targetId = targetEntity == null ? null : targetEntity.getUUID();
        anchorX = target.x;
        anchorY = target.y;
        anchorZ = target.z;
        setPos(start);
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
        if (activeTicks < START_TICKS) {
            activeTicks++;
            return;
        }
        int elapsed = activeTicks - START_TICKS;
        activeTicks++;
        if (elapsed % 10 == 0 && owner != null) applyOrbEffects(owner);
        if (elapsed == BURST_TICKS && owner != null) owner.hurtNearby(this, 3.0,
                (float) owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.5F,
                false);
        if (elapsed >= DISCARD_TICKS || owner == null || !owner.isAlive()) discard();
    }

    private void applyOrbEffects(PrimitiveParasiteEntity owner) {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.5),
                owner::isValidParasiteTarget)) {
            target.hurt(damageSources().indirectMagic(this, owner), 2.0F);
        }
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

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) ownerId = tag.getUUID("owner");
        if (tag.hasUUID("target")) targetId = tag.getUUID("target");
        activeTicks = tag.getInt("active_ticks");
        travelTicks = tag.getInt("travel_ticks");
        launched = tag.getBoolean("launched");
        anchored = tag.getBoolean("anchored");
        anchorX = tag.getDouble("anchor_x");
        anchorY = tag.getDouble("anchor_y");
        anchorZ = tag.getDouble("anchor_z");
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("owner", ownerId);
        if (targetId != null) tag.putUUID("target", targetId);
        tag.putInt("active_ticks", activeTicks);
        tag.putInt("travel_ticks", travelTicks);
        tag.putBoolean("launched", launched);
        tag.putBoolean("anchored", anchored);
        tag.putDouble("anchor_x", anchorX);
        tag.putDouble("anchor_y", anchorY);
        tag.putDouble("anchor_z", anchorZ);
    }
}
