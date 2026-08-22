package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

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
