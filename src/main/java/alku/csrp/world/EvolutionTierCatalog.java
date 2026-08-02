package alku.csrp.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class EvolutionTierCatalog {
    private static final Map<String, String> TIERS = loadTiers();

    private EvolutionTierCatalog() {
    }

    static String tier(String entityId) {
        return TIERS.getOrDefault(entityId, "");
    }

    private static Map<String, String> loadTiers() {
        Map<String, String> tiers = new HashMap<>();
        ClassLoader loader = EvolutionTierCatalog.class.getClassLoader();
        try (InputStreamReader reader = reader(loader, "assets/csrp/compendium/entries/index.json")) {
            JsonArray entries = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("mobs");
            for (var element : entries) {
                String path = element.getAsString();
                try (InputStreamReader entryReader = reader(loader,
                        "assets/csrp/compendium/entries/" + path + ".json")) {
                    JsonObject entry = JsonParser.parseReader(entryReader).getAsJsonObject();
                    String id = entry.get("id").getAsString().replace("srparasites:", "csrp:");
                    tiers.put(id, entry.get("tier").getAsString());
                }
            }
        } catch (Exception ignored) {
            // A missing optional catalog only disables tier-specific death deductions.
        }
        return Map.copyOf(tiers);
    }

    private static InputStreamReader reader(ClassLoader loader, String path) {
        var stream = loader.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing evolution tier resource: " + path);
        }
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
}
