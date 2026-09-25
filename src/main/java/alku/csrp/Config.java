package alku.csrp;

import alku.csrp.world.SrpWorldData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final List<Integer> DEFAULT_DISLODGMENT_PHASE_CODES = List.of(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25);
    private static final ForgeConfigSpec.IntValue EVOLUTION_PHASE = BUILDER
            .comment("Current parasite evolution phase used by phase-gated spawning and behavior.")
            .defineInRange("evolutionPhase", -1, -2, 10);
    private static final ForgeConfigSpec.BooleanValue SCENT_ENABLED = BUILDER
            .comment("Allow parasitic Scent entities to spawn.")
            .define("scentEnabled", true);
    private static final ForgeConfigSpec.IntValue SCENT_CAP = BUILDER
            .comment("Maximum Scent count checked before a Seeker creates another Scent.")
            .defineInRange("scentCap", 2, 1, 100);
    private static final ForgeConfigSpec.IntValue SCENT_DEVELOPMENT_LEVEL = BUILDER
            .comment("Minimum creation-phase development level at which a Seeker can create Scent.")
            .defineInRange("scentDevelopmentLevel", 2, 0, 100);
    private static final ForgeConfigSpec.BooleanValue RAGE_ENABLED = BUILDER
            .comment("Allow parasites to grant the Rage effect.")
            .define("rageEnabled", true);
    private static final ForgeConfigSpec.BooleanValue MOB_ATTACKING_ENABLED = BUILDER
            .comment("Allow parasites to target non-player mobs.")
            .define("mobAttackingEnabled", true);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_ATTACKING_BLACKLIST = BUILDER
            .comment("Entity ids or whole namespaces that parasites will not target as mobs.")
            .defineList("mobAttackingBlacklist", List.of(
                    "srmonstress", "minecraft:creeper", "minecraft:bat"),
                    value -> value instanceof String entry && !entry.isBlank());
    private static final ForgeConfigSpec.BooleanValue MOB_ATTACKING_BLACKLIST_INVERTED = BUILDER
            .comment("Treat the mob-attacking blacklist as a whitelist.")
            .define("mobAttackingBlacklistInverted", false);
    private static final ForgeConfigSpec.BooleanValue COLLECTIVE_CONSCIOUSNESS_ENABLED = BUILDER
            .comment("Allow parasite target acquisition without direct line of sight.")
            .define("collectiveConsciousnessEnabled", true);
    private static final ForgeConfigSpec.IntValue WORLD_GNAT_CAP = BUILDER
            .comment("Maximum loaded Gnat or Lice count before a Vermin drops a bomb instead.")
            .defineInRange("worldGnatCap", 20, 0, 50000);
    private static final ForgeConfigSpec.DoubleValue VARIANT_SPAWN_CHANCE = BUILDER
            .comment("Chance for a parasite with an available variant to spawn as that variant.")
            .defineInRange("variantSpawnChance", 0.33D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.IntValue ALWAYS_VARIANT_PHASE = BUILDER
            .comment("From this evolution phase onward, parasites always use an available variant.")
            .defineInRange("alwaysVariantPhase", 11, -1, 11);
    private static final ForgeConfigSpec.DoubleValue TENDRIL_HEALTH = BUILDER
            .comment("Fraction of a parent parasite's maximum health assigned to each detachable tendril.")
            .defineInRange("tendrilHealth", 0.5D, 0.5D, 100.0D);
    private static final ForgeConfigSpec.IntValue PURE_POINT_DAMAGE_CAP = BUILDER
            .comment("Legacy resistance points removed when a pure parasite body part is destroyed.")
            .defineInRange("purePointDamageCap", 12, 0, 1000);
    private static final ForgeConfigSpec.DoubleValue ADAPTATION_CHANCE = BUILDER
            .comment("Chance for a linked parasite outside a colony to share its adaptation on death.")
            .defineInRange("adaptationChance", 0.1D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue PARASITE_KILLING_REDUCTION = BUILDER
            .comment("Damage reduction per level of the matching parasite-killing status effect.")
            .defineInRange("parasiteKillingReduction", 0.15D, 0.0D, 0.95D);
    private static final ForgeConfigSpec.DoubleValue KILLCOUNT_PLUS = BUILDER
            .comment("Killcount added every second on HARD or HARDCORE when evolution phases are disabled.")
            .defineInRange("killcountPlus", 0.0D, 0.0D, 1000000.0D);
    private static final ForgeConfigSpec.DoubleValue PRIMITIVE_MINIMUM_DAMAGE = BUILDER
            .comment("Armor-bypassing minimum damage dealt by primitive parasite special attacks.")
            .defineInRange("primitiveMinimumDamage", 2.0D, 0.0D, 1000.0D);
    private static final ForgeConfigSpec.BooleanValue USE_EVOLUTION_PHASES = BUILDER
            .comment("Use SRP evolution phases instead of the legacy difficulty killcount behavior.")
            .define("useEvolutionPhases", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_ENABLED = BUILDER
            .comment("Use parasite generations. When disabled, parasites retain their full gene abilities.")
            .define("generationEnabled", true);
    private static final ForgeConfigSpec.BooleanValue PARASITE_GEN_RESIDUE = BUILDER
            .comment("Allow adapted parasites to place infested remains while idle.")
            .define("parasiteGenResidue", true);

    // ------------------------------------------------------------------
    // Evolution Phases - original SRP "Phase # Delay" options.
    // ------------------------------------------------------------------
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> PHASE_DELAY_SECONDS = BUILDER
            .comment("Point gain lock, in seconds, applied when a dimension enters a new evolution phase",
                    "(original SRP \"Phase # Delay\"). Index 0 is phase 0, index 10 is phase 10; an",
                    "index beyond the end of the list means no lock for that phase.",
                    "Every entry defaults to 0, so reaching a new phase never stops the dimension from",
                    "gaining points - the phase cooldown is disabled unless a value is set here.",
                    "For the original 1.10.9 values use:",
                    "[0, 4000, 4800, 4700, 4500, 4200, 3800, 3700, 3700, 3800, 6000].",
                    "默认全部为 0：阶段冷却关闭，进化到下一阶段后可以立即正常获得点数。")
            .defineList("phaseDelaySeconds", List.of(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                    value -> value instanceof Integer seconds && seconds >= 0);

    // ------------------------------------------------------------------
    // Parasite Generations - original SRP "parasite_generation" category.
    // ------------------------------------------------------------------
    private static final ForgeConfigSpec.IntValue GENERATION_DEFAULT_VALUE = BUILDER
            .comment("Generation value when starting a world (original SRP \"Generation Value\").")
            .defineInRange("generationDefaultValue", 0, 0, 5);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> GENERATION_DIMENSION_STARTING_LIST =
            BUILDER
                    .comment("Dimensions that start at a specific generation, formatted as"
                            + " \"dimension_id;generation\" (original SRP"
                            + " \"Generation Dimension Starting List\").")
                    .defineList("generationDimensionStartingList", List.of(),
                            value -> value instanceof String entry && entry.contains(";")
                                    && entry.indexOf(';') > 0);
    private static final ForgeConfigSpec.IntValue GENERATION_TIME_1 = BUILDER
            .comment("Ticks needed to leave generation 0 (original SRP \"Generation 1 Time Needed\").")
            .defineInRange("generationTime1", 25000, 0, 2147483640);
    private static final ForgeConfigSpec.IntValue GENERATION_TIME_2 = BUILDER
            .comment("Ticks needed to leave generation 1 (original SRP \"Generation 2 Time Needed\").")
            .defineInRange("generationTime2", 45000, 0, 2147483640);
    private static final ForgeConfigSpec.IntValue GENERATION_TIME_3 = BUILDER
            .comment("Ticks needed to leave generation 2 (original SRP \"Generation 3 Time Needed\").")
            .defineInRange("generationTime3", 72000, 0, 2147483640);
    private static final ForgeConfigSpec.IntValue GENERATION_TIME_4 = BUILDER
            .comment("Ticks needed to leave generation 3 (original SRP \"Generation 4 Time Needed\").")
            .defineInRange("generationTime4", 72000, 0, 2147483640);
    private static final ForgeConfigSpec.IntValue GENERATION_TIME_5 = BUILDER
            .comment("Ticks needed to leave generation 4 (original SRP \"Generation 5 Time Needed\").")
            .defineInRange("generationTime5", 72000, 0, 2147483640);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> GENERATION_PHASES_1 =
            BUILDER
                    .comment("Evolution phases without an extra time penalty for generation 0"
                            + " (original SRP \"Generation 1 Phases\").")
                    .defineList("generationPhases1", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                            value -> value instanceof Integer phase && phase >= -2 && phase <= 10);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> GENERATION_PHASES_2 =
            BUILDER
                    .comment("Evolution phases without an extra time penalty for generation 1"
                            + " (original SRP \"Generation 2 Phases\").")
                    .defineList("generationPhases2", List.of(3, 4, 5, 6, 7, 8, 9, 10),
                            value -> value instanceof Integer phase && phase >= -2 && phase <= 10);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> GENERATION_PHASES_3 =
            BUILDER
                    .comment("Evolution phases without an extra time penalty for generation 2"
                            + " (original SRP \"Generation 3 Phases\").")
                    .defineList("generationPhases3", List.of(5, 6, 7, 8, 9, 10),
                            value -> value instanceof Integer phase && phase >= -2 && phase <= 10);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> GENERATION_PHASES_4 =
            BUILDER
                    .comment("Evolution phases without an extra time penalty for generation 3"
                            + " (original SRP \"Generation 4 Phases\").")
                    .defineList("generationPhases4", List.of(7, 8, 9, 10),
                            value -> value instanceof Integer phase && phase >= -2 && phase <= 10);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> GENERATION_PHASES_5 =
            BUILDER
                    .comment("Evolution phases without an extra time penalty for generation 4"
                            + " (original SRP \"Generation 5 Phases\").")
                    .defineList("generationPhases5", List.of(9, 10),
                            value -> value instanceof Integer phase && phase >= -2 && phase <= 10);
    private static final ForgeConfigSpec.DoubleValue GENERATION_PHASE_PENALTY = BUILDER
            .comment("Multiplier applied to the needed time when the evolution phase is outside the generation"
                    + " phase list (original SRP \"Generation 0th\").")
            .defineInRange("generationPhasePenalty", 1.5D, 0.0D, 10.0D);
    // ---- original SRP "parasite_generation_00" ----
    private static final ForgeConfigSpec.DoubleValue GENERATION_0_COTH = BUILDER
            .comment("\"Generation 0 COTH Spawning Stats\" (original SRP).")
            .defineInRange("generation0Coth", 0.2D, 0D, 6D);
    private static final ForgeConfigSpec.BooleanValue GENERATION_0_SPECIAL_MOVES = BUILDER
            .comment("\"Generation 0 Special Moves\" (original SRP).")
            .define("generation0SpecialMoves", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_0_SPRINTING = BUILDER
            .comment("\"Generation 0 Sprinting\" (original SRP).")
            .define("generation0Sprinting", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_0_LOOK_WALLS = BUILDER
            .comment("\"Generation 0 X Ray\" (original SRP).")
            .define("generation0LookWalls", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_0_ADAPTATION = BUILDER
            .comment("\"Generation 0 Adaptation\" (original SRP).")
            .define("generation0Adaptation", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_0_DAMAGE_CAP = BUILDER
            .comment("\"Generation 0 Damage Cap\" (original SRP).")
            .define("generation0DamageCap", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_0_MINIMUM_DAMAGE = BUILDER
            .comment("\"Generation 0 Minimum Damage\" (original SRP).")
            .define("generation0MinimumDamage", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_0_WATER_LEAP = BUILDER
            .comment("\"Generation 0 Water Leap\" (original SRP).")
            .define("generation0WaterLeap", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_0_BLOCK_SEARCH = BUILDER
            .comment("\"Generation 0 Block Searching\" (original SRP).")
            .define("generation0BlockSearch", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_0_RESIDUE = BUILDER
            .comment("\"Generation 0 Residue\" (original SRP).")
            .define("generation0Residue", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_0_ORB = BUILDER
            .comment("\"Generation 0 Orb\" (original SRP).")
            .define("generation0Orb", false);
    private static final ForgeConfigSpec.DoubleValue GENERATION_0_POISON_HEAL = BUILDER
            .comment("\"Generation 0 Poison\" (original SRP).")
            .defineInRange("generation0PoisonHeal", 0D, 0D, 10D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_0_MOB_HEALING = BUILDER
            .comment("\"Generation 0 Mob Healing\" (original SRP).")
            .defineInRange("generation0MobHealing", 0D, 0D, 100D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_0_ATTACK_SPEED = BUILDER
            .comment("\"Generation 0 Attack Speed\" (original SRP).")
            .defineInRange("generation0AttackSpeed", 1D, 0D, 1D);
    // ---- original SRP "parasite_generation_01" ----
    private static final ForgeConfigSpec.DoubleValue GENERATION_1_COTH = BUILDER
            .comment("\"Generation 1 COTH Spawning Stats\" (original SRP).")
            .defineInRange("generation1Coth", 0.3D, 0D, 6D);
    private static final ForgeConfigSpec.BooleanValue GENERATION_1_SPECIAL_MOVES = BUILDER
            .comment("\"Generation 1 Special Moves\" (original SRP).")
            .define("generation1SpecialMoves", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_1_SPRINTING = BUILDER
            .comment("\"Generation 1 Sprinting\" (original SRP).")
            .define("generation1Sprinting", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_1_LOOK_WALLS = BUILDER
            .comment("\"Generation 1 X Ray\" (original SRP).")
            .define("generation1LookWalls", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_1_ADAPTATION = BUILDER
            .comment("\"Generation 1 Adaptation\" (original SRP).")
            .define("generation1Adaptation", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_1_DAMAGE_CAP = BUILDER
            .comment("\"Generation 1 Damage Cap\" (original SRP).")
            .define("generation1DamageCap", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_1_MINIMUM_DAMAGE = BUILDER
            .comment("\"Generation 1 Minimum Damage\" (original SRP).")
            .define("generation1MinimumDamage", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_1_WATER_LEAP = BUILDER
            .comment("\"Generation 1 Water Leap\" (original SRP).")
            .define("generation1WaterLeap", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_1_BLOCK_SEARCH = BUILDER
            .comment("\"Generation 1 Block Searching\" (original SRP).")
            .define("generation1BlockSearch", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_1_RESIDUE = BUILDER
            .comment("\"Generation 1 Residue\" (original SRP).")
            .define("generation1Residue", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_1_ORB = BUILDER
            .comment("\"Generation 1 Orb\" (original SRP).")
            .define("generation1Orb", false);
    private static final ForgeConfigSpec.DoubleValue GENERATION_1_POISON_HEAL = BUILDER
            .comment("\"Generation 1 Poison\" (original SRP).")
            .defineInRange("generation1PoisonHeal", 0.3D, 0D, 10D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_1_MOB_HEALING = BUILDER
            .comment("\"Generation 1 Mob Healing\" (original SRP).")
            .defineInRange("generation1MobHealing", 0D, 0D, 100D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_1_ATTACK_SPEED = BUILDER
            .comment("\"Generation 1 Attack Speed\" (original SRP).")
            .defineInRange("generation1AttackSpeed", 1D, 0D, 1D);
    // ---- original SRP "parasite_generation_02" ----
    private static final ForgeConfigSpec.DoubleValue GENERATION_2_COTH = BUILDER
            .comment("\"Generation 2 COTH Spawning Stats\" (original SRP).")
            .defineInRange("generation2Coth", 0.65D, 0D, 6D);
    private static final ForgeConfigSpec.BooleanValue GENERATION_2_SPECIAL_MOVES = BUILDER
            .comment("\"Generation 2 Special Moves\" (original SRP).")
            .define("generation2SpecialMoves", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_2_SPRINTING = BUILDER
            .comment("\"Generation 2 Sprinting\" (original SRP).")
            .define("generation2Sprinting", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_2_LOOK_WALLS = BUILDER
            .comment("\"Generation 2 X Ray\" (original SRP).")
            .define("generation2LookWalls", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_2_ADAPTATION = BUILDER
            .comment("\"Generation 2 Adaptation\" (original SRP).")
            .define("generation2Adaptation", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_2_DAMAGE_CAP = BUILDER
            .comment("\"Generation 2 Damage Cap\" (original SRP).")
            .define("generation2DamageCap", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_2_MINIMUM_DAMAGE = BUILDER
            .comment("\"Generation 2 Minimum Damage\" (original SRP).")
            .define("generation2MinimumDamage", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_2_WATER_LEAP = BUILDER
            .comment("\"Generation 2 Water Leap\" (original SRP).")
            .define("generation2WaterLeap", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_2_BLOCK_SEARCH = BUILDER
            .comment("\"Generation 2 Block Searching\" (original SRP).")
            .define("generation2BlockSearch", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_2_RESIDUE = BUILDER
            .comment("\"Generation 2 Residue\" (original SRP).")
            .define("generation2Residue", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_2_ORB = BUILDER
            .comment("\"Generation 2 Orb\" (original SRP).")
            .define("generation2Orb", false);
    private static final ForgeConfigSpec.DoubleValue GENERATION_2_POISON_HEAL = BUILDER
            .comment("\"Generation 2 Poison\" (original SRP).")
            .defineInRange("generation2PoisonHeal", 1D, 0D, 10D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_2_MOB_HEALING = BUILDER
            .comment("\"Generation 2 Mob Healing\" (original SRP).")
            .defineInRange("generation2MobHealing", 0.5D, 0D, 100D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_2_ATTACK_SPEED = BUILDER
            .comment("\"Generation 2 Attack Speed\" (original SRP).")
            .defineInRange("generation2AttackSpeed", 1D, 0D, 1D);
    // ---- original SRP "parasite_generation_03" ----
    private static final ForgeConfigSpec.DoubleValue GENERATION_3_COTH = BUILDER
            .comment("\"Generation 3 COTH Spawning Stats\" (original SRP).")
            .defineInRange("generation3Coth", 1D, 0D, 6D);
    private static final ForgeConfigSpec.BooleanValue GENERATION_3_SPECIAL_MOVES = BUILDER
            .comment("\"Generation 3 Special Moves\" (original SRP).")
            .define("generation3SpecialMoves", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_3_SPRINTING = BUILDER
            .comment("\"Generation 3 Sprinting\" (original SRP).")
            .define("generation3Sprinting", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_3_LOOK_WALLS = BUILDER
            .comment("\"Generation 3 X Ray\" (original SRP).")
            .define("generation3LookWalls", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_3_ADAPTATION = BUILDER
            .comment("\"Generation 3 Adaptation\" (original SRP).")
            .define("generation3Adaptation", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_3_DAMAGE_CAP = BUILDER
            .comment("\"Generation 3 Damage Cap\" (original SRP).")
            .define("generation3DamageCap", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_3_MINIMUM_DAMAGE = BUILDER
            .comment("\"Generation 3 Minimum Damage\" (original SRP).")
            .define("generation3MinimumDamage", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_3_WATER_LEAP = BUILDER
            .comment("\"Generation 3 Water Leap\" (original SRP).")
            .define("generation3WaterLeap", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_3_BLOCK_SEARCH = BUILDER
            .comment("\"Generation 3 Block Searching\" (original SRP).")
            .define("generation3BlockSearch", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_3_RESIDUE = BUILDER
            .comment("\"Generation 3 Residue\" (original SRP).")
            .define("generation3Residue", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_3_ORB = BUILDER
            .comment("\"Generation 3 Orb\" (original SRP).")
            .define("generation3Orb", false);
    private static final ForgeConfigSpec.DoubleValue GENERATION_3_POISON_HEAL = BUILDER
            .comment("\"Generation 3 Poison\" (original SRP).")
            .defineInRange("generation3PoisonHeal", 1.5D, 0D, 10D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_3_MOB_HEALING = BUILDER
            .comment("\"Generation 3 Mob Healing\" (original SRP).")
            .defineInRange("generation3MobHealing", 1D, 0D, 100D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_3_ATTACK_SPEED = BUILDER
            .comment("\"Generation 3 Attack Speed\" (original SRP).")
            .defineInRange("generation3AttackSpeed", 0.9D, 0D, 1D);
    // ---- original SRP "parasite_generation_04" ----
    private static final ForgeConfigSpec.DoubleValue GENERATION_4_COTH = BUILDER
            .comment("\"Generation 4 COTH Spawning Stats\" (original SRP).")
            .defineInRange("generation4Coth", 1D, 0D, 6D);
    private static final ForgeConfigSpec.BooleanValue GENERATION_4_SPECIAL_MOVES = BUILDER
            .comment("\"Generation 4 Special Moves\" (original SRP).")
            .define("generation4SpecialMoves", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_4_SPRINTING = BUILDER
            .comment("\"Generation 4 Sprinting\" (original SRP).")
            .define("generation4Sprinting", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_4_LOOK_WALLS = BUILDER
            .comment("\"Generation 4 X Ray\" (original SRP).")
            .define("generation4LookWalls", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_4_ADAPTATION = BUILDER
            .comment("\"Generation 4 Adaptation\" (original SRP).")
            .define("generation4Adaptation", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_4_DAMAGE_CAP = BUILDER
            .comment("\"Generation 4 Damage Cap\" (original SRP).")
            .define("generation4DamageCap", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_4_MINIMUM_DAMAGE = BUILDER
            .comment("\"Generation 4 Minimum Damage\" (original SRP).")
            .define("generation4MinimumDamage", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_4_WATER_LEAP = BUILDER
            .comment("\"Generation 4 Water Leap\" (original SRP).")
            .define("generation4WaterLeap", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_4_BLOCK_SEARCH = BUILDER
            .comment("\"Generation 4 Block Searching\" (original SRP).")
            .define("generation4BlockSearch", false);
    private static final ForgeConfigSpec.BooleanValue GENERATION_4_RESIDUE = BUILDER
            .comment("\"Generation 4 Residue\" (original SRP).")
            .define("generation4Residue", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_4_ORB = BUILDER
            .comment("\"Generation 4 Orb\" (original SRP).")
            .define("generation4Orb", false);
    private static final ForgeConfigSpec.DoubleValue GENERATION_4_POISON_HEAL = BUILDER
            .comment("\"Generation 4 Poison\" (original SRP).")
            .defineInRange("generation4PoisonHeal", 2D, 0D, 10D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_4_MOB_HEALING = BUILDER
            .comment("\"Generation 4 Mob Healing\" (original SRP).")
            .defineInRange("generation4MobHealing", 2D, 0D, 100D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_4_ATTACK_SPEED = BUILDER
            .comment("\"Generation 4 Attack Speed\" (original SRP).")
            .defineInRange("generation4AttackSpeed", 0.7D, 0D, 1D);
    // ---- original SRP "parasite_generation_05" ----
    private static final ForgeConfigSpec.DoubleValue GENERATION_5_COTH = BUILDER
            .comment("\"Generation 5 COTH Spawning Stats\" (original SRP).")
            .defineInRange("generation5Coth", 1D, 0D, 6D);
    private static final ForgeConfigSpec.BooleanValue GENERATION_5_SPECIAL_MOVES = BUILDER
            .comment("\"Generation 5 Special Moves\" (original SRP).")
            .define("generation5SpecialMoves", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_5_SPRINTING = BUILDER
            .comment("\"Generation 5 Sprinting\" (original SRP).")
            .define("generation5Sprinting", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_5_LOOK_WALLS = BUILDER
            .comment("\"Generation 5 X Ray\" (original SRP).")
            .define("generation5LookWalls", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_5_ADAPTATION = BUILDER
            .comment("\"Generation 5 Adaptation\" (original SRP).")
            .define("generation5Adaptation", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_5_DAMAGE_CAP = BUILDER
            .comment("\"Generation 5 Damage Cap\" (original SRP).")
            .define("generation5DamageCap", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_5_MINIMUM_DAMAGE = BUILDER
            .comment("\"Generation 5 Minimum Damage\" (original SRP).")
            .define("generation5MinimumDamage", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_5_WATER_LEAP = BUILDER
            .comment("\"Generation 5 Water Leap\" (original SRP).")
            .define("generation5WaterLeap", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_5_BLOCK_SEARCH = BUILDER
            .comment("\"Generation 5 Block Searching\" (original SRP).")
            .define("generation5BlockSearch", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_5_RESIDUE = BUILDER
            .comment("\"Generation 5 Residue\" (original SRP).")
            .define("generation5Residue", true);
    private static final ForgeConfigSpec.BooleanValue GENERATION_5_ORB = BUILDER
            .comment("\"Generation 5 Orb\" (original SRP).")
            .define("generation5Orb", true);
    private static final ForgeConfigSpec.DoubleValue GENERATION_5_POISON_HEAL = BUILDER
            .comment("\"Generation 5 Poison\" (original SRP).")
            .defineInRange("generation5PoisonHeal", 2.5D, 0D, 10D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_5_MOB_HEALING = BUILDER
            .comment("\"Generation 5 Mob Healing\" (original SRP).")
            .defineInRange("generation5MobHealing", 3D, 0D, 100D);
    private static final ForgeConfigSpec.DoubleValue GENERATION_5_ATTACK_SPEED = BUILDER
            .comment("\"Generation 5 Attack Speed\" (original SRP).")
            .defineInRange("generation5AttackSpeed", 0.5D, 0D, 1D);
    private static final ForgeConfigSpec.BooleanValue PEARL_DESTROYED_ON_BEHOLDER_KILL = BUILDER
            .comment("Destroy dropped Eyes of the Beholder when their owner is slain by a beholder.")
            .define("pearlDestroyedOnBeholderKill", true);
    private static final ForgeConfigSpec.DoubleValue OVERLAST_NATURAL_EVOLUTION_SCALE = BUILDER
            .comment("OverLast natural evolution points multiplier. Set to 0 to disable.")
            .defineInRange("overlastNaturalEvolutionScale", 1.0D, 0.0D, 10.0D);
    private static final ForgeConfigSpec.BooleanValue OVERLAST_HUD_REQUIRES_CLOCK = BUILDER
            .comment("Only show the OverLast evolution HUD while holding an evolution clock.")
            .define("overlastHudRequiresClock", false);
    private static final ForgeConfigSpec.ConfigValue<String> OVERLAST_HUD_POSITION = BUILDER
            .comment("OverLast HUD position: top left, top right, middle left, middle right, bottom left, bottom right.")
            .defineInList("overlastHudPosition", "top left", Arrays.asList(
                    "top left", "top right", "middle left", "middle right", "bottom left", "bottom right"));
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> COTH_VICTIM_PARASITES = BUILDER
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
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> COTH_IMMUNE_ENTITIES = BUILDER
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
    private static final ForgeConfigSpec.BooleanValue COTH_IMMUNE_LIST_INVERTED = BUILDER
            .comment("Treat cothImmuneEntities as a whitelist of entities that are not immune.")
            .define("cothImmuneListInverted", false);
    private static final ForgeConfigSpec.DoubleValue COTH_CONVERT_AT_KILL_CHANCE = BUILDER
            .comment("Base chance for a parasite kill to convert a COTH I victim. Higher COTH levels increase it.")
            .defineInRange("cothConvertAtKillChance", 0.3D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue COTH_ASSIMILATED_SPREAD_CHANCE = BUILDER
            .comment("Chance for an Assimilated parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothAssimilatedSpreadChance", 0.1D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue COTH_HIJACKED_SPREAD_CHANCE = BUILDER
            .comment("Chance for a Hijacked parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothHijackedSpreadChance", 0.05D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue COTH_FERAL_SPREAD_CHANCE = BUILDER
            .comment("Chance for a Feral parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothFeralSpreadChance", 0.2D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue COTH_CRUDE_SPREAD_CHANCE = BUILDER
            .comment("Chance for a Crude parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothCrudeSpreadChance", 0.4D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue COTH_PRIMITIVE_SPREAD_CHANCE = BUILDER
            .comment("Chance for a Primitive parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothPrimitiveSpreadChance", 0.5D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue COTH_ADAPTED_SPREAD_CHANCE = BUILDER
            .comment("Chance for an Adapted parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothAdaptedSpreadChance", 0.6D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue COTH_PURE_SPREAD_CHANCE = BUILDER
            .comment("Chance for a Pure or Preeminent parasite melee hit to infect an uninfected victim with COTH.")
            .defineInRange("cothPureSpreadChance", 0.8D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.IntValue COLONY_EXTRA_HEALTH_POINT = BUILDER
            .defineInRange("colonyExtraHealthPoint", 20, 1, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue COLONY_EXTRA_HEALTH_VALUE = BUILDER
            .defineInRange("colonyExtraHealthValue", 0.1D, 0.0D, 100.0D);
    private static final ForgeConfigSpec.IntValue COLONY_EXTRA_ARMOR_POINT = BUILDER
            .defineInRange("colonyExtraArmorPoint", 20, 1, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue COLONY_EXTRA_ARMOR_VALUE = BUILDER
            .defineInRange("colonyExtraArmorValue", 0.1D, 0.0D, 100.0D);
    private static final ForgeConfigSpec.IntValue COLONY_EXTRA_DAMAGE_POINT = BUILDER
            .defineInRange("colonyExtraDamagePoint", 20, 1, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue COLONY_EXTRA_DAMAGE_VALUE = BUILDER
            .defineInRange("colonyExtraDamageValue", 0.1D, 0.0D, 100.0D);
    private static final ForgeConfigSpec.IntValue COLONY_EXTRA_KD_POINT = BUILDER
            .defineInRange("colonyExtraKDResPoint", 20, 1, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue COLONY_EXTRA_KD_VALUE = BUILDER
            .defineInRange("colonyExtraKDResValue", 0.1D, 0.0D, 100.0D);
    private static final ForgeConfigSpec.IntValue COLONY_DAMAGE_CAP_POINT = BUILDER
            .defineInRange("colonyDamageCapPoint", 15, 1, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue COLONY_DAMAGE_CAP_VALUE = BUILDER
            .defineInRange("colonyDamageCapValue", 0.5D, 0.0D, 100.0D);
    private static final ForgeConfigSpec.IntValue COLONY_POINT_CAP = BUILDER
            .defineInRange("colonyPointCap", 100, 1, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue COLONY_TOTAL_POINT_CAP = BUILDER
            .defineInRange("colonyTotalPointCap", 100000, 1, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue COLONY_MAXIMUM_NUMBER = BUILDER
            .defineInRange("colonyMaximumNumber", 20, 1, 100);
    private static final ForgeConfigSpec.IntValue COLONY_MINIMUM_DISTANCE = BUILDER
            .defineInRange("colonyMinimumDistance", 2000, 1, 100000);
    private static final ForgeConfigSpec.IntValue COLONY_SPREAD_POINT = BUILDER
            .defineInRange("colonySpreadPoint", 2, 1, 10000);
    private static final ForgeConfigSpec.IntValue COLONY_SPREAD_VALUE = BUILDER
            .defineInRange("colonySpreadValue", 20, 1, 10000);
    private static final ForgeConfigSpec.IntValue COLONY_BASE_RADIUS = BUILDER
            .defineInRange("colonyBaseRadius", 120, 1, 10000);
    private static final ForgeConfigSpec.IntValue COLONY_EFFECT_SPREAD_POINT = BUILDER
            .defineInRange("colonyEffectSpreadPoint", 1, 1, 10000);
    private static final ForgeConfigSpec.IntValue COLONY_EFFECT_SPREAD_VALUE = BUILDER
            .defineInRange("colonyEffectSpreadValue", 40, 1, 10000);
    private static final ForgeConfigSpec.IntValue COLONY_BASE_EFFECT_RADIUS = BUILDER
            .defineInRange("colonyBaseEffectRadius", 300, 1, 10000);
    private static final ForgeConfigSpec.BooleanValue USE_DISLODGMENT = BUILDER
            .comment("Enable the original parasite dislodgment system.")
            .define("useDislodgment", true);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_DEATH_TRIGGER_CHANCE = BUILDER
            .comment("Chance for a parasite death to activate an eligible dislodgment code.")
            .defineInRange("dislodgmentDeathTriggerChance", 0.001D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.IntValue DISLODGMENT_GLOBAL_COOLDOWN = BUILDER
            .comment("Global dislodgment trigger cooldown in ticks.")
            .defineInRange("dislodgmentGlobalCooldown", 200, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLODGMENT_COTH_SPY = BUILDER
            .comment("Nearby COTH carriers required for player-action dislodgment triggers.")
            .defineInRange("dislodgmentCothSpy", 4, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_ONE_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseOneCodes");
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_TWO_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseTwoCodes");
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_THREE_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseThreeCodes");
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_FOUR_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseFourCodes");
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_FIVE_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseFiveCodes");
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_SIX_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseSixCodes");
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_SEVEN_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseSevenCodes");
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_EIGHT_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseEightCodes");
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_NINE_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseNineCodes");
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLODGMENT_PHASE_TEN_CODES =
            dislodgmentPhaseCodes("dislodgmentPhaseTenCodes");
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_RIGHT_CLICK_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentRightClickTriggerChance", 0.01D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_XP_PICKUP_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentXpPickupTriggerChance", 0.03D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_ITEM_PICKUP_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentItemPickupTriggerChance", 0.03D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_HEALING_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentHealingTriggerChance", 0.001D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_USE_ITEM_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentUseItemTriggerChance", 0.01D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_MENU_CLOSE_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentMenuCloseTriggerChance", 0.001D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_BLOCK_BREAK_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentBlockBreakTriggerChance", 0.1D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_NEXUS_ONE_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentNexusOneTriggerChance", 0.05D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_NEXUS_TWO_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentNexusTwoTriggerChance", 0.06D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_NEXUS_THREE_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentNexusThreeTriggerChance", 0.07D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.DoubleValue DISLODGMENT_NEXUS_FOUR_TRIGGER_CHANCE = BUILDER
            .defineInRange("dislodgmentNexusFourTriggerChance", 0.1D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.BooleanValue DISLO_COTH_IGNORE_AMPLIFIER = BUILDER
            .define("disloCothIgnoreAmplifier", true);
    private static final ForgeConfigSpec.IntValue DISLO_COTH_IGNORE_AMPLIFIER_POINT_COST = BUILDER
            .defineInRange("disloCothIgnoreAmplifierPointCost", 100, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_COTH_IGNORE_AMPLIFIER_DURATION = BUILDER
            .defineInRange("disloCothIgnoreAmplifierDuration", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_COTH_IGNORE_AMPLIFIER_COOLDOWN = BUILDER
            .defineInRange("disloCothIgnoreAmplifierCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_COTH_IGNORE_AMPLIFIER_TRIGGERS =
            dislodgmentTriggers("disloCothIgnoreAmplifierTriggers", List.of(1, 10, 14, 16));
    private static final ForgeConfigSpec.BooleanValue DISLO_COTH_TIERS = BUILDER
            .define("disloCothTiers", true);
    private static final ForgeConfigSpec.IntValue DISLO_COTH_TIERS_POINT_COST = BUILDER
            .defineInRange("disloCothTiersPointCost", 200, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_COTH_TIERS_VALUE = BUILDER
            .defineInRange("disloCothTiersValue", 1, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_COTH_TIERS_DURATION = BUILDER
            .defineInRange("disloCothTiersDuration", 40, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_COTH_TIERS_COOLDOWN = BUILDER
            .defineInRange("disloCothTiersCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_COTH_TIERS_PRIMITIVE = BUILDER
            .defineInRange("disloCothTiersPrimitive", 9, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_COTH_TIERS_ADAPTED = BUILDER
            .defineInRange("disloCothTiersAdapted", 15, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_COTH_TIERS_PURE = BUILDER
            .defineInRange("disloCothTiersPure", 21, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_COTH_TIERS_TRIGGERS =
            dislodgmentTriggers("disloCothTiersTriggers", List.of(12, 13, 14, 15, 16));
    private static final ForgeConfigSpec.BooleanValue DISLO_SUMMON_BY_DEATH = BUILDER
            .define("disloSummonByDeath", true);
    private static final ForgeConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_POINT_COST = BUILDER
            .defineInRange("disloSummonByDeathPointCost", 200, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_VALUE = BUILDER
            .defineInRange("disloSummonByDeathValue", 1, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_DURATION = BUILDER
            .defineInRange("disloSummonByDeathDuration", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_COOLDOWN = BUILDER
            .defineInRange("disloSummonByDeathCooldown", 200, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_KILLING = BUILDER
            .defineInRange("disloSummonByDeathKilling", 5, 0, 255);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISLO_SUMMON_BY_DEATH_MOBS = BUILDER
            .comment("Dislodgment 2 payload table formatted as minimum accumulated health;entity id.")
            .defineList("disloSummonByDeathMobs", List.of(
                    "1;csrp:sim_enderman",
                    "50;csrp:fer_enderman",
                    "100;csrp:warden"),
                    value -> value instanceof String && ((String) value).split(";", -1).length == 2);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_SUMMON_BY_DEATH_TRIGGERS =
            dislodgmentTriggers("disloSummonByDeathTriggers", List.of(10, 15, 16));
    private static final ForgeConfigSpec.BooleanValue DISLO_POTION_EFFECT = BUILDER
            .define("disloPotionEffect", true);
    private static final ForgeConfigSpec.IntValue DISLO_POTION_EFFECT_POINT_COST = BUILDER
            .defineInRange("disloPotionEffectPointCost", 200, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_POTION_EFFECT_VALUE = BUILDER
            .defineInRange("disloPotionEffectValue", 1, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_POTION_EFFECT_DURATION = BUILDER
            .defineInRange("disloPotionEffectDuration", 120, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_POTION_EFFECT_COOLDOWN = BUILDER
            .defineInRange("disloPotionEffectCooldown", 300, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISLO_POTION_EFFECTS = BUILDER
            .defineList("disloPotionEffects", List.of(
                    "minecraft:speed", "minecraft:fire_resistance", "minecraft:invisibility"),
                    value -> value instanceof String && ResourceLocation.tryParse((String) value) != null);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_POTION_EFFECT_TRIGGERS =
            dislodgmentTriggers("disloPotionEffectTriggers", List.of(4, 13, 14, 15, 16));
    private static final ForgeConfigSpec.BooleanValue DISLO_STATS = BUILDER.define("disloStats", true);
    private static final ForgeConfigSpec.IntValue DISLO_STATS_POINT_COST = BUILDER
            .defineInRange("disloStatsPointCost", 1000, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_STATS_VALUE = BUILDER
            .defineInRange("disloStatsValue", 2, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_STATS_DURATION = BUILDER
            .defineInRange("disloStatsDuration", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_STATS_COOLDOWN = BUILDER
            .defineInRange("disloStatsCooldown", 300, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_STATS_TRIGGERS =
            dislodgmentTriggers("disloStatsTriggers", List.of(14, 15, 17, 18));
    private static final ForgeConfigSpec.BooleanValue DISLO_DEATH_RAID = BUILDER.define("disloDeathRaid", true);
    private static final ForgeConfigSpec.IntValue DISLO_DEATH_RAID_POINT_COST = BUILDER
            .defineInRange("disloDeathRaidPointCost", 10, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DEATH_RAID_VALUE = BUILDER
            .defineInRange("disloDeathRaidValue", 10, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DEATH_RAID_DURATION = BUILDER
            .defineInRange("disloDeathRaidDuration", 10, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DEATH_RAID_COOLDOWN = BUILDER
            .defineInRange("disloDeathRaidCooldown", 10, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_DEATH_RAID_TRIGGERS =
            dislodgmentTriggers("disloDeathRaidTriggers", List.of(0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 16, 17, 18));
    private static final ForgeConfigSpec.BooleanValue DISLO_ITEM_DURABILITY = BUILDER.define("disloItemDurability", true);
    private static final ForgeConfigSpec.IntValue DISLO_ITEM_DURABILITY_POINT_COST = BUILDER
            .defineInRange("disloItemDurabilityPointCost", 100, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_ITEM_DURABILITY_VALUE = BUILDER
            .defineInRange("disloItemDurabilityValue", 2, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_ITEM_DURABILITY_DURATION = BUILDER
            .defineInRange("disloItemDurabilityDuration", 120, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_ITEM_DURABILITY_COOLDOWN = BUILDER
            .defineInRange("disloItemDurabilityCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_ITEM_DURABILITY_TRIGGERS =
            dislodgmentTriggers("disloItemDurabilityTriggers", List.of(4, 12, 13, 16));
    private static final ForgeConfigSpec.BooleanValue DISLO_HEALING_DEATH = BUILDER
            .define("disloHealingDeath", true);
    private static final ForgeConfigSpec.IntValue DISLO_HEALING_DEATH_POINT_COST = BUILDER
            .defineInRange("disloHealingDeathPointCost", 500, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_HEALING_DEATH_VALUE = BUILDER
            .defineInRange("disloHealingDeathValue", 100, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_HEALING_DEATH_DURATION = BUILDER
            .defineInRange("disloHealingDeathDuration", 40, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_HEALING_DEATH_COOLDOWN = BUILDER
            .defineInRange("disloHealingDeathCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_HEALING_DEATH_TRIGGERS =
            dislodgmentTriggers("disloHealingDeathTriggers", List.of(1, 3, 10, 12, 16));
    private static final ForgeConfigSpec.BooleanValue DISLO_DAMAGE_DEATH = BUILDER
            .define("disloDamageDeath", true);
    private static final ForgeConfigSpec.IntValue DISLO_DAMAGE_DEATH_POINT_COST = BUILDER
            .defineInRange("disloDamageDeathPointCost", 500, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DAMAGE_DEATH_VALUE = BUILDER
            .defineInRange("disloDamageDeathValue", 10, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DAMAGE_DEATH_DURATION = BUILDER
            .defineInRange("disloDamageDeathDuration", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DAMAGE_DEATH_COOLDOWN = BUILDER
            .defineInRange("disloDamageDeathCooldown", 300, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_DAMAGE_DEATH_TRIGGERS =
            dislodgmentTriggers("disloDamageDeathTriggers", List.of(0, 5, 13, 16));
    private static final ForgeConfigSpec.BooleanValue DISLO_FOOD_DEATH = BUILDER
            .define("disloFoodDeath", true);
    private static final ForgeConfigSpec.IntValue DISLO_FOOD_DEATH_POINT_COST = BUILDER
            .defineInRange("disloFoodDeathPointCost", 500, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_FOOD_DEATH_VALUE = BUILDER
            .defineInRange("disloFoodDeathValue", 100, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_FOOD_DEATH_DURATION = BUILDER
            .defineInRange("disloFoodDeathDuration", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_FOOD_DEATH_COOLDOWN = BUILDER
            .defineInRange("disloFoodDeathCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_FOOD_DEATH_TRIGGERS =
            dislodgmentTriggers("disloFoodDeathTriggers", List.of(3, 12, 13, 16));
    private static final ForgeConfigSpec.BooleanValue DISLO_DEATH_HIGH_VERSIONS = BUILDER
            .define("disloDeathHighVersions", true);
    private static final ForgeConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_POINT_COST = BUILDER
            .defineInRange("disloDeathHighVersionsPointCost", 300, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_VALUE = BUILDER
            .defineInRange("disloDeathHighVersionsValue", 1, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_ADAPTED = BUILDER
            .defineInRange("disloDeathHighVersionsAdapted", 12, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_PURE = BUILDER
            .defineInRange("disloDeathHighVersionsPure", 21, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_DURATION = BUILDER
            .defineInRange("disloDeathHighVersionsDuration", 120, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_DEATH_HIGH_VERSIONS_COOLDOWN = BUILDER
            .defineInRange("disloDeathHighVersionsCooldown", 360, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue DISLO_DEATH_HIGH_VERSIONS_CHANCE = BUILDER
            .defineInRange("disloDeathHighVersionsChance", 0.5D, 0.0D, 1.0D);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_DEATH_HIGH_VERSIONS_TRIGGERS =
            dislodgmentTriggers("disloDeathHighVersionsTriggers",
                    List.of(0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 16, 17, 18));
    private static final ForgeConfigSpec.BooleanValue DISLO_PARASITE_NO_POTION = BUILDER
            .define("disloParasiteNoPotion", true);
    private static final ForgeConfigSpec.IntValue DISLO_PARASITE_NO_POTION_POINT_COST = BUILDER
            .defineInRange("disloParasiteNoPotionPointCost", 100, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_PARASITE_NO_POTION_DURATION = BUILDER
            .defineInRange("disloParasiteNoPotionDuration", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_PARASITE_NO_POTION_COOLDOWN = BUILDER
            .defineInRange("disloParasiteNoPotionCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_PARASITE_NO_POTION_TRIGGERS =
            dislodgmentTriggers("disloParasiteNoPotionTriggers", List.of(3, 4, 16));
    private static final ForgeConfigSpec.BooleanValue DISLO_HEALTH_DRAINING = BUILDER
            .define("disloHealthDraining", true);
    private static final ForgeConfigSpec.IntValue DISLO_HEALTH_DRAINING_POINT_COST = BUILDER
            .defineInRange("disloHealthDrainingPointCost", 50000, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_HEALTH_DRAINING_VALUE = BUILDER
            .defineInRange("disloHealthDrainingValue", 10, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_HEALTH_DRAINING_DURATION = BUILDER
            .defineInRange("disloHealthDrainingDuration", 3, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_HEALTH_DRAINING_COOLDOWN = BUILDER
            .defineInRange("disloHealthDrainingCooldown", 300, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_HEALTH_DRAINING_TRIGGERS =
            dislodgmentTriggers("disloHealthDrainingTriggers", List.of(14, 15, 17, 18));
    private static final ForgeConfigSpec.BooleanValue DISLO_FOOD_DRAINING = BUILDER
            .define("disloFoodDraining", true);
    private static final ForgeConfigSpec.IntValue DISLO_FOOD_DRAINING_POINT_COST = BUILDER
            .defineInRange("disloFoodDrainingPointCost", 500, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_FOOD_DRAINING_VALUE = BUILDER
            .defineInRange("disloFoodDrainingValue", 200, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_FOOD_DRAINING_DURATION = BUILDER
            .defineInRange("disloFoodDrainingDuration", 3, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_FOOD_DRAINING_COOLDOWN = BUILDER
            .defineInRange("disloFoodDrainingCooldown", 300, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_FOOD_DRAINING_TRIGGERS =
            dislodgmentTriggers("disloFoodDrainingTriggers", List.of(12, 13, 14, 15, 17, 18));
    private static final ForgeConfigSpec.BooleanValue DISLO_NEXT_PHASE_LIST = BUILDER
            .define("disloNextPhaseList", true);
    private static final ForgeConfigSpec.IntValue DISLO_NEXT_PHASE_LIST_POINT_COST = BUILDER
            .defineInRange("disloNextPhaseListPointCost", 50000, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_NEXT_PHASE_LIST_VALUE = BUILDER
            .defineInRange("disloNextPhaseListValue", 1, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_NEXT_PHASE_LIST_DURATION = BUILDER
            .defineInRange("disloNextPhaseListDuration", 30, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_NEXT_PHASE_LIST_COOLDOWN = BUILDER
            .defineInRange("disloNextPhaseListCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_NEXT_PHASE_LIST_TRIGGERS =
            dislodgmentTriggers("disloNextPhaseListTriggers", List.of(15, 16, 17, 18));
    private static final ForgeConfigSpec.BooleanValue DISLO_GROWL_NOISE = BUILDER
            .define("disloGrowlNoise", true);
    private static final ForgeConfigSpec.IntValue DISLO_GROWL_NOISE_POINT_COST = BUILDER
            .defineInRange("disloGrowlNoisePointCost", 100, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_GROWL_NOISE_DURATION = BUILDER
            .defineInRange("disloGrowlNoiseDuration", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_GROWL_NOISE_COOLDOWN = BUILDER
            .defineInRange("disloGrowlNoiseCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_GROWL_NOISE_TRIGGERS =
            dislodgmentTriggers("disloGrowlNoiseTriggers", List.of(0, 4, 10, 11));
    private static final ForgeConfigSpec.BooleanValue DISLO_WALK_NOISE = BUILDER
            .define("disloWalkNoise", true);
    private static final ForgeConfigSpec.IntValue DISLO_WALK_NOISE_POINT_COST = BUILDER
            .defineInRange("disloWalkNoisePointCost", 100, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_WALK_NOISE_DURATION = BUILDER
            .defineInRange("disloWalkNoiseDuration", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_WALK_NOISE_COOLDOWN = BUILDER
            .defineInRange("disloWalkNoiseCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_WALK_NOISE_TRIGGERS =
            dislodgmentTriggers("disloWalkNoiseTriggers", List.of(0, 5, 10, 11));
    private static final ForgeConfigSpec.BooleanValue DISLO_SHIELD_FOOD = BUILDER
            .define("disloShieldFood", true);
    private static final ForgeConfigSpec.IntValue DISLO_SHIELD_FOOD_POINT_COST = BUILDER
            .defineInRange("disloShieldFoodPointCost", 180, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_SHIELD_FOOD_DURATION = BUILDER
            .defineInRange("disloShieldFoodDuration", 400, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_SHIELD_FOOD_COOLDOWN = BUILDER
            .defineInRange("disloShieldFoodCooldown", 550, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_SHIELD_FOOD_TRIGGERS =
            dislodgmentTriggers("disloShieldFoodTriggers",
                    List.of(0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 16, 17, 18));
    private static final ForgeConfigSpec.BooleanValue DISLO_LOOT_XP_CANCEL = BUILDER
            .define("disloLootXpCancel", true);
    private static final ForgeConfigSpec.IntValue DISLO_LOOT_XP_CANCEL_POINT_COST = BUILDER
            .defineInRange("disloLootXpCancelPointCost", 100, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_LOOT_XP_CANCEL_DURATION = BUILDER
            .defineInRange("disloLootXpCancelDuration", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_LOOT_XP_CANCEL_COOLDOWN = BUILDER
            .defineInRange("disloLootXpCancelCooldown", 240, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_LOOT_XP_CANCEL_TRIGGERS =
            dislodgmentTriggers("disloLootXpCancelTriggers", List.of(2, 10, 16));
    private static final ForgeConfigSpec.BooleanValue DISLO_BURNING_DEATH = BUILDER
            .define("disloBurningDeath", true);
    private static final ForgeConfigSpec.IntValue DISLO_BURNING_DEATH_POINT_COST = BUILDER
            .defineInRange("disloBurningDeathPointCost", 50000, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_BURNING_DEATH_DURATION = BUILDER
            .defineInRange("disloBurningDeathDuration", 60, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DISLO_BURNING_DEATH_COOLDOWN = BUILDER
            .defineInRange("disloBurningDeathCooldown", 180, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DISLO_BURNING_DEATH_TRIGGERS =
            dislodgmentTriggers("disloBurningDeathTriggers",
                    List.of(0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 16, 17, 18));

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends Integer>> dislodgmentTriggers(
            String name, List<Integer> defaults) {
        return BUILDER.defineList(name, defaults,
                value -> value instanceof Integer trigger && trigger >= 0 && trigger <= 18);
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends Integer>> dislodgmentPhaseCodes(String name) {
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

    public static boolean scentEnabled() { return SCENT_ENABLED.get(); }
    public static int scentCap() { return SCENT_CAP.get(); }
    public static int scentDevelopmentLevel() { return SCENT_DEVELOPMENT_LEVEL.get(); }
    public static boolean rageEnabled() { return RAGE_ENABLED.get(); }
    public static boolean mobAttackingEnabled() { return MOB_ATTACKING_ENABLED.get(); }
    public static List<? extends String> mobAttackingBlacklist() { return MOB_ATTACKING_BLACKLIST.get(); }
    public static boolean mobAttackingBlacklistInverted() { return MOB_ATTACKING_BLACKLIST_INVERTED.get(); }
    public static boolean collectiveConsciousnessEnabled() { return COLLECTIVE_CONSCIOUSNESS_ENABLED.get(); }
    public static int worldGnatCap() { return WORLD_GNAT_CAP.get(); }

    public static double variantSpawnChance() { return VARIANT_SPAWN_CHANCE.get(); }
    public static int alwaysVariantPhase() { return ALWAYS_VARIANT_PHASE.get(); }
    public static double tendrilHealth() { return TENDRIL_HEALTH.get(); }
    public static int purePointDamageCap() { return PURE_POINT_DAMAGE_CAP.get(); }

    public static double adaptationChance() {
        return ADAPTATION_CHANCE.get();
    }

    public static double parasiteKillingReduction() { return PARASITE_KILLING_REDUCTION.get(); }

    public static double killcountPlus() { return KILLCOUNT_PLUS.get(); }
    public static float primitiveMinimumDamage() { return PRIMITIVE_MINIMUM_DAMAGE.get().floatValue(); }
    public static boolean useEvolutionPhases() { return USE_EVOLUTION_PHASES.get(); }

    /**
     * Phase cooldown, in seconds, for the phase a dimension just entered: the original
     * {@code SRPConfigSystems} "Phase # Delay" option. Returns 0 when the phase has no configured
     * lock, when the index is past the end of the list, or for the pre-phase-0 start states, which
     * is also the default for every phase.
     */
    public static int phaseDelaySeconds(int phase) {
        if (phase < 0) {
            return 0;
        }
        List<? extends Integer> delays = PHASE_DELAY_SECONDS.get();
        if (phase >= delays.size()) {
            return 0;
        }
        Integer seconds = delays.get(phase);
        return seconds == null ? 0 : Math.max(0, seconds);
    }

    public static boolean generationEnabled() { return GENERATION_ENABLED.get(); }
    public static int generationDefaultValue() { return GENERATION_DEFAULT_VALUE.get(); }
    public static List<? extends String> generationDimensionStartingList() {
        return GENERATION_DIMENSION_STARTING_LIST.get();
    }
    public static int generationTime(int generation) {
        return switch (generation) {
            case 0 -> GENERATION_TIME_1.get();
            case 1 -> GENERATION_TIME_2.get();
            case 2 -> GENERATION_TIME_3.get();
            case 3 -> GENERATION_TIME_4.get();
            case 4 -> GENERATION_TIME_5.get();
            default -> 0;
        };
    }
    public static List<? extends Integer> generationPhases(int generation) {
        return switch (generation) {
            case 0 -> GENERATION_PHASES_1.get();
            case 1 -> GENERATION_PHASES_2.get();
            case 2 -> GENERATION_PHASES_3.get();
            case 3 -> GENERATION_PHASES_4.get();
            case 4 -> GENERATION_PHASES_5.get();
            default -> List.of();
        };
    }
    public static double generationPhasePenalty() { return GENERATION_PHASE_PENALTY.get(); }
    public static double generation0Coth() { return GENERATION_0_COTH.get(); }
    public static boolean generation0SpecialMoves() { return GENERATION_0_SPECIAL_MOVES.get(); }
    public static boolean generation0Sprinting() { return GENERATION_0_SPRINTING.get(); }
    public static boolean generation0LookWalls() { return GENERATION_0_LOOK_WALLS.get(); }
    public static boolean generation0Adaptation() { return GENERATION_0_ADAPTATION.get(); }
    public static boolean generation0DamageCap() { return GENERATION_0_DAMAGE_CAP.get(); }
    public static boolean generation0MinimumDamage() { return GENERATION_0_MINIMUM_DAMAGE.get(); }
    public static boolean generation0WaterLeap() { return GENERATION_0_WATER_LEAP.get(); }
    public static boolean generation0BlockSearch() { return GENERATION_0_BLOCK_SEARCH.get(); }
    public static boolean generation0Residue() { return GENERATION_0_RESIDUE.get(); }
    public static boolean generation0Orb() { return GENERATION_0_ORB.get(); }
    public static double generation0PoisonHeal() { return GENERATION_0_POISON_HEAL.get(); }
    public static double generation0MobHealing() { return GENERATION_0_MOB_HEALING.get(); }
    public static double generation0AttackSpeed() { return GENERATION_0_ATTACK_SPEED.get(); }
    public static double generation1Coth() { return GENERATION_1_COTH.get(); }
    public static boolean generation1SpecialMoves() { return GENERATION_1_SPECIAL_MOVES.get(); }
    public static boolean generation1Sprinting() { return GENERATION_1_SPRINTING.get(); }
    public static boolean generation1LookWalls() { return GENERATION_1_LOOK_WALLS.get(); }
    public static boolean generation1Adaptation() { return GENERATION_1_ADAPTATION.get(); }
    public static boolean generation1DamageCap() { return GENERATION_1_DAMAGE_CAP.get(); }
    public static boolean generation1MinimumDamage() { return GENERATION_1_MINIMUM_DAMAGE.get(); }
    public static boolean generation1WaterLeap() { return GENERATION_1_WATER_LEAP.get(); }
    public static boolean generation1BlockSearch() { return GENERATION_1_BLOCK_SEARCH.get(); }
    public static boolean generation1Residue() { return GENERATION_1_RESIDUE.get(); }
    public static boolean generation1Orb() { return GENERATION_1_ORB.get(); }
    public static double generation1PoisonHeal() { return GENERATION_1_POISON_HEAL.get(); }
    public static double generation1MobHealing() { return GENERATION_1_MOB_HEALING.get(); }
    public static double generation1AttackSpeed() { return GENERATION_1_ATTACK_SPEED.get(); }
    public static double generation2Coth() { return GENERATION_2_COTH.get(); }
    public static boolean generation2SpecialMoves() { return GENERATION_2_SPECIAL_MOVES.get(); }
    public static boolean generation2Sprinting() { return GENERATION_2_SPRINTING.get(); }
    public static boolean generation2LookWalls() { return GENERATION_2_LOOK_WALLS.get(); }
    public static boolean generation2Adaptation() { return GENERATION_2_ADAPTATION.get(); }
    public static boolean generation2DamageCap() { return GENERATION_2_DAMAGE_CAP.get(); }
    public static boolean generation2MinimumDamage() { return GENERATION_2_MINIMUM_DAMAGE.get(); }
    public static boolean generation2WaterLeap() { return GENERATION_2_WATER_LEAP.get(); }
    public static boolean generation2BlockSearch() { return GENERATION_2_BLOCK_SEARCH.get(); }
    public static boolean generation2Residue() { return GENERATION_2_RESIDUE.get(); }
    public static boolean generation2Orb() { return GENERATION_2_ORB.get(); }
    public static double generation2PoisonHeal() { return GENERATION_2_POISON_HEAL.get(); }
    public static double generation2MobHealing() { return GENERATION_2_MOB_HEALING.get(); }
    public static double generation2AttackSpeed() { return GENERATION_2_ATTACK_SPEED.get(); }
    public static double generation3Coth() { return GENERATION_3_COTH.get(); }
    public static boolean generation3SpecialMoves() { return GENERATION_3_SPECIAL_MOVES.get(); }
    public static boolean generation3Sprinting() { return GENERATION_3_SPRINTING.get(); }
    public static boolean generation3LookWalls() { return GENERATION_3_LOOK_WALLS.get(); }
    public static boolean generation3Adaptation() { return GENERATION_3_ADAPTATION.get(); }
    public static boolean generation3DamageCap() { return GENERATION_3_DAMAGE_CAP.get(); }
    public static boolean generation3MinimumDamage() { return GENERATION_3_MINIMUM_DAMAGE.get(); }
    public static boolean generation3WaterLeap() { return GENERATION_3_WATER_LEAP.get(); }
    public static boolean generation3BlockSearch() { return GENERATION_3_BLOCK_SEARCH.get(); }
    public static boolean generation3Residue() { return GENERATION_3_RESIDUE.get(); }
    public static boolean generation3Orb() { return GENERATION_3_ORB.get(); }
    public static double generation3PoisonHeal() { return GENERATION_3_POISON_HEAL.get(); }
    public static double generation3MobHealing() { return GENERATION_3_MOB_HEALING.get(); }
    public static double generation3AttackSpeed() { return GENERATION_3_ATTACK_SPEED.get(); }
    public static double generation4Coth() { return GENERATION_4_COTH.get(); }
    public static boolean generation4SpecialMoves() { return GENERATION_4_SPECIAL_MOVES.get(); }
    public static boolean generation4Sprinting() { return GENERATION_4_SPRINTING.get(); }
    public static boolean generation4LookWalls() { return GENERATION_4_LOOK_WALLS.get(); }
    public static boolean generation4Adaptation() { return GENERATION_4_ADAPTATION.get(); }
    public static boolean generation4DamageCap() { return GENERATION_4_DAMAGE_CAP.get(); }
    public static boolean generation4MinimumDamage() { return GENERATION_4_MINIMUM_DAMAGE.get(); }
    public static boolean generation4WaterLeap() { return GENERATION_4_WATER_LEAP.get(); }
    public static boolean generation4BlockSearch() { return GENERATION_4_BLOCK_SEARCH.get(); }
    public static boolean generation4Residue() { return GENERATION_4_RESIDUE.get(); }
    public static boolean generation4Orb() { return GENERATION_4_ORB.get(); }
    public static double generation4PoisonHeal() { return GENERATION_4_POISON_HEAL.get(); }
    public static double generation4MobHealing() { return GENERATION_4_MOB_HEALING.get(); }
    public static double generation4AttackSpeed() { return GENERATION_4_ATTACK_SPEED.get(); }
    public static double generation5Coth() { return GENERATION_5_COTH.get(); }
    public static boolean generation5SpecialMoves() { return GENERATION_5_SPECIAL_MOVES.get(); }
    public static boolean generation5Sprinting() { return GENERATION_5_SPRINTING.get(); }
    public static boolean generation5LookWalls() { return GENERATION_5_LOOK_WALLS.get(); }
    public static boolean generation5Adaptation() { return GENERATION_5_ADAPTATION.get(); }
    public static boolean generation5DamageCap() { return GENERATION_5_DAMAGE_CAP.get(); }
    public static boolean generation5MinimumDamage() { return GENERATION_5_MINIMUM_DAMAGE.get(); }
    public static boolean generation5WaterLeap() { return GENERATION_5_WATER_LEAP.get(); }
    public static boolean generation5BlockSearch() { return GENERATION_5_BLOCK_SEARCH.get(); }
    public static boolean generation5Residue() { return GENERATION_5_RESIDUE.get(); }
    public static boolean generation5Orb() { return GENERATION_5_ORB.get(); }
    public static double generation5PoisonHeal() { return GENERATION_5_POISON_HEAL.get(); }
    public static double generation5MobHealing() { return GENERATION_5_MOB_HEALING.get(); }
    public static double generation5AttackSpeed() { return GENERATION_5_ATTACK_SPEED.get(); }
    public static boolean parasiteGenResidue() { return PARASITE_GEN_RESIDUE.get(); }
    public static boolean pearlDestroyedOnBeholderKill() { return PEARL_DESTROYED_ON_BEHOLDER_KILL.get(); }
    public static double overlastNaturalEvolutionScale() { return OVERLAST_NATURAL_EVOLUTION_SCALE.get(); }
    public static boolean overlastHudRequiresClock() { return OVERLAST_HUD_REQUIRES_CLOCK.get(); }
    public static String overlastHudPosition() { return OVERLAST_HUD_POSITION.get(); }
    public static List<? extends String> cothVictimParasites() { return COTH_VICTIM_PARASITES.get(); }
    public static List<? extends String> cothImmuneEntities() { return COTH_IMMUNE_ENTITIES.get(); }
    public static boolean cothImmuneListInverted() { return COTH_IMMUNE_LIST_INVERTED.get(); }
    public static double cothConvertAtKillChance() { return COTH_CONVERT_AT_KILL_CHANCE.get(); }
    public static double cothAssimilatedSpreadChance() { return COTH_ASSIMILATED_SPREAD_CHANCE.get(); }
    public static double cothHijackedSpreadChance() { return COTH_HIJACKED_SPREAD_CHANCE.get(); }
    public static double cothFeralSpreadChance() { return COTH_FERAL_SPREAD_CHANCE.get(); }
    public static double cothCrudeSpreadChance() { return COTH_CRUDE_SPREAD_CHANCE.get(); }
    public static double cothPrimitiveSpreadChance() { return COTH_PRIMITIVE_SPREAD_CHANCE.get(); }
    public static double cothAdaptedSpreadChance() { return COTH_ADAPTED_SPREAD_CHANCE.get(); }
    public static double cothPureSpreadChance() { return COTH_PURE_SPREAD_CHANCE.get(); }

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
    public static int colonyMaximumNumber() { return COLONY_MAXIMUM_NUMBER.get(); }
    public static int colonyMinimumDistance() { return COLONY_MINIMUM_DISTANCE.get(); }
    public static int colonySpreadPoint() { return COLONY_SPREAD_POINT.get(); }
    public static int colonySpreadValue() { return COLONY_SPREAD_VALUE.get(); }
    public static int colonyBaseRadius() { return COLONY_BASE_RADIUS.get(); }
    public static int colonyEffectSpreadPoint() { return COLONY_EFFECT_SPREAD_POINT.get(); }
    public static int colonyEffectSpreadValue() { return COLONY_EFFECT_SPREAD_VALUE.get(); }
    public static int colonyBaseEffectRadius() { return COLONY_BASE_EFFECT_RADIUS.get(); }
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
