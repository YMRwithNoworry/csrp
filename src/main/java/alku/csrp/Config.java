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
    private static final ModConfigSpec.BooleanValue SCENT_ENABLED = BUILDER
            .comment("Allow parasitic Scent entities to spawn.")
            .define("scentEnabled", true);
    private static final ModConfigSpec.IntValue SCENT_CAP = BUILDER
            .comment("Maximum Scent count checked before a Seeker creates another Scent.")
            .defineInRange("scentCap", 2, 1, 100);
    private static final ModConfigSpec.IntValue SCENT_DEVELOPMENT_LEVEL = BUILDER
            .comment("Minimum creation-phase development level at which a Seeker can create Scent.")
            .defineInRange("scentDevelopmentLevel", 2, 0, 100);
    private static final ModConfigSpec.BooleanValue RAGE_ENABLED = BUILDER
            .comment("Allow parasites to grant the Rage effect.")
            .define("rageEnabled", true);
    private static final ModConfigSpec.BooleanValue DERIVED_TEXT_DISTORTION_ENABLED = BUILDER
            .comment("Show the obfuscated/garbled text effect caused by nearby Kirin and Draconite.")
            .define("derivedTextDistortionEnabled", false);
    private static final ModConfigSpec.BooleanValue MOB_ATTACKING_ENABLED = BUILDER
            .comment("Allow parasites to target non-player mobs.")
            .define("mobAttackingEnabled", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_ATTACKING_BLACKLIST = BUILDER
            .comment("Entity ids or whole namespaces that parasites will not target as mobs.")
            .defineList("mobAttackingBlacklist", List.of(
                    "srmonstress", "minecraft:creeper", "minecraft:bat"),
                    value -> value instanceof String entry && !entry.isBlank());
    private static final ModConfigSpec.BooleanValue MOB_ATTACKING_BLACKLIST_INVERTED = BUILDER
            .comment("Treat the mob-attacking blacklist as a whitelist.")
            .define("mobAttackingBlacklistInverted", false);
    private static final ModConfigSpec.BooleanValue COLLECTIVE_CONSCIOUSNESS_ENABLED = BUILDER
            .comment("Allow parasite target acquisition without direct line of sight.")
            .define("collectiveConsciousnessEnabled", true);
    private static final ModConfigSpec.IntValue WORLD_GNAT_CAP = BUILDER
            .comment("Maximum loaded Gnat or Lice count before a Vermin drops a bomb instead.")
            .defineInRange("worldGnatCap", 20, 0, 50000);
    private static final ModConfigSpec.DoubleValue VARIANT_SPAWN_CHANCE = BUILDER
            .comment("Chance for a parasite with an available variant to spawn as that variant.")
            .defineInRange("variantSpawnChance", 0.33D, 0.0D, 1.0D);
    private static final ModConfigSpec.IntValue ALWAYS_VARIANT_PHASE = BUILDER
            .comment("From this evolution phase onward, parasites always use an available variant.")
            .defineInRange("alwaysVariantPhase", 11, -1, 11);
    private static final ModConfigSpec.DoubleValue TENDRIL_HEALTH = BUILDER
            .comment("Fraction of a parent parasite's maximum health assigned to each detachable tendril.")
            .defineInRange("tendrilHealth", 0.5D, 0.5D, 100.0D);
    private static final ModConfigSpec.IntValue PURE_POINT_DAMAGE_CAP = BUILDER
            .comment("Legacy resistance points removed when a pure parasite body part is destroyed.")
            .defineInRange("purePointDamageCap", 12, 0, 1000);
    private static final ModConfigSpec.DoubleValue ADAPTATION_CHANCE = BUILDER
            .comment("Chance for a linked parasite outside a colony to share its adaptation on death.")
            .defineInRange("adaptationChance", 0.1D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue PARASITE_KILLING_REDUCTION = BUILDER
            .comment("Damage reduction per level of the matching parasite-killing status effect.")
            .defineInRange("parasiteKillingReduction", 0.15D, 0.0D, 0.95D);
    private static final ModConfigSpec.DoubleValue KILLCOUNT_PLUS = BUILDER
            .comment("Killcount added every second on HARD or HARDCORE when evolution phases are disabled.")
            .defineInRange("killcountPlus", 0.0D, 0.0D, 1000000.0D);
    private static final ModConfigSpec.DoubleValue PRIMITIVE_MINIMUM_DAMAGE = BUILDER
            .comment("Armor-bypassing minimum damage dealt by primitive parasite special attacks.")
            .defineInRange("primitiveMinimumDamage", 2.0D, 0.0D, 1000.0D);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> PARASITE_COMBAT_TABLE = BUILDER
            .comment("Per-tier legacy combat values (SRPConfig.<tier>Cap / <tier>MinDamage / foodSteal)",
                    "format: tier;damageCapDivisor;minimumDamage;foodSteal",
                    "tiers: infected, feral, hijacked, assimara, primitive, adapted, pure, preeminent,",
                    "ancient, derived, nexus_si, nexus_sii, nexus_siii, nexus_siv")
            .defineList("parasiteCombatTable", List.of(
                    "infected;2;0.5;0.1",
                    "feral;3;0.75;0.5",
                    "hijacked;5;1.3;0.1",
                    "assimara;5;1.1;0.5",
                    "primitive;6;2.0;0.5",
                    "adapted;9;4.0;0.5",
                    "pure;13;7.0;0.5",
                    "preeminent;18;10.0;0.5",
                    "ancient;5;2.5;0.5",
                    "derived;25;14.0;0.5",
                    "nexus_si;4;0.0;0.5",
                    "nexus_sii;8;0.0;0.5",
                    "nexus_siii;14;0.0;0.5",
                    "nexus_siv;20;0.0;0.5"),
                    Config::validCombatTableEntry);
    private static final ModConfigSpec.DoubleValue PARASITE_POISON_HEALING = BUILDER
            .comment("Health a parasite regains when poison damage is converted into healing"
                    + " (legacy genePoisonHealing).")
            .defineInRange("parasitePoisonHealing", 2.5D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue PARASITE_FOOD_THEFT_CHANCE = BUILDER
            .comment("Chance for a parasite hit to convert one of the victim's food items into"
                    + " assimilated flesh (legacy foodRott).")
            .defineInRange("parasiteFoodTheftChance", 0.1D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue PARASITE_FIRE_MULTIPLIER = BUILDER
            .comment("Fire damage multiplier applied to parasites (legacy firemultyplier).")
            .defineInRange("parasiteFireMultiplier", 4.0D, 1.0D, 100.0D);
    private static final ModConfigSpec.BooleanValue PARASITE_GORE_ENABLED = BUILDER
            .comment("Let parasites leave gore blocks and Remains behind (legacy paraGore).")
            .define("parasiteGore", true);
    private static final ModConfigSpec.IntValue PARASITE_REMAIN_VALUE = BUILDER
            .comment("Remain life points per unit: the original rebuilt a Remain with"
                    + " 20 * value ticks (SRPConfig.infectedRemainValue).")
            .defineInRange("parasiteRemainValue", 10, 1, 50000);
    private static final ModConfigSpec.DoubleValue PARASITE_SELF_EXPLODE_CHANCE = BUILDER
            .comment("Chance for a dying parasite to burst into a toxic cloud and gore"
                    + " (legacy dyingBurst / selfExplode).")
            .defineInRange("parasiteSelfExplodeChance", 0.5D, 0.0D, 1.0D);
    private static final ModConfigSpec.IntValue EVOLUTION_STAT_INCREASE_PHASE = BUILDER
            .comment("Phase from which parasites spawn with the legacy stat bonus"
                    + " (SRPConfigSystems.evolutionParasiteStatIncrease = 10).")
            .defineInRange("evolutionStatIncreasePhase", 10, 0, 100);
    private static final ModConfigSpec.DoubleValue EVOLUTION_STAT_INCREASE_VALUE = BUILDER
            .comment("Fraction added to max health, armor and attack damage"
                    + " (SRPConfigSystems.evolutionParasiteStatIncreaseValue = 0.07).")
            .defineInRange("evolutionStatIncreaseValue", 0.07D, 0.0D, 10.0D);
    private static final ModConfigSpec.IntValue SPAWN_DAYS = BUILDER
            .comment("Ticks the world must have run before parasites may spawn naturally"
                    + " (SRPConfig.spawnDays = 0; the legacy name says days but the unit is ticks).")
            .defineInRange("spawnDays", 0, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue EVOLUTION_SPAWN_IGNORE_SUNLIGHT = BUILDER
            .comment("Phase from which parasites use the looser light-level spawn check"
                    + " (SRPConfigSystems.evolutionSpawningIgnoreSunlight = 1).")
            .defineInRange("evolutionSpawningIgnoreSunlight", 1, -1, 100);
    private static final ModConfigSpec.BooleanValue PHASE_LIGHTLESS_MINUS_ONE = BUILDER
            .comment("Treat phase -1 parasites as lightless, i.e. use the looser light check"
                    + " (SRPConfigSystems.phaseLightlessMinusOne = true).")
            .define("phaseLightlessMinusOne", true);
    private static final ModConfigSpec.BooleanValue IGNORE_LIGHT = BUILDER
            .comment("Legacy SRPConfig.ignoreL (false): when evolution phases are disabled, use the"
                    + " looser light-level check for natural spawns.")
            .define("ignoreL", false);
    private static final ModConfigSpec.DoubleValue PARASITE_REGEN = BUILDER
            .comment("Health a parasite regains per regeneration tick while it has killcount left"
                    + " (legacy primitiveRegen, consumed one killcount per 5 heals).")
            .defineInRange("parasiteRegen", 4.0D, 0.0D, 1000.0D);
    private static final ModConfigSpec.BooleanValue USE_EVOLUTION_PHASES = BUILDER
            .comment("Use SRP evolution phases instead of the legacy difficulty killcount behavior.")
            .define("useEvolutionPhases", true);
    private static final ModConfigSpec.BooleanValue GENERATION_ENABLED = BUILDER
            .comment("Use parasite generations. When disabled, parasites retain their full gene abilities.")
            .define("generationEnabled", true);
    private static final ModConfigSpec.BooleanValue PEARL_DESTROYED_ON_BEHOLDER_KILL = BUILDER
            .comment("Destroy dropped Eyes of the Beholder when their owner is slain by a beholder.")
            .define("pearlDestroyedOnBeholderKill", true);
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
    private static final ModConfigSpec.ConfigValue<List<? extends String>> COTH_IMMUNE_ENTITIES = BUILDER
            .comment("Entity ids or whole namespaces whose normal COTH effect cannot advance or create an Incomplete Form.")
            .defineList("cothImmuneEntities", List.of(
                    "minecraft:iron_golem",
                    "minecraft:vex",
                    "minecraft:creeper",
                    "minecraft:slime",
                    "minecraft:blaze",
                    "minecraft:guardian",
                    "minecraft:elder_guardian",
                    "minecraft:stray",
                    "minecraft:skeleton",
                    "minecraft:skeleton_horse",
                    "minecraft:wither_skeleton",
                    "minecraft:magma_cube",
                    "minecraft:ghast",
                    "minecraft:shulker",
                    "minecraft:snow_golem",
                    "wyrmsofnyrus",
                    "srrevenants"),
                    value -> value instanceof String entry && !entry.isBlank());
    private static final ModConfigSpec.BooleanValue COTH_IMMUNE_LIST_INVERTED = BUILDER
            .comment("Treat cothImmuneEntities as a whitelist of entities that are not immune.")
            .define("cothImmuneListInverted", false);
    private static final ModConfigSpec.DoubleValue COTH_CONVERT_AT_KILL_CHANCE = BUILDER
            .comment("Base chance for a parasite kill to convert a COTH I victim. Higher COTH levels increase it.")
            .defineInRange("cothConvertAtKillChance", 0.3D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue COTH_ASSIMILATED_SPREAD_CHANCE = BUILDER
            .comment("Chance for an Assimilated parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothAssimilatedSpreadChance", 0.1D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue COTH_HIJACKED_SPREAD_CHANCE = BUILDER
            .comment("Chance for a Hijacked parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothHijackedSpreadChance", 0.05D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue COTH_FERAL_SPREAD_CHANCE = BUILDER
            .comment("Chance for a Feral parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothFeralSpreadChance", 0.2D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue COTH_CRUDE_SPREAD_CHANCE = BUILDER
            .comment("Chance for a Crude parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothCrudeSpreadChance", 0.4D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue COTH_PRIMITIVE_SPREAD_CHANCE = BUILDER
            .comment("Chance for a Primitive parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothPrimitiveSpreadChance", 0.5D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue COTH_ADAPTED_SPREAD_CHANCE = BUILDER
            .comment("Chance for an Adapted parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothAdaptedSpreadChance", 0.6D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue COTH_PURE_SPREAD_CHANCE = BUILDER
            .comment("Chance for a Pure or Preeminent parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothPureSpreadChance", 0.8D, 0.0D, 1.0D);
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
    private static final ModConfigSpec.IntValue COLONY_MAXIMUM_NUMBER = BUILDER
            .defineInRange("colonyMaximumNumber", 20, 1, 100);
    private static final ModConfigSpec.IntValue COLONY_MINIMUM_DISTANCE = BUILDER
            .defineInRange("colonyMinimumDistance", 2000, 1, 100000);
    private static final ModConfigSpec.IntValue COLONY_SPREAD_POINT = BUILDER
            .defineInRange("colonySpreadPoint", 2, 1, 10000);
    private static final ModConfigSpec.IntValue COLONY_SPREAD_VALUE = BUILDER
            .defineInRange("colonySpreadValue", 20, 1, 10000);
    private static final ModConfigSpec.IntValue COLONY_BASE_RADIUS = BUILDER
            .defineInRange("colonyBaseRadius", 120, 1, 10000);
    private static final ModConfigSpec.IntValue COLONY_EFFECT_SPREAD_POINT = BUILDER
            .defineInRange("colonyEffectSpreadPoint", 1, 1, 10000);
    private static final ModConfigSpec.IntValue COLONY_EFFECT_SPREAD_VALUE = BUILDER
            .defineInRange("colonyEffectSpreadValue", 40, 1, 10000);
    private static final ModConfigSpec.IntValue COLONY_BASE_EFFECT_RADIUS = BUILDER
            .defineInRange("colonyBaseEffectRadius", 300, 1, 10000);
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

    // Original SRPConfigWorld meteor-infection event settings.
    private static final ModConfigSpec.BooleanValue METEOR_ENABLED = BUILDER
            .comment("Allow the periodic SRParasites meteor infection world event.")
            .define("meteorEnabled", false);
    private static final ModConfigSpec.IntValue METEOR_CHECK_TICKS = BUILDER
            .comment("Ticks between meteor spawn checks (original default: 3600).")
            .defineInRange("meteorCheckTicks", 3600, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue METEOR_CHANCE = BUILDER
            .comment("Chance that a meteor check launches a meteor.")
            .defineInRange("meteorChance", 0.5D, 0.0D, 1.0D);
    private static final ModConfigSpec.IntValue METEOR_START_TICKS = BUILDER
            .comment("World age in ticks before periodic meteor infection may begin.")
            .defineInRange("meteorStartTicks", 0, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue METEOR_DAMAGE_RADIUS = BUILDER
            .comment("Radius of the root meteor's distance-scaled impact damage.")
            .defineInRange("meteorDamageRadius", 110, 1, 2048);
    private static final ModConfigSpec.IntValue METEOR_MAX_RADIUS = BUILDER
            .comment("Maximum horizontal launch/target offset from the selected player.")
            .defineInRange("meteorRadius", 120, 2, 2048);
    private static final ModConfigSpec.IntValue METEOR_MIN_RADIUS = BUILDER
            .comment("Minimum horizontal launch/target offset from the selected player.")
            .defineInRange("meteorMinimumRadius", 80, 1, 2047);
    private static final ModConfigSpec.BooleanValue METEOR_VECTORLESS = BUILDER
            .comment("Only spawn periodic meteors while the dimension has no infestation vector.")
            .define("meteorVectorless", true);
    private static final ModConfigSpec.BooleanValue METEOR_CREATES_VECTOR = BUILDER
            .comment("Create an Emerging Infestation Vector at a root meteor impact.")
            .define("meteorCreatesVector", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> METEOR_DIMENSION_BLACKLIST = BUILDER
            .comment("Dimension ids where periodic meteor infection is disabled.")
            .defineList("meteorDimensionBlacklist", List.of("minecraft:the_nether"),
                    value -> value instanceof String entry && ResourceLocation.tryParse(entry) != null);


    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    private static ModConfigSpec.ConfigValue<List<? extends Integer>> dislodgmentTriggers(
            String name, List<Integer> defaults) {
        return BUILDER.defineList(name, defaults,
                value -> value instanceof Integer trigger && trigger >= 0 && trigger <= 18);
    }

    /** Validates a parasiteCombatTable entry: tier;damageCapDivisor;minimumDamage;foodSteal. */
    private static boolean validCombatTableEntry(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }
        String[] parts = entry.split(";", -1);
        if (parts.length != 4 || parts[0].isBlank()) {
            return false;
        }
        try {
            return Integer.parseInt(parts[1].trim()) >= 1
                    && Float.parseFloat(parts[2].trim()) >= 0.0F
                    && Float.parseFloat(parts[3].trim()) >= 0.0F;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static ModConfigSpec.ConfigValue<List<? extends Integer>> dislodgmentPhaseCodes(String name) {
        return BUILDER.defineList(name, DEFAULT_DISLODGMENT_PHASE_CODES,
                value -> value instanceof Integer code && code >= 0 && code <= 29);
    }

    public static int evolutionPhase() {
        return safe(EVOLUTION_PHASE);
    }

    public static int evolutionPhase(Level level) {
        return level instanceof ServerLevel serverLevel
                ? SrpWorldData.get(serverLevel).evolutionPhase()
                : evolutionPhase();
    }

    public static boolean scentEnabled() { return safe(SCENT_ENABLED); }
    public static int scentCap() { return safe(SCENT_CAP); }
    public static int scentDevelopmentLevel() { return safe(SCENT_DEVELOPMENT_LEVEL); }
    public static boolean rageEnabled() { return safe(RAGE_ENABLED); }
    public static boolean mobAttackingEnabled() { return safe(MOB_ATTACKING_ENABLED); }
    public static boolean derivedTextDistortionEnabled() { return safe(DERIVED_TEXT_DISTORTION_ENABLED); }
    public static List<? extends String> mobAttackingBlacklist() { return safe(MOB_ATTACKING_BLACKLIST); }
    public static boolean mobAttackingBlacklistInverted() { return safe(MOB_ATTACKING_BLACKLIST_INVERTED); }
    public static boolean collectiveConsciousnessEnabled() { return safe(COLLECTIVE_CONSCIOUSNESS_ENABLED); }
    public static int worldGnatCap() { return safe(WORLD_GNAT_CAP); }

    public static double variantSpawnChance() { return safe(VARIANT_SPAWN_CHANCE); }
    public static int alwaysVariantPhase() { return safe(ALWAYS_VARIANT_PHASE); }
    public static double tendrilHealth() { return safe(TENDRIL_HEALTH); }
    public static int purePointDamageCap() { return safe(PURE_POINT_DAMAGE_CAP); }

    public static double adaptationChance() {
        return safe(ADAPTATION_CHANCE);
    }

    public static double parasiteKillingReduction() { return safe(PARASITE_KILLING_REDUCTION); }

    public static double killcountPlus() { return safe(KILLCOUNT_PLUS); }
    public static float primitiveMinimumDamage() { return safe(PRIMITIVE_MINIMUM_DAMAGE).floatValue(); }
    public static List<? extends String> parasiteCombatTable() { return safe(PARASITE_COMBAT_TABLE); }
    public static float parasitePoisonHealing() { return safe(PARASITE_POISON_HEALING).floatValue(); }
    public static float parasiteFoodTheftChance() { return safe(PARASITE_FOOD_THEFT_CHANCE).floatValue(); }
    public static float parasiteFireMultiplier() { return safe(PARASITE_FIRE_MULTIPLIER).floatValue(); }
    public static boolean parasiteGoreEnabled() { return safe(PARASITE_GORE_ENABLED); }
    public static int parasiteRemainValue() { return safe(PARASITE_REMAIN_VALUE); }
    public static double parasiteSelfExplodeChance() { return safe(PARASITE_SELF_EXPLODE_CHANCE); }
    public static float parasiteRegen() { return safe(PARASITE_REGEN).floatValue(); }
    public static int evolutionStatIncreasePhase() { return safe(EVOLUTION_STAT_INCREASE_PHASE); }
    public static double evolutionStatIncreaseValue() { return safe(EVOLUTION_STAT_INCREASE_VALUE); }
    public static int spawnDays() { return safe(SPAWN_DAYS); }
    public static int evolutionSpawningIgnoreSunlight() { return safe(EVOLUTION_SPAWN_IGNORE_SUNLIGHT); }
    public static boolean phaseLightlessMinusOne() { return safe(PHASE_LIGHTLESS_MINUS_ONE); }
    public static boolean ignoreLightLevel() { return safe(IGNORE_LIGHT); }
    public static boolean useEvolutionPhases() { return safe(USE_EVOLUTION_PHASES); }
    public static boolean generationEnabled() { return safe(GENERATION_ENABLED); }
    public static boolean pearlDestroyedOnBeholderKill() { return safe(PEARL_DESTROYED_ON_BEHOLDER_KILL); }
    public static double overlastNaturalEvolutionScale() { return safe(OVERLAST_NATURAL_EVOLUTION_SCALE); }
    public static boolean overlastHudRequiresClock() { return safe(OVERLAST_HUD_REQUIRES_CLOCK); }
    public static String overlastHudPosition() { return safe(OVERLAST_HUD_POSITION); }
    public static List<? extends String> cothVictimParasites() { return safe(COTH_VICTIM_PARASITES); }
    public static List<? extends String> cothImmuneEntities() { return safe(COTH_IMMUNE_ENTITIES); }
    public static boolean cothImmuneListInverted() { return safe(COTH_IMMUNE_LIST_INVERTED); }
    public static double cothConvertAtKillChance() { return safe(COTH_CONVERT_AT_KILL_CHANCE); }
    public static double cothAssimilatedSpreadChance() { return safe(COTH_ASSIMILATED_SPREAD_CHANCE); }
    public static double cothHijackedSpreadChance() { return safe(COTH_HIJACKED_SPREAD_CHANCE); }
    public static double cothFeralSpreadChance() { return safe(COTH_FERAL_SPREAD_CHANCE); }
    public static double cothCrudeSpreadChance() { return safe(COTH_CRUDE_SPREAD_CHANCE); }
    public static double cothPrimitiveSpreadChance() { return safe(COTH_PRIMITIVE_SPREAD_CHANCE); }
    public static double cothAdaptedSpreadChance() { return safe(COTH_ADAPTED_SPREAD_CHANCE); }
    public static double cothPureSpreadChance() { return safe(COTH_PURE_SPREAD_CHANCE); }

    public static int colonyExtraHealthPoint() { return safe(COLONY_EXTRA_HEALTH_POINT); }
    public static double colonyExtraHealthValue() { return safe(COLONY_EXTRA_HEALTH_VALUE); }
    public static int colonyExtraArmorPoint() { return safe(COLONY_EXTRA_ARMOR_POINT); }
    public static double colonyExtraArmorValue() { return safe(COLONY_EXTRA_ARMOR_VALUE); }
    public static int colonyExtraDamagePoint() { return safe(COLONY_EXTRA_DAMAGE_POINT); }
    public static double colonyExtraDamageValue() { return safe(COLONY_EXTRA_DAMAGE_VALUE); }
    public static int colonyExtraKDPoint() { return safe(COLONY_EXTRA_KD_POINT); }
    public static double colonyExtraKDValue() { return safe(COLONY_EXTRA_KD_VALUE); }
    public static int colonyDamageCapPoint() { return safe(COLONY_DAMAGE_CAP_POINT); }
    public static double colonyDamageCapValue() { return safe(COLONY_DAMAGE_CAP_VALUE); }
    public static int colonyPointCap() { return safe(COLONY_POINT_CAP); }
    public static int colonyTotalPointCap() { return safe(COLONY_TOTAL_POINT_CAP); }
    public static int colonyMaximumNumber() { return safe(COLONY_MAXIMUM_NUMBER); }
    public static int colonyMinimumDistance() { return safe(COLONY_MINIMUM_DISTANCE); }
    public static int colonySpreadPoint() { return safe(COLONY_SPREAD_POINT); }
    public static int colonySpreadValue() { return safe(COLONY_SPREAD_VALUE); }
    public static int colonyBaseRadius() { return safe(COLONY_BASE_RADIUS); }
    public static int colonyEffectSpreadPoint() { return safe(COLONY_EFFECT_SPREAD_POINT); }
    public static int colonyEffectSpreadValue() { return safe(COLONY_EFFECT_SPREAD_VALUE); }
    public static int colonyBaseEffectRadius() { return safe(COLONY_BASE_EFFECT_RADIUS); }
    public static boolean useDislodgment() { return safe(USE_DISLODGMENT); }
    public static double dislodgmentDeathTriggerChance() { return safe(DISLODGMENT_DEATH_TRIGGER_CHANCE); }
    public static int dislodgmentGlobalCooldown() { return safe(DISLODGMENT_GLOBAL_COOLDOWN); }
    public static int dislodgmentCothSpy() { return safe(DISLODGMENT_COTH_SPY); }
    public static List<? extends Integer> dislodgmentPhaseCodes(int phase) {
        return switch (phase) {
            case 1 -> safe(DISLODGMENT_PHASE_ONE_CODES);
            case 2 -> safe(DISLODGMENT_PHASE_TWO_CODES);
            case 3 -> safe(DISLODGMENT_PHASE_THREE_CODES);
            case 4 -> safe(DISLODGMENT_PHASE_FOUR_CODES);
            case 5 -> safe(DISLODGMENT_PHASE_FIVE_CODES);
            case 6 -> safe(DISLODGMENT_PHASE_SIX_CODES);
            case 7 -> safe(DISLODGMENT_PHASE_SEVEN_CODES);
            case 8 -> safe(DISLODGMENT_PHASE_EIGHT_CODES);
            case 9 -> safe(DISLODGMENT_PHASE_NINE_CODES);
            case 10 -> safe(DISLODGMENT_PHASE_TEN_CODES);
            default -> List.of();
        };
    }
    public static double dislodgmentRightClickTriggerChance() { return safe(DISLODGMENT_RIGHT_CLICK_TRIGGER_CHANCE); }
    public static double dislodgmentXpPickupTriggerChance() { return safe(DISLODGMENT_XP_PICKUP_TRIGGER_CHANCE); }
    public static double dislodgmentItemPickupTriggerChance() { return safe(DISLODGMENT_ITEM_PICKUP_TRIGGER_CHANCE); }
    public static double dislodgmentHealingTriggerChance() { return safe(DISLODGMENT_HEALING_TRIGGER_CHANCE); }
    public static double dislodgmentUseItemTriggerChance() { return safe(DISLODGMENT_USE_ITEM_TRIGGER_CHANCE); }
    public static double dislodgmentMenuCloseTriggerChance() { return safe(DISLODGMENT_MENU_CLOSE_TRIGGER_CHANCE); }
    public static double dislodgmentBlockBreakTriggerChance() { return safe(DISLODGMENT_BLOCK_BREAK_TRIGGER_CHANCE); }
    public static double dislodgmentNexusTriggerChance(int stage) {
        return switch (stage) {
            case 1 -> safe(DISLODGMENT_NEXUS_ONE_TRIGGER_CHANCE);
            case 2 -> safe(DISLODGMENT_NEXUS_TWO_TRIGGER_CHANCE);
            case 3 -> safe(DISLODGMENT_NEXUS_THREE_TRIGGER_CHANCE);
            case 4 -> safe(DISLODGMENT_NEXUS_FOUR_TRIGGER_CHANCE);
            default -> 0.0D;
        };
    }
    public static boolean disloCothIgnoreAmplifier() { return safe(DISLO_COTH_IGNORE_AMPLIFIER); }
    public static int disloCothIgnoreAmplifierPointCost() { return safe(DISLO_COTH_IGNORE_AMPLIFIER_POINT_COST); }
    public static int disloCothIgnoreAmplifierDuration() { return safe(DISLO_COTH_IGNORE_AMPLIFIER_DURATION); }
    public static boolean disloCothTiers() { return safe(DISLO_COTH_TIERS); }
    public static int disloCothTiersPointCost() { return safe(DISLO_COTH_TIERS_POINT_COST); }
    public static int disloCothTiersValue() { return safe(DISLO_COTH_TIERS_VALUE); }
    public static int disloCothTiersDuration() { return safe(DISLO_COTH_TIERS_DURATION); }
    public static int disloCothTiersPrimitive() { return safe(DISLO_COTH_TIERS_PRIMITIVE); }
    public static int disloCothTiersAdapted() { return safe(DISLO_COTH_TIERS_ADAPTED); }
    public static int disloCothTiersPure() { return safe(DISLO_COTH_TIERS_PURE); }
    public static boolean disloSummonByDeath() { return safe(DISLO_SUMMON_BY_DEATH); }
    public static int disloSummonByDeathPointCost() { return safe(DISLO_SUMMON_BY_DEATH_POINT_COST); }
    public static int disloSummonByDeathValue() { return safe(DISLO_SUMMON_BY_DEATH_VALUE); }
    public static int disloSummonByDeathDuration() { return safe(DISLO_SUMMON_BY_DEATH_DURATION); }
    public static int disloSummonByDeathKilling() { return safe(DISLO_SUMMON_BY_DEATH_KILLING); }
    public static List<? extends String> disloSummonByDeathMobs() { return safe(DISLO_SUMMON_BY_DEATH_MOBS); }
    public static boolean disloPotionEffect() { return safe(DISLO_POTION_EFFECT); }
    public static int disloPotionEffectPointCost() { return safe(DISLO_POTION_EFFECT_POINT_COST); }
    public static int disloPotionEffectValue() { return safe(DISLO_POTION_EFFECT_VALUE); }
    public static int disloPotionEffectDuration() { return safe(DISLO_POTION_EFFECT_DURATION); }
    public static List<? extends String> disloPotionEffects() { return safe(DISLO_POTION_EFFECTS); }
    public static boolean disloStats() { return safe(DISLO_STATS); }
    public static int disloStatsPointCost() { return safe(DISLO_STATS_POINT_COST); }
    public static int disloStatsValue() { return safe(DISLO_STATS_VALUE); }
    public static int disloStatsDuration() { return safe(DISLO_STATS_DURATION); }
    public static boolean disloDeathRaid() { return safe(DISLO_DEATH_RAID); }
    public static int disloDeathRaidPointCost() { return safe(DISLO_DEATH_RAID_POINT_COST); }
    public static int disloDeathRaidValue() { return safe(DISLO_DEATH_RAID_VALUE); }
    public static int disloDeathRaidDuration() { return safe(DISLO_DEATH_RAID_DURATION); }
    public static boolean disloItemDurability() { return safe(DISLO_ITEM_DURABILITY); }
    public static int disloItemDurabilityPointCost() { return safe(DISLO_ITEM_DURABILITY_POINT_COST); }
    public static int disloItemDurabilityValue() { return safe(DISLO_ITEM_DURABILITY_VALUE); }
    public static int disloItemDurabilityDuration() { return safe(DISLO_ITEM_DURABILITY_DURATION); }
    public static boolean disloHealingDeath() { return safe(DISLO_HEALING_DEATH); }
    public static int disloHealingDeathPointCost() { return safe(DISLO_HEALING_DEATH_POINT_COST); }
    public static int disloHealingDeathValue() { return safe(DISLO_HEALING_DEATH_VALUE); }
    public static int disloHealingDeathDuration() { return safe(DISLO_HEALING_DEATH_DURATION); }
    public static boolean disloDamageDeath() { return safe(DISLO_DAMAGE_DEATH); }
    public static int disloDamageDeathPointCost() { return safe(DISLO_DAMAGE_DEATH_POINT_COST); }
    public static int disloDamageDeathValue() { return safe(DISLO_DAMAGE_DEATH_VALUE); }
    public static int disloDamageDeathDuration() { return safe(DISLO_DAMAGE_DEATH_DURATION); }
    public static boolean disloFoodDeath() { return safe(DISLO_FOOD_DEATH); }
    public static int disloFoodDeathPointCost() { return safe(DISLO_FOOD_DEATH_POINT_COST); }
    public static int disloFoodDeathValue() { return safe(DISLO_FOOD_DEATH_VALUE); }
    public static int disloFoodDeathDuration() { return safe(DISLO_FOOD_DEATH_DURATION); }
    public static boolean disloDeathHighVersions() { return safe(DISLO_DEATH_HIGH_VERSIONS); }
    public static int disloDeathHighVersionsPointCost() { return safe(DISLO_DEATH_HIGH_VERSIONS_POINT_COST); }
    public static int disloDeathHighVersionsValue() { return safe(DISLO_DEATH_HIGH_VERSIONS_VALUE); }
    public static int disloDeathHighVersionsAdapted() { return safe(DISLO_DEATH_HIGH_VERSIONS_ADAPTED); }
    public static int disloDeathHighVersionsPure() { return safe(DISLO_DEATH_HIGH_VERSIONS_PURE); }
    public static int disloDeathHighVersionsDuration() { return safe(DISLO_DEATH_HIGH_VERSIONS_DURATION); }
    public static double disloDeathHighVersionsChance() { return safe(DISLO_DEATH_HIGH_VERSIONS_CHANCE); }
    public static boolean disloParasiteNoPotion() { return safe(DISLO_PARASITE_NO_POTION); }
    public static int disloParasiteNoPotionPointCost() { return safe(DISLO_PARASITE_NO_POTION_POINT_COST); }
    public static int disloParasiteNoPotionDuration() { return safe(DISLO_PARASITE_NO_POTION_DURATION); }
    public static boolean disloHealthDraining() { return safe(DISLO_HEALTH_DRAINING); }
    public static int disloHealthDrainingPointCost() { return safe(DISLO_HEALTH_DRAINING_POINT_COST); }
    public static int disloHealthDrainingValue() { return safe(DISLO_HEALTH_DRAINING_VALUE); }
    public static int disloHealthDrainingDuration() { return safe(DISLO_HEALTH_DRAINING_DURATION); }
    public static boolean disloFoodDraining() { return safe(DISLO_FOOD_DRAINING); }
    public static int disloFoodDrainingPointCost() { return safe(DISLO_FOOD_DRAINING_POINT_COST); }
    public static int disloFoodDrainingValue() { return safe(DISLO_FOOD_DRAINING_VALUE); }
    public static int disloFoodDrainingDuration() { return safe(DISLO_FOOD_DRAINING_DURATION); }
    public static boolean disloNextPhaseList() { return safe(DISLO_NEXT_PHASE_LIST); }
    public static int disloNextPhaseListPointCost() { return safe(DISLO_NEXT_PHASE_LIST_POINT_COST); }
    public static int disloNextPhaseListValue() { return safe(DISLO_NEXT_PHASE_LIST_VALUE); }
    public static int disloNextPhaseListDuration() { return safe(DISLO_NEXT_PHASE_LIST_DURATION); }
    public static boolean disloGrowlNoise() { return safe(DISLO_GROWL_NOISE); }
    public static int disloGrowlNoisePointCost() { return safe(DISLO_GROWL_NOISE_POINT_COST); }
    public static int disloGrowlNoiseDuration() { return safe(DISLO_GROWL_NOISE_DURATION); }
    public static boolean disloWalkNoise() { return safe(DISLO_WALK_NOISE); }
    public static int disloWalkNoisePointCost() { return safe(DISLO_WALK_NOISE_POINT_COST); }
    public static int disloWalkNoiseDuration() { return safe(DISLO_WALK_NOISE_DURATION); }
    public static boolean disloShieldFood() { return safe(DISLO_SHIELD_FOOD); }
    public static int disloShieldFoodPointCost() { return safe(DISLO_SHIELD_FOOD_POINT_COST); }
    public static int disloShieldFoodDuration() { return safe(DISLO_SHIELD_FOOD_DURATION); }
    public static boolean disloLootXpCancel() { return safe(DISLO_LOOT_XP_CANCEL); }
    public static int disloLootXpCancelPointCost() { return safe(DISLO_LOOT_XP_CANCEL_POINT_COST); }
    public static int disloLootXpCancelDuration() { return safe(DISLO_LOOT_XP_CANCEL_DURATION); }
    public static boolean disloBurningDeath() { return safe(DISLO_BURNING_DEATH); }
    public static int disloBurningDeathPointCost() { return safe(DISLO_BURNING_DEATH_POINT_COST); }
    public static int disloBurningDeathDuration() { return safe(DISLO_BURNING_DEATH_DURATION); }

    public static List<? extends Integer> dislodgmentTriggers(int code) {
        return switch (code) {
            case 0 -> safe(DISLO_COTH_IGNORE_AMPLIFIER_TRIGGERS);
            case 1 -> safe(DISLO_COTH_TIERS_TRIGGERS);
            case 2 -> safe(DISLO_SUMMON_BY_DEATH_TRIGGERS);
            case 3 -> safe(DISLO_POTION_EFFECT_TRIGGERS);
            case 4 -> safe(DISLO_STATS_TRIGGERS);
            case 5 -> safe(DISLO_DEATH_RAID_TRIGGERS);
            case 6 -> safe(DISLO_ITEM_DURABILITY_TRIGGERS);
            case 7 -> safe(DISLO_HEALING_DEATH_TRIGGERS);
            case 8 -> safe(DISLO_DAMAGE_DEATH_TRIGGERS);
            case 9 -> safe(DISLO_FOOD_DEATH_TRIGGERS);
            case 10 -> safe(DISLO_DEATH_HIGH_VERSIONS_TRIGGERS);
            case 11 -> safe(DISLO_PARASITE_NO_POTION_TRIGGERS);
            case 12 -> safe(DISLO_HEALTH_DRAINING_TRIGGERS);
            case 13 -> safe(DISLO_FOOD_DRAINING_TRIGGERS);
            case 14 -> safe(DISLO_NEXT_PHASE_LIST_TRIGGERS);
            case 15 -> safe(DISLO_GROWL_NOISE_TRIGGERS);
            case 16 -> safe(DISLO_WALK_NOISE_TRIGGERS);
            case 17 -> safe(DISLO_SHIELD_FOOD_TRIGGERS);
            case 18 -> safe(DISLO_LOOT_XP_CANCEL_TRIGGERS);
            case 21 -> safe(DISLO_BURNING_DEATH_TRIGGERS);
            default -> List.of();
        };
    }

    public static int dislodgmentCodeCooldown(int code) {
        return switch (code) {
            case 0 -> safe(DISLO_COTH_IGNORE_AMPLIFIER_COOLDOWN);
            case 1 -> safe(DISLO_COTH_TIERS_COOLDOWN);
            case 2 -> safe(DISLO_SUMMON_BY_DEATH_COOLDOWN);
            case 3 -> safe(DISLO_POTION_EFFECT_COOLDOWN);
            case 4 -> safe(DISLO_STATS_COOLDOWN);
            case 5 -> safe(DISLO_DEATH_RAID_COOLDOWN);
            case 6 -> safe(DISLO_ITEM_DURABILITY_COOLDOWN);
            case 7 -> safe(DISLO_HEALING_DEATH_COOLDOWN);
            case 8 -> safe(DISLO_DAMAGE_DEATH_COOLDOWN);
            case 9 -> safe(DISLO_FOOD_DEATH_COOLDOWN);
            case 10 -> safe(DISLO_DEATH_HIGH_VERSIONS_COOLDOWN);
            case 11 -> safe(DISLO_PARASITE_NO_POTION_COOLDOWN);
            case 12 -> safe(DISLO_HEALTH_DRAINING_COOLDOWN);
            case 13 -> safe(DISLO_FOOD_DRAINING_COOLDOWN);
            case 14 -> safe(DISLO_NEXT_PHASE_LIST_COOLDOWN);
            case 15 -> safe(DISLO_GROWL_NOISE_COOLDOWN);
            case 16 -> safe(DISLO_WALK_NOISE_COOLDOWN);
            case 17 -> safe(DISLO_SHIELD_FOOD_COOLDOWN);
            case 18 -> safe(DISLO_LOOT_XP_CANCEL_COOLDOWN);
            case 21 -> safe(DISLO_BURNING_DEATH_COOLDOWN);
            default -> 0;
        };
    }

    public static boolean meteorEnabled() { return safe(METEOR_ENABLED); }
    public static int meteorCheckTicks() { return safe(METEOR_CHECK_TICKS); }
    public static double meteorChance() { return safe(METEOR_CHANCE); }
    public static int meteorStartTicks() { return safe(METEOR_START_TICKS); }
    public static int meteorDamageRadius() { return safe(METEOR_DAMAGE_RADIUS); }
    public static int meteorRadius() { return safe(METEOR_MAX_RADIUS); }
    public static int meteorMinimumRadius() { return safe(METEOR_MIN_RADIUS); }
    public static boolean meteorVectorless() { return safe(METEOR_VECTORLESS); }
    public static boolean meteorCreatesVector() { return safe(METEOR_CREATES_VECTOR); }
    public static List<? extends String> meteorDimensionBlacklist() { return safe(METEOR_DIMENSION_BLACKLIST); }


    /**
     * Reads a config value, falling back to its declared default while the config file has not been
     * read yet. NeoForge runs EntityAttributeCreationEvent before configs are loaded, and CSRP's
     * createAttributes() methods read config values, so an unguarded get() there crashes startup.
     */
    private static <T> T safe(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
