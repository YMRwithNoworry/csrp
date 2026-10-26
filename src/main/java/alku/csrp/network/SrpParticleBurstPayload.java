package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.client.SrpParticleEffectsClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** SRPPacketParticle 1.10.9 的坐标、范围与 type 数据包。 */
public record SrpParticleBurstPayload(double x, double y, double z, float width, float height, byte particleType)
        implements CustomPacketPayload {
    public static final Type<SrpParticleBurstPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "particle_burst"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SrpParticleBurstPayload> STREAM_CODEC =
            StreamCodec.ofMember(SrpParticleBurstPayload::encode, SrpParticleBurstPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeFloat(width);
        buffer.writeFloat(height);
        buffer.writeByte(particleType);
    }

    private static SrpParticleBurstPayload decode(RegistryFriendlyByteBuf buffer) {
        return new SrpParticleBurstPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readFloat(), buffer.readByte());
    }

    public static void handle(SrpParticleBurstPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SrpParticleEffectsClient.play(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
