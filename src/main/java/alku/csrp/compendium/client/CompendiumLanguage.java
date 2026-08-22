package alku.csrp.compendium.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class CompendiumLanguage {
    private static final Map<String, String> VALUES = new LinkedHashMap<>();
    private static String loadedLanguage = "";

    private CompendiumLanguage() {
    }

    public static String get(String key) {
        ensureLoaded();
        return VALUES.getOrDefault(key, key);
    }

    private static void ensureLoaded() {
        Minecraft minecraft = Minecraft.getInstance();
        String selected = minecraft.getLanguageManager().getSelected();
        String language = selected.equals("zh_cn") ? "zh_cn" : "en_us";
        if (language.equals(loadedLanguage)) {
            return;
        }
        VALUES.clear();
        load("en_us");
        if (!language.equals("en_us")) {
            load(language);
        }
        loadedLanguage = language;
    }

    private static void load(String language) {
        ResourceLocation id = new ResourceLocation(
                "csrp", "compendium/lang/" + language + ".lang");
        Minecraft.getInstance().getResourceManager().getResource(id).ifPresent(resource -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                reader.lines().forEach(line -> {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        return;
                    }
                    int separator = trimmed.indexOf('=');
                    if (separator > 0) {
                        VALUES.put(trimmed.substring(0, separator), trimmed.substring(separator + 1));
                    }
                });
            } catch (Exception ignored) {
                // Missing optional translations fall back to their key.
            }
        });
    }
}
