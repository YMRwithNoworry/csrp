package alku.csrp.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GeneralConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ALLOW_MOBS = BUILDER
            .comment("Allow CSRP parasites to spawn naturally.")
            .define("allowMobs", true);
    private static final ForgeConfigSpec.DoubleValue GLOBAL_HEALTH_MULTIPLIER = BUILDER
            .comment("Global maximum-health multiplier for every parasite.")
            .defineInRange("globalHealthMultiplier", 1.0D, 0.01D, 100.0D);
    private static final ForgeConfigSpec.DoubleValue GLOBAL_ARMOR_MULTIPLIER = BUILDER
            .comment("Global armor multiplier for every parasite.")
            .defineInRange("globalArmorMultiplier", 1.0D, 0.01D, 100.0D);
    private static final ForgeConfigSpec.DoubleValue GLOBAL_DAMAGE_MULTIPLIER = BUILDER
            .comment("Global attack-damage multiplier for every parasite.")
            .defineInRange("globalDamageMultiplier", 1.0D, 0.01D, 100.0D);
    private static final ForgeConfigSpec.DoubleValue GLOBAL_KNOCKBACK_RESISTANCE_MULTIPLIER = BUILDER
            .comment("Global knockback-resistance multiplier for every parasite.")
            .defineInRange("globalKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private GeneralConfig() {
    }

    public static boolean allowMobs() {
        return ALLOW_MOBS.get();
    }

    public static double globalHealthMultiplier() {
        return GLOBAL_HEALTH_MULTIPLIER.get();
    }

    public static double globalArmorMultiplier() {
        return GLOBAL_ARMOR_MULTIPLIER.get();
    }

    public static double globalDamageMultiplier() {
        return GLOBAL_DAMAGE_MULTIPLIER.get();
    }

    public static double globalKnockbackResistanceMultiplier() {
        return GLOBAL_KNOCKBACK_RESISTANCE_MULTIPLIER.get();
    }
}
