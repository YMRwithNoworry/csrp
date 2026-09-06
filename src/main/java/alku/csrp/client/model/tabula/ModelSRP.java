package alku.csrp.client.model.tabula;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import alku.csrp.entity.TabulaAnimationAccess;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Citadel-backed compatibility base for the original SRParasites Tabula Java models.
 *
 * <p>The 1.12 models expose every cube as a public field. Discovering those fields
 * lets mechanically ported models keep their original hierarchy and animation code
 * without maintaining a second hand-written part list.</p>
 */
public abstract class ModelSRP<T extends Entity> extends AdvancedEntityModel<T> {
    private List<AdvancedModelBox> allParts;
    private List<BasicModelPart> rootParts;

    /** Called once at the end of each ported model constructor. */
    protected final void captureDefaultPose() {
        discoverParts();
        updateDefaultPose();
    }

    private void discoverParts() {
        if (allParts != null) {
            return;
        }
        List<AdvancedModelBox> discovered = new ArrayList<>();
        for (Field field : getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    || !AdvancedModelBox.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                AdvancedModelBox part = (AdvancedModelBox) field.get(this);
                if (part != null && !discovered.contains(part)) {
                    discovered.add(part);
                }
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Unable to discover Tabula model part " + field.getName(), exception);
            }
        }
        allParts = Collections.unmodifiableList(discovered);
        rootParts = discovered.stream()
                .filter(part -> part.getParent() == null)
                .map(part -> (BasicModelPart) part)
                .toList();
    }

    @Override
    public final Iterable<BasicModelPart> parts() {
        discoverParts();
        return rootParts;
    }

    @Override
    public final Iterable<AdvancedModelBox> getAllParts() {
        discoverParts();
        return allParts;
    }

    @Override
    public void resetToDefaultPose() {
        super.resetToDefaultPose();
        // Citadel restores rotation/position but not legacy Tabula moveY offsets.
        // Clear them every frame so a prior animation cannot leave the model sunk.
        for (AdvancedModelBox part : getAllParts()) {
            part.offsetX = part.defaultOffsetX;
            part.offsetY = part.defaultOffsetY;
            part.offsetZ = part.defaultOffsetZ;
        }
    }

    @Override
    public final void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        resetToDefaultPose();
        func_78087_a(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625F, entity);
    }

    /** Original 1.12 {@code ModelBase#setRotationAngles} signature retained for direct ports. */
    protected void func_78087_a(float limbSwing, float limbSwingAmount, float ageInTicks,
                                float netHeadYaw, float headPitch, float scaleFactor, T entityIn) {
    }

    protected void underground(Object parasite, float ageInTicks, AdvancedModelBox mainbody) {
    }

    protected final TabulaAnimationAccess animationAccess(T entity) {
        return entity instanceof TabulaAnimationAccess access
                ? access : TabulaAnimationAccess.adapt(entity);
    }

    /**
     * Hides a named Tabula part without coupling the port to one Citadel field
     * name. Citadel has used both {@code showModel} and {@code visible} in its
     * supported Minecraft mappings; the reflective bridge works with either
     * version while keeping generated ports source-compatible.
     */
    public final void setHidden(String fieldName, boolean hidden) {
        try {
            Field field = getClass().getField(fieldName);
            Object value = field.get(this);
            if (!(value instanceof AdvancedModelBox part)) {
                return;
            }
            try {
                Method setter = part.getClass().getMethod("setHidden", boolean.class);
                setter.invoke(part, hidden);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Fall through to field-compatible Citadel variants.
            }
            for (String visibilityField : new String[] {"showModel", "visible"}) {
                try {
                    Field visible = part.getClass().getField(visibilityField);
                    visible.setBoolean(part, !hidden);
                    return;
                } catch (ReflectiveOperationException ignored) {
                    // Try the next supported field name.
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // A model may legitimately omit an optional part.
        }
    }

    public void swingX(AdvancedModelBox model, float speed, float degree, int invert,
                       float limbSwing, float limbSwingAmount) {
        model.rotateAngleX = (float) (invert * limbSwingAmount * degree
                * Math.cos(limbSwing * speed) * limbSwingAmount);
    }

    public void swingX(AdvancedModelBox model, float speed, float degree, int invert,
                       float offset, float weight, float limbSwing, float limbSwingAmount) {
        model.rotateAngleX = (float) (invert * limbSwingAmount * degree
                * Math.cos(limbSwing * speed + offset) + weight * limbSwingAmount);
    }

    public void swingX(float base, AdvancedModelBox model, float speed, float degree, int invert,
                       float limbSwing, float limbSwingAmount) {
        model.rotateAngleX = base + (float) (invert * limbSwingAmount * degree
                * Math.cos(limbSwing * speed) * limbSwingAmount);
    }

    public void swingY(AdvancedModelBox model, float speed, float degree, int invert,
                       float limbSwing, float limbSwingAmount) {
        model.rotateAngleY = (float) (invert * limbSwingAmount * degree
                * Math.cos(limbSwing * speed) * limbSwingAmount);
    }

    public void swingY(AdvancedModelBox model, float speed, float degree, int invert,
                       float offset, float weight, float limbSwing, float limbSwingAmount) {
        model.rotateAngleY = (float) (invert * limbSwingAmount * degree
                * Math.cos(limbSwing * speed + offset) + weight * limbSwingAmount);
    }

    public void swingY(float base, AdvancedModelBox model, float speed, float degree, int invert,
                       float limbSwing, float limbSwingAmount) {
        model.rotateAngleY = base + (float) (invert * limbSwingAmount * degree
                * Math.cos(limbSwing * speed) * limbSwingAmount);
    }

    public void swingZ(AdvancedModelBox model, float speed, float degree, int invert,
                       float limbSwing, float limbSwingAmount) {
        model.rotateAngleZ = (float) (invert * limbSwingAmount * degree
                * Math.cos(limbSwing * speed) * limbSwingAmount);
    }

    public void swingZ(AdvancedModelBox model, float speed, float degree, int invert,
                       float offset, float weight, float limbSwing, float limbSwingAmount) {
        model.rotateAngleZ = (float) (invert * limbSwingAmount * degree
                * Math.cos(limbSwing * speed + offset) + weight * limbSwingAmount);
    }

    public void swingZ(float base, AdvancedModelBox model, float speed, float degree, int invert,
                       float limbSwing, float limbSwingAmount) {
        model.rotateAngleZ = base + (float) (invert * limbSwingAmount * degree
                * Math.cos(limbSwing * speed) * limbSwingAmount);
    }

    public void moveY(AdvancedModelBox model, float speed, int invert,
                      float limbSwing, float limbSwingAmount, float distance) {
        model.offsetY = invert * Mth.cos(limbSwing * speed) * limbSwingAmount * distance;
    }
}
