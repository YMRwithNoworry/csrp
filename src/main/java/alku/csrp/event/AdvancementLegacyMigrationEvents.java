package alku.csrp.event;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import alku.csrp.Csrp;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Keeps advancement progress from saves that were written before the port moved every CSRP
 * advancement from the legacy {@code csrp:csrp/<name>} ids to the canonical {@code csrp:<name>}
 * ids.
 *
 * <p>When a player joins, the ids stored on disk are rewritten in place:
 * <ul>
 *     <li>the modern per-player store, {@code <world>/advancements/<uuid>.json}, which is the file
 *     {@link net.minecraft.server.PlayerAdvancements} actually reads in 1.21.1, and</li>
 *     <li>the legacy {@code advancements} compound inside {@code <world>/playerdata/<uuid>.dat},
 *     which older saves (and the 1.12-era SRP saves the port inherits from) used.</li>
 * </ul>
 *
 * <p>{@link net.minecraft.server.PlayerAdvancements} is created before this event fires, so after a
 * successful rewrite the live progress is reloaded from the migrated file. Every step is wrapped in
 * a defensive try/catch: a broken save file must never keep a player out of the world.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class AdvancementLegacyMigrationEvents {

    /** Every legacy id was namespaced as {@code csrp:csrp/<name>}. */
    public static final String LEGACY_PREFIX = Csrp.MODID + ":csrp/";

    /** Key of the advancement map inside a player data file. */
    public static final String ADVANCEMENTS_TAG = "advancements";

    /** Directory (relative to the player data directory) that holds the modern advancement json. */
    public static final String ADVANCEMENTS_DIRECTORY = "advancements";

    private static final Map<String, String> LEGACY_PATHS;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("root", "root");
        paths.put("monumenta", "monument_root");
        paths.put("firmamentum", "firmamentum_root");
        paths.put("cognitio", "secret_root");
        paths.put("venari", "kill_root");
        paths.put("zayin_suppression", "kill_dispatcher_s1");
        paths.put("teth_suppression", "kill_dispatcher_s2");
        paths.put("he_suppression", "kill_dispatcher_s3");
        paths.put("aleph_suppression", "kill_dispatcher_s4");
        paths.put("gardener", "kill_beckon_s1");
        paths.put("pruner", "kill_beckon_s2");
        paths.put("uprooter", "kill_beckon_s3");
        paths.put("eradicator", "kill_beckon_s4");
        paths.put("monument_of_sins", "monument");
        paths.put("ecstasy", "brew_fear");
        paths.put("enemy_of_my_enemy", "enemy_enemy");
        paths.put("beautiful_self_destruction", "thornshade_self_destruct");
        paths.put("tricked_me", "tricked_me_did_you");
        paths.put("inborn", "kill_inborn");
        paths.put("assimilated", "kill_assimilated");
        paths.put("crude", "kill_crude");
        paths.put("primitive", "kill_primitive");
        paths.put("adapted", "kill_adapted");
        paths.put("deterrent", "kill_deterrent");
        paths.put("pure", "kill_pure");
        paths.put("qliphoth_overload", "qliphoth_overload");
        paths.put("fog_nullifier", "fog_nullifier");
        paths.put("peacekeeper", "peacekeeper");
        paths.put("fermented_arrogance", "fermented_arrogance");
        paths.put("sepeku", "sepeku");
        paths.put("cut_roots", "cut_roots");
        paths.put("columbus", "columbus");
        paths.put("stolas", "stolas");
        paths.put("dark_days", "dark_days");
        paths.put("controversial", "controversial");
        paths.put("guerilla", "guerilla");
        paths.put("hunt_season", "hunt_season");
        paths.put("cosmic_structural_failure", "cosmic_structural_failure");
        paths.put("hellfire_chemical_warfare", "hellfire_chemical_warfare");
        paths.put("paojiao_wolf", "paojiao_wolf");
        LEGACY_PATHS = Collections.unmodifiableMap(paths);
    }

    private AdvancementLegacyMigrationEvents() {
    }

    /**
     * Every legacy path (the {@code <name>} of {@code csrp:csrp/<name>}) mapped to the canonical
     * advancement path. Exposed so tests can walk the whole table.
     */
    public static Map<String, String> legacyMappings() {
        return LEGACY_PATHS;
    }

    /**
     * Rewrites a single stored advancement id. Ids that are not legacy CSRP ids - already canonical
     * CSRP ids, vanilla ids, other mods - are returned unchanged.
     *
     * @param oldId the id stored on disk, may be {@code null}
     * @return the canonical id to store instead
     */
    public static String migrateId(String oldId) {
        if (oldId == null || !oldId.startsWith(LEGACY_PREFIX)) {
            return oldId;
        }
        String legacyPath = oldId.substring(LEGACY_PREFIX.length());
        String canonicalPath = LEGACY_PATHS.get(legacyPath);
        if (canonicalPath == null) {
            return oldId;
        }
        return Csrp.MODID + ":" + canonicalPath;
    }

    /**
     * Rewrites every legacy key of an {@code advancements} compound in place. Progress that is
     * already stored under the canonical id is kept and the two progress compounds are merged so
     * neither side loses criteria.
     *
     * @param advancements the {@code advancements} compound of a player data file, may be {@code null}
     */
    public static void migrateTag(CompoundTag advancements) {
        if (advancements == null || advancements.isEmpty()) {
            return;
        }
        for (String oldKey : new ArrayList<>(advancements.getAllKeys())) {
            String newKey = migrateId(oldKey);
            if (newKey == null || newKey.equals(oldKey)) {
                continue;
            }
            Tag legacyValue = advancements.get(oldKey);
            if (legacyValue == null) {
                continue;
            }
            Tag existingValue = advancements.get(newKey);
            if (existingValue instanceof CompoundTag existingProgress && legacyValue instanceof CompoundTag legacyProgress) {
                advancements.put(newKey, mergeProgress(existingProgress, legacyProgress));
            } else if (existingValue == null) {
                advancements.put(newKey, legacyValue.copy());
            }
            advancements.remove(oldKey);
        }
    }

    /**
     * Rewrites every legacy key of a modern {@code advancements/<uuid>.json} object in place.
     *
     * @param root the parsed advancement file
     * @return {@code true} when at least one key was rewritten
     */
    public static boolean migrateJson(JsonObject root) {
        if (root == null || root.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (String oldKey : new ArrayList<>(root.keySet())) {
            String newKey = migrateId(oldKey);
            if (newKey == null || newKey.equals(oldKey)) {
                continue;
            }
            JsonElement legacyValue = root.get(oldKey);
            JsonElement existingValue = root.get(newKey);
            if (existingValue == null || existingValue.isJsonNull()) {
                if (legacyValue != null) {
                    root.add(newKey, legacyValue);
                }
            } else if (existingValue.isJsonObject() && legacyValue != null && legacyValue.isJsonObject()) {
                root.add(newKey, mergeProgress(existingValue.getAsJsonObject(), legacyValue.getAsJsonObject()));
            }
            root.remove(oldKey);
            changed = true;
        }
        return changed;
    }

    @SubscribeEvent
    public static void onLoadFromFile(PlayerEvent.LoadFromFile event) {
        boolean migrated = false;
        try {
            migrated |= migratePlayerDataFile(event);
        } catch (Exception ignored) {
            // A save we cannot rewrite must never keep the player out of the world.
        }
        try {
            migrated |= migrateAdvancementsFile(event);
        } catch (Exception ignored) {
            // Same here.
        }
        if (migrated) {
            reloadPlayerAdvancements(event);
        }
    }

    private static boolean migratePlayerDataFile(PlayerEvent.LoadFromFile event) throws IOException {
        File directory = event.getPlayerDirectory();
        String uuid = event.getPlayerUUID();
        if (directory == null || uuid == null || uuid.isEmpty()) {
            return false;
        }
        File playerFile = new File(directory, uuid + ".dat");
        if (!playerFile.isFile()) {
            return false;
        }

        CompoundTag root;
        try {
            root = NbtIo.readCompressed(playerFile.toPath(), NbtAccounter.unlimitedHeap());
        } catch (Exception exception) {
            return false;
        }
        if (root == null || !(root.get(ADVANCEMENTS_TAG) instanceof CompoundTag stored)) {
            return false;
        }

        CompoundTag migrated = stored.copy();
        migrateTag(migrated);
        if (migrated.equals(stored)) {
            return false;
        }

        root.put(ADVANCEMENTS_TAG, migrated);
        Path target = playerFile.toPath();
        Path temp = target.resolveSibling(target.getFileName() + ".csrp-migration.tmp");
        NbtIo.writeCompressed(root, temp);
        moveIntoPlace(temp, target);
        return true;
    }

    private static boolean migrateAdvancementsFile(PlayerEvent.LoadFromFile event) throws IOException {
        Path path = resolveAdvancementsPath(event);
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }

        JsonObject root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                return false;
            }
            root = element.getAsJsonObject();
        } catch (Exception exception) {
            return false;
        }

        if (!migrateJson(root)) {
            return false;
        }

        Path temp = path.resolveSibling(path.getFileName() + ".csrp-migration.tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(root, GSON.newJsonWriter(writer));
        }
        moveIntoPlace(temp, path);
        return true;
    }

    private static Path resolveAdvancementsPath(PlayerEvent.LoadFromFile event) {
        String uuid = event.getPlayerUUID();
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                return server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(uuid + ".json");
            }
        }
        File directory = event.getPlayerDirectory();
        File parent = directory == null ? null : directory.getParentFile();
        if (parent == null) {
            return null;
        }
        return new File(parent, ADVANCEMENTS_DIRECTORY + File.separator + uuid + ".json").toPath();
    }

    private static void moveIntoPlace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception exception) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void reloadPlayerAdvancements(PlayerEvent.LoadFromFile event) {
        try {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                MinecraftServer server = serverPlayer.getServer();
                if (server != null) {
                    serverPlayer.getAdvancements().reload(server.getAdvancements());
                }
            }
        } catch (Exception ignored) {
            // The next login will pick the migrated file up anyway.
        }
    }

    /** Keeps the criteria of both progress compounds, the preferred one wins on conflicts. */
    private static CompoundTag mergeProgress(CompoundTag preferred, CompoundTag other) {
        CompoundTag merged = preferred.copy();
        for (String key : other.getAllKeys()) {
            Tag otherValue = other.get(key);
            if (otherValue == null) {
                continue;
            }
            Tag existingValue = merged.get(key);
            if (existingValue == null) {
                merged.put(key, otherValue.copy());
            } else if (existingValue instanceof CompoundTag existingCompound
                    && otherValue instanceof CompoundTag otherCompound) {
                CompoundTag combined = existingCompound.copy();
                for (String nestedKey : otherCompound.getAllKeys()) {
                    Tag nestedValue = otherCompound.get(nestedKey);
                    if (nestedValue != null && !combined.contains(nestedKey)) {
                        combined.put(nestedKey, nestedValue.copy());
                    }
                }
                merged.put(key, combined);
            }
        }
        return merged;
    }

    /** Keeps every entry of both progress objects, the preferred one wins on conflicts. */
    private static JsonObject mergeProgress(JsonObject preferred, JsonObject other) {
        JsonObject merged = preferred.deepCopy();
        for (Map.Entry<String, JsonElement> entry : other.entrySet()) {
            JsonElement existingValue = merged.get(entry.getKey());
            if (existingValue == null || existingValue.isJsonNull()) {
                merged.add(entry.getKey(), entry.getValue());
            } else if (existingValue.isJsonObject() && entry.getValue().isJsonObject()) {
                merged.add(entry.getKey(), mergeProgress(existingValue.getAsJsonObject(), entry.getValue().getAsJsonObject()));
            }
        }
        return merged;
    }
}
