package alku.csrp.client.model.tabula;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Self-contained stand-in for the Citadel {@code AdvancedModelBox} the SRParasites models were
 * animated against.
 *
 * <p>The 26.3 {@link ModelPart} is {@code final} and owns both the pivot and the rotation, so this
 * type wraps one part and re-exposes Citadel's public field surface on top of it. The angle, pivot
 * and scale fields stay plain public fields so the existing animation code keeps compiling and
 * running unchanged; {@link #sync()} pushes them onto the wrapped part just before rendering.</p>
 *
 * <p>{@code offsetX/Y/Z} are kept for source compatibility only: the Citadel revision these models
 * were authored against never fed them into {@code translateAndRotate}, and its default-pose
 * save/restore of them is commented out, so they have no effect on rendering here either.</p>
 */
public final class LegacyModelBox {
    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;

    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;

    public float offsetX;
    public float offsetY;
    public float offsetZ;

    public float scaleX = 1.0F;
    public float scaleY = 1.0F;
    public float scaleZ = 1.0F;

    public boolean showModel = true;
    public boolean scaleChildren = true;

    public int textureOffsetX;
    public int textureOffsetY;

    public float defaultRotationX;
    public float defaultRotationY;
    public float defaultRotationZ;
    public float defaultPositionX;
    public float defaultPositionY;
    public float defaultPositionZ;

    private final String boxName;
    private final ModelPart part;
    private final LegacyModelBox parent;
    private final List<LegacyModelBox> children = new ArrayList<>();

    LegacyModelBox(String boxName, ModelPart part, LegacyModelBox parent) {
        this.boxName = boxName;
        this.part = part;
        this.parent = parent;
    }

    /** The wrapped vanilla part, for renderers and {@code parts()} style traversal. */
    public ModelPart part() {
        return part;
    }

    public String getName() {
        return boxName;
    }

    public LegacyModelBox getParent() {
        return parent;
    }

    public List<LegacyModelBox> getChildren() {
        return List.copyOf(children);
    }

    void addChild(LegacyModelBox child) {
        children.add(child);
    }

    public void setPos(float x, float y, float z) {
        rotationPointX = x;
        rotationPointY = y;
        rotationPointZ = z;
        part.setPos(x, y, z);
    }

    public void setRotationAngle(float x, float y, float z) {
        rotateAngleX = x;
        rotateAngleY = y;
        rotateAngleZ = z;
        part.setRotation(x, y, z);
    }

    public void setScale(float x, float y, float z) {
        scaleX = x;
        scaleY = y;
        scaleZ = z;
    }

    public void setScaleX(float value) {
        scaleX = value;
    }

    public void setScaleY(float value) {
        scaleY = value;
    }

    public void setScaleZ(float value) {
        scaleZ = value;
    }

    public void setTextureOffset(int x, int y) {
        textureOffsetX = x;
        textureOffsetY = y;
    }

    /** Captures the authored (or previously synced) pose as the one {@code resetToDefaultPose} restores. */
    public void updateDefaultPose() {
        defaultRotationX = rotateAngleX;
        defaultRotationY = rotateAngleY;
        defaultRotationZ = rotateAngleZ;
        defaultPositionX = rotationPointX;
        defaultPositionY = rotationPointY;
        defaultPositionZ = rotationPointZ;
    }

    public void resetToDefaultPose() {
        rotateAngleX = defaultRotationX;
        rotateAngleY = defaultRotationY;
        rotateAngleZ = defaultRotationZ;
        rotationPointX = defaultPositionX;
        rotationPointY = defaultPositionY;
        rotationPointZ = defaultPositionZ;
        sync();
    }

    /** Pushes the animated fields onto the wrapped vanilIa part. */
    public void sync() {
        part.setRotation(rotateAngleX, rotateAngleY, rotateAngleZ);
        part.setPos(rotationPointX, rotationPointY, rotationPointZ);
        part.xScale = scaleX;
        part.yScale = scaleY;
        part.zScale = scaleZ;
        part.visible = showModel;
        for (LegacyModelBox child : children) {
            child.sync();
        }
    }
}
