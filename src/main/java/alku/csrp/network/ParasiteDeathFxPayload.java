package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.client.ParasiteDeathFxClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ParasiteDeathFxPayload(double x, double y, double z, float scale)
        implements CustomPacketPayload {
    public static final Type<ParasiteDeathFxPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "parasite_death_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ParasiteDeathFxPayload> STREAM_CODEC =
            StreamCodec.ofMember(ParasiteDeathFxPayload::encode, ParasiteDeathFxPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeFloat(scale);
    }

    private static ParasiteDeathFxPayload decode(RegistryFriendlyByteBuf buffer) {
        return new ParasiteDeathFxPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat());
    }

    public static void handle(ParasiteDeathFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ParasiteDeathFxClient.play(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
