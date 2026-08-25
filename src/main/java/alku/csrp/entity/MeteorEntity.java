package alku.csrp.entity;

import alku.csrp.config.WorldConfig;
import alku.csrp.network.CsrpNetwork;
import alku.csrp.network.MeteorShakePayload;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModEntities;
import alku.csrp.world.MeteorImpactGenerator;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this,
                entity -> life >= 25 && entity.isPickable() && !entity.isSpectator());
        if (hit.getType() != HitResult.Type.MISS || life > MAX_LIFETIME
                || getY() <= level().getMinBuildHeight() + 1) {
            impact(serverLevel);
            return;
        }

        move(MoverType.SELF, movement);
        ProjectileUtil.rotateTowardsMovement(this, 0.2F);
        float drag = isInWater() ? 0.8F : 0.95F;
        setDeltaMovement(movement.add(acceleration).scale(drag));
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

    private void impact(ServerLevel level) {
        impacted = true;
        BlockPos hitPos = blockPosition();
        BlockPos surface = MeteorImpactGenerator.surface(level, hitPos);
        spawnImpactPulse(level, hitPos, isMainMeteor() ? 40 : 8);
        if (isMainMeteor()) {
            sendShake(level, hitPos, 400, 150, 8.0F);
            damageMainImpact(level, hitPos);
            MeteorImpactGenerator.generateMain(level, surface, random);
            if (WorldConfig.meteorCreatesVector()) {
                SrpWorldData.get(level).setVector(hitPos, WorldConfig.meteorVectorHealth(),
                        WorldConfig.meteorVectorRadius());
            }
        } else {
            MeteorImpactGenerator.generateFragment(level, surface, random);
        }
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
