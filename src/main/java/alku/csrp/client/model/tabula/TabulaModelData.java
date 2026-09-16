package alku.csrp.client.model.tabula;

import java.util.List;

/**
 * The contents of a {@code .tbl} archive's {@code model.json}: texture size plus the root cubes of
 * the model tree.
 */
public record TabulaModelData(
        String name,
        int textureWidth,
        int textureHeight,
        double[] scale,
        List<TabulaCube> roots) {
}
