package alku.csrp.client.model;

import alku.csrp.animation.CitadelAnimatedEntity;
import alku.csrp.client.model.tabula.LegacyModelBox;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Selects one of several original Tabula models for entity types with runtime skins.
 *
 * <p>26.3 renders one {@link ModelPart} root per {@link EntityModel} and both {@code root()} and
 * {@code allParts()} are final, so the selection cannot be delegated by swapping roots. Every
 * variant is built as a {@link CitadelParasiteModel} and their roots are mounted as children of a
 * single synthetic root; {@link #setupAnim} makes the selected variant visible, drives its
 * entity-based animation and hides the others. The variant animation code is untouched and still
 * reads the live entity carried by {@link LegacyMobRenderState#legacyEntity}.</p>
 */
public class CitadelModelSet<T extends Mob & CitadelAnimatedEntity>
        extends EntityModel<LegacyMobRenderState> implements CitadelTextureProvider<T> {
    public record ModelSpec(String geometry, String animation) {
        public ModelSpec(String id) {
            this(id, id);
        }
    }

    private record Assembly<E extends Mob & CitadelAnimatedEntity>(
            ModelPart root, Map<String, CitadelParasiteModel<E>> models, Map<String, ModelPart> roots) {
    }

    private final Map<String, CitadelParasiteModel<T>> models;
    private final Map<String, ModelPart> roots;
    private final Function<T, String> selector;
    private final Function<T, Identifier> textureSelector;
    private CitadelParasiteModel<T> active;

    public CitadelModelSet(Map<String, ModelSpec> specifications, Function<T, String> selector,
            Function<T, Identifier> textureSelector) {
        this(assemble(specifications), selector, textureSelector);
    }

    private CitadelModelSet(Assembly<T> assembly, Function<T, String> selector,
            Function<T, Identifier> textureSelector) {
        super(assembly.root(), RenderTypes::entityCutout);
        this.selector = selector;
        this.textureSelector = textureSelector;
        this.models = new LinkedHashMap<>(assembly.models());
        this.roots = new LinkedHashMap<>(assembly.roots());
        this.active = models.values().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Citadel model set cannot be empty"));
        showOnly(active);
    }

    private static <E extends Mob & CitadelAnimatedEntity> Assembly<E> assemble(
            Map<String, ModelSpec> specifications) {
        Map<String, CitadelParasiteModel<E>> models = new LinkedHashMap<>();
        Map<String, ModelPart> roots = new LinkedHashMap<>();
        for (Map.Entry<String, ModelSpec> entry : specifications.entrySet()) {
            ModelSpec spec = entry.getValue();
            CitadelParasiteModel<E> model = new CitadelParasiteModel<>(spec.geometry(), spec.animation());
            models.put(entry.getKey(), model);
            roots.put(entry.getKey(), model.root());
        }
        ModelPart root = roots.size() == 1
                ? roots.values().iterator().next()
                : new ModelPart(List.of(), new LinkedHashMap<>(roots));
        return new Assembly<>(root, models, roots);
    }

    @Override
    public void setupAnim(LegacyMobRenderState state) {
        LivingEntity legacyEntity = state.legacyEntity;
        if (legacyEntity == null) {
            active.setupAnim(state);
            return;
        }
        @SuppressWarnings("unchecked")
        T entity = (T) legacyEntity;
        String id = selector.apply(entity);
        CitadelParasiteModel<T> selected = models.get(id);
        if (selected == null) {
            throw new IllegalStateException("No Citadel model registered for " + id);
        }
        if (selected != active) {
            active = selected;
            showOnly(active);
        }
        active.setupAnim(state);
        customize(entity, active, state.ageInTicks);
        // customize() mutates LegacyModelBox fields after the variant already synced, so push the
        // final pose onto the wrapped ModelParts before this frame is rendered.
        for (LegacyModelBox box : active.parts()) {
            box.sync();
        }
    }

    protected void customize(T entity, CitadelParasiteModel<T> model, float ageInTicks) {
    }

    @Override
    public Identifier texture(T entity) {
        return textureSelector.apply(entity);
    }

    public Iterable<LegacyModelBox> parts() {
        return active.parts();
    }

    public Iterable<LegacyModelBox> getAllParts() {
        return active.getAllParts();
    }

    private void showOnly(CitadelParasiteModel<T> model) {
        for (Map.Entry<String, ModelPart> entry : roots.entrySet()) {
            entry.getValue().visible = models.get(entry.getKey()) == model;
        }
    }
}
