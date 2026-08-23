package alku.csrp.world;

import alku.csrp.Csrp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.List;
import java.util.stream.Stream;

/** Original SRP phase and ubiquitous-development natural spawn tables. */
public final class NaturalSpawnTables {
    // Legacy SpawnPlacementTypes.IN_AIR is represented by Forge 1.20.1 NO_RESTRICTIONS;
    // CommonModEvents still enforces the three-block air column predicate.
    private static final double UBIQUITOUS_TABLE_CHANCE = 0.5D;

    private static final List<MobSpawnSettings.SpawnerData> PHASE_MINUS_ONE = List.of(
            spawn("rupter", 3, 6, 30),
            spawn("sim_squid", 1, 2, 15),
            spawn("sim_bigspider", 3, 5, 25),
            spawn("sim_human", 3, 5, 25),
            spawn("sim_cow", 3, 5, 25),
            spawn("sim_sheep", 3, 5, 25),
            spawn("sim_wolf", 3, 5, 25),
            spawn("sim_pig", 3, 5, 25),
            spawn("sim_villager", 3, 5, 25),
            spawn("sim_adventurer", 3, 5, 25),
            spawn("sim_horse", 3, 5, 25),
            spawn("sim_bear", 3, 5, 25),
            spawn("sim_enderman", 1, 1, 1),
            spawn("host", 1, 2, 5),
            spawn("heed", 1, 2, 5),
            spawn("pri_devourer", 1, 2, 1),
            spawn("pri_longarms", 2, 3, 5),
            spawn("pri_manducater", 2, 3, 5),
            spawn("pri_reeker", 2, 3, 5),
            spawn("pri_yelloweye", 2, 3, 5),
            spawn("pri_summoner", 2, 3, 5),
            spawn("pri_bolster", 2, 3, 5),
            spawn("pri_arachnida", 2, 3, 5),
            spawn("thrall", 3, 5, 25));

    private static final List<MobSpawnSettings.SpawnerData> PHASE_ZERO = List.of(
            spawn("buglin", 2, 6, 30));

    private static final List<MobSpawnSettings.SpawnerData> PHASE_ONE = List.of(
            spawn("buglin", 2, 6, 30),
            spawn("rupter", 3, 6, 30),
            spawn("carrier_light", 1, 1, 1),
            spawn("worker", 1, 1, 5),
            spawn("architect", 1, 1, 5),
            spawn("bomber_heavy", 1, 1, 1),
            spawn("wraith", 1, 1, 1),
            spawn("bogle", 1, 1, 1),
            spawn("haunter", 1, 1, 1),
            spawn("carrier_colony", 1, 1, 1),
            spawn("kirin", 1, 1, 1),
            spawn("draconite", 1, 1, 1));

    private static final List<MobSpawnSettings.SpawnerData> PHASE_TWO = List.of(
            spawn("buglin", 2, 6, 30),
            spawn("rupter", 3, 6, 30),
            spawn("carrier_light", 1, 1, 2),
            spawn("carrier_heavy", 1, 1, 1),
            spawn("sim_squid", 1, 2, 15),
            spawn("sim_bigspider", 3, 5, 25),
            spawn("sim_human", 3, 5, 25),
            spawn("sim_cow", 3, 5, 25),
            spawn("sim_sheep", 3, 5, 25),
            spawn("sim_wolf", 3, 5, 25),
            spawn("sim_pig", 3, 5, 25),
            spawn("sim_villager", 3, 5, 25),
            spawn("sim_adventurer", 3, 5, 25),
            spawn("sim_horse", 3, 5, 25),
            spawn("sim_bear", 3, 5, 25),
            spawn("sim_enderman", 1, 1, 1),
            spawn("worker", 1, 1, 5),
            spawn("architect", 1, 1, 5),
            spawn("bomber_heavy", 1, 1, 1),
            spawn("wraith", 1, 1, 1),
            spawn("bogle", 1, 1, 1),
            spawn("haunter", 1, 1, 1),
            spawn("carrier_colony", 1, 1, 1),
            spawn("kirin", 1, 1, 1),
            spawn("draconite", 1, 1, 1));

