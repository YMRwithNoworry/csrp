package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.client.weather.SRPBlizzardDirectionClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of the 1.10.9 {@code MsgSyncBlizzardReverse} packet: the server tells one client that the
 * blizzard wind should reverse (a Heblu/Kirin is nearby on the cold star).
 *
 * <p>1.12.2 used the SimpleImpl {@code IMessage}/{@code IMessageHandler} pair. The 26.3 form is a
 * {@link CustomPacketPayload} record with a {@link StreamCodec}, matching the other packets in
 * this package.</p>
 */
public record MsgSyncBlizzardReverse(boolean reversed) implements CustomPacketPayload {
    public static final Type<MsgSyncBlizzardReverse> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "sync_blizzard_reverse"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MsgSyncBlizzardReverse> STREAM_CODEC =
            StreamCodec.ofMember(MsgSyncBlizzardReverse::encode, MsgSyncBlizzardReverse::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(reversed);
    }

    private static MsgSyncBlizzardReverse decode(RegistryFriendlyByteBuf buffer) {
        return new MsgSyncBlizzardReverse(buffer.readBoolean());
    }

    public static void handle(MsgSyncBlizzardReverse payload, IPayloadContext context) {
        context.enqueueWork(() -> SRPBlizzardDirectionClient.setReverseRequested(payload.reversed));
    }

    public static void send(ServerPlayer player, boolean reversed) {
        PacketDistributor.sendToPlayer(player, new MsgSyncBlizzardReverse(reversed));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
