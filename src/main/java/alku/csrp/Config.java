package alku.csrp;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue EVOLUTION_PHASE = BUILDER
            .comment("Current parasite evolution phase used by phase-gated spawning and behavior.")
            .defineInRange("evolutionPhase", 1, -1, 10);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static int evolutionPhase() {
        return EVOLUTION_PHASE.get();
    }
}
