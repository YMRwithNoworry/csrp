package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.client.sound.ClientPhaseMusicScheduler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of the 1.10.9 {@code network/SRPPacketUpdateEvoPhaseClient}: the answer to
 * {@link RequestEvolutionPhasePayload} — the dimension's evolution phase plus whether the player is
 * inside an infection vector (which the original used to keep the OST running).
 */
public record EvolutionPhaseClientPayload(int phase, boolean vector) implements CustomPacketPayload {
    public static final Type<EvolutionPhaseClientPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "evolution_phase_client"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EvolutionPhaseClientPayload> STREAM_CODEC =
            StreamCodec.ofMember(EvolutionPhaseClientPayload::encode, EvolutionPhaseClientPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(phase);
        buffer.writeBoolean(vector);
    }

    private static EvolutionPhaseClientPayload decode(RegistryFriendlyByteBuf buffer) {
        return new EvolutionPhaseClientPayload(buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(EvolutionPhaseClientPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPhaseMusicScheduler.applyServerState(payload.phase, payload.vector));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
