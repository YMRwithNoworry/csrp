package alku.csrp.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Legacy Marauderized enderman: teleporting ambusher that tethers targets after a melee hit. */
public final class MarauderizedEndermanEntity extends TetheredMarauderizedEntity {
    private static final double TELEPORT_RADIUS = 32.0D;
    private static final double MIN_TARGET_DISTANCE_SQR = 4.0D;
    private static final int TELEPORT_COOLDOWN_TICKS = 20;

    private int teleportCooldown;

    public MarauderizedEndermanEntity(EntityType<? extends MarauderizedEndermanEntity> type, Level level) {
        super(type, level, 16);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMarauderizedAttributes(80.0D, 6.0D, 21.0D, 0.5D, 0.1496D, 64.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnPortalParticles();
            return;
        }

        tickTether();
        if (teleportCooldown > 0) {
            teleportCooldown--;
        }

        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && !hasPullTarget() && tickCount % 20 == 0
                && distanceToSqr(target) > MIN_TARGET_DISTANCE_SQR && random.nextInt(4) == 0) {
            teleportAwayFromTarget(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.is(DamageTypeTags.IS_PROJECTILE)) {
            for (int attempt = 0; attempt < 64; attempt++) {
                if (teleportAwayFromTarget(getTarget())) {
                    return true;
                }
            }
        }

        boolean damaged = super.hurt(source, amount);
        if (damaged && !level().isClientSide && teleportCooldown <= 0 && random.nextInt(4) == 0) {
            teleportAwayFromTarget(getTarget());
        }
        return damaged;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean damaged = super.doHurtTarget(target);
        if (damaged && target instanceof LivingEntity living) {
            captureTarget(living);
            if (!level().isClientSide && teleportCooldown <= 0 && random.nextInt(4) == 0) {
                teleportAwayFromTarget(getTarget());
            }
        }
        return damaged;
    }

    @Override
    protected int initialWeaknessAmplifier() {
        return 3;
    }

    @Override
    protected double pullStrength() {
        return 0.13D;
    }

    @Override
    protected double maxPullDistanceSqr() {
        return 9.0D;
    }

    @Override
    protected float tetherDamage() {
        return 0.02F;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return distance >= 60.0F && super.causeFallDamage(distance, damageMultiplier, source);
    }

    private void spawnPortalParticles() {
        for (int index = 0; index < 2; index++) {
            level().addParticle(ParticleTypes.PORTAL,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight() - 0.25D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    (random.nextDouble() - 0.5D) * 2.0D, -random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 2.0D);
        }
    }

    private boolean teleportAwayFromTarget(LivingEntity target) {
        if (teleportCooldown > 0 || hasPullTarget()) {
            return false;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 requested = position().add((random.nextDouble() - 0.5D) * TELEPORT_RADIUS * 2.0D,
                    random.nextInt(64) - 32, (random.nextDouble() - 0.5D) * TELEPORT_RADIUS * 2.0D);
            if (target != null && target.distanceToSqr(requested) < 100.0D) {
                continue;
            }
            if (tryTeleport(requested)) {
                teleportCooldown = TELEPORT_COOLDOWN_TICKS;
                return true;
            }
        }
        return false;
    }

    private boolean tryTeleport(Vec3 requested) {
        BlockPos landing = BlockPos.containing(requested);
        while (landing.getY() > level().getMinBuildHeight() && !level().getBlockState(landing).blocksMotion()) {
            landing = landing.below();
        }
        if (!level().getBlockState(landing).blocksMotion()) {
            return false;
        }

        Vec3 destination = new Vec3(requested.x, landing.getY() + 1.0D, requested.z);
        AABB movedBox = getBoundingBox().move(destination.subtract(position()));
        if (!level().noCollision(this, movedBox)) {
            return false;
        }

        teleportTo(destination.x, destination.y, destination.z);
        resetFallDistance();
        playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        return true;
    }
}
