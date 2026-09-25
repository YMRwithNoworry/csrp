package alku.csrp.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GeneralConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ALLOW_MOBS = BUILDER
            .comment("Allow CSRP parasites to spawn naturally.")
            .define("allowMobs", true);
    private static final ModConfigSpec.DoubleValue GLOBAL_HEALTH_MULTIPLIER = BUILDER
            .comment("Global maximum-health multiplier for every parasite.")
            .defineInRange("globalHealthMultiplier", 1.0D, 0.01D, 100.0D);
    private static final ModConfigSpec.DoubleValue GLOBAL_ARMOR_MULTIPLIER = BUILDER
            .comment("Global armor multiplier for every parasite.")
            .defineInRange("globalArmorMultiplier", 1.0D, 0.01D, 100.0D);
    private static final ModConfigSpec.DoubleValue GLOBAL_DAMAGE_MULTIPLIER = BUILDER
            .comment("Global attack-damage multiplier for every parasite.")
            .defineInRange("globalDamageMultiplier", 1.0D, 0.01D, 100.0D);
    private static final ModConfigSpec.DoubleValue GLOBAL_KNOCKBACK_RESISTANCE_MULTIPLIER = BUILDER
            .comment("Global knockback-resistance multiplier for every parasite.")
            .defineInRange("globalKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private GeneralConfig() {
    }

    public static boolean allowMobs() {
        return safe(ALLOW_MOBS);
    }

    public static double globalHealthMultiplier() {
        return safe(GLOBAL_HEALTH_MULTIPLIER);
    }

    public static double globalArmorMultiplier() {
        return safe(GLOBAL_ARMOR_MULTIPLIER);
    }

    public static double globalDamageMultiplier() {
        return safe(GLOBAL_DAMAGE_MULTIPLIER);
    }

    public static double globalKnockbackResistanceMultiplier() {
        return safe(GLOBAL_KNOCKBACK_RESISTANCE_MULTIPLIER);
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
