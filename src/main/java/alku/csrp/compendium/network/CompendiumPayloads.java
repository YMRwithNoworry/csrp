package alku.csrp.compendium.network;

import alku.csrp.Csrp;
import alku.csrp.celestial.network.CelestialStatePayload;
import alku.csrp.overlast.network.EvolutionHudPayload;
import alku.csrp.network.ParasiteDeathFxPayload;
import alku.csrp.relay.network.RelayReportOpenPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class CompendiumPayloads {
    private CompendiumPayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(CompendiumRequestPayload.TYPE, CompendiumRequestPayload.STREAM_CODEC,
                CompendiumRequestPayload::handle);
        registrar.playToClient(CompendiumOpenPayload.TYPE, CompendiumOpenPayload.STREAM_CODEC,
                CompendiumOpenPayload::handle);
        registrar.playToClient(CompendiumUnlockPayload.TYPE, CompendiumUnlockPayload.STREAM_CODEC,
                CompendiumUnlockPayload::handle);
        registrar.playToClient(CelestialStatePayload.TYPE, CelestialStatePayload.STREAM_CODEC,
                CelestialStatePayload::handle);
        registrar.playToClient(EvolutionHudPayload.TYPE, EvolutionHudPayload.STREAM_CODEC,
                EvolutionHudPayload::handle);
        registrar.playToClient(RelayReportOpenPayload.TYPE, RelayReportOpenPayload.STREAM_CODEC,
                RelayReportOpenPayload::handle);
        registrar.playToClient(ParasiteDeathFxPayload.TYPE, ParasiteDeathFxPayload.STREAM_CODEC,
                ParasiteDeathFxPayload::handle);
    }
}
