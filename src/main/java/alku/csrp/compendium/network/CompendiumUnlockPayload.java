package alku.csrp.compendium.network;

import alku.csrp.Csrp;
import alku.csrp.compendium.client.CompendiumClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CompendiumUnlockPayload(String category) implements CustomPacketPayload {
    public static final Type<CompendiumUnlockPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "compendium_unlock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompendiumUnlockPayload> STREAM_CODEC =
            StreamCodec.ofMember(CompendiumUnlockPayload::encode, CompendiumUnlockPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(category, 16);
    }

    private static CompendiumUnlockPayload decode(RegistryFriendlyByteBuf buffer) {
        return new CompendiumUnlockPayload(buffer.readUtf(16));
    }

    public static void handle(CompendiumUnlockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CompendiumClient.playUnlock(payload.category));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
