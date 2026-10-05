package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.client.weather.ClientExtremeSnow;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of the 1.10.9 {@code network/ExtremeSnowNetwork.S2CExtremeSnow}: the server tells every client in
 * a dimension that the forced extreme snow was reconfigured.
 *
 * <p>The original broadcast to the players of one world from
 * {@code ExtremeSnowNetwork.broadcast(WorldServer, ...)}; that is what {@link #broadcast} does here.</p>
 */
public record ExtremeSnowStatePayload(boolean enabled, float intensity, boolean forceAnywhere,
        float windDeg, float windSpeed) implements CustomPacketPayload {
    public static final Type<ExtremeSnowStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "extreme_snow_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtremeSnowStatePayload> STREAM_CODEC =
            StreamCodec.ofMember(ExtremeSnowStatePayload::encode, ExtremeSnowStatePayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(enabled);
        buffer.writeFloat(intensity);
        buffer.writeBoolean(forceAnywhere);
        buffer.writeFloat(windDeg);
        buffer.writeFloat(windSpeed);
    }

    private static ExtremeSnowStatePayload decode(RegistryFriendlyByteBuf buffer) {
        return new ExtremeSnowStatePayload(buffer.readBoolean(), buffer.readFloat(), buffer.readBoolean(),
                buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(ExtremeSnowStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientExtremeSnow.setState(payload.enabled, payload.intensity,
                payload.forceAnywhere, payload.windDeg, payload.windSpeed));
    }

    /** {@code ExtremeSnowNetwork.broadcast}. */
    public static void broadcast(ServerLevel level, boolean enabled, float intensity, boolean forceAnywhere,
            float windDeg, float windSpeed) {
        ExtremeSnowStatePayload payload = new ExtremeSnowStatePayload(enabled, intensity, forceAnywhere,
                windDeg, windSpeed);
        for (ServerPlayer player : level.players()) {
            if (player.connection != null) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
