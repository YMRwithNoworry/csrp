package alku.csrp.config;

import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.AncientParasiteEntity;
import alku.csrp.entity.ArchitectEntity;
import alku.csrp.entity.AssimilatedParasiteEntity;
import alku.csrp.entity.MovingFleshEntity;
import alku.csrp.entity.PreeminentParasiteEntity;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.entity.PureParasiteEntity;
import alku.csrp.entity.WorkerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class MobsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue INFECTED_FOLLOW = followRange(
            "parasitePropertiesAssimilated", "infectedFollow", 16.0D);
    private static final ModConfigSpec.DoubleValue PRIMITIVE_FOLLOW = followRange(
            "parasitePropertiesPrimitive", "primitiveFollow", 24.0D);
    private static final ModConfigSpec.DoubleValue ADAPTED_FOLLOW = followRange(
            "parasitePropertiesAdapted", "adaptedFollow", 32.0D);
    private static final ModConfigSpec.DoubleValue PURE_FOLLOW = followRange(
            "parasitePropertiesPure", "pureFollow", 32.0D);
    private static final ModConfigSpec.DoubleValue ANCIENT_FOLLOW = followRange(
            "parasitePropertiesAncient", "ancientFollow", 64.0D);
    private static final ModConfigSpec.IntValue ANCIENT_MAX_Y = intValue(
            "srparasites:anc_dreadnaut", "ancientDreadnautMaxY", 256, 1, 320,
            "Maximum flight height for the Ancient Dreadnaut.");
    private static final ModConfigSpec.IntValue ANCIENT_MIN_Y = intValue(
            "srparasites:anc_dreadnaut", "ancientDreadnautMinY", 7, 0, 320,
            "Minimum flight height for the Ancient Dreadnaut.");
    private static final ModConfigSpec.IntValue ANCIENT_POD_COOLDOWN = intValue(
            "srparasites:anc_dreadnaut", "ancientDreadnautPodCooldownSeconds", 12, 1, 256,
            "Cooldown in seconds between Ancient Dreadnaut drop-pod attacks.");
    private static final ModConfigSpec.IntValue ANCIENT_POD_NUMBER = intValue(
            "srparasites:anc_dreadnaut", "ancientDreadnautPodNumber", 5, 1, 256,
            "Number of drop pods spawned per Ancient Dreadnaut attack.");
    private static final ModConfigSpec.IntValue ANCIENT_POD_MAX_MOBS = intValue(
            "srparasites:anc_dreadnaut", "ancientDreadnautPodMaxMobs", 1, 1, 256,
            "Maximum payload mobs spawned by one Ancient Dreadnaut drop pod.");
    private static final ModConfigSpec.BooleanValue ANCIENT_POD_GRIEFING = booleanValue(
            "srparasites:anc_pod", "ancientPodGriefing", false,
            "Whether Ancient Drop Pods may destroy blocks when mobGriefing is enabled.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ANCIENT_MOB_LIST = stringList(
            "srparasites:anc_dreadnaut", "ancientDreadnautMobList", List.of(
                    "csrp:rupter;1", "csrp:rupter;1", "csrp:rupter;1", "csrp:rupter;0.5", "csrp:rupter;0.5",
                    "csrp:grunt;0.7"),
            "Ancient Dreadnaut payload table: entity_id;weight.", MobsConfig::validMobEntry);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ANCIENT_POD_EFFECTS = stringList(
            "srparasites:anc_pod", "ancientPodEffects", List.of("10;-2;minecraft:saturation"),
            "Ancient Drop Pod effects: seconds;amplifier;effect_id.", MobsConfig::validAreaEffect);
    private static final ModConfigSpec.DoubleValue PREEMINENT_FOLLOW = followRange(
            "parasitePropertiesPreeminent", "preeminentFollow", 80.0D);

    private static final ModConfigSpec.BooleanValue RUPTER_ANIMAL_ATTACKING = booleanValue(
            "srparasites:rupter", "rupterPassiveMobAttacking", true,
            "Whether Rupters may attack passive mobs when evolution phases are disabled.");
    private static final ModConfigSpec.DoubleValue RUPTER_MINIMUM_DAMAGE = value(
            "srparasites:rupter", "rupterMinimumDamage", 0.1D, 0.0D, 1024.0D,
            "Minimum damage applied by a Rupter melee hit after armor reduction.");
    private static final ModConfigSpec.IntValue RUPTER_TUNNEL_COST = intValue(
            "srparasites:rupter", "rupterTunnelCost", 5, 0, 100,
            "Killcount cost of placing a Buglin Tunnel.");
    private static final ModConfigSpec.IntValue RUPTER_TUNNEL_PHASE = intValue(
            "srparasites:rupter", "rupterTunnelPhase", 3, 0, 9,
            "From this creation phase onward, Rupters do not place tunnels.");
    private static final ModConfigSpec.IntValue RUPTER_MANGLER_KILLS = intValue(
            "srparasites:rupter", "rupterManglerKills", 30, 0, 1000,
            "Kills required for a Rupter to become a Mangler.");
    private static final ModConfigSpec.DoubleValue MANGLER_MINIMUM_DAMAGE = value(
            "srparasites:mangler", "manglerMinimumDamage", 0.3D, 0.0D, 1024.0D,
            "Minimum damage applied by a Mangler melee hit after armor reduction.");
    private static final ModConfigSpec.DoubleValue MANGLER_REGENERATION = value(
            "srparasites:mangler", "manglerRegeneration", 14.0D, 0.0D, 1024.0D,
            "Health restored by each Mangler regeneration pulse.");

    private static final ModConfigSpec.BooleanValue CARRIER_HEAVY_GRIEFING = booleanValue(
            "srparasites:carrier_heavy", "carrierHeavyGriefing", false,
            "Whether Heavy Carriers may destroy blocks when exploding.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> CARRIER_HEAVY_MOBS = stringList(
            "srparasites:carrier_heavy", "carrierHeavyMobTable", List.of(
                    "srparasites:rupter;4;1", "srparasites:buglin;5;2", "srparasites:gnat;6;2"),
            "Heavy Carrier payload table: entity_id;maximum;minimum.", MobsConfig::validSpawnTableEntry);
    private static final ModConfigSpec.BooleanValue CARRIER_LIGHT_GRIEFING = booleanValue(
            "srparasites:carrier_light", "carrierLightGriefing", false,
            "Whether Light Carriers may destroy blocks when exploding.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> CARRIER_LIGHT_MOBS = stringList(
            "srparasites:carrier_light", "carrierLightMobTable", List.of(
                    "srparasites:rupter;3;2", "srparasites:buglin;4;3", "srparasites:gnat;5;3"),
            "Light Carrier payload table: entity_id;maximum;minimum.", MobsConfig::validSpawnTableEntry);
    private static final ModConfigSpec.BooleanValue CARRIER_FLYING_GRIEFING = booleanValue(
            "srparasites:carrier_flying", "carrierFlyingGriefing", false,
            "Whether Flying Carriers may destroy blocks when exploding.");
    private static final ModConfigSpec.IntValue CARRIER_FLYING_MAX_Y = intValue(
            "srparasites:carrier_flying", "carrierFlyingMaxY", 256, 1, 320,
            "Maximum flight height for the Flying Carrier.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> CARRIER_FLYING_MOBS = stringList(
            "srparasites:carrier_flying", "carrierFlyingMobTable", List.of(
                    "srparasites:rupter;3;1", "srparasites:buglin;3;2"),
            "Flying Carrier payload table: entity_id;maximum;minimum.", MobsConfig::validSpawnTableEntry);
    private static final ModConfigSpec.BooleanValue MERGE_RANDOM = booleanValue(
            "merge_System", "mergeSystemRandom", true,
            "Whether Moving Flesh always selects a random entry from its mob table.");
    private static final ModConfigSpec.DoubleValue MERGE_HEALTH = value(
            "merge_System", "mergeSystemMobHealth", 0.5D, 0.0D, 1.0D,
            "Health fraction of the parasite spawned by Moving Flesh.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MERGE_MOB_TABLE = stringList(
            "merge_System", "mergeSystemMobList", List.of(
                    // Crude
                    "srparasites:airscrew;0", "srparasites:crux;0",
                    "srparasites:crux_incomplete;0", "srparasites:dredge;0",
                    "srparasites:heed;0", "srparasites:host;0",
                    "srparasites:hostii;0", "srparasites:incompleteform_medium;0",
                    "srparasites:incompleteform_small;0", "srparasites:thrall;0",
                    // Feral
                    "srparasites:fer_bear;0", "srparasites:fer_cow;0",
                    "srparasites:fer_enderman;0", "srparasites:fer_horse;0",
                    "srparasites:fer_human;0", "srparasites:fer_pig;0",
                    "srparasites:fer_sheep;0", "srparasites:fer_villager;0",
                    "srparasites:fer_wolf;0",
                    // Assimara
                    "srparasites:mar_bear;0", "srparasites:mar_cow;0",
                    "srparasites:mar_enderman;0", "srparasites:mar_human;0",
                    "srparasites:mar_sheep;0", "srparasites:mar_villager;0",
                    // Hijacked
                    "srparasites:hi_blaze;0", "srparasites:hi_golem;0",
                    "srparasites:hi_skeleton;0",
                    // Primitive
                    "srparasites:pri_summoner;0", "srparasites:pri_longarms;0",
                    "srparasites:pri_reeker;0", "srparasites:pri_manducater;0",
                    "srparasites:pri_bolster;0", "srparasites:pri_yelloweye;0",
                    "srparasites:pri_arachnida;0", "srparasites:pri_vermin;0",
                    "srparasites:pri_tozoon;0", "srparasites:pri_burrower;0",
                    "srparasites:pri_devourer;0", "srparasites:pri_viscera;0",
                    // Adapted
                    "srparasites:ada_arachnida;0", "srparasites:ada_bolster;0",
                    "srparasites:ada_burrower;0", "srparasites:ada_devourer;0",
                    "srparasites:ada_longarms;0", "srparasites:ada_manducater;0",
                    "srparasites:ada_reeker;0", "srparasites:ada_summoner;0",
                    "srparasites:ada_tozoon;0", "srparasites:ada_vermin;0",
                    "srparasites:ada_viscera;0", "srparasites:ada_yelloweye;0",
                    // Pure
                    "srparasites:bomber_light;0", "srparasites:grunt;0",
                    "srparasites:marauder;0", "srparasites:monarch;0",
                    "srparasites:overseer;0", "srparasites:vigilante;0",
                    "srparasites:warden;0"),
            "Moving Flesh merge table: entity_id;merge_value. Every tier is listed, so two Living "
                    + "Flesh masses melt into a random Crude/Feral/Assimara/Hijacked/Primitive/"
                    + "Adapted/Pure parasite.", MobsConfig::validMergeMobEntry);

    // Legacy SRPConfigMobs: per-mob attribute multipliers (default 1.0F in the original).
    private static final ModConfigSpec.DoubleValue DORPA_HEALTH_MULTIPLIER = value(
            "srparasites:dorpa", "dorpaHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the assimilated spider (legacy SRPConfigMobs.dorpaHealthMultiplier).");
    private static final ModConfigSpec.DoubleValue DORPA_DAMAGE_MULTIPLIER = value(
            "srparasites:dorpa", "dorpaDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the assimilated spider.");
    private static final ModConfigSpec.DoubleValue DORPA_ARMOR_MULTIPLIER = value(
            "srparasites:dorpa", "dorpaArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the assimilated spider.");
    private static final ModConfigSpec.DoubleValue DORPA_KNOCKBACK_MULTIPLIER = value(
            "srparasites:dorpa", "dorpaKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the assimilated spider.");

    // Legacy SRPConfigMobs.infcow* (the assimilated cow).
    private static final ModConfigSpec.DoubleValue INFCOW_HEALTH_MULTIPLIER = value(
            "srparasites:infcow", "infcowHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the assimilated cow (legacy SRPConfigMobs.infcowHealthMultiplier).");
    private static final ModConfigSpec.DoubleValue INFCOW_DAMAGE_MULTIPLIER = value(
            "srparasites:infcow", "infcowDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the assimilated cow.");
    private static final ModConfigSpec.DoubleValue INFCOW_ARMOR_MULTIPLIER = value(
            "srparasites:infcow", "infcowArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the assimilated cow.");
    private static final ModConfigSpec.DoubleValue INFCOW_KNOCKBACK_MULTIPLIER = value(
            "srparasites:infcow", "infcowKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the assimilated cow.");

    // Legacy SRPConfigMobs.infvillager* (the assimilated variant villager; keys verified in the
    // original at SRPConfigMobs.java:440-443, all defaulting to 1.0F).
    private static final ModConfigSpec.DoubleValue INFVILLAGER_HEALTH_MULTIPLIER = value(
            "srparasites:infvillager", "infvillagerHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the assimilated villager.");
    private static final ModConfigSpec.DoubleValue INFVILLAGER_DAMAGE_MULTIPLIER = value(
            "srparasites:infvillager", "infvillagerDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the assimilated villager.");
    private static final ModConfigSpec.DoubleValue INFVILLAGER_ARMOR_MULTIPLIER = value(
            "srparasites:infvillager", "infvillagerArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the assimilated villager.");
    private static final ModConfigSpec.DoubleValue INFVILLAGER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:infvillager", "infvillagerKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the assimilated villager.");

    // Legacy self-destruct summons (SRPConfigMobs.*mob, all summoning buglin).
    private static final ModConfigSpec.ConfigValue<String> _D_O_R_P_A_M_O_B_S_U_M_M_O_N = BUILDER
            .comment("Legacy SRPConfigMobs.dorpamob — self-destruct summon spec <entity id>;<min>;<max>.")
            .define("dorpamob", "srparasites:buglin;5;5");
    private static final ModConfigSpec.ConfigValue<String> _I_N_F_C_O_W_M_O_B_S_U_M_M_O_N = BUILDER
            .comment("Legacy SRPConfigMobs.infcowmob — self-destruct summon spec <entity id>;<min>;<max>.")
            .define("infcowmob", "srparasites:buglin;4;3");
    private static final ModConfigSpec.ConfigValue<String> _I_N_F_S_H_E_E_P_M_O_B_S_U_M_M_O_N = BUILDER
            .comment("Legacy SRPConfigMobs.infsheepmob — self-destruct summon spec <entity id>;<min>;<max>.")
            .define("infsheepmob", "srparasites:buglin;3;3");
    private static final ModConfigSpec.ConfigValue<String> _I_N_F_W_O_L_F_M_O_B_S_U_M_M_O_N = BUILDER
            .comment("Legacy SRPConfigMobs.infwolfmob — self-destruct summon spec <entity id>;<min>;<max>.")
            .define("infwolfmob", "srparasites:buglin;2;2");
    private static final ModConfigSpec.ConfigValue<String> _I_N_F_P_I_G_M_O_B_S_U_M_M_O_N = BUILDER
            .comment("Legacy SRPConfigMobs.infpigmob — self-destruct summon spec <entity id>;<min>;<max>.")
            .define("infpigmob", "srparasites:buglin;2;2");
    private static final ModConfigSpec.ConfigValue<String> _I_N_F_V_I_L_L_A_G_E_R_M_O_B_S_U_M_M_O_N = BUILDER
            .comment("Legacy SRPConfigMobs.infvillagermob — self-destruct summon spec <entity id>;<min>;<max>.")
            .define("infvillagermob", "srparasites:buglin;2;2");
    private static final ModConfigSpec.ConfigValue<String> _I_N_F_H_O_R_S_E_M_O_B_S_U_M_M_O_N = BUILDER
            .comment("Legacy SRPConfigMobs.infhorsemob — self-destruct summon spec <entity id>;<min>;<max>.")
            .define("infhorsemob", "srparasites:buglin;2;2");
    private static final ModConfigSpec.ConfigValue<String> _I_N_F_A_D_V_E_N_T_U_R_E_R_M_O_B_S_U_M_M_O_N = BUILDER
            .comment("Legacy SRPConfigMobs.infadventurermob — self-destruct summon spec <entity id>;<min>;<max>.")
            .define("infadventurermob", "srparasites:buglin;4;3");

    public static double infcowHealthMultiplier() { return safe(INFCOW_HEALTH_MULTIPLIER); }
    public static double infvillagerHealthMultiplier() { return safe(INFVILLAGER_HEALTH_MULTIPLIER); }
    public static double infvillagerDamageMultiplier() { return safe(INFVILLAGER_DAMAGE_MULTIPLIER); }
    public static double infvillagerArmorMultiplier() { return safe(INFVILLAGER_ARMOR_MULTIPLIER); }
    public static double infvillagerKnockbackMultiplier() { return safe(INFVILLAGER_KNOCKBACK_MULTIPLIER); }
    public static String dorpaMobSummon() { return safe(_D_O_R_P_A_M_O_B_S_U_M_M_O_N); }
    public static String infcowMobSummon() { return safe(_I_N_F_C_O_W_M_O_B_S_U_M_M_O_N); }
    public static String infsheepMobSummon() { return safe(_I_N_F_S_H_E_E_P_M_O_B_S_U_M_M_O_N); }
    public static String infwolfMobSummon() { return safe(_I_N_F_W_O_L_F_M_O_B_S_U_M_M_O_N); }
    public static String infpigMobSummon() { return safe(_I_N_F_P_I_G_M_O_B_S_U_M_M_O_N); }
    public static String infvillagerMobSummon() { return safe(_I_N_F_V_I_L_L_A_G_E_R_M_O_B_S_U_M_M_O_N); }
    public static String infhorseMobSummon() { return safe(_I_N_F_H_O_R_S_E_M_O_B_S_U_M_M_O_N); }
    public static String infadventurerMobSummon() { return safe(_I_N_F_A_D_V_E_N_T_U_R_E_R_M_O_B_S_U_M_M_O_N); }
    public static double infcowDamageMultiplier() { return safe(INFCOW_DAMAGE_MULTIPLIER); }
    public static double infcowArmorMultiplier() { return safe(INFCOW_ARMOR_MULTIPLIER); }
    public static double infcowKnockbackMultiplier() { return safe(INFCOW_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs.infsheep* / infwolf*.
    private static final ModConfigSpec.DoubleValue INFSHEEP_HEALTH_MULTIPLIER = value(
            "srparasites:infsheep", "infsheepHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the assimilated sheep.");
    private static final ModConfigSpec.DoubleValue INFSHEEP_DAMAGE_MULTIPLIER = value(
            "srparasites:infsheep", "infsheepDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the assimilated sheep.");
    private static final ModConfigSpec.DoubleValue INFSHEEP_ARMOR_MULTIPLIER = value(
            "srparasites:infsheep", "infsheepArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the assimilated sheep.");
    private static final ModConfigSpec.DoubleValue INFSHEEP_KNOCKBACK_MULTIPLIER = value(
            "srparasites:infsheep", "infsheepKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the assimilated sheep.");
    private static final ModConfigSpec.DoubleValue INFWOLF_HEALTH_MULTIPLIER = value(
            "srparasites:infwolf", "infwolfHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the assimilated wolf.");
    private static final ModConfigSpec.DoubleValue INFWOLF_DAMAGE_MULTIPLIER = value(
            "srparasites:infwolf", "infwolfDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the assimilated wolf.");
    private static final ModConfigSpec.DoubleValue INFWOLF_ARMOR_MULTIPLIER = value(
            "srparasites:infwolf", "infwolfArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the assimilated wolf.");
    private static final ModConfigSpec.DoubleValue INFWOLF_KNOCKBACK_MULTIPLIER = value(
            "srparasites:infwolf", "infwolfKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the assimilated wolf.");

    // Legacy SRPConfigMobs.arachnida* accessors: the keys existed but were unreachable, so no code
    // could read the per-mob multipliers they describe.
    public static double arachnidaHealthMultiplier() { return safe(ARACHNIDA_HEALTH_MULTIPLIER); }
    public static double arachnidaDamageMultiplier() { return safe(ARACHNIDA_DAMAGE_MULTIPLIER); }
    public static double arachnidaArmorMultiplier() { return safe(ARACHNIDA_ARMOR_MULTIPLIER); }
    public static double arachnidaKnockbackMultiplier() { return safe(ARACHNIDA_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs primitive bolster multipliers (keys had no reachable reader; the constant
    // names below are the ones verified by scripts/audit-mob-multipliers.cjs).
    public static double bolsterHealthMultiplier() { return safe(BOLSTER_HEALTH_MULTIPLIER); }
    public static double bolsterDamageMultiplier() { return safe(BOLSTER_DAMAGE_MULTIPLIER); }
    public static double bolsterArmorMultiplier() { return safe(BOLSTER_ARMOR_MULTIPLIER); }
    public static double bolsterKnockbackMultiplier() { return safe(BOLSTER_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs primitive burrower multipliers (constant names verified by the audit).
    public static double burrowerHealthMultiplier() { return safe(BURROWER_HEALTH_MULTIPLIER); }
    public static double burrowerDamageMultiplier() { return safe(BURROWER_DAMAGE_MULTIPLIER); }
    public static double burrowerArmorMultiplier() { return safe(BURROWER_ARMOR_MULTIPLIER); }
    public static double burrowerKnockbackMultiplier() { return safe(BURROWER_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs primitive devourer multipliers (constant names verified by the audit).
    public static double devourerHealthMultiplier() { return safe(DEVOURER_HEALTH_MULTIPLIER); }
    public static double devourerDamageMultiplier() { return safe(DEVOURER_DAMAGE_MULTIPLIER); }
    public static double devourerArmorMultiplier() { return safe(DEVOURER_ARMOR_MULTIPLIER); }
    public static double devourerKnockbackMultiplier() { return safe(DEVOURER_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs primitive manducater multipliers (constant names verified by the audit).
    public static double manducaterHealthMultiplier() { return safe(MANDUCATER_HEALTH_MULTIPLIER); }
    public static double manducaterDamageMultiplier() { return safe(MANDUCATER_DAMAGE_MULTIPLIER); }
    public static double manducaterArmorMultiplier() { return safe(MANDUCATER_ARMOR_MULTIPLIER); }
    public static double manducaterKnockbackMultiplier() { return safe(MANDUCATER_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs primitive tozoon multipliers (constant names verified by the audit).
    public static double tozoonHealthMultiplier() { return safe(TOZOON_HEALTH_MULTIPLIER); }
    public static double tozoonDamageMultiplier() { return safe(TOZOON_DAMAGE_MULTIPLIER); }
    public static double tozoonArmorMultiplier() { return safe(TOZOON_ARMOR_MULTIPLIER); }
    public static double tozoonKnockbackMultiplier() { return safe(TOZOON_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs.jinjo* (port key: heavyBomber*; the JINJO_* constants are what the
    // three-way audit recognised as original-backed).
    public static double heavyBomberHealthMultiplier() { return safe(JINJO_HEALTH_MULTIPLIER); }
    public static double heavyBomberDamageMultiplier() { return safe(JINJO_DAMAGE_MULTIPLIER); }
    public static double heavyBomberArmorMultiplier() { return safe(JINJO_ARMOR_MULTIPLIER); }

    public static double infsheepHealthMultiplier() { return safe(INFSHEEP_HEALTH_MULTIPLIER); }
    public static double infsheepDamageMultiplier() { return safe(INFSHEEP_DAMAGE_MULTIPLIER); }
    public static double infsheepArmorMultiplier() { return safe(INFSHEEP_ARMOR_MULTIPLIER); }
    public static double infsheepKnockbackMultiplier() { return safe(INFSHEEP_KNOCKBACK_MULTIPLIER); }
    public static double infwolfHealthMultiplier() { return safe(INFWOLF_HEALTH_MULTIPLIER); }
    public static double infwolfDamageMultiplier() { return safe(INFWOLF_DAMAGE_MULTIPLIER); }
    public static double infwolfArmorMultiplier() { return safe(INFWOLF_ARMOR_MULTIPLIER); }
    public static double infwolfKnockbackMultiplier() { return safe(INFWOLF_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs.infsquid* (the assimilated squid).
    private static final ModConfigSpec.DoubleValue INFSQUID_HEALTH_MULTIPLIER = value(
            "srparasites:infsquid", "infsquidHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the assimilated squid.");
    private static final ModConfigSpec.DoubleValue INFSQUID_DAMAGE_MULTIPLIER = value(
            "srparasites:infsquid", "infsquidDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the assimilated squid.");
    private static final ModConfigSpec.DoubleValue INFSQUID_ARMOR_MULTIPLIER = value(
            "srparasites:infsquid", "infsquidArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the assimilated squid.");
    private static final ModConfigSpec.DoubleValue INFSQUID_KNOCKBACK_MULTIPLIER = value(
            "srparasites:infsquid", "infsquidKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the assimilated squid.");

    public static double infsquidHealthMultiplier() { return safe(INFSQUID_HEALTH_MULTIPLIER); }
    public static double infsquidDamageMultiplier() { return safe(INFSQUID_DAMAGE_MULTIPLIER); }
    public static double infsquidArmorMultiplier() { return safe(INFSQUID_ARMOR_MULTIPLIER); }
    public static double infsquidKnockbackMultiplier() { return safe(INFSQUID_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs.infhuman* (the assimilated human).
    private static final ModConfigSpec.DoubleValue INFHUMAN_HEALTH_MULTIPLIER = value(
            "srparasites:infhuman", "infhumanHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the assimilated human.");
    private static final ModConfigSpec.DoubleValue INFHUMAN_DAMAGE_MULTIPLIER = value(
            "srparasites:infhuman", "infhumanDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the assimilated human.");
    private static final ModConfigSpec.DoubleValue INFHUMAN_ARMOR_MULTIPLIER = value(
            "srparasites:infhuman", "infhumanArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the assimilated human.");
    private static final ModConfigSpec.DoubleValue INFHUMAN_KNOCKBACK_MULTIPLIER = value(
            "srparasites:infhuman", "infhumanKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the assimilated human.");

    public static double infhumanHealthMultiplier() { return safe(INFHUMAN_HEALTH_MULTIPLIER); }
    public static double infhumanDamageMultiplier() { return safe(INFHUMAN_DAMAGE_MULTIPLIER); }
    public static double infhumanArmorMultiplier() { return safe(INFHUMAN_ARMOR_MULTIPLIER); }
    public static double infhumanKnockbackMultiplier() { return safe(INFHUMAN_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs.fervillager* (the feral villager).
    private static final ModConfigSpec.DoubleValue FERVILLAGER_HEALTH_MULTIPLIER = value(
            "srparasites:fervillager", "fervillagerHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the feral villager.");
    private static final ModConfigSpec.DoubleValue FERVILLAGER_DAMAGE_MULTIPLIER = value(
            "srparasites:fervillager", "fervillagerDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the feral villager.");
    private static final ModConfigSpec.DoubleValue FERVILLAGER_ARMOR_MULTIPLIER = value(
            "srparasites:fervillager", "fervillagerArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the feral villager.");
    private static final ModConfigSpec.DoubleValue FERVILLAGER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:fervillager", "fervillagerKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the feral villager.");

    public static double fervillagerHealthMultiplier() { return safe(FERVILLAGER_HEALTH_MULTIPLIER); }
    public static double fervillagerDamageMultiplier() { return safe(FERVILLAGER_DAMAGE_MULTIPLIER); }
    public static double fervillagerArmorMultiplier() { return safe(FERVILLAGER_ARMOR_MULTIPLIER); }
    public static double fervillagerKnockbackMultiplier() { return safe(FERVILLAGER_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs.shyco* (the primitive longarms).
    private static final ModConfigSpec.DoubleValue SHYCO_HEALTH_MULTIPLIER = value(
            "srparasites:shyco", "shycoHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the primitive longarms (legacy SRPConfigMobs.shycoHealthMultiplier).");
    private static final ModConfigSpec.DoubleValue SHYCO_DAMAGE_MULTIPLIER = value(
            "srparasites:shyco", "shycoDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the primitive longarms.");
    private static final ModConfigSpec.DoubleValue SHYCO_ARMOR_MULTIPLIER = value(
            "srparasites:shyco", "shycoArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the primitive longarms.");
    private static final ModConfigSpec.DoubleValue SHYCO_KNOCKBACK_MULTIPLIER = value(
            "srparasites:shyco", "shycoKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the primitive longarms.");

    public static double shycoHealthMultiplier() { return safe(SHYCO_HEALTH_MULTIPLIER); }
    public static double shycoDamageMultiplier() { return safe(SHYCO_DAMAGE_MULTIPLIER); }
    public static double shycoArmorMultiplier() { return safe(SHYCO_ARMOR_MULTIPLIER); }
    public static double shycoKnockbackMultiplier() { return safe(SHYCO_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs.hiskeleton* (the hijacked skeleton).
    private static final ModConfigSpec.DoubleValue HISKELETON_HEALTH_MULTIPLIER = value(
            "srparasites:hiskeleton", "hiskeletonHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the hijacked skeleton.");
    private static final ModConfigSpec.DoubleValue HISKELETON_DAMAGE_MULTIPLIER = value(
            "srparasites:hiskeleton", "hiskeletonDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the hijacked skeleton.");
    private static final ModConfigSpec.DoubleValue HISKELETON_ARMOR_MULTIPLIER = value(
            "srparasites:hiskeleton", "hiskeletonArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the hijacked skeleton.");
    private static final ModConfigSpec.DoubleValue HISKELETON_KNOCKBACK_MULTIPLIER = value(
            "srparasites:hiskeleton", "hiskeletonKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the hijacked skeleton.");

    public static double hiskeletonHealthMultiplier() { return safe(HISKELETON_HEALTH_MULTIPLIER); }
    public static double hiskeletonDamageMultiplier() { return safe(HISKELETON_DAMAGE_MULTIPLIER); }
    public static double hiskeletonArmorMultiplier() { return safe(HISKELETON_ARMOR_MULTIPLIER); }
    public static double hiskeletonKnockbackMultiplier() { return safe(HISKELETON_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs.marcow* (the marauderized cow).
    private static final ModConfigSpec.DoubleValue MARCOW_HEALTH_MULTIPLIER = value(
            "srparasites:marcow", "marcowHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the marauderized cow.");
    private static final ModConfigSpec.DoubleValue MARCOW_DAMAGE_MULTIPLIER = value(
            "srparasites:marcow", "marcowDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the marauderized cow.");
    private static final ModConfigSpec.DoubleValue MARCOW_ARMOR_MULTIPLIER = value(
            "srparasites:marcow", "marcowArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the marauderized cow.");
    private static final ModConfigSpec.DoubleValue MARCOW_KNOCKBACK_MULTIPLIER = value(
            "srparasites:marcow", "marcowKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the marauderized cow.");

    public static double marcowHealthMultiplier() { return safe(MARCOW_HEALTH_MULTIPLIER); }
    public static double marcowDamageMultiplier() { return safe(MARCOW_DAMAGE_MULTIPLIER); }
    public static double marcowArmorMultiplier() { return safe(MARCOW_ARMOR_MULTIPLIER); }
    public static double marcowKnockbackMultiplier() { return safe(MARCOW_KNOCKBACK_MULTIPLIER); }

    // Legacy SRPConfigMobs.host* (the assimilated host).
    private static final ModConfigSpec.DoubleValue HOST_HEALTH_MULTIPLIER = value(
            "srparasites:host", "hostHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the host parasite (legacy SRPConfigMobs.hostHealthMultiplier).");
    private static final ModConfigSpec.DoubleValue HOST_DAMAGE_MULTIPLIER = value(
            "srparasites:host", "hostDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for the host parasite.");
    private static final ModConfigSpec.DoubleValue HOST_ARMOR_MULTIPLIER = value(
            "srparasites:host", "hostArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the host parasite.");
    private static final ModConfigSpec.DoubleValue HOST_KNOCKBACK_MULTIPLIER = value(
            "srparasites:host", "hostKDResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the host parasite.");

    public static double hostHealthMultiplier() { return safe(HOST_HEALTH_MULTIPLIER); }
    public static double hostDamageMultiplier() { return safe(HOST_DAMAGE_MULTIPLIER); }
    public static double hostArmorMultiplier() { return safe(HOST_ARMOR_MULTIPLIER); }
    public static double hostKnockbackMultiplier() { return safe(HOST_KNOCKBACK_MULTIPLIER); }

    public static double dorpaHealthMultiplier() { return safe(DORPA_HEALTH_MULTIPLIER); }
    public static double dorpaDamageMultiplier() { return safe(DORPA_DAMAGE_MULTIPLIER); }
    public static double dorpaArmorMultiplier() { return safe(DORPA_ARMOR_MULTIPLIER); }
    public static double dorpaKnockbackMultiplier() { return safe(DORPA_KNOCKBACK_MULTIPLIER); }

    private static final ModConfigSpec.DoubleValue ARACHNIDA_HEALTH_MULTIPLIER = value(
            "srparasites:arachnida", "arachnidaHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier shared by Primitive and Adapted Arachnida.");
    private static final ModConfigSpec.DoubleValue ARACHNIDA_DAMAGE_MULTIPLIER = value(
            "srparasites:arachnida", "arachnidaDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier shared by Primitive and Adapted Arachnida.");
    private static final ModConfigSpec.DoubleValue ARACHNIDA_ARMOR_MULTIPLIER = value(
            "srparasites:arachnida", "arachnidaArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier shared by Primitive and Adapted Arachnida.");
    private static final ModConfigSpec.DoubleValue ARACHNIDA_KNOCKBACK_MULTIPLIER = value(
            "srparasites:arachnida", "arachnidaKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier shared by Primitive and Adapted Arachnida.");
    private static final ModConfigSpec.DoubleValue ADAPTED_ARACHNIDA_ADDITIONAL_HEALTH = value(
            "srparasites:arachnida", "adaptedArachnidaAdditionalHealth", 45.0D, 0.01D, 100.0D,
            "Additional health for the Adapted Arachnida.");
    private static final ModConfigSpec.DoubleValue ADAPTED_ARACHNIDA_ADDITIONAL_DAMAGE = value(
            "srparasites:arachnida", "adaptedArachnidaAdditionalDamage", 15.0D, 0.01D, 100.0D,
            "Additional attack damage for the Adapted Arachnida.");
    private static final ModConfigSpec.DoubleValue ADAPTED_ARACHNIDA_ADDITIONAL_ARMOR = value(
            "srparasites:arachnida", "adaptedArachnidaAdditionalArmor", 10.0D, 0.01D, 100.0D,
            "Additional armor for the Adapted Arachnida.");
    private static final ModConfigSpec.DoubleValue ADAPTED_ARACHNIDA_ADDITIONAL_KNOCKBACK = value(
            "srparasites:arachnida", "adaptedArachnidaAdditionalKnockbackResistance", 0.2D, 0.01D, 100.0D,
            "Additional knockback resistance for the Adapted Arachnida.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ADAPTED_ARACHNIDA_ORB_EFFECTS = stringList(
            "srparasites:arachnida", "adaptedArachnidaOrbEffects", List.of(
                    "0;15;2;minecraft:hunger;0;0",
                    "0;35;2;csrp:needler;0;0",
                    "0;15;2;minecraft:blindness;0;0"),
            "Adapted Arachnida scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);

    private static final ModConfigSpec.DoubleValue VISCERA_HEALTH_MULTIPLIER = value(
            "srparasites:viscera", "primitiveVisceraHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for Primitive Viscera.");
    private static final ModConfigSpec.DoubleValue VISCERA_DAMAGE_MULTIPLIER = value(
            "srparasites:viscera", "primitiveVisceraDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for Primitive Viscera.");
    private static final ModConfigSpec.DoubleValue VISCERA_ARMOR_MULTIPLIER = value(
            "srparasites:viscera", "primitiveVisceraArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for Primitive Viscera.");
    private static final ModConfigSpec.DoubleValue VISCERA_KNOCKBACK_MULTIPLIER = value(
            "srparasites:viscera", "primitiveVisceraKnockbackResistanceMultiplier",
            1.0D, 0.01D, 100.0D, "Knockback-resistance multiplier for Primitive Viscera.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> VISCERA_ORB_EFFECTS = stringList(
            "srparasites:viscera", "primitiveVisceraOrbEffects", List.of(
                    "0;15;1;minecraft:hunger;0;0",
                    "0;15;1;minecraft:slowness;0;0"),
            "Primitive Viscera scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);

    private static final ModConfigSpec.DoubleValue BOLSTER_HEALTH_MULTIPLIER = value(
            "srparasites:bolster", "primitiveBolsterHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Bolster.");
    private static final ModConfigSpec.DoubleValue BOLSTER_DAMAGE_MULTIPLIER = value(
            "srparasites:bolster", "primitiveBolsterDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Bolster.");
    private static final ModConfigSpec.DoubleValue BOLSTER_ARMOR_MULTIPLIER = value(
            "srparasites:bolster", "primitiveBolsterArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Bolster.");
    private static final ModConfigSpec.DoubleValue BOLSTER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:bolster", "primitiveBolsterKnockbackResistanceMultiplier",
            1.0D, 0.01D, 100.0D, "Knockback resistance multiplier for the Primitive Bolster.");
    private static final ModConfigSpec.IntValue BOLSTER_BUFF_COOLDOWN = intValue(
            "srparasites:bolster", "primitiveBolsterBuffCooldownSeconds", 30, 0, 100,
            "Cooldown in seconds between Primitive Bolster area buffs.");
    private static final ModConfigSpec.IntValue BOLSTER_BUFF_RANGE = intValue(
            "srparasites:bolster", "primitiveBolsterBuffRange", 16, 0, 100,
            "Range of the Primitive Bolster area buff.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BOLSTER_EFFECTS = stringList(
            "srparasites:bolster", "primitiveBolsterEffects",
            List.of("30;1;minecraft:regeneration"),
            "Primitive Bolster area effects: seconds;amplifier;effect_id.",
            MobsConfig::validAreaEffect);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BOLSTER_ORB_EFFECTS = stringList(
            "srparasites:bolster", "primitiveBolsterOrbEffects", List.of(
                    "0;15;1;minecraft:hunger;0;0",
                    "2;30;1;minecraft:speed;0;0"),
            "Primitive Bolster scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);

    private static final ModConfigSpec.DoubleValue MANDUCATER_HEALTH_MULTIPLIER = value(
            "srparasites:manducater", "primitiveManducaterHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Manducater.");
    private static final ModConfigSpec.DoubleValue MANDUCATER_DAMAGE_MULTIPLIER = value(
            "srparasites:manducater", "primitiveManducaterDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Manducater.");
    private static final ModConfigSpec.DoubleValue MANDUCATER_ARMOR_MULTIPLIER = value(
            "srparasites:manducater", "primitiveManducaterArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Manducater.");
    private static final ModConfigSpec.DoubleValue MANDUCATER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:manducater", "primitiveManducaterKnockbackResistanceMultiplier",
            1.0D, 0.01D, 100.0D, "Knockback resistance multiplier for the Primitive Manducater.");
    private static final ModConfigSpec.DoubleValue MANDUCATER_NEEDED_HEALTH = value(
            "srparasites:manducater", "manducaterNeededHealth", 0.70D, 0.0D, 1.0D,
            "Health ratio needed for a Primitive Manducater to camouflage.");
    private static final ModConfigSpec.DoubleValue MANDUCATER_NEEDED_TIME = value(
            "srparasites:manducater", "manducaterNeededTime", 15.0D, 1.0D, 100.0D,
            "Camouflage charge time in periodic checks.");
    private static final ModConfigSpec.DoubleValue MANDUCATER_STEALTH_DAMAGE = value(
            "srparasites:manducater", "manducaterStealthDamageMultiplier", 2.0D, 0.01D, 100.0D,
            "Damage multiplier for a camouflaged Primitive Manducater attack.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MANDUCATER_ORB_EFFECTS = stringList(
            "srparasites:manducater", "primitiveManducaterOrbEffects", List.of(
                    "0;15;1;minecraft:hunger;0;0",
                    "1;20;1;minecraft:invisibility;2;2"),
            "Primitive Manducater scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ModConfigSpec.DoubleValue DEVOURER_HEALTH_MULTIPLIER = value(
            "srparasites:devourer", "primitiveDevourerHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Devourer.");
    private static final ModConfigSpec.DoubleValue DEVOURER_DAMAGE_MULTIPLIER = value(
            "srparasites:devourer", "primitiveDevourerDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Devourer.");
    private static final ModConfigSpec.DoubleValue DEVOURER_ARMOR_MULTIPLIER = value(
            "srparasites:devourer", "primitiveDevourerArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Devourer.");
    private static final ModConfigSpec.DoubleValue DEVOURER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:devourer", "primitiveDevourerKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback resistance multiplier for the Primitive Devourer.");
    private static final ModConfigSpec.BooleanValue DEVOURER_WATER_PLACEMENT = booleanValue(
            "srparasites:devourer", "devourerWaterPlacement", true,
            "Whether Devourers replace blocks they break with water.");
    private static final ModConfigSpec.DoubleValue BURROWER_HEALTH_MULTIPLIER = value(
            "srparasites:burrower", "primitiveBurrowerHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Burrower.");
    private static final ModConfigSpec.DoubleValue BURROWER_DAMAGE_MULTIPLIER = value(
            "srparasites:burrower", "primitiveBurrowerDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Burrower.");
    private static final ModConfigSpec.DoubleValue BURROWER_ARMOR_MULTIPLIER = value(
            "srparasites:burrower", "primitiveBurrowerArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Burrower.");
    private static final ModConfigSpec.DoubleValue BURROWER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:burrower", "primitiveBurrowerKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback resistance multiplier for the Primitive Burrower.");
    private static final ModConfigSpec.DoubleValue ADAPTED_BURROWER_ADDITIONAL_HEALTH = value(
            "srparasites:burrower", "adaptedBurrowerAdditionalHealth", 50.0D, 0.01D, 100.0D,
            "Legacy Adapted Burrower health setting; SRP 1.10.7 defines it but does not use it.");
    private static final ModConfigSpec.DoubleValue ADAPTED_BURROWER_ADDITIONAL_DAMAGE = value(
            "srparasites:burrower", "adaptedBurrowerAdditionalDamage", 12.0D, 0.01D, 100.0D,
            "Legacy Adapted Burrower damage setting; SRP 1.10.7 defines it but does not use it.");
    private static final ModConfigSpec.DoubleValue ADAPTED_BURROWER_ADDITIONAL_ARMOR = value(
            "srparasites:burrower", "adaptedBurrowerAdditionalArmor", 7.0D, 0.01D, 100.0D,
            "Legacy Adapted Burrower armor setting; SRP 1.10.7 defines it but does not use it.");
    private static final ModConfigSpec.DoubleValue ADAPTED_BURROWER_ADDITIONAL_KNOCKBACK = value(
            "srparasites:burrower", "adaptedBurrowerAdditionalKnockbackResistance", 0.3D, 0.01D, 100.0D,
            "Legacy Adapted Burrower knockback setting; SRP 1.10.7 defines it but does not use it.");
    private static final ModConfigSpec.DoubleValue TOZOON_HEALTH_MULTIPLIER = value(
            "srparasites:tozoon", "primitiveTozoonHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Tozoon.");
    private static final ModConfigSpec.DoubleValue TOZOON_DAMAGE_MULTIPLIER = value(
            "srparasites:tozoon", "primitiveTozoonDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Tozoon.");
    private static final ModConfigSpec.DoubleValue TOZOON_ARMOR_MULTIPLIER = value(
            "srparasites:tozoon", "primitiveTozoonArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Tozoon.");
    private static final ModConfigSpec.DoubleValue TOZOON_KNOCKBACK_MULTIPLIER = value(
            "srparasites:tozoon", "primitiveTozoonKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback resistance multiplier for the Primitive Tozoon.");
    private static final ModConfigSpec.DoubleValue ADAPTED_TOZOON_ADDITIONAL_HEALTH = value(
            "srparasites:tozoon", "adaptedTozoonAdditionalHealth", 70.0D, 0.01D, 100.0D,
            "Additional health for the Adapted Tozoon.");
    private static final ModConfigSpec.DoubleValue ADAPTED_TOZOON_ADDITIONAL_DAMAGE = value(
            "srparasites:tozoon", "adaptedTozoonAdditionalDamage", 30.0D, 0.01D, 100.0D,
            "Additional attack damage for the Adapted Tozoon.");
    private static final ModConfigSpec.DoubleValue ADAPTED_TOZOON_ADDITIONAL_ARMOR = value(
            "srparasites:tozoon", "adaptedTozoonAdditionalArmor", 15.0D, 0.01D, 100.0D,
            "Additional armor for the Adapted Tozoon.");
    private static final ModConfigSpec.DoubleValue ADAPTED_TOZOON_ADDITIONAL_KNOCKBACK = value(
            "srparasites:tozoon", "adaptedTozoonAdditionalKnockbackResistance", 0.65D, 0.01D, 100.0D,
            "Original additional knockback-resistance setting for the Adapted Tozoon.");
    private static final ModConfigSpec.DoubleValue REEKER_HEALTH_MULTIPLIER = value(
            "srparasites:reeker", "primitiveReekerHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Reeker.");
    private static final ModConfigSpec.DoubleValue REEKER_DAMAGE_MULTIPLIER = value(
            "srparasites:reeker", "primitiveReekerDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Reeker.");
    private static final ModConfigSpec.DoubleValue REEKER_ARMOR_MULTIPLIER = value(
            "srparasites:reeker", "primitiveReekerArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Reeker.");
    private static final ModConfigSpec.DoubleValue REEKER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:reeker", "primitiveReekerKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback resistance multiplier for the Primitive Reeker.");
    private static final ModConfigSpec.BooleanValue REEKER_RICARDO_ENABLED = booleanValue(
            "srparasites:reeker", "enableRicardoVariant", false,
            "Whether naming a Primitive Reeker Ricardo enables its special variant.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> REEKER_ORB_EFFECTS = stringList(
            "srparasites:reeker", "reekerOrbEffects", List.of(
                    "0;15;1;minecraft:hunger;0;0",
                    "0;15;1;minecraft:nausea;0;0"),
            "Primitive Reeker scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ModConfigSpec.DoubleValue YELLOWEYE_HEALTH_MULTIPLIER = value(
            "srparasites:yelloweye", "primitiveYelloweyeHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Yelloweye.");
    private static final ModConfigSpec.DoubleValue YELLOWEYE_DAMAGE_MULTIPLIER = value(
            "srparasites:yelloweye", "primitiveYelloweyeDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Ranged damage multiplier for the Primitive Yelloweye.");
    private static final ModConfigSpec.DoubleValue YELLOWEYE_ARMOR_MULTIPLIER = value(
            "srparasites:yelloweye", "primitiveYelloweyeArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Yelloweye.");
    private static final ModConfigSpec.DoubleValue YELLOWEYE_KNOCKBACK_MULTIPLIER = value(
            "srparasites:yelloweye", "primitiveYelloweyeKnockbackResistanceMultiplier",
            1.0D, 0.01D, 100.0D, "Knockback resistance multiplier for the Primitive Yelloweye.");
    private static final ModConfigSpec.IntValue YELLOWEYE_POISON_DURATION = intValue(
            "srparasites:yelloweye", "primitiveYelloweyePoisonDuration", 3, 0, 100,
            "Poison duration in seconds for the Primitive Yelloweye spine projectile.");
    private static final ModConfigSpec.IntValue YELLOWEYE_POISON_AMPLIFIER = intValue(
            "srparasites:yelloweye", "primitiveYelloweyePoisonAmplifier", 1, 1, 100,
            "One-based poison amplifier for the Primitive Yelloweye spine projectile.");
    private static final ModConfigSpec.DoubleValue YELLOWEYE_GEAR_DAMAGE = value(
            "srparasites:yelloweye", "primitiveYelloweyeGearDegrade", 0.04D, 0.0D, 1.0D,
            "Fraction of maximum durability removed from armor by a Primitive Yelloweye spine.");
    private static final ModConfigSpec.IntValue YELLOWEYE_MAX_FLIGHT_HEIGHT = intValue(
            "srparasites:yelloweye", "primitiveYelloweyeFlightHeightLimit", 256, 0, 256,
            "Maximum number of air blocks the Primitive Yelloweye may fly above terrain.");
    private static final ModConfigSpec.DoubleValue HOST_BOMB_DAMAGE = value(
            "srparasites:host", "hostBombDamage", 7.0D, 0.0D, 1000.0D,
            "Damage dealt by a Host bomb before minimum damage.");
    private static final ModConfigSpec.DoubleValue HERD_BOMB_DAMAGE = value(
            "srparasites:hostii", "herdBombDamage", 14.0D, 0.0D, 1000.0D,
            "Damage dealt by a Hostii bomb before minimum damage.");
    private static final ModConfigSpec.DoubleValue OMBOO_BOMB_DAMAGE = value(
            "srparasites:bomber_light", "lightBomberBombDamage", 20.0D, 0.0D, 1000.0D,
            "Damage dealt by a Light Bomber bomb before minimum damage.");
    private static final ModConfigSpec.IntValue OMBOO_MAX_Y = intValue(
            "srparasites:bomber_light", "lightBomberFlightHeightLimit", 256, 0, 256,
            "Maximum number of air blocks the Light Bomber may fly above terrain.");
    private static final ModConfigSpec.BooleanValue OMBOO_GRIEFING = booleanValue(
            "srparasites:bomber_light", "lightBomberGriefing", true,
            "Whether Light Bomber explosions may destroy blocks when mobGriefing is enabled.");
    private static final ModConfigSpec.DoubleValue OVERSEER_HEALTH_MULTIPLIER = value(
            "srparasites:overseer", "overseerHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Overseer.");
    private static final ModConfigSpec.DoubleValue OVERSEER_DAMAGE_MULTIPLIER = value(
            "srparasites:overseer", "overseerDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Melee and projectile damage multiplier for the Overseer.");
    private static final ModConfigSpec.DoubleValue OVERSEER_ARMOR_MULTIPLIER = value(
            "srparasites:overseer", "overseerArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Overseer.");
    private static final ModConfigSpec.DoubleValue OVERSEER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:overseer", "overseerKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the Overseer.");
    private static final ModConfigSpec.IntValue OVERSEER_SUMMON_COOLDOWN = intValue(
            "srparasites:overseer", "overseerSummoningCooldown", 10, 0, 100,
            "Legacy Overseer summon charge value; SRP 1.10.8 consumes it directly as ticks.");
    private static final ModConfigSpec.IntValue OVERSEER_TOTAL_ACTIVE_MOBS = intValue(
            "srparasites:overseer", "overseerTotalActiveMobs", 6, 0, 100,
            "Maximum total summon-capacity points controlled by an Overseer.");
    private static final ModConfigSpec.IntValue OVERSEER_SUMMON_LIMIT = intValue(
            "srparasites:overseer", "overseerSummonLimit", 6, 0, 10000,
            "Maximum successful biomass launches in one Overseer summon cast.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> OVERSEER_SUMMON_MOBS = stringList(
            "srparasites:overseer", "overseerMobList", List.of(
                    "srparasites:rupter;1;1", "srparasites:grunt;0.5;1"),
            "Overseer summon table: entity_id;chance;capacity_cost.", MobsConfig::validSummonMobEntry);
    private static final ModConfigSpec.IntValue OVERSEER_MAX_Y = intValue(
            "srparasites:overseer", "overseerFlightHeightLimit", 256, 0, 256,
            "Maximum number of air blocks the Overseer may fly above terrain.");
    private static final ModConfigSpec.DoubleValue VIGILANTE_HEALTH_MULTIPLIER = value(
            "srparasites:vigilante", "vigilanteHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Vigilante.");
    private static final ModConfigSpec.DoubleValue VIGILANTE_DAMAGE_MULTIPLIER = value(
            "srparasites:vigilante", "vigilanteDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Melee damage multiplier for the Vigilante.");
    private static final ModConfigSpec.DoubleValue VIGILANTE_RANGED_DAMAGE_MULTIPLIER = value(
            "srparasites:vigilante", "vigilanteRangedDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Ranged damage multiplier for the Vigilante.");
    private static final ModConfigSpec.DoubleValue VIGILANTE_ARMOR_MULTIPLIER = value(
            "srparasites:vigilante", "vigilanteArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Vigilante.");
    private static final ModConfigSpec.DoubleValue VIGILANTE_KNOCKBACK_MULTIPLIER = value(
            "srparasites:vigilante", "vigilanteKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the Vigilante.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> VIGILANTE_ORB_EFFECTS = stringList(
            "srparasites:vigilante", "vigilanteOrbEffects", List.of(
                    "0;15;3;minecraft:hunger;0;0",
                    "0;70;3;csrp:needler;0;0",
                    "0;15;3;minecraft:mining_fatigue;0;0",
                    "2;30;3;minecraft:speed;0;0"),
            "Vigilante scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ModConfigSpec.DoubleValue WARDEN_HEALTH_MULTIPLIER = value(
            "srparasites:warden", "wardenHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Warden.");
    private static final ModConfigSpec.DoubleValue WARDEN_DAMAGE_MULTIPLIER = value(
            "srparasites:warden", "wardenDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Melee and skill damage multiplier for the Warden.");
    private static final ModConfigSpec.DoubleValue WARDEN_ARMOR_MULTIPLIER = value(
            "srparasites:warden", "wardenArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Warden.");
    private static final ModConfigSpec.DoubleValue WARDEN_KNOCKBACK_MULTIPLIER = value(
            "srparasites:warden", "wardenKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the Warden.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> WARDEN_ORB_EFFECTS = stringList(
            "srparasites:warden", "wardenOrbEffects", List.of(
                    "0;15;3;minecraft:hunger;0;0",
                    "0;70;3;csrp:needler;0;0",
                    "0;15;3;minecraft:mining_fatigue;0;0",
                    "2;30;3;minecraft:absorption;0;0"),
            "Warden scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MONARCH_ORB_EFFECTS = stringList(
            "srparasites:monarch", "monarchOrbEffects", List.of(
                    "0;15;3;minecraft:hunger;0;0",
                    "0;70;3;csrp:needler;0;0",
                    "0;15;3;minecraft:mining_fatigue;0;0",
                    "0;15;3;minecraft:wither;0;0"),
            "Monarch scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ModConfigSpec.DoubleValue JINJO_EXPLOSION_MULTIPLIER = value(
            "srparasites:bomber_heavy", "heavyBomberExplosionMultiplier", 6.0D, 0.0D, 100.0D,
            "Multiplier applied to Heavy Bomber attack damage by its bomb.");
    private static final ModConfigSpec.DoubleValue JINJO_HEALTH_MULTIPLIER = value(
            "srparasites:bomber_heavy", "heavyBomberHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Heavy Bomber.");
    private static final ModConfigSpec.DoubleValue JINJO_DAMAGE_MULTIPLIER = value(
            "srparasites:bomber_heavy", "heavyBomberDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Heavy Bomber.");
    private static final ModConfigSpec.DoubleValue JINJO_ARMOR_MULTIPLIER = value(
            "srparasites:bomber_heavy", "heavyBomberArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Heavy Bomber.");
    private static final ModConfigSpec.DoubleValue JINJO_KNOCKBACK_MULTIPLIER = value(
            "srparasites:bomber_heavy", "heavyBomberKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the Heavy Bomber.");
    private static final ModConfigSpec.IntValue JINJO_MAX_Y = intValue(
            "srparasites:bomber_heavy", "heavyBomberFlightHeightLimit", 256, 0, 256,
            "Maximum number of air blocks the Heavy Bomber may fly above terrain.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> JINJO_ORB_EFFECTS = stringList(
            "srparasites:bomber_heavy", "heavyBomberOrbEffects", List.of(
                    "0;15;4;minecraft:hunger;0;0",
                    "0;120;4;csrp:needler;0;0",
                    "0;15;4;minecraft:mining_fatigue;0;0",
                    "0;10;4;minecraft:wither;0;0"),
            "Heavy Bomber scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ModConfigSpec.BooleanValue JINJO_GRIEFING = booleanValue(
            "srparasites:bomber_heavy", "heavyBomberGriefing", true,
            "Whether Heavy Bomber explosions may destroy blocks when mobGriefing is enabled.");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> JINJO_MOBS = stringList(
            "srparasites:bomber_heavy", "heavyBomberMobTable", List.of(
                    "csrp:overseer", "csrp:vigilante", "csrp:marauder", "csrp:monarch"),
            "Entity ids available to the spawning Heavy Bomber bomb.",
            value -> value instanceof String id
                    && net.minecraft.resources.ResourceLocation.tryParse(id) != null);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MobsConfig() {
    }

    private static ModConfigSpec.DoubleValue followRange(String category, String name, double defaultValue) {
        BUILDER.push(category);
        ModConfigSpec.DoubleValue value = BUILDER.comment("Follow range for this parasite tier.")
                .defineInRange(name, defaultValue, 0.0D, 128.0D);
        BUILDER.pop();
        return value;
    }

    private static ModConfigSpec.DoubleValue value(String category, String name, double defaultValue,
                                                   double min, double max, String comment) {
        BUILDER.push(category);
        ModConfigSpec.DoubleValue value = BUILDER.comment(comment).defineInRange(name, defaultValue, min, max);
        BUILDER.pop();
        return value;
    }

    private static ModConfigSpec.BooleanValue booleanValue(String category, String name,
                                                            boolean defaultValue, String comment) {
        BUILDER.push(category);
        ModConfigSpec.BooleanValue value = BUILDER.comment(comment).define(name, defaultValue);
        BUILDER.pop();
        return value;
    }

    private static ModConfigSpec.IntValue intValue(String category, String name, int defaultValue,
                                                    int min, int max, String comment) {
        BUILDER.push(category);
        ModConfigSpec.IntValue value = BUILDER.comment(comment).defineInRange(name, defaultValue, min, max);
        BUILDER.pop();
        return value;
    }

    private static ModConfigSpec.ConfigValue<List<? extends String>> stringList(String category, String name,
            List<String> defaults, String comment, java.util.function.Predicate<Object> validator) {
        BUILDER.push(category);
        ModConfigSpec.ConfigValue<List<? extends String>> value = BUILDER.comment(comment)
                .defineList(name, defaults, validator);
        BUILDER.pop();
        return value;
    }

    public static double followRange(LivingEntity entity) {
        if (entity instanceof WorkerEntity) return -1.0D;
        if (entity instanceof ArchitectEntity) return safe(PURE_FOLLOW);
        if (entity instanceof MovingFleshEntity) return safe(ADAPTED_FOLLOW);
        if (entity instanceof PreeminentParasiteEntity) return safe(PREEMINENT_FOLLOW);
        if (entity instanceof AncientParasiteEntity) return safe(ANCIENT_FOLLOW);
        if (entity instanceof PureParasiteEntity) return safe(PURE_FOLLOW);
        if (entity instanceof AdaptedVariantEntity) return safe(ADAPTED_FOLLOW);
        if (entity instanceof AssimilatedParasiteEntity) return safe(INFECTED_FOLLOW);
        if (entity instanceof PrimitiveParasiteEntity) return safe(PRIMITIVE_FOLLOW);
        return -1.0D;
    }

    public static double pureFollowRange() {
        return safe(PURE_FOLLOW);
    }

    public static double preeminentFollowRange() {
        return safe(PREEMINENT_FOLLOW);
    }

    public static double adaptedFollowRange() {
        return safe(ADAPTED_FOLLOW);
    }

    public static boolean rupterPassiveMobAttacking() {
        return safe(RUPTER_ANIMAL_ATTACKING);
    }

    public static float rupterMinimumDamage() {
        return safe(RUPTER_MINIMUM_DAMAGE).floatValue();
    }

    public static int rupterTunnelCost() {
        return safe(RUPTER_TUNNEL_COST);
    }

    public static int rupterTunnelPhase() {
        return safe(RUPTER_TUNNEL_PHASE);
    }

    public static int rupterManglerKills() {
        return safe(RUPTER_MANGLER_KILLS);
    }

    public static float manglerMinimumDamage() {
        return safe(MANGLER_MINIMUM_DAMAGE).floatValue();
    }

    public static float manglerRegeneration() {
        return safe(MANGLER_REGENERATION).floatValue();
    }

    public static int ancientDreadnautMaxY() {
        return safe(ANCIENT_MAX_Y);
    }

    public static int ancientDreadnautMinY() {
        return Math.min(safe(ANCIENT_MIN_Y), safe(ANCIENT_MAX_Y));
    }

    public static int ancientDreadnautPodCooldownTicks() {
        return safe(ANCIENT_POD_COOLDOWN) * 20;
    }

    public static int ancientDreadnautPodNumber() {
        return safe(ANCIENT_POD_NUMBER);
    }

    public static int ancientDreadnautPodMaxMobs() {
        return safe(ANCIENT_POD_MAX_MOBS);
    }

    public static boolean carrierHeavyGriefing() {
        return safe(CARRIER_HEAVY_GRIEFING);
    }

    public static List<? extends String> carrierHeavyMobTable() {
        return safe(CARRIER_HEAVY_MOBS);
    }

    public static boolean carrierLightGriefing() {
        return safe(CARRIER_LIGHT_GRIEFING);
    }

    public static List<? extends String> carrierLightMobTable() {
        return safe(CARRIER_LIGHT_MOBS);
    }

    public static boolean carrierFlyingGriefing() {
        return safe(CARRIER_FLYING_GRIEFING);
    }

    public static int carrierFlyingMaxY() {
        return safe(CARRIER_FLYING_MAX_Y);
    }

    public static List<? extends String> carrierFlyingMobTable() {
        return safe(CARRIER_FLYING_MOBS);
    }

    public static boolean mergeSystemRandom() {
        return safe(MERGE_RANDOM);
    }

    public static double mergeSystemMobHealth() {
        return safe(MERGE_HEALTH);
    }

    public static List<? extends String> mergeSystemMobList() {
        return safe(MERGE_MOB_TABLE);
    }

    public static boolean ancientPodGriefing() {
        return safe(ANCIENT_POD_GRIEFING);
    }

    public static List<? extends String> ancientDreadnautMobList() {
        return safe(ANCIENT_MOB_LIST);
    }

    public static List<? extends String> ancientPodEffects() {
        return safe(ANCIENT_POD_EFFECTS);
    }

    public static double arachnidaHealth() {
        return 35.0D * safe(ARACHNIDA_HEALTH_MULTIPLIER);
    }

    public static double arachnidaDamage() {
        return 15.0D * safe(ARACHNIDA_DAMAGE_MULTIPLIER);
    }

    public static double arachnidaArmor() {
        return 4.0D * safe(ARACHNIDA_ARMOR_MULTIPLIER);
    }

    public static double arachnidaKnockbackResistance() {
        return Math.min(1.0D, 0.2D * safe(ARACHNIDA_KNOCKBACK_MULTIPLIER));
    }

    public static double adaptedArachnidaHealth() {
        return (35.0D + safe(ADAPTED_ARACHNIDA_ADDITIONAL_HEALTH)) * safe(ARACHNIDA_HEALTH_MULTIPLIER);
    }

    public static double adaptedArachnidaDamage() {
        return (15.0D + safe(ADAPTED_ARACHNIDA_ADDITIONAL_DAMAGE)) * safe(ARACHNIDA_DAMAGE_MULTIPLIER);
    }

    public static double adaptedArachnidaArmor() {
        return (4.0D + safe(ADAPTED_ARACHNIDA_ADDITIONAL_ARMOR)) * safe(ARACHNIDA_ARMOR_MULTIPLIER);
    }

    public static double adaptedArachnidaKnockbackResistance() {
        return Math.min(1.0D, (0.8D + safe(ADAPTED_ARACHNIDA_ADDITIONAL_KNOCKBACK))
                * safe(ARACHNIDA_KNOCKBACK_MULTIPLIER));
    }

    public static List<? extends String> adaptedArachnidaOrbEffects() {
        return safe(ADAPTED_ARACHNIDA_ORB_EFFECTS);
    }

    public static double visceraHealth() {
        return 45.0D * safe(VISCERA_HEALTH_MULTIPLIER);
    }

    public static double visceraDamage() {
        return 15.0D * safe(VISCERA_DAMAGE_MULTIPLIER);
    }

    public static double visceraArmor() {
        return 9.0D * safe(VISCERA_ARMOR_MULTIPLIER);
    }

    public static double visceraKnockbackResistance() {
        return Math.min(1.0D, 0.7D * safe(VISCERA_KNOCKBACK_MULTIPLIER));
    }

    public static List<? extends String> visceraOrbEffects() {
        return safe(VISCERA_ORB_EFFECTS);
    }

    public static double bolsterHealth() {
        return 35.0D * safe(BOLSTER_HEALTH_MULTIPLIER);
    }

    public static double bolsterDamage() {
        return 6.0D * safe(BOLSTER_DAMAGE_MULTIPLIER);
    }

    public static double bolsterArmor() {
        return 4.0D * safe(BOLSTER_ARMOR_MULTIPLIER);
    }

    public static double bolsterKnockbackResistance() {
        return Math.min(1.0D, 0.35D * safe(BOLSTER_KNOCKBACK_MULTIPLIER));
    }

    public static int bolsterBuffCooldownTicks() {
        return safe(BOLSTER_BUFF_COOLDOWN) * 20;
    }

    public static int bolsterBuffRange() {
        return safe(BOLSTER_BUFF_RANGE);
    }

    public static List<? extends String> bolsterEffects() {
        return safe(BOLSTER_EFFECTS);
    }

    public static List<? extends String> bolsterOrbEffects() {
        return safe(BOLSTER_ORB_EFFECTS);
    }

    public static double manducaterHealth() {
        return 30.0D * safe(MANDUCATER_HEALTH_MULTIPLIER);
    }

    public static double manducaterDamage() {
        return 12.0D * safe(MANDUCATER_DAMAGE_MULTIPLIER);
    }

    public static double manducaterArmor() {
        return 4.0D * safe(MANDUCATER_ARMOR_MULTIPLIER);
    }

    public static double manducaterKnockbackResistance() {
        return Math.min(1.0D, 0.5D * safe(MANDUCATER_KNOCKBACK_MULTIPLIER));
    }

    public static double manducaterNeededHealth() {
        return safe(MANDUCATER_NEEDED_HEALTH);
    }

    public static double manducaterNeededTime() {
        return safe(MANDUCATER_NEEDED_TIME);
    }

    public static double manducaterStealthDamageMultiplier() {
        return safe(MANDUCATER_STEALTH_DAMAGE);
    }

    public static List<? extends String> manducaterOrbEffects() {
        return safe(MANDUCATER_ORB_EFFECTS);
    }

    public static double devourerHealth() {
        return 60.0D * safe(DEVOURER_HEALTH_MULTIPLIER);
    }

    public static double devourerDamage() {
        return 20.0D * safe(DEVOURER_DAMAGE_MULTIPLIER);
    }

    public static double devourerArmor() {
        return 4.0D * safe(DEVOURER_ARMOR_MULTIPLIER);
    }

    public static double devourerKnockbackResistance() {
        return Math.min(1.0D, safe(DEVOURER_KNOCKBACK_MULTIPLIER));
    }

    public static boolean devourerWaterPlacement() {
        return safe(DEVOURER_WATER_PLACEMENT);
    }

    public static double burrowerHealth() {
        return 45.0D * safe(BURROWER_HEALTH_MULTIPLIER);
    }

    public static double burrowerDamage() {
        return 15.0D * safe(BURROWER_DAMAGE_MULTIPLIER);
    }

    public static double burrowerArmor() {
        return 9.0D * safe(BURROWER_ARMOR_MULTIPLIER);
    }

    public static double burrowerKnockbackResistance() {
        return Math.min(1.0D, 0.7D * safe(BURROWER_KNOCKBACK_MULTIPLIER));
    }

    public static double tozoonHealth() {
        return 45.0D * safe(TOZOON_HEALTH_MULTIPLIER);
    }

    public static double tozoonDamage() {
        return 15.0D * safe(TOZOON_DAMAGE_MULTIPLIER);
    }

    public static double tozoonArmor() {
        return 9.0D * safe(TOZOON_ARMOR_MULTIPLIER);
    }

    public static double tozoonKnockbackResistance() {
        return Math.min(1.0D, safe(TOZOON_KNOCKBACK_MULTIPLIER));
    }

    public static double adaptedTozoonHealth() {
        return (45.0D + safe(ADAPTED_TOZOON_ADDITIONAL_HEALTH)) * safe(TOZOON_HEALTH_MULTIPLIER);
    }

    public static double adaptedTozoonDamage() {
        return (15.0D + safe(ADAPTED_TOZOON_ADDITIONAL_DAMAGE)) * safe(TOZOON_DAMAGE_MULTIPLIER);
    }

    public static double adaptedTozoonArmor() {
        return (9.0D + safe(ADAPTED_TOZOON_ADDITIONAL_ARMOR)) * safe(TOZOON_ARMOR_MULTIPLIER);
    }

    public static double adaptedTozoonConfiguredKnockbackResistance() {
        return (0.7D + safe(ADAPTED_TOZOON_ADDITIONAL_KNOCKBACK)) * safe(TOZOON_KNOCKBACK_MULTIPLIER);
    }

    public static double reekerHealth() {
        return 40.0D * safe(REEKER_HEALTH_MULTIPLIER);
    }

    public static double reekerDamage() {
        return 12.0D * safe(REEKER_DAMAGE_MULTIPLIER);
    }

    public static double reekerArmor() {
        return 12.0D * safe(REEKER_ARMOR_MULTIPLIER);
    }

    public static double reekerKnockbackResistance() {
        return 0.6D * safe(REEKER_KNOCKBACK_MULTIPLIER);
    }

    public static boolean reekerRicardoVariantEnabled() {
        return safe(REEKER_RICARDO_ENABLED);
    }

    public static List<? extends String> reekerOrbEffects() {
        return safe(REEKER_ORB_EFFECTS);
    }

    public static double yelloweyeHealth() {
        return 30.0D * safe(YELLOWEYE_HEALTH_MULTIPLIER);
    }

    public static double yelloweyeArmor() {
        return 3.5D * safe(YELLOWEYE_ARMOR_MULTIPLIER);
    }

    public static double yelloweyeNadeDamage() {
        return 3.5D * safe(YELLOWEYE_ARMOR_MULTIPLIER);
    }

    public static float yelloweyeRangedDamage() {
        return (float) (5.0D * safe(YELLOWEYE_DAMAGE_MULTIPLIER));
    }

    public static double yelloweyeKnockbackResistance() {
        return 0.2D * safe(YELLOWEYE_KNOCKBACK_MULTIPLIER);
    }

    public static int yelloweyePoisonDurationTicks() {
        return safe(YELLOWEYE_POISON_DURATION) * 20;
    }

    public static int yelloweyePoisonAmplifier() {
        return safe(YELLOWEYE_POISON_AMPLIFIER) - 1;
    }

    public static double yelloweyeGearDamage() {
        return safe(YELLOWEYE_GEAR_DAMAGE);
    }

    public static int yelloweyeMaxFlightHeight() {
        return safe(YELLOWEYE_MAX_FLIGHT_HEIGHT);
    }

    public static float hostBombDamage() {
        return safe(HOST_BOMB_DAMAGE).floatValue();
    }

    public static float herdBombDamage() {
        return safe(HERD_BOMB_DAMAGE).floatValue();
    }

    public static float ombooBombDamage() {
        return safe(OMBOO_BOMB_DAMAGE).floatValue();
    }

    public static int ombooMaxY() {
        return safe(OMBOO_MAX_Y);
    }

    public static boolean ombooGriefing() {
        return safe(OMBOO_GRIEFING);
    }

    public static double overseerHealth() {
        return 80.0D * safe(OVERSEER_HEALTH_MULTIPLIER);
    }

    public static double overseerArmor() {
        return 20.0D * safe(OVERSEER_ARMOR_MULTIPLIER);
    }

    public static double overseerMeleeDamage() {
        return 22.0D * safe(OVERSEER_DAMAGE_MULTIPLIER);
    }

    public static float overseerProjectileDamage() {
        return (float) (30.0D * safe(OVERSEER_DAMAGE_MULTIPLIER));
    }

    public static double overseerKnockbackResistance() {
        return 0.4D * safe(OVERSEER_KNOCKBACK_MULTIPLIER);
    }

    public static int overseerSummonCooldownTicks() {
        return safe(OVERSEER_SUMMON_COOLDOWN);
    }

    public static int overseerTotalActiveMobs() {
        return safe(OVERSEER_TOTAL_ACTIVE_MOBS);
    }

    public static int overseerSummonLimit() {
        return safe(OVERSEER_SUMMON_LIMIT);
    }

    public static List<? extends String> overseerSummonMobs() {
        return safe(OVERSEER_SUMMON_MOBS);
    }

    public static int overseerMaxY() {
        return safe(OVERSEER_MAX_Y);
    }

    public static double vigilanteHealth() {
        return 70.0D * safe(VIGILANTE_HEALTH_MULTIPLIER);
    }

    public static double vigilanteArmor() {
        return 25.0D * safe(VIGILANTE_ARMOR_MULTIPLIER);
    }

    public static double vigilanteMeleeDamage() {
        return 23.0D * safe(VIGILANTE_DAMAGE_MULTIPLIER);
    }

    public static float vigilanteRangedDamage() {
        return (float) (27.0D * safe(VIGILANTE_RANGED_DAMAGE_MULTIPLIER));
    }

    public static double vigilanteKnockbackResistance() {
        return Math.min(1.0D, safe(VIGILANTE_KNOCKBACK_MULTIPLIER));
    }

    public static List<? extends String> vigilanteOrbEffects() {
        return safe(VIGILANTE_ORB_EFFECTS);
    }

    public static double wardenHealth() {
        return 80.0D * safe(WARDEN_HEALTH_MULTIPLIER);
    }

    public static double wardenArmor() {
        return 15.0D * safe(WARDEN_ARMOR_MULTIPLIER);
    }

    public static double wardenDamage() {
        return 25.0D * safe(WARDEN_DAMAGE_MULTIPLIER);
    }

    public static double wardenKnockbackResistance() {
        return Math.min(1.0D, safe(WARDEN_KNOCKBACK_MULTIPLIER));
    }

    public static List<? extends String> wardenOrbEffects() {
        return safe(WARDEN_ORB_EFFECTS);
    }

    public static List<? extends String> monarchOrbEffects() {
        return safe(MONARCH_ORB_EFFECTS);
    }

    public static float jinjoExplosionMultiplier() {
        return safe(JINJO_EXPLOSION_MULTIPLIER).floatValue();
    }

    public static double jinjoHealth() {
        return 420.0D * safe(JINJO_HEALTH_MULTIPLIER);
    }

    public static double jinjoArmor() {
        return 15.5D * safe(JINJO_ARMOR_MULTIPLIER);
    }

    public static double jinjoDamage() {
        return 33.0D * safe(JINJO_DAMAGE_MULTIPLIER);
    }

    public static double jinjoKnockbackResistance() {
        return Math.min(1.0D, 0.15D * safe(JINJO_KNOCKBACK_MULTIPLIER));
    }

    public static int jinjoMaxY() {
        return safe(JINJO_MAX_Y);
    }

    public static List<? extends String> jinjoOrbEffects() {
        return safe(JINJO_ORB_EFFECTS);
    }

    public static boolean jinjoGriefing() {
        return safe(JINJO_GRIEFING);
    }

    public static List<? extends String> jinjoMobs() {
        return safe(JINJO_MOBS);
    }

    private static boolean validOrbEffect(Object value) {
        if (!(value instanceof String effect)) {
            return false;
        }
        String[] parts = effect.split(";", -1);
        if (parts.length != 6 || net.minecraft.resources.ResourceLocation.tryParse(parts[3].trim()) == null) {
            return false;
        }
        try {
            for (int index : new int[] {0, 1, 2, 4, 5}) {
                Integer.parseInt(parts[index].trim());
            }
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean validAreaEffect(Object value) {
        if (!(value instanceof String effect)) {
            return false;
        }
        String[] parts = effect.split(";", -1);
        if (parts.length != 3 || net.minecraft.resources.ResourceLocation.tryParse(parts[2].trim()) == null) {
            return false;
        }
        try {
            Integer.parseInt(parts[0].trim());
            Integer.parseInt(parts[1].trim());
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean validMobEntry(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }
        String[] parts = entry.split(";", -1);
        if (parts.length < 1 || net.minecraft.resources.ResourceLocation.tryParse(parts[0].trim()) == null) {
            return false;
        }
        if (parts.length == 1) {
            return true;
        }
        try {
            return Double.parseDouble(parts[1].trim()) >= 0.0D;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean validSpawnTableEntry(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }
        String[] parts = entry.split(";", -1);
        if (parts.length != 3 || net.minecraft.resources.ResourceLocation.tryParse(parts[0].trim()) == null) {
            return false;
        }
        try {
            int maximum = Integer.parseInt(parts[1].trim());
            int minimum = Integer.parseInt(parts[2].trim());
            return minimum >= 0 && maximum >= minimum;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean validSummonMobEntry(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }
        String[] parts = entry.split(";", -1);
        if (parts.length != 3 || net.minecraft.resources.ResourceLocation.tryParse(parts[0].trim()) == null) {
            return false;
        }
        try {
            double chance = Double.parseDouble(parts[1].trim());
            int capacityCost = Integer.parseInt(parts[2].trim());
            return chance >= 0.0D && chance <= 1.0D && capacityCost > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean validMergeMobEntry(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }
        String[] parts = entry.split(";", -1);
        if (parts.length != 2 || net.minecraft.resources.ResourceLocation.tryParse(parts[0].trim()) == null) {
            return false;
        }
        try {
            Integer.parseInt(parts[1].trim());
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
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
