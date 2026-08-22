package alku.csrp.relay.network;

import alku.csrp.relay.client.RelayReportClient;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record RelayReportOpenPayload(String reportType, CompoundTag data) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(reportType);
        buffer.writeNbt(data);
    }

    public static RelayReportOpenPayload decode(FriendlyByteBuf buffer) {
        String reportType = buffer.readUtf();
        CompoundTag tag = buffer.readNbt();
        return new RelayReportOpenPayload(reportType, tag == null ? new CompoundTag() : tag);
    }

    public static void handle(RelayReportOpenPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> RelayReportClient.open(payload.reportType, payload.data));
        ctx.get().setPacketHandled(true);
    }

}
