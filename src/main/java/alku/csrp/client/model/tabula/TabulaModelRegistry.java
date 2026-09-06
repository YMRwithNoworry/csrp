package alku.csrp.client.model.tabula;

import alku.csrp.client.model.tabula.inborn.ModelLodo;
import alku.csrp.client.model.tabula.inborn.ModelMudo;
import alku.csrp.client.model.tabula.primitive.ModelCanra;
import alku.csrp.client.model.tabula.primitive.ModelGim;
import alku.csrp.client.model.tabula.primitive.ModelIki;
import alku.csrp.client.model.tabula.primitive.ModelRanrac;
import alku.csrp.client.model.tabula.primitive.ModelShyco;
import alku.csrp.client.model.tabula.pure.ModelEsor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Mob;

import java.util.function.Supplier;

/**
 * Resolves the direct Citadel ports of SRParasites' Tabula models.
 *
 * <p>Most generated model classes deliberately retain their original Tabula
 * name. Reflection keeps this registry small and, more importantly, means a
 * newly imported model does not require a second hand-maintained import list.
 * The five primitive models and three hand-written models have legacy names,
 * so they are the only explicit aliases here.</p>
 */
public final class TabulaModelRegistry {
    private static final String GENERATED_PACKAGE =
            "alku.csrp.client.model.tabula.generated.ModelTabula_";

    private TabulaModelRegistry() {
    }

    /** Create a model using the model id used by the SRP asset/entity registry. */
    @SuppressWarnings("unchecked")
    public static EntityModel<Mob> create(String id) {
        Supplier<? extends EntityModel<?>> supplier = switch (id) {
            case "pri_longarms" -> ModelShyco::new;
            case "pri_summoner" -> ModelCanra::new;
            case "pri_vermin" -> ModelIki::new;
            case "pri_viscera" -> ModelGim::new;
            case "pri_arachnida" -> ModelRanrac::new;
            case "pri_bolster" -> alku.csrp.client.model.tabula.primitive.ModelBano::new;
            case "buglin" -> ModelLodo::new;
            case "rupter" -> ModelMudo::new;
            case "marauder" -> ModelEsor::new;
            default -> () -> reflectGenerated(id);
        };
        return (EntityModel<Mob>) supplier.get();
    }

    private static EntityModel<?> reflectGenerated(String id) {
        String className = GENERATED_PACKAGE + id.replace('-', '_');
        try {
            return (EntityModel<?>) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException(
                    "No Citadel Tabula model registered for entity/model id '" + id + "'", exception);
        }
    }
}
