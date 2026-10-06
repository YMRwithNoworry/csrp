package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Port of the 1.10.9 {@code network/SRPPacketRequestEvoPhaseClient}: the client asks the server for
 * its dimension's evolution phase and whether it currently sits inside an infection vector, every 20
 * ticks while the OST is enabled.
 *
 * <p>The original answered with {@code SRPPacketUpdateEvoPhaseClient}; that reply is
 * {@link EvolutionPhaseClientPayload} here.</p>
 */
public record RequestEvolutionPhasePayload() implements CustomPacketPayload {
    public static final Type<RequestEvolutionPhasePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "request_evolution_phase"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestEvolutionPhasePayload> STREAM_CODEC =
            StreamCodec.unit(new RequestEvolutionPhasePayload());

    public static void handle(RequestEvolutionPhasePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = (ServerLevel) player.level();
            SrpWorldData data = SrpWorldData.get(level);
            boolean inVector = inVectorRange(data, player.blockPosition());
            PacketDistributor.sendToPlayer(player,
                    new EvolutionPhaseClientPayload(data.evolutionPhase(), inVector));
        });
    }

    /** {@code SRPWorldData#nearestInfectionPosition(false, pos) != null}. */
    private static boolean inVectorRange(SrpWorldData data, BlockPos pos) {
        for (SrpWorldData.VectorEntry vector : data.vectors()) {
            double radius = vector.radius();
            if (vector.pos().distSqr(pos) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
