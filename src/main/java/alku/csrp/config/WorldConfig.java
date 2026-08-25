package alku.csrp.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class WorldConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue WORLD_SPAWNING_MOB_CAP = BUILDER
            .comment("Base number of parasites allowed to spawn naturally in one dimension. Set to 0 to disable the cap.")
            .defineInRange("worldSpawningMobCap", 20, 0, 50000);
    private static final ForgeConfigSpec.IntValue WORLD_MOB_CAP_PLUS_PLAYER = BUILDER
            .comment("Natural parasite cap added for each player in the dimension.")
            .defineInRange("worldMobCapPlusPlayer", 5, 0, 50000);
    private static final ForgeConfigSpec.BooleanValue MOB_CLEANER_ENABLED = BUILDER
            .comment("Remove excess parasites when their count exceeds twice the current natural mob cap.")
            .define("mobCleanerEnabled", true);
    private static final ForgeConfigSpec.IntValue BECKON_INFESTATION_BLOCK_LIMIT = BUILDER
            .comment("Transformed blocks before Beckon infestation enters its cooldown.")
            .defineInRange("beckonInfestationBlockLimit", 1000, 0, 8192);
    private static final ForgeConfigSpec.IntValue BIOME_INFESTATION_BLOCK_LIMIT = BUILDER
            .comment("Transformed blocks before biome infestation enters its cooldown.")
            .defineInRange("biomeInfestationBlockLimit", 2000, 0, 8192);
    private static final ForgeConfigSpec.IntValue BECKON_INFESTATION_COOLDOWN = BUILDER
            .comment("Ticks before the Beckon infestation block counter resets after reaching its limit.")
            .defineInRange("beckonInfestationCooldown", 300, 0, 1024);
    private static final ForgeConfigSpec.IntValue BIOME_INFESTATION_COOLDOWN = BUILDER
            .comment("Ticks before the biome infestation block counter resets after reaching its limit.")
            .defineInRange("biomeInfestationCooldown", 300, 0, 1024);
    private static final ForgeConfigSpec.BooleanValue ENABLE_STAR_WORLD_SHADERS = BUILDER
            .comment("Enable the post-processing shaders used by Cold and Warm Star worlds.")
            .define("enableStarWorldShaders", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_COLD_STAR_SHADER = BUILDER
            .comment("Enable the outdoor frozen-fog shader in Cold Star worlds.")
            .define("enableColdStarShader", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_WARM_STAR_SHADER = BUILDER
            .comment("Enable the outdoor heat-haze shader in Warm Star worlds.")
            .define("enableWarmStarShader", true);
    private static final ForgeConfigSpec.BooleanValue DIMENSION_LIST_IS_BLACKLIST = BUILDER
            .comment("When true, listed dimensions deny natural parasite spawning; when false, only listed dimensions allow it.")
            .define("dimensionListIsBlacklist", true);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_LIST = BUILDER
            .comment("Dimension ids used by dimensionListIsBlacklist.")
            .defineList("dimensionList", List.of(), value -> value instanceof String id
                    && ResourceLocation.tryParse(id) != null);
    private static final ForgeConfigSpec.BooleanValue METEOR_ENABLED = BUILDER
            .comment("Enable natural Hive Satellite meteor impacts.")
            .define("meteorEnabled", false);
    private static final ForgeConfigSpec.IntValue METEOR_CHECK_INTERVAL = BUILDER
            .comment("Ticks between natural meteor spawn checks.")
            .defineInRange("meteorCheckInterval", 3600, 1, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue METEOR_MINIMUM_WORLD_TICKS = BUILDER
            .comment("World age in ticks required before natural meteors may spawn.")
            .defineInRange("meteorMinimumWorldTicks", 0, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue METEOR_CHANCE = BUILDER
            .comment("Chance that each meteor check spawns a meteor.")
            .defineInRange("meteorChance", 0.5D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.IntValue METEOR_DAMAGE_RADIUS = BUILDER
            .comment("Radius of the distance-scaled main meteor impact damage.")
            .defineInRange("meteorDamageRadius", 110, 0, 1024);
    private static final ForgeConfigSpec.IntValue METEOR_MINIMUM_RADIUS = BUILDER
            .comment("Minimum horizontal spawn and target offset from the selected player.")
            .defineInRange("meteorMinimumRadius", 80, 0, 4096);
    private static final ForgeConfigSpec.IntValue METEOR_MAXIMUM_RADIUS = BUILDER
            .comment("Maximum horizontal spawn and target offset from the selected player.")
            .defineInRange("meteorMaximumRadius", 120, 1, 4096);
    private static final ForgeConfigSpec.BooleanValue METEOR_REQUIRES_NO_VECTOR = BUILDER
            .comment("Prevent natural meteors in dimensions which already contain an infestation vector.")
            .define("meteorRequiresNoVector", true);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> METEOR_DIMENSION_BLACKLIST = BUILDER
            .comment("Dimension ids which cannot receive natural meteors.")
            .defineList("meteorDimensionBlacklist", List.of("minecraft:the_nether"), value -> value instanceof String id
                    && ResourceLocation.tryParse(id) != null);
    private static final ForgeConfigSpec.BooleanValue METEOR_CREATES_VECTOR = BUILDER
            .comment("Create an Emerging Infestation Vector when a main meteor impacts.")
            .define("meteorCreatesVector", true);
    private static final ForgeConfigSpec.IntValue METEOR_VECTOR_HEALTH = BUILDER
            .comment("Initial health of an infestation vector created by a meteor.")
            .defineInRange("meteorVectorHealth", 350, 1, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue METEOR_VECTOR_RADIUS = BUILDER
            .comment("Initial radius of an infestation vector created by a meteor.")
            .defineInRange("meteorVectorRadius", 200, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private WorldConfig() {
    }

    public static int naturalMobCap(ServerLevel level) {
        int base = WORLD_SPAWNING_MOB_CAP.get();
        return base == 0 ? 0 : base + level.players().size() * WORLD_MOB_CAP_PLUS_PLAYER.get();
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
        String dimension = level.dimension().location().toString();
        boolean listed = DIMENSION_LIST.get().contains(dimension);
        return DIMENSION_LIST_IS_BLACKLIST.get() != listed;
    }

    public static boolean meteorsEnabled() {
        return METEOR_ENABLED.get();
    }

    public static int meteorCheckInterval() {
        return METEOR_CHECK_INTERVAL.get();
    }

    public static int meteorMinimumWorldTicks() {
        return METEOR_MINIMUM_WORLD_TICKS.get();
    }

    public static double meteorChance() {
        return METEOR_CHANCE.get();
    }

    public static int meteorDamageRadius() {
        return METEOR_DAMAGE_RADIUS.get();
    }

    public static int meteorMinimumRadius() {
        return METEOR_MINIMUM_RADIUS.get();
    }

    public static int meteorMaximumRadius() {
        return METEOR_MAXIMUM_RADIUS.get();
    }

    public static boolean meteorRequiresNoVector() {
        return METEOR_REQUIRES_NO_VECTOR.get();
    }

    public static boolean dimensionAllowsMeteors(ServerLevel level) {
        return !METEOR_DIMENSION_BLACKLIST.get().contains(level.dimension().location().toString());
    }

    public static boolean meteorCreatesVector() {
        return METEOR_CREATES_VECTOR.get();
    }

    public static int meteorVectorHealth() {
        return METEOR_VECTOR_HEALTH.get();
    }

    public static int meteorVectorRadius() {
        return METEOR_VECTOR_RADIUS.get();
    }
}
