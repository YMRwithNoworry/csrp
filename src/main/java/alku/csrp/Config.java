package alku.csrp;

import alku.csrp.world.SrpWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue EVOLUTION_PHASE = BUILDER
            .comment("Current parasite evolution phase used by phase-gated spawning and behavior.")
            .defineInRange("evolutionPhase", -1, -2, 10);
    private static final ModConfigSpec.DoubleValue ADAPTATION_CHANCE = BUILDER
            .comment("Chance for a linked parasite outside a colony to share its adaptation on death.")
            .defineInRange("adaptationChance", 0.1D, 0.0D, 1.0D);
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
}
