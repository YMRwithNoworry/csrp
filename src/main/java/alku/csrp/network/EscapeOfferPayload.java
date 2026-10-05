package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.client.EscapeClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of SRP 1.10.9 {@code network/msg/S2CSetEscapeOffer}: tells the client whether the death screen
 * should offer the escape system.  {@code EscapeOnDeathHandler} sends {@code true} once a player dies
 * to parasites more than five times inside the one-minute window, and {@code false} when the offer is
 * cleared.
 */
public record EscapeOfferPayload(boolean offer) implements CustomPacketPayload {
    public static final Type<EscapeOfferPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "escape_offer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EscapeOfferPayload> STREAM_CODEC =
            StreamCodec.ofMember(EscapeOfferPayload::encode, EscapeOfferPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(offer);
    }

    private static EscapeOfferPayload decode(RegistryFriendlyByteBuf buffer) {
        return new EscapeOfferPayload(buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EscapeOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EscapeClientState.setOffer(payload.offer));
    }

    public static void send(ServerPlayer player, boolean offer) {
        if (player.connection == null) {
            // GameTest mock players have no connection; the flag itself is still applied server side.
            return;
        }
        PacketDistributor.sendToPlayer(player, new EscapeOfferPayload(offer));
    }
}
