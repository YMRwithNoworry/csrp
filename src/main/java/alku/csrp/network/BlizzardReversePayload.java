package alku.csrp.network;

import alku.csrp.client.weather.BlizzardDirectionClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

/**
 * S2C: the server tells the client whether the cold-star blizzard wind should be reversed.
 *
 * <p>1.10.9 counterpart: {@code MsgSyncBlizzardReverse} carrying a single boolean, handled by
 * {@code SRPBlizzardDirectionClient.setReverseRequested}. The trigger lives in
 * {@code alku.csrp.world.SrpBlizzardDerivedHandler}.
 *
 * <p><b>Client-class isolation on a dedicated server.</b> The receiver is
 * {@code alku.csrp.client.weather.BlizzardDirectionClient}, a client-only class that touches
 * {@code Minecraft.getInstance()} and the sound manager. Two things keep a dedicated server from ever
 * loading it:
 * <ol>
 *   <li>the only reference sits <em>inside</em> the {@code enqueueWork} lambda, so it is resolved
 *       lazily on first invocation instead of being a class that this payload must resolve during
 *       verification (the same shape the project already uses in {@code MeteorShakePayload} and
 *       {@code ParasiteDeathFxPayload});</li>
 *   <li>the lambda itself is guarded by {@link FMLEnvironment#dist}, so on a dedicated server the
 *       {@code setReverseRequested} call site is unreachable and the client class is never linked.
 *       This message is registered as {@code PLAY_TO_CLIENT}, so no server-side handler path exists
 *       anyway — the guard is belt-and-braces for the case where a client-side packet arrives while
 *       the class loader is in an odd state.</li>
 * </ol>
 */
public record BlizzardReversePayload(boolean reversed) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(reversed);
    }

    public static BlizzardReversePayload decode(FriendlyByteBuf buffer) {
        return new BlizzardReversePayload(buffer.readBoolean());
    }

    public static void handle(BlizzardReversePayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                BlizzardDirectionClient.setReverseRequested(payload.reversed());
            }
        });
        ctx.get().setPacketHandled(true);
    }

}
