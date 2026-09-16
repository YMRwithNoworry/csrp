package alku.csrp.client.model.tabula;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Reads the Tabula {@code .tbl} archives that ship in {@code assets/<namespace>/tabula/}.
 *
 * <p>A {@code .tbl} is a ZIP holding a single {@code model.json}. The mod used to hand that stream to
 * Citadel's {@code TabulaModelHandler}; with Citadel unavailable past 1.21.11 the container is parsed
 * here, keeping the exact same field semantics so the authored SRParasites models are reproduced
 * unchanged.</p>
 */
public final class TabulaModelLoader {
    private static final String MODEL_ENTRY = "model.json";

    /**
     * The build-time importer wrapped every model in a synthetic {@code srp_coordinate_root} cube
     * positioned at y=24, which would shift every part down by 24 pixels (1.5 blocks). The original
     * models already use the vanilla 24-pixel ground plane, so the synthetic root is flattened back
     * onto the origin.
     */
    private static final String SYNTHETIC_ROOT = "srp_coordinate_root";

    private TabulaModelLoader() {
    }

    /** Loads {@code assets/<namespace>/tabula/<modelId>.tbl}. */
    public static TabulaModelData load(Identifier location) {
        try (InputStream stream = open(location); ZipInputStream archive = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (MODEL_ENTRY.equals(entry.getName())) {
                    TabulaModelData data = parse(new InputStreamReader(archive, StandardCharsets.UTF_8));
                    normaliseGroundPlane(data);
                    return data;
                }
            }
            throw new IOException("Tabula archive has no " + MODEL_ENTRY + ": " + location);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load Tabula model " + location, exception);
        }
    }

    private static InputStream open(Identifier location) throws IOException {
        return Minecraft.getInstance().getResourceManager().getResource(location)
                .orElseThrow(() -> new IOException("Missing Tabula model " + location))
                .open();
    }

    static TabulaModelData parse(InputStreamReader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        List<TabulaCube> cubes = new ArrayList<>();
        if (root.has("cubes")) {
            for (JsonElement element : root.getAsJsonArray("cubes")) {
                cubes.add(parseCube(element.getAsJsonObject()));
            }
        }
        return new TabulaModelData(
                string(root, "modelName", ""),
                integer(root, "textureWidth", 64),
                integer(root, "textureHeight", 32),
                doubles(root, "scale", 3),
                List.copyOf(cubes));
    }

    private static TabulaCube parseCube(JsonObject json) {
        List<TabulaCube> children = new ArrayList<>();
        if (json.has("children")) {
            for (JsonElement element : json.getAsJsonArray("children")) {
                children.add(parseCube(element.getAsJsonObject()));
            }
        }
        return new TabulaCube(
                string(json, "name", ""),
                string(json, "identifier", ""),
                ints(json, "dimensions", 3),
                doubles(json, "position", 3),
                doubles(json, "offset", 3),
                doubles(json, "rotation", 3),
                doubles(json, "scale", 3),
                ints(json, "txOffset", 2),
                json.has("txMirror") && json.get("txMirror").getAsBoolean(),
                number(json, "mcScale", 0.0D),
                number(json, "opacity", 100.0D),
                json.has("hidden") && json.get("hidden").getAsBoolean(),
                List.copyOf(children));
    }

    private static void normaliseGroundPlane(TabulaModelData data) {
        for (TabulaCube root : data.roots()) {
            if (!SYNTHETIC_ROOT.equals(root.name())) {
                continue;
            }
            double[] position = root.position();
            if (position == null || position.length < 2) {
                return;
            }
            position[1] = 0.0D;
            return;
        }
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : fallback;
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsDouble() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsInt() : fallback;
    }

    private static double[] doubles(JsonObject json, String key, int length) {
        double[] out = new double[length];
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray array = json.getAsJsonArray(key);
            for (int i = 0; i < length && i < array.size(); i++) {
                out[i] = array.get(i).getAsDouble();
            }
        }
        return out;
    }

    private static int[] ints(JsonObject json, String key, int length) {
        int[] out = new int[length];
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray array = json.getAsJsonArray(key);
            for (int i = 0; i < length && i < array.size(); i++) {
                out[i] = array.get(i).getAsInt();
            }
        }
        return out;
    }
}
