package alku.csrp.config;

import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.AncientParasiteEntity;
import alku.csrp.entity.AssimilatedParasiteEntity;
import alku.csrp.entity.PreeminentParasiteEntity;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.entity.PureParasiteEntity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.ModConfigSpec;

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
}
