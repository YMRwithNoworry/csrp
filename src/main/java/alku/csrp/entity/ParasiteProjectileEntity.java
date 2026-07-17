package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ParasiteProjectileEntity extends Entity {
    public enum Mode {
        BOMB,
        SPINE,
        METEOR,
        LIGHT
    }

    private UUID ownerId;
    private Mode mode = Mode.SPINE;
    private float damage = 4.0F;
    private double radius = 1.0;
    private int maximumLifetime = 80;

    public ParasiteProjectileEntity(EntityType<? extends ParasiteProjectileEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(PrimitiveParasiteEntity owner, Mode mode, Vec3 start, Vec3 target,
                          double speed, float damage, double radius, int maximumLifetime) {
        ownerId = owner.getUUID();
        this.mode = mode;
        this.damage = damage;
        this.radius = radius;
        this.maximumLifetime = maximumLifetime;
        setPos(start);
        Vec3 direction = target.subtract(start);
        if (direction.lengthSqr() > 0.001) {
            setDeltaMovement(direction.normalize().scale(speed));
        }
    }

    @Override
    public void tick() {
        super.tick();
        PrimitiveParasiteEntity owner = owner();
        if (!level().isClientSide && (owner == null || !owner.isAlive())) {
            discard();
            return;
        }

        Vec3 start = position();
        Vec3 movement = getDeltaMovement();
        Vec3 end = start.add(movement);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        setPos(end.x, end.y, end.z);

        if (level().isClientSide) {
            ParticleOptions particle = switch (mode) {
                case BOMB, METEOR -> ParticleTypes.FLAME;
                case LIGHT -> ParticleTypes.SOUL_FIRE_FLAME;
                case SPINE -> ParticleTypes.CRIT;
            };
            level().addParticle(particle, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            return;
        }

        if (mode == Mode.BOMB || mode == Mode.METEOR) {
            setDeltaMovement(movement.add(0.0, -0.025, 0.0));
        }

        LivingEntity hit = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.65),
                        target -> owner != null && owner.isValidParasiteTarget(target))
                .stream().findFirst().orElse(null);
        if (blockHit.getType() != HitResult.Type.MISS || hit != null || tickCount >= maximumLifetime) {
            impact(owner);
        }
    }

    private void impact(PrimitiveParasiteEntity owner) {
        if (owner == null) {
            discard();
            return;
        }
        boolean launch = mode == Mode.BOMB || mode == Mode.METEOR;
        owner.hurtNearby(this, radius, damage, launch);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius), owner::isValidParasiteTarget)) {
            switch (mode) {
                case SPINE -> {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), owner);
                }
                case LIGHT -> target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 100, 0), owner);
                case BOMB, METEOR -> target.igniteForSeconds(4.0F);
            }
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(mode == Mode.LIGHT ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.EXPLOSION,
                    getX(), getY(), getZ(), 12, radius * 0.25, radius * 0.25, radius * 0.25, 0.02);
        }
        discard();
    }

    private PrimitiveParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof PrimitiveParasiteEntity parasite ? parasite : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        int modeIndex = tag.getInt("mode");
        mode = modeIndex >= 0 && modeIndex < Mode.values().length ? Mode.values()[modeIndex] : Mode.SPINE;
        damage = tag.getFloat("damage");
        radius = tag.getDouble("radius");
        maximumLifetime = tag.getInt("maximum_lifetime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        tag.putInt("mode", mode.ordinal());
        tag.putFloat("damage", damage);
        tag.putDouble("radius", radius);
        tag.putInt("maximum_lifetime", maximumLifetime);
    }
}
