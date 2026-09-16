package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.entity.MeteorEntity;
import alku.csrp.network.MeteorShakePayload;
import alku.csrp.registry.ModEntities;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Port of the meteor infection section of SRParasites 1.10.8 {@code SRPEventHandlerBus#worldTick}.
 * Every configured interval the dimension rolls a chance to launch a Hive Satellite meteor toward
 * a player, provided the dimension has no EIV when {@code meteorVectorless} is enabled.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class MeteorInfectionSystem {
    private static final Map<ResourceKey<Level>, Integer> COUNTERS = new HashMap<>();
    private static final int VECTOR_HEALTH = 350;
    private static final int VECTOR_RADIUS = 200;

    private MeteorInfectionSystem() {
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        MeteorImpactUtil.tickPendingStructures(level);
        ResourceKey<Level> dimension = level.dimension();
        if (Config.meteorDimensionBlacklist().contains(dimension.identifier().toString())) {
            return;
        }
        // The create-world "Meteor Infection" toggle wins; worlds created before the option
        // existed keep following the global config value.
        if (!SrpWorldData.get(level).meteorInfectionEnabled()) {
            return;
        }

        int counter = COUNTERS.getOrDefault(dimension, 0) + 1;
        if (counter <= Config.meteorCheckTicks()) {
            COUNTERS.put(dimension, counter);
            return;
        }
        COUNTERS.put(dimension, 0);

        if (level.getRandom().nextDouble() >= Config.meteorChance()) {
            return;
        }
        if (level.getGameTime() < Config.meteorStartTicks()) {
            return;
        }
        SrpWorldData data = SrpWorldData.get(level);
        if (data.evolutionPhase() < 0) {
            return;
        }
        if (Config.meteorVectorless() && !data.vectors().isEmpty()) {
            return;
        }
        spawnMeteor(level);
    }

    /** Original {@code SRPEventHandlerBus#spawningMet}: prefer a player standing under open sky. */
    public static boolean spawnMeteor(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return false;
        }
        ServerPlayer chosen = null;
        for (ServerPlayer player : players) {
            if (level.canSeeSky(player.blockPosition())) {
                chosen = player;
                break;
            }
        }
        if (chosen == null) {
            chosen = players.get(0);
        }
        return spawnMeteorAround(level, chosen.blockPosition());
    }

    /** Original {@code ParasiteSummon.spawnMeteor(BlockPos, rad, minRad, World)}. */
    public static boolean spawnMeteorAround(ServerLevel level, BlockPos center) {
        int rad = level.getRandom().nextInt(Math.max(2, Config.meteorRadius()));
        int minRad = Config.meteorMinimumRadius();
        if (rad > Config.meteorRadius()) {
            rad = Config.meteorRadius();
        }
        if (minRad < Config.meteorMinimumRadius()) {
            minRad = Config.meteorMinimumRadius();
        }
        if (rad < minRad) {
            rad = minRad + 1;
        }
        int span = Math.max(1, rad - minRad + 1);
        RandomSource random = level.getRandom();
        int originX = signedOffset(random, minRad, span);
        int originY = level.getMaxY() + 1;
        int originZ = signedOffset(random, minRad, span);
        int targetX = signedOffset(random, minRad, span);
        int targetZ = signedOffset(random, minRad, span);

        Vec3 origin = new Vec3(center.getX() + originX, originY, center.getZ() + originZ);
        Vec3 target = new Vec3(center.getX() + targetX, center.getY(), center.getZ() + targetZ);
        return spawnMeteor(level, origin, target);
    }

    /** Direct launch helper used by the admin command. */
    public static boolean spawnMeteor(ServerLevel level, Vec3 origin, Vec3 target) {
        Vec3 direction = target.subtract(origin);
        if (direction.lengthSqr() < 1.0E-6D) {
            return false;
        }
        MeteorEntity meteor = ModEntities.HIVE_SATELLITE.get().create(level, EntitySpawnReason.EVENT);
        if (meteor == null) {
            return false;
        }
        meteor.configureLaunch(origin, direction);
        level.addFreshEntity(meteor);
        for (ServerPlayer player : level.players()) {
            MeteorShakePayload.send(player, 0, 0, true, false, 0.0F);
        }
        return true;
    }

    public static void createVectorAt(ServerLevel level, BlockPos pos) {
        SrpCoreSystems.placeVector(level, pos, VECTOR_HEALTH, VECTOR_RADIUS);
    }

    private static int signedOffset(RandomSource random, int min, int span) {
        int value = random.nextInt(span) + min;
        return random.nextBoolean() ? value : -value;
    }
}
