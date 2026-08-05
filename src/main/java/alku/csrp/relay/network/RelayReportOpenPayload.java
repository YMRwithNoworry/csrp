package alku.csrp.relay.network;

import alku.csrp.Csrp;
import alku.csrp.relay.client.RelayReportClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RelayReportOpenPayload(String reportType, CompoundTag data) implements CustomPacketPayload {
    public static final Type<RelayReportOpenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "relay_report_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RelayReportOpenPayload> STREAM_CODEC =
            StreamCodec.ofMember(RelayReportOpenPayload::encode, RelayReportOpenPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(reportType);
        buffer.writeNbt(data);
    }

    private static RelayReportOpenPayload decode(RegistryFriendlyByteBuf buffer) {
        String reportType = buffer.readUtf();
        CompoundTag tag = buffer.readNbt();
        return new RelayReportOpenPayload(reportType, tag == null ? new CompoundTag() : tag);
    }

    public static void handle(RelayReportOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RelayReportClient.open(payload.reportType, payload.data));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