    private static final List<MobSpawnSettings.SpawnerData> PHASE_THREE = List.of(
            spawn("rupter", 3, 6, 30),
            spawn("lice", 1, 4, 20),
            spawn("carrier_flying", 1, 2, 15),
            spawn("carrier_light", 1, 2, 5),
            spawn("carrier_heavy", 1, 1, 2),
            spawn("sim_squid", 1, 2, 15),
            spawn("sim_bigspider", 3, 5, 25),
            spawn("sim_human", 3, 5, 25),
            spawn("sim_cow", 3, 5, 25),
            spawn("sim_sheep", 3, 5, 25),
            spawn("sim_wolf", 3, 5, 25),
            spawn("sim_pig", 3, 5, 25),
            spawn("sim_villager", 3, 5, 25),
            spawn("sim_adventurer", 3, 5, 25),
            spawn("sim_horse", 3, 5, 25),
            spawn("sim_bear", 3, 5, 25),
            spawn("sim_enderman", 1, 1, 1),
            spawn("host", 1, 2, 5),
            spawn("worker", 1, 1, 5),
            spawn("architect", 1, 1, 5),
            spawn("bomber_heavy", 1, 1, 1),
            spawn("wraith", 1, 1, 1),
            spawn("bogle", 1, 1, 1),
            spawn("haunter", 1, 1, 1),
            spawn("carrier_colony", 1, 1, 1),
            spawn("kirin", 1, 1, 1),
            spawn("draconite", 1, 1, 1));

    private static final List<MobSpawnSettings.SpawnerData> PHASE_FOUR = List.of(
            spawn("rupter", 3, 6, 30),
            spawn("lice", 1, 4, 20),
            spawn("carrier_flying", 1, 2, 15),
            spawn("carrier_light", 1, 2, 5),
            spawn("carrier_heavy", 1, 1, 2),
            spawn("sim_squid", 1, 2, 15),
            spawn("sim_bigspider", 3, 5, 25),
            spawn("sim_human", 3, 5, 25),
            spawn("sim_cow", 3, 5, 25),
            spawn("sim_sheep", 3, 5, 25),
            spawn("sim_wolf", 3, 5, 25),
            spawn("sim_pig", 3, 5, 25),
            spawn("sim_villager", 3, 5, 25),
            spawn("sim_adventurer", 3, 5, 25),
            spawn("sim_horse", 3, 5, 25),
            spawn("sim_bear", 3, 5, 25),
            spawn("sim_enderman", 1, 1, 1),
            spawn("host", 1, 2, 5),
            spawn("heed", 1, 2, 5),
            spawn("mar_human", 1, 1, 1),
            spawn("mar_cow", 1, 1, 1),
            spawn("mar_sheep", 1, 1, 1),
            spawn("mar_villager", 1, 1, 1),
            spawn("mar_bear", 1, 1, 1),
            spawn("mar_enderman", 1, 1, 1),
            spawn("worker", 1, 1, 5),
            spawn("architect", 1, 1, 5),
            spawn("bomber_heavy", 1, 1, 1),
            spawn("wraith", 1, 1, 1),
            spawn("bogle", 1, 1, 1),
            spawn("haunter", 1, 1, 1),
            spawn("carrier_colony", 1, 1, 1),
            spawn("kirin", 1, 1, 1),
            spawn("draconite", 1, 1, 1));

    private static final List<MobSpawnSettings.SpawnerData> PHASE_FIVE = List.of(
            spawn("rupter", 3, 6, 30),
            spawn("lice", 1, 4, 20),
            spawn("sim_squid", 1, 2, 15),
            spawn("sim_bigspider", 3, 5, 25),
            spawn("sim_human", 3, 5, 25),
            spawn("sim_cow", 3, 5, 25),
            spawn("sim_sheep", 3, 5, 25),
            spawn("sim_wolf", 3, 5, 25),
            spawn("sim_pig", 3, 5, 25),
            spawn("sim_villager", 3, 5, 25),
            spawn("sim_adventurer", 3, 5, 25),
            spawn("sim_horse", 3, 5, 25),
            spawn("sim_bear", 3, 5, 25),
            spawn("sim_enderman", 1, 1, 1),
            spawn("host", 1, 2, 5),
            spawn("heed", 1, 2, 5),
            spawn("crux", 1, 2, 5),
            spawn("dredge", 1, 2, 5),
            spawn("mar_human", 1, 1, 1),
            spawn("mar_cow", 1, 1, 1),
            spawn("mar_sheep", 1, 1, 1),
            spawn("mar_villager", 1, 1, 1),
            spawn("mar_bear", 1, 1, 1),
            spawn("mar_enderman", 1, 1, 1),
            spawn("worker", 1, 1, 5),
            spawn("architect", 1, 1, 5),
            spawn("bomber_heavy", 1, 1, 1),
            spawn("wraith", 1, 1, 1),
            spawn("bogle", 1, 1, 1),
            spawn("haunter", 1, 1, 1),
            spawn("carrier_colony", 1, 1, 1),
            spawn("kirin", 1, 1, 1),
            spawn("draconite", 1, 1, 1));

