package alku.csrp.overlast.network;

import alku.csrp.Csrp;
import alku.csrp.overlast.client.EvolutionHudState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EvolutionHudPayload(int phase, int points, int currentThreshold, int nextThreshold, boolean visible)
        implements CustomPacketPayload {
    public static final Type<EvolutionHudPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "overlast_evolution_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EvolutionHudPayload> STREAM_CODEC =
            StreamCodec.ofMember(EvolutionHudPayload::encode, EvolutionHudPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(phase + 2);
        buffer.writeVarInt(points);
        buffer.writeVarInt(Math.max(0, currentThreshold));
        buffer.writeVarInt(Math.max(0, nextThreshold));
        buffer.writeBoolean(visible);
    }

    private static EvolutionHudPayload decode(RegistryFriendlyByteBuf buffer) {
        return new EvolutionHudPayload(buffer.readVarInt() - 2, buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(EvolutionHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EvolutionHudState.update(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
