package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.client.MeteorShakeClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of the 1.10.8 {@code MsgQlipShake} packet: drives the meteor screen darkening and camera
 * shake on the client.
 */
public record MeteorShakePayload(int duration, int delay, boolean dark, boolean shake, float value)
        implements CustomPacketPayload {
    public static final Type<MeteorShakePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "meteor_shake"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MeteorShakePayload> STREAM_CODEC =
            StreamCodec.ofMember(MeteorShakePayload::encode, MeteorShakePayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(duration);
        buffer.writeVarInt(delay);
        buffer.writeBoolean(dark);
        buffer.writeBoolean(shake);
        buffer.writeFloat(value);
    }

    private static MeteorShakePayload decode(RegistryFriendlyByteBuf buffer) {
        return new MeteorShakePayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readFloat());
    }

    public static void handle(MeteorShakePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MeteorShakeClient.triggerDelayed(payload.duration, payload.delay,
                payload.dark, payload.shake, payload.value));
    }

    public static void send(ServerPlayer player, int duration, int delay, boolean dark,
            boolean shake, float value) {
        PacketDistributor.sendToPlayer(player,
                new MeteorShakePayload(duration, delay, dark, shake, value));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