    private static final List<MobSpawnSettings.SpawnerData> PHASE_SIX = appendPhaseSix(PHASE_FIVE);

    private static final List<MobSpawnSettings.SpawnerData> PHASE_SEVEN = List.of(
            spawn("rupter", 3, 6, 30),
            spawn("lice", 1, 4, 20),
            spawn("sim_squid", 1, 2, 15),
            spawn("sim_bigspider", 3, 5, 25),
            spawn("sim_human", 3, 5, 25),
            spawn("sim_cow", 3, 5, 25),
            spawn("sim_sheep", 3, 5, 25),
            spawn("sim_wolf", 3, 5, 25),
            spawn("sim_pig", 3, 5, 25),
            spawn("sim_villager", 3, 5, 25),
            spawn("sim_adventurer", 3, 5, 25),
            spawn("sim_horse", 3, 5, 25),
            spawn("sim_bear", 3, 5, 25),
            spawn("sim_enderman", 1, 1, 1),
            spawn("host", 1, 2, 5),
            spawn("hostii", 1, 2, 5),
            spawn("heed", 1, 2, 5),
            spawn("crux", 1, 2, 5),
            spawn("dredge", 1, 2, 5),
            spawn("airscrew", 1, 2, 5),
            spawn("mar_human", 1, 1, 1),
            spawn("mar_cow", 1, 1, 1),
            spawn("mar_sheep", 1, 1, 1),
            spawn("mar_villager", 1, 1, 1),
            spawn("mar_bear", 1, 1, 1),
            spawn("mar_enderman", 1, 1, 1),
            spawn("abo_bodies", 1, 2, 5),
            spawn("mangler", 3, 6, 30),
            spawn("worker", 1, 1, 5),
            spawn("architect", 1, 1, 5),
            spawn("bomber_heavy", 1, 1, 1),
            spawn("wraith", 1, 1, 1),
            spawn("bogle", 1, 1, 1),
            spawn("haunter", 1, 1, 1),
            spawn("carrier_colony", 1, 1, 1),
            spawn("kirin", 1, 1, 1),
            spawn("draconite", 1, 1, 1));

    private static final List<MobSpawnSettings.SpawnerData> PHASE_EIGHT = latePhase(false, false);
    private static final List<MobSpawnSettings.SpawnerData> PHASE_NINE = latePhase(true, true);
    private static final List<MobSpawnSettings.SpawnerData> PHASE_TEN = latePhase(true, false);

    private static final List<MobSpawnSettings.SpawnerData> UD_TWO = List.of(
            spawn("pri_devourer", 1, 2, 5),
            spawn("pri_longarms", 2, 3, 15),
            spawn("pri_manducater", 2, 3, 15),
            spawn("pri_reeker", 2, 3, 15),
            spawn("pri_yelloweye", 2, 3, 10),
            spawn("pri_summoner", 2, 3, 15),
            spawn("pri_bolster", 2, 3, 15),
            spawn("pri_arachnida", 2, 3, 15),
            spawn("thrall", 3, 5, 25));

