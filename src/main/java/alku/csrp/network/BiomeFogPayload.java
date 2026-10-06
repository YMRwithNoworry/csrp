package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.client.weather.ClientParasiteFog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of the 1.10.9 {@code network/SRPPacketFog}: the server pushes one player's parasite biome fog
 * (its density plus the biome's colour) to that player, where the original stored it in the
 * {@code SRPEventHandlerBus.fog / fogRed / fogGreen / fogBlue} statics.
 *
 * <p>The original sent the packet only while the density was still ramping, which is exactly when the
 * client's copy changes; {@link alku.csrp.world.ParasiteBiomeFogServer} keeps that rule.</p>
 */
public record BiomeFogPayload(float density, float red, float green, float blue)
        implements CustomPacketPayload {
    public static final Type<BiomeFogPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "biome_fog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BiomeFogPayload> STREAM_CODEC =
            StreamCodec.ofMember(BiomeFogPayload::encode, BiomeFogPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(density);
        buffer.writeFloat(red);
        buffer.writeFloat(green);
        buffer.writeFloat(blue);
    }

    private static BiomeFogPayload decode(RegistryFriendlyByteBuf buffer) {
        return new BiomeFogPayload(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat());
    }

    public static void handle(BiomeFogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientParasiteFog.setBiomeFog(payload.density, payload.red,
                payload.green, payload.blue));
    }

    /** {@code SRPMain.network.sendTo(new SRPPacketFog(fog, red, green, blue), (EntityPlayerMP) player)}. */
    public static void sendTo(ServerPlayer player, float density, float red, float green, float blue) {
        if (player.connection != null) {
            PacketDistributor.sendToPlayer(player, new BiomeFogPayload(density, red, green, blue));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
