package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.entity.MeteorEntity;
import alku.csrp.network.CsrpNetwork;
import alku.csrp.network.MeteorShakePayload;
import alku.csrp.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Csrp.MODID)
public final class MeteorEvents {
    private static final Map<ServerLevel, Integer> CHECK_TIMERS = new WeakHashMap<>();

    private MeteorEvents() {
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (!WorldConfig.meteorsEnabled() || !WorldConfig.dimensionAllowsMeteors(level)) {
            CHECK_TIMERS.remove(level);
            return;
        }
        int interval = WorldConfig.meteorCheckInterval();
        int elapsed = CHECK_TIMERS.getOrDefault(level, 0) + 1;
        if (elapsed < interval) {
            CHECK_TIMERS.put(level, elapsed);
            return;
        }
        CHECK_TIMERS.put(level, 0);
        if (SrpWorldData.get(level).evolutionPhase() < 0
                || level.getGameTime() < WorldConfig.meteorMinimumWorldTicks()
                || level.random.nextDouble() >= WorldConfig.meteorChance()
                || WorldConfig.meteorRequiresNoVector() && !SrpWorldData.get(level).vectors().isEmpty()) {
            return;
        }

        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.isAlive()) {
                players.add(player);
            }
        }
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer selected = players.stream().filter(player -> level.canSeeSky(player.blockPosition()))
                .findFirst().orElse(players.get(0));
        int randomRadius = level.random.nextInt(Math.max(1, WorldConfig.meteorMaximumRadius()));
        spawnNear(level, selected.blockPosition(), randomRadius, WorldConfig.meteorMinimumRadius());
    }

    public static MeteorEntity spawnNear(ServerLevel level, ServerPlayer player) {
        return spawnNear(level, player.blockPosition(), WorldConfig.meteorMaximumRadius(),
                WorldConfig.meteorMinimumRadius());
    }

    public static MeteorEntity spawnNear(ServerLevel level, BlockPos center, int radius, int minimumRadius) {
        int minimum = Math.max(WorldConfig.meteorMinimumRadius(), minimumRadius);
        int maximum = Math.min(WorldConfig.meteorMaximumRadius(), radius);
        if (maximum < minimum) {
            maximum = minimum + 1;
        }
        int originX = signedDistance(level, minimum, maximum);
        int originZ = signedDistance(level, minimum, maximum);
        int targetX = signedDistance(level, minimum, maximum);
        int targetZ = signedDistance(level, minimum, maximum);
        Vec3 start = new Vec3(center.getX() + originX, level.getMaxBuildHeight(), center.getZ() + originZ);
        Vec3 target = new Vec3(center.getX() + targetX, center.getY(), center.getZ() + targetZ);
        return spawn(level, start, target);
    }

    public static MeteorEntity spawn(ServerLevel level, Vec3 start, Vec3 target) {
        MeteorEntity meteor = ModEntities.METEOR.get().create(level);
        if (meteor == null) {
            return null;
        }
        meteor.moveTo(start.x, start.y, start.z, 0.0F, 0.0F);
        meteor.configure(target.subtract(start), true);
        level.addFreshEntity(meteor);
        for (ServerPlayer player : level.players()) {
            CsrpNetwork.sendToPlayer(player, new MeteorShakePayload(0, 0.0F, true));
        }
        return meteor;
    }

    private static int signedDistance(ServerLevel level, int minimum, int maximum) {
        int value = minimum + level.random.nextInt(maximum - minimum + 1);
        return level.random.nextBoolean() ? value : -value;
    }
}
