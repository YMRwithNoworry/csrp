package alku.csrp.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Server-only equivalent of SRP's short-lived EntityDamage hitbox. */
public final class HaunterDamageEntity extends Entity {
    private static final int LIFETIME_TICKS = 10;
    private static final float DEFAULT_KNOCKBACK_STRENGTH = 3.0F;

    private UUID ownerId;
    private float knockbackStrength = DEFAULT_KNOCKBACK_STRENGTH;

    public HaunterDamageEntity(EntityType<? extends HaunterDamageEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(PreeminentParasiteEntity owner, Vec3 position, float knockbackStrength) {
        ownerId = owner.getUUID();
        this.knockbackStrength = knockbackStrength;
        setPos(position);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        PreeminentParasiteEntity owner = owner();
        if (owner != null) {
            DragonEggAssimilationEntity.assimilateDragonEggs(level(),
                    getBoundingBox().inflate(0.3D, 0.0D, 0.2D));
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(0.3D, 0.0D, 0.2D), candidate -> isValidTarget(owner, candidate))) {
                knockBack(owner, target, knockbackStrength);
                owner.doHurtTarget(target);
            }
        }
        if (tickCount > LIFETIME_TICKS) {
            discard();
        }
    }

    private static boolean isValidTarget(PreeminentParasiteEntity owner, LivingEntity target) {
        return target != owner && target.isAlive() && !target.isInvulnerable() && !(target instanceof Parasite)
                && !owner.isAlliedTo(target);
    }

    private static void knockBack(PreeminentParasiteEntity owner, LivingEntity target, float knockbackStrength) {
        double xRatio = owner.getX() - target.getX();
        double zRatio = owner.getZ() - target.getZ();
        double horizontalLength = Math.sqrt(xRatio * xRatio + zRatio * zRatio);
        if (horizontalLength < 0.001D) {
            return;
        }
        Vec3 motion = target.getDeltaMovement();
        double vertical = motion.y;
        if (target.onGround()) {
            vertical = Math.min(0.4D, vertical * 0.5D + knockbackStrength);
        }
        target.setDeltaMovement(motion.x * 0.5D - xRatio / horizontalLength * knockbackStrength,
                vertical, motion.z * 0.5D - zRatio / horizontalLength * knockbackStrength);
        target.hurtMarked = true;
    }

    private PreeminentParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof PreeminentParasiteEntity preeminent ? preeminent : null;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        if (tag.contains("knockback_strength")) {
            knockbackStrength = tag.getFloat("knockback_strength");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        tag.putFloat("knockback_strength", knockbackStrength);
    }
}
