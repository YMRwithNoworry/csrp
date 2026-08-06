package alku.csrp.celestial.client;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class DarkDaysSoundEvents {
    private static DarkDaysRumbleSound current;

    private DarkDaysSoundEvents() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!CelestialClientState.isActive("dark_days") || Minecraft.getInstance().level == null) return;
        if (current == null || current.isStopped()) {
            current = new DarkDaysRumbleSound();
            Minecraft.getInstance().getSoundManager().play(current);
        }
    }
}
