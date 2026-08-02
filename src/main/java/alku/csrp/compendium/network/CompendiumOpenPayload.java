package alku.csrp.compendium.network;

import alku.csrp.Csrp;
import alku.csrp.compendium.client.CompendiumClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CompendiumOpenPayload(CompoundTag progress) implements CustomPacketPayload {
    public static final Type<CompendiumOpenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "compendium_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompendiumOpenPayload> STREAM_CODEC =
            StreamCodec.ofMember(CompendiumOpenPayload::encode, CompendiumOpenPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeNbt(progress);
    }

    private static CompendiumOpenPayload decode(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new CompendiumOpenPayload(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(CompendiumOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CompendiumClient.open(payload.progress));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
