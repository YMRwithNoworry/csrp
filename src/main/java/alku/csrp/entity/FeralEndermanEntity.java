package alku.csrp.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Feral Enderman teleport combat and parasite relocation from the legacy implementation. */
public final class FeralEndermanEntity extends FeralParasiteEntity {
    private static final double TELEPORT_RADIUS = 32.0D;
    private static final double MIN_TARGET_DISTANCE_SQR = 49.0D;
    private static final int TELEPORT_COOLDOWN_TICKS = 20;
    private static final int ALLY_TELEPORT_COOLDOWN_TICKS = 200;

    private int teleportCooldown;
    private int allyTeleportCooldown;

    public FeralEndermanEntity(EntityType<? extends FeralEndermanEntity> type, Level level) {
        super(type, level, Kind.ENDERMAN);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnPortalParticles();
            return;
        }

        if (teleportCooldown > 0) {
            teleportCooldown--;
        }
        if (allyTeleportCooldown > 0) {
            allyTeleportCooldown--;
        }

        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && tickCount % 20 == 0 && teleportCooldown <= 0
                && distanceToSqr(target) > MIN_TARGET_DISTANCE_SQR) {
            if (!teleportAllyToTarget(target)) {
                teleportAwayFromTarget(target);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !level().isClientSide && teleportCooldown <= 0 && random.nextBoolean()) {
            teleportAwayFromTarget(getTarget());
        }
        return damaged;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean damaged = super.doHurtTarget(target);
        if (damaged && !level().isClientSide && teleportCooldown <= 0 && random.nextBoolean()) {
            teleportAwayFromTarget(getTarget());
        }
        return damaged;
    }

    private void spawnPortalParticles() {
        for (int i = 0; i < 2; i++) {
            level().addParticle(ParticleTypes.PORTAL,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight() - 0.25D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    (random.nextDouble() - 0.5D) * 2.0D, -random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 2.0D);
        }
    }

    private boolean teleportAllyToTarget(LivingEntity target) {
        if (allyTeleportCooldown > 0) {
            return false;
        }
        List<Mob> allies = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(64.0D),
                ally -> ally != this && ally instanceof Parasite && ally.isAlive() && ally.getTarget() == null);
        for (Mob ally : allies) {
            for (int attempt = 0; attempt < 8; attempt++) {
                Vec3 destination = target.position().add((random.nextDouble() - 0.5D) * 8.0D,
                        random.nextInt(5) - 2, (random.nextDouble() - 0.5D) * 8.0D);
                if (teleportEntity(ally, destination)) {
                    ally.setTarget(target);
                    allyTeleportCooldown = ALLY_TELEPORT_COOLDOWN_TICKS;
                    teleportCooldown = TELEPORT_COOLDOWN_TICKS;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean teleportAwayFromTarget(LivingEntity target) {
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 destination = position().add((random.nextDouble() - 0.5D) * TELEPORT_RADIUS * 2.0D,
                    random.nextInt(64) - 32, (random.nextDouble() - 0.5D) * TELEPORT_RADIUS * 2.0D);
            if (target != null && target.distanceToSqr(destination) < MIN_TARGET_DISTANCE_SQR) {
                continue;
            }
            if (teleportEntity(this, destination)) {
                teleportCooldown = TELEPORT_COOLDOWN_TICKS;
                return true;
            }
        }
        return false;
    }

    private boolean teleportEntity(Entity entity, Vec3 requestedPosition) {
        BlockPos position = BlockPos.containing(requestedPosition);
        while (position.getY() > level().getMinBuildHeight() && !level().getBlockState(position).blocksMotion()) {
            position = position.below();
        }
        if (!level().getBlockState(position).blocksMotion()) {
            return false;
        }

        Vec3 destination = new Vec3(requestedPosition.x, position.getY() + 1.0D, requestedPosition.z);
        AABB movedBox = entity.getBoundingBox().move(destination.subtract(entity.position()));
        if (!level().noCollision(entity, movedBox)) {
            return false;
        }

        entity.teleportTo(destination.x, destination.y, destination.z);
        entity.resetFallDistance();
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        return true;
    }
}
