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

    private static final ModConfigSpec.DoubleValue MANDUCATER_NEEDED_HEALTH = value(
            "srparasites:manducater", "manducaterNeededHealth", 0.70D, 0.0D, 1.0D,
            "Health ratio needed for a Primitive Manducater to camouflage.");
    private static final ModConfigSpec.DoubleValue MANDUCATER_NEEDED_TIME = value(
            "srparasites:manducater", "manducaterNeededTime", 15.0D, 1.0D, 100.0D,
            "Camouflage charge time in periodic checks.");
    private static final ModConfigSpec.DoubleValue MANDUCATER_STEALTH_DAMAGE = value(
            "srparasites:manducater", "manducaterStealthDamageMultiplier", 2.0D, 0.01D, 100.0D,
            "Damage multiplier for a camouflaged Primitive Manducater attack.");
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

    public static double manducaterNeededHealth() {
        return MANDUCATER_NEEDED_HEALTH.get();
    }

    public static double manducaterNeededTime() {
        return MANDUCATER_NEEDED_TIME.get();
    }

    public static double manducaterStealthDamageMultiplier() {
        return MANDUCATER_STEALTH_DAMAGE.get();
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
