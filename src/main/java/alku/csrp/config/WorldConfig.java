package alku.csrp.config;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class WorldConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue WORLD_SPAWNING_MOB_CAP = BUILDER
            .comment("Base number of parasites allowed to spawn naturally in one dimension. Set to 0 to disable the cap.")
            .defineInRange("worldSpawningMobCap", 20, 0, 50000);
    private static final ModConfigSpec.IntValue WORLD_MOB_CAP = BUILDER
            .comment("Base world-wide parasite limit used by Beckon biomass summons (original: worldMobCap).")
            .defineInRange("worldMobCap", 40, 0, 50000);
    private static final ModConfigSpec.IntValue WORLD_MOB_CAP_PLUS_PLAYER = BUILDER
            .comment("Parasite cap added for each player to natural spawning and Beckon summons.")
            .defineInRange("worldMobCapPlusPlayer", 5, 0, 50000);
    private static final ModConfigSpec.IntValue WORLD_BECKON_SPAWNS_CAP = BUILDER
            .comment("Additional world-wide parasite capacity reserved for Beckon summons (original: worldBeckonSpawnsCap).")
            .defineInRange("worldBeckonSpawnsCap", 20, 0, 50000);
    private static final ModConfigSpec.BooleanValue MOB_CLEANER_ENABLED = BUILDER
            .comment("Remove excess parasites when their count exceeds twice the current natural mob cap.")
            .define("mobCleanerEnabled", true);
    private static final ModConfigSpec.IntValue BECKON_INFESTATION_BLOCK_LIMIT = BUILDER
            .comment("Transformed blocks before Beckon infestation enters its cooldown.")
            .defineInRange("beckonInfestationBlockLimit", 1000, 0, 8192);
    private static final ModConfigSpec.IntValue BIOME_INFESTATION_BLOCK_LIMIT = BUILDER
            .comment("Transformed blocks before biome infestation enters its cooldown.")
            .defineInRange("biomeInfestationBlockLimit", 2000, 0, 8192);
    private static final ModConfigSpec.IntValue BECKON_INFESTATION_COOLDOWN = BUILDER
            .comment("Ticks before the Beckon infestation block counter resets after reaching its limit.")
            .defineInRange("beckonInfestationCooldown", 300, 0, 1024);
    private static final ModConfigSpec.IntValue BIOME_INFESTATION_COOLDOWN = BUILDER
            .comment("Ticks before the biome infestation block counter resets after reaching its limit.")
            .defineInRange("biomeInfestationCooldown", 300, 0, 1024);
    private static final ModConfigSpec.BooleanValue ENABLE_STAR_WORLD_SHADERS = BUILDER
            .comment("Enable the post-processing shaders used by Cold and Warm Star worlds.")
            .define("enableStarWorldShaders", true);
    private static final ModConfigSpec.BooleanValue ENABLE_COLD_STAR_SHADER = BUILDER
            .comment("Enable the outdoor frozen-fog shader in Cold Star worlds.")
            .define("enableColdStarShader", true);
    private static final ModConfigSpec.BooleanValue ENABLE_WARM_STAR_SHADER = BUILDER
            .comment("Enable the outdoor heat-haze shader in Warm Star worlds.")
            .define("enableWarmStarShader", true);
    private static final ModConfigSpec.BooleanValue DIMENSION_LIST_IS_BLACKLIST = BUILDER
            .comment("When true, listed dimensions deny natural parasite spawning; when false, only listed dimensions allow it.")
            .define("dimensionListIsBlacklist", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSION_LIST = BUILDER
            .comment("Dimension ids used by dimensionListIsBlacklist.")
            .defineList("dimensionList", List.of(), value -> value instanceof String id
                    && Identifier.tryParse(id) != null);

    // Original SRPConfigWorld "EscapeSystem" section (EscapeOnDeathHandler / EscapeRespawnHandler).
    private static final ModConfigSpec.BooleanValue ESCAPE_ENABLED = BUILDER
            .comment("If false, the escape system will never trigger.")
            .define("escapeEnabled", true);
    private static final ModConfigSpec.IntValue ESCAPE_MIN_DISTANCE = BUILDER
            .comment("Minimum distance from the source that escape teleport can choose.")
            .defineInRange("escapeMinDistance", 200, 0, 100000000);
    private static final ModConfigSpec.IntValue ESCAPE_MAX_DISTANCE = BUILDER
            .comment("Maximum distance from the source that escape teleport can choose.")
            .defineInRange("escapeMaxDistance", 300, 0, 100000000);

    // Original SRPConfigWorld "world_biome" section: the node age thresholds, the two per-stage range
    // tables and the parasite biome fog.
    private static final ModConfigSpec.IntValue NODE_TWO_TIME = BUILDER
            .comment("Node age from which a node counts as stage 2 (original: Node 2 Time Needed).")
            .defineInRange("nodeTwoTime", 10, 0, 2147483640);
    private static final ModConfigSpec.IntValue NODE_THREE_TIME = BUILDER
            .comment("Node age from which a node counts as stage 3 (original: Node 3 Time Needed).")
            .defineInRange("nodeThreeTime", 40, 0, 2147483640);
    private static final ModConfigSpec.IntValue NODE_RANGE_EFFECTS_ONE = BUILDER
            .comment("Radius in which a stage 1 node affects the world (original: Node 1 Effect Range).")
            .defineInRange("nodeRangeEffectsOne", 400, 0, 2147483640);
    private static final ModConfigSpec.IntValue NODE_RANGE_EFFECTS_TWO = BUILDER
            .comment("Radius in which a stage 2 node affects the world (original: Node 2 Effect Range).")
            .defineInRange("nodeRangeEffectsTwo", 1600, 0, 2147483640);
    private static final ModConfigSpec.IntValue NODE_RANGE_EFFECTS_THREE = BUILDER
            .comment("Radius in which a stage 3 node affects the world (original: Node 3 Effect Range).")
            .defineInRange("nodeRangeEffectsThree", 8000, 0, 2147483640);
    private static final ModConfigSpec.IntValue NODE_RANGE_SPREAD_ONE = BUILDER
            .comment("Radius a stage 1 node spreads its biome over (original: Node 1 Spread Range).")
            .defineInRange("nodeRangeSpreadOne", 200, 0, 2147483640);
    private static final ModConfigSpec.IntValue NODE_RANGE_SPREAD_TWO = BUILDER
            .comment("Radius a stage 2 node spreads its biome over (original: Node 2 Spread Range).")
            .defineInRange("nodeRangeSpreadTwo", 800, 0, 2147483640);
    private static final ModConfigSpec.IntValue NODE_RANGE_SPREAD_THREE = BUILDER
            .comment("Radius a stage 3 node spreads its biome over (original: Node 3 Spread Range).")
            .defineInRange("nodeRangeSpreadThree", 4000, 0, 2147483640);
    private static final ModConfigSpec.DoubleValue BIOME_FOG_DENSITY = BUILDER
            .comment("Amount of fog a parasite biome has (original: Biome Fog Density).")
            .defineInRange("biomeFogDensity", 0.06D, 0.0D, 1.0D);
    private static final ModConfigSpec.IntValue BIOME_HEART_FREQ = BUILDER
            .comment("Player ticks between two plays of the core block node sound (original: Biome Node Heart Frequency).")
            .defineInRange("biomeHeartFreq", 30, 0, 100);
    private static final ModConfigSpec.DoubleValue BIOME_HEART_VOL = BUILDER
            .comment("Volume of the core block node sound (original: Biome Node Heart Volume).")
            .defineInRange("biomeHeartVol", 0.4D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue BIOME_ONE_FOG_RED = fogColor("biomeOneFogRed", 38.0D);
    private static final ModConfigSpec.DoubleValue BIOME_ONE_FOG_GREEN = fogColor("biomeOneFogGreen", 36.0D);
    private static final ModConfigSpec.DoubleValue BIOME_ONE_FOG_BLUE = fogColor("biomeOneFogBlue", 36.0D);
    private static final ModConfigSpec.DoubleValue BIOME_TWO_FOG_RED = fogColor("biomeTwoFogRed", 81.0D);
    private static final ModConfigSpec.DoubleValue BIOME_TWO_FOG_GREEN = fogColor("biomeTwoFogGreen", 107.0D);
    private static final ModConfigSpec.DoubleValue BIOME_TWO_FOG_BLUE = fogColor("biomeTwoFogBlue", 8.0D);
    private static final ModConfigSpec.DoubleValue BIOME_THREE_FOG_RED = fogColor("biomeThreeFogRed", 199.0D);
    private static final ModConfigSpec.DoubleValue BIOME_THREE_FOG_GREEN = fogColor("biomeThreeFogGreen", 201.0D);
    private static final ModConfigSpec.DoubleValue BIOME_THREE_FOG_BLUE = fogColor("biomeThreeFogBlue", 91.0D);
    private static final ModConfigSpec.DoubleValue BIOME_FOUR_FOG_RED = fogColor("biomeFourFogRed", 100.0D);
    private static final ModConfigSpec.DoubleValue BIOME_FOUR_FOG_GREEN = fogColor("biomeFourFogGreen", 100.0D);
    private static final ModConfigSpec.DoubleValue BIOME_FOUR_FOG_BLUE = fogColor("biomeFourFogBlue", 100.0D);
    // Original SRPConfigWorld origin / EIV creation section (SRPEventHandlerBus#worldTick ->
    // EIVUtil.createRandomOrigin). The world rolls once per in-game day for a new Emerging
    // Infestation Vector around a random player.
    // Original SRPConfigWorld.nodesActivated / coloniesActivated.
    private static final ModConfigSpec.BooleanValue NODES_ACTIVATED = BUILDER
            .comment("Enable the node (Biome Heart) system (original: nodesActivated).")
            .define("nodesActivated", true);
    private static final ModConfigSpec.BooleanValue COLONIES_ACTIVATED = BUILDER
            .comment("Enable the colony (Colony Heart) system (original: coloniesActivated).")
            .define("coloniesActivated", true);
    private static final ModConfigSpec.BooleanValue ORIGIN_ACTIVATED = BUILDER
            .comment("Allow the world to create random Emerging Infestation Vectors (origins) over time.")
            .define("originActivated", true);
    private static final ModConfigSpec.IntValue ORIGIN_CREATING_RAND = BUILDER
            .comment("1-in-N chance per day to create an origin while one already exists (original: originCreatingRand).")
            .defineInRange("originCreatingRand", 10, 0, 1000000);
    private static final ModConfigSpec.IntValue ORIGIN_CREATING_RAND_ZERO = BUILDER
            .comment("1-in-N chance per day to create the first origin (original: originCreatingRandZero).")
            .defineInRange("originCreatingRandZero", 3, 0, 1000000);
    private static final ModConfigSpec.IntValue ORIGIN_CREATING_DISTANCE_MIN = BUILDER
            .comment("Minimum distance from a random player for a new origin (original: originCreatingDistanceMin).")
            .defineInRange("originCreatingDistanceMin", 1000, 0, 100000000);
    private static final ModConfigSpec.IntValue ORIGIN_CREATING_DISTANCE_MAX = BUILDER
            .comment("Maximum distance from a random player for a new origin (original: originCreatingDistanceMax).")
            .defineInRange("originCreatingDistanceMax", 10000, 0, 100000000);
    private static final ModConfigSpec.IntValue ORIGIN_HEALTH = BUILDER
            .comment("Initial health of a randomly created origin (original: originHealth).")
            .defineInRange("originHealth", 350, 0, 2000000000);

    // Original SRPConfigWorld: node-age crop suppression and the per-biome heal penalty
    // (SRPEventHandlerBus#cropGrow / #entityHeal).
    private static final ModConfigSpec.DoubleValue NODE_CROP_STOP_ONE = BUILDER
            .comment("Chance a stage 1 node stops crop growth (original: nodeCropStopNodeOne).")
            .defineInRange("nodeCropStopNodeOne", 0.3D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue NODE_CROP_STOP_TWO = BUILDER
            .comment("Chance a stage 2 node stops crop growth (original: nodeCropStopNodeTwo).")
            .defineInRange("nodeCropStopNodeTwo", 0.6D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue NODE_CROP_STOP_THREE = BUILDER
            .comment("Chance a stage 3 node stops crop growth (original: nodeCropStopNodeThree).")
            .defineInRange("nodeCropStopNodeThree", 1.0D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue BIOME_ONE_HEAL_PENALTY = BUILDER
            .comment("Healing multiplier for non-parasites in a shrouded biome (original: biomeOneHealPenalty).")
            .defineInRange("biomeOneHealPenalty", 0.5D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue BIOME_TWO_HEAL_PENALTY = BUILDER
            .comment("Healing multiplier for non-parasites in a boils biome (original: biomeTwoHealPenalty).")
            .defineInRange("biomeTwoHealPenalty", 0.5D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue BIOME_THREE_HEAL_PENALTY = BUILDER
            .comment("Healing multiplier for non-parasites in a harlequin biome (original: biomeThreeHealPenalty).")
            .defineInRange("biomeThreeHealPenalty", 0.5D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue BIOME_FOUR_HEAL_PENALTY = BUILDER
            .comment("Healing multiplier for non-parasites in a demen biome (original: biomeFourHealPenalty).")
            .defineInRange("biomeFourHealPenalty", 0.5D, 0.0D, 1000.0D);
    // Original SRPConfigWorld.potionEffectForNodes: parasite spawn buffs from node points.
    private static final ModConfigSpec.ConfigValue<List<? extends String>> NODE_POTION_EFFECTS = BUILDER
            .comment("Potion effects granted to parasites spawned under nodes, \"points;effect;amplifier\" "
                    + "(original: potionEffectForNodes).")
            .defineList("potionEffectForNodes", List.of(
                    "3;minecraft:speed;2",
                    "4;minecraft:fire_resistance;1",
                    "7;minecraft:invisibility;1"),
                    value -> value instanceof String entry && entry.split(";", -1).length == 3);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BIOME_HEAL_PENALTY_BLACKLIST = BUILDER
            .comment("Entity ids exempt from the parasite-biome healing penalty (original: biomeHealPenaltyBlackList).")
            .defineList("biomeHealPenaltyBlackList", List.of("minecraft:villager_golem"),
                    value -> value instanceof String entry && !entry.isBlank());
    private static final ModConfigSpec.BooleanValue BIOME_HEAL_PENALTY_BLACKLIST_INVERTED = BUILDER
            .comment("Treat the biome healing-penalty blacklist as a whitelist (original: biomeHealPenaltyBlackListWhite).")
            .define("biomeHealPenaltyBlackListInverted", false);
    private static final ModConfigSpec.IntValue ORIGIN_RADIUS = BUILDER
            .comment("Initial radius of a randomly created origin (original: originRadius).")
            .defineInRange("originRadius", 200, 0, 2000000000);

    private static ModConfigSpec.DoubleValue fogColor(String name, double defaultValue) {
        return BUILDER.comment("Parasite biome fog colour channel, 0..255 (original: " + name + ").")
                .defineInRange(name, defaultValue, 0.0D, 255.0D);
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WorldConfig() {
    }

    public static int naturalMobCap(ServerLevel level) {
        int base = WORLD_SPAWNING_MOB_CAP.get();
        return base == 0 ? 0 : base + level.players().size() * WORLD_MOB_CAP_PLUS_PLAYER.get();
    }

    /** {@code SRPConfig.worldMobCap + players * worldMobCapPlusPlayer + worldBeckonSpawnsCap}. */
    public static int beckonSummonLimit(ServerLevel level) {
        return WORLD_MOB_CAP.get() + level.players().size() * WORLD_MOB_CAP_PLUS_PLAYER.get()
                + WORLD_BECKON_SPAWNS_CAP.get();
    }

    public static boolean mobCleanerEnabled() {
        return MOB_CLEANER_ENABLED.get();
    }

    public static int beckonInfestationBlockLimit() {
        return BECKON_INFESTATION_BLOCK_LIMIT.get();
    }

    public static int biomeInfestationBlockLimit() {
        return BIOME_INFESTATION_BLOCK_LIMIT.get();
    }

    public static int beckonInfestationCooldown() {
        return BECKON_INFESTATION_COOLDOWN.get();
    }

    public static int biomeInfestationCooldown() {
        return BIOME_INFESTATION_COOLDOWN.get();
    }

    public static boolean starWorldShadersEnabled() {
        return ENABLE_STAR_WORLD_SHADERS.get();
    }

    public static boolean coldStarShaderEnabled() {
        return ENABLE_COLD_STAR_SHADER.get();
    }

    public static boolean warmStarShaderEnabled() {
        return ENABLE_WARM_STAR_SHADER.get();
    }

    public static boolean dimensionAllowsNaturalSpawning(ServerLevel level) {
        String dimension = level.dimension().identifier().toString();
        boolean listed = DIMENSION_LIST.get().contains(dimension);
        return DIMENSION_LIST_IS_BLACKLIST.get() != listed;
    }

    /** {@code SRPConfigWorld.escapeEnabled}. */
    public static boolean escapeEnabled() {
        return ESCAPE_ENABLED.get();
    }

    /** {@code SRPConfigWorld.escapeMinDistance}. */
    public static int escapeMinDistance() {
        return ESCAPE_MIN_DISTANCE.get();
    }

    /** {@code SRPConfigWorld.escapeMaxDistance}. */
    public static int escapeMaxDistance() {
        return ESCAPE_MAX_DISTANCE.get();
    }

    /** {@code SRPConfigWorld.timeNeedeToNodeTwo} — the age a node becomes stage 2 at. */
    public static int nodeTwoTime() {
        return NODE_TWO_TIME.get();
    }

    /** {@code SRPConfigWorld.timeNeedeToNodeThree} — the age a node becomes stage 3 at. */
    public static int nodeThreeTime() {
        return NODE_THREE_TIME.get();
    }

    /** {@code SRPWorldData#getDistanceEffectByAge} — the effect radius of a node stage, -1 outside 1..3. */
    public static int nodeRangeEffects(int stage) {
        return switch (stage) {
            case 1 -> NODE_RANGE_EFFECTS_ONE.get();
            case 2 -> NODE_RANGE_EFFECTS_TWO.get();
            case 3 -> NODE_RANGE_EFFECTS_THREE.get();
            default -> -1;
        };
    }

    /** {@code SRPWorldData#getDistanceSpreadByAge} — the spread radius of a node stage, -1 outside 1..3. */
    public static int nodeRangeSpread(int stage) {
        return switch (stage) {
            case 1 -> NODE_RANGE_SPREAD_ONE.get();
            case 2 -> NODE_RANGE_SPREAD_TWO.get();
            case 3 -> NODE_RANGE_SPREAD_THREE.get();
            default -> -1;
        };
    }

    /** {@code SRPConfigWorld.biomeFogDensity} — how thick the parasite biome fog gets, 0..1. */
    public static float biomeFogDensity() {
        return BIOME_FOG_DENSITY.get().floatValue();
    }

    /** {@code SRPConfigWorld.biomeHeartFreq} — player ticks between two node heart sounds. */
    public static int biomeHeartFreq() {
        return BIOME_HEART_FREQ.get();
    }

    /** {@code SRPConfigWorld.biomeHeartVol} — volume of the node heart sound. */
    public static float biomeHeartVol() {
        return BIOME_HEART_VOL.get().floatValue();
    }

    /** {@code BiomeParasiteShrouded/Boils/Harlequin/Demen#getRedValue()} — {@code biome} is 1..4. */
    public static float biomeFogRed(int biome) {
        return (float) switch (biome) {
            case 1 -> BIOME_ONE_FOG_RED.get();
            case 2 -> BIOME_TWO_FOG_RED.get();
            case 3 -> BIOME_THREE_FOG_RED.get();
            case 4 -> BIOME_FOUR_FOG_RED.get();
            default -> 0.0D;
        };
    }

    /** {@code BiomeParasite*#getGreenValue()} — {@code biome} is 1..4. */
    public static float biomeFogGreen(int biome) {
        return (float) switch (biome) {
            case 1 -> BIOME_ONE_FOG_GREEN.get();
            case 2 -> BIOME_TWO_FOG_GREEN.get();
            case 3 -> BIOME_THREE_FOG_GREEN.get();
            case 4 -> BIOME_FOUR_FOG_GREEN.get();
            default -> 0.0D;
        };
    }

    /** {@code BiomeParasite*#getBlueValue()} — {@code biome} is 1..4. */
    public static float biomeFogBlue(int biome) {
        return (float) switch (biome) {
            case 1 -> BIOME_ONE_FOG_BLUE.get();
            case 2 -> BIOME_TWO_FOG_BLUE.get();
            case 3 -> BIOME_THREE_FOG_BLUE.get();
            case 4 -> BIOME_FOUR_FOG_BLUE.get();
            default -> 0.0D;
        };
    }
    /** {@code SRPConfigWorld.nodesActivated}. */
    public static boolean nodesActivated() {
        return NODES_ACTIVATED.get();
    }

    /** {@code SRPConfigWorld.coloniesActivated}. */
    public static boolean coloniesActivated() {
        return COLONIES_ACTIVATED.get();
    }
    /** {@code SRPConfigWorld.originActivated} — whether random origins may be created. */
    public static boolean originActivated() {
        return ORIGIN_ACTIVATED.get();
    }

    /** {@code SRPConfigWorld.originCreatingRand} — 1-in-N daily chance once an origin exists. */
    public static int originCreatingRand() {
        return ORIGIN_CREATING_RAND.get();
    }

    /** {@code SRPConfigWorld.originCreatingRandZero} — 1-in-N daily chance for the first origin. */
    public static int originCreatingRandZero() {
        return ORIGIN_CREATING_RAND_ZERO.get();
    }

    /** {@code SRPConfigWorld.originCreatingDistanceMin}. */
    public static int originCreatingDistanceMin() {
        return ORIGIN_CREATING_DISTANCE_MIN.get();
    }

    /** {@code SRPConfigWorld.originCreatingDistanceMax}. */
    public static int originCreatingDistanceMax() {
        return ORIGIN_CREATING_DISTANCE_MAX.get();
    }

    /** {@code SRPConfigWorld.originHealth} — initial origin health. */
    public static int originHealth() {
        return ORIGIN_HEALTH.get();
    }


    /** {@code SRPConfigWorld.nodeCropStopNodeOne/Two/Three} — {@code nodeAge} is 1..3. */
    public static float nodeCropStopChance(int nodeAge) {
        return (float) switch (nodeAge) {
            case 1 -> NODE_CROP_STOP_ONE.get();
            case 2 -> NODE_CROP_STOP_TWO.get();
            case 3 -> NODE_CROP_STOP_THREE.get();
            default -> 0.0D;
        };
    }

    /** {@code SRPConfigWorld.biomeOne..FourHealPenalty} — {@code biome} is 1..4. */
    public static float biomeHealPenalty(int biome) {
        return switch (biome) {
            case 2 -> BIOME_TWO_HEAL_PENALTY.get().floatValue();
            case 3 -> BIOME_THREE_HEAL_PENALTY.get().floatValue();
            case 4 -> BIOME_FOUR_HEAL_PENALTY.get().floatValue();
            default -> BIOME_ONE_HEAL_PENALTY.get().floatValue();
        };
    }

    /** {@code SRPConfigWorld.potionEffectForNodes} — raw {@code points;effect;amplifier} rows. */
    public static List<? extends String> nodePotionEffects() {
        return NODE_POTION_EFFECTS.get();
    }
    /** {@code SRPConfigWorld.biomeHealPenaltyBlackList}. */
    public static boolean biomeHealPenaltyExempt(String entityId) {
        boolean listed = false;
        for (String entry : BIOME_HEAL_PENALTY_BLACKLIST.get()) {
            if (entry != null && !entry.isEmpty() && entityId.contains(entry)) {
                listed = true;
                break;
            }
        }
        // Original ParasiteEventEntity#checkName: anyMatch != isWhitelist, and a true result skips the penalty.
        return listed != BIOME_HEAL_PENALTY_BLACKLIST_INVERTED.get();
    }    /** {@code SRPConfigWorld.originRadius} — initial origin radius. */
    public static int originRadius() {
        return ORIGIN_RADIUS.get();
    }
}
