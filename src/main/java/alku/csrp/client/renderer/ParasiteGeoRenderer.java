package alku.csrp.client.renderer;

import alku.csrp.animation.CitadelAnimatedEntity;
import alku.csrp.client.model.CitadelTextureProvider;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.registry.ModMobEffects;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Shared Citadel renderer gate and adaptation tint used by SRP parasites. */
public class ParasiteGeoRenderer<T extends Mob & CitadelAnimatedEntity>
        extends MobRenderer<T, AdvancedEntityModel<T>> {
    protected ParasiteGeoRenderer(EntityRendererProvider.Context context, AdvancedEntityModel<T> model) {
        super(context, model, 0.5F);
        addLayer(new AdaptationTintLayer<>(this));
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

    private static final class AdaptationTintLayer<T extends Mob & CitadelAnimatedEntity>
            extends RenderLayer<T, AdvancedEntityModel<T>> {
        private AdaptationTintLayer(ParasiteGeoRenderer<T> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                T entity, float limbSwing, float limbSwingAmount, float partialTick,
                float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(entity instanceof PrimitiveParasiteEntity parasite) || entity.isInvisible()
                    || entity.hurtTime <= 0) {
                return;
            }
            int color = switch (parasite.getAdaptationHitStatus()) {
                case 1 -> 0xFF40FF40;
                case 2 -> 0xFFFF40FF;
                default -> 0;
            };
            if (color != 0) {
                RenderType renderType = RenderType.entityTranslucent(getTextureLocation(entity));
                getParentModel().renderToBuffer(poseStack, bufferSource.getBuffer(renderType),
                        packedLight, OverlayTexture.NO_OVERLAY, color);
            }
        }
    }
}
