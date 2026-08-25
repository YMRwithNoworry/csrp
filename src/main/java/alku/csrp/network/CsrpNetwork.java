package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.celestial.network.CelestialStatePayload;
import alku.csrp.celestial.network.StarWorldStatePayload;
import alku.csrp.compendium.network.CompendiumOpenPayload;
import alku.csrp.compendium.network.CompendiumRequestPayload;
import alku.csrp.compendium.network.CompendiumUnlockPayload;
import alku.csrp.overlast.network.EvolutionHudPayload;
import alku.csrp.relay.network.RelayReportOpenPayload;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** SimpleChannel-based network layer for the 1.20.1 Forge port. */
public final class CsrpNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Csrp.MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private CsrpNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CompendiumRequestPayload.class, CompendiumRequestPayload::encode,
                CompendiumRequestPayload::decode, CompendiumRequestPayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, CompendiumOpenPayload.class, CompendiumOpenPayload::encode,
                CompendiumOpenPayload::decode, CompendiumOpenPayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, CompendiumUnlockPayload.class, CompendiumUnlockPayload::encode,
                CompendiumUnlockPayload::decode, CompendiumUnlockPayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, CelestialStatePayload.class, CelestialStatePayload::encode,
                CelestialStatePayload::decode, CelestialStatePayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, StarWorldStatePayload.class, StarWorldStatePayload::encode,
                StarWorldStatePayload::decode, StarWorldStatePayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, EvolutionHudPayload.class, EvolutionHudPayload::encode,
                EvolutionHudPayload::decode, EvolutionHudPayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, RelayReportOpenPayload.class, RelayReportOpenPayload::encode,
                RelayReportOpenPayload::decode, RelayReportOpenPayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, ParasiteDeathFxPayload.class, ParasiteDeathFxPayload::encode,
                ParasiteDeathFxPayload::decode, ParasiteDeathFxPayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, MeteorShakePayload.class, MeteorShakePayload::encode,
                MeteorShakePayload::decode, MeteorShakePayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