    private static final List<MobSpawnSettings.SpawnerData> UD_THREE = List.of(
            spawn("pri_devourer", 1, 2, 5),
            spawn("pri_longarms", 2, 3, 15),
            spawn("pri_manducater", 2, 3, 15),
            spawn("pri_reeker", 2, 3, 15),
            spawn("pri_yelloweye", 2, 3, 10),
            spawn("pri_summoner", 2, 3, 15),
            spawn("pri_bolster", 2, 3, 15),
            spawn("pri_arachnida", 2, 3, 15),
            spawn("thrall", 3, 5, 25),
            spawn("ada_devourer", 1, 2, 10),
            spawn("ada_longarms", 2, 3, 20),
            spawn("ada_manducater", 2, 3, 20),
            spawn("ada_reeker", 2, 3, 20),
            spawn("ada_yelloweye", 2, 3, 15),
            spawn("ada_summoner", 2, 3, 20),
            spawn("ada_bolster", 2, 3, 20),
            spawn("ada_arachnida", 2, 3, 20));

    private static final List<MobSpawnSettings.SpawnerData> UD_FOUR = List.of(
            spawn("ada_devourer", 1, 2, 10),
            spawn("ada_longarms", 2, 3, 20),
            spawn("ada_manducater", 2, 3, 20),
            spawn("ada_reeker", 2, 3, 20),
            spawn("ada_yelloweye", 2, 3, 15),
            spawn("ada_summoner", 2, 3, 20),
            spawn("ada_bolster", 2, 3, 20),
            spawn("ada_arachnida", 2, 3, 20),
            spawn("grunt", 3, 6, 30),
            spawn("monarch", 1, 2, 10),
            spawn("warden", 1, 2, 10),
            spawn("overseer", 1, 2, 10),
            spawn("vigilante", 1, 2, 10),
            spawn("marauder", 1, 2, 10),
            spawn("grunt", 6, 10, 40));

    private NaturalSpawnTables() {
    }

    public static List<MobSpawnSettings.SpawnerData> select(ServerLevel level, BlockPos pos) {
        int phase = SrpWorldData.get(level).evolutionPhase();
        if (phase == -2 || phase == -1 && !isInsideVector(level, pos)) {
            return List.of();
        }
        List<MobSpawnSettings.SpawnerData> ubiquitous = ubiquitousEntries(level);
        if (!ubiquitous.isEmpty() && usesUbiquitousTable(level)) {
            return ubiquitous;
        }
        return phaseEntries(phase);
    }

    public static boolean canSpawnAtPhase(String path, int phase) {
        return contains(phaseEntries(phase), path);
    }

    public static boolean canSpawnNaturally(ServerLevel level, BlockPos pos, String path) {
        return contains(select(level, pos), path)
                && EvolutionSystem.crossDimensionUnlocked(level, path);
    }

    public static List<EntityType<?>> allSpawnTypes() {
        return Stream.of(
                        PHASE_MINUS_ONE, PHASE_ZERO, PHASE_ONE, PHASE_TWO, PHASE_THREE, PHASE_FOUR,
                        PHASE_FIVE, PHASE_SIX, PHASE_SEVEN, PHASE_EIGHT, PHASE_NINE, PHASE_TEN,
                        UD_TWO, UD_THREE, UD_FOUR)
                .flatMap(List::stream)
                .<EntityType<?>>map(entry -> entry.type)
                .distinct()
                .toList();
    }

