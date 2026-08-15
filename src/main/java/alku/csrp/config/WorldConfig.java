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
    private static final ModConfigSpec.IntValue BECKON_INFESTATION_BLOCK_LIMIT = BUILDER
            .comment("Transformed blocks before Beckon infestation enters its cooldown.")
            .defineInRange("beckonInfestationBlockLimit", 1000, 0, 8192);
    private static final ModConfigSpec.IntValue BIOME_INFESTATION_BLOCK_LIMIT = BUILDER
            .comment("Transformed blocks before biome infestation enters its cooldown.")
            .defineInRange("biomeInfestationBlockLimit", 2000, 0, 8192);
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
}
