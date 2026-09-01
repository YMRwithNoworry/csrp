package alku.csrp.client.model;

import alku.csrp.entity.AssimilatedParasiteEntity;
import java.util.Locale;
import java.util.Map;

/** Chooses the original Tabula model matching the registered assimilated animal kind. */
public final class AssimilatedParasiteModel extends CitadelModelSet<AssimilatedParasiteEntity> {
    public AssimilatedParasiteModel() {
        super(Map.of(
                "sim_bear", new ModelSpec("sim_bear"),
                "sim_cow", new ModelSpec("sim_cow"),
                "sim_pig", new ModelSpec("sim_pig"),
                "sim_sheep", new ModelSpec("sim_sheep"),
                "sim_wolf", new ModelSpec("sim_wolf"),
                "sim_squid", new ModelSpec("sim_squid")),
                entity -> "sim_" + entity.getKind().name().toLowerCase(Locale.ROOT),
                AssimilatedParasiteEntity::getTextureResource);
    }
}
