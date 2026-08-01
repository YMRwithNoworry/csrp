package alku.csrp.effect;

import alku.csrp.Csrp;
import alku.csrp.entity.DeterrentParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/** Calls tier-appropriate parasite reinforcements through an emerging Worm. */
public final class SpottedMobEffect extends MobEffect {
    private static final int VECTOR_HEALTH_REQUIRED = 4500;
    private static final int PARASITE_CAP = 40;
    private static final Set<String> PURE_IDS = Set.of(
            "overseer", "vigilante", "warden", "bomber_light", "marauder", "monarch", "grunt");
    private static final List<ResourceLocation> INFECTED_REINFORCEMENTS = ids(
            "sim_bigspider", "sim_human", "sim_cow", "sim_sheep", "sim_wolf",
            "sim_pig", "sim_villager", "sim_horse", "sim_bear", "sim_enderman");
    private static final List<ResourceLocation> PRIMITIVE_REINFORCEMENTS = ids(
            "sim_bigspider", "sim_human", "sim_cow", "sim_sheep", "sim_wolf",
            "sim_pig", "sim_villager", "sim_horse", "sim_bear", "sim_enderman",
            "pri_longarms", "pri_manducater", "pri_reeker", "pri_yelloweye", "pri_summoner",
            "pri_bolster", "pri_arachnida", "pri_vermin", "pri_viscera");
    private static final List<ResourceLocation> ADAPTED_REINFORCEMENTS = ids(
            "pri_longarms", "pri_manducater", "pri_reeker", "pri_yelloweye", "pri_summoner",
            "pri_bolster", "pri_arachnida", "pri_vermin", "pri_viscera",
            "ada_longarms", "ada_manducater", "ada_reeker", "ada_yelloweye", "ada_summoner",
            "ada_bolster", "ada_arachnida", "ada_vermin", "ada_viscera");
    private static final List<ResourceLocation> PURE_REINFORCEMENTS = ids(
            "ada_longarms", "ada_manducater", "ada_reeker", "ada_yelloweye", "ada_summoner",
            "ada_bolster", "ada_arachnida", "ada_vermin", "ada_viscera",
            "overseer", "vigilante", "warden", "bomber_light", "marauder", "monarch", "grunt");

    public SpottedMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 8149607);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level) || entity.tickCount % 200 != 0) {
            return true;
        }
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return true;
        }
        if (entity instanceof Parasite || !hasStrongVector(level, entity.blockPosition())) {
            entity.removeEffect(ModMobEffects.SPOTTED);
            return true;
        }

        Mob nearbyParasite = findNearbyParasite(level, entity);
        if (nearbyParasite == null || countParasites(level) >= PARASITE_CAP) {
            return true;
        }
        Reinforcement reinforcement = reinforcementFor(nearbyParasite);
        if (reinforcement == null) {
            return true;
        }
        spawnWorm(level, entity, reinforcement);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    private static boolean hasStrongVector(ServerLevel level, BlockPos targetPos) {
        return SrpWorldData.get(level).vectors().stream().anyMatch(entry -> entry.health() >= VECTOR_HEALTH_REQUIRED
                && entry.pos().distSqr(targetPos) <= (double) entry.radius() * entry.radius());
    }

    private static Mob findNearbyParasite(ServerLevel level, LivingEntity target) {
        AABB search = new AABB(target.blockPosition()).inflate(32.0D, 16.0D, 32.0D);
        List<Mob> parasites = level.getEntitiesOfClass(Mob.class, search, mob -> mob instanceof Parasite);
        return parasites.isEmpty() ? null : parasites.getFirst();
    }

    private static int countParasites(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Parasite) {
                count++;
            }
        }
        return count;
    }

    private static Reinforcement reinforcementFor(Mob parasite) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(parasite.getType());
        if (!id.getNamespace().equals(Csrp.MODID)) {
            return null;
        }
        String path = id.getPath();
        if (path.startsWith("sim_")) {
            return new Reinforcement(INFECTED_REINFORCEMENTS, 2, 4);
        }
        if (path.startsWith("pri_")) {
            return new Reinforcement(PRIMITIVE_REINFORCEMENTS, 2, 3);
        }
        if (path.startsWith("ada_")) {
            return new Reinforcement(ADAPTED_REINFORCEMENTS, 1, 3);
        }
        if (PURE_IDS.contains(path)) {
            return new Reinforcement(PURE_REINFORCEMENTS, 1, 2);
        }
        return null;
    }

    private static void spawnWorm(ServerLevel level, LivingEntity target, Reinforcement reinforcement) {
        double theta = level.random.nextDouble() * Math.PI * 2.0D;
        double radius = Math.sqrt(level.random.nextDouble()) * 5.0D;
        BlockPos candidate = BlockPos.containing(
                target.getX() + Math.cos(theta) * radius,
                target.getY(),
                target.getZ() + Math.sin(theta) * radius);
        BlockPos floor = findFloor(level, candidate, 10);
        if (floor == null) {
            return;
        }

        DeterrentParasiteEntity worm = ModEntities.WORM.get().create(level);
        if (worm == null) {
            return;
        }
        worm.moveTo(floor.getX() + 0.5D, floor.getY(), floor.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(worm, worm.getBoundingBox().inflate(1.0D, 7.0D, 1.0D))) {
            return;
        }
        worm.finalizeSpawn(level, level.getCurrentDifficultyAt(floor), MobSpawnType.MOB_SUMMONED, null);
        worm.setTarget(target);
        worm.setWormPayload(reinforcement.minimum(), reinforcement.maximum());
        worm.setWormPayloadTypes(reinforcement.types());
        level.addFreshEntity(worm);
    }

    private static BlockPos findFloor(ServerLevel level, BlockPos start, int attempts) {
        BlockPos cursor = start;
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (level.getBlockState(cursor).isAir()) {
                if (!level.getBlockState(cursor.below()).isAir()) {
                    return cursor;
                }
                cursor = cursor.below();
            } else {
                cursor = cursor.above();
            }
        }
        return null;
    }

    private static List<ResourceLocation> ids(String... paths) {
        return java.util.Arrays.stream(paths)
                .map(path -> ResourceLocation.fromNamespaceAndPath(Csrp.MODID, path))
                .toList();
    }

    private record Reinforcement(List<ResourceLocation> types, int minimum, int maximum) {
    }
}
