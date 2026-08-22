package alku.csrp.overlast.network;

import alku.csrp.overlast.client.EvolutionHudState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record EvolutionHudPayload(int phase, int points, int currentThreshold, int nextThreshold, boolean visible)
        {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(phase + 2);
        buffer.writeVarInt(points);
        buffer.writeVarInt(Math.max(0, currentThreshold));
        buffer.writeVarInt(Math.max(0, nextThreshold));
        buffer.writeBoolean(visible);
    }

    public static EvolutionHudPayload decode(FriendlyByteBuf buffer) {
        return new EvolutionHudPayload(buffer.readVarInt() - 2, buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(EvolutionHudPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> EvolutionHudState.update(payload));
        ctx.get().setPacketHandled(true);
    }

}
