package alku.csrp.celestial.network;

import alku.csrp.Csrp;
import alku.csrp.celestial.client.StarWorldClientState;
import alku.csrp.world.SrpStarType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StarWorldStatePayload(SrpStarType starType) implements CustomPacketPayload {
    public static final Type<StarWorldStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "star_world_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StarWorldStatePayload> STREAM_CODEC =
            StreamCodec.ofMember(StarWorldStatePayload::encode, StarWorldStatePayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(starType.value());
    }

    private static StarWorldStatePayload decode(RegistryFriendlyByteBuf buffer) {
        return new StarWorldStatePayload(SrpStarType.byValue(buffer.readVarInt()));
    }

    public static void handle(StarWorldStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> StarWorldClientState.update(payload.starType));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
