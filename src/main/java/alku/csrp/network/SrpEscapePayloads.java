package alku.csrp.network;

import alku.csrp.Csrp;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Network registration for the escape-system payloads ({@code EscapeOfferPayload} /
 * {@code EscapeRequestPayload}).
 *
 * <p>Mirrors {@code alku.csrp.world.star.SrpStarPayloads}: NeoForge lets several
 * {@code @EventBusSubscriber}s handle {@link RegisterPayloadHandlersEvent}, and
 * {@code registrar(String)} creates an independent registrar per call, so the escape payloads use the
 * mod-wide network version {@code "1"} without touching another workstream's registration class.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SrpEscapePayloads {
    private SrpEscapePayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(EscapeOfferPayload.TYPE, EscapeOfferPayload.STREAM_CODEC,
                EscapeOfferPayload::handle);
        registrar.playToServer(EscapeRequestPayload.TYPE, EscapeRequestPayload.STREAM_CODEC,
                EscapeRequestPayload::handle);
    }
}
