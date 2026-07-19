package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
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
        LIGHT,
        ACID,
        VOMIT
    }

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HOMING_TARGET = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.INT);

    private UUID ownerId;
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
        configure(owner, mode, start, target, speed, damage, radius, maximumLifetime, null);
    }

    public void configure(PrimitiveParasiteEntity owner, Mode mode, Vec3 start, Vec3 target,
                          double speed, float damage, double radius, int maximumLifetime,
                          LivingEntity homingTarget) {
        ownerId = owner.getUUID();
        entityData.set(MODE, mode.ordinal());
        entityData.set(HOMING_TARGET, homingTarget == null ? 0 : homingTarget.getId());
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
        Mode mode = getMode();
        PrimitiveParasiteEntity owner = owner();
        if (!level().isClientSide && (owner == null || !owner.isAlive())) {
            discard();
            return;
        }

        Vec3 start = position();
        Vec3 movement = steerTowardsHomingTarget(owner, getDeltaMovement(), mode);
        Vec3 end = start.add(movement);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        setPos(end.x, end.y, end.z);

        if (level().isClientSide) {
            ParticleOptions particle = switch (mode) {
                case BOMB, METEOR -> ParticleTypes.FLAME;
                case LIGHT -> ParticleTypes.SOUL_FIRE_FLAME;
                case SPINE -> ParticleTypes.CRIT;
                case ACID, VOMIT -> ParticleTypes.WITCH;
            };
            level().addParticle(particle, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            return;
        }

        if (mode == Mode.BOMB || mode == Mode.METEOR || mode == Mode.ACID || mode == Mode.VOMIT) {
            setDeltaMovement(movement.add(0.0, -0.025, 0.0));
        }

        LivingEntity hit = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.65),
                        target -> owner != null && owner.isValidParasiteTarget(target))
                .stream().findFirst().orElse(null);
        if (blockHit.getType() != HitResult.Type.MISS || hit != null || tickCount >= maximumLifetime) {
            impact(owner, mode);
        }
    }

    private Vec3 steerTowardsHomingTarget(PrimitiveParasiteEntity owner, Vec3 movement, Mode mode) {
        if (level().isClientSide || mode != Mode.LIGHT || tickCount < 10 || owner == null) {
            return movement;
        }
        Entity entity = level().getEntity(entityData.get(HOMING_TARGET));
        if (!(entity instanceof LivingEntity target) || !owner.isValidParasiteTarget(target)) {
            return movement;
        }
        Vec3 direction = target.getEyePosition().subtract(position());
        if (direction.lengthSqr() < 0.001D) {
            return movement;
        }
        return movement.scale(0.78D).add(direction.normalize().scale(0.42D));
    }

    private void impact(PrimitiveParasiteEntity owner, Mode mode) {
        if (owner == null) {
            discard();
            return;
        }
        boolean launch = mode == Mode.BOMB || mode == Mode.METEOR || mode == Mode.ACID;
        owner.hurtNearby(this, radius, damage, launch);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius), owner::isValidParasiteTarget)) {
            switch (mode) {
                case SPINE -> {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), owner);
                    if (owner instanceof DeterrentParasiteEntity deterrent
                            && deterrent.getKind() == DeterrentParasiteEntity.Kind.SENTRY) {
                        deterrent.applySentrySpineEffects(target);
                    }
                }
                case LIGHT -> {
                    target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 100, 0), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 0), owner);
                }
                case ACID -> {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 180, 0), owner);
                }
                case VOMIT -> {
                    target.addEffect(new MobEffectInstance(ModMobEffects.COTH, 160, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 160, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 160, 0), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 1), owner);
                }
                case BOMB, METEOR -> target.igniteForSeconds(4.0F);
            }
        }
        if (mode == Mode.BOMB || mode == Mode.METEOR) {
            spawnLingeringCothCloud(owner);
        } else if (mode == Mode.ACID && radius >= 1.25D) {
            spawnLingeringAcidCloud(owner);
        } else if (mode == Mode.VOMIT) {
            spawnLingeringVomitCloud(owner);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(mode == Mode.LIGHT ? ParticleTypes.SOUL_FIRE_FLAME
                            : mode == Mode.ACID || mode == Mode.VOMIT ? ParticleTypes.WITCH : ParticleTypes.EXPLOSION,
                    getX(), getY(), getZ(), 12, radius * 0.25, radius * 0.25, radius * 0.25, 0.02);
        }
        discard();
    }

    private void spawnLingeringCothCloud(PrimitiveParasiteEntity owner) {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius((float) Math.max(2.0D, radius + 1.0D));
        cloud.setDuration(60);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 300, 0, false, true));
        level().addFreshEntity(cloud);
    }

    private void spawnLingeringAcidCloud(PrimitiveParasiteEntity owner) {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius((float) Math.max(2.0D, radius + 0.5D));
        cloud.setDuration(60);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 180, 0, false, true));
        level().addFreshEntity(cloud);
    }

    private void spawnLingeringVomitCloud(PrimitiveParasiteEntity owner) {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius((float) Math.max(2.0D, radius));
        cloud.setDuration(70);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 160, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 160, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 160, 0, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 1, false, true));
        level().addFreshEntity(cloud);
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
        builder.define(MODE, Mode.SPINE.ordinal());
        builder.define(HOMING_TARGET, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        entityData.set(MODE, sanitizeMode(tag.getInt("mode")));
        entityData.set(HOMING_TARGET, tag.getInt("homing_target"));
        damage = tag.getFloat("damage");
        radius = tag.getDouble("radius");
        maximumLifetime = tag.getInt("maximum_lifetime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        tag.putInt("mode", entityData.get(MODE));
        tag.putInt("homing_target", entityData.get(HOMING_TARGET));
        tag.putFloat("damage", damage);
        tag.putDouble("radius", radius);
        tag.putInt("maximum_lifetime", maximumLifetime);
    }

    public Mode getMode() {
        return Mode.values()[sanitizeMode(entityData.get(MODE))];
    }

    private static int sanitizeMode(int modeIndex) {
        return modeIndex >= 0 && modeIndex < Mode.values().length ? modeIndex : Mode.SPINE.ordinal();
    }
}
