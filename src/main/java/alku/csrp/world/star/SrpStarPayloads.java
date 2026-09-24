package alku.csrp.world.star;

import alku.csrp.Csrp;
import alku.csrp.network.MsgSyncBlizzardReverse;
import alku.csrp.network.MsgSyncStarType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Network registration for the R2 (blizzard / star) payloads.
 *
 * <p>The project keeps all payload registrations in {@code CompendiumPayloads}; that file is owned
 * by another workstream, so the two R2 payloads register themselves from this class instead. NeoForge
 * allows several {@code @EventBusSubscriber}s to handle
 * {@link RegisterPayloadHandlersEvent}, and {@code registrar(String)} creates an independent
 * registrar per call, so both use network version {@code "1"} like the rest of the mod.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SrpStarPayloads {
    private SrpStarPayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(MsgSyncBlizzardReverse.TYPE, MsgSyncBlizzardReverse.STREAM_CODEC,
                MsgSyncBlizzardReverse::handle);
        registrar.playToClient(MsgSyncStarType.TYPE, MsgSyncStarType.STREAM_CODEC,
                MsgSyncStarType::handle);
    }
}
