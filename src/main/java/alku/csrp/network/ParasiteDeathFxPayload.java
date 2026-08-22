package alku.csrp.network;

import alku.csrp.client.ParasiteDeathFxClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ParasiteDeathFxPayload(double x, double y, double z, float scale)
        {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeFloat(scale);
    }

    public static ParasiteDeathFxPayload decode(FriendlyByteBuf buffer) {
        return new ParasiteDeathFxPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat());
    }

    public static void handle(ParasiteDeathFxPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ParasiteDeathFxClient.play(payload));
        ctx.get().setPacketHandled(true);
    }

}
