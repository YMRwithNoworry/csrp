package alku.csrp.client.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/** Keeps imported SRP texture colors while preserving normal world light levels. */
public abstract class ParasiteGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {
        return NeoForgeRenderTypes.getUnlitTranslucent(texture);
    }
}
