package alku.csrp.celestial.network;

import alku.csrp.celestial.client.CelestialClientState;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record CelestialStatePayload(Set<String> active, long nightIndex, long gameTime)
        {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(active.size());
        active.forEach(id -> buffer.writeUtf(id, 32));
        buffer.writeLong(nightIndex);
        buffer.writeLong(gameTime);
    }

    public static CelestialStatePayload decode(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), 64);
        Set<String> active = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) active.add(buffer.readUtf(32));
        return new CelestialStatePayload(Set.copyOf(active), buffer.readLong(), buffer.readLong());
    }

    public static void handle(CelestialStatePayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CelestialClientState.update(payload.active, payload.nightIndex, payload.gameTime));
        ctx.get().setPacketHandled(true);
    }

}
