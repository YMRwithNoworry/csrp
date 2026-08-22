package alku.csrp.compendium.network;

import alku.csrp.Csrp;
import alku.csrp.network.CsrpNetwork;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = Csrp.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class CompendiumPayloads {
    private CompendiumPayloads() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CsrpNetwork::register);
    }
}
