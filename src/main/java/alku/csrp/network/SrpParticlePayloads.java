package alku.csrp.network;

import alku.csrp.Csrp;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class SrpParticlePayloads {
    private SrpParticlePayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(SrpParticleBurstPayload.TYPE,
                SrpParticleBurstPayload.STREAM_CODEC, SrpParticleBurstPayload::handle);
    }
}
