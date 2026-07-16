package alku.csrp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class ScaryOrbEntity extends Entity {
    private static final int START_TICKS = 40;
    private static final int BURST_TICKS = 35;
    private static final int DISCARD_TICKS = 45;
    private UUID ownerId;
    private int activeTicks;

    public ScaryOrbEntity(EntityType<? extends ScaryOrbEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public ScaryOrbEntity(EntityType<? extends ScaryOrbEntity> type, Level level, PrimitiveParasiteEntity owner) {
        this(type, level);
        ownerId = owner.getUUID();
    }

    @Override public void tick() {
        super.tick();
        setDeltaMovement(0.0, 0.0, 0.0);
        PrimitiveParasiteEntity owner = owner();
        if (owner != null && owner.isAlive()) setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.5, owner.getZ());
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getRandomX(0.6), getRandomY(), getRandomZ(0.6),
                        random.nextGaussian() * 0.01, 0.02, random.nextGaussian() * 0.01);
            }
            return;
        }
        if (tickCount <= START_TICKS) return;
        activeTicks++;
        if (activeTicks % 10 == 0 && owner != null) applyOrbEffects(owner);
        if (activeTicks == BURST_TICKS && owner != null) owner.hurtNearby(this, 3.0,
                (float) owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.5F,
                false);
        if (activeTicks >= DISCARD_TICKS || owner == null || !owner.isAlive()) discard();
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

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) ownerId = tag.getUUID("owner");
        activeTicks = tag.getInt("active_ticks");
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("owner", ownerId);
        tag.putInt("active_ticks", activeTicks);
    }
}
