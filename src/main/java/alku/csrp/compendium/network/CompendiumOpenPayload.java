package alku.csrp.compendium.network;

import alku.csrp.compendium.client.CompendiumClient;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record CompendiumOpenPayload(CompoundTag progress) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(progress);
    }

    public static CompendiumOpenPayload decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new CompendiumOpenPayload(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(CompendiumOpenPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CompendiumClient.open(payload.progress));
        ctx.get().setPacketHandled(true);
    }

}
