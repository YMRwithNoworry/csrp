package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.client.model.tabula.LegacyModelBox;
import alku.csrp.client.model.tabula.TabulaModelLoader;
import alku.csrp.client.model.tabula.TabulaModelTree;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Render-state base for the original SRParasites Tabula models.
 *
 * <p>26.3 renders {@link EntityModel}s from an {@link net.minecraft.client.renderer.entity.state.EntityRenderState}
 * instead of a live entity. The mod's 138 Tabula models animate by reading live entity state through
 * custom entity methods, so {@link LegacyMobRenderState} carries the entity reference back to
 * {@link #animateLegacy}; the entity-driven animation code is therefore preserved verbatim.</p>
 *
 * <p>The model tree is built by the project-local {@code alku.csrp.client.model.tabula} replacement
 * (which stands in for Citadel's {@code TabulaModel}), and {@link #setupAnim} drives it exactly as
 * the old Citadel-backed class did.</p>
 */
public abstract class LegacyTabulaModel<S extends LegacyMobRenderState> extends EntityModel<S> {
    private final TabulaModelTree tree;

    protected LegacyTabulaModel(String modelId) {
        this(TabulaModelTree.build(TabulaModelLoader.load(Identifier.fromNamespaceAndPath(
                Csrp.MODID, "tabula/" + modelId + ".tbl"))));
    }

    private LegacyTabulaModel(TabulaModelTree tree) {
        super(tree.root(), RenderTypes::entityCutout);
        this.tree = tree;
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        tree.resetToDefaultPose();
        if (state.legacyEntity != null) {
            animateLegacy(state.legacyEntity, state.walkAnimationPos, state.walkAnimationSpeed,
                    state.ageInTicks, state.yRot, state.xRot);
        }
        tree.sync();
    }

    protected abstract void animateLegacy(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch);

    protected final LegacyModelBox part(String name) {
        LegacyModelBox part = findPart(name);
        if (part == null) {
            throw new IllegalArgumentException("Unknown legacy Tabula part: " + name);
        }
        return part;
    }

    public final LegacyModelBox findPart(String name) {
        return tree.getBox(name);
    }

    public final Collection<String> partNames() {
        return List.copyOf(tree.boxes().keySet());
    }

    public final Iterable<LegacyModelBox> parts() {
        return tree.allBoxes();
    }

    public final Iterable<LegacyModelBox> getAllParts() {
        return tree.allBoxes();
    }
}
