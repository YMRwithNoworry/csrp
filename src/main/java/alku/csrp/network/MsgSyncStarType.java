package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.celestial.client.StarWorldClientState;
import alku.csrp.world.SrpStarType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of the 1.10.9 {@code MsgSyncStarType} packet: the overworld star type
 * (normal / cold / warm) is pushed to a client on login, respawn and dimension change.
 *
 * <p>The client side writes into {@link StarWorldClientState}, the exact same state object the
 * pre-existing {@code StarWorldStatePayload} fills, and the server side reads
 * {@code SrpWorldData#starType()} - so both channels stay on one data source.</p>
 */
public record MsgSyncStarType(SrpStarType starType) implements CustomPacketPayload {
    public static final Type<MsgSyncStarType> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "sync_star_type"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MsgSyncStarType> STREAM_CODEC =
            StreamCodec.ofMember(MsgSyncStarType::encode, MsgSyncStarType::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(starType.value());
    }

    private static MsgSyncStarType decode(RegistryFriendlyByteBuf buffer) {
        return new MsgSyncStarType(SrpStarType.byValue(buffer.readVarInt()));
    }

    public static void handle(MsgSyncStarType payload, IPayloadContext context) {
        context.enqueueWork(() -> StarWorldClientState.update(payload.starType));
    }

    public static void send(ServerPlayer player, SrpStarType starType) {
        PacketDistributor.sendToPlayer(player, new MsgSyncStarType(starType));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
