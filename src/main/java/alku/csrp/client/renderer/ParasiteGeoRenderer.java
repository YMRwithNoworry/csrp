package alku.csrp.client.renderer;

import alku.csrp.animation.CitadelAnimatedEntity;
import alku.csrp.client.model.CitadelTextureProvider;
import alku.csrp.client.model.LegacyMobRenderState;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/** Shared Citadel renderer gate and adaptation tint used by SRP parasites. */
public class ParasiteGeoRenderer<T extends Mob & CitadelAnimatedEntity, M extends EntityModel<LegacyMobRenderState>>
        extends MobRenderer<T, LegacyMobRenderState, M> {
    /** Legacy SRP feedback colours for a partial and a full damage adaptation. */
    private static final int ADAPTATION_PARTIAL_TINT = 0xFF40FF40;
    private static final int ADAPTATION_FULL_TINT = 0xFFFF40FF;

    protected ParasiteGeoRenderer(EntityRendererProvider.Context context, M model) {
        super(context, model, 0.5F);
    }

    @Override
    public LegacyMobRenderState createRenderState() {
        return new LegacyMobRenderState();
    }

    @Override
    public void extractRenderState(T entity, LegacyMobRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.legacyEntity = entity;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Identifier getTextureLocation(LegacyMobRenderState state) {
        T entity = (T) state.legacyEntity;
        if (model instanceof CitadelTextureProvider<?> provider) {
            CitadelTextureProvider<T> typed = (CitadelTextureProvider<T>) provider;
            return typed.texture(entity);
        }
        throw new IllegalStateException("Citadel parasite model does not supply a texture");
    }

    @Override
    public boolean shouldRender(T entity, Frustum culler, double cameraX, double cameraY, double cameraZ,
            float partialTicks) {
        return !isHiddenByBraining() && super.shouldRender(entity, culler, cameraX, cameraY, cameraZ,
                partialTicks);
    }

    protected final boolean isHiddenByBraining() {
        var player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(ModMobEffects.BRAINING);
    }

    /**
     * Reproduces the legacy {@code getRenderColor} adaptation feedback: while a parasite is still
     * flashing from a hit, the whole model is tinted green for a partial adaptation and purple once
     * the learned points exceed the tier's maximum. 26.3 pushes the tint through this hook because
     * the living render state no longer carries {@code hurtTime}.
     */
    @Override
    protected int getModelTint(LegacyMobRenderState state) {
        if (!(state.legacyEntity instanceof PrimitiveParasiteEntity parasite) || parasite.hurtTime <= 0) {
            return super.getModelTint(state);
        }
        return switch (parasite.getAdaptationHitStatus()) {
            case 1 -> ADAPTATION_PARTIAL_TINT;
            case 2 -> ADAPTATION_FULL_TINT;
            default -> super.getModelTint(state);
        };
    }

}
