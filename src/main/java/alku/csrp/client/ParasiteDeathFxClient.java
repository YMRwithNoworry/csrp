package alku.csrp.client;

import alku.csrp.network.ParasiteDeathFxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ParasiteDeathFxClient {
    private ParasiteDeathFxClient() {
    }

    public static void play(ParasiteDeathFxPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        float scale = Mth.clamp(payload.scale(), 0.25F, 4.0F);
        int count = Mth.clamp(Math.round(42.0F * scale), 24, 168);
        RandomSource random = level.random;
        DustParticleOptions blood = new DustParticleOptions(new Vector3f(0.61F, 0.06F, 0.09F),
                Mth.clamp(0.65F * scale, 0.35F, 2.5F));
        for (int i = 0; i < count; i++) {
            Vector3f direction = randomDirection(random);
            double speed = Mth.lerp(random.nextDouble(), 0.06D, 0.16D) * Math.sqrt(scale);
            double x = payload.x() + direction.x() * random.nextDouble() * 0.2D * scale;
            double y = payload.y() + direction.y() * random.nextDouble() * 0.2D * scale;
            double z = payload.z() + direction.z() * random.nextDouble() * 0.2D * scale;
            level.addParticle(blood, x, y, z,
                    direction.x() * speed, direction.y() * speed + 0.025D, direction.z() * speed);
            if (i % 5 == 0) {
                level.addParticle(ParticleTypes.POOF, x, y, z,
                        direction.x() * speed * 0.45D, direction.y() * speed * 0.45D,
                        direction.z() * speed * 0.45D);
            }
        }
    }

    private static Vector3f randomDirection(RandomSource random) {
        Vector3f direction = new Vector3f(
                random.nextFloat() * 2.0F - 1.0F,
                random.nextFloat() * 2.0F - 1.0F,
                random.nextFloat() * 2.0F - 1.0F);
        if (direction.lengthSquared() < 1.0E-4F) {
            direction.set(0.0F, 1.0F, 0.0F);
        }
        return direction.normalize();
    }
}
