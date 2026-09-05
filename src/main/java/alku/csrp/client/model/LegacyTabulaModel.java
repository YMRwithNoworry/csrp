package alku.csrp.client.model;

import alku.csrp.Csrp;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.TabulaModel;
import com.github.alexthe666.citadel.client.model.TabulaModelHandler;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.github.alexthe666.citadel.client.model.container.TabulaModelContainer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Citadel-backed base for the original SRParasites Tabula models.
 *
 * <p>The 1.10.8 source distribution contains the Java files emitted by Tabula,
 * not the editor's project files. The build-time importer converts those
 * exports to Citadel's native {@code .tbl} container format. This class loads
 * that container through {@link TabulaModelHandler} and delegates the model
 * tree to Citadel's {@link TabulaModel}. Composition keeps the public model
 * type generic, which is required by NeoForge's typed renderer API.</p>
 */
public abstract class LegacyTabulaModel<T extends LivingEntity> extends AdvancedEntityModel<T> {
    private final TabulaModel tabulaModel;

    protected LegacyTabulaModel(String modelId) {
        tabulaModel = new TabulaModel(loadContainer(modelId));
        texWidth = tabulaModel.texWidth;
        texHeight = tabulaModel.texHeight;
    }

    @Override
    public final void setupAnim(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        tabulaModel.resetToDefaultPose();
        animateLegacy(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    protected abstract void animateLegacy(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch);

    protected final AdvancedModelBox part(String name) {
        AdvancedModelBox part = findPart(name);
        if (part == null) {
            throw new IllegalArgumentException("Unknown legacy Tabula part: " + name);
        }
        return part;
    }

    public final AdvancedModelBox findPart(String name) {
        return tabulaModel.getCube(name);
    }

    public final Collection<String> partNames() {
        return List.copyOf(tabulaModel.getCubes().keySet());
    }

    @Override
    public final Iterable<BasicModelPart> parts() {
        return tabulaModel.parts();
    }

    @Override
    public final Iterable<AdvancedModelBox> getAllParts() {
        return tabulaModel.getAllParts();
    }

    private static TabulaModelContainer loadContainer(String modelId) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                Csrp.MODID, "tabula/" + modelId + ".tbl");
        try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(location)
                .orElseThrow(() -> new IOException("Missing Tabula model " + location))
                .open(); ZipInputStream archive = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if ("model.json".equals(entry.getName())) {
                    return TabulaModelHandler.INSTANCE.loadTabulaModel(archive);
                }
            }
            throw new IOException("Tabula archive has no model.json: " + location);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load Citadel Tabula model " + location, exception);
        }
    }
}
