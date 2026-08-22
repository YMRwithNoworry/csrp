package alku.csrp.celestial.network;

import alku.csrp.celestial.client.StarWorldClientState;
import alku.csrp.world.SrpStarType;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record StarWorldStatePayload(SrpStarType starType) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(starType.value());
    }

    public static StarWorldStatePayload decode(FriendlyByteBuf buffer) {
        return new StarWorldStatePayload(SrpStarType.byValue(buffer.readVarInt()));
    }

    public static void handle(StarWorldStatePayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> StarWorldClientState.update(payload.starType));
        ctx.get().setPacketHandled(true);
    }

}
