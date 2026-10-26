package alku.csrp.particle;

import alku.csrp.network.SrpParticleBurstPayload;
import alku.csrp.registry.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** SRPPacketParticle 1.10.9 的服务器发送入口。 */
public final class SrpParticleEffects {
    private SrpParticleEffects() {
    }

    /** 原件 packet type 1：一个 assimilated GSPLASH。 */
    public static void spawnType1(ServerLevel level, double x, double y, double z, float width, float height) {
        send(level, x, y, z, width, height, 1);
    }

    /** 原件 packet type 2：四个黑色 GCLOUD。 */
    public static void spawnType2(ServerLevel level, double x, double y, double z, float width, float height) {
        send(level, x, y, z, width, height, 2);
    }

    /** 原件 packet type 3：六个 RHAPPY。 */
    public static void spawnType3(ServerLevel level, double x, double y, double z, float width, float height) {
        send(level, x, y, z, width, height, 3);
    }

    /** 原件 packet type 4：二十四个绿色 GCLOUD（125, 227, 118）。 */
    public static void spawnType4(ServerLevel level, double x, double y, double z, float width, float height) {
        send(level, x, y, z, width, height, 4);
    }

    /** 原件 packet type 12：一个 RAGE。 */
    public static void spawnType12Tracking(Entity entity) {
        sendTracking(entity, 12);
    }

    /** 原件 SRPEffectBase type 2：四个黑色 GCLOUD，仅发送给追踪实体的客户端。 */
    public static void spawnType2Tracking(Entity entity) {
        sendTracking(entity, 2);
    }

    /** 原件 packet type 10：3 个体积溅射、5 个定向溅射和 5 个红色 GCLOUD。 */
    public static void spawnType10(ServerLevel level, double x, double y, double z) {
        send(level, x, y, z, 0.5F, 0.5F, 10);
    }

    /** 原件 packet type 11：33 个定向溅射和 13 个红色 GCLOUD。 */
    public static void spawnType11(ServerLevel level, double x, double y, double z) {
        send(level, x, y, z, 0.5F, 0.5F, 11);
    }

    /** 添加一个携带原件 GSPLASH 贴图族索引的粒子。 */
    public static void spawnGoreSplash(Level level, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ,
                                       int textureFamily) {
        ParticleOptions particle = new SrpColorParticleOptions(ModParticles.GORE_SPLASH.get(),
                0, 0, 0, textureFamily);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, x, y, z, 0, velocityX, velocityY, velocityZ, 1.0D);
        } else {
            level.addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    private static void send(ServerLevel level, double x, double y, double z,
                             float width, float height, int type) {
        SrpParticleBurstPayload payload = new SrpParticleBurstPayload(x, y, z, width, height, (byte) type);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static void sendTracking(Entity entity, int type) {
        if (!(entity.level() instanceof ServerLevel)) {
            return;
        }
        SrpParticleBurstPayload payload = new SrpParticleBurstPayload(entity.getX(), entity.getY(), entity.getZ(),
                entity.getBbWidth(), entity.getBbHeight(), (byte) type);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }
}
