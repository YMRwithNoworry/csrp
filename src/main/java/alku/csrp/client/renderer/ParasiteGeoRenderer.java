package alku.csrp.client.renderer;

import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

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

    @Override
    public Color getRenderColor(T entity, float partialTick, int packedLight) {
        if (entity instanceof PrimitiveParasiteEntity parasite && parasite.hurtTime > 0) {
            return switch (parasite.getAdaptationHitStatus()) {
                case 1 -> Color.ofRGBA(64, 255, 64, 255);
                case 2 -> Color.ofRGBA(255, 64, 255, 255);
                default -> super.getRenderColor(entity, partialTick, packedLight);
            };
        }
        return super.getRenderColor(entity, partialTick, packedLight);
    }
}
