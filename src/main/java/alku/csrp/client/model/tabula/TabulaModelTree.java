package alku.csrp.client.model.tabula;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

/**
 * Turns a parsed {@link TabulaModelData} into the vanilla {@link ModelPart} tree 26.3 renders, plus
 * the named {@link LegacyModelBox} wrappers the animation code drives.
 *
 * <p>The geometry mapping is deliberately the same one Citadel's {@code TabulaModel} used so the
 * authored models are reproduced without any shift: the container's {@code position} becomes the
 * part pivot, {@code offset} plus {@code dimensions} become the cube box, {@code rotation} is
 * converted from degrees to radians, and {@code txOffset}/{@code txMirror} become the texture offset
 * and mirror flag. The container's {@code hidden}, {@code opacity}, {@code mcScale} and the model
 * level {@code scale} were never consulted on the render path before, so they stay ignored.</p>
 */
public final class TabulaModelTree {
    private static final java.util.Set<Direction> ALL_FACES = java.util.EnumSet.allOf(Direction.class);

    private final ModelPart root;
    private final Map<String, LegacyModelBox> byName;
    private final List<LegacyModelBox> allBoxes;

    private TabulaModelTree(ModelPart root, Map<String, LegacyModelBox> byName, List<LegacyModelBox> allBoxes) {
        this.root = root;
        this.byName = byName;
        this.allBoxes = allBoxes;
    }

    public static TabulaModelTree build(TabulaModelData data) {
        Map<String, LegacyModelBox> byName = new LinkedHashMap<>();
        java.util.List<LegacyModelBox> all = new java.util.ArrayList<>();

        // A Tabula export normally has exactly one synthetic root; the vanilla model needs a single
        // part to render from, so multiple roots are folded under a new invisible parent.
        java.util.List<LegacyModelBox> roots = new java.util.ArrayList<>();
        Map<String, ModelPart> rootChildren = new LinkedHashMap<>();
        for (TabulaCube cube : data.roots()) {
            LegacyModelBox box = buildNode(cube, null, data, byName, all);
            roots.add(box);
            rootChildren.put(cube.name(), box.part());
        }

        ModelPart rootPart = roots.size() == 1
                ? roots.get(0).part()
                : new ModelPart(List.of(), rootChildren);

        for (LegacyModelBox box : all) {
            box.updateDefaultPose();
        }
        return new TabulaModelTree(rootPart, byName, all);
    }

    private static LegacyModelBox buildNode(TabulaCube cube, LegacyModelBox parent, TabulaModelData data,
            Map<String, LegacyModelBox> byName, java.util.List<LegacyModelBox> all) {
        Map<String, ModelPart> childParts = new LinkedHashMap<>();
        java.util.List<LegacyModelBox> builtChildren = new java.util.ArrayList<>();
        for (TabulaCube child : cube.children()) {
            LegacyModelBox built = buildNode(child, null, data, byName, all);
            childParts.put(child.name(), built.part());
            builtChildren.add(built);
        }

        List<ModelPart.Cube> cubes = List.of(new ModelPart.Cube(
                cube.textureOffset()[0],
                cube.textureOffset()[1],
                (float) cube.offset()[0],
                (float) cube.offset()[1],
                (float) cube.offset()[2],
                cube.dimensions()[0],
                cube.dimensions()[1],
                cube.dimensions()[2],
                0.0F,
                0.0F,
                0.0F,
                cube.mirrored(),
                data.textureWidth(),
                data.textureHeight(),
                ALL_FACES));

        ModelPart part = new ModelPart(cubes, childParts);
        part.setPos((float) cube.position()[0], (float) cube.position()[1], (float) cube.position()[2]);
        part.setRotation(
                (float) Math.toRadians(cube.rotation()[0]),
                (float) Math.toRadians(cube.rotation()[1]),
                (float) Math.toRadians(cube.rotation()[2]));

        LegacyModelBox box = new LegacyModelBox(cube.name(), part, parent);
        box.setTextureOffset(cube.textureOffset()[0], cube.textureOffset()[1]);
        box.rotationPointX = (float) cube.position()[0];
        box.rotationPointY = (float) cube.position()[1];
        box.rotationPointZ = (float) cube.position()[2];
        box.rotateAngleX = part.xRot;
        box.rotateAngleY = part.yRot;
        box.rotateAngleZ = part.zRot;

        if (parent != null) {
            parent.addChild(box);
        }
        all.add(box);
        if (!byName.containsKey(cube.name())) {
            byName.put(cube.name(), box);
        }
        return box;
    }

    public ModelPart root() {
        return root;
    }

    public LegacyModelBox getBox(String name) {
        return byName.get(name);
    }

    public Map<String, LegacyModelBox> boxes() {
        return java.util.Collections.unmodifiableMap(byName);
    }

    public List<LegacyModelBox> allBoxes() {
        return allBoxes;
    }

    /** Pushes every animated box onto its vanilla part; call right before the model is rendered. */
    public void sync() {
        for (LegacyModelBox box : allBoxes) {
            box.sync();
        }
    }

    /** Restores every box to the pose captured when the tree was built. */
    public void resetToDefaultPose() {
        for (LegacyModelBox box : allBoxes) {
            box.resetToDefaultPose();
        }
    }
}
