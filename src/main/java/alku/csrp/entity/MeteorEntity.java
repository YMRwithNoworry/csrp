package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.network.MeteorShakePayload;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.MeteorCrashFeature;
import alku.csrp.world.SrpCoreSystems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

/**
 * Port of SRParasites 1.10.8 {@code EntityMeteor} (in-game name "Hive Satellite").
 *
 * <p>The root meteor is launched from the sky at zero velocity and accelerates toward its target
 * at 0.1 blocks/tick with 0.95 drag, exactly like the original. It sheds smaller fragment meteors
 * every 20 ticks and shakes nearby players while it falls. Impact damages and infects everything
 * in range, spawns the shockwave orb, then runs {@link MeteorCrashFeature}.</p>
 */
public final class MeteorEntity extends Entity {
    private static final EntityDataAccessor<Boolean> ROOT = SynchedEntityData.defineId(
            MeteorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double LAUNCH_ACCELERATION = 0.1D;
    private static final double FRAGMENT_SPREAD = 0.9D;
    private static final int MAX_LIFETIME = 1200;

    private int ticksInAir;
    private Vec3 acceleration = Vec3.ZERO;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setRoot(true);
    }

    public void configureLaunch(Vec3 start, Vec3 direction) {
        setPos(start.x, start.y, start.z);
        setDeltaMovement(Vec3.ZERO);
        acceleration = direction.lengthSqr() > 1.0E-6D
                ? direction.normalize().scale(LAUNCH_ACCELERATION) : Vec3.ZERO;
        ticksInAir = 0;
        setRoot(true);
    }

    public void setRoot(boolean root) {
        entityData.set(ROOT, root);
    }

    public boolean isRoot() {
        return entityData.get(ROOT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ROOT, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            spawnClientParticles();
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level();
        if (isRoot() && tickCount % 20 == 0) {
            for (ServerPlayer player : serverLevel.players()) {
                float strength = distancePack(blockPosition(), player.blockPosition(), 150);
                if (strength > 0.0F) {
                    MeteorShakePayload.send(player, 20, 0, false, true, strength * 2.0F);
                }
            }
            if (random.nextInt(2) == 0) {
                spawnFragment(serverLevel);
            }
        }

        ticksInAir++;
        HitResult hit = raycast();
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            onImpact();
            return;
        }

        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y,
                getZ() + getDeltaMovement().z);
        ProjectileUtil.rotateTowardsMovement(this, 0.2F);

