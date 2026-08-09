package alku.csrp.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared carrier for legacy auxiliary ids that are spawned by old saves or commands. */
public final class LegacyAuxiliaryEntity extends Entity {
    public enum Kind {
        SOURCE,
        REMAIN,
        BOMB,
        GORE,
        TENDRIL,
        WAVE
    }

    private final Kind kind;
    private int lifetime;

    public LegacyAuxiliaryEntity(EntityType<? extends LegacyAuxiliaryEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        this.lifetime = switch (kind) {
            case SOURCE -> 2400;
            case REMAIN -> 1200;
            case BOMB -> 120;
            default -> 200;
        };
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientParticle();
            return;
        }
        Vec3 movement = getDeltaMovement();
        if (kind == Kind.GORE || kind == Kind.WAVE) {
            setPos(position().add(movement));
            setDeltaMovement(movement.scale(0.96D).add(0.0D, kind == Kind.GORE ? -0.04D : 0.0D, 0.0D));
        }
        if (tickCount >= lifetime) {
            discard();
        }
    }

    private void spawnClientParticle() {
        if (tickCount % 5 != 0) {
            return;
        }
        ParticleOptions particle = switch (kind) {
            case BOMB -> ParticleTypes.EXPLOSION;
            case SOURCE, REMAIN, TENDRIL -> ParticleTypes.END_ROD;
            case GORE -> ParticleTypes.WITCH;
            case WAVE -> ParticleTypes.CLOUD;
        };
        level().addParticle(particle, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                0.0D, 0.02D, 0.0D);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("lifetime")) {
            lifetime = Math.max(1, tag.getInt("lifetime"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("lifetime", lifetime);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
