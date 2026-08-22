package alku.csrp.celestial.client;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class DarkDaysSoundEvents {
    private static DarkDaysRumbleSound current;

    private DarkDaysSoundEvents() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
        if (!CelestialClientState.isActive("dark_days") || Minecraft.getInstance().level == null) return;
        if (current == null || current.isStopped()) {
            current = new DarkDaysRumbleSound();
            Minecraft.getInstance().getSoundManager().play(current);
        }
    }
}
