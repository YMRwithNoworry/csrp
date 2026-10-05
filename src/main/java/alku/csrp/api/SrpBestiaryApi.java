package alku.csrp.api;

import alku.csrp.compendium.CompendiumSavedData;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

/**
 * Port of SRP 1.10.9 {@code api/SRPBestiaryApi}: the small public surface other mods and addons use to
 * read which celestial events a player has witnessed.
 *
 * <p>The original read the {@code IBestiaryProgress} capability.  26.3 has no capability system, so the
 * same data is read from the compendium progress this mod already persists per player
 * ({@link CompendiumSavedData#progress(UUID)}).</p>
 */
public final class SrpBestiaryApi {
    private SrpBestiaryApi() {
    }

    public static boolean hasSeenCelestial(Player player, String celestialId) {
        if (celestialId == null || celestialId.isEmpty()) {
            return false;
        }
        return getSeenCelestials(player).contains(celestialId);
    }

    /** @return an unmodifiable view of the witnessed celestial ids, empty when unavailable. */
    public static Set<String> getSeenCelestials(Player player) {
        if (player == null) {
            return Collections.emptySet();
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return Collections.emptySet();
        }
        return CompendiumSavedData.get(server).progress(player.getUUID()).celestials();
    }

    public static int getSeenCelestialCount(Player player) {
        return getSeenCelestials(player).size();
    }
}
