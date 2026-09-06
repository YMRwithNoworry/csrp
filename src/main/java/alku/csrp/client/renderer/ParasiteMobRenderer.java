package alku.csrp.client.renderer;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Mob;

/** Shared vanilla/Citadel renderer gate used by the legacy Braining effect. */
public abstract class ParasiteMobRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    protected ParasiteMobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        var player = Minecraft.getInstance().player;
        return (player == null || !player.hasEffect(ModMobEffects.BRAINING.get()))
                && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
    }
}
