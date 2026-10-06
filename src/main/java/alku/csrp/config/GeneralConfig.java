package alku.csrp.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GeneralConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ALLOW_MOBS = BUILDER
            .comment("Allow CSRP parasites to spawn naturally.")
            .define("allowMobs", true);
    private static final ModConfigSpec.BooleanValue PARASITE_BLOCK_DROPS = BUILDER
            .comment("Set to true if the blocks broken by parasites should have drops.")
            .define("parasiteBlockDrops", true);
    private static final ModConfigSpec.BooleanValue WORLD_CREATION_UI = BUILDER
            .comment("Set to false if you want to disable SRP world creation options.")
            .define("worldCreationUi", true);
    private static final ModConfigSpec.BooleanValue MUSIC_ENABLED = BUILDER
            .comment("Set to false if you want to disable OST (original: Music Enabled).")
            .define("musicEnabled", true);
    private static final ModConfigSpec.IntValue MUSIC_PHASE_MIN = BUILDER
            .comment("Minimum value in ticks at which music will be played (original: Music Phase Min).")
            .defineInRange("musicPhaseMin", 14420, 1, 2147483640);
    private static final ModConfigSpec.IntValue MUSIC_PHASE_MAX = BUILDER
            .comment("Maximum value in ticks at which music will be played (original: Music Phase Max).")
            .defineInRange("musicPhaseMax", 18200, 1, 2147483640);
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
        return ALLOW_MOBS.get();
    }

    /** {@code SRPConfig.doTileDrops} — whether parasite block breaking produces drops. */
    public static boolean parasiteBlockDrops() {
        return PARASITE_BLOCK_DROPS.get();
    }

    /** {@code SRPConfig.worldGIU} — whether the SRP world-creation options are shown. */
    public static boolean worldCreationUi() {
        return WORLD_CREATION_UI.get();
    }

    /** {@code SRPConfig.musicTrue} — the OST switch. */
    public static boolean musicEnabled() {
        return MUSIC_ENABLED.get();
    }

    /** {@code SRPConfig.musicMin} — shortest gap in ticks between two OST tracks. */
    public static int musicPhaseMin() {
        return MUSIC_PHASE_MIN.get();
    }

    /** {@code SRPConfig.musicMax} — longest gap in ticks between two OST tracks. */
    public static int musicPhaseMax() {
        return MUSIC_PHASE_MAX.get();
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
