package alku.csrp.infection;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server-wide SRP 1.10.8 counters for Beckon and parasite-biome block conversion. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class InfestationSpreadLimiter {
    private static final Map<MinecraftServer, Counters> COUNTERS = new WeakHashMap<>();

    private InfestationSpreadLimiter() {
    }

    public static boolean canSpread(ServerLevel level, Type type) {
        Counters counters = counters(level.getServer());
        return type == Type.BECKON
                ? counters.beckonBlocks <= WorldConfig.beckonInfestationBlockLimit()
                : counters.biomeBlocks <= WorldConfig.biomeInfestationBlockLimit();
    }

    public static void record(ServerLevel level, Type type, int convertedBlocks) {
        if (convertedBlocks <= 0) {
            return;
        }
        Counters counters = counters(level.getServer());
        if (type == Type.BECKON) {
            counters.beckonBlocks += convertedBlocks;
        } else {
            counters.biomeBlocks += convertedBlocks;
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        Counters counters = COUNTERS.get(event.getServer());
        if (counters == null) {
            return;
        }
        if (counters.beckonBlocks > WorldConfig.beckonInfestationBlockLimit()
                && ++counters.beckonCooldown > WorldConfig.beckonInfestationCooldown()) {
            counters.beckonBlocks = 0;
            counters.beckonCooldown = 0;
        }
        if (counters.biomeBlocks > WorldConfig.biomeInfestationBlockLimit()
                && ++counters.biomeCooldown > WorldConfig.biomeInfestationCooldown()) {
            counters.biomeBlocks = 0;
            counters.biomeCooldown = 0;
        }
    }

    private static Counters counters(MinecraftServer server) {
        return COUNTERS.computeIfAbsent(server, ignored -> new Counters());
    }

    public enum Type {
        BECKON,
        BIOME
    }

    private static final class Counters {
        private int beckonBlocks;
        private int biomeBlocks;
        private int beckonCooldown;
        private int biomeCooldown;
    }
}
