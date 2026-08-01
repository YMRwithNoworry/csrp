package alku.csrp.client.renderer;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Shared SRP renderer gate used by the legacy Braining effect. */
public class ParasiteGeoRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {
    protected ParasiteGeoRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return !isHiddenByBraining() && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
    }

    protected final boolean isHiddenByBraining() {
        var player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(ModMobEffects.BRAINING);
    }
}
