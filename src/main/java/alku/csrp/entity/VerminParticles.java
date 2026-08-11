package alku.csrp.entity;

import alku.csrp.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class VerminParticles {
    private static final int AMBIENT_SPLASH_COUNT = 5;
    private static final int PAYLOAD_SPLASH_COUNT = 3;
    private static final int PAYLOAD_SPRAY_COUNT = 5;
    private static final int PAYLOAD_CLOUD_COUNT = 5;
    private static final int LARGE_SPRAY_COUNT = 33;
    private static final int LARGE_CLOUD_COUNT = 13;

    private VerminParticles() {
    }

    static void spawnMouthDrips(Level level, Entity source) {
        RandomSource random = level.random;
        for (int index = 0; index < AMBIENT_SPLASH_COUNT; index++) {
            Vec3 velocity = sprayVelocity(source, random, 0.2D, 0.0D);
            level.addParticle(ModParticles.ASSIMILATION_SPLASH.get(),
                    source.getX(), source.getY(), source.getZ(),
                    velocity.x, velocity.y, velocity.z);
        }
    }

    static void sendPayloadBurst(ServerLevel level, Entity source) {
        sendType10Burst(level, source);
    }

    static void sendType10Burst(ServerLevel level, Entity source) {
        double x = source.getX();
        double y = source.getY();
        double z = source.getZ();
        level.sendParticles(ModParticles.ASSIMILATION_SPLASH.get(), x, y + 0.75D, z,
                PAYLOAD_SPLASH_COUNT, 0.5D, 0.25D, 0.5D, 0.02D);
        for (int index = 0; index < PAYLOAD_SPRAY_COUNT; index++) {
            Vec3 velocity = sprayVelocity(source, level.random, 1.0D, 4.0D);
            level.sendParticles(ModParticles.ASSIMILATION_SPLASH.get(), x, y + 1.2D, z,
                    0, velocity.x, velocity.y, velocity.z, 1.0D);
        }
        level.sendParticles(ModParticles.GORE_CLOUD.get(), x, y + 0.75D, z,
                PAYLOAD_CLOUD_COUNT, 0.5D, 0.25D, 0.5D, 0.02D);
    }

    static void sendType11Burst(ServerLevel level, Entity source) {
        double x = source.getX();
        double y = source.getY();
        double z = source.getZ();
        for (int index = 0; index < LARGE_SPRAY_COUNT; index++) {
            Vec3 velocity = sprayVelocity(source, level.random, 1.0D, 4.0D);
            level.sendParticles(ModParticles.ASSIMILATION_SPLASH.get(), x, y + 1.2D, z,
                    0, velocity.x, velocity.y, velocity.z, 1.0D);
        }
        level.sendParticles(ModParticles.GORE_CLOUD.get(), x, y + 0.75D, z,
                LARGE_CLOUD_COUNT, 0.5D, 0.25D, 0.5D, 0.02D);
    }

    static void sendContactBursts(ServerLevel level, Entity source, boolean converted) {
        if (converted) {
            repeat(level, source, 2, true);
            repeat(level, source, 3, false);
        } else {
            repeat(level, source, 4, false);
        }
    }

    private static void repeat(ServerLevel level, Entity source, int count, boolean large) {
        for (int index = 0; index < count; index++) {
            if (large) {
                sendType11Burst(level, source);
            } else {
                sendType10Burst(level, source);
            }
        }
    }

    private static Vec3 sprayVelocity(Entity source, RandomSource random,
                                      double horizontalFactor, double verticalFactor) {
        double sampleX = (float) source.getX() + random.nextFloat();
        double sampleY = (float) source.getY() + random.nextFloat();
        double sampleZ = (float) source.getZ() + random.nextFloat();
        double dx = sampleX - source.getX();
        double dy = sampleY - source.getY();
        double dz = sampleZ - source.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 1.0E-7D) {
            return Vec3.ZERO;
        }
        dx /= distance;
        dy /= distance;
        dz /= distance;
        double strength = 0.5D / (distance / 4.0D + 0.1D);
        strength *= random.nextFloat() * random.nextFloat() + 0.3F;
        dx = Math.min(dx * strength * horizontalFactor, 0.2D) * randomSign(random);
        dy = Math.min(dy * strength * verticalFactor, 0.6D) * randomSign(random);
        dz = Math.min(dz * strength * horizontalFactor, 0.2D) * randomSign(random);
        return new Vec3(dx, dy, dz);
    }

    private static double randomSign(RandomSource random) {
        return random.nextDouble() * 2.0D - 1.0D;
    }
}
