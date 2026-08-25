package alku.csrp.network;

import alku.csrp.client.MeteorClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MeteorShakePayload(int ticks, float strength) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(ticks);
        buffer.writeFloat(strength);
    }

    public static MeteorShakePayload decode(FriendlyByteBuf buffer) {
        return new MeteorShakePayload(buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(MeteorShakePayload payload, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MeteorClientEvents.startShake(payload.ticks(), payload.strength()));
        context.get().setPacketHandled(true);
    }
}
