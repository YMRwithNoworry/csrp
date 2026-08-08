package alku.csrp.config;

import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.AncientParasiteEntity;
import alku.csrp.entity.AssimilatedParasiteEntity;
import alku.csrp.entity.PreeminentParasiteEntity;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.entity.PureParasiteEntity;
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
    private static final ModConfigSpec.DoubleValue PREEMINENT_FOLLOW = followRange(
            "parasitePropertiesPreeminent", "preeminentFollow", 80.0D);

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

    private static final ModConfigSpec.DoubleValue MANDUCATER_NEEDED_HEALTH = value(
            "srparasites:manducater", "manducaterNeededHealth", 0.70D, 0.0D, 1.0D,
            "Health ratio needed for a Primitive Manducater to camouflage.");
    private static final ModConfigSpec.DoubleValue MANDUCATER_NEEDED_TIME = value(
            "srparasites:manducater", "manducaterNeededTime", 15.0D, 1.0D, 100.0D,
            "Camouflage charge time in periodic checks.");
    private static final ModConfigSpec.DoubleValue MANDUCATER_STEALTH_DAMAGE = value(
            "srparasites:manducater", "manducaterStealthDamageMultiplier", 2.0D, 0.01D, 100.0D,
            "Damage multiplier for a camouflaged Primitive Manducater attack.");
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
        if (entity instanceof PreeminentParasiteEntity) return PREEMINENT_FOLLOW.get();
        if (entity instanceof AncientParasiteEntity) return ANCIENT_FOLLOW.get();
        if (entity instanceof PureParasiteEntity) return PURE_FOLLOW.get();
        if (entity instanceof AdaptedVariantEntity) return ADAPTED_FOLLOW.get();
        if (entity instanceof AssimilatedParasiteEntity) return INFECTED_FOLLOW.get();
        if (entity instanceof PrimitiveParasiteEntity) return PRIMITIVE_FOLLOW.get();
        return -1.0D;
    }

    public static double adaptedFollowRange() {
        return ADAPTED_FOLLOW.get();
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

    public static double manducaterNeededHealth() {
        return MANDUCATER_NEEDED_HEALTH.get();
    }

    public static double manducaterNeededTime() {
        return MANDUCATER_NEEDED_TIME.get();
    }

    public static double manducaterStealthDamageMultiplier() {
        return MANDUCATER_STEALTH_DAMAGE.get();
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
}
