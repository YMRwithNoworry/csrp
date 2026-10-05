package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.client.GuiDistortionClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of the 1.10.9 {@code SRPPacketGuiDistortionState}: tells a client whether the derived-parasite
 * GUI corruption is switched off for it, and whether that player is allowed to keep the effect while in
 * creative or spectator mode.
 */
public record GuiDistortionStatePayload(boolean disabled, boolean creativeOverride)
        implements CustomPacketPayload {
    public static final Type<GuiDistortionStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "gui_distortion_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuiDistortionStatePayload> STREAM_CODEC =
            StreamCodec.ofMember(GuiDistortionStatePayload::encode, GuiDistortionStatePayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(disabled);
        buffer.writeBoolean(creativeOverride);
    }

    private static GuiDistortionStatePayload decode(RegistryFriendlyByteBuf buffer) {
        return new GuiDistortionStatePayload(buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(GuiDistortionStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GuiDistortionClientState.set(payload.disabled, payload.creativeOverride));
    }

    public static void send(ServerPlayer player, boolean disabled, boolean creativeOverride) {
        if (player.connection == null) {
            // GameTest mock players have no connection; the server-side flags are still applied.
            return;
        }
        PacketDistributor.sendToPlayer(player, new GuiDistortionStatePayload(disabled, creativeOverride));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
