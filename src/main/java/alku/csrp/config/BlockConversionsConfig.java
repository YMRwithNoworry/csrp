package alku.csrp.config;

import alku.csrp.block.InfestedBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class BlockConversionsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue USE_DEFAULT_CONVERSIONS = BUILDER
            .comment("Use CSRP's material and block-tag based infestation conversions.")
            .define("useDefaultConversions", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_CONVERSIONS = BUILDER
            .comment("Exact block conversions formatted as source block id;infected target block id. These override defaults.")
            .defineList("customConversions", List.of(), BlockConversionsConfig::validConversion);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private BlockConversionsConfig() {
    }

    public static boolean useDefaultConversions() {
        return USE_DEFAULT_CONVERSIONS.get();
    }

    public static Block customTarget(Block source) {
        Identifier sourceId = BuiltInRegistries.BLOCK.getKey(source);
        for (String entry : CUSTOM_CONVERSIONS.get()) {
            String[] parts = entry.split(";", -1);
            if (sourceId.toString().equals(parts[0])) {
                return BuiltInRegistries.BLOCK.getValue(Identifier.parse(parts[1]));
            }
        }
        return null;
    }

    private static boolean validConversion(Object value) {
        if (!(value instanceof String entry)) return false;
        String[] parts = entry.split(";", -1);
        if (parts.length != 2) return false;
        Identifier source = Identifier.tryParse(parts[0]);
        Identifier target = Identifier.tryParse(parts[1]);
        return source != null && target != null && BuiltInRegistries.BLOCK.containsKey(source)
                && BuiltInRegistries.BLOCK.containsKey(target)
                && BuiltInRegistries.BLOCK.getValue(target) instanceof InfestedBlock;
    }
}
