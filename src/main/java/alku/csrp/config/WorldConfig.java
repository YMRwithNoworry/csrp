package alku.csrp.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class WorldConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue WORLD_SPAWNING_MOB_CAP = BUILDER
            .comment("Base number of parasites allowed to spawn naturally in one dimension. Set to 0 to disable the cap.")
            .defineInRange("worldSpawningMobCap", 20, 0, 50000);
    private static final ModConfigSpec.IntValue WORLD_MOB_CAP_PLUS_PLAYER = BUILDER
            .comment("Natural parasite cap added for each player in the dimension.")
            .defineInRange("worldMobCapPlusPlayer", 5, 0, 50000);
    private static final ModConfigSpec.BooleanValue MOB_CLEANER_ENABLED = BUILDER
            .comment("Remove excess parasites when their count exceeds twice the current natural mob cap.")
            .define("mobCleanerEnabled", true);
    private static final ModConfigSpec.BooleanValue NEXUS_DESPAWN = BUILDER
            .comment("Legacy SRPConfig.rsDespawn (\"Nexus Versions Despawn\"): set to true to let Beckon, "
                    + "Dispatcher and Rooter versions despawn naturally. Defaults to false, which is the legacy "
                    + "behaviour: Nexus versions never disappear from natural despawn rules.")
            .define("nexusDespawn", false);
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
                    && ResourceLocation.tryParse(id) != null);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WorldConfig() {
    }

    public static int naturalMobCap(ServerLevel level) {
        int base = safe(WORLD_SPAWNING_MOB_CAP);
        return base == 0 ? 0 : base + level.players().size() * safe(WORLD_MOB_CAP_PLUS_PLAYER);
    }

    public static boolean mobCleanerEnabled() {
        return safe(MOB_CLEANER_ENABLED);
    }

    /** Legacy {@code SRPConfig.rsDespawn} ("Nexus Versions Despawn"); default false. */
    public static boolean nexusDespawn() {
        return safe(NEXUS_DESPAWN);
    }

    public static int beckonInfestationBlockLimit() {
        return safe(BECKON_INFESTATION_BLOCK_LIMIT);
    }

    public static int biomeInfestationBlockLimit() {
        return safe(BIOME_INFESTATION_BLOCK_LIMIT);
    }

    public static int beckonInfestationCooldown() {
        return safe(BECKON_INFESTATION_COOLDOWN);
    }

    public static int biomeInfestationCooldown() {
        return safe(BIOME_INFESTATION_COOLDOWN);
    }

    public static boolean starWorldShadersEnabled() {
        return safe(ENABLE_STAR_WORLD_SHADERS);
    }

    public static boolean coldStarShaderEnabled() {
        return safe(ENABLE_COLD_STAR_SHADER);
    }

    public static boolean warmStarShaderEnabled() {
        return safe(ENABLE_WARM_STAR_SHADER);
    }

    public static boolean dimensionAllowsNaturalSpawning(ServerLevel level) {
        String dimension = level.dimension().location().toString();
        boolean listed = safe(DIMENSION_LIST).contains(dimension);
        return safe(DIMENSION_LIST_IS_BLACKLIST) != listed;
    }

    /**
     * Reads a config value, falling back to its declared default while the config file has not been
     * read yet. NeoForge runs EntityAttributeCreationEvent before configs are loaded, and CSRP's
     * createAttributes() methods read config values, so an unguarded get() there crashes startup.
     */
    private static <T> T safe(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
