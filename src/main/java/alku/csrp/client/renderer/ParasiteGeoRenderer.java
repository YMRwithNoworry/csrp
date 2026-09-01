package alku.csrp.client.renderer;

import alku.csrp.animation.CitadelAnimatedEntity;
import alku.csrp.client.model.CitadelTextureProvider;
import alku.csrp.registry.ModMobEffects;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Shared Citadel renderer gate and adaptation tint used by SRP parasites. */
public class ParasiteGeoRenderer<T extends Mob & CitadelAnimatedEntity>
        extends MobRenderer<T, AdvancedEntityModel<T>> {
    protected ParasiteGeoRenderer(EntityRendererProvider.Context context, AdvancedEntityModel<T> model) {
        super(context, model, 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        if (model instanceof CitadelTextureProvider<?> provider) {
            @SuppressWarnings("unchecked")
            CitadelTextureProvider<T> typed = (CitadelTextureProvider<T>) provider;
            return typed.texture(entity);
        }
        throw new IllegalStateException("Citadel parasite model does not supply a texture");
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
