package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.client.sound.ClientNodeHeart;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of the 1.10.9 {@code network/SRPPacketMovingSound} with {@code evPhase = -1}: the server tells
 * one player's client to play the core block node heart sound at a given volume.
 *
 * <p>The original carried an {@code evPhase} switch because the same packet also drove the evolution
 * phase jingles ({@code 1..6}); those are handled by the port's own music system, so this payload only
 * carries the heart branch's volume.</p>
 */
public record NodeHeartPayload(float volume) implements CustomPacketPayload {
    public static final Type<NodeHeartPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "node_heart"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NodeHeartPayload> STREAM_CODEC =
            StreamCodec.ofMember(NodeHeartPayload::encode, NodeHeartPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(volume);
    }

    private static NodeHeartPayload decode(RegistryFriendlyByteBuf buffer) {
        return new NodeHeartPayload(buffer.readFloat());
    }

    public static void handle(NodeHeartPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientNodeHeart.play(payload.volume));
    }

    /** {@code SRPMain.network.sendTo(new SRPPacketMovingSound(-1, vol), (EntityPlayerMP) player)}. */
    public static void sendTo(ServerPlayer player, float volume) {
        if (player.connection != null) {
            PacketDistributor.sendToPlayer(player, new NodeHeartPayload(volume));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
