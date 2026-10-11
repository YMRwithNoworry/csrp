package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public final class ScaryOrbEntity extends Entity {
    private static final EntityDataAccessor<Boolean> BOLSTER_ORB = SynchedEntityData.defineId(
            ScaryOrbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> BURST_SCALE = SynchedEntityData.defineId(
            ScaryOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final int DEFAULT_START_TICKS = 30;
    private static final int DEFAULT_FUSE_TICKS = 11;
    private static final int ADAPTED_START_TICKS = 15;
    private static final int ADAPTED_FUSE_TICKS = 16;
    private static final int BURST_TICKS = 35;
    private static final int DISCARD_TICKS = 45;
    private static final float INITIAL_WIDTH = 0.5F;
    private static final float INITIAL_HEIGHT = 0.5F;
    private static final float FUSE_WIDTH_GROWTH = 0.8F;
    private static final float FUSE_HEIGHT_GROWTH = 0.482F;
    private static final float BURST_WIDTH_SHRINK = 0.1F;
    private static final float BURST_HEIGHT_SHRINK = 0.32F;
    private UUID ownerId;
    private UUID targetId;
    private int activeTicks;
    private int travelTicks;
    private int startTicks = DEFAULT_START_TICKS;
    private int fuseTicks = DEFAULT_FUSE_TICKS;
    private int fuseProgress;
    private int burstTicks;
    private float orbWidth = INITIAL_WIDTH;
    private float orbHeight = INITIAL_HEIGHT;
    private boolean ignited;
    private boolean burstApplied;
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
        if (owner.tier() == ParasiteTier.ADAPTED) {
            startTicks = ADAPTED_START_TICKS;
            fuseTicks = ADAPTED_FUSE_TICKS;
        }
        entityData.set(BOLSTER_ORB, owner instanceof AdaptedVariantEntity adapted && adapted.isAdaptedBolster());
    }

    public boolean isBolsterOrb() {
        return entityData.get(BOLSTER_ORB);
    }

    public void setAnchor(Vec3 anchor) {
        launched = false;
        anchored = true;
        activeTicks = 0;
        fuseProgress = 0;
        burstTicks = 0;
        ignited = false;
        burstApplied = false;
        orbWidth = INITIAL_WIDTH;
        orbHeight = INITIAL_HEIGHT;
        entityData.set(BURST_SCALE, 1.0F);
        anchorX = anchor.x;
        anchorY = anchor.y;
        anchorZ = anchor.z;
        setDeltaMovement(Vec3.ZERO);
        setPos(anchor);
        refreshDimensions();
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
        fuseProgress = 0;
        burstTicks = 0;
        ignited = false;
        burstApplied = false;
        orbWidth = INITIAL_WIDTH;
        orbHeight = INITIAL_HEIGHT;
        entityData.set(BURST_SCALE, 1.0F);
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
        int age = ++activeTicks;
        // The original orb pulse cadence is based on absolute orb age. It is
        // intentionally independent of the warm-up timer, so age 10 pulses
        // even when the orb is still waiting to ignite.
        if (age % 10 == 0) {
            applyOrbEffects(owner);
        }
        if (age <= startTicks) {
            return;
        }
        ignited = true;
        if (burstApplied) {
            tickBurst(owner);
            return;
        }

        // The tick which reaches the fuse enters terminal state without a
        // growth step. The preceding fuse-1 ticks each grow the orb.
        if (fuseProgress + 1 < fuseTicks) {
            fuseProgress++;
            orbWidth += FUSE_WIDTH_GROWTH;
            orbHeight += FUSE_HEIGHT_GROWTH;
            refreshDimensions();
        } else {
            fuseProgress = fuseTicks;
            burstApplied = true;
            burstTicks = 1;
        }
    }

    private void applyOrbEffects(PrimitiveParasiteEntity owner) {
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(2.5D), LivingEntity::isAlive);
        if (owner == null || !owner.isAlive()) {
            for (LivingEntity target : targets) {
                if (target instanceof Player player && !player.getAbilities().instabuild) {
                    applyOwnerlessCooldown(player);
                }
            }
            return;
        }
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(2.5D));
        int nearbyEntities = targets.size();
        for (LivingEntity target : targets) {
            owner.applyScaryOrbEffect(target, nearbyEntities);
            owner.applyScaryOrbMinimumDamage(target, 0.5F);
        }
    }

    private void applyOwnerlessCooldown(Player player) {
        Set<Item> cooledItems = new HashSet<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && cooledItems.add(stack.getItem())
                    && player.getCooldowns().getCooldownPercent(stack.getItem(), 0.0F) <= 0.0F) {
                player.getCooldowns().addCooldown(stack.getItem(), 100);
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && cooledItems.add(offhand.getItem())
                && player.getCooldowns().getCooldownPercent(offhand.getItem(), 0.0F) <= 0.0F) {
            player.getCooldowns().addCooldown(offhand.getItem(), 100);
        }
    }

    private void tickBurst(PrimitiveParasiteEntity owner) {
        burstTicks++;
        if (burstTicks > BURST_TICKS) {
            orbWidth = Math.max(0.1F, orbWidth - BURST_WIDTH_SHRINK);
            orbHeight = Math.max(0.1F, orbHeight - BURST_HEIGHT_SHRINK);
            entityData.set(BURST_SCALE, orbWidth / INITIAL_WIDTH);
            refreshDimensions();
            if (owner != null && owner.isAlive()) {
                DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox());
                for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox(), LivingEntity::isAlive)) {
                    owner.applyScaryOrbMinimumDamage(target, 5.0F);
                }
            }
        }
        if (burstTicks > DISCARD_TICKS) {
            discard();
        }
    }

    public float burstScale() {
        return entityData.get(BURST_SCALE);
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
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) ownerId = tag.getUUID("owner");
        if (tag.hasUUID("target")) targetId = tag.getUUID("target");
        activeTicks = tag.getInt("active_ticks");
        travelTicks = tag.getInt("travel_ticks");
        startTicks = tag.contains("start_ticks") ? tag.getInt("start_ticks") : DEFAULT_START_TICKS;
        fuseTicks = tag.contains("fuse_ticks") ? tag.getInt("fuse_ticks") : DEFAULT_FUSE_TICKS;
        fuseProgress = Math.max(0, tag.getInt("fuse_progress"));
        burstTicks = Math.max(0, tag.getInt("burst_ticks"));
        orbWidth = tag.contains("orb_width") ? tag.getFloat("orb_width") : INITIAL_WIDTH;
        orbHeight = tag.contains("orb_height") ? tag.getFloat("orb_height") : INITIAL_HEIGHT;
        ignited = tag.contains("ignited") ? tag.getBoolean("ignited") : activeTicks > startTicks;
        burstApplied = tag.getBoolean("burst_applied");
        launched = tag.getBoolean("launched");
        anchored = tag.getBoolean("anchored");
        anchorX = tag.getDouble("anchor_x");
        anchorY = tag.getDouble("anchor_y");
        anchorZ = tag.getDouble("anchor_z");
        entityData.set(BOLSTER_ORB, tag.getBoolean("bolster_orb"));
        entityData.set(BURST_SCALE, orbWidth / INITIAL_WIDTH);
        refreshDimensions();
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("owner", ownerId);
        if (targetId != null) tag.putUUID("target", targetId);
        tag.putInt("active_ticks", activeTicks);
        tag.putInt("travel_ticks", travelTicks);
        tag.putInt("start_ticks", startTicks);
        tag.putInt("fuse_ticks", fuseTicks);
        tag.putInt("fuse_progress", fuseProgress);
        tag.putInt("burst_ticks", burstTicks);
        tag.putFloat("orb_width", orbWidth);
        tag.putFloat("orb_height", orbHeight);
        tag.putBoolean("ignited", ignited);
        tag.putBoolean("burst_applied", burstApplied);
        tag.putBoolean("launched", launched);
        tag.putBoolean("anchored", anchored);
        tag.putDouble("anchor_x", anchorX);
        tag.putDouble("anchor_y", anchorY);
        tag.putDouble("anchor_z", anchorZ);
        tag.putBoolean("bolster_orb", entityData.get(BOLSTER_ORB));
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(Math.max(0.1F, orbWidth), Math.max(0.1F, orbHeight));
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