    public static boolean isInsideVector(ServerLevel level, BlockPos pos) {
        for (SrpWorldData.VectorEntry vector : SrpWorldData.get(level).vectors()) {
            long radius = Math.max(1, vector.radius());
            if (vector.health() > 0 && vector.pos().distSqr(pos) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private static List<MobSpawnSettings.SpawnerData> phaseEntries(int phase) {
        return switch (phase) {
            case -1 -> PHASE_MINUS_ONE;
            case 0 -> PHASE_ZERO;
            case 1 -> PHASE_ONE;
            case 2 -> PHASE_TWO;
            case 3 -> PHASE_THREE;
            case 4 -> PHASE_FOUR;
            case 5 -> PHASE_FIVE;
            case 6 -> PHASE_SIX;
            case 7 -> PHASE_SEVEN;
            case 8 -> PHASE_EIGHT;
            case 9 -> PHASE_NINE;
            default -> phase >= 10 ? PHASE_TEN : List.of();
        };
    }

    private static List<MobSpawnSettings.SpawnerData> ubiquitousEntries(ServerLevel level) {
        return switch (EvolutionSystem.ubiquitousDevelopment(level.getServer())) {
            case 2 -> UD_TWO;
            case 3 -> UD_THREE;
            case 4 -> UD_FOUR;
            default -> List.of();
        };
    }

    private static boolean usesUbiquitousTable(ServerLevel level) {
        long hash = level.getSeed();
        hash ^= level.getGameTime() * 0x9E3779B97F4A7C15L;
        hash ^= (long) level.dimension().location().hashCode() * 0xC2B2AE3D27D4EB4FL;
        hash ^= hash >>> 33;
        hash *= 0xFF51AFD7ED558CCDL;
        hash ^= hash >>> 33;
        hash *= 0xC4CEB9FE1A85EC53L;
        hash ^= hash >>> 33;
        double sample = (hash >>> 11) * 0x1.0p-53;
        return sample < UBIQUITOUS_TABLE_CHANCE;
    }

    private static boolean contains(List<MobSpawnSettings.SpawnerData> entries, String path) {
        for (MobSpawnSettings.SpawnerData entry : entries) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    private static List<MobSpawnSettings.SpawnerData> appendPhaseSix(
            List<MobSpawnSettings.SpawnerData> phaseFive) {
        java.util.ArrayList<MobSpawnSettings.SpawnerData> entries = new java.util.ArrayList<>(phaseFive);
        int insertion = entries.size() - 9;
        entries.add(insertion, spawn("abo_bodies", 1, 2, 5));
        entries.add(insertion + 1, spawn("mangler", 3, 6, 30));
        return List.copyOf(entries);
    }

    private static List<MobSpawnSettings.SpawnerData> latePhase(boolean dragon, boolean largerPreeminentGroups) {
        java.util.ArrayList<MobSpawnSettings.SpawnerData> entries = new java.util.ArrayList<>();
        entries.add(spawn("lice", 1, 4, 20));
        if (dragon) {
            entries.add(spawn("sim_dragone", 1, 1, 1));
        }
        entries.addAll(List.of(
                spawn("fer_human", 4, 5, 25),
                spawn("fer_cow", 3, 6, 25),
                spawn("fer_sheep", 4, 5, 25),
                spawn("fer_wolf", 3, 6, 25),
                spawn("fer_pig", 3, 5, 25),
                spawn("fer_villager", 3, 5, 25),
                spawn("fer_horse", 3, 5, 25),
                spawn("fer_enderman", 3, 5, 25),
                spawn("hostii", 1, 2, 5),
                spawn("heed", 1, 2, 5),
                spawn("crux", 1, 2, 5),
                spawn("dredge", 1, 2, 5),
                spawn("airscrew", 1, 2, 5),
                spawn("mar_human", 1, 1, 1),
                spawn("mar_cow", 1, 1, 1),
                spawn("mar_sheep", 1, 1, 1),
                spawn("mar_villager", 1, 1, 1),
                spawn("mar_bear", 1, 1, 1),
                spawn("mar_enderman", 1, 1, 1),
                spawn("abo_bodies", 1, 2, 5),
                spawn("mangler", 3, 6, 30),
                spawn("bomber_light", 1, 1, 5),
                spawn("worker", 1, 1, 5)));
        if (!dragon || largerPreeminentGroups) {
            entries.add(spawn("architect", 1, 1, 5));
        }
        int max = largerPreeminentGroups ? 2 : 1;
        int weight = largerPreeminentGroups ? 5 : 1;
        entries.addAll(List.of(
                spawn("bomber_heavy", 1, max, weight),
                spawn("wraith", 1, max, weight),
                spawn("bogle", 1, max, weight),
                spawn("haunter", 1, max, weight),
                spawn("carrier_colony", 1, max, weight),
                spawn("kirin", 1, 1, 1),
                spawn("draconite", 1, 1, 1)));
        return List.copyOf(entries);
    }

    private static MobSpawnSettings.SpawnerData spawn(String path, int minCount, int maxCount, int weight) {
        ResourceLocation id = new ResourceLocation(Csrp.MODID, path);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                .orElseThrow(() -> new IllegalStateException("Missing natural spawn entity " + id));
        return new MobSpawnSettings.SpawnerData(type, weight, minCount, maxCount);
    }
}
