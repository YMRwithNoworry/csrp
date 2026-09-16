package alku.csrp.client.model.tabula;

import java.util.List;

/**
 * One cube of a Tabula export, mirroring the container the original SRParasites models were authored
 * with. Field order and units match the {@code .tbl} container format exactly so the tree can be
 * rebuilt without reinterpretation.
 *
 * <p>All geometry is in Minecraft's usual 1/16-block model units: {@link #position()} is the pivot
 * (rotation point), {@link #offset()} is the cube origin relative to that pivot, and
 * {@link #rotation()} is stored in degrees.</p>
 */
public record TabulaCube(
        String name,
        String identifier,
        int[] dimensions,
        double[] position,
        double[] offset,
        double[] rotation,
        double[] scale,
        int[] textureOffset,
        boolean mirrored,
        double mcScale,
        double opacity,
        boolean hidden,
        List<TabulaCube> children) {
}
