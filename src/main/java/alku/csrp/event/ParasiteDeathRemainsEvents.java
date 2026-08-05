package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.ParasiteRemainsEntity;
import alku.csrp.network.ParasiteDeathFxPayload;
import alku.csrp.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteDeathRemainsEvents {
    private ParasiteDeathRemainsEvents() {
    }

    @SubscribeEvent
    public static void onParasiteDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead instanceof Parasite) || !(dead.level() instanceof ServerLevel level)) {
            return;
        }

        double centerY = dead.getY() + dead.getBbHeight() * 0.45D;
        float scale = Mth.clamp((dead.getBbWidth() + dead.getBbHeight()) * 0.35F, 0.65F, 3.0F);
        ParasiteDeathFxPayload payload = new ParasiteDeathFxPayload(dead.getX(), centerY, dead.getZ(), scale);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(dead) <= 128.0D * 128.0D) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }

        int count = Mth.clamp(Math.round(4.0F + dead.getBbWidth() * dead.getBbHeight() * 1.4F), 4, 10);
        for (int i = 0; i < count; i++) {
            ParasiteRemainsEntity remains = ModEntities.PARASITE_REMAINS.get().create(level);
            if (remains == null) {
                continue;
            }
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double horizontalSpeed = 0.16D + level.random.nextDouble() * 0.34D;
            Vec3 velocity = new Vec3(Math.cos(angle) * horizontalSpeed,
                    0.18D + level.random.nextDouble() * 0.42D,
                    Math.sin(angle) * horizontalSpeed);
            remains.setPos(dead.getX() + (level.random.nextDouble() - 0.5D) * dead.getBbWidth() * 0.5D,
                    centerY + (level.random.nextDouble() - 0.5D) * dead.getBbHeight() * 0.35D,
                    dead.getZ() + (level.random.nextDouble() - 0.5D) * dead.getBbWidth() * 0.5D);
            remains.initialize(dead, level.random.nextInt(8), velocity);
            level.addFreshEntity(remains);
        }
    }
}
