package alku.csrp.compendium.client;

import com.google.gson.JsonObject;

public record CompendiumEntry(String path, String entityId, String tier, String nameKey, String loreKey,
        float renderScale, int minimumLoreKills, int minimumStatKills) {
    public static CompendiumEntry fromJson(String path, JsonObject json) {
        String originalId = json.get("id").getAsString();
        String mappedId = originalId.startsWith("srparasites:")
                ? "csrp:" + originalId.substring("srparasites:".length()) : originalId;
        return new CompendiumEntry(
                path,
                mappedId,
                json.get("tier").getAsString(),
                json.get("name_key").getAsString(),
                json.get("lore_key").getAsString(),
                json.has("render_scale") ? json.get("render_scale").getAsFloat() : 1.0F,
                json.has("minlorekill") ? json.get("minlorekill").getAsInt() : 1,
                json.has("minstatkill") ? json.get("minstatkill").getAsInt() : 1);
    }
}