        float factor = 0.95F;
        if (isInWater()) {
            for (int i = 0; i < 4; i++) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE, getX() - getDeltaMovement().x * 0.25D,
                        getY() - getDeltaMovement().y * 0.25D, getZ() - getDeltaMovement().z * 0.25D,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            factor = 0.8F;
        }
        setDeltaMovement(getDeltaMovement().add(acceleration).scale(factor));
        serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5D, getZ(), 1,
                0.0D, 0.0D, 0.0D, 0.0D);

        if (ticksInAir > MAX_LIFETIME || getY() <= 0.0D) {
            onImpact();
        }
    }

    private HitResult raycast() {
        if (ticksInAir >= 25) {
            return ProjectileUtil.getHitResultOnMoveVector(this,
                    entity -> entity != this && entity.isAlive());
        }
        return ProjectileUtil.getEntityHitResult(level(), this, position(),
                position().add(getDeltaMovement()),
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(1.0D),
                entity -> entity != this && entity.isAlive());
    }

    private void spawnFragment(ServerLevel serverLevel) {
        MeteorEntity fragment = ModEntities.HIVE_SATELLITE.get().create(serverLevel);
        if (fragment == null) {
            return;
        }
        Vec3 velocity = getDeltaMovement().add(
                (random.nextDouble() - 0.5D) * FRAGMENT_SPREAD,
                (random.nextDouble() - 0.5D) * FRAGMENT_SPREAD,
                (random.nextDouble() - 0.5D) * FRAGMENT_SPREAD);
        fragment.setPos(getX(), getY(), getZ());
        fragment.setRoot(false);
        fragment.acceleration = velocity.lengthSqr() > 1.0E-6D
                ? velocity.normalize().scale(LAUNCH_ACCELERATION) : Vec3.ZERO;
        fragment.setDeltaMovement(Vec3.ZERO);
        serverLevel.addFreshEntity(fragment);
    }

    private void onImpact() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        if (isRoot()) {
            int damageRadius = Config.meteorDamageRadius();
            for (Entity entity : serverLevel.getAllEntities()) {
                if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                    continue;
                }
                if (living instanceof ServerPlayer player) {
                    float strength = distancePack(blockPosition(), player.blockPosition(), 400);
                    if (strength > 0.0F) {
                        MeteorShakePayload.send(player, 150, 0, false, true, strength * 8.0F);
                    }
                }
                float strength = distancePack(blockPosition(), living.blockPosition(), damageRadius);
                if (strength > 0.0F) {
                    living.hurt(damageSources().fallingBlock(this), strength * 450.0F);
                }
                strength = distancePack(blockPosition(), living.blockPosition(), 800);
                if (strength > 0.5F) {
                    InfectionMechanics.applyCothEffect(living, this, 1200, 0, false, false);
                }
            }
        }

        int orbRadius = isRoot() ? 40 : 8;
        OrbBoomEntity orb = ModEntities.ORB_BOOM.get().create(serverLevel);
        if (orb != null) {
            orb.setPos(getX(), getY(), getZ());
            orb.configure(null, orbRadius, 1);
            serverLevel.addFreshEntity(orb);
        }

        MeteorCrashFeature.generate(serverLevel, RandomSource.create(), blockPosition(),
                isRoot() ? 5 : 1);

        if (isRoot()) {
            serverLevel.playSound(null, getX(), getY(), getZ(), ModSounds.METEOR_IMPACT.get(),
                    SoundSource.AMBIENT, 8.0F, 1.0F);
            if (Config.meteorCreatesVector()) {
                SrpCoreSystems.placeVector(serverLevel, blockPosition(), 350, 200);
            }
        }
        discard();
    }

    private void spawnClientParticles() {
        for (int i = 0; i < 5; i++) {
            double offsetX = (random.nextDouble() - 0.5D) * getBbWidth();
            double offsetY = (random.nextDouble() - 0.5D) * getBbHeight() * 4.0D;
            double offsetZ = (random.nextDouble() - 0.5D) * getBbWidth();
            double x = getX() + offsetX;
            double y = getY() + offsetY + getBbHeight();
            double z = getZ() + offsetZ;
            double vx = -getDeltaMovement().x + random.nextGaussian() * 0.05D;
            double vy = -getDeltaMovement().y + random.nextGaussian() * 0.05D;
            double vz = -getDeltaMovement().z + random.nextGaussian() * 0.05D;
            level().addParticle(ParticleTypes.FLAME, x, y, z, vx, vy, vz);
            level().addParticle(isRoot() ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION,
                    x, y, z, vx, vy, vz);
        }
    }

    public static float distancePack(BlockPos first, BlockPos second, int maxDistance) {
        if (maxDistance <= 0) {
            return 0.0F;
        }
        double maxDistSq = (double) maxDistance * maxDistance;
        double distSq = first.distSqr(second);
        double value = 1.0D - distSq / maxDistSq;
        return (float) Math.max(0.0D, Math.min(1.0D, value));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65536.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setRoot(!tag.contains("bigmet") || tag.getBoolean("bigmet"));
        if (tag.contains("direction")) {
            setDeltaMovement(tag.getDouble("direction_x"), tag.getDouble("direction_y"),
                    tag.getDouble("direction_z"));
        }
        if (tag.contains("power")) {
            acceleration = new Vec3(tag.getDouble("power_x"), tag.getDouble("power_y"),
                    tag.getDouble("power_z"));
        }
        ticksInAir = tag.getInt("life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        Vec3 motion = getDeltaMovement();
        tag.putDouble("direction_x", motion.x);
        tag.putDouble("direction_y", motion.y);
        tag.putDouble("direction_z", motion.z);
        tag.putDouble("power_x", acceleration.x);
        tag.putDouble("power_y", acceleration.y);
        tag.putDouble("power_z", acceleration.z);
        tag.putInt("life", ticksInAir);
        tag.putBoolean("bigmet", isRoot());
    }
}
