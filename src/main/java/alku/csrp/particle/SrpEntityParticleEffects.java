package alku.csrp.particle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 原版实体粒子的体积采样公式。仅应从客户端实体事件或 tick 调用。 */
public final class SrpEntityParticleEffects {
    private SrpEntityParticleEffects() {
    }

    public static void spawnVomitCloud(LivingEntity entity, RandomSource random,
                                       ParticleOptions particle, int count) {
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        double baseVelocityX = -Mth.sin(yaw) * 0.2D;
        double baseVelocityZ = Mth.cos(yaw) * 0.2D;
        for (int index = 0; index < count; index++) {
            Vec3 direction = entity.getViewVector(1.0F);
            double x = entity.getX() + direction.x * 1.2D;
            double y = entity.getY() + entity.getEyeHeight() - 0.2D;
            double z = entity.getZ() + direction.z * 1.2D;
            double velocityY = 0.01D + random.nextDouble() * 0.1D;
            double velocityX = baseVelocityX + (random.nextDouble() - 0.5D) * 0.25D;
            double velocityZ = baseVelocityZ + (random.nextDouble() - 0.5D) * 0.25D;
            entity.level().addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    public static void spawnVolume(Entity entity, RandomSource random, ParticleOptions particle, int count) {
        spawnVolume(entity, random, particle, count, entity.getBbHeight());
    }

    public static void spawnVolume(Entity entity, RandomSource random, ParticleOptions particle,
                                   int count, double sampleHeight) {
        for (int index = 0; index < count; index++) {
            double velocityX = random.nextGaussian() * 0.02D;
            double velocityY = random.nextGaussian() * 0.02D;
            double velocityZ = random.nextGaussian() * 0.02D;
            double x = entity.getX() + random.nextFloat() * entity.getBbWidth() * 2.0F - entity.getBbWidth();
            double y = entity.getY() + 0.5D + random.nextFloat() * sampleHeight;
            double z = entity.getZ() + random.nextFloat() * entity.getBbWidth() * 2.0F - entity.getBbWidth();
            entity.level().addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
        }
    }
}
