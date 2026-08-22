package alku.csrp.compendium.client;

import alku.csrp.compendium.CompendiumProgress;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class CompendiumClient {
    private static boolean soundsEnabled = true;
    private static List<CompendiumEntry> entries;

    private CompendiumClient() {
    }

    public static void open(CompoundTag tag) {
        Minecraft.getInstance().setScreen(new CompendiumScreen(CompendiumProgress.load(tag), entries()));
    }

    public static void toggleSounds() {
        soundsEnabled = !soundsEnabled;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(
                    soundsEnabled ? "message.csrp.compendium.sounds_on" : "message.csrp.compendium.sounds_off"),
                    true);
        }
    }

    public static void playUnlock(String category) {
        if (!soundsEnabled) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ResourceLocation id = new ResourceLocation("csrp", "compendium.unlock_" + category);
        minecraft.player.playSound(SoundEvent.createVariableRangeEvent(id), 0.8F, 1.0F);
    }

    private static List<CompendiumEntry> entries() {
        if (entries != null) {
            return entries;
        }
        List<CompendiumEntry> loaded = new ArrayList<>();
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation indexId = new ResourceLocation("csrp", "compendium/entries/index.json");
        minecraft.getResourceManager().getResource(indexId).ifPresent(resource -> {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonArray paths = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("mobs");
                for (var element : paths) {
                    String path = element.getAsString();
                    ResourceLocation entryId = new ResourceLocation(
                            "csrp", "compendium/entries/" + path + ".json");
                    minecraft.getResourceManager().getResource(entryId).ifPresent(entryResource -> {
                        try (InputStreamReader entryReader = new InputStreamReader(
                                entryResource.open(), StandardCharsets.UTF_8)) {
                            JsonObject json = JsonParser.parseReader(entryReader).getAsJsonObject();
                            loaded.add(CompendiumEntry.fromJson(path, json));
                        } catch (Exception ignored) {
                        }
                    });
                }
            } catch (Exception ignored) {
            }
        });
        entries = List.copyOf(loaded);
        return entries;
    }

    public static List<String> drops(String path) {
        Set<String> drops = new LinkedHashSet<>();
        ResourceLocation id = new ResourceLocation("csrp", "compendium/drops/" + path + ".json");
        Minecraft.getInstance().getResourceManager().getResource(id).ifPresent(resource -> {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                collectItems(JsonParser.parseReader(reader), drops);
            } catch (Exception ignored) {
            }
        });
        return List.copyOf(drops);
    }

    private static void collectItems(JsonElement element, Set<String> output) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectItems(child, output));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("type") && object.has("name")
                && object.get("type").getAsString().equals("minecraft:item")) {
            output.add(object.get("name").getAsString());
        }
        object.entrySet().forEach(entry -> collectItems(entry.getValue(), output));
    }
}
