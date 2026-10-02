package alku.csrp.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue DERIVED_TEXT_DISTORTION_ENABLED = BUILDER
            .comment("Allow nearby derived parasites to scramble client-side GUI text.")
            .define("derivedTextDistortionEnabled", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    public static boolean derivedTextDistortionEnabled() {
        return DERIVED_TEXT_DISTORTION_ENABLED.get();
    }
}
