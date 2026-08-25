package alku.csrp.entity;

import alku.csrp.config.WorldConfig;
import alku.csrp.network.CsrpNetwork;
import alku.csrp.network.MeteorShakePayload;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.MeteorImpactGenerator;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class MeteorEntity extends Entity {
    private static final EntityDataAccessor<Boolean> MAIN = SynchedEntityData.defineId(
            MeteorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_LIFETIME = 1200;
    private Vec3 acceleration = Vec3.ZERO;
    private int life;
    private boolean impacted;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(Vec3 direction, boolean main) {
        Vec3 normalized = direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, -1.0D, 0.0D)
                : direction.normalize();
        acceleration = normalized.scale(0.1D);
        entityData.set(MAIN, main);
    }

    public boolean isMainMeteor() {
        return entityData.get(MAIN);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(MAIN, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnTrail();
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel) || impacted) {
            return;
        }

        life++;
        if (isMainMeteor() && tickCount % 20 == 0) {
            sendShake(serverLevel, blockPosition(), 150, 20, 2.0F);
            if (random.nextBoolean()) {
                spawnFragment(serverLevel);
            }
        }

        Vec3 movement = getDeltaMovement();
        Vec3 start = position();
        Vec3 end = start.add(movement);
        HitResult hit = level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityHit = life >= 25 ? findEntityCollision(start, end) : null;
        if (entityHit != null && (hit.getType() == HitResult.Type.MISS
                || start.distanceToSqr(entityHit) < start.distanceToSqr(hit.getLocation()))) {
            impact(serverLevel, BlockPos.containing(entityHit));
            return;
        }
        if (hit.getType() != HitResult.Type.MISS || life > MAX_LIFETIME
                || getY() <= level().getMinBuildHeight() + 1) {
            impact(serverLevel, BlockPos.containing(hit.getType() == HitResult.Type.MISS ? position() : hit.getLocation()));
            return;
        }

        move(MoverType.SELF, movement);
        float drag = isInWater() ? 0.8F : 0.95F;
        setDeltaMovement(movement.add(acceleration).scale(drag));
        updateRotationFromMovement();
    }

    private void spawnFragment(ServerLevel level) {
        Entity created = getType().create(level);
        MeteorEntity fragment = created instanceof MeteorEntity meteor ? meteor : null;
        if (fragment == null) {
            return;
        }
        Vec3 direction = getDeltaMovement().add(
                (random.nextDouble() - 0.5D) * 0.9D,
                (random.nextDouble() - 0.5D) * 0.9D,
                (random.nextDouble() - 0.5D) * 0.9D);
        fragment.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        fragment.configure(direction, false);
        fragment.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(fragment);
    }

    private Vec3 findEntityCollision(Vec3 start, Vec3 end) {
        AABB swept = getBoundingBox().expandTowards(getDeltaMovement()).inflate(1.0D);
        Vec3 closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, swept,
                entity -> entity.isAlive() && !entity.isSpectator())) {
            Optional<Vec3> intersection = living.getBoundingBox().inflate(0.3D).clip(start, end);
            if (intersection.isPresent()) {
                double distance = start.distanceToSqr(intersection.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = intersection.get();
                }
            }
        }
        return closest;
    }

    private void impact(ServerLevel level, BlockPos hitPos) {
        impacted = true;
        BlockPos surface = MeteorImpactGenerator.surface(level, hitPos);
        spawnImpactPulse(level, hitPos, isMainMeteor() ? 40 : 8);
        if (isMainMeteor()) {
            sendShake(level, surface, 400, 150, 8.0F);
            damageMainImpact(level, surface);
            MeteorImpactGenerator.generateMain(level, surface, random);
            if (WorldConfig.meteorCreatesVector()) {
                SrpWorldData.get(level).setVector(surface, WorldConfig.meteorVectorHealth(),
                        WorldConfig.meteorVectorRadius());
            }
        } else {
            MeteorImpactGenerator.generateFragment(level, surface, random);
        }
        level.playSound(null, surface, ModSounds.METEOR_IMPACT.get(), SoundSource.HOSTILE,
                isMainMeteor() ? 16.0F : 5.0F, isMainMeteor() ? 0.7F : 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, surface.getX() + 0.5D, surface.getY() + 1.0D,
                surface.getZ() + 0.5D, isMainMeteor() ? 12 : 3, 2.0D, 1.0D, 2.0D, 0.0D);
        discard();
    }

    private static void sendShake(ServerLevel level, BlockPos center, int radius, int ticks, float multiplier) {
        double radiusSqr = (double) radius * radius;
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            double distanceSqr = player.blockPosition().distSqr(center);
            if (distanceSqr < radiusSqr) {
                float strength = (float) (1.0D - distanceSqr / radiusSqr) * multiplier;
                CsrpNetwork.sendToPlayer(player, new MeteorShakePayload(ticks, strength, false));
            }
        }
    }

    private void spawnImpactPulse(ServerLevel level, BlockPos center, int fuse) {
        OrbBoomEntity orb = ModEntities.ORB_BOOM.get().create(level);
        if (orb == null) {
            return;
        }
        orb.configure(null, fuse, 1);
        orb.moveTo(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
        level.addFreshEntity(orb);
    }

    private void damageMainImpact(ServerLevel level, BlockPos center) {
        int radius = WorldConfig.meteorDamageRadius();
        AABB area = new AABB(center).inflate(Math.max(radius, 800));
        Vec3 impact = Vec3.atCenterOf(center);
        double damageRadiusSqr = (double) radius * radius;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            double distanceSqr = living.position().distanceToSqr(impact);
            if (distanceSqr < damageRadiusSqr && radius > 0) {
                float strength = (float) (1.0D - distanceSqr / damageRadiusSqr);
                living.invulnerableTime = 0;
                living.hurt(damageSources().fellOutOfWorld(), 450.0F * strength);
            }
            if (distanceSqr < 320000.0D) {
                living.addEffect(new MobEffectInstance(ModMobEffects.COTH.get(), 1200, 0, false, false));
            }
        }
    }

    private void spawnTrail() {
        Vec3 movement = getDeltaMovement();
        level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5D, getZ(), 0.0D, 0.0D, 0.0D);
        if (isInWater()) {
            for (int index = 0; index < 4; index++) {
                level().addParticle(ParticleTypes.BUBBLE, getX() - movement.x * 0.25D,
                        getY() - movement.y * 0.25D, getZ() - movement.z * 0.25D,
                        movement.x, movement.y, movement.z);
            }
        }
        for (int index = 0; index < 5; index++) {
            double spread = getBbWidth();
            double x = getX() + (random.nextDouble() - 0.5D) * spread;
            double y = getY() + (random.nextDouble() - 0.5D) * getBbHeight() * 4.0D + getBbHeight();
            double z = getZ() + (random.nextDouble() - 0.5D) * spread;
            double velocityX = -movement.x + random.nextGaussian() * 0.05D;
            double velocityY = -movement.y + random.nextGaussian() * 0.05D;
            double velocityZ = -movement.z + random.nextGaussian() * 0.05D;
            level().addParticle(ParticleTypes.FLAME, x, y, z, velocityX, velocityY, velocityZ);
            level().addParticle(isMainMeteor() ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION,
                    x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        markHurt();
        return true;
    }

    private void updateRotationFromMovement() {
        Vec3 movement = getDeltaMovement();
        double horizontal = movement.horizontalDistance();
        if (movement.lengthSqr() > 1.0E-6D) {
            setYRot((float) (Math.atan2(movement.z, movement.x) * 180.0D / Math.PI) - 90.0F);
            setXRot((float) (-(Math.atan2(movement.y, horizontal) * 180.0D / Math.PI)));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(MAIN, !tag.contains("main") || tag.getBoolean("main"));
        acceleration = new Vec3(tag.getDouble("acceleration_x"), tag.getDouble("acceleration_y"),
                tag.getDouble("acceleration_z"));
        if (acceleration.lengthSqr() < 1.0E-6D) {
            acceleration = new Vec3(0.0D, -0.1D, 0.0D);
        }
        life = Math.max(0, tag.getInt("life"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("main", isMainMeteor());
        tag.putDouble("acceleration_x", acceleration.x);
        tag.putDouble("acceleration_y", acceleration.y);
        tag.putDouble("acceleration_z", acceleration.z);
        tag.putInt("life", life);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65536.0D;
    }
}
