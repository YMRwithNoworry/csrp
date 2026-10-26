package alku.csrp.client;

import alku.csrp.network.SrpParticleBurstPayload;
import alku.csrp.particle.SrpColorParticleOptions;
import alku.csrp.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SrpParticleEffectsClient {
    private static final int TYPE10_SPLASH_COUNT = 3;
    private static final int TYPE10_SPRAY_COUNT = 5;
    private static final int TYPE10_CLOUD_COUNT = 5;
    private static final int TYPE11_SPRAY_COUNT = 33;
    private static final int TYPE11_CLOUD_COUNT = 13;

    private SrpParticleEffectsClient() {
    }

    public static void play(SrpParticleBurstPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        double x = payload.x();
        double y = payload.y();
        double z = payload.z();
        float width = payload.width();
        float height = payload.height();
        RandomSource random = RandomSource.create();
        switch (payload.particleType()) {
            case 1 -> spawnVolume(level, random, goreSplash(0), x, y, z, 1, width, height);
            case 2 -> spawnVolume(level, random, goreCloud(0, 0, 0), x, y, z, 4, width, height);
            case 3 -> spawnVolume(level, random, ModParticles.RHAPPY.get(), x, y, z, 6, width, height);
            case 4 -> spawnVolume(level, random, goreCloud(125, 227, 118), x, y, z, 24, width, height);
            case 10 -> {
                spawnVolume(level, random, goreSplash(0), x, y, z,
                        TYPE10_SPLASH_COUNT, width, height);
                spawnDirectedGore(level, x, y, z, TYPE10_SPRAY_COUNT, 1.0D, 4.0D);
                spawnVolume(level, random, goreCloud(127, 0, 0), x, y, z,
                        TYPE10_CLOUD_COUNT, width, height);
            }
            case 11 -> {
                spawnDirectedGore(level, x, y, z, TYPE11_SPRAY_COUNT, 1.0D, 4.0D);
                spawnVolume(level, random, goreCloud(127, 0, 0), x, y, z,
                        TYPE11_CLOUD_COUNT, width, height);
            }
            case 12 -> spawnVolume(level, random, ModParticles.RAGE.get(), x, y, z, 1, width, height);
            default -> {
            }
        }
    }

    private static void spawnVolume(ClientLevel level, RandomSource random, ParticleOptions particle,
                                    double x, double y, double z, int count, float width, float height) {
        for (int index = 0; index < count; index++) {
            double velocityX = random.nextGaussian() * 0.02D;
            double velocityY = random.nextGaussian() * 0.02D;
            double velocityZ = random.nextGaussian() * 0.02D;
            double particleX = x + random.nextFloat() * width * 2.0F - width;
            double particleY = y + 0.5D + random.nextFloat() * height;
            double particleZ = z + random.nextFloat() * width * 2.0F - width;
            level.addParticle(particle, particleX, particleY, particleZ,
                    velocityX, velocityY, velocityZ);
        }
    }

    private static void spawnDirectedGore(ClientLevel level, double x, double y, double z,
                                          int count, double horizontalFactor, double verticalFactor) {
        ParticleOptions particle = goreSplash(0);
        for (int index = 0; index < count; index++) {
            RandomSource random = RandomSource.create();
            double sampleX = (float) x + random.nextFloat();
            double sampleY = (float) y + random.nextFloat();
            double sampleZ = (float) z + random.nextFloat();
            double dx = sampleX - x;
            double dy = sampleY - y;
            double dz = sampleZ - z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 1.0E-7D) {
                continue;
            }
            dx /= distance;
            dy /= distance;
            dz /= distance;
            double strength = 0.5D / (distance / 4.0D + 0.1D);
            strength *= random.nextFloat() * random.nextFloat() + 0.3F;
            dx = Math.min(dx * strength * horizontalFactor, 0.2D)
                    * (random.nextDouble() * 2.0D - 1.0D);
            dy = Math.min(dy * strength * verticalFactor, 0.6D)
                    * (random.nextDouble() * 2.0D - 1.0D);
            dz = Math.min(dz * strength * horizontalFactor, 0.2D)
                    * (random.nextDouble() * 2.0D - 1.0D);
            level.addParticle(particle, x, y + 1.2D, z, dx, dy, dz);
        }
    }

    private static ParticleOptions goreSplash(int textureFamily) {
        return new SrpColorParticleOptions(ModParticles.GORE_SPLASH.get(),
                0, 0, 0, textureFamily);
    }

    private static ParticleOptions goreCloud(int red, int green, int blue) {
        return new SrpColorParticleOptions(ModParticles.GORE_CLOUD.get(), red, green, blue, 0);
    }
}
