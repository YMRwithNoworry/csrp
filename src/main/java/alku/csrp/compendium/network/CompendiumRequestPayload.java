package alku.csrp.compendium.network;

import alku.csrp.Csrp;
import alku.csrp.compendium.CompendiumSavedData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CompendiumRequestPayload() implements CustomPacketPayload {
    public static final Type<CompendiumRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "compendium_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompendiumRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new CompendiumRequestPayload());

    public static void handle(CompendiumRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                var progress = CompendiumSavedData.get(player.getServer()).progress(player.getUUID());
                context.reply(new CompendiumOpenPayload(progress.save()));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
