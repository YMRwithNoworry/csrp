package alku.csrp;

import alku.csrp.world.SrpWorldData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final List<Integer> DEFAULT_DISLODGMENT_PHASE_CODES = List.of(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25);
    private static final ModConfigSpec.IntValue EVOLUTION_PHASE = BUILDER
            .comment("Current parasite evolution phase used by phase-gated spawning and behavior.")
            .defineInRange("evolutionPhase", -1, -2, 10);
    private static final ModConfigSpec.DoubleValue VARIANT_SPAWN_CHANCE = BUILDER
            .comment("Chance for a parasite with an available variant to spawn as that variant.")
            .defineInRange("variantSpawnChance", 0.33D, 0.0D, 1.0D);
    private static final ModConfigSpec.IntValue ALWAYS_VARIANT_PHASE = BUILDER
            .comment("From this evolution phase onward, parasites always use an available variant.")
            .defineInRange("alwaysVariantPhase", 11, -1, 11);
    private static final ModConfigSpec.DoubleValue ADAPTATION_CHANCE = BUILDER
            .comment("Chance for a linked parasite outside a colony to share its adaptation on death.")
            .defineInRange("adaptationChance", 0.1D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue PARASITE_KILLING_REDUCTION = BUILDER
            .comment("Damage reduction per level of the matching parasite-killing status effect.")
            .defineInRange("parasiteKillingReduction", 0.15D, 0.0D, 0.95D);
    private static final ModConfigSpec.DoubleValue COTH_CONVERT = BUILDER
            .comment("Chance for a parasite kill to convert a victim that has COTH.")
            .defineInRange("cothConvert", 0.3D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue KILLCOUNT_PLUS = BUILDER
            .comment("Killcount added every second on HARD or HARDCORE when evolution phases are disabled.")
            .defineInRange("killcountPlus", 0.0D, 0.0D, 1000000.0D);
    private static final ModConfigSpec.BooleanValue USE_EVOLUTION_PHASES = BUILDER
            .comment("Use SRP evolution phases instead of the legacy difficulty killcount behavior.")
            .define("useEvolutionPhases", true);
    private static final ModConfigSpec.DoubleValue OVERLAST_NATURAL_EVOLUTION_SCALE = BUILDER
            .comment("OverLast natural evolution points multiplier. Set to 0 to disable.")
            .defineInRange("overlastNaturalEvolutionScale", 1.0D, 0.0D, 10.0D);
    private static final ModConfigSpec.BooleanValue OVERLAST_HUD_REQUIRES_CLOCK = BUILDER
            .comment("Only show the OverLast evolution HUD while holding an evolution clock.")
            .define("overlastHudRequiresClock", false);
    private static final ModConfigSpec.ConfigValue<String> OVERLAST_HUD_POSITION = BUILDER
            .comment("OverLast HUD position: top left, top right, middle left, middle right, bottom left, bottom right.")
            .defineInList("overlastHudPosition", "top left", Arrays.asList(
                    "top left", "top right", "middle left", "middle right", "bottom left", "bottom right"));
    private static final ModConfigSpec.ConfigValue<List<? extends String>> COTH_VICTIM_PARASITES = BUILDER
            .comment("Victim entity id to parasite entity id mappings, formatted as victim;parasite.")
            .defineList("cothVictimParasites", List.of(
                    "minecraft:pig;csrp:sim_pig",
                    "minecraft:sheep;csrp:sim_sheep",
                    "minecraft:cow;csrp:sim_cow",
                    "minecraft:wolf;csrp:sim_wolf",
                    "minecraft:horse;csrp:sim_horse",
                    "minecraft:zombie;csrp:sim_human",
                    "minecraft:husk;csrp:sim_human",
                    "minecraft:zombie_villager;csrp:sim_villager",
                    "minecraft:villager;csrp:sim_villager",
                    "minecraft:polar_bear;csrp:sim_bear",
                    "minecraft:enderman;csrp:sim_enderman",
                    "minecraft:squid;csrp:sim_squid",
                    "wyrmsofnyrus:creepedhumanoid;csrp:sim_human",
                    "wyrmsofnyrus:creepedbiter;csrp:sim_cow",
                    "wyrmsofnyrus:crawler;csrp:sim_bigspider"),
                    value -> value instanceof String && ((String) value).split(";", -1).length == 2);
    private static final ModConfigSpec.IntValue COLONY_EXTRA_HEALTH_POINT = BUILDER
            .defineInRange("colonyExtraHealthPoint", 20, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue COLONY_EXTRA_HEALTH_VALUE = BUILDER
            .defineInRange("colonyExtraHealthValue", 0.1D, 0.0D, 100.0D);
    private static final ModConfigSpec.IntValue COLONY_EXTRA_ARMOR_POINT = BUILDER
            .defineInRange("colonyExtraArmorPoint", 20, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue COLONY_EXTRA_ARMOR_VALUE = BUILDER
            .defineInRange("colonyExtraArmorValue", 0.1D, 0.0D, 100.0D);
    private static final ModConfigSpec.IntValue COLONY_EXTRA_DAMAGE_POINT = BUILDER
            .defineInRange("colonyExtraDamagePoint", 20, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue COLONY_EXTRA_DAMAGE_VALUE = BUILDER
            .defineInRange("colonyExtraDamageValue", 0.1D, 0.0D, 100.0D);
    private static final ModConfigSpec.IntValue COLONY_EXTRA_KD_POINT = BUILDER
            .defineInRange("colonyExtraKDResPoint", 20, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue COLONY_EXTRA_KD_VALUE = BUILDER
            .defineInRange("colonyExtraKDResValue", 0.1D, 0.0D, 100.0D);
    private static final ModConfigSpec.IntValue COLONY_DAMAGE_CAP_POINT = BUILDER
            .defineInRange("colonyDamageCapPoint", 15, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue COLONY_DAMAGE_CAP_VALUE = BUILDER
            .defineInRange("colonyDamageCapValue", 0.5D, 0.0D, 100.0D);
    private static final ModConfigSpec.IntValue COLONY_POINT_CAP = BUILDER
            .defineInRange("colonyPointCap", 100, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue COLONY_TOTAL_POINT_CAP = BUILDER
            .defineInRange("colonyTotalPointCap", 100000, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.BooleanValue USE_DISLODGMENT = BUILDER
            .comment("Enable the original parasite dislodgment system.")
            .define("useDislodgment", true);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_DEATH_TRIGGER_CHANCE = BUILDER
            .comment("Chance for a parasite death to activate an eligible dislodgment code.")
            .defineInRange("dislodgmentDeathTriggerChance", 0.001D, 0.0D, 1.0D);
    private static final ModConfigSpec.IntValue DISLODGMENT_GLOBAL_COOLDOWN = BUILDER
            .comment("Global dislodgment trigger cooldown in ticks.")
            .defineInRange("dislodgmentGlobalCooldown", 200, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLODGMENT_COTH_SPY = BUILDER
            .comment("Nearby COTH carriers required for player-action dislodgment triggers.")
            .defineInRange("dislodgmentCothSpy", 4, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_ONE_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseOneCodes");
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_TWO_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseTwoCodes");
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_THREE_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseThreeCodes");
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_FOUR_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseFourCodes");
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_FIVE_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseFiveCodes");
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_SIX_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseSixCodes");
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_SEVEN_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseSevenCodes");
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_EIGHT_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseEightCodes");
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_NINE_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseNineCodes");
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_TEN_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseTenCodes");
    private static final ModConfigSpec.DoubleValue DISLODGMENT_RIGHT_CLICK_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentRightClickTriggerChance", 0.01D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_XP_PICKUP_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentXpPickupTriggerChance", 0.03D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_ITEM_PICKUP_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentItemPickupTriggerChance", 0.03D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_HEALING_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentHealingTriggerChance", 0.001D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_USE_ITEM_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentUseItemTriggerChance", 0.01D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_MENU_CLOSE_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentMenuCloseTriggerChance", 0.001D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_BLOCK_BREAK_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentBlockBreakTriggerChance", 0.1D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_NEXUS_ONE_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentNexusOneTriggerChance", 0.05D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_NEXUS_TWO_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentNexusTwoTriggerChance", 0.06D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_NEXUS_THREE_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentNexusThreeTriggerChance", 0.07D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DISLODGMENT_NEXUS_FOUR_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentNexusFourTriggerChance", 0.1D, 0.0D, 1.0D);
    private static final ModConfigSpec.BooleanValue DISLO_COTH_IGNORE_AMPLIFIER = BUILDER
            .define("disloCothIgnoreAmplifier", true);
    private static final ModConfigSpec.IntValue DISLO_COTH_IGNORE_AMPLIFIER_POINT_COST = BUILDER
            .defineInRange("disloCothIgnoreAmplifierPointCost", 100, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_COTH_IGNORE_AMPLIFIER_DURATION = BUILDER
            .defineInRange("disloCothIgnoreAmplifierDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_COTH_IGNORE_AMPLIFIER_COOLDOWN = BUILDER
            .defineInRange("disloCothIgnoreAmplifierCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_COTH_IGNORE_AMPLIFIER_TRIGGERS =
            dislodgmentTriggers("disloCothIgnoreAmplifierTriggers", List.of(1, 10, 14, 16));
    private static final ModConfigSpec.BooleanValue DISLO_COTH_TIERS = BUILDER
            .define("disloCothTiers", true);
    private static final ModConfigSpec.IntValue DISLO_COTH_TIERS_POINT_COST = BUILDER
            .defineInRange("disloCothTiersPointCost", 200, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_COTH_TIERS_VALUE = BUILDER
            .defineInRange("disloCothTiersValue", 1, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_COTH_TIERS_DURATION = BUILDER
            .defineInRange("disloCothTiersDuration", 40, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_COTH_TIERS_COOLDOWN = BUILDER
            .defineInRange("disloCothTiersCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_COTH_TIERS_PRIMITIVE = BUILDER
            .defineInRange("disloCothTiersPrimitive", 9, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_COTH_TIERS_ADAPTED = BUILDER
            .defineInRange("disloCothTiersAdapted", 15, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_COTH_TIERS_PURE = BUILDER
            .defineInRange("disloCothTiersPure", 21, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_COTH_TIERS_TRIGGERS =
            dislodgmentTriggers("disloCothTiersTriggers", List.of(12, 13, 14, 15, 16));
    private static final ModConfigSpec.BooleanValue DISLO_SUMMON_BY_DEATH = BUILDER
            .define("disloSummonByDeath", true);
    private static final ModConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_POINT_COST = BUILDER
            .defineInRange("disloSummonByDeathPointCost", 200, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_VALUE = BUILDER
            .defineInRange("disloSummonByDeathValue", 1, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_DURATION = BUILDER
            .defineInRange("disloSummonByDeathDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_COOLDOWN = BUILDER
            .defineInRange("disloSummonByDeathCooldown", 200, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_KILLING = BUILDER
            .defineInRange("disloSummonByDeathKilling", 5, 0, 255);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DISLO_SUMMON_BY_DEATH_MOBS = BUILDER
            .comment("Dislodgment 2 payload table formatted as minimum accumulated health;entity id.")
            .defineList("disloSummonByDeathMobs", List.of(
                    "1;csrp:sim_enderman",
                    "50;csrp:fer_enderman",
                    "100;csrp:warden"),
                    value -> value instanceof String && ((String) value).split(";", -1).length == 2);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_SUMMON_BY_DEATH_TRIGGERS =
            dislodgmentTriggers("disloSummonByDeathTriggers", List.of(10, 15, 16));
    private static final ModConfigSpec.BooleanValue DISLO_POTION_EFFECT = BUILDER
            .define("disloPotionEffect", true);
    private static final ModConfigSpec.IntValue DISLO_POTION_EFFECT_POINT_COST = BUILDER
            .defineInRange("disloPotionEffectPointCost", 200, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_POTION_EFFECT_VALUE = BUILDER
            .defineInRange("disloPotionEffectValue", 1, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_POTION_EFFECT_DURATION = BUILDER
            .defineInRange("disloPotionEffectDuration", 120, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_POTION_EFFECT_COOLDOWN = BUILDER
            .defineInRange("disloPotionEffectCooldown", 300, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DISLO_POTION_EFFECTS = BUILDER
            .defineList("disloPotionEffects", List.of(
                    "minecraft:speed", "minecraft:fire_resistance", "minecraft:invisibility"),
                    value -> value instanceof String && ResourceLocation.tryParse((String) value) != null);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_POTION_EFFECT_TRIGGERS =
            dislodgmentTriggers("disloPotionEffectTriggers", List.of(4, 13, 14, 15, 16));
    private static final ModConfigSpec.BooleanValue DISLO_STATS = BUILDER.define("disloStats", true);
    private static final ModConfigSpec.IntValue DISLO_STATS_POINT_COST = BUILDER
            .defineInRange("disloStatsPointCost", 1000, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_STATS_VALUE = BUILDER
            .defineInRange("disloStatsValue", 2, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_STATS_DURATION = BUILDER
            .defineInRange("disloStatsDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_STATS_COOLDOWN = BUILDER
            .defineInRange("disloStatsCooldown", 300, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_STATS_TRIGGERS =
            dislodgmentTriggers("disloStatsTriggers", List.of(14, 15, 17, 18));
    private static final ModConfigSpec.BooleanValue DISLO_DEATH_RAID = BUILDER.define("disloDeathRaid", true);
    private static final ModConfigSpec.IntValue DISLO_DEATH_RAID_POINT_COST = BUILDER
            .defineInRange("disloDeathRaidPointCost", 10, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DEATH_RAID_VALUE = BUILDER
            .defineInRange("disloDeathRaidValue", 10, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DEATH_RAID_DURATION = BUILDER
            .defineInRange("disloDeathRaidDuration", 10, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DEATH_RAID_COOLDOWN = BUILDER
            .defineInRange("disloDeathRaidCooldown", 10, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_DEATH_RAID_TRIGGERS =
            dislodgmentTriggers("disloDeathRaidTriggers", List.of(0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 16, 17, 18));
    private static final ModConfigSpec.BooleanValue DISLO_ITEM_DURABILITY = BUILDER.define("disloItemDurability", true);
    private static final ModConfigSpec.IntValue DISLO_ITEM_DURABILITY_POINT_COST = BUILDER
            .defineInRange("disloItemDurabilityPointCost", 100, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_ITEM_DURABILITY_VALUE = BUILDER
            .defineInRange("disloItemDurabilityValue", 2, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_ITEM_DURABILITY_DURATION = BUILDER
            .defineInRange("disloItemDurabilityDuration", 120, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_ITEM_DURABILITY_COOLDOWN = BUILDER
            .defineInRange("disloItemDurabilityCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_ITEM_DURABILITY_TRIGGERS =
            dislodgmentTriggers("disloItemDurabilityTriggers", List.of(4, 12, 13, 16));
    private static final ModConfigSpec.BooleanValue DISLO_HEALING_DEATH = BUILDER
            .define("disloHealingDeath", true);
    private static final ModConfigSpec.IntValue DISLO_HEALING_DEATH_POINT_COST = BUILDER
            .defineInRange("disloHealingDeathPointCost", 500, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_HEALING_DEATH_VALUE = BUILDER
            .defineInRange("disloHealingDeathValue", 100, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_HEALING_DEATH_DURATION = BUILDER
            .defineInRange("disloHealingDeathDuration", 40, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_HEALING_DEATH_COOLDOWN = BUILDER
            .defineInRange("disloHealingDeathCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_HEALING_DEATH_TRIGGERS =
            dislodgmentTriggers("disloHealingDeathTriggers", List.of(1, 3, 10, 12, 16));
    private static final ModConfigSpec.BooleanValue DISLO_DAMAGE_DEATH = BUILDER
            .define("disloDamageDeath", true);
    private static final ModConfigSpec.IntValue DISLO_DAMAGE_DEATH_POINT_COST = BUILDER
            .defineInRange("disloDamageDeathPointCost", 500, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DAMAGE_DEATH_VALUE = BUILDER
            .defineInRange("disloDamageDeathValue", 10, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DAMAGE_DEATH_DURATION = BUILDER
            .defineInRange("disloDamageDeathDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DAMAGE_DEATH_COOLDOWN = BUILDER
            .defineInRange("disloDamageDeathCooldown", 300, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_DAMAGE_DEATH_TRIGGERS =
            dislodgmentTriggers("disloDamageDeathTriggers", List.of(0, 5, 13, 16));
    private static final ModConfigSpec.BooleanValue DISLO_FOOD_DEATH = BUILDER
            .define("disloFoodDeath", true);
    private static final ModConfigSpec.IntValue DISLO_FOOD_DEATH_POINT_COST = BUILDER
            .defineInRange("disloFoodDeathPointCost", 500, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_FOOD_DEATH_VALUE = BUILDER
            .defineInRange("disloFoodDeathValue", 100, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_FOOD_DEATH_DURATION = BUILDER
            .defineInRange("disloFoodDeathDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_FOOD_DEATH_COOLDOWN = BUILDER
            .defineInRange("disloFoodDeathCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_FOOD_DEATH_TRIGGERS =
            dislodgmentTriggers("disloFoodDeathTriggers", List.of(3, 12, 13, 16));
    private static final ModConfigSpec.BooleanValue DISLO_DEATH_HIGH_VERSIONS = BUILDER
            .define("disloDeathHighVersions", true);
    private static final ModConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_POINT_COST = BUILDER
            .defineInRange("disloDeathHighVersionsPointCost", 300, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_VALUE = BUILDER
            .defineInRange("disloDeathHighVersionsValue", 1, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_ADAPTED = BUILDER
            .defineInRange("disloDeathHighVersionsAdapted", 12, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_PURE = BUILDER
            .defineInRange("disloDeathHighVersionsPure", 21, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_DURATION = BUILDER
            .defineInRange("disloDeathHighVersionsDuration", 120, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_COOLDOWN = BUILDER
            .defineInRange("disloDeathHighVersionsCooldown", 360, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue DISLO_DEATH_HIGH_VERSIONS_CHANCE = BUILDER
            .defineInRange("disloDeathHighVersionsChance", 0.5D, 0.0D, 1.0D);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_DEATH_HIGH_VERSIONS_TRIGGERS =
            dislodgmentTriggers("disloDeathHighVersionsTriggers",
                    List.of(0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 16, 17, 18));
    private static final ModConfigSpec.BooleanValue DISLO_PARASITE_NO_POTION = BUILDER
            .define("disloParasiteNoPotion", true);
    private static final ModConfigSpec.IntValue DISLO_PARASITE_NO_POTION_POINT_COST = BUILDER
            .defineInRange("disloParasiteNoPotionPointCost", 100, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_PARASITE_NO_POTION_DURATION = BUILDER
            .defineInRange("disloParasiteNoPotionDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_PARASITE_NO_POTION_COOLDOWN = BUILDER
            .defineInRange("disloParasiteNoPotionCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_PARASITE_NO_POTION_TRIGGERS =
            dislodgmentTriggers("disloParasiteNoPotionTriggers", List.of(3, 4, 16));
    private static final ModConfigSpec.BooleanValue DISLO_HEALTH_DRAINING = BUILDER
            .define("disloHealthDraining", true);
    private static final ModConfigSpec.IntValue DISLO_HEALTH_DRAINING_POINT_COST = BUILDER
            .defineInRange("disloHealthDrainingPointCost", 50000, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_HEALTH_DRAINING_VALUE = BUILDER
            .defineInRange("disloHealthDrainingValue", 10, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_HEALTH_DRAINING_DURATION = BUILDER
            .defineInRange("disloHealthDrainingDuration", 3, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_HEALTH_DRAINING_COOLDOWN = BUILDER
            .defineInRange("disloHealthDrainingCooldown", 300, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_HEALTH_DRAINING_TRIGGERS =
            dislodgmentTriggers("disloHealthDrainingTriggers", List.of(14, 15, 17, 18));
    private static final ModConfigSpec.BooleanValue DISLO_FOOD_DRAINING = BUILDER
            .define("disloFoodDraining", true);
    private static final ModConfigSpec.IntValue DISLO_FOOD_DRAINING_POINT_COST = BUILDER
            .defineInRange("disloFoodDrainingPointCost", 500, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_FOOD_DRAINING_VALUE = BUILDER
            .defineInRange("disloFoodDrainingValue", 200, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_FOOD_DRAINING_DURATION = BUILDER
            .defineInRange("disloFoodDrainingDuration", 3, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_FOOD_DRAINING_COOLDOWN = BUILDER
            .defineInRange("disloFoodDrainingCooldown", 300, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_FOOD_DRAINING_TRIGGERS =
            dislodgmentTriggers("disloFoodDrainingTriggers", List.of(12, 13, 14, 15, 17, 18));
    private static final ModConfigSpec.BooleanValue DISLO_NEXT_PHASE_LIST = BUILDER
            .define("disloNextPhaseList", true);
    private static final ModConfigSpec.IntValue DISLO_NEXT_PHASE_LIST_POINT_COST = BUILDER
            .defineInRange("disloNextPhaseListPointCost", 50000, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_NEXT_PHASE_LIST_VALUE = BUILDER
            .defineInRange("disloNextPhaseListValue", 1, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_NEXT_PHASE_LIST_DURATION = BUILDER
            .defineInRange("disloNextPhaseListDuration", 30, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_NEXT_PHASE_LIST_COOLDOWN = BUILDER
            .defineInRange("disloNextPhaseListCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_NEXT_PHASE_LIST_TRIGGERS =
            dislodgmentTriggers("disloNextPhaseListTriggers", List.of(15, 16, 17, 18));
    private static final ModConfigSpec.BooleanValue DISLO_GROWL_NOISE = BUILDER
            .define("disloGrowlNoise", true);
    private static final ModConfigSpec.IntValue DISLO_GROWL_NOISE_POINT_COST = BUILDER
            .defineInRange("disloGrowlNoisePointCost", 100, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_GROWL_NOISE_DURATION = BUILDER
            .defineInRange("disloGrowlNoiseDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_GROWL_NOISE_COOLDOWN = BUILDER
            .defineInRange("disloGrowlNoiseCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_GROWL_NOISE_TRIGGERS =
            dislodgmentTriggers("disloGrowlNoiseTriggers", List.of(0, 4, 10, 11));
    private static final ModConfigSpec.BooleanValue DISLO_WALK_NOISE = BUILDER
            .define("disloWalkNoise", true);
    private static final ModConfigSpec.IntValue DISLO_WALK_NOISE_POINT_COST = BUILDER
            .defineInRange("disloWalkNoisePointCost", 100, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_WALK_NOISE_DURATION = BUILDER
            .defineInRange("disloWalkNoiseDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_WALK_NOISE_COOLDOWN = BUILDER
            .defineInRange("disloWalkNoiseCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_WALK_NOISE_TRIGGERS =
            dislodgmentTriggers("disloWalkNoiseTriggers", List.of(0, 5, 10, 11));
    private static final ModConfigSpec.BooleanValue DISLO_SHIELD_FOOD = BUILDER
            .define("disloShieldFood", true);
    private static final ModConfigSpec.IntValue DISLO_SHIELD_FOOD_POINT_COST = BUILDER
            .defineInRange("disloShieldFoodPointCost", 180, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_SHIELD_FOOD_DURATION = BUILDER
            .defineInRange("disloShieldFoodDuration", 400, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_SHIELD_FOOD_COOLDOWN = BUILDER
            .defineInRange("disloShieldFoodCooldown", 550, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_SHIELD_FOOD_TRIGGERS =
            dislodgmentTriggers("disloShieldFoodTriggers",
                    List.of(0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 16, 17, 18));
    private static final ModConfigSpec.BooleanValue DISLO_LOOT_XP_CANCEL = BUILDER
            .define("disloLootXpCancel", true);
    private static final ModConfigSpec.IntValue DISLO_LOOT_XP_CANCEL_POINT_COST = BUILDER
            .defineInRange("disloLootXpCancelPointCost", 100, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_LOOT_XP_CANCEL_DURATION = BUILDER
            .defineInRange("disloLootXpCancelDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_LOOT_XP_CANCEL_COOLDOWN = BUILDER
            .defineInRange("disloLootXpCancelCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_LOOT_XP_CANCEL_TRIGGERS =
            dislodgmentTriggers("disloLootXpCancelTriggers", List.of(2, 10, 16));
    private static final ModConfigSpec.BooleanValue DISLO_BURNING_DEATH = BUILDER
            .define("disloBurningDeath", true);
    private static final ModConfigSpec.IntValue DISLO_BURNING_DEATH_POINT_COST = BUILDER
            .defineInRange("disloBurningDeathPointCost", 50000, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_BURNING_DEATH_DURATION = BUILDER
            .defineInRange("disloBurningDeathDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_BURNING_DEATH_COOLDOWN = BUILDER
            .defineInRange("disloBurningDeathCooldown", 180, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> DISLO_BURNING_DEATH_TRIGGERS =
            dislodgmentTriggers("disloBurningDeathTriggers",
                    List.of(0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 16, 17, 18));

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    private static ModConfigSpec.ConfigValue<List<? extends Integer>> dislodgmentTriggers(
            String name, List<Integer> defaults) {
        return BUILDER.defineList(name, defaults,
                value -> value instanceof Integer trigger && trigger >= 0 && trigger <= 18);
    }

    private static ModConfigSpec.ConfigValue<List<? extends Integer>> dislodgmentPhaseCodes(String name) {
        return BUILDER.defineList(name, DEFAULT_DISLODGMENT_PHASE_CODES,
                value -> value instanceof Integer code && code >= 0 && code <= 29);
    }

    public static int evolutionPhase() {
        return EVOLUTION_PHASE.get();
    }

    public static int evolutionPhase(Level level) {
        return level instanceof ServerLevel serverLevel
                ? SrpWorldData.get(serverLevel).evolutionPhase()
                : evolutionPhase();
    }

    public static double variantSpawnChance() { return VARIANT_SPAWN_CHANCE.get(); }
    public static int alwaysVariantPhase() { return ALWAYS_VARIANT_PHASE.get(); }

    public static double adaptationChance() {
        return ADAPTATION_CHANCE.get();
    }

    public static double parasiteKillingReduction() { return PARASITE_KILLING_REDUCTION.get(); }

    public static double cothConvert() { return COTH_CONVERT.get(); }
    public static double killcountPlus() { return KILLCOUNT_PLUS.get(); }
    public static boolean useEvolutionPhases() { return USE_EVOLUTION_PHASES.get(); }
    public static double overlastNaturalEvolutionScale() { return OVERLAST_NATURAL_EVOLUTION_SCALE.get(); }
    public static boolean overlastHudRequiresClock() { return OVERLAST_HUD_REQUIRES_CLOCK.get(); }
    public static String overlastHudPosition() { return OVERLAST_HUD_POSITION.get(); }
    public static List<? extends String> cothVictimParasites() { return COTH_VICTIM_PARASITES.get(); }

    public static int colonyExtraHealthPoint() { return COLONY_EXTRA_HEALTH_POINT.get(); }
    public static double colonyExtraHealthValue() { return COLONY_EXTRA_HEALTH_VALUE.get(); }
    public static int colonyExtraArmorPoint() { return COLONY_EXTRA_ARMOR_POINT.get(); }
    public static double colonyExtraArmorValue() { return COLONY_EXTRA_ARMOR_VALUE.get(); }
    public static int colonyExtraDamagePoint() { return COLONY_EXTRA_DAMAGE_POINT.get(); }
    public static double colonyExtraDamageValue() { return COLONY_EXTRA_DAMAGE_VALUE.get(); }
    public static int colonyExtraKDPoint() { return COLONY_EXTRA_KD_POINT.get(); }
    public static double colonyExtraKDValue() { return COLONY_EXTRA_KD_VALUE.get(); }
    public static int colonyDamageCapPoint() { return COLONY_DAMAGE_CAP_POINT.get(); }
    public static double colonyDamageCapValue() { return COLONY_DAMAGE_CAP_VALUE.get(); }
    public static int colonyPointCap() { return COLONY_POINT_CAP.get(); }
    public static int colonyTotalPointCap() { return COLONY_TOTAL_POINT_CAP.get(); }
    public static boolean useDislodgment() { return USE_DISLODGMENT.get(); }
    public static double dislodgmentDeathTriggerChance() { return DISLODGMENT_DEATH_TRIGGER_CHANCE.get(); }
    public static int dislodgmentGlobalCooldown() { return DISLODGMENT_GLOBAL_COOLDOWN.get(); }
    public static int dislodgmentCothSpy() { return DISLODGMENT_COTH_SPY.get(); }
    public static List<? extends Integer> dislodgmentPhaseCodes(int phase) {
        return switch (phase) {
            case 1 -> DISLODGMENT_PHASE_ONE_CODES.get();
            case 2 -> DISLODGMENT_PHASE_TWO_CODES.get();
            case 3 -> DISLODGMENT_PHASE_THREE_CODES.get();
            case 4 -> DISLODGMENT_PHASE_FOUR_CODES.get();
            case 5 -> DISLODGMENT_PHASE_FIVE_CODES.get();
            case 6 -> DISLODGMENT_PHASE_SIX_CODES.get();
            case 7 -> DISLODGMENT_PHASE_SEVEN_CODES.get();
            case 8 -> DISLODGMENT_PHASE_EIGHT_CODES.get();
            case 9 -> DISLODGMENT_PHASE_NINE_CODES.get();
            case 10 -> DISLODGMENT_PHASE_TEN_CODES.get();
            default -> List.of();
        };
    }
    public static double dislodgmentRightClickTriggerChance() { return DISLODGMENT_RIGHT_CLICK_TRIGGER_CHANCE.get(); }
    public static double dislodgmentXpPickupTriggerChance() { return DISLODGMENT_XP_PICKUP_TRIGGER_CHANCE.get(); }
    public static double dislodgmentItemPickupTriggerChance() { return DISLODGMENT_ITEM_PICKUP_TRIGGER_CHANCE.get(); }
    public static double dislodgmentHealingTriggerChance() { return DISLODGMENT_HEALING_TRIGGER_CHANCE.get(); }
    public static double dislodgmentUseItemTriggerChance() { return DISLODGMENT_USE_ITEM_TRIGGER_CHANCE.get(); }
    public static double dislodgmentMenuCloseTriggerChance() { return DISLODGMENT_MENU_CLOSE_TRIGGER_CHANCE.get(); }
    public static double dislodgmentBlockBreakTriggerChance() { return DISLODGMENT_BLOCK_BREAK_TRIGGER_CHANCE.get(); }
    public static double dislodgmentNexusTriggerChance(int stage) {
        return switch (stage) {
            case 1 -> DISLODGMENT_NEXUS_ONE_TRIGGER_CHANCE.get();
            case 2 -> DISLODGMENT_NEXUS_TWO_TRIGGER_CHANCE.get();
            case 3 -> DISLODGMENT_NEXUS_THREE_TRIGGER_CHANCE.get();
            case 4 -> DISLODGMENT_NEXUS_FOUR_TRIGGER_CHANCE.get();
            default -> 0.0D;
        };
    }
    public static boolean disloCothIgnoreAmplifier() { return DISLO_COTH_IGNORE_AMPLIFIER.get(); }
    public static int disloCothIgnoreAmplifierPointCost() { return DISLO_COTH_IGNORE_AMPLIFIER_POINT_COST.get(); }
    public static int disloCothIgnoreAmplifierDuration() { return DISLO_COTH_IGNORE_AMPLIFIER_DURATION.get(); }
    public static boolean disloCothTiers() { return DISLO_COTH_TIERS.get(); }
    public static int disloCothTiersPointCost() { return DISLO_COTH_TIERS_POINT_COST.get(); }
    public static int disloCothTiersValue() { return DISLO_COTH_TIERS_VALUE.get(); }
    public static int disloCothTiersDuration() { return DISLO_COTH_TIERS_DURATION.get(); }
    public static int disloCothTiersPrimitive() { return DISLO_COTH_TIERS_PRIMITIVE.get(); }
    public static int disloCothTiersAdapted() { return DISLO_COTH_TIERS_ADAPTED.get(); }
    public static int disloCothTiersPure() { return DISLO_COTH_TIERS_PURE.get(); }
    public static boolean disloSummonByDeath() { return DISLO_SUMMON_BY_DEATH.get(); }
    public static int disloSummonByDeathPointCost() { return DISLO_SUMMON_BY_DEATH_POINT_COST.get(); }
    public static int disloSummonByDeathValue() { return DISLO_SUMMON_BY_DEATH_VALUE.get(); }
    public static int disloSummonByDeathDuration() { return DISLO_SUMMON_BY_DEATH_DURATION.get(); }
    public static int disloSummonByDeathKilling() { return DISLO_SUMMON_BY_DEATH_KILLING.get(); }
    public static List<? extends String> disloSummonByDeathMobs() { return DISLO_SUMMON_BY_DEATH_MOBS.get(); }
    public static boolean disloPotionEffect() { return DISLO_POTION_EFFECT.get(); }
    public static int disloPotionEffectPointCost() { return DISLO_POTION_EFFECT_POINT_COST.get(); }
    public static int disloPotionEffectValue() { return DISLO_POTION_EFFECT_VALUE.get(); }
    public static int disloPotionEffectDuration() { return DISLO_POTION_EFFECT_DURATION.get(); }
    public static List<? extends String> disloPotionEffects() { return DISLO_POTION_EFFECTS.get(); }
    public static boolean disloStats() { return DISLO_STATS.get(); }
    public static int disloStatsPointCost() { return DISLO_STATS_POINT_COST.get(); }
    public static int disloStatsValue() { return DISLO_STATS_VALUE.get(); }
    public static int disloStatsDuration() { return DISLO_STATS_DURATION.get(); }
    public static boolean disloDeathRaid() { return DISLO_DEATH_RAID.get(); }
    public static int disloDeathRaidPointCost() { return DISLO_DEATH_RAID_POINT_COST.get(); }
    public static int disloDeathRaidValue() { return DISLO_DEATH_RAID_VALUE.get(); }
    public static int disloDeathRaidDuration() { return DISLO_DEATH_RAID_DURATION.get(); }
    public static boolean disloItemDurability() { return DISLO_ITEM_DURABILITY.get(); }
    public static int disloItemDurabilityPointCost() { return DISLO_ITEM_DURABILITY_POINT_COST.get(); }
    public static int disloItemDurabilityValue() { return DISLO_ITEM_DURABILITY_VALUE.get(); }
    public static int disloItemDurabilityDuration() { return DISLO_ITEM_DURABILITY_DURATION.get(); }
    public static boolean disloHealingDeath() { return DISLO_HEALING_DEATH.get(); }
    public static int disloHealingDeathPointCost() { return DISLO_HEALING_DEATH_POINT_COST.get(); }
    public static int disloHealingDeathValue() { return DISLO_HEALING_DEATH_VALUE.get(); }
    public static int disloHealingDeathDuration() { return DISLO_HEALING_DEATH_DURATION.get(); }
    public static boolean disloDamageDeath() { return DISLO_DAMAGE_DEATH.get(); }
    public static int disloDamageDeathPointCost() { return DISLO_DAMAGE_DEATH_POINT_COST.get(); }
    public static int disloDamageDeathValue() { return DISLO_DAMAGE_DEATH_VALUE.get(); }
    public static int disloDamageDeathDuration() { return DISLO_DAMAGE_DEATH_DURATION.get(); }
    public static boolean disloFoodDeath() { return DISLO_FOOD_DEATH.get(); }
    public static int disloFoodDeathPointCost() { return DISLO_FOOD_DEATH_POINT_COST.get(); }
    public static int disloFoodDeathValue() { return DISLO_FOOD_DEATH_VALUE.get(); }
    public static int disloFoodDeathDuration() { return DISLO_FOOD_DEATH_DURATION.get(); }
    public static boolean disloDeathHighVersions() { return DISLO_DEATH_HIGH_VERSIONS.get(); }
    public static int disloDeathHighVersionsPointCost() { return DISLO_DEATH_HIGH_VERSIONS_POINT_COST.get(); }
    public static int disloDeathHighVersionsValue() { return DISLO_DEATH_HIGH_VERSIONS_VALUE.get(); }
    public static int disloDeathHighVersionsAdapted() { return DISLO_DEATH_HIGH_VERSIONS_ADAPTED.get(); }
    public static int disloDeathHighVersionsPure() { return DISLO_DEATH_HIGH_VERSIONS_PURE.get(); }
    public static int disloDeathHighVersionsDuration() { return DISLO_DEATH_HIGH_VERSIONS_DURATION.get(); }
    public static double disloDeathHighVersionsChance() { return DISLO_DEATH_HIGH_VERSIONS_CHANCE.get(); }
    public static boolean disloParasiteNoPotion() { return DISLO_PARASITE_NO_POTION.get(); }
    public static int disloParasiteNoPotionPointCost() { return DISLO_PARASITE_NO_POTION_POINT_COST.get(); }
    public static int disloParasiteNoPotionDuration() { return DISLO_PARASITE_NO_POTION_DURATION.get(); }
    public static boolean disloHealthDraining() { return DISLO_HEALTH_DRAINING.get(); }
    public static int disloHealthDrainingPointCost() { return DISLO_HEALTH_DRAINING_POINT_COST.get(); }
    public static int disloHealthDrainingValue() { return DISLO_HEALTH_DRAINING_VALUE.get(); }
    public static int disloHealthDrainingDuration() { return DISLO_HEALTH_DRAINING_DURATION.get(); }
    public static boolean disloFoodDraining() { return DISLO_FOOD_DRAINING.get(); }
    public static int disloFoodDrainingPointCost() { return DISLO_FOOD_DRAINING_POINT_COST.get(); }
    public static int disloFoodDrainingValue() { return DISLO_FOOD_DRAINING_VALUE.get(); }
    public static int disloFoodDrainingDuration() { return DISLO_FOOD_DRAINING_DURATION.get(); }
    public static boolean disloNextPhaseList() { return DISLO_NEXT_PHASE_LIST.get(); }
    public static int disloNextPhaseListPointCost() { return DISLO_NEXT_PHASE_LIST_POINT_COST.get(); }
    public static int disloNextPhaseListValue() { return DISLO_NEXT_PHASE_LIST_VALUE.get(); }
    public static int disloNextPhaseListDuration() { return DISLO_NEXT_PHASE_LIST_DURATION.get(); }
    public static boolean disloGrowlNoise() { return DISLO_GROWL_NOISE.get(); }
    public static int disloGrowlNoisePointCost() { return DISLO_GROWL_NOISE_POINT_COST.get(); }
    public static int disloGrowlNoiseDuration() { return DISLO_GROWL_NOISE_DURATION.get(); }
    public static boolean disloWalkNoise() { return DISLO_WALK_NOISE.get(); }
    public static int disloWalkNoisePointCost() { return DISLO_WALK_NOISE_POINT_COST.get(); }
    public static int disloWalkNoiseDuration() { return DISLO_WALK_NOISE_DURATION.get(); }
    public static boolean disloShieldFood() { return DISLO_SHIELD_FOOD.get(); }
    public static int disloShieldFoodPointCost() { return DISLO_SHIELD_FOOD_POINT_COST.get(); }
    public static int disloShieldFoodDuration() { return DISLO_SHIELD_FOOD_DURATION.get(); }
    public static boolean disloLootXpCancel() { return DISLO_LOOT_XP_CANCEL.get(); }
    public static int disloLootXpCancelPointCost() { return DISLO_LOOT_XP_CANCEL_POINT_COST.get(); }
    public static int disloLootXpCancelDuration() { return DISLO_LOOT_XP_CANCEL_DURATION.get(); }
    public static boolean disloBurningDeath() { return DISLO_BURNING_DEATH.get(); }
    public static int disloBurningDeathPointCost() { return DISLO_BURNING_DEATH_POINT_COST.get(); }
    public static int disloBurningDeathDuration() { return DISLO_BURNING_DEATH_DURATION.get(); }

    public static List<? extends Integer> dislodgmentTriggers(int code) {
        return switch (code) {
            case 0 -> DISLO_COTH_IGNORE_AMPLIFIER_TRIGGERS.get();
            case 1 -> DISLO_COTH_TIERS_TRIGGERS.get();
            case 2 -> DISLO_SUMMON_BY_DEATH_TRIGGERS.get();
            case 3 -> DISLO_POTION_EFFECT_TRIGGERS.get();
            case 4 -> DISLO_STATS_TRIGGERS.get();
            case 5 -> DISLO_DEATH_RAID_TRIGGERS.get();
            case 6 -> DISLO_ITEM_DURABILITY_TRIGGERS.get();
            case 7 -> DISLO_HEALING_DEATH_TRIGGERS.get();
            case 8 -> DISLO_DAMAGE_DEATH_TRIGGERS.get();
            case 9 -> DISLO_FOOD_DEATH_TRIGGERS.get();
            case 10 -> DISLO_DEATH_HIGH_VERSIONS_TRIGGERS.get();
            case 11 -> DISLO_PARASITE_NO_POTION_TRIGGERS.get();
            case 12 -> DISLO_HEALTH_DRAINING_TRIGGERS.get();
            case 13 -> DISLO_FOOD_DRAINING_TRIGGERS.get();
            case 14 -> DISLO_NEXT_PHASE_LIST_TRIGGERS.get();
            case 15 -> DISLO_GROWL_NOISE_TRIGGERS.get();
            case 16 -> DISLO_WALK_NOISE_TRIGGERS.get();
            case 17 -> DISLO_SHIELD_FOOD_TRIGGERS.get();
            case 18 -> DISLO_LOOT_XP_CANCEL_TRIGGERS.get();
            case 21 -> DISLO_BURNING_DEATH_TRIGGERS.get();
            default -> List.of();
        };
    }

    public static int dislodgmentCodeCooldown(int code) {
        return switch (code) {
            case 0 -> DISLO_COTH_IGNORE_AMPLIFIER_COOLDOWN.get();
            case 1 -> DISLO_COTH_TIERS_COOLDOWN.get();
            case 2 -> DISLO_SUMMON_BY_DEATH_COOLDOWN.get();
            case 3 -> DISLO_POTION_EFFECT_COOLDOWN.get();
            case 4 -> DISLO_STATS_COOLDOWN.get();
            case 5 -> DISLO_DEATH_RAID_COOLDOWN.get();
            case 6 -> DISLO_ITEM_DURABILITY_COOLDOWN.get();
            case 7 -> DISLO_HEALING_DEATH_COOLDOWN.get();
            case 8 -> DISLO_DAMAGE_DEATH_COOLDOWN.get();
            case 9 -> DISLO_FOOD_DEATH_COOLDOWN.get();
            case 10 -> DISLO_DEATH_HIGH_VERSIONS_COOLDOWN.get();
            case 11 -> DISLO_PARASITE_NO_POTION_COOLDOWN.get();
            case 12 -> DISLO_HEALTH_DRAINING_COOLDOWN.get();
            case 13 -> DISLO_FOOD_DRAINING_COOLDOWN.get();
            case 14 -> DISLO_NEXT_PHASE_LIST_COOLDOWN.get();
            case 15 -> DISLO_GROWL_NOISE_COOLDOWN.get();
            case 16 -> DISLO_WALK_NOISE_COOLDOWN.get();
            case 17 -> DISLO_SHIELD_FOOD_COOLDOWN.get();
            case 18 -> DISLO_LOOT_XP_CANCEL_COOLDOWN.get();
            case 21 -> DISLO_BURNING_DEATH_COOLDOWN.get();
            default -> 0;
        };
    }
}
