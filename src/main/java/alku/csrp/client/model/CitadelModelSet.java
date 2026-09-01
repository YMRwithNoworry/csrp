package alku.csrp.client.model;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import alku.csrp.animation.CitadelAnimatedEntity;

/** Selects one of several original Tabula models for entity types with runtime skins. */
public class CitadelModelSet<T extends Mob & CitadelAnimatedEntity>
        extends AdvancedEntityModel<T> implements CitadelTextureProvider<T> {
    public record ModelSpec(String geometry, String animation) {
        public ModelSpec(String id) {
            this(id, id);
        }
    }

    private final Map<String, CitadelParasiteModel<T>> models = new LinkedHashMap<>();
    private final Function<T, String> selector;
    private final Function<T, ResourceLocation> textureSelector;
    private CitadelParasiteModel<T> active;

    public CitadelModelSet(Map<String, ModelSpec> specifications, Function<T, String> selector,
            Function<T, ResourceLocation> textureSelector) {
        this.selector = selector;
        this.textureSelector = textureSelector;
        for (Map.Entry<String, ModelSpec> entry : specifications.entrySet()) {
            ModelSpec spec = entry.getValue();
            models.put(entry.getKey(), new CitadelParasiteModel<>(spec.geometry(), spec.animation()));
        }
        active = models.values().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Citadel model set cannot be empty"));
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        active = models.get(selector.apply(entity));
        if (active == null) {
            throw new IllegalStateException("No Citadel model registered for " + selector.apply(entity));
        }
        active.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        customize(entity, active, ageInTicks);
    }

    protected void customize(T entity, CitadelParasiteModel<T> model, float ageInTicks) {
    }

    @Override
    public ResourceLocation texture(T entity) {
        return textureSelector.apply(entity);
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return active.parts();
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return active.getAllParts();
    }
}
