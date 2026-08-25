package alku.csrp.entity;

import alku.csrp.config.WorldConfig;
import alku.csrp.network.CsrpNetwork;
import alku.csrp.network.MeteorShakePayload;
import alku.csrp.registry.ModMobEffects;
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
        HitResult hit = level().clip(new ClipContext(position(), position().add(movement),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
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
        fragment.setDeltaMovement(getDeltaMovement().scale(0.35D));
        level.addFreshEntity(fragment);
    }

    private void impact(ServerLevel level, BlockPos hitPos) {
        impacted = true;
        BlockPos surface = MeteorImpactGenerator.surface(level, hitPos);
        if (isMainMeteor()) {
            sendShake(level, surface, 400, 150, 8.0F);
            damageMainImpact(level, surface);
            MeteorImpactGenerator.generateMain(level, surface, random);
            if (WorldConfig.meteorCreatesVector()) {
                SrpWorldData.get(level).setVector(surface, WorldConfig.meteorVectorHealth(),
                        WorldConfig.meteorVectorRadius());
            }
        } else {
            damageFragmentImpact(level, surface);
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
                CsrpNetwork.sendToPlayer(player, new MeteorShakePayload(ticks, strength));
            }
        }
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
                living.hurt(damageSources().magic(), 450.0F * strength);
            }
            if (distanceSqr < 320000.0D) {
                living.addEffect(new MobEffectInstance(ModMobEffects.COTH.get(), 1200, 0, false, false));
            }
        }
    }

    private void damageFragmentImpact(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(8.0D);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            living.invulnerableTime = 0;
            living.hurt(damageSources().magic(), 10.0F);
            living.setSecondsOnFire(4);
        }
    }

    private void spawnTrail() {
        Vec3 movement = getDeltaMovement();
        int count = isMainMeteor() ? 8 : 4;
        for (int index = 0; index < count; index++) {
            double spread = isMainMeteor() ? 2.25D : 0.8D;
            level().addParticle(index % 3 == 0 ? ParticleTypes.EXPLOSION : ParticleTypes.FLAME,
                    getX() + (random.nextDouble() - 0.5D) * spread,
                    getY() + random.nextDouble() * getBbHeight(),
                    getZ() + (random.nextDouble() - 0.5D) * spread,
                    -movement.x * 0.15D, -movement.y * 0.15D, -movement.z * 0.15D);
        }
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
