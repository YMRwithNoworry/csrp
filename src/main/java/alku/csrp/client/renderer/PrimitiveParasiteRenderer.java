package alku.csrp.client.renderer;

import alku.csrp.client.model.PrimitiveParasiteModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;

public final class PrimitiveParasiteRenderer<T extends Mob & GeoEntity> extends ParasiteGeoRenderer<T> {
    public PrimitiveParasiteRenderer(EntityRendererProvider.Context context, String id, float shadowRadius) {
        super(context, new PrimitiveParasiteModel<>(id));
        this.shadowRadius = shadowRadius;
    }
}
