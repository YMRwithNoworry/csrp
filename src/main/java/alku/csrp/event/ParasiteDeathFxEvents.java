package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.network.ParasiteDeathFxPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteDeathFxEvents {
    private ParasiteDeathFxEvents() {
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
    }
}
