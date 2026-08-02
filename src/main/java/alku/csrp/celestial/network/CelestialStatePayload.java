package alku.csrp.celestial.network;

import alku.csrp.Csrp;
import alku.csrp.celestial.client.CelestialClientState;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CelestialStatePayload(Set<String> active, long nightIndex, long gameTime)
        implements CustomPacketPayload {
    public static final Type<CelestialStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "celestial_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CelestialStatePayload> STREAM_CODEC =
            StreamCodec.ofMember(CelestialStatePayload::encode, CelestialStatePayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(active.size());
        active.forEach(id -> buffer.writeUtf(id, 32));
        buffer.writeLong(nightIndex);
        buffer.writeLong(gameTime);
    }

    private static CelestialStatePayload decode(RegistryFriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), 64);
        Set<String> active = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) active.add(buffer.readUtf(32));
        return new CelestialStatePayload(Set.copyOf(active), buffer.readLong(), buffer.readLong());
    }

    public static void handle(CelestialStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CelestialClientState.update(payload.active, payload.nightIndex, payload.gameTime));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
