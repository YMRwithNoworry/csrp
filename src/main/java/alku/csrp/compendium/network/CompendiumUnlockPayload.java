package alku.csrp.compendium.network;

import alku.csrp.compendium.client.CompendiumClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record CompendiumUnlockPayload(String category) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(category, 16);
    }

    public static CompendiumUnlockPayload decode(FriendlyByteBuf buffer) {
        return new CompendiumUnlockPayload(buffer.readUtf(16));
    }

    public static void handle(CompendiumUnlockPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CompendiumClient.playUnlock(payload.category));
        ctx.get().setPacketHandled(true);
    }

}
