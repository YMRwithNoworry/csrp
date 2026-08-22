package alku.csrp.compendium.network;

import alku.csrp.compendium.CompendiumSavedData;
import alku.csrp.network.CsrpNetwork;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record CompendiumRequestPayload() {
    public void encode(FriendlyByteBuf buffer) {
    }

    public static CompendiumRequestPayload decode(FriendlyByteBuf buffer) {
        return new CompendiumRequestPayload();
    }

    public static void handle(CompendiumRequestPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getSender() instanceof ServerPlayer player) {
                var progress = CompendiumSavedData.get(player.getServer()).progress(player.getUUID());
                CsrpNetwork.sendToPlayer(player, new CompendiumOpenPayload(progress.save()));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
