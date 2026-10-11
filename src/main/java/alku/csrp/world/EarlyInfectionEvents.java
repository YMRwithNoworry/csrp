package alku.csrp.world;

import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.entity.BuglinEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * A small, warned early-game encounter which uses the existing Buglin entity.
 * The warning and cooldown live in SrpWorldData so a restart cannot duplicate it.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class EarlyInfectionEvents {
    private static final long CHECK_INTERVAL_TICKS = 200L;
    private static final int WARNING_TICKS = 80;

    private EarlyInfectionEvents() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START || !(event.level instanceof ServerLevel level)) {
            return;
        }

        SrpWorldData data = SrpWorldData.get(level);
        if (data.earlyEventWarningReady(level)) {
            spawnScouts(level, data);
            return;
        }
        if (!Config.earlyEventEnabled() || level.getGameTime() % CHECK_INTERVAL_TICKS != 0L
                || !data.earlyEventCooldownReady(level)) {
            return;
        }
        trySchedule(level, data);
    }

    private static void trySchedule(ServerLevel level, SrpWorldData data) {
        int phase = data.evolutionPhase();
        if (phase < 0 || phase > 1 || level.getGameTime() < Config.earlyEventMinimumWorldTicks()
                || level.players().isEmpty() || level.random.nextDouble() >= Config.earlyEventChance()) {
            return;
        }

        int start = level.random.nextInt(level.players().size());
        int minimumDistance = Config.earlyEventMinimumDistance();
        int maximumDistance = Config.earlyEventMaximumDistance();
        for (int offset = 0; offset < level.players().size(); offset++) {
            ServerPlayer player = level.players().get((start + offset) % level.players().size());
            if (player.isSpectator() || nearbyParasites(level, player.blockPosition(), maximumDistance)
                    >= Config.earlyEventLocalCap()) {
                continue;
            }
            BlockPos warning = findWarningPosition(level, player, minimumDistance, maximumDistance);
            if (warning == null) {
                continue;
            }
            data.scheduleEarlyEvent(level, warning, WARNING_TICKS, Config.earlyEventCooldownSeconds());
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, warning.getX() + 0.5D, warning.getY() + 0.5D,
                    warning.getZ() + 0.5D, 8, 0.35D, 0.25D, 0.35D, 0.01D);
            level.playSound(null, warning, ModSounds.BUGLIN_EMERGE.get(), SoundSource.HOSTILE, 0.65F, 0.8F);
            player.displayClientMessage(Component.translatable("message.csrp.early_infection_warning"), false);
            return;
        }
    }

    private static BlockPos findWarningPosition(ServerLevel level, ServerPlayer player,
                                                 int minimumDistance, int maximumDistance) {
        int distanceRange = Math.max(1, maximumDistance - minimumDistance + 1);
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            int distance = minimumDistance + level.random.nextInt(distanceRange);
            int x = Mth.floor(player.getX() + Mth.cos((float) angle) * distance);
            int z = Mth.floor(player.getZ() + Mth.sin((float) angle) * distance);
            BlockPos column = new BlockPos(x, player.blockPosition().getY(), z);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos position = new BlockPos(x, y, z);
            if (!level.hasChunkAt(column) || y <= level.getMinBuildHeight()
                    || !level.getBlockState(position).isAir()
                    || !level.getFluidState(position).isEmpty()) {
                continue;
            }
            return position;
        }
        return null;
    }

    private static void spawnScouts(ServerLevel level, SrpWorldData data) {
        BlockPos origin = data.earlyEventPosition();
        data.clearEarlyEventWarning();
        if (level.players().stream().noneMatch(player -> player.distanceToSqr(origin.getX() + 0.5D,
                origin.getY() + 0.5D, origin.getZ() + 0.5D) <= 3600.0D)) {
            return;
        }

        int spawned = 0;
        for (int index = 0; index < Config.earlyEventMaxScouts(); index++) {
            if (nearbyParasites(level, origin, Config.earlyEventMaximumDistance())
                    >= Config.earlyEventLocalCap()) {
                break;
            }
            BlockPos spawnPosition = findScoutPosition(level, origin, level.random);
            if (spawnPosition == null) {
                continue;
            }
            BuglinEntity buglin = ModEntities.BUGLIN.get().create(level);
            if (buglin == null) {
                continue;
            }
            buglin.moveTo(spawnPosition.getX() + 0.5D, spawnPosition.getY(), spawnPosition.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            buglin.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPosition), MobSpawnType.MOB_SUMMONED,
                    null, null);
            buglin.startBuriedEmergence();
            if (level.noCollision(buglin) && level.addFreshEntity(buglin)) {
                spawned++;
            } else {
                buglin.discard();
            }
        }
        if (spawned > 0) {
            level.sendParticles(ParticleTypes.POOF, origin.getX() + 0.5D, origin.getY() + 0.5D,
                    origin.getZ() + 0.5D, spawned * 3, 0.5D, 0.3D, 0.5D, 0.02D);
            level.playSound(null, origin, ModSounds.BUGLIN_EMERGE.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
        }
    }

    private static BlockPos findScoutPosition(ServerLevel level, BlockPos origin, net.minecraft.util.RandomSource random) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = origin.getX() + random.nextInt(7) - 3;
            int z = origin.getZ() + random.nextInt(7) - 3;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos position = new BlockPos(x, y, z);
            if (level.getBlockState(position).isAir() && level.getFluidState(position).isEmpty()) {
                return position;
            }
        }
        return null;
    }

    private static int nearbyParasites(ServerLevel level, BlockPos center, int radius) {
        AABB area = new AABB(center).inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity instanceof Parasite).size();
    }
}
