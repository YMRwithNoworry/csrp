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
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class MobsConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue INFECTED_FOLLOW = followRange(
            "parasitePropertiesAssimilated", "infectedFollow", 16.0D);
    private static final ForgeConfigSpec.DoubleValue PRIMITIVE_FOLLOW = followRange(
            "parasitePropertiesPrimitive", "primitiveFollow", 24.0D);
    private static final ForgeConfigSpec.DoubleValue ADAPTED_FOLLOW = followRange(
            "parasitePropertiesAdapted", "adaptedFollow", 32.0D);
    private static final ForgeConfigSpec.DoubleValue PURE_FOLLOW = followRange(
            "parasitePropertiesPure", "pureFollow", 32.0D);
    private static final ForgeConfigSpec.DoubleValue ANCIENT_FOLLOW = followRange(
            "parasitePropertiesAncient", "ancientFollow", 64.0D);
    private static final ForgeConfigSpec.IntValue ANCIENT_MAX_Y = intValue(
            "srparasites:anc_dreadnaut", "ancientDreadnautMaxY", 256, 1, 320,
            "Maximum flight height for the Ancient Dreadnaut.");
    private static final ForgeConfigSpec.IntValue ANCIENT_MIN_Y = intValue(
            "srparasites:anc_dreadnaut", "ancientDreadnautMinY", 7, 0, 320,
            "Minimum flight height for the Ancient Dreadnaut.");
    private static final ForgeConfigSpec.IntValue ANCIENT_POD_COOLDOWN = intValue(
            "srparasites:anc_dreadnaut", "ancientDreadnautPodCooldownSeconds", 12, 1, 256,
            "Cooldown in seconds between Ancient Dreadnaut drop-pod attacks.");
    private static final ForgeConfigSpec.IntValue ANCIENT_POD_NUMBER = intValue(
            "srparasites:anc_dreadnaut", "ancientDreadnautPodNumber", 5, 1, 256,
            "Number of drop pods spawned per Ancient Dreadnaut attack.");
    private static final ForgeConfigSpec.IntValue ANCIENT_POD_MAX_MOBS = intValue(
            "srparasites:anc_dreadnaut", "ancientDreadnautPodMaxMobs", 1, 1, 256,
            "Maximum payload mobs spawned by one Ancient Dreadnaut drop pod.");
    private static final ForgeConfigSpec.BooleanValue ANCIENT_POD_GRIEFING = booleanValue(
            "srparasites:anc_pod", "ancientPodGriefing", false,
            "Whether Ancient Drop Pods may destroy blocks when mobGriefing is enabled.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ANCIENT_MOB_LIST = stringList(
            "srparasites:anc_dreadnaut", "ancientDreadnautMobList", List.of(
                    "csrp:rupter;1", "csrp:rupter;1", "csrp:rupter;1", "csrp:rupter;0.5", "csrp:rupter;0.5",
                    "csrp:grunt;0.7"),
            "Ancient Dreadnaut payload table: entity_id;weight.", MobsConfig::validMobEntry);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ANCIENT_POD_EFFECTS = stringList(
            "srparasites:anc_pod", "ancientPodEffects", List.of("10;-2;minecraft:saturation"),
            "Ancient Drop Pod effects: seconds;amplifier;effect_id.", MobsConfig::validAreaEffect);
    private static final ForgeConfigSpec.DoubleValue PREEMINENT_FOLLOW = followRange(
            "parasitePropertiesPreeminent", "preeminentFollow", 80.0D);

    private static final ForgeConfigSpec.BooleanValue RUPTER_ANIMAL_ATTACKING = booleanValue(
            "srparasites:rupter", "rupterPassiveMobAttacking", true,
            "Whether Rupters may attack passive mobs when evolution phases are disabled.");
    private static final ForgeConfigSpec.DoubleValue RUPTER_MINIMUM_DAMAGE = value(
            "srparasites:rupter", "rupterMinimumDamage", 0.1D, 0.0D, 1024.0D,
            "Minimum damage applied by a Rupter melee hit after armor reduction.");
    private static final ForgeConfigSpec.IntValue RUPTER_TUNNEL_COST = intValue(
            "srparasites:rupter", "rupterTunnelCost", 5, 0, 100,
            "Killcount cost of placing a Buglin Tunnel.");
    private static final ForgeConfigSpec.IntValue RUPTER_TUNNEL_PHASE = intValue(
            "srparasites:rupter", "rupterTunnelPhase", 3, 0, 9,
            "From this creation phase onward, Rupters do not place tunnels.");
    private static final ForgeConfigSpec.IntValue RUPTER_MANGLER_KILLS = intValue(
            "srparasites:rupter", "rupterManglerKills", 30, 0, 1000,
            "Kills required for a Rupter to become a Mangler.");
    private static final ForgeConfigSpec.DoubleValue MANGLER_MINIMUM_DAMAGE = value(
            "srparasites:mangler", "manglerMinimumDamage", 0.3D, 0.0D, 1024.0D,
            "Minimum damage applied by a Mangler melee hit after armor reduction.");
    private static final ForgeConfigSpec.DoubleValue MANGLER_REGENERATION = value(
            "srparasites:mangler", "manglerRegeneration", 14.0D, 0.0D, 1024.0D,
            "Health restored by each Mangler regeneration pulse.");

    private static final ForgeConfigSpec.BooleanValue CARRIER_HEAVY_GRIEFING = booleanValue(
            "srparasites:carrier_heavy", "carrierHeavyGriefing", false,
            "Whether Heavy Carriers may destroy blocks when exploding.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CARRIER_HEAVY_MOBS = stringList(
            "srparasites:carrier_heavy", "carrierHeavyMobTable", List.of(
                    "srparasites:rupter;4;1", "srparasites:buglin;5;2", "srparasites:gnat;6;2"),
            "Heavy Carrier payload table: entity_id;maximum;minimum.", MobsConfig::validSpawnTableEntry);
    private static final ForgeConfigSpec.BooleanValue CARRIER_LIGHT_GRIEFING = booleanValue(
            "srparasites:carrier_light", "carrierLightGriefing", false,
            "Whether Light Carriers may destroy blocks when exploding.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CARRIER_LIGHT_MOBS = stringList(
            "srparasites:carrier_light", "carrierLightMobTable", List.of(
                    "srparasites:rupter;3;2", "srparasites:buglin;4;3", "srparasites:gnat;5;3"),
            "Light Carrier payload table: entity_id;maximum;minimum.", MobsConfig::validSpawnTableEntry);
    private static final ForgeConfigSpec.BooleanValue CARRIER_FLYING_GRIEFING = booleanValue(
            "srparasites:carrier_flying", "carrierFlyingGriefing", false,
            "Whether Flying Carriers may destroy blocks when exploding.");
    private static final ForgeConfigSpec.IntValue CARRIER_FLYING_MAX_Y = intValue(
            "srparasites:carrier_flying", "carrierFlyingMaxY", 256, 1, 320,
            "Maximum flight height for the Flying Carrier.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CARRIER_FLYING_MOBS = stringList(
            "srparasites:carrier_flying", "carrierFlyingMobTable", List.of(
                    "srparasites:rupter;3;1", "srparasites:buglin;3;2"),
            "Flying Carrier payload table: entity_id;maximum;minimum.", MobsConfig::validSpawnTableEntry);
    private static final ForgeConfigSpec.BooleanValue MERGE_RANDOM = booleanValue(
            "merge_System", "mergeSystemRandom", true,
            "Whether Moving Flesh always selects a random entry from its mob table.");
    private static final ForgeConfigSpec.DoubleValue MERGE_HEALTH = value(
            "merge_System", "mergeSystemMobHealth", 0.5D, 0.0D, 1.0D,
            "Health fraction of a primitive spawned by Moving Flesh.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MERGE_MOB_TABLE = stringList(
            "merge_System", "mergeSystemMobList", List.of(
                    "srparasites:pri_summoner;0", "srparasites:pri_longarms;0",
                    "srparasites:pri_reeker;0", "srparasites:pri_manducater;0",
                    "srparasites:pri_bolster;0", "srparasites:pri_yelloweye;0",
                    "srparasites:pri_arachnida;0", "srparasites:pri_vermin;0",
                    "srparasites:pri_tozoon;0"),
            "Moving Flesh merge table: entity_id;merge_value.", MobsConfig::validMergeMobEntry);

    private static final ForgeConfigSpec.DoubleValue ARACHNIDA_HEALTH_MULTIPLIER = value(
            "srparasites:arachnida", "arachnidaHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier shared by Primitive and Adapted Arachnida.");
    private static final ForgeConfigSpec.DoubleValue ARACHNIDA_DAMAGE_MULTIPLIER = value(
            "srparasites:arachnida", "arachnidaDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier shared by Primitive and Adapted Arachnida.");
    private static final ForgeConfigSpec.DoubleValue ARACHNIDA_ARMOR_MULTIPLIER = value(
            "srparasites:arachnida", "arachnidaArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier shared by Primitive and Adapted Arachnida.");
    private static final ForgeConfigSpec.DoubleValue ARACHNIDA_KNOCKBACK_MULTIPLIER = value(
            "srparasites:arachnida", "arachnidaKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier shared by Primitive and Adapted Arachnida.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_ARACHNIDA_ADDITIONAL_HEALTH = value(
            "srparasites:arachnida", "adaptedArachnidaAdditionalHealth", 45.0D, 0.01D, 100.0D,
            "Additional health for the Adapted Arachnida.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_ARACHNIDA_ADDITIONAL_DAMAGE = value(
            "srparasites:arachnida", "adaptedArachnidaAdditionalDamage", 15.0D, 0.01D, 100.0D,
            "Additional attack damage for the Adapted Arachnida.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_ARACHNIDA_ADDITIONAL_ARMOR = value(
            "srparasites:arachnida", "adaptedArachnidaAdditionalArmor", 10.0D, 0.01D, 100.0D,
            "Additional armor for the Adapted Arachnida.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_ARACHNIDA_ADDITIONAL_KNOCKBACK = value(
            "srparasites:arachnida", "adaptedArachnidaAdditionalKnockbackResistance", 0.2D, 0.01D, 100.0D,
            "Additional knockback resistance for the Adapted Arachnida.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ADAPTED_ARACHNIDA_ORB_EFFECTS = stringList(
            "srparasites:arachnida", "adaptedArachnidaOrbEffects", List.of(
                    "0;15;2;minecraft:hunger;0;0",
                    "0;35;2;csrp:needler;0;0",
                    "0;15;2;minecraft:blindness;0;0"),
            "Adapted Arachnida scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);

    private static final ForgeConfigSpec.DoubleValue VISCERA_HEALTH_MULTIPLIER = value(
            "srparasites:viscera", "primitiveVisceraHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for Primitive Viscera.");
    private static final ForgeConfigSpec.DoubleValue VISCERA_DAMAGE_MULTIPLIER = value(
            "srparasites:viscera", "primitiveVisceraDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack-damage multiplier for Primitive Viscera.");
    private static final ForgeConfigSpec.DoubleValue VISCERA_ARMOR_MULTIPLIER = value(
            "srparasites:viscera", "primitiveVisceraArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for Primitive Viscera.");
    private static final ForgeConfigSpec.DoubleValue VISCERA_KNOCKBACK_MULTIPLIER = value(
            "srparasites:viscera", "primitiveVisceraKnockbackResistanceMultiplier",
            1.0D, 0.01D, 100.0D, "Knockback-resistance multiplier for Primitive Viscera.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> VISCERA_ORB_EFFECTS = stringList(
            "srparasites:viscera", "primitiveVisceraOrbEffects", List.of(
                    "0;15;1;minecraft:hunger;0;0",
                    "0;15;1;minecraft:slowness;0;0"),
            "Primitive Viscera scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);

    private static final ForgeConfigSpec.DoubleValue BOLSTER_HEALTH_MULTIPLIER = value(
            "srparasites:bolster", "primitiveBolsterHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Bolster.");
    private static final ForgeConfigSpec.DoubleValue BOLSTER_DAMAGE_MULTIPLIER = value(
            "srparasites:bolster", "primitiveBolsterDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Bolster.");
    private static final ForgeConfigSpec.DoubleValue BOLSTER_ARMOR_MULTIPLIER = value(
            "srparasites:bolster", "primitiveBolsterArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Bolster.");
    private static final ForgeConfigSpec.DoubleValue BOLSTER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:bolster", "primitiveBolsterKnockbackResistanceMultiplier",
            1.0D, 0.01D, 100.0D, "Knockback resistance multiplier for the Primitive Bolster.");
    private static final ForgeConfigSpec.IntValue BOLSTER_BUFF_COOLDOWN = intValue(
            "srparasites:bolster", "primitiveBolsterBuffCooldownSeconds", 30, 0, 100,
            "Cooldown in seconds between Primitive Bolster area buffs.");
    private static final ForgeConfigSpec.IntValue BOLSTER_BUFF_RANGE = intValue(
            "srparasites:bolster", "primitiveBolsterBuffRange", 16, 0, 100,
            "Range of the Primitive Bolster area buff.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOLSTER_EFFECTS = stringList(
            "srparasites:bolster", "primitiveBolsterEffects",
            List.of("30;1;minecraft:regeneration"),
            "Primitive Bolster area effects: seconds;amplifier;effect_id.",
            MobsConfig::validAreaEffect);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOLSTER_ORB_EFFECTS = stringList(
            "srparasites:bolster", "primitiveBolsterOrbEffects", List.of(
                    "0;15;1;minecraft:hunger;0;0",
                    "2;30;1;minecraft:speed;0;0"),
            "Primitive Bolster scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);

    private static final ForgeConfigSpec.DoubleValue MANDUCATER_HEALTH_MULTIPLIER = value(
            "srparasites:manducater", "primitiveManducaterHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Manducater.");
    private static final ForgeConfigSpec.DoubleValue MANDUCATER_DAMAGE_MULTIPLIER = value(
            "srparasites:manducater", "primitiveManducaterDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Manducater.");
    private static final ForgeConfigSpec.DoubleValue MANDUCATER_ARMOR_MULTIPLIER = value(
            "srparasites:manducater", "primitiveManducaterArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Manducater.");
    private static final ForgeConfigSpec.DoubleValue MANDUCATER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:manducater", "primitiveManducaterKnockbackResistanceMultiplier",
            1.0D, 0.01D, 100.0D, "Knockback resistance multiplier for the Primitive Manducater.");
    private static final ForgeConfigSpec.DoubleValue MANDUCATER_NEEDED_HEALTH = value(
            "srparasites:manducater", "manducaterNeededHealth", 0.70D, 0.0D, 1.0D,
            "Health ratio needed for a Primitive Manducater to camouflage.");
    private static final ForgeConfigSpec.DoubleValue MANDUCATER_NEEDED_TIME = value(
            "srparasites:manducater", "manducaterNeededTime", 15.0D, 1.0D, 100.0D,
            "Camouflage charge time in periodic checks.");
    private static final ForgeConfigSpec.DoubleValue MANDUCATER_STEALTH_DAMAGE = value(
            "srparasites:manducater", "manducaterStealthDamageMultiplier", 2.0D, 0.01D, 100.0D,
            "Damage multiplier for a camouflaged Primitive Manducater attack.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MANDUCATER_ORB_EFFECTS = stringList(
            "srparasites:manducater", "primitiveManducaterOrbEffects", List.of(
                    "0;15;1;minecraft:hunger;0;0",
                    "1;20;1;minecraft:invisibility;2;2"),
            "Primitive Manducater scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ForgeConfigSpec.DoubleValue DEVOURER_HEALTH_MULTIPLIER = value(
            "srparasites:devourer", "primitiveDevourerHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Devourer.");
    private static final ForgeConfigSpec.DoubleValue DEVOURER_DAMAGE_MULTIPLIER = value(
            "srparasites:devourer", "primitiveDevourerDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Devourer.");
    private static final ForgeConfigSpec.DoubleValue DEVOURER_ARMOR_MULTIPLIER = value(
            "srparasites:devourer", "primitiveDevourerArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Devourer.");
    private static final ForgeConfigSpec.DoubleValue DEVOURER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:devourer", "primitiveDevourerKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback resistance multiplier for the Primitive Devourer.");
    private static final ForgeConfigSpec.BooleanValue DEVOURER_WATER_PLACEMENT = booleanValue(
            "srparasites:devourer", "devourerWaterPlacement", true,
            "Whether Devourers replace blocks they break with water.");
    private static final ForgeConfigSpec.DoubleValue BURROWER_HEALTH_MULTIPLIER = value(
            "srparasites:burrower", "primitiveBurrowerHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Burrower.");
    private static final ForgeConfigSpec.DoubleValue BURROWER_DAMAGE_MULTIPLIER = value(
            "srparasites:burrower", "primitiveBurrowerDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Burrower.");
    private static final ForgeConfigSpec.DoubleValue BURROWER_ARMOR_MULTIPLIER = value(
            "srparasites:burrower", "primitiveBurrowerArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Burrower.");
    private static final ForgeConfigSpec.DoubleValue BURROWER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:burrower", "primitiveBurrowerKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback resistance multiplier for the Primitive Burrower.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_BURROWER_ADDITIONAL_HEALTH = value(
            "srparasites:burrower", "adaptedBurrowerAdditionalHealth", 50.0D, 0.01D, 100.0D,
            "Legacy Adapted Burrower health setting; SRP 1.10.7 defines it but does not use it.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_BURROWER_ADDITIONAL_DAMAGE = value(
            "srparasites:burrower", "adaptedBurrowerAdditionalDamage", 12.0D, 0.01D, 100.0D,
            "Legacy Adapted Burrower damage setting; SRP 1.10.7 defines it but does not use it.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_BURROWER_ADDITIONAL_ARMOR = value(
            "srparasites:burrower", "adaptedBurrowerAdditionalArmor", 7.0D, 0.01D, 100.0D,
            "Legacy Adapted Burrower armor setting; SRP 1.10.7 defines it but does not use it.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_BURROWER_ADDITIONAL_KNOCKBACK = value(
            "srparasites:burrower", "adaptedBurrowerAdditionalKnockbackResistance", 0.3D, 0.01D, 100.0D,
            "Legacy Adapted Burrower knockback setting; SRP 1.10.7 defines it but does not use it.");
    private static final ForgeConfigSpec.DoubleValue TOZOON_HEALTH_MULTIPLIER = value(
            "srparasites:tozoon", "primitiveTozoonHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Tozoon.");
    private static final ForgeConfigSpec.DoubleValue TOZOON_DAMAGE_MULTIPLIER = value(
            "srparasites:tozoon", "primitiveTozoonDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Tozoon.");
    private static final ForgeConfigSpec.DoubleValue TOZOON_ARMOR_MULTIPLIER = value(
            "srparasites:tozoon", "primitiveTozoonArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Tozoon.");
    private static final ForgeConfigSpec.DoubleValue TOZOON_KNOCKBACK_MULTIPLIER = value(
            "srparasites:tozoon", "primitiveTozoonKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback resistance multiplier for the Primitive Tozoon.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_TOZOON_ADDITIONAL_HEALTH = value(
            "srparasites:tozoon", "adaptedTozoonAdditionalHealth", 70.0D, 0.01D, 100.0D,
            "Additional health for the Adapted Tozoon.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_TOZOON_ADDITIONAL_DAMAGE = value(
            "srparasites:tozoon", "adaptedTozoonAdditionalDamage", 30.0D, 0.01D, 100.0D,
            "Additional attack damage for the Adapted Tozoon.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_TOZOON_ADDITIONAL_ARMOR = value(
            "srparasites:tozoon", "adaptedTozoonAdditionalArmor", 15.0D, 0.01D, 100.0D,
            "Additional armor for the Adapted Tozoon.");
    private static final ForgeConfigSpec.DoubleValue ADAPTED_TOZOON_ADDITIONAL_KNOCKBACK = value(
            "srparasites:tozoon", "adaptedTozoonAdditionalKnockbackResistance", 0.65D, 0.01D, 100.0D,
            "Original additional knockback-resistance setting for the Adapted Tozoon.");
    private static final ForgeConfigSpec.DoubleValue REEKER_HEALTH_MULTIPLIER = value(
            "srparasites:reeker", "primitiveReekerHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Reeker.");
    private static final ForgeConfigSpec.DoubleValue REEKER_DAMAGE_MULTIPLIER = value(
            "srparasites:reeker", "primitiveReekerDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Primitive Reeker.");
    private static final ForgeConfigSpec.DoubleValue REEKER_ARMOR_MULTIPLIER = value(
            "srparasites:reeker", "primitiveReekerArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Reeker.");
    private static final ForgeConfigSpec.DoubleValue REEKER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:reeker", "primitiveReekerKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback resistance multiplier for the Primitive Reeker.");
    private static final ForgeConfigSpec.BooleanValue REEKER_RICARDO_ENABLED = booleanValue(
            "srparasites:reeker", "enableRicardoVariant", false,
            "Whether naming a Primitive Reeker Ricardo enables its special variant.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> REEKER_ORB_EFFECTS = stringList(
            "srparasites:reeker", "reekerOrbEffects", List.of(
                    "0;15;1;minecraft:hunger;0;0",
                    "0;15;1;minecraft:nausea;0;0"),
            "Primitive Reeker scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ForgeConfigSpec.DoubleValue YELLOWEYE_HEALTH_MULTIPLIER = value(
            "srparasites:yelloweye", "primitiveYelloweyeHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Primitive Yelloweye.");
    private static final ForgeConfigSpec.DoubleValue YELLOWEYE_DAMAGE_MULTIPLIER = value(
            "srparasites:yelloweye", "primitiveYelloweyeDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Ranged damage multiplier for the Primitive Yelloweye.");
    private static final ForgeConfigSpec.DoubleValue YELLOWEYE_ARMOR_MULTIPLIER = value(
            "srparasites:yelloweye", "primitiveYelloweyeArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Primitive Yelloweye.");
    private static final ForgeConfigSpec.DoubleValue YELLOWEYE_KNOCKBACK_MULTIPLIER = value(
            "srparasites:yelloweye", "primitiveYelloweyeKnockbackResistanceMultiplier",
            1.0D, 0.01D, 100.0D, "Knockback resistance multiplier for the Primitive Yelloweye.");
    private static final ForgeConfigSpec.IntValue YELLOWEYE_POISON_DURATION = intValue(
            "srparasites:yelloweye", "primitiveYelloweyePoisonDuration", 3, 0, 100,
            "Poison duration in seconds for the Primitive Yelloweye spine projectile.");
    private static final ForgeConfigSpec.IntValue YELLOWEYE_POISON_AMPLIFIER = intValue(
            "srparasites:yelloweye", "primitiveYelloweyePoisonAmplifier", 1, 1, 100,
            "One-based poison amplifier for the Primitive Yelloweye spine projectile.");
    private static final ForgeConfigSpec.DoubleValue YELLOWEYE_GEAR_DAMAGE = value(
            "srparasites:yelloweye", "primitiveYelloweyeGearDegrade", 0.04D, 0.0D, 1.0D,
            "Fraction of maximum durability removed from armor by a Primitive Yelloweye spine.");
    private static final ForgeConfigSpec.IntValue YELLOWEYE_MAX_FLIGHT_HEIGHT = intValue(
            "srparasites:yelloweye", "primitiveYelloweyeFlightHeightLimit", 256, 0, 256,
            "Maximum number of air blocks the Primitive Yelloweye may fly above terrain.");
    private static final ForgeConfigSpec.DoubleValue HOST_BOMB_DAMAGE = value(
            "srparasites:host", "hostBombDamage", 7.0D, 0.0D, 1000.0D,
            "Damage dealt by a Host bomb before minimum damage.");
    private static final ForgeConfigSpec.DoubleValue HERD_BOMB_DAMAGE = value(
            "srparasites:hostii", "herdBombDamage", 14.0D, 0.0D, 1000.0D,
            "Damage dealt by a Hostii bomb before minimum damage.");
    private static final ForgeConfigSpec.DoubleValue OMBOO_BOMB_DAMAGE = value(
            "srparasites:bomber_light", "lightBomberBombDamage", 20.0D, 0.0D, 1000.0D,
            "Damage dealt by a Light Bomber bomb before minimum damage.");
    private static final ForgeConfigSpec.IntValue OMBOO_MAX_Y = intValue(
            "srparasites:bomber_light", "lightBomberFlightHeightLimit", 256, 0, 256,
            "Maximum number of air blocks the Light Bomber may fly above terrain.");
    private static final ForgeConfigSpec.BooleanValue OMBOO_GRIEFING = booleanValue(
            "srparasites:bomber_light", "lightBomberGriefing", true,
            "Whether Light Bomber explosions may destroy blocks when mobGriefing is enabled.");
    private static final ForgeConfigSpec.DoubleValue OVERSEER_HEALTH_MULTIPLIER = value(
            "srparasites:overseer", "overseerHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Overseer.");
    private static final ForgeConfigSpec.DoubleValue OVERSEER_DAMAGE_MULTIPLIER = value(
            "srparasites:overseer", "overseerDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Melee and projectile damage multiplier for the Overseer.");
    private static final ForgeConfigSpec.DoubleValue OVERSEER_ARMOR_MULTIPLIER = value(
            "srparasites:overseer", "overseerArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Overseer.");
    private static final ForgeConfigSpec.DoubleValue OVERSEER_KNOCKBACK_MULTIPLIER = value(
            "srparasites:overseer", "overseerKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the Overseer.");
    private static final ForgeConfigSpec.IntValue OVERSEER_SUMMON_COOLDOWN = intValue(
            "srparasites:overseer", "overseerSummoningCooldown", 10, 0, 100,
            "Legacy Overseer summon charge value; SRP 1.10.8 consumes it directly as ticks.");
    private static final ForgeConfigSpec.IntValue OVERSEER_TOTAL_ACTIVE_MOBS = intValue(
            "srparasites:overseer", "overseerTotalActiveMobs", 6, 0, 100,
            "Maximum total summon-capacity points controlled by an Overseer.");
    private static final ForgeConfigSpec.IntValue OVERSEER_SUMMON_LIMIT = intValue(
            "srparasites:overseer", "overseerSummonLimit", 6, 0, 10000,
            "Maximum successful biomass launches in one Overseer summon cast.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> OVERSEER_SUMMON_MOBS = stringList(
            "srparasites:overseer", "overseerMobList", List.of(
                    "srparasites:rupter;1;1", "srparasites:grunt;0.5;1"),
            "Overseer summon table: entity_id;chance;capacity_cost.", MobsConfig::validSummonMobEntry);
    private static final ForgeConfigSpec.IntValue OVERSEER_MAX_Y = intValue(
            "srparasites:overseer", "overseerFlightHeightLimit", 256, 0, 256,
            "Maximum number of air blocks the Overseer may fly above terrain.");
    private static final ForgeConfigSpec.DoubleValue VIGILANTE_HEALTH_MULTIPLIER = value(
            "srparasites:vigilante", "vigilanteHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Vigilante.");
    private static final ForgeConfigSpec.DoubleValue VIGILANTE_DAMAGE_MULTIPLIER = value(
            "srparasites:vigilante", "vigilanteDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Melee damage multiplier for the Vigilante.");
    private static final ForgeConfigSpec.DoubleValue VIGILANTE_RANGED_DAMAGE_MULTIPLIER = value(
            "srparasites:vigilante", "vigilanteRangedDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Ranged damage multiplier for the Vigilante.");
    private static final ForgeConfigSpec.DoubleValue VIGILANTE_ARMOR_MULTIPLIER = value(
            "srparasites:vigilante", "vigilanteArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Vigilante.");
    private static final ForgeConfigSpec.DoubleValue VIGILANTE_KNOCKBACK_MULTIPLIER = value(
            "srparasites:vigilante", "vigilanteKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the Vigilante.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> VIGILANTE_ORB_EFFECTS = stringList(
            "srparasites:vigilante", "vigilanteOrbEffects", List.of(
                    "0;15;3;minecraft:hunger;0;0",
                    "0;70;3;csrp:needler;0;0",
                    "0;15;3;minecraft:mining_fatigue;0;0",
                    "2;30;3;minecraft:speed;0;0"),
            "Vigilante scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ForgeConfigSpec.DoubleValue WARDEN_HEALTH_MULTIPLIER = value(
            "srparasites:warden", "wardenHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Warden.");
    private static final ForgeConfigSpec.DoubleValue WARDEN_DAMAGE_MULTIPLIER = value(
            "srparasites:warden", "wardenDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Melee and skill damage multiplier for the Warden.");
    private static final ForgeConfigSpec.DoubleValue WARDEN_ARMOR_MULTIPLIER = value(
            "srparasites:warden", "wardenArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Warden.");
    private static final ForgeConfigSpec.DoubleValue WARDEN_KNOCKBACK_MULTIPLIER = value(
            "srparasites:warden", "wardenKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the Warden.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> WARDEN_ORB_EFFECTS = stringList(
            "srparasites:warden", "wardenOrbEffects", List.of(
                    "0;15;3;minecraft:hunger;0;0",
                    "0;70;3;csrp:needler;0;0",
                    "0;15;3;minecraft:mining_fatigue;0;0",
                    "2;30;3;minecraft:absorption;0;0"),
            "Warden scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MONARCH_ORB_EFFECTS = stringList(
            "srparasites:monarch", "monarchOrbEffects", List.of(
                    "0;15;3;minecraft:hunger;0;0",
                    "0;70;3;csrp:needler;0;0",
                    "0;15;3;minecraft:mining_fatigue;0;0",
                    "0;15;3;minecraft:wither;0;0"),
            "Monarch scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ForgeConfigSpec.DoubleValue JINJO_EXPLOSION_MULTIPLIER = value(
            "srparasites:bomber_heavy", "heavyBomberExplosionMultiplier", 6.0D, 0.0D, 100.0D,
            "Multiplier applied to Heavy Bomber attack damage by its bomb.");
    private static final ForgeConfigSpec.DoubleValue JINJO_HEALTH_MULTIPLIER = value(
            "srparasites:bomber_heavy", "heavyBomberHealthMultiplier", 1.0D, 0.01D, 100.0D,
            "Health multiplier for the Heavy Bomber.");
    private static final ForgeConfigSpec.DoubleValue JINJO_DAMAGE_MULTIPLIER = value(
            "srparasites:bomber_heavy", "heavyBomberDamageMultiplier", 1.0D, 0.01D, 100.0D,
            "Attack damage multiplier for the Heavy Bomber.");
    private static final ForgeConfigSpec.DoubleValue JINJO_ARMOR_MULTIPLIER = value(
            "srparasites:bomber_heavy", "heavyBomberArmorMultiplier", 1.0D, 0.01D, 100.0D,
            "Armor multiplier for the Heavy Bomber.");
    private static final ForgeConfigSpec.DoubleValue JINJO_KNOCKBACK_MULTIPLIER = value(
            "srparasites:bomber_heavy", "heavyBomberKnockbackResistanceMultiplier", 1.0D, 0.01D, 100.0D,
            "Knockback-resistance multiplier for the Heavy Bomber.");
    private static final ForgeConfigSpec.IntValue JINJO_MAX_Y = intValue(
            "srparasites:bomber_heavy", "heavyBomberFlightHeightLimit", 256, 0, 256,
            "Maximum number of air blocks the Heavy Bomber may fly above terrain.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> JINJO_ORB_EFFECTS = stringList(
            "srparasites:bomber_heavy", "heavyBomberOrbEffects", List.of(
                    "0;15;4;minecraft:hunger;0;0",
                    "0;120;4;csrp:needler;0;0",
                    "0;15;4;minecraft:mining_fatigue;0;0",
                    "0;10;4;minecraft:wither;0;0"),
            "Heavy Bomber scary-orb effects: self;seconds;amplifier;effect_id;mob_amplifier_step;mob_duration_step.",
            MobsConfig::validOrbEffect);
    private static final ForgeConfigSpec.BooleanValue JINJO_GRIEFING = booleanValue(
            "srparasites:bomber_heavy", "heavyBomberGriefing", true,
            "Whether Heavy Bomber explosions may destroy blocks when mobGriefing is enabled.");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> JINJO_MOBS = stringList(
            "srparasites:bomber_heavy", "heavyBomberMobTable", List.of(
                    "csrp:overseer", "csrp:vigilante", "csrp:marauder", "csrp:monarch"),
            "Entity ids available to the spawning Heavy Bomber bomb.",
            value -> value instanceof String id
                    && net.minecraft.resources.ResourceLocation.tryParse(id) != null);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private MobsConfig() {
    }

    private static ForgeConfigSpec.DoubleValue followRange(String category, String name, double defaultValue) {
        BUILDER.push(category);
        ForgeConfigSpec.DoubleValue value = BUILDER.comment("Follow range for this parasite tier.")
                .defineInRange(name, defaultValue, 0.0D, 128.0D);
        BUILDER.pop();
        return value;
    }

    private static ForgeConfigSpec.DoubleValue value(String category, String name, double defaultValue,
                                                   double min, double max, String comment) {
        BUILDER.push(category);
        ForgeConfigSpec.DoubleValue value = BUILDER.comment(comment).defineInRange(name, defaultValue, min, max);
        BUILDER.pop();
        return value;
    }

    private static ForgeConfigSpec.BooleanValue booleanValue(String category, String name,
                                                            boolean defaultValue, String comment) {
        BUILDER.push(category);
        ForgeConfigSpec.BooleanValue value = BUILDER.comment(comment).define(name, defaultValue);
        BUILDER.pop();
        return value;
    }

    private static ForgeConfigSpec.IntValue intValue(String category, String name, int defaultValue,
                                                    int min, int max, String comment) {
        BUILDER.push(category);
        ForgeConfigSpec.IntValue value = BUILDER.comment(comment).defineInRange(name, defaultValue, min, max);
        BUILDER.pop();
        return value;
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> stringList(String category, String name,
            List<String> defaults, String comment, java.util.function.Predicate<Object> validator) {
        BUILDER.push(category);
        ForgeConfigSpec.ConfigValue<List<? extends String>> value = BUILDER.comment(comment)
                .defineList(name, defaults, validator);
        BUILDER.pop();
        return value;
    }

    public static double followRange(LivingEntity entity) {
        if (entity instanceof WorkerEntity) return -1.0D;
        if (entity instanceof ArchitectEntity) return PURE_FOLLOW.get();
        if (entity instanceof MovingFleshEntity) return ADAPTED_FOLLOW.get();
        if (entity instanceof PreeminentParasiteEntity) return PREEMINENT_FOLLOW.get();
        if (entity instanceof AncientParasiteEntity) return ANCIENT_FOLLOW.get();
        if (entity instanceof PureParasiteEntity) return PURE_FOLLOW.get();
        if (entity instanceof AdaptedVariantEntity) return ADAPTED_FOLLOW.get();
        if (entity instanceof AssimilatedParasiteEntity) return INFECTED_FOLLOW.get();
        if (entity instanceof PrimitiveParasiteEntity) return PRIMITIVE_FOLLOW.get();
        return -1.0D;
    }

    public static double pureFollowRange() {
        return PURE_FOLLOW.get();
    }

    public static double preeminentFollowRange() {
        return PREEMINENT_FOLLOW.get();
    }

    public static double adaptedFollowRange() {
        return ADAPTED_FOLLOW.get();
    }

    public static boolean rupterPassiveMobAttacking() {
        return RUPTER_ANIMAL_ATTACKING.get();
    }

    public static float rupterMinimumDamage() {
        return RUPTER_MINIMUM_DAMAGE.get().floatValue();
    }

    public static int rupterTunnelCost() {
        return RUPTER_TUNNEL_COST.get();
    }

    public static int rupterTunnelPhase() {
        return RUPTER_TUNNEL_PHASE.get();
    }

    public static int rupterManglerKills() {
        return RUPTER_MANGLER_KILLS.get();
    }

    public static float manglerMinimumDamage() {
        return MANGLER_MINIMUM_DAMAGE.get().floatValue();
    }

    public static float manglerRegeneration() {
        return MANGLER_REGENERATION.get().floatValue();
    }

    public static int ancientDreadnautMaxY() {
        return ANCIENT_MAX_Y.get();
    }

    public static int ancientDreadnautMinY() {
        return Math.min(ANCIENT_MIN_Y.get(), ANCIENT_MAX_Y.get());
    }

    public static int ancientDreadnautPodCooldownTicks() {
        return ANCIENT_POD_COOLDOWN.get() * 20;
    }

    public static int ancientDreadnautPodNumber() {
        return ANCIENT_POD_NUMBER.get();
    }

    public static int ancientDreadnautPodMaxMobs() {
        return ANCIENT_POD_MAX_MOBS.get();
    }

    public static boolean carrierHeavyGriefing() {
        return CARRIER_HEAVY_GRIEFING.get();
    }

    public static List<? extends String> carrierHeavyMobTable() {
        return CARRIER_HEAVY_MOBS.get();
    }

    public static boolean carrierLightGriefing() {
        return CARRIER_LIGHT_GRIEFING.get();
    }

    public static List<? extends String> carrierLightMobTable() {
        return CARRIER_LIGHT_MOBS.get();
    }

    public static boolean carrierFlyingGriefing() {
        return CARRIER_FLYING_GRIEFING.get();
    }

    public static int carrierFlyingMaxY() {
        return CARRIER_FLYING_MAX_Y.get();
    }

    public static List<? extends String> carrierFlyingMobTable() {
        return CARRIER_FLYING_MOBS.get();
    }

    public static boolean mergeSystemRandom() {
        return MERGE_RANDOM.get();
    }

    public static double mergeSystemMobHealth() {
        return MERGE_HEALTH.get();
    }

    public static List<? extends String> mergeSystemMobList() {
        return MERGE_MOB_TABLE.get();
    }

    public static boolean ancientPodGriefing() {
        return ANCIENT_POD_GRIEFING.get();
    }

    public static List<? extends String> ancientDreadnautMobList() {
        return ANCIENT_MOB_LIST.get();
    }

    public static List<? extends String> ancientPodEffects() {
        return ANCIENT_POD_EFFECTS.get();
    }

    public static double arachnidaHealth() {
        return 35.0D * ARACHNIDA_HEALTH_MULTIPLIER.get();
    }

    public static double arachnidaDamage() {
        return 15.0D * ARACHNIDA_DAMAGE_MULTIPLIER.get();
    }

    public static double arachnidaArmor() {
        return 4.0D * ARACHNIDA_ARMOR_MULTIPLIER.get();
    }

    public static double arachnidaKnockbackResistance() {
        return Math.min(1.0D, 0.2D * ARACHNIDA_KNOCKBACK_MULTIPLIER.get());
    }

    public static double adaptedArachnidaHealth() {
        return (35.0D + ADAPTED_ARACHNIDA_ADDITIONAL_HEALTH.get()) * ARACHNIDA_HEALTH_MULTIPLIER.get();
    }

    public static double adaptedArachnidaDamage() {
        return (15.0D + ADAPTED_ARACHNIDA_ADDITIONAL_DAMAGE.get()) * ARACHNIDA_DAMAGE_MULTIPLIER.get();
    }

    public static double adaptedArachnidaArmor() {
        return (4.0D + ADAPTED_ARACHNIDA_ADDITIONAL_ARMOR.get()) * ARACHNIDA_ARMOR_MULTIPLIER.get();
    }

    public static double adaptedArachnidaKnockbackResistance() {
        return Math.min(1.0D, (0.8D + ADAPTED_ARACHNIDA_ADDITIONAL_KNOCKBACK.get())
                * ARACHNIDA_KNOCKBACK_MULTIPLIER.get());
    }

    public static List<? extends String> adaptedArachnidaOrbEffects() {
        return ADAPTED_ARACHNIDA_ORB_EFFECTS.get();
    }

    public static double visceraHealth() {
        return 45.0D * VISCERA_HEALTH_MULTIPLIER.get();
    }

    public static double visceraDamage() {
        return 15.0D * VISCERA_DAMAGE_MULTIPLIER.get();
    }

    public static double visceraArmor() {
        return 9.0D * VISCERA_ARMOR_MULTIPLIER.get();
    }

    public static double visceraKnockbackResistance() {
        return Math.min(1.0D, 0.7D * VISCERA_KNOCKBACK_MULTIPLIER.get());
    }

    public static List<? extends String> visceraOrbEffects() {
        return VISCERA_ORB_EFFECTS.get();
    }

    public static double bolsterHealth() {
        return 35.0D * BOLSTER_HEALTH_MULTIPLIER.get();
    }

    public static double bolsterDamage() {
        return 6.0D * BOLSTER_DAMAGE_MULTIPLIER.get();
    }

    public static double bolsterArmor() {
        return 4.0D * BOLSTER_ARMOR_MULTIPLIER.get();
    }

    public static double bolsterKnockbackResistance() {
        return Math.min(1.0D, 0.35D * BOLSTER_KNOCKBACK_MULTIPLIER.get());
    }

    public static int bolsterBuffCooldownTicks() {
        return BOLSTER_BUFF_COOLDOWN.get() * 20;
    }

    public static int bolsterBuffRange() {
        return BOLSTER_BUFF_RANGE.get();
    }

    public static List<? extends String> bolsterEffects() {
        return BOLSTER_EFFECTS.get();
    }

    public static List<? extends String> bolsterOrbEffects() {
        return BOLSTER_ORB_EFFECTS.get();
    }

    public static double manducaterHealth() {
        return 30.0D * MANDUCATER_HEALTH_MULTIPLIER.get();
    }

    public static double manducaterDamage() {
        return 12.0D * MANDUCATER_DAMAGE_MULTIPLIER.get();
    }

    public static double manducaterArmor() {
        return 4.0D * MANDUCATER_ARMOR_MULTIPLIER.get();
    }

    public static double manducaterKnockbackResistance() {
        return Math.min(1.0D, 0.5D * MANDUCATER_KNOCKBACK_MULTIPLIER.get());
    }

    public static double manducaterNeededHealth() {
        return MANDUCATER_NEEDED_HEALTH.get();
    }

    public static double manducaterNeededTime() {
        return MANDUCATER_NEEDED_TIME.get();
    }

    public static double manducaterStealthDamageMultiplier() {
        return MANDUCATER_STEALTH_DAMAGE.get();
    }

    public static List<? extends String> manducaterOrbEffects() {
        return MANDUCATER_ORB_EFFECTS.get();
    }

    public static double devourerHealth() {
        return 60.0D * DEVOURER_HEALTH_MULTIPLIER.get();
    }

    public static double devourerDamage() {
        return 20.0D * DEVOURER_DAMAGE_MULTIPLIER.get();
    }

    public static double devourerArmor() {
        return 4.0D * DEVOURER_ARMOR_MULTIPLIER.get();
    }

    public static double devourerKnockbackResistance() {
        return Math.min(1.0D, DEVOURER_KNOCKBACK_MULTIPLIER.get());
    }

    public static boolean devourerWaterPlacement() {
        return DEVOURER_WATER_PLACEMENT.get();
    }

    public static double burrowerHealth() {
        return 45.0D * BURROWER_HEALTH_MULTIPLIER.get();
    }

    public static double burrowerDamage() {
        return 15.0D * BURROWER_DAMAGE_MULTIPLIER.get();
    }

    public static double burrowerArmor() {
        return 9.0D * BURROWER_ARMOR_MULTIPLIER.get();
    }

    public static double burrowerKnockbackResistance() {
        return Math.min(1.0D, 0.7D * BURROWER_KNOCKBACK_MULTIPLIER.get());
    }

    public static double tozoonHealth() {
        return 45.0D * TOZOON_HEALTH_MULTIPLIER.get();
    }

    public static double tozoonDamage() {
        return 15.0D * TOZOON_DAMAGE_MULTIPLIER.get();
    }

    public static double tozoonArmor() {
        return 9.0D * TOZOON_ARMOR_MULTIPLIER.get();
    }

    public static double tozoonKnockbackResistance() {
        return Math.min(1.0D, TOZOON_KNOCKBACK_MULTIPLIER.get());
    }

    public static double adaptedTozoonHealth() {
        return (45.0D + ADAPTED_TOZOON_ADDITIONAL_HEALTH.get()) * TOZOON_HEALTH_MULTIPLIER.get();
    }

    public static double adaptedTozoonDamage() {
        return (15.0D + ADAPTED_TOZOON_ADDITIONAL_DAMAGE.get()) * TOZOON_DAMAGE_MULTIPLIER.get();
    }

    public static double adaptedTozoonArmor() {
        return (9.0D + ADAPTED_TOZOON_ADDITIONAL_ARMOR.get()) * TOZOON_ARMOR_MULTIPLIER.get();
    }

    public static double adaptedTozoonConfiguredKnockbackResistance() {
        return (0.7D + ADAPTED_TOZOON_ADDITIONAL_KNOCKBACK.get()) * TOZOON_KNOCKBACK_MULTIPLIER.get();
    }

    public static double reekerHealth() {
        return 40.0D * REEKER_HEALTH_MULTIPLIER.get();
    }

    public static double reekerDamage() {
        return 12.0D * REEKER_DAMAGE_MULTIPLIER.get();
    }

    public static double reekerArmor() {
        return 12.0D * REEKER_ARMOR_MULTIPLIER.get();
    }

    public static double reekerKnockbackResistance() {
        return 0.6D * REEKER_KNOCKBACK_MULTIPLIER.get();
    }

    public static boolean reekerRicardoVariantEnabled() {
        return REEKER_RICARDO_ENABLED.get();
    }

    public static List<? extends String> reekerOrbEffects() {
        return REEKER_ORB_EFFECTS.get();
    }

    public static double yelloweyeHealth() {
        return 30.0D * YELLOWEYE_HEALTH_MULTIPLIER.get();
    }

    public static double yelloweyeArmor() {
        return 3.5D * YELLOWEYE_ARMOR_MULTIPLIER.get();
    }

    public static double yelloweyeNadeDamage() {
        return 3.5D * YELLOWEYE_ARMOR_MULTIPLIER.get();
    }

    public static float yelloweyeRangedDamage() {
        return (float) (5.0D * YELLOWEYE_DAMAGE_MULTIPLIER.get());
    }

    public static double yelloweyeKnockbackResistance() {
        return 0.2D * YELLOWEYE_KNOCKBACK_MULTIPLIER.get();
    }

    public static int yelloweyePoisonDurationTicks() {
        return YELLOWEYE_POISON_DURATION.get() * 20;
    }

    public static int yelloweyePoisonAmplifier() {
        return YELLOWEYE_POISON_AMPLIFIER.get() - 1;
    }

    public static double yelloweyeGearDamage() {
        return YELLOWEYE_GEAR_DAMAGE.get();
    }

    public static int yelloweyeMaxFlightHeight() {
        return YELLOWEYE_MAX_FLIGHT_HEIGHT.get();
    }

    public static float hostBombDamage() {
        return HOST_BOMB_DAMAGE.get().floatValue();
    }

    public static float herdBombDamage() {
        return HERD_BOMB_DAMAGE.get().floatValue();
    }

    public static float ombooBombDamage() {
        return OMBOO_BOMB_DAMAGE.get().floatValue();
    }

    public static int ombooMaxY() {
        return OMBOO_MAX_Y.get();
    }

    public static boolean ombooGriefing() {
        return OMBOO_GRIEFING.get();
    }

    public static double overseerHealth() {
        return 80.0D * OVERSEER_HEALTH_MULTIPLIER.get();
    }

    public static double overseerArmor() {
        return 20.0D * OVERSEER_ARMOR_MULTIPLIER.get();
    }

    public static double overseerMeleeDamage() {
        return 22.0D * OVERSEER_DAMAGE_MULTIPLIER.get();
    }

    public static float overseerProjectileDamage() {
        return (float) (30.0D * OVERSEER_DAMAGE_MULTIPLIER.get());
    }

    public static double overseerKnockbackResistance() {
        return 0.4D * OVERSEER_KNOCKBACK_MULTIPLIER.get();
    }

    public static int overseerSummonCooldownTicks() {
        return OVERSEER_SUMMON_COOLDOWN.get();
    }

    public static int overseerTotalActiveMobs() {
        return OVERSEER_TOTAL_ACTIVE_MOBS.get();
    }

    public static int overseerSummonLimit() {
        return OVERSEER_SUMMON_LIMIT.get();
    }

    public static List<? extends String> overseerSummonMobs() {
        return OVERSEER_SUMMON_MOBS.get();
    }

    public static int overseerMaxY() {
        return OVERSEER_MAX_Y.get();
    }

    public static double vigilanteHealth() {
        return 70.0D * VIGILANTE_HEALTH_MULTIPLIER.get();
    }

    public static double vigilanteArmor() {
        return 25.0D * VIGILANTE_ARMOR_MULTIPLIER.get();
    }

    public static double vigilanteMeleeDamage() {
        return 23.0D * VIGILANTE_DAMAGE_MULTIPLIER.get();
    }

    public static float vigilanteRangedDamage() {
        return (float) (27.0D * VIGILANTE_RANGED_DAMAGE_MULTIPLIER.get());
    }

    public static double vigilanteKnockbackResistance() {
        return Math.min(1.0D, VIGILANTE_KNOCKBACK_MULTIPLIER.get());
    }

    public static List<? extends String> vigilanteOrbEffects() {
        return VIGILANTE_ORB_EFFECTS.get();
    }

    public static double wardenHealth() {
        return 80.0D * WARDEN_HEALTH_MULTIPLIER.get();
    }

    public static double wardenArmor() {
        return 15.0D * WARDEN_ARMOR_MULTIPLIER.get();
    }

    public static double wardenDamage() {
        return 25.0D * WARDEN_DAMAGE_MULTIPLIER.get();
    }

    public static double wardenKnockbackResistance() {
        return Math.min(1.0D, WARDEN_KNOCKBACK_MULTIPLIER.get());
    }

    public static List<? extends String> wardenOrbEffects() {
        return WARDEN_ORB_EFFECTS.get();
    }

    public static List<? extends String> monarchOrbEffects() {
        return MONARCH_ORB_EFFECTS.get();
    }

    public static float jinjoExplosionMultiplier() {
        return JINJO_EXPLOSION_MULTIPLIER.get().floatValue();
    }

    public static double jinjoHealth() {
        return 420.0D * JINJO_HEALTH_MULTIPLIER.get();
    }

    public static double jinjoArmor() {
        return 15.5D * JINJO_ARMOR_MULTIPLIER.get();
    }

    public static double jinjoDamage() {
        return 33.0D * JINJO_DAMAGE_MULTIPLIER.get();
    }

    public static double jinjoKnockbackResistance() {
        return Math.min(1.0D, 0.15D * JINJO_KNOCKBACK_MULTIPLIER.get());
    }

    public static int jinjoMaxY() {
        return JINJO_MAX_Y.get();
    }

    public static List<? extends String> jinjoOrbEffects() {
        return JINJO_ORB_EFFECTS.get();
    }

    public static boolean jinjoGriefing() {
        return JINJO_GRIEFING.get();
    }

    public static List<? extends String> jinjoMobs() {
        return JINJO_MOBS.get();
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
}
