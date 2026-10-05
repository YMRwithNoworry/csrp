package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.feature.EscapeSystem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of SRP 1.10.9 {@code network/msg/C2SRequestEscape}: the "escape" button on the death screen asks
 * the server to mark the player as escaping, so {@code EscapeRespawnHandler} teleports them away from
 * the death loop on respawn.  The original packet carried no data.
 */
public record EscapeRequestPayload() implements CustomPacketPayload {
    public static final EscapeRequestPayload INSTANCE = new EscapeRequestPayload();
    public static final Type<EscapeRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "escape_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EscapeRequestPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EscapeRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                EscapeSystem.markEscapePending(player);
            }
        });
    }
}
