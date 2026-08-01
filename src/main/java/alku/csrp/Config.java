package alku.csrp;

import alku.csrp.world.SrpWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue EVOLUTION_PHASE = BUILDER
            .comment("Current parasite evolution phase used by phase-gated spawning and behavior.")
            .defineInRange("evolutionPhase", -1, -2, 10);
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
    private static final ModConfigSpec.BooleanValue DISLO_SUMMON_BY_DEATH = BUILDER
            .define("disloSummonByDeath", true);
    private static final ModConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_POINT_COST = BUILDER
            .defineInRange("disloSummonByDeathPointCost", 200, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_VALUE = BUILDER
            .defineInRange("disloSummonByDeathValue", 1, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_DURATION = BUILDER
            .defineInRange("disloSummonByDeathDuration", 60, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_SUMMON_BY_DEATH_KILLING = BUILDER
            .defineInRange("disloSummonByDeathKilling", 5, 0, 255);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DISLO_SUMMON_BY_DEATH_MOBS = BUILDER
            .comment("Dislodgment 2 payload table formatted as minimum accumulated health;entity id.")
            .defineList("disloSummonByDeathMobs", List.of(
                    "1;csrp:sim_enderman",
                    "50;csrp:fer_enderman",
                    "100;csrp:warden"),
                    value -> value instanceof String && ((String) value).split(";", -1).length == 2);
    private static final ModConfigSpec.BooleanValue DISLO_HEALING_DEATH = BUILDER
            .define("disloHealingDeath", true);
    private static final ModConfigSpec.IntValue DISLO_HEALING_DEATH_POINT_COST = BUILDER
            .defineInRange("disloHealingDeathPointCost", 500, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_HEALING_DEATH_VALUE = BUILDER
            .defineInRange("disloHealingDeathValue", 100, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_HEALING_DEATH_DURATION = BUILDER
            .defineInRange("disloHealingDeathDuration", 40, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.BooleanValue DISLO_DAMAGE_DEATH = BUILDER
            .define("disloDamageDeath", true);
    private static final ModConfigSpec.BooleanValue DISLO_FOOD_DEATH = BUILDER
            .define("disloFoodDeath", true);
    private static final ModConfigSpec.BooleanValue DISLO_LOOT_XP_CANCEL = BUILDER
            .define("disloLootXpCancel", true);
    private static final ModConfigSpec.IntValue DISLO_LOOT_XP_CANCEL_POINT_COST = BUILDER
            .defineInRange("disloLootXpCancelPointCost", 100, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DISLO_LOOT_XP_CANCEL_DURATION = BUILDER
            .defineInRange("disloLootXpCancelDuration", 60, 0, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static int evolutionPhase() {
        return EVOLUTION_PHASE.get();
    }

    public static int evolutionPhase(Level level) {
        return level instanceof ServerLevel serverLevel
                ? SrpWorldData.get(serverLevel).evolutionPhase()
                : evolutionPhase();
    }

    public static double adaptationChance() {
        return ADAPTATION_CHANCE.get();
    }

    public static double parasiteKillingReduction() { return PARASITE_KILLING_REDUCTION.get(); }

    public static double cothConvert() { return COTH_CONVERT.get(); }
    public static double killcountPlus() { return KILLCOUNT_PLUS.get(); }
    public static boolean useEvolutionPhases() { return USE_EVOLUTION_PHASES.get(); }
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
    public static boolean disloSummonByDeath() { return DISLO_SUMMON_BY_DEATH.get(); }
    public static int disloSummonByDeathPointCost() { return DISLO_SUMMON_BY_DEATH_POINT_COST.get(); }
    public static int disloSummonByDeathValue() { return DISLO_SUMMON_BY_DEATH_VALUE.get(); }
    public static int disloSummonByDeathDuration() { return DISLO_SUMMON_BY_DEATH_DURATION.get(); }
    public static int disloSummonByDeathKilling() { return DISLO_SUMMON_BY_DEATH_KILLING.get(); }
    public static List<? extends String> disloSummonByDeathMobs() { return DISLO_SUMMON_BY_DEATH_MOBS.get(); }
    public static boolean disloHealingDeath() { return DISLO_HEALING_DEATH.get(); }
    public static int disloHealingDeathPointCost() { return DISLO_HEALING_DEATH_POINT_COST.get(); }
    public static int disloHealingDeathValue() { return DISLO_HEALING_DEATH_VALUE.get(); }
    public static int disloHealingDeathDuration() { return DISLO_HEALING_DEATH_DURATION.get(); }
    public static boolean disloDamageDeath() { return DISLO_DAMAGE_DEATH.get(); }
    public static boolean disloFoodDeath() { return DISLO_FOOD_DEATH.get(); }
    public static boolean disloLootXpCancel() { return DISLO_LOOT_XP_CANCEL.get(); }
    public static int disloLootXpCancelPointCost() { return DISLO_LOOT_XP_CANCEL_POINT_COST.get(); }
    public static int disloLootXpCancelDuration() { return DISLO_LOOT_XP_CANCEL_DURATION.get(); }
}
