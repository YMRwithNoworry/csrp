package alku.csrp.client.model;

import alku.csrp.Csrp;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Citadel model base for the original SRParasites Tabula-exported models.
 *
 * <p>The 1.10.8 distribution contains Java exported by Tabula rather than the
 * original {@code .tbl} projects. During the first port those exports were
 * losslessly represented as Bedrock geometry: every absolute pivot, cube,
 * parent and texture offset is retained. This loader reverses that coordinate
 * conversion and constructs Citadel {@link AdvancedModelBox} instances. The
 * entity-specific subclasses then execute the original Java animation
 * formulae directly on Citadel model boxes.</p>
 */
public abstract class LegacyTabulaModel<T extends LivingEntity> extends AdvancedEntityModel<T> {
    private static final String COORDINATE_ROOT = "srp_coordinate_root";

    private final Map<String, AdvancedModelBox> partsByName = new LinkedHashMap<>();
    private final List<BasicModelPart> rootParts = new ArrayList<>();

    protected LegacyTabulaModel(String modelId) {
        loadGeometry(modelId);
        updateDefaultPose();
    }

    @Override
    public final void setupAnim(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        resetToDefaultPose();
        animateLegacy(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    protected abstract void animateLegacy(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch);

    protected final AdvancedModelBox part(String name) {
        AdvancedModelBox part = partsByName.get(name);
        if (part == null) {
            throw new IllegalArgumentException("Unknown legacy Tabula part: " + name);
        }
        return part;
    }

    public final AdvancedModelBox findPart(String name) {
        return partsByName.get(name);
    }

    public final Collection<String> partNames() {
        return List.copyOf(partsByName.keySet());
    }

    @Override
    public final Iterable<BasicModelPart> parts() {
        return rootParts;
    }

    @Override
    public final Iterable<AdvancedModelBox> getAllParts() {
        return partsByName.values();
    }

    private void loadGeometry(String modelId) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                Csrp.MODID, "geo/" + modelId + ".geo.json");
        try (InputStream stream = Minecraft.getInstance().getResourceManager()
                .getResource(location)
                .orElseThrow(() -> new IOException("Missing legacy Tabula geometry " + location))
                .open();
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject description = geometry.getAsJsonObject("description");
            texWidth = description.get("texture_width").getAsInt();
            texHeight = description.get("texture_height").getAsInt();

            Map<String, BoneDefinition> definitions = new LinkedHashMap<>();
            for (JsonElement element : geometry.getAsJsonArray("bones")) {
                JsonObject bone = element.getAsJsonObject();
                String name = bone.get("name").getAsString();
                String parent = bone.has("parent") ? bone.get("parent").getAsString() : null;
                definitions.put(name, new BoneDefinition(name, parent, vector(bone, "pivot"), bone));
                partsByName.put(name, new AdvancedModelBox(this, name));
            }

            for (BoneDefinition definition : definitions.values()) {
                configurePart(definition, definitions);
            }
            for (BoneDefinition definition : definitions.values()) {
                AdvancedModelBox current = partsByName.get(definition.name());
                if (definition.parent() == null) {
                    rootParts.add(current);
                } else {
                    AdvancedModelBox parent = partsByName.get(definition.parent());
                    if (parent == null) {
                        throw new IOException("Missing parent " + definition.parent()
                                + " for " + definition.name() + " in " + location);
                    }
                    parent.addChild(current);
                }
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load legacy Tabula model " + modelId, exception);
        }
    }

    private void configurePart(BoneDefinition definition,
            Map<String, BoneDefinition> definitions) throws IOException {
        AdvancedModelBox box = partsByName.get(definition.name());
        JsonObject json = definition.json();
        float[] pivot = definition.pivot();

        if (COORDINATE_ROOT.equals(definition.name())) {
            // The intermediate Bedrock export introduced this part solely to
            // change coordinate systems. Citadel already uses the original
            // Tabula/ModelRenderer axes, so retain only the usual model origin.
            box.setPos(0.0F, 24.0F, 0.0F);
        } else {
            float[] parentPivot = definition.parent() == null
                    ? new float[] {0.0F, 24.0F, 0.0F}
                    : requireDefinition(definitions, definition.parent()).pivot();
            box.setPos(-(pivot[0] - parentPivot[0]),
                    -(pivot[1] - parentPivot[1]),
                    -(pivot[2] - parentPivot[2]));
            float[] rotation = vector(json, "rotation");
            box.rotateAngleX = (float) Math.toRadians(-rotation[0]);
            box.rotateAngleY = (float) Math.toRadians(rotation[1]);
            box.rotateAngleZ = (float) Math.toRadians(-rotation[2]);
        }

        box.mirror = json.has("mirror") && json.get("mirror").getAsBoolean();
        box.showModel = !json.has("never_render") || !json.get("never_render").getAsBoolean();
        if (!json.has("cubes")) {
            return;
        }
        for (JsonElement element : json.getAsJsonArray("cubes")) {
            addCube(box, pivot, element.getAsJsonObject());
        }
    }

    private static BoneDefinition requireDefinition(Map<String, BoneDefinition> definitions,
            String name) throws IOException {
        BoneDefinition definition = definitions.get(name);
        if (definition == null) {
            throw new IOException("Missing Tabula bone definition " + name);
        }
        return definition;
    }

    private static void addCube(AdvancedModelBox box, float[] pivot, JsonObject cube) {
        float[] origin = vector(cube, "origin");
        float[] size = vector(cube, "size");
        int[] textureOffset = textureOffset(cube, size);
        float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0.0F;
        boolean mirror = cube.has("mirror") ? cube.get("mirror").getAsBoolean() : box.mirror;

        // The Bedrock intermediate uses absolute pivots and opposite axes.
        // Negating the far cube corner recovers Tabula's local addBox offset.
        float x = -(origin[0] - pivot[0] + size[0]);
        float y = -(origin[1] - pivot[1] + size[1]);
        float z = -(origin[2] - pivot[2] + size[2]);
        box.setTextureOffset(textureOffset[0], textureOffset[1]);
        box.addBox(x, y, z, size[0], size[1], size[2], inflate, mirror);
    }

    private static int[] textureOffset(JsonObject cube, float[] size) {
        JsonElement uvElement = cube.get("uv");
        if (uvElement == null) {
            return new int[] {0, 0};
        }
        if (uvElement.isJsonArray()) {
            JsonArray uv = uvElement.getAsJsonArray();
            return new int[] {uv.get(0).getAsInt(), uv.get(1).getAsInt()};
        }
        JsonObject faces = uvElement.getAsJsonObject();
        JsonObject west = faces.has("west") ? faces.getAsJsonObject("west")
                : faces.entrySet().iterator().next().getValue().getAsJsonObject();
        JsonArray uv = west.getAsJsonArray("uv");
        return new int[] {uv.get(0).getAsInt(), Math.round(uv.get(1).getAsFloat() - size[2])};
    }

    private static float[] vector(JsonObject object, String member) {
        if (!object.has(member)) {
            return new float[] {0.0F, 0.0F, 0.0F};
        }
        JsonArray array = object.getAsJsonArray(member);
        return new float[] {array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    private record BoneDefinition(String name, String parent, float[] pivot, JsonObject json) {
    }
}
