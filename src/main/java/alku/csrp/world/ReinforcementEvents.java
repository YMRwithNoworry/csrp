package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class ReinforcementEvents {
    private ReinforcementEvents() {
    }

    @SubscribeEvent
    public static void onParasiteDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Parasite && event.getEntity().level() instanceof ServerLevel level) {
            ReinforcementSystem.tryFromParasiteDeath(level, event.getEntity().blockPosition(),
                    event.getEntity().getBbWidth(), event.getEntity().getBbHeight(), level.random);
        }
    }
}
